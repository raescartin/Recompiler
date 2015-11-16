/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import utils.FixedBitSet;
//Each node may have 1 or 3 children, if a node has 1 children it's a subnode of this 1 node, if a node has 3 children it's a supernode of these
//Each node may have 1 or multiple parents, if a node has 1 parent it's a subnode of this one parent, if a node has multiple parents it's a supernode of these
//Each node can only be parent to one children, but each node may me "in" of multiple instances
public class Node {
	public ArrayList<Node> parents;//ArrayList, since there can be repetition
	public ArrayList<Node> children;//TODO:LinkedHashSet for order without repetition //needed?//size min 3?
	public Instance outOfInstance;
	public HashSet<Instance> inOfInstances;	
	public Definition definition;
	//DEBUGGING ONLY
	public int idForDefinition;//id of node for the definition where it's used
	//END OF DEBUGGING ONLY
	
	public Node() { 
		this.parents = new ArrayList<Node>();
		this.children = new ArrayList<Node>();
		this.inOfInstances = new HashSet<Instance>();//FIXME:needed?
	}
	public Node add(Node node) {//add subnode to supernode		
		node.parents.add(this);
		this.children.add(node);
		if(this.definition!=null){
			this.definition.add(node);
		}
		if(node.definition!=null){
			node.definition.add(this);
		}
		return node;
	}
	public NandNode toNands(HashMap<Node, NandNode> nodeToNand, NandForest nandForest) {
		//PRE: Node fission
		//Nodes are mapped up to down
		//tree is traversed down to up (so recursive calls needed)
		//evaluate node
		//-if a node is both in and out, there's no need to optimize it, nor take it to nandForest
		//-nand ins maybe set before toNands
		//-using "indexOf(this) seems unreliable
		NandNode nandNode;
		if(nodeToNand.containsKey(this)){//this map is to keep track of evaluated nodes
			nandNode=nodeToNand.get(this);//evaluated node
		}else{//node is out of an instance of NAND definition
			Node node1=this.outOfInstance.in.get(0);
			Node node2=this.outOfInstance.in.get(1);
			NandNode nandNode1=node1.toNands(nodeToNand,nandForest);
			NandNode nandNode2=node2.toNands(nodeToNand,nandForest);
			nandNode=nandForest.add(nandNode1,nandNode2);
			nodeToNand.put(this, nandNode);
		}
		return nandNode;
	}
	public String toString() {
		String string = new String();		
		if(!this.parents.isEmpty()){
			if(this.parents.size()==1){
				string+=this.parents.get(0).toString();
				string+="{";
				string+=this.parents.get(0).children.indexOf(this);
				string+="}";
			}else{
				string+=this.idForDefinition;
				string+="(";
				for(Node node: this.parents){
					string+=node.toString();
					string+=("&");
				}
				string=string.substring(0, string.length() - 1);//remove last enumeration ","
				string+=")";
			}
		}else{
			string+=this.idForDefinition;
		}
		return string;
	}
	public int write(ObjectOutputStream stream, HashMap<Node, Integer> nodeMap, int nodeIndex) throws IOException {
		if(nodeMap.containsKey(this)){
			stream.write(nodeMap.get(this));
		}else{
			nodeMap.put(this, nodeIndex);
			stream.write(nodeIndex);
			nodeIndex++;
			stream.write(this.parents.size());
			for (Node node : this.parents) {
				nodeIndex=node.write(stream,nodeMap,nodeIndex);
			}
		}
		return nodeIndex;
	}
	public void read(ObjectInputStream stream, HashMap<Integer, Node> nodeMap) throws IOException {
		//TODO: read subnodes
		Node node;
		int keyNode;
		int size =stream.read();//subnodes size
		for (int i = 0; i < size; i++) {//add subnodes
			keyNode=stream.read();
			if(nodeMap.containsKey(keyNode)){
				node=nodeMap.get(keyNode);
				this.parents.add(node);
			}else{
				node = new Node();
				node.read(stream,nodeMap);
				this.parents.add(node);
				nodeMap.put(keyNode,node);
			}
		}
		
	}
	public void eval(HashMap<Node, FixedBitSet> valueMap, HashSet<Instance> recursiveInstances, HashSet<Instance> instancesToExpand) {
		if(!valueMap.containsKey(this)){//Non evaluated node
			if(!this.parents.isEmpty()){
				for (Node parent : this.parents) {
					parent.eval(valueMap, recursiveInstances, instancesToExpand);
				}
				if(this.parents.size()==1){
					if(valueMap.containsKey(this.parents.get(0))){
						if(valueMap.get(this.parents.get(0)).length()==0){
							valueMap.put(this.parents.get(0).children.get(0),new FixedBitSet());
							valueMap.put(this.parents.get(0).children.get(1),new FixedBitSet());
							valueMap.put(this.parents.get(0).children.get(2),new FixedBitSet());
						}else if(valueMap.get(this.parents.get(0)).length()==1){
							if(!this.parents.get(0).children.get(2).children.isEmpty()&&this.parents.get(0).children.get(1).children.get(0)==this.parents.get(0).children.get(2).children.get(0)){
								valueMap.put(this.parents.get(0).children.get(0),valueMap.get(this.parents.get(0)).get(0,0));
								valueMap.put(this.parents.get(0).children.get(1),new FixedBitSet());
								valueMap.put(this.parents.get(0).children.get(2),new FixedBitSet());
							}else{
								valueMap.put(this.parents.get(0).children.get(0),new FixedBitSet());
								valueMap.put(this.parents.get(0).children.get(1),new FixedBitSet());
								valueMap.put(this.parents.get(0).children.get(2),valueMap.get(this.parents.get(0)).get(0,0));//Always left recursion TODO: check if there's a better way
							}
						}else{
							for(int i=0;i<this.parents.get(0).children.size();i++){
								if(i<this.parents.get(0).children.size()/2){
									valueMap.put(this.parents.get(0).children.get(i), valueMap.get(this.parents.get(0)).get(i,i));
								}else if(valueMap.get(this.parents.get(0)).length()==2){
									valueMap.put(this.parents.get(0).children.get(1),new FixedBitSet());
									valueMap.put(this.parents.get(0).children.get(2),valueMap.get(this.parents.get(0)).get(1,1));
							    }else if(i>(this.parents.get(0).children.size())/2){
							    	valueMap.put(this.parents.get(0).children.get(i), valueMap.get(this.parents.get(0)).get(valueMap.get(this.parents.get(0)).length()-this.parents.get(0).children.size()/2-2+i,valueMap.get(this.parents.get(0)).length()-this.parents.get(0).children.size()/2-2+i));
							    }else if(i==this.parents.get(0).children.size()/2){
							    	valueMap.put(this.parents.get(0).children.get(i),valueMap.get(this.parents.get(0)).get(i,valueMap.get(this.parents.get(0)).length()-i-1));
							    }
							}
						}
					}
				}else{
					FixedBitSet fixedBitSet = new FixedBitSet();
					boolean parents=false;
					for(Node parent:this.parents){
						if(valueMap.containsKey(parent)){
							fixedBitSet.concat(valueMap.get(parent));
							parents=true;
						}
					}
					if(parents)valueMap.put(this, fixedBitSet);
				}
			}else{
				if(this.outOfInstance!=null){
					if(this.definition==this.outOfInstance.definition){//recursive
						if(instancesToExpand.contains(this.outOfInstance)){
							recursiveInstances.remove(this.outOfInstance);
							this.outOfInstance.eval(valueMap, recursiveInstances, instancesToExpand);							
						}else{
							recursiveInstances.add(this.outOfInstance);
						}
					}else{
						this.outOfInstance.eval(valueMap, recursiveInstances, instancesToExpand);
					}
				}
			}
		}
	}
	public boolean childrenAreSubnodes() {
		boolean subnodes=false;
		if(this.children.size()>1){
			subnodes=true;
			for(Node node:this.children){
				if(node.parents.size()!=1){
					subnodes=false;
				}
			}
		}
		return subnodes;
	}
	public void getChildren(HashSet<Node> inOutNodes) {
		inOutNodes.add(this);
		for(Node child:this.children){
			child.getChildren(inOutNodes);
		}
	}
	public void getParents(HashSet<Node> inOutNodes) {
		inOutNodes.add(this);
		for(Node parent:this.parents){
			parent.getParents(inOutNodes);
		}
	}
	
	public void mapInChildren(Boolean used,HashMap<Node, NandNode> nodeToNand, NandForest nandForest,ArrayList<Node> nandToNodeIn, HashSet<Node> inOutNodes) {	
		//Only maps nodes that are used in the definition		
		inOutNodes.add(this);//keep track of nodes previous to the nodes mapped to NandForest in order to not erase them
		if(!this.inOfInstances.isEmpty()) used=true;
		if(this.children.size()<=1){
			if(used){
				NandNode nandNode;
				if(nandToNodeIn.contains(this)){
					nandNode=nodeToNand.get(this);
				}else{
					nandNode = nandForest.addIn();
					nandToNodeIn.add(this);
				}
				nodeToNand.put(this, nandNode);
			}
		}else{		
			for(Node child:this.children){
				child.mapInChildren(used, nodeToNand, nandForest, nandToNodeIn, inOutNodes);	
			}
			
		}
	}
	public void mapOutParents(HashMap<Node, NandNode> nodeToNand,
			NandForest nandForest, ArrayList<Node> nandToNodeOut, HashSet<Node> inOutNodes) {
		ArrayList<NandNode> nandNodes = new ArrayList<NandNode> ();
		inOutNodes.add(this);
		if(this.outOfInstance!=null){
			NandNode nandNode;
			if(nandToNodeOut.contains(this)){
				nandNode = nodeToNand.get(this);
			}else{
				nandNode = nandForest.setOut(this.toNands(nodeToNand,nandForest));
				nandToNodeOut.add(this);
			}
			nandNodes.add(nandNode);
		}else{
			for(Node parent:this.parents){
				parent.mapOutParents(nodeToNand, nandForest, nandToNodeOut, inOutNodes);
			}
		}
	}
	public void fission() {
		//PRE: only nand definitions
		//find the smallest node and normalize the rest to it's size
		if(this.outOfInstance!=null){//the node is out of NAND instance
				this.outOfInstance.in.get(0).fission();
				this.outOfInstance.in.get(1).fission();
				this.carryNand(this.outOfInstance.definition,this.outOfInstance.in.get(0),this.outOfInstance.in.get(1));
		}else{
			for(Node parent:this.parents){
				parent.fission();
			}
		}
	}
	private void carryNand(Definition definition, Node node0, Node node1) { 
		this.carryNandParents(definition, node0, node1);
		this.carryNandChildren(definition, node0, node1);
	}
	
	private void carryNandChildren(Definition definition, Node node0,
			Node node1) {
		if(this.parents.size()<2&&(node0.children.size()>1||node1.children.size()>1)){
			this.outOfInstance.definition.chopEquals(this.outOfInstance.in.get(0),this.outOfInstance.in.get(1),this.outOfInstance.out.get(0));//recursively splits nodes in the same quantity of subnodes
			this.definition.remove(this.outOfInstance);
			this.outOfInstance=null;
			node0.inOfInstances.remove(this.outOfInstance);
			node1.inOfInstances.remove(this.outOfInstance);
			for(int i=0;i<this.children.size();i++){
				Node[] nodes={node0.children.get(i),node1.children.get(i),this.children.get(i)};
				this.definition.add(definition, nodes);
				this.children.get(i).carryNandChildren(definition, node0.children.get(i), node1.children.get(i));
			}
		}
	}
	private void carryNandParents(Definition definition, Node node0, Node node1) {
		if(this.parents.size()>1&&node0.children.size()<2&&node1.children.size()<2){
			this.outOfInstance.definition.chopEquals(this.outOfInstance.in.get(0),this.outOfInstance.in.get(1),this.outOfInstance.out.get(0));//recursively splits nodes in the same quantity of subnodes
			this.definition.remove(this.outOfInstance);
			this.outOfInstance=null;
			node0.inOfInstances.remove(this.outOfInstance);
			node1.inOfInstances.remove(this.outOfInstance);
			for(int i=0;i<this.parents.size();i++){
				Node[] nodes={node0.parents.get(i),node1.parents.get(i),this.parents.get(i)};
				this.definition.add(definition, nodes);
				this.parents.get(i).carryNandParents(definition, node0.parents.get(i), node1.parents.get(i));
			}
		}
	}
	public void splitChildren() {
		if(this.children.size()!=3){
			if(this.children.size()==0){
				Node leftNode = new Node();
				Node centerNode = new Node();
				Node rightNode = new Node();
				this.add(leftNode);
				this.add(centerNode);
				this.add(rightNode);
			}
		}
		
	}
public void childrenFission() {
	if(this.parents.size()==1){
		this.nandChildrenFission();
	}
	for(Node parent:this.parents){
		parent.childrenFission();
	}
}
private void nandChildrenFission() {
	if(this.parents.get(0).parents.size()==1){
		this.parents.get(0).nandChildrenFission();
	}
	if(this.parents.get(0).outOfInstance!=null){
		Node inLeft=this.parents.get(0).outOfInstance.in.get(0);
		Node inRight=this.parents.get(0).outOfInstance.in.get(1);
		Node out=this.parents.get(0);
		inLeft.inOfInstances.remove(out.outOfInstance);
		inRight.inOfInstances.remove(out.outOfInstance);
		if(inLeft.parents.size()==1){
			if(inLeft.parents.get(0).children.indexOf(inLeft)==0||inLeft.parents.get(0).children.indexOf(inLeft)==2){
				
			}
		}
		if(inRight.parents.size()==1){
			if(inRight.parents.get(0).children.indexOf(inRight)==0||inRight.parents.get(0).children.indexOf(inRight)==2){
				
			}
		}
		inLeft.splitChildren();
		inRight.splitChildren();
		for(int i=0;i<3;i++){
			Node[] nodes={inLeft.children.get(i),inRight.children.get(i),out.children.get(i)};
			this.definition.add(out.outOfInstance.definition, nodes);
			out.children.get(i).parents.clear();
		}
		this.definition.remove(out.outOfInstance);
		inLeft.children.get(1).childrenFission();
		inRight.children.get(1).childrenFission();
	}
}
public void parentsFission() {
	for(Node parent:this.parents){
		parent.parentsFission();
	}
	if(this.outOfInstance!=null){//out of nand
		this.nandParentFission();
	}
}
private void nandParentFission() {
	Node in0=this.outOfInstance.in.get(0);
	Node in1=this.outOfInstance.in.get(1);
	in0.parentsFission();
	in1.parentsFission();
	if(in0.parents.size()>1||in1.parents.size()>1){
		if(in0.parents.size()!=in1.parents.size()){
			System.out.print("Error, different parentSize.");
		}else{
			this.definition.remove(this.outOfInstance);
			in0.inOfInstances.remove(this.outOfInstance);
			in1.inOfInstances.remove(this.outOfInstance); 
			for(int i=0;i<in0.parents.size();i++){//should be recursive into parents
				Node newNode = new Node();
				Node[] nodes={in0.parents.get(i),in1.parents.get(i),newNode};
				this.definition.add(this.outOfInstance.definition, nodes);
				newNode.add(this);
				newNode.nandParentFission();
			}
			this.outOfInstance=null;
			
		}
	}
}
	//	public void parentsFission() {
//		for(Node children:this.children){//recursive
//			children.parentsFission();
//		}
//		if(this.children.size()==1){
//			if(!this.children.get(0).inOfInstances.isEmpty()){//is in of at least one instance of nand definition
//				for(Instance nandInstance:this.children.get(0).inOfInstances){
//					Definition nand = nandInstance.definition;
//					Node in0=nandInstance.in.get(0);
//					Node in1=nandInstance.in.get(1);
//					Node out=nandInstance.out.get(0);
//					//TODO make sure in0.children.size()==in1.children.size() by going up
//					//should be RECURSIVE into parents
//					if(in0.parents.size()!=in1.parents.size()){
//						System.out.print("Error, different parentSize.");
//					}else{
//						this.definition.remove(nandInstance);
//						in0.inOfInstances.remove(nandInstance);
//						in1.inOfInstances.remove(nandInstance);
//						out.outOfInstance=null;
//						for(int i=0;i<in0.parents.size();i++){//should be recursive into parents
//							Node newNode = new Node();
//							Node[] nodes={in0.parents.get(i),in1.parents.get(i),newNode};
//							this.definition.add(nand, nodes);
//							newNode.add(out);
//						}
//						out.parents.get(0).parentsFission();
//					
//					}
//					
//				}
//			}
//		}
//		if(!this.inOfInstances.isEmpty()){//is in of at least one instance of nand definition
//			for(Instance nandInstance:this.inOfInstances){
//				nandInstance.out.get(0).parentsFission();
//			}
//		}
//		
//	}
	public void recursivelyMapParents(HashMap<Node, Node> definitionToInstanceNodes) {
		if(this.parents.size()==1){
			Node parent=this.parents.get(0);
			if(definitionToInstanceNodes.containsKey(parent)){
				definitionToInstanceNodes.get(parent).add(definitionToInstanceNodes.get(this));
			}else{
				Node newParent = new Node();
				definitionToInstanceNodes.put(parent, newParent);
				parent.recursivelyMapParents(definitionToInstanceNodes);
			}
			for(int j=0;j<3;j++){
				Node child=parent.children.get(j);
				if(definitionToInstanceNodes.containsKey(child)){
					definitionToInstanceNodes.get(parent).add(definitionToInstanceNodes.get(child));
				}else{
					Node newChild = new Node();
					definitionToInstanceNodes.get(parent).add(newChild);
					definitionToInstanceNodes.put(child, newChild);
				}
			}
		}else{
			for(Node parent:this.parents){//map parent nodes //think don't need to map children//TODO:recursive
				if(definitionToInstanceNodes.containsKey(parent)){
					definitionToInstanceNodes.get(parent).add(definitionToInstanceNodes.get(this));
				}else{
					Node newParent = new Node();
					newParent.add(definitionToInstanceNodes.get(this));
					definitionToInstanceNodes.put(parent, newParent);
					parent.recursivelyMapParents(definitionToInstanceNodes);
				}
			}
		}
	}
}
