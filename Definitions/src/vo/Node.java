/*******************************************************************************
 * Copyright (c) 2015 Rub�n Alejandro Escart�n Aparicio.
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
	public void setInSize(HashMap<Node, Integer> inNodeSize) {
		if(!this.parents.isEmpty()){
			for(Node parent: this.parents){
				parent.setInSize(inNodeSize);
			}	
		}else{
			if(this.outOfInstance!=null){//out node of instance
				if(this.outOfInstance.definition.name=="nand"){
					this.outOfInstance.in.get(0).setInSize(inNodeSize);
					if(this.outOfInstance.in.get(0)!=this.outOfInstance.in.get(1)){//TODO: maybe other instances also need this fix
						this.outOfInstance.in.get(1).setInSize(inNodeSize);
					}
				}else{//definition different from nand
					HashMap<Node, Integer> instanceNodeSize = new HashMap<Node, Integer>();
					this.outOfInstance.definition.out.get(this.outOfInstance.out.indexOf(this)).setInSize(instanceNodeSize);
					for(int i=0;i<this.outOfInstance.in.size();i++){
						if(instanceNodeSize.containsKey(this.outOfInstance.definition.in.get(i))){
							this.outOfInstance.in.get(i).setInSize(inNodeSize);
						}
					}
				}
			}else{//in node
				if(inNodeSize.containsKey(this)){
					inNodeSize.put(this, inNodeSize.get(this)+1);
				}else{
					inNodeSize.put(this, 1);
				}
			}
		}
	}
//	public void mapOutNode(HashMap<Node, ArrayList<NandNode>> nodeToNands,
//			HashMap<NandNode, Node> nandToNodeOut) {
//		if(nodeToNands.get(this).size()==1){
//			nandToNodeOut.put(nodeToNands.get(this).get(0), this);
//		}else{
//			if(this.subnodes.isEmpty()){
//				for (Node node:this.supernodes) {
//					node.mapOutNode(nodeToNands, nandToNodeOut);
//				}
//			}else{
//				for (Node node:this.subnodes) {
//					node.mapOutNode(nodeToNands, nandToNodeOut);
//				}
//			}
//		}
//		
//	}
//	public void copy(Node node) {
//		this.parents=node.parents;
//		this.children=node.children;
//		this.outOfInstance=node.outOfInstance;
//		this.inOfInstances=node.inOfInstances;
//		this.idForDefinition=node.idForDefinition;
//		
//	}
//	public void copy(Node node,HashMap<Node,Node> defToTempNodes) {
//		this.idForDefinition=node.idForDefinition;
//		for(Node copiedSubnode:node.subnodes){
//			if(defToTempNodes.containsKey(copiedSubnode)){
//				this.add(defToTempNodes.get(copiedSubnode));
//			}else{
//				Node subnode = new Node();
//				defToTempNodes.put(copiedSubnode, subnode);
//				this.add(subnode);
//			}
//		}
//	}
//	public void fusion(Definition definition,HashSet<Node> nodes) {
//		//fusion of subnodes in a definition
//		if(!nodes.contains(this)){
//			nodes.add(this);
//			if(!this.subnodes.isEmpty()){
//				//for all the subnodes that are out of nand of same supernode in1 and supernode in2
//				//fuse node(out)
//				ArrayList<Node> inNodes = new ArrayList<Node> ();
//				ArrayList<Node> outNodes = new ArrayList<Node> ();
//				boolean fuse=true;
//				int i=1;
//				while(fuse&&i<this.subnodes.size()){//all the subnodes must be of the same definition
//					if(this.subnodes.get(0).outOfInstance==null||this.subnodes.get(i).outOfInstance==null||this.subnodes.get(i).outOfInstance.definition!=this.subnodes.get(0).outOfInstance.definition){
//						fuse=false;
//					}
//					i++;
//				}
//				i=1;
//				while(fuse&&i<this.subnodes.size()){//all the instance inss must have the same supernode each
//					int j=1;
//					while(fuse&&j<this.subnodes.get(0).outOfInstance.in.size()){
//						if(this.subnodes.get(0).outOfInstance.in.get(j).supernodes!=this.subnodes.get(i).outOfInstance.in.get(j).supernodes){
//							fuse=false;
//						}
//						inNodes.addAll(this.subnodes.get(0).outOfInstance.in.get(j).supernodes);
//						j++;
//					}
//					i++;
//				}
//				i=1;
//				while(fuse&&i<this.subnodes.size()){//all the instance outs must have the same supernode each
//					int j=1;
//					while(fuse&&j<this.subnodes.get(0).outOfInstance.out.size()){
//						if(this.subnodes.get(0).outOfInstance.out.get(j).supernodes!=this.subnodes.get(i).outOfInstance.out.get(j).supernodes){
//							fuse=false;
//						}
//						outNodes.addAll(this.subnodes.get(0).outOfInstance.out.get(j).supernodes);
//						j++;
//					}
//					i++;
//				}
//				if(fuse){
//					ArrayList<Node> inOutNodes = new ArrayList<Node>();
//					nodes.addAll(inNodes);
//					nodes.addAll(outNodes);
//					definition.add(this.outOfInstance.definition,inOutNodes.toArray(new Node[nodes.size()]));//add fusion instance
//					//remove all the fused instances with their nodes
//					for(Node subnode:this.subnodes){
//						Instance instance=subnode.outOfInstance;
//						definition.remove(instance);
//					}
//				}
//				//continue evaluation of superior nodes
//				for(Node node:this.subnodes){
//					if(node.outOfInstance!=null){
//						for(Node inNode:node.outOfInstance.in){
//							inNode.fusion(definition,nodes);
//						}
//					}
//				}
//			}else{//this.subnodes.isEmpty()
//				if(this.outOfInstance!=null){//if this.subnodes.isEmpty()&&this.outOfInstance==null it's always an in node
//					for(Node node:this.outOfInstance.in){//expand instance in nodes
//						node.fusion(definition, nodes);
//					}
//				}
//			}
//		}
//	}
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
//	private void chopEqual(Node node) {
//		for(int i=0;i<this.parents.size();i++){
//			Node newNode = new Node();
//			newNode.add(node);
//		}
////		for(int i=0;i<this.children.size();i++){
////			this.children.get(i).chopEqual(node.children.get(i));
////			
////		}
//		
//	}
//	private void mirror(Node node) {
//		if(node.parents.size()==3){
//			this.splitChildren();
//			for(int i=0;i<node.parents.size();i++){
//				this.children.get(i).mirror(node.parents.get(i));
//			}
//		}
//	}
	public void splitChildren() {
		if(this.children.size()!=3){
			if(this.children.size()==0){
				Node leftNode = new Node();
				Node centerNode = new Node();
				Node rightNode = new Node();
				this.add(leftNode);
				this.add(centerNode);
				this.add(rightNode);
//			}else{//this.children.size()==1
//				Node supernode =this.children.get(0);
//				this.children.clear();
//				int insertIndex=supernode.parents.indexOf(this);
//				supernode.parents.remove(insertIndex);
//				for(int i=0;i<3;i++){
//					Node newChild= new Node();
//					this.add(newChild);
//					supernode.parents.add(insertIndex+i, newChild);
//					newChild.children.add(supernode);
//					newChild.definition=supernode.definition;
//					newChild.definition.nodes.add(this);
//				}
			}
		}
		
	}
	public Node removeRedundantSubnodes() {
		Node node = this;
		if(this.parents.size()==1&&this.parents.get(0).parents.size()>1){
			if(this.parents.get(0).children.indexOf(this)==0){
				if(this.parents.get(0).parents.get(0).children.size()==3){
					node=this.parents.get(0).parents.get(0).children.get(0);
				}else if(this.parents.get(0).parents.get(0).children.size()==0){
					Node leftNode = new Node();
					Node centerNode = new Node();
					Node rightNode = new Node();
					this.parents.get(0).parents.get(0).add(leftNode);
					this.parents.get(0).parents.get(0).add(centerNode);
					this.parents.get(0).parents.get(0).add(rightNode);
					node = leftNode;
				}else if(this.parents.get(0).parents.get(0).children.size()==1){
					Node supernode =this.parents.get(0).parents.get(0).children.get(0);
					this.parents.get(0).parents.get(0).children.clear();
					int insertIndex=supernode.parents.indexOf(this.parents.get(0).parents.get(0));
					supernode.parents.remove(insertIndex);
					for(int i=0;i<3;i++){
						Node newChild= new Node();
						this.add(newChild);
						supernode.parents.add(insertIndex+i, newChild);
						newChild.children.add(supernode);
						newChild.definition=supernode.definition;
						newChild.definition.nodes.add(this);
					}
					node = this.parents.get(0).parents.get(0).children.get(0);
				}
			}
			if(this.parents.get(0).children.indexOf(this)==2){
				if(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).children.size()==3){
					node=this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).children.get(2);
				}else if(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).children.size()==0){
					Node leftNode = new Node();
					Node centerNode = new Node();
					Node rightNode = new Node();
					this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).add(leftNode);
					this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).add(centerNode);
					this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).add(rightNode);
					node = rightNode;
				}else if(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).children.size()==1){
					Node supernode =this.parents.get(this.parents.get(0).parents.size()-1).parents.get(0).children.get(0);
					this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).children.clear();
					int insertIndex=supernode.parents.indexOf(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1));
					supernode.parents.remove(insertIndex);
					for(int i=0;i<3;i++){
						Node newChild= new Node();
						this.add(newChild);
						supernode.parents.add(insertIndex+i, newChild);
						newChild.children.add(supernode);
						newChild.definition=supernode.definition;
						newChild.definition.nodes.add(this);
					}
					node = this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).children.get(2);
				}
			}
			ArrayList<Node> parents = new ArrayList<Node>();
			for(Node parent:this.parents){
				parents.add(parent.removeRedundantSubnodes());
			}
			this.parents=parents;
			if(this.outOfInstance!=null){
				ArrayList<Node> ins = new ArrayList<Node>();
				for(Node inNode:this.outOfInstance.in){
					ins.add(inNode.removeRedundantSubnodes());
				}
				this.outOfInstance.in=ins;
			}
		}
		return node;
	}
//	public void findIns(Boolean used,HashSet<Node> inNodes,
//			HashMap<Node, ArrayList<NandNode>> nodeToNands,
//			NandForest nandForest, ArrayList<Node> nandToNodeIn,
//			HashSet<Node> inOutNodes) {//Trying not to use inOfInstances
//		if(used&&this.parents.isEmpty()&&this.outOfInstance==null){
//			this.mapInChildren(nodeToNands,nandForest, nandToNodeIn,inOutNodes);
//		}else{
//			for(Node parent:this.parents){
//				parent.findIns(used,inNodes, nodeToNands, nandForest, nandToNodeIn, inOutNodes);
//			}
//			if(this.outOfInstance!=null){
//				for(Node instanceInNode:this.outOfInstance.in){
//					instanceInNode.findIns(true,inNodes, nodeToNands, nandForest, nandToNodeIn, inOutNodes);
//				}
//			}
//		}
//		
//	}
public void childrenFission() {
	for(Node parent:this.parents){
		parent.childrenFission();
	}
	if(this.outOfInstance!=null){
		this.nandBotInstanceFission();
	}else if(this.parents.size()==1){
		this.nandTopInstanceFission();
	}
}
private void nandTopInstanceFission() {
	if(this.parents.get(0).parents.size()==1){
		this.parents.get(0).nandTopInstanceFission();
	}
	if(this.parents.get(0).outOfInstance!=null){
		Node inLeft=this.parents.get(0).outOfInstance.in.get(0);
		Node inRight=this.parents.get(0).outOfInstance.in.get(1);
		Node out=this.parents.get(0);
		inLeft.inOfInstances.remove(out.outOfInstance);
		inRight.inOfInstances.remove(out.outOfInstance);
		inLeft.splitChildren();
		inRight.splitChildren();
		for(int i=0;i<3;i++){
			Node[] nodes={inLeft.children.get(i),inRight.children.get(i),out.children.get(i)};
			this.definition.add(out.outOfInstance.definition, nodes);
		}
		this.definition.remove(out.outOfInstance);
		inLeft.childrenFission();
		inRight.childrenFission();
	}
}
private void nandBotInstanceFission() {
	Node inLeft=this.outOfInstance.in.get(0);
	Node inRight=this.outOfInstance.in.get(1);
	if(inLeft.children.size()>1||inRight.children.size()>1){
		inLeft.splitChildren();
		inRight.splitChildren();
		this.splitChildren();
		for(int i=0;i<3;i++){
			Node[] nodes={inLeft.children.get(i),inRight.children.get(i),this.children.get(i)};
			this.definition.add(this.outOfInstance.definition, nodes);
		}
		inLeft.inOfInstances.remove(this.outOfInstance);
		inRight.inOfInstances.remove(this.outOfInstance);
		this.definition.remove(this.outOfInstance);
		this.outOfInstance=null;
	}else{
		this.outOfInstance.in.get(0).childrenFission();
		this.outOfInstance.in.get(1).childrenFission();
	}
	for(Node parent:this.parents){
		parent.nandBotInstanceFission();
	}
}
	//	public void childrenFission() {
//		if(this.parents.size()==1){
////			if(this.parents.get(0).parents.size()==1){//child with children (recursive)//remove redundant subnodes
//////we may need to divide node in the smallest subnodes
////			}
//			if(this.parents.get(0).outOfInstance!=null){//out of NAND definition
//				//REPLACE NAND WITH 3 NAND FOR THE CHILDREN
//				this.nandChildrenFission();
//			}else if(this.parents.get(0).parents.size()>1){//parent node has  both children and parents
////				this.parents.get(0).parents.get(0).childrenFission();//recursively remove redundant subnodes //NOT NEEDED: REDUNDANT
////				this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenFission();//recursively remove redundant subnodes
//				Node parent = this.parents.get(0);//variables to conserve references
//				Node left = parent.children.get(0);
//				Node mid = parent.children.get(1);
//				Node right = parent.children.get(2);
//				Node parentLeft = parent.parents.get(0);
//				Node parentRight = parent.parents.get(parent.parents.size()-1);
//				parentLeft.children.clear();
//				parentLeft.splitChildren();//split parents in the extremes into children
//				parentRight.children.clear();
//				parentRight.splitChildren();
//				left.parents.set(0,parentLeft);
//				parentLeft.children.set(0,left);
//				right.parents.set(0,parentRight);
//				parentRight.children.set(2,right);
//				parent.parents.remove(0);
//				parent.parents.remove(parent.parents.size()-1);
//				mid.parents.clear();
//				parentLeft.children.get(1).add(mid);
//				parentLeft.children.get(2).add(mid);
//				for(Node par:parent.parents){//remove parent
//					par.add(mid);
//				}
//				parentRight.children.get(0).add(mid);
//				parentRight.children.get(1).add(mid);
//				mid.childrenFission();
//			}
//		}else if(this.parents.size()>1){
//			for(Node parent:this.parents){
//				parent.childrenFission();
//			}
//		}else if(this.outOfInstance!=null){
//			this.outOfInstance.in.get(0).childrenFission();
//			this.outOfInstance.in.get(1).childrenFission();
//		}//else: the node is an input	
//	}
	private void nandChildrenFission() {
		//remove this instance of nand definition
		Node parent=this.parents.get(0);
		@SuppressWarnings("unused")
		Node left=parent.children.get(0);
		@SuppressWarnings("unused")
		Node mid=parent.children.get(1);
		@SuppressWarnings("unused")
		Node right=parent.children.get(2);
		Node inLeft=parent.outOfInstance.in.get(0);
		Node inRight=parent.outOfInstance.in.get(1);
		this.definition.remove(parent.outOfInstance);
		inLeft.inOfInstances.remove(parent.outOfInstance);
		inRight.inOfInstances.remove(parent.outOfInstance);
		inLeft.splitChildren();
		inRight.splitChildren();
		//add the 3 new nands that replace the removed nand
		for(int i=0;i<3;i++){
			Node[] nodes={inLeft.children.get(i),inRight.children.get(i),parent.children.get(i)};
			this.definition.add(parent.outOfInstance.definition, nodes);
			parent.children.get(i).parents.clear();
		}
		parent.outOfInstance=null;//no need to parent.children.clear() nor child.parents.remove(parent);
//		for(int i=0;i<3;i++){
//			this.parents.get(0).children.get(i).childrenFission();//only need recursion on one of the three nodes
//		}
		inLeft.children.get(1).childrenFission();//only need recursion on one of the three nodes
		inRight.children.get(1).childrenFission();//only need recursion on one of the three nodes
	}
	public void parentsFission() {
		for(Node children:this.children){//recursive
			children.parentsFission();
		}
		if(this.children.size()==1){
			if(!this.children.get(0).inOfInstances.isEmpty()){//is in of at least one instance of nand definition
				for(Instance nandInstance:this.children.get(0).inOfInstances){
					Definition nand = nandInstance.definition;
					Node in0=nandInstance.in.get(0);
					Node in1=nandInstance.in.get(1);
					Node out=nandInstance.out.get(0);
					//TODO make sure in0.children.size()==in1.children.size() by going up
					//should be RECURSIVE into parents
					if(in0.children.size()!=in1.children.size()){
						System.out.print("Error, different parentSize.");
					}else{
						this.definition.remove(nandInstance);
						in0.inOfInstances.remove(nandInstance);
						in1.inOfInstances.remove(nandInstance);
						out.outOfInstance=null;
						for(int i=0;i<in0.parents.size();i++){//should be recursive into parents
							Node newNode = new Node();
							Node[] nodes={in0.parents.get(i),in1.parents.get(i),newNode};
							this.definition.add(nand, nodes);
							newNode.add(out);
						}
						out.parents.get(0).parentsFission();
					
					}
					
				}
			}
		}
		if(!this.inOfInstances.isEmpty()){//is in of at least one instance of nand definition
			for(Instance nandInstance:this.inOfInstances){
				nandInstance.out.get(0).parentsFission();
			}
		}
		
	}
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
