/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.Stack;

import utils.FixedBitSet;
//Each node may have 1 or multiple parents, if a node has 1 parent it's a subnode of this one parent, if a node has multiple parents it's a supernode of these
//Each node may have supernode OR subnode children but not both
//When a node has children has 3 children subnodes, the leftmost represents the leftmost bit, the rightmost the rightmost bit and the middle one the rest
public class Node {
	public ArrayList<Node> parents;//ArrayList, since there can be repetition
	public ArrayList<Node> children;//TODO:LinkedHashSet for order without repetition //needed?//size min 3?
	public Instance outOfInstance;
	public Definition definition;
	//DEBUGGING ONLY
	public int idForDefinition;//id of node for the definition where it's used
	//END OF DEBUGGING ONLY
	
	public Node() { 
		this.parents = new ArrayList<Node>();
		this.children = new ArrayList<Node>();
	}
	public Node add(Node node) {//add subnode to supernode		
		if(!node.children.isEmpty()&&this.parents.size()==1&&(this.parents.get(0).children.get(0)==this||this.parents.get(0).children.get(this.parents.get(0).children.size()-1)==this)){
			System.out.print("Node can't be divided");
		}else{
			node.parents.add(this);
			this.children.add(node);
			if(this.definition!=null){
				this.definition.add(node);
			}
			if(node.definition!=null){
				node.definition.add(this);
			}
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
//	public void eval(HashMap<Node, FixedBitSet> valueMap, Queue<Node> nodesToExpand, Stack<Instance> instancesToExpand) {//FIXME
//		if(!valueMap.containsKey(this)){//Non evaluated node
//			if(!this.parents.isEmpty()){
//				for (Node parent : this.parents) {
//					parent.eval(valueMap, nodesToExpand, instancesToExpand);
//				}
//				if(this.parents.size()==1){
//					if(valueMap.containsKey(this.parents.get(0))){
//						if(valueMap.get(this.parents.get(0)).length()==0){
//							valueMap.put(this.parents.get(0).children.get(0),new FixedBitSet());
//							valueMap.put(this.parents.get(0).children.get(1),new FixedBitSet());
//							valueMap.put(this.parents.get(0).children.get(2),new FixedBitSet());
//						}else if(valueMap.get(this.parents.get(0)).length()==1){
//							if(!this.parents.get(0).children.get(2).children.isEmpty()&&this.parents.get(0).children.get(1).children.get(0)==this.parents.get(0).children.get(2).children.get(0)){
//								valueMap.put(this.parents.get(0).children.get(0),valueMap.get(this.parents.get(0)).get(0,0));
//								valueMap.put(this.parents.get(0).children.get(1),new FixedBitSet());
//								valueMap.put(this.parents.get(0).children.get(2),new FixedBitSet());
//							}else{
//								valueMap.put(this.parents.get(0).children.get(0),new FixedBitSet());
//								valueMap.put(this.parents.get(0).children.get(1),new FixedBitSet());
//								valueMap.put(this.parents.get(0).children.get(2),valueMap.get(this.parents.get(0)).get(0,0));//Always left recursion TODO: check if there's a better way
//							}
//						}else{
//							for(int i=0;i<this.parents.get(0).children.size();i++){
//								if(i<this.parents.get(0).children.size()/2){
//									valueMap.put(this.parents.get(0).children.get(i), valueMap.get(this.parents.get(0)).get(i,i));
//								}else if(valueMap.get(this.parents.get(0)).length()==2){
//									valueMap.put(this.parents.get(0).children.get(1),new FixedBitSet());
//									valueMap.put(this.parents.get(0).children.get(2),valueMap.get(this.parents.get(0)).get(1,1));
//							    }else if(i>(this.parents.get(0).children.size())/2){
//							    	valueMap.put(this.parents.get(0).children.get(i), valueMap.get(this.parents.get(0)).get(valueMap.get(this.parents.get(0)).length()-this.parents.get(0).children.size()/2-2+i,valueMap.get(this.parents.get(0)).length()-this.parents.get(0).children.size()/2-2+i));
//							    }else if(i==this.parents.get(0).children.size()/2){
//							    	valueMap.put(this.parents.get(0).children.get(i),valueMap.get(this.parents.get(0)).get(i,valueMap.get(this.parents.get(0)).length()-i-1));
//							    }
//							}
//						}
//					}
//				}else{
//					FixedBitSet fixedBitSet = new FixedBitSet();
//					boolean parents=false;
//					for(Node parent:this.parents){
//						if(valueMap.containsKey(parent)){
//							fixedBitSet.concat(valueMap.get(parent));
//							parents=true;
//						}
//					}
//					if(parents)valueMap.put(this, fixedBitSet);
//				}
//			}else{
//				if(this.outOfInstance!=null){
//					if(this.definition==this.outOfInstance.definition){//recursive
//						if(instancesToExpand.contains(this.outOfInstance)){
//							nodesToExpand.remove(this.outOfInstance);
//							this.outOfInstance.eval(valueMap, nodesToExpand, instancesToExpand);							
//						}else{
//							nodesToExpand.add(this.outOfInstance);
//						}
//					}else{
//						this.outOfInstance.eval(valueMap, nodesToExpand, instancesToExpand);
//					}
//				}
//			}
//		}
//	}
	public void mapInChildren(HashMap<Node, NandNode> nodeToNand, NandForest nandForest,ArrayList<Node> nandToNodeIn) {		
		int subnodes=0;
		for(Node child:this.children){
			if(child.parents.size()==1){//subnode  
				subnodes++;
				child.mapInChildren(nodeToNand, nandForest, nandToNodeIn);	
			}
		}
		if(subnodes==0){
			NandNode nandNode;
			if(nandToNodeIn.contains(this)){
				nandNode=nodeToNand.get(this);
			}else{
				nandNode = nandForest.addIn();
				nandToNodeIn.add(this);
			}
			nodeToNand.put(this, nandNode);
		}
	}
	public void mapOutParents(HashMap<Node, NandNode> nodeToNand,
			NandForest nandForest, ArrayList<Node> nandToNodeOut) {
		ArrayList<NandNode> nandNodes = new ArrayList<NandNode> ();
		NandNode nandNode;
		if(nodeToNand.containsKey(this)){
			nandNode = nandForest.setOut(nodeToNand.get(this));
			nandToNodeOut.add(this);
			nandNodes.add(nandNode);
		}else if(this.outOfInstance!=null){
			nandNode = nandForest.setOut(this.toNands(nodeToNand,nandForest));
			nandToNodeOut.add(this);
			nandNodes.add(nandNode);
		}else{
			for(Node parent:this.parents){
				parent.mapOutParents(nodeToNand, nandForest, nandToNodeOut);
			}
		}
	}
	public void splitChildren() {
		//split in 3 children/subnodes
		if(this.children.size()==0){
				Node leftNode = new Node();
				Node centerNode = new Node();
				Node rightNode = new Node();
				this.add(leftNode);
				this.add(centerNode);
				this.add(rightNode);
		}else if(!(this.children.size()==3&&this.children.get(0).parents.size()==1)){//this.children.size()>0
				if(this.children.get(0).parents.size()!=1){//if not already split then splice
					ArrayList<Node> removedChildren = new ArrayList<Node>();
					HashMap<Node,Integer> insertNode = new HashMap<Node,Integer>();
					for(Node child:this.children){
						insertNode.put(child,child.parents.indexOf(this));
					}
					removedChildren.addAll(this.children);
					this.children.clear();
					Node leftNode = new Node();
					Node centerNode = new Node();
					Node rightNode = new Node();
					this.add(leftNode);
					this.add(centerNode);
					this.add(rightNode);
					for(Node child:removedChildren){
						leftNode.children.add(child);
						centerNode.children.add(child);
						rightNode.children.add(child);
						child.parents.remove(insertNode.get(child).intValue());//Needs to force int since remove() is overloaded (remove(int) and remove(Object)
						child.parents.add(insertNode.get(child), rightNode);
						child.parents.add(insertNode.get(child), centerNode);
						child.parents.add(insertNode.get(child), leftNode);
					}
				}
			}
	}
	public void childrenFission() {
		//if out of nand has subnodes, separate in multiple nands
		if(this.outOfInstance!=null){
			this.outOfInstance.in.get(0).childrenFission();
			this.outOfInstance.in.get(1).childrenFission();
			if(!this.outOfInstance.in.get(0).children.isEmpty()&&this.outOfInstance.in.get(0).children.get(0).parents.size()==1 
					||!this.outOfInstance.in.get(1).children.isEmpty()&&this.outOfInstance.in.get(1).children.get(0).parents.size()==1
					||!this.outOfInstance.out.get(0).children.isEmpty()&&this.outOfInstance.out.get(0).children.get(0).parents.size()==1){
				Node parentLeft=this.outOfInstance.in.get(0);
				Node parentRight=this.outOfInstance.in.get(1);
				ArrayList<Node> leftArray = new ArrayList<Node>();
				ArrayList<Node> rightArray = new ArrayList<Node>();
				parentLeft.splitChildren(leftArray);
				parentRight.splitChildren(rightArray);
				this.outOfInstance.out.get(0).splitChildren();
				for(int i=0; i<3;i++){
					Node[] nodes={leftArray.get(i),rightArray.get(i),this.outOfInstance.out.get(0).children.get(i)};
					this.definition.add(this.outOfInstance.definition, nodes);
				}
				this.definition.instances.remove(this.outOfInstance);
				this.outOfInstance.in.get(0).childrenFission();
				this.outOfInstance.in.get(1).childrenFission();
				this.outOfInstance=null;
			}
		}
		ArrayList<Node> parentNodes = new ArrayList<Node>();
		parentNodes.addAll(this.parents);
		for(Node parent:parentNodes){
			parent.childrenFission();
		}
	}
	void splitChildren(ArrayList<Node> childArray) {
		if(this.parents.size()>1){
			Node leftParent=this.parents.get(0);
			Node rightParent=this.parents.get(this.parents.size()-1);
			Node newMid= new Node();
			if(leftParent.parents.size()==1&&(leftParent.parents.get(0).children.indexOf(leftParent)==0||leftParent.parents.get(0).children.indexOf(leftParent)==2)){
				//if node is not divisible
				childArray.add(leftParent);
			}else{
				ArrayList<Node> leftArray = new ArrayList<Node>();
				leftParent.splitChildren(leftArray);
				childArray.add(leftArray.get(0));
				leftArray.get(1).add(newMid);
				leftArray.get(2).add(newMid);
			}
			childArray.add(newMid);
			for(Node node:this.parents.subList(1, this.parents.size()-1)){
				node.add(newMid);
			}
			if(rightParent.parents.size()==1&&(rightParent.parents.get(0).children.indexOf(rightParent)==0||rightParent.parents.get(0).children.indexOf(rightParent)==2)){
				//if node is not divisible
				childArray.add(rightParent);
			}else{
				ArrayList<Node> rightArray = new ArrayList<Node>();
				rightParent.splitChildren(rightArray);
				rightArray.get(0).add(newMid);
				rightArray.get(1).add(newMid);
				childArray.add(rightArray.get(2));
			}
		}else{
			//split in 3 children/subnodes
			if(this.children.size()==0){
				Node leftNode = new Node();
				Node midNode = new Node();
				Node rightNode = new Node();
				childArray.add(leftNode);
				childArray.add(midNode);
				childArray.add(rightNode);
				this.add(leftNode);
				this.add(midNode);
				this.add(rightNode);
			}else{//this.children.size()>0
				if(this.children.get(0).parents.size()==1){
					Node leftNode=this.children.get(0);
					Node midNode=this.children.get(1);
					Node rightNode=this.children.get(2);
					childArray.add(leftNode);
					childArray.add(midNode);
					childArray.add(rightNode);
				}else{//if not already split then SPLICE
					Node leftNode = new Node();
					Node midNode = new Node();
					Node rightNode = new Node();
					ArrayList<Node> removedChildren = new ArrayList<Node>();
					removedChildren.addAll(this.children);
					HashMap<Node,Integer> insertNode = new HashMap<Node,Integer>();
					for(Node child:this.children){
						insertNode.put(child,child.parents.indexOf(this));
					}
					this.children.clear();
					this.add(leftNode);
					this.add(midNode);
					this.add(rightNode);
					childArray.add(leftNode);
					childArray.add(midNode);
					childArray.add(rightNode);
					for(Node child:removedChildren){
						leftNode.children.add(child);
						midNode.children.add(child);
						rightNode.children.add(child);
						child.parents.remove(insertNode.get(child).intValue());//Needs to force int since remove() is overloaded (remove(int) and remove(Object)
						child.parents.add(insertNode.get(child), rightNode);
						child.parents.add(insertNode.get(child), midNode);
						child.parents.add(insertNode.get(child), leftNode);
					}
				}
			}
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
			if(in0.parents.size()!=in1.parents.size()){//can happen?
				ArrayList<Node> leftArray = new ArrayList<Node>();
				ArrayList<Node> rightArray = new ArrayList<Node>();
				in0.splitChildren(leftArray);
				in1.splitChildren(rightArray);
				this.definition.instances.remove(this.outOfInstance);
				for(int i=0;i<3;i++){//should be recursive into parents
					Node newNode = new Node();
					Node[] nodes={leftArray.get(i),rightArray.get(i),newNode};
					this.definition.add(this.outOfInstance.definition, nodes);
					newNode.add(this);
	//				newNode.nandParentFission();
				}
				this.outOfInstance=null;
				this.parents.get(1).nandParentFission();
			}else{
				this.definition.instances.remove(this.outOfInstance);
				for(int i=0;i<in0.parents.size();i++){//should be recursive into parents
					Node newNode = new Node();
					Node[] nodes={in0.parents.get(i),in1.parents.get(i),newNode};
					this.definition.add(this.outOfInstance.definition, nodes);
//					this.parents.clear();
					newNode.add(this);
					newNode.nandParentFission();
				}
				this.outOfInstance=null;
			}
		}
	}
	public void recursivelyMapParents(HashMap<Node, Node> definitionToInstanceNodes) {
		if(this.parents.size()==1){
			Node parent=this.parents.get(0);
			if(!definitionToInstanceNodes.containsKey(parent)){
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
			for(Node parent:this.parents){//map parent nodes //think don't need to map children
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
	public void flattenParents() {
		if(this.parents.size()==1){
			this.parents.get(0).flattenParents();
		}else{
			ArrayList<Node> parents = new ArrayList<Node>();
			parents.addAll(this.parents);
			for(Node parent:parents){
				if(parent.parents.size()>1){
					parent.flattenParents();
					int parentIndex=this.parents.indexOf(parent);
					for(Node parentParent:parent.parents){
						Collections.replaceAll(parentParent.children, parent,this);
					}
					this.parents.remove(parentIndex);
					this.parents.addAll(parentIndex, parent.parents);
				}
			}
		}
		if(this.outOfInstance!=null){//out of nand
			this.outOfInstance.in.get(0).flattenParents();
			this.outOfInstance.in.get(1).flattenParents();
		}
	}
	public void nodeFussion() {
		for(int i=0;i<this.parents.size();i++){
			if(this.parents.size()-i>=3){//posible subnodes(indexes) of the same node
				if(this.parents.get(i).parents.size()==1){
					Node grandfather=this.parents.get(i).parents.get(0);
					if(grandfather.children.get(0)==this.parents.get(i)){
						if(grandfather.children.get(1)==this.parents.get(i+1)){
							if(grandfather.children.get(2)==this.parents.get(i+2)){
								this.parents.remove(i);
								this.parents.remove(i);
								this.parents.remove(i);
//								grandfather.children.add(this);//error, cant do this because we don't know if the subnodes are used//a node can't have both subnode and supernode children
								this.parents.add(i,grandfather);
							}
						}
						
					}
				}
			}
		}
		for(Node parent:this.parents){
			parent.nodeFussion();
		}
		if(this.outOfInstance!=null){//out of nand
			this.outOfInstance.in.get(0).nodeFussion();
			this.outOfInstance.in.get(1).nodeFussion();
		}
		
	}
	public void carryNodeIndexes(HashSet<Node> inNodes, HashMap<Node, ArrayList<Instance>> in0OfInstances, HashMap<Node, ArrayList<Instance>> in1OfInstances) {
		if(!inNodes.contains(this)){
			for(Node parent:this.parents){
				parent.carryNodeIndexes(inNodes, in0OfInstances, in1OfInstances);
			}
			if(this.outOfInstance!=null){//out of nand
				this.put(in0OfInstances,this.outOfInstance.in.get(0),this.outOfInstance);
				this.put(in1OfInstances,this.outOfInstance.in.get(1),this.outOfInstance);
				this.outOfInstance.in.get(0).carryNodeIndexes(inNodes, in0OfInstances, in1OfInstances);
				this.outOfInstance.in.get(1).carryNodeIndexes(inNodes, in0OfInstances, in1OfInstances);
				int index=-1;
				if(this.outOfInstance.in.get(0).parents.size()==1&&this.outOfInstance.in.get(1)!=this.outOfInstance.in.get(0).parents.get(0)){
					index=this.outOfInstance.in.get(0).parents.get(0).children.indexOf(this.outOfInstance.in.get(0));
					if(this.outOfInstance.in.get(1).parents.isEmpty()){
						Node supernode = new Node();
						this.definition.add(supernode);
						supernode.splitChildren();
						this.outOfInstance.in.get(1).parents.add(supernode);
						supernode.children.set(index, this.outOfInstance.in.get(1));
					}
				}
				if(this.outOfInstance.in.get(1).parents.size()==1&&this.outOfInstance.in.get(0)!=this.outOfInstance.in.get(1).parents.get(0)){
					index=this.outOfInstance.in.get(1).parents.get(0).children.indexOf(this.outOfInstance.in.get(1));
					if(this.outOfInstance.in.get(0).parents.isEmpty()&&this.outOfInstance.in.get(0)!=this.outOfInstance.in.get(1).parents.get(0)){
						Node supernode = new Node();
						this.definition.add(supernode);
						supernode.splitChildren();
						this.outOfInstance.in.get(0).parents.add(supernode);
						supernode.children.set(index, this.outOfInstance.in.get(0));
					}
				}
				if(index>=0){//this.outOfInstance.in.get(0).parents.size()==1&&this.outOfInstance.in.get(1).parents.size()==1
					Node supernode = new Node();
					supernode.splitChildren();
					this.parents.add(supernode);
					supernode.children.set(index, this);
					Node[] nodes={this.outOfInstance.in.get(0).parents.get(0),this.outOfInstance.in.get(1).parents.get(0),supernode};
					ArrayList<Instance> instances = new ArrayList<Instance>();
					if(in0OfInstances.containsKey(nodes[0])&&in1OfInstances.containsKey(nodes[1])){
						instances.addAll(in0OfInstances.get(nodes[0]));
						instances.retainAll(in1OfInstances.get(nodes[1]));
					}
					if(instances.isEmpty()){
						Instance newInstance=this.definition.add(this.outOfInstance.definition, nodes);
						this.put(in0OfInstances,nodes[0],newInstance);
						this.put(in1OfInstances,nodes[1],newInstance);
					}else{
						Instance instance=instances.get(0);
						instance.out.get(0).children.set(index,this);
						this.parents.clear();
						this.parents.add(instance.out.get(0));
					}
					this.definition.instances.remove(this.outOfInstance);
					this.outOfInstance=null;
					
				}
			}
		}
		
	}
	private void put(HashMap<Node, ArrayList<Instance>> inOfInstances,
			Node node, Instance instance) {
		if(inOfInstances.containsKey(node)){
			ArrayList<Instance> instances=inOfInstances.get(node);
			instances.add(instance);
		}else{
			ArrayList<Instance> newArray = new ArrayList<Instance>();
			newArray.add(instance);
			inOfInstances.put(node, newArray);
		}
		
	}
	public void mapSupernodeParents(HashSet<Node> supernodeParents) {
		if(this.parents.size()==1){
			this.parents.get(0).mapSupernodeParents(supernodeParents);
		}else{
			supernodeParents.add(this);
			for(Node parent:this.parents){
				parent.mapSupernodeParents(supernodeParents);
			}	
		}
	}
	public void mapChildren(HashSet<Node> inOutNodes) {
		inOutNodes.add(this);
		for(Node child:this.children){
			if(child.parents.size()==1){
				child.mapChildren(inOutNodes);
			}
		}
		
	}
	public Instance findRootInstance() {
		Instance instance;
		if(this.outOfInstance!=null){
			if(this.outOfInstance.definition==this.definition){//ensure root instance is not a recursive one
				instance=this.outOfInstance.in.get(0).findRootInstance();
			}else{
				instance=this.outOfInstance;
			}
		}else{
			if(this.parents.isEmpty()){
				instance=null;
			}else{
				instance=this.parents.get(0).findRootInstance();
			}
		}
		return instance;
	}
	public void splice(Node childMid) {
		if(childMid.children.size()==3&&childMid.children.get(0).parents.size()==1){
			this.splice(childMid.children.get(0));
			this.splice(childMid.children.get(1));
			this.splice(childMid.children.get(2));
		}else{
			childMid.add(this);
		}
	}
	public void addSubnodes(Node node) {
		node.children.get(0).add(this);
		if(!node.children.get(1).children.isEmpty()&&node.children.get(1).children.get(0).parents.size()==1){
			this.addSubnodes(node.children.get(1));
		}
		node.children.get(2).add(this);
	}
	public Node mapLeft(ArrayList<Node> midArray) {
//		if(this.parents.size()>1){
//			this.mapLeft(leftArray,midArray);
//			for(int i=1;i<this.parents.size();i++){
//				midArray.add(this.parents.get(i));
//			}
//		}else{
			if(this.parents.size()==1&&(this.parents.get(0).children.indexOf(this)==0||this.parents.get(0).children.indexOf(this)==2)){
				//if node is not divisible
				return(this);
			}else{
				ArrayList<Node> nodeArray = new ArrayList<Node>();
				this.splitChildren(nodeArray);
				midArray.addAll(nodeArray.subList(1, nodeArray.size()));
				return(nodeArray.get(0));
				
//				if(!childrenArray.get(1).children.isEmpty()&&childrenArray.get(1).children.get(0).parents.size()==1){//node has subnodes
//					midArray.add(childrenArray.get(1));
//				}else{
//					childrenArray.get(1).add(newNode);
//				}
//				childrenArray.get(2).add(newNode);
			}
//		}
	}
	public Node mapRight(ArrayList<Node> midArray) {
		if(this.parents.size()==1&&(this.parents.get(0).children.indexOf(this)==0||this.parents.get(0).children.indexOf(this)==2)){
			//if node is not divisible
			return(this);
		}else{
			ArrayList<Node> nodeArray = new ArrayList<Node>();
			this.splitChildren(nodeArray);
			midArray.addAll(nodeArray.subList(0, nodeArray.size()-1));
			return(nodeArray.get(nodeArray.size()-1));
		}
		
	}
	public Node supernodeParent() {
		if(this.parents.size()==1){
			return this.parents.get(0).supernodeParent();
		}else{
			return this;
		}
	}
	public void nodeMapFission(Definition definition,
			HashSet<Node> originalNodes) {
		for(Node parent:this.parents){
			if(!originalNodes.contains(parent)){
				originalNodes.add(parent);
				parent.nodeMapFission(definition,originalNodes);
			}
		}
		for(Node child:this.children){
			if(!originalNodes.contains(child)){
				originalNodes.add(child);
			}
			child.nodeMapFission(definition,originalNodes);
		}
		
	}
	public void findInsMapping(HashSet<Node> inNodes,
			HashMap<Node, NandNode> nodeToNand, NandForest nandForest,
			ArrayList<Node> nandToNodeIn, HashSet<Node> inOutNodes,
			int addedInNodes, ArrayList<NandNode> originalDefinitionNandIn,
			ArrayList<NandNode> originalAddedDefinitionNandIn) {
		if(inNodes.contains(this)){
			if(this.definition.in.indexOf(this)<this.definition.in.size()-addedInNodes){
				this.mapInChildrenMapping(nodeToNand, nandForest, nandToNodeIn, inOutNodes, originalDefinitionNandIn);	
			}else{
				this.mapInChildrenMapping(nodeToNand, nandForest, nandToNodeIn, inOutNodes, originalAddedDefinitionNandIn);	
			}
		}else{
			for(Node parent:this.parents){
				parent.findInsMapping(inNodes, nodeToNand, nandForest, nandToNodeIn, inOutNodes, addedInNodes, originalDefinitionNandIn, originalAddedDefinitionNandIn);	
			}
			if(this.outOfInstance!=null){
				this.outOfInstance.in.get(0).findInsMapping(inNodes, nodeToNand, nandForest, nandToNodeIn, inOutNodes, addedInNodes, originalDefinitionNandIn, originalAddedDefinitionNandIn);
				this.outOfInstance.in.get(1).findInsMapping(inNodes, nodeToNand, nandForest, nandToNodeIn, inOutNodes, addedInNodes, originalDefinitionNandIn, originalAddedDefinitionNandIn);
			}
		}
	
}
	void mapInChildrenMapping(HashMap<Node, NandNode> nodeToNand,
			NandForest nandForest, ArrayList<Node> nandToNodeIn,
			HashSet<Node> inOutNodes, ArrayList<NandNode> originalDefinitionNandIn) {
		//Only maps nodes that are used in the definition		
				inOutNodes.add(this);//keep track of nodes previous to the nodes mapped to NandForest in order to not erase them
				if(this.parents.size()==1){
					inOutNodes.add(this.parents.get(0));
					inOutNodes.add(this.parents.get(0).children.get(0));
					inOutNodes.add(this.parents.get(0).children.get(1));
					inOutNodes.add(this.parents.get(0).children.get(2));
				}
				int subnodes=0;
				for(Node child:this.children){
					if(child.parents.size()==1){//subnode  
						subnodes++;
						child.mapInChildrenMapping(nodeToNand, nandForest, nandToNodeIn, inOutNodes, originalDefinitionNandIn);	
					}
				}
				if(subnodes==0){
					NandNode nandNode;
					if(nandToNodeIn.contains(this)){
						nandNode=nodeToNand.get(this);
					}else{
						nandNode = nandForest.addIn();
						nandToNodeIn.add(this);
						originalDefinitionNandIn.add(nandNode);
					}
					nodeToNand.put(this, nandNode);
				}
		
	}
	public void mapOutParentsMapping(HashMap<Node, NandNode> nodeToNand,
			NandForest nandForest, ArrayList<Node> nandToNodeOut,
			HashSet<Node> inOutNodes,
			ArrayList<NandNode> originalDefinitionNandOut) {
		ArrayList<NandNode> nandNodes = new ArrayList<NandNode> ();
		inOutNodes.add(this);
		if(this.parents.size()==1){
			inOutNodes.add(this.parents.get(0));
			inOutNodes.add(this.parents.get(0).children.get(0));
			inOutNodes.add(this.parents.get(0).children.get(1));
			inOutNodes.add(this.parents.get(0).children.get(2));
		}
		if(this.outOfInstance!=null){
			NandNode nandNode;
			if(nandToNodeOut.contains(this)){
				nandNode = nodeToNand.get(this);
			}else{
				nandNode = nandForest.setOut(this.toNands(nodeToNand,nandForest));
				nandToNodeOut.add(this);
				originalDefinitionNandOut.add(nandNode);
			}
			nandNodes.add(nandNode);
		}else{
			for(Node parent:this.parents){
				parent.mapOutParentsMapping(nodeToNand, nandForest, nandToNodeOut, inOutNodes, originalDefinitionNandOut);
			}
		}
		
	}
	public void updateDefinition(Definition definition) {
		if(!definition.nodes.contains(this)){
			this.idForDefinition=definition.maxNode;//debugging only
			definition.maxNode++;
			this.definition=definition;
			definition.nodes.add(this);
			if(this.outOfInstance!=null){
				this.outOfInstance.updateDefinition(definition);
			}
			for(Node parent:this.parents){
				parent.updateDefinition(definition);
			}
		}
	}
	public void breakSubnodes() {
		if(this.outOfInstance!=null){
			if(this.parents.size()==1){
				this.parents.clear();
			}
			this.outOfInstance.in.get(0).breakSubnodes();
			this.outOfInstance.in.get(1).breakSubnodes();
		}
		for(Node parent:this.parents){
			parent.breakSubnodes();
		}
	}
	public void mapIns(Definition definition) {
		if(!definition.nodes.contains(this)){
			this.idForDefinition=definition.maxNode;//debugging only
			definition.maxNode++;
			this.definition=definition;
			definition.nodes.add(this);
			for(Node parent:this.parents){
				parent.mapIns(definition);
			}
		}
	}
	public void eval(HashMap<Node, FixedBitSet> valueMap,
			Queue<Node> nodesToExpand, Stack<Instance> instancesToExpand) {
		if(!valueMap.containsKey(this)){//Non evaluated node
			if(!this.parents.isEmpty()){//deal with subnodes and supernodes
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
					}else{
						nodesToExpand.add(this.parents.get(0));
						nodesToExpand.add(this);
					}
				}else{
					FixedBitSet fixedBitSet = new FixedBitSet();
					boolean allExpanded=true;
					for (Node parent : this.parents) {
						if(valueMap.containsKey(parent)){
							fixedBitSet.concat(valueMap.get(parent));
						}else{
							nodesToExpand.add(parent);
							allExpanded=false;
						}
					}
					if(allExpanded){
						valueMap.put(this, fixedBitSet);
					}else{
						nodesToExpand.add(this);
					}
				}
			}else{
				if(this.outOfInstance!=null){
					if(this.outOfInstance.definition.name=="nand"){
						//lazy evaluation?
						if(valueMap.containsKey(this.outOfInstance.in.get(0))&&valueMap.containsKey(this.outOfInstance.in.get(1))){
							valueMap.put(this.outOfInstance.out.get(0), valueMap.get(this.outOfInstance.in.get(0)).nand(valueMap.get(this.outOfInstance.in.get(1))));
						}else{
							nodesToExpand.add(this.outOfInstance.in.get(0));
							nodesToExpand.add(this.outOfInstance.in.get(1));
							nodesToExpand.add(this.outOfInstance.out.get(0));
						}
						
					}else{
						for (Node nodeIn : this.outOfInstance.in) {
							nodesToExpand.add(nodeIn);
						}
						instancesToExpand.add(this.outOfInstance);
					}
				}
			}
		}
		
	}
}
