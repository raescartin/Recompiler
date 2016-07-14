/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import utils.FixedBitSet;
//Each node can have 1 or multiple parents, if a node has 1 parent it's a subnode of this one parent, if a node has multiple parents it's a supernode of these
//Each node can have supernode AND subnode children
//If a node has subnode children, the leftmost represents the leftmost bit, the rightmost the rightmost bit and the middle one the rest
public class Node {
	public ArrayList<Node> parents;//ArrayList, since there can be repetition
	public ArrayList<Node> childrenSubnodes;
	public ArrayList<Node> childrenSupernodes;
	public Instance outOfInstance;
	public Definition definition;
	//DEBUGGING ONLY
	public int idForDefinition;//id of node for the definition where it's used
	//END OF DEBUGGING ONLY
	
	public Node() { 
		this.parents = new ArrayList<Node>();
		this.childrenSubnodes = new ArrayList<Node>();
		this.childrenSupernodes = new ArrayList<Node>();
	}
	public Node addChildSupernode(Node childSuperNode) {//add children supernode to node		
		childSuperNode.parents.add(this);
		this.childrenSupernodes.add(childSuperNode);
		if(this.definition!=null){
			this.definition.add(childSuperNode);
		}
		if(childSuperNode.definition!=null){
			childSuperNode.definition.add(this);
		}
		return childSuperNode;
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
				string+=this.parents.get(0).childrenSubnodes.indexOf(this);
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
	public void mapInChildren(HashMap<Node, NandNode> nodeToNand, NandForest nandForest,ArrayList<Node> nandToNodeIn) {		
		int subnodes=0;
		for(Node child:this.childrenSubnodes){
			subnodes++;
			child.mapInChildren(nodeToNand, nandForest, nandToNodeIn);
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
	public void mapInChildrenMapping(HashMap<Node, NandNode> nodeToNand, NandForest nandForest,ArrayList<Node> nandToNodeIn, HashMap<NandNode, Node> nandToNode) {		
		int subnodes=0;
		for(Node child:this.childrenSubnodes){
			subnodes++;
			child.mapInChildrenMapping(nodeToNand, nandForest, nandToNodeIn, nandToNode);
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
			nandToNode.put(nandNode,this);
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
	public void splitChildrenSubnodes() {
		//split in 3 children/subnodes
		if(this.childrenSubnodes.isEmpty()){
				Node leftNode = new Node();
				Node centerNode = new Node();
				Node rightNode = new Node();
				this.addChildSubnode(leftNode);
				this.addChildSubnode(centerNode);
				this.addChildSubnode(rightNode);
		}
	}
	public void childrenFission() {
		//if out of nand has  children subnodes, separate in multiple nands
		//TODO: check best recursion
		if(this.outOfInstance!=null){
			Node parentLeftIn=this.outOfInstance.in.get(0);
			Node parentRightIn=this.outOfInstance.in.get(1);
			if(!this.childrenSubnodes.isEmpty()){
				this.nandInsFission();
				parentLeftIn.childrenFission();
				if(parentLeftIn!=parentRightIn)parentRightIn.childrenFission();
			}else{
				parentLeftIn.childrenFission();
				if(parentLeftIn!=parentRightIn)parentRightIn.childrenFission();
				if(!parentLeftIn.childrenSubnodes.isEmpty()&&!parentRightIn.childrenSubnodes.isEmpty()){
					this.nandOutFission();
				}
			}
		}
		ArrayList<Node> parentNodes = new ArrayList<Node>();
		parentNodes.addAll(this.parents);
		for(Node parent:parentNodes){
			parent.childrenFission();
		}
	}
private void nandInsFission() {
		Node parentLeftIn=this.outOfInstance.in.get(0);
		Node parentRightIn=this.outOfInstance.in.get(1);
		ArrayList<Node> childrenSubnodes;
		childrenSubnodes=parentLeftIn.getChildrenSubnodes();
		Node parentLeftLeftChild=childrenSubnodes.get(0);
		Node parentLeftMidChild=childrenSubnodes.get(1);
		Node parentLeftRightChild=childrenSubnodes.get(2);
		childrenSubnodes=parentRightIn.getChildrenSubnodes();
		Node parentRightLeftChild=childrenSubnodes.get(0);
		Node parentRightMidChild=childrenSubnodes.get(1);
		Node parentRightRightChild=childrenSubnodes.get(2);
		Node[] nodes0={parentLeftLeftChild,parentRightLeftChild,this.childrenSubnodes.get(0)};
		this.definition.add(this.outOfInstance.definition, nodes0);
//		this.childrenSubnodes.get(0).parents.clear();// break children subnodes from now non-existant node
		Node[] nodes1={parentLeftMidChild,parentRightMidChild,this.childrenSubnodes.get(1)};
		this.definition.add(this.outOfInstance.definition, nodes1);
//		this.childrenSubnodes.get(1).parents.clear();// break children subnodes from now non-existant node
		Node[] nodes2={parentLeftRightChild,parentRightRightChild,this.childrenSubnodes.get(2)};
		this.definition.add(this.outOfInstance.definition, nodes2);
//		this.childrenSubnodes.get(2).parents.clear();// break children subnodes from now non-existant node
		this.definition.removeInstance(this.outOfInstance);
		this.outOfInstance=null;
		this.definition.nodes.remove(this);//remove node form definition, since instance plsit in subnodes
		if(!this.childrenSubnodes.get(1).childrenSubnodes.isEmpty()){
			this.childrenSubnodes.get(1).nandInsFission();
		}
	}
private void nandOutFission() {
	Node parentLeftIn=this.outOfInstance.in.get(0);
	Node parentRightIn=this.outOfInstance.in.get(1);
	Node parentLeftLeftChild=parentLeftIn.childrenSubnodes.get(0);
	Node parentLeftMidChild=parentLeftIn.childrenSubnodes.get(1);
	Node parentLeftRightChild=parentLeftIn.childrenSubnodes.get(2);
	Node parentRightLeftChild=parentRightIn.childrenSubnodes.get(0);
	Node parentRightMidChild=parentRightIn.childrenSubnodes.get(1);
	Node parentRightRightChild=parentRightIn.childrenSubnodes.get(2);
	this.splitChildrenSubnodes();
	Node[] nodes0={parentLeftLeftChild,parentRightLeftChild,this.childrenSubnodes.get(0)};
	this.definition.add(this.outOfInstance.definition, nodes0);
//	this.childrenSubnodes.get(0).parents.clear();// break children subnodes from now non-existant node
	Node[] nodes1={parentLeftMidChild,parentRightMidChild,this.childrenSubnodes.get(1)};
	this.definition.add(this.outOfInstance.definition, nodes1);
//	this.childrenSubnodes.get(1).parents.clear();// break children subnodes from now non-existant node
	Node[] nodes2={parentLeftRightChild,parentRightRightChild,this.childrenSubnodes.get(2)};
	this.definition.add(this.outOfInstance.definition, nodes2);
//	this.childrenSubnodes.get(2).parents.clear();// break children subnodes from now non-existant node
	this.definition.removeInstance(this.outOfInstance);
	this.outOfInstance=null;
	this.definition.nodes.remove(this);//remove node form definition, since instance plsit in subnodes
	if(!parentLeftIn.childrenSubnodes.get(1).childrenSubnodes.isEmpty()&&!parentRightIn.childrenSubnodes.get(1).childrenSubnodes.isEmpty()){
		this.childrenSubnodes.get(1).nandOutFission();
	}
}
private ArrayList<Node> getChildrenSubnodes() {
		ArrayList<Node> childrenSubnodes = new ArrayList<Node>();
		if(this.parents.isEmpty()){
			this.splitChildrenSubnodes();
			childrenSubnodes=this.childrenSubnodes;
		}else{
			if(this.parents.size()==1){//can't be an indivisible node
				Node parentMidNode = this.parents.get(0).getChildrenSubnodes().get(1);
				if(parentMidNode==this){
					this.splitChildrenSubnodes();
					childrenSubnodes=this.childrenSubnodes;
				}else{
					childrenSubnodes=parentMidNode.getChildrenSubnodes();
				}
			}else{
				Node leftSubnode;
				Node midSubnode;
				Node rightSubnode;
				midSubnode=new Node();
				leftSubnode=this.parents.get(0).findLeftChild(midSubnode);
				for(int i=1;i<this.parents.size()-1;i++){
					this.parents.get(i).addChildSupernode(midSubnode);
				}
				rightSubnode=this.parents.get(this.parents.size()-1).findRightChild(midSubnode);
				childrenSubnodes.add(leftSubnode);
				childrenSubnodes.add(midSubnode);
				childrenSubnodes.add(rightSubnode);
			}
		}
		return childrenSubnodes;
	}
	//	void splitChildren(ArrayList<Node> childArray) {
//		if(this.parents.size()>1){
//			Node leftParent=this.parents.get(0);
//			Node rightParent=this.parents.get(this.parents.size()-1);
//			Node newMid= new Node();
//			if(leftParent.parents.size()==1&&(leftParent.parents.get(0).children.indexOf(leftParent)==0||leftParent.parents.get(0).children.indexOf(leftParent)==2)){
//				//if node is not divisible
//				childArray.add(leftParent);
//			}else{
//				ArrayList<Node> leftArray = new ArrayList<Node>();
//				leftParent.splitChildren(leftArray);
//				childArray.add(leftArray.get(0));
//				leftArray.get(1).add(newMid);
//				leftArray.get(2).add(newMid);
//			}
//			childArray.add(newMid);
//			for(Node node:this.parents.subList(1, this.parents.size()-1)){
//				node.add(newMid);
//			}
//			if(rightParent.parents.size()==1&&(rightParent.parents.get(0).children.indexOf(rightParent)==0||rightParent.parents.get(0).children.indexOf(rightParent)==2)){
//				//if node is not divisible
//				childArray.add(rightParent);
//			}else{
//				ArrayList<Node> rightArray = new ArrayList<Node>();
//				rightParent.splitChildren(rightArray);
//				rightArray.get(0).add(newMid);
//				rightArray.get(1).add(newMid);
//				childArray.add(rightArray.get(2));
//			}
//		}else{
//			//split in 3 children/subnodes
//			if(this.children.size()==0){
//				Node leftNode = new Node();
//				Node midNode = new Node();
//				Node rightNode = new Node();
//				childArray.add(leftNode);
//				childArray.add(midNode);
//				childArray.add(rightNode);
//				this.add(leftNode);
//				this.add(midNode);
//				this.add(rightNode);
//			}else{//this.children.size()>0
//				if(this.children.get(0).parents.size()==1){
//					Node leftNode=this.children.get(0);
//					Node midNode=this.children.get(1);
//					Node rightNode=this.children.get(2);
//					childArray.add(leftNode);
//					childArray.add(midNode);
//					childArray.add(rightNode);
//				}else{//if not already split then SPLICE
//					Node leftNode = new Node();
//					Node midNode = new Node();
//					Node rightNode = new Node();
//					ArrayList<Node> removedChildren = new ArrayList<Node>();
//					removedChildren.addAll(this.children);
//					HashMap<Node,Integer> insertNode = new HashMap<Node,Integer>();
//					for(Node child:this.children){
//						insertNode.put(child,child.parents.indexOf(this));
//					}
//					this.children.clear();
//					this.add(leftNode);
//					this.add(midNode);
//					this.add(rightNode);
//					childArray.add(leftNode);
//					childArray.add(midNode);
//					childArray.add(rightNode);
//					for(Node child:removedChildren){
//						leftNode.children.add(child);
//						midNode.children.add(child);
//						rightNode.children.add(child);
//						child.parents.remove(insertNode.get(child).intValue());//Needs to force int since remove() is overloaded (remove(int) and remove(Object)
//						child.parents.add(insertNode.get(child), rightNode);
//						child.parents.add(insertNode.get(child), midNode);
//						child.parents.add(insertNode.get(child), leftNode);
//					}
//				}
//			}
//		}
//		
//	}
	public void parentsFission() {
		//if in of nand has  multiple parents, separate in multiple nands
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
			if(in0.parents.size()==in1.parents.size()){
				for(int i=0;i<in0.parents.size();i++){//should be recursive into parents
					Node newNode = new Node();
					Node[] nodes={in0.parents.get(i),in1.parents.get(i),newNode};
					this.definition.add(this.outOfInstance.definition, nodes);
//					this.parents.clear();
					newNode.addChildSupernode(this);
					newNode.nandParentFission();
				}
				this.definition.removeInstance(this.outOfInstance);
			}
		}
	}
//	public void recursivelyMapParents(HashMap<Node, Node> definitionToInstanceNodes) {
//		if(this.parents.size()==1){
//			Node parent=this.parents.get(0);
//			if(!definitionToInstanceNodes.containsKey(parent)){
//				Node newParent = new Node();
//				definitionToInstanceNodes.put(parent, newParent);
//				parent.recursivelyMapParents(definitionToInstanceNodes);
//			}
//			for(int j=0;j<3;j++){
//				Node child=parent.children.get(j);
//				if(definitionToInstanceNodes.containsKey(child)){
//					definitionToInstanceNodes.get(parent).add(definitionToInstanceNodes.get(child));
//				}else{
//					Node newChild = new Node();
//					definitionToInstanceNodes.get(parent).add(newChild);
//					definitionToInstanceNodes.put(child, newChild);
//				}
//			}
//		}else{
//			for(Node parent:this.parents){//map parent nodes //think don't need to map children
//				if(definitionToInstanceNodes.containsKey(parent)){
//					definitionToInstanceNodes.get(parent).add(definitionToInstanceNodes.get(this));
//				}else{
//					Node newParent = new Node();
//					newParent.add(definitionToInstanceNodes.get(this));
//					definitionToInstanceNodes.put(parent, newParent);
//					parent.recursivelyMapParents(definitionToInstanceNodes);
//				}
//			}
//		}
//	}
	public ArrayList<Node> flattenParents() {
		ArrayList<Node> nodes = new ArrayList<Node>();
		if(this.parents.size()==0){
			nodes.add(this);
		}else if(this.parents.size()==1){	
			this.parents.get(0).flattenParents();
			nodes.add(this);
		}else{
			for(Node parent:this.parents){
				nodes.addAll(parent.flattenParents());
			}
			this.parents.clear();
			for(Node parent:nodes){
				if(parent.childrenSupernodes.contains(this)){
					parents.add(parent);
				}else{
					parent.addChildSupernode(this);
				}
			}
		}		
//		if(this.parents.size()==1){
//			this.parents.get(0).flattenParents();
//		}else{
//			ArrayList<Node> parents = new ArrayList<Node>();
//			parents.addAll(this.parents);
//			for(Node parent:parents){
//				if(parent.parents.size()>1){
//					parent.flattenParents();
//					int parentIndex=this.parents.indexOf(parent);
//					for(Node parentParent:parent.parents){
//						Collections.replaceAll(parentParent.childrenSubnodes, parent,this);
//					}
//					this.parents.remove(parentIndex);
//					this.parents.addAll(parentIndex, parent.parents);
//				}
//			}
//		}
		if(this.outOfInstance!=null){//out of nand
			this.outOfInstance.in.get(0).flattenParents();
			this.outOfInstance.in.get(1).flattenParents();
		}
		return nodes;
	}
	public void nodeFussion() {
		for(int i=0;i<this.parents.size();i++){
			if(this.parents.size()-i>=3){//posible subnodes(indexes) of the same node
				if(this.parents.get(i).parents.size()==1){
					Node grandfather=this.parents.get(i).parents.get(0);
					if(grandfather.childrenSubnodes.get(0)==this.parents.get(i)){
						if(grandfather.childrenSubnodes.get(1)==this.parents.get(i+1)){
							if(grandfather.childrenSubnodes.get(2)==this.parents.get(i+2)){
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
	public void mapInsOfNandInstances(HashMap<Node, HashSet<Instance>> inOfInstances) {
		for(Node parent:this.parents){
			parent.mapInsOfNandInstances(inOfInstances);
		}
		if(this.outOfInstance!=null){//out of nand
			if(!inOfInstances.containsKey(this.outOfInstance.in.get(0))){
				HashSet<Instance> hashSet = new HashSet<Instance>();
				hashSet.add(this.outOfInstance);
				inOfInstances.put(this.outOfInstance.in.get(0), hashSet);
			}else{
				HashSet<Instance> hashSet = inOfInstances.get(this.outOfInstance.in.get(0));
				hashSet.add(this.outOfInstance);
			}
			if(!inOfInstances.containsKey(this.outOfInstance.in.get(1))){
				HashSet<Instance> hashSet = new HashSet<Instance>();
				hashSet.add(this.outOfInstance);
				inOfInstances.put(this.outOfInstance.in.get(1), hashSet);
			}else{
				HashSet<Instance> hashSet = inOfInstances.get(this.outOfInstance.in.get(1));
				hashSet.add(this.outOfInstance);
			}
			this.outOfInstance.in.get(0).mapInsOfNandInstances(inOfInstances);
			this.outOfInstance.in.get(1).mapInsOfNandInstances(inOfInstances);
		}
	}
	public void triFusion(HashMap<Node, HashSet<Instance>> inOfNandInstances) {
		//fusion of three nand instances where outs are childrenSubnodes of same node
		//TODO: rewrite for efficiency like bifusion
		if(!this.childrenSubnodes.isEmpty()){
			if(inOfNandInstances.containsKey(this.childrenSubnodes.get(0))&&inOfNandInstances.containsKey(this.childrenSubnodes.get(1))&&inOfNandInstances.containsKey(this.childrenSubnodes.get(2))){
				ArrayList<Instance> instances0 = new ArrayList<Instance>();
				instances0.addAll(inOfNandInstances.get(this.childrenSubnodes.get(0)));
				ArrayList<Instance> instances1 = new ArrayList<Instance>();
				instances1.addAll(inOfNandInstances.get(this.childrenSubnodes.get(1)));
				ArrayList<Instance> instances2 = new ArrayList<Instance>();
				instances2.addAll(inOfNandInstances.get(this.childrenSubnodes.get(2)));
				for(Instance instance0:instances0){
					for(Instance instance1:instances1){
						for(Instance instance2:instances2){
							if(instance0.in.get(0).parents.size()==1&&instance1.in.get(0).parents.size()==1&&instance2.in.get(0).parents.size()==1
							&&instance0.in.get(1).parents.size()==1&&instance1.in.get(1).parents.size()==1&&instance2.in.get(1).parents.size()==1
							&&instance0.in.get(0).parents.get(0)==instance1.in.get(0).parents.get(0)&&instance2.in.get(0).parents.get(0)==instance1.in.get(0).parents.get(0)
							&&instance0.in.get(1).parents.get(0)==instance1.in.get(1).parents.get(0)&&instance2.in.get(1).parents.get(0)==instance1.in.get(1).parents.get(0)
							&&instance0.in.get(0).parents.get(0).childrenSubnodes.indexOf(instance0.in.get(0))==instance0.in.get(1).parents.get(0).childrenSubnodes.indexOf(instance0.in.get(1))
							&&instance1.in.get(0).parents.get(0).childrenSubnodes.indexOf(instance1.in.get(0))==instance1.in.get(1).parents.get(0).childrenSubnodes.indexOf(instance1.in.get(1))
							&&instance2.in.get(0).parents.get(0).childrenSubnodes.indexOf(instance2.in.get(0))==instance2.in.get(1).parents.get(0).childrenSubnodes.indexOf(instance2.in.get(1))){
								Node supernode= new Node();
								Node[] nodes={instance0.in.get(0).parents.get(0),instance0.in.get(1).parents.get(0),supernode};
								Instance instance=this.definition.add(instance0.definition, nodes);
								supernode.addChildSubnode(instance0.out.get(0));
								supernode.addChildSubnode(instance1.out.get(0));
								supernode.addChildSubnode(instance2.out.get(0));
								this.definition.removeInstance(instance0);
								this.definition.removeInstance(instance1);
								this.definition.removeInstance(instance2);
								instance0.out.get(0).outOfInstance=null;
								instance1.out.get(0).outOfInstance=null;
								instance2.out.get(0).outOfInstance=null;
								inOfNandInstances.get(instance0.in.get(0)).remove(instance0);
								inOfNandInstances.get(instance0.in.get(1)).remove(instance0);
								inOfNandInstances.get(instance1.in.get(0)).remove(instance1);
								inOfNandInstances.get(instance1.in.get(1)).remove(instance1);
								inOfNandInstances.get(instance2.in.get(0)).remove(instance2);
								inOfNandInstances.get(instance2.in.get(1)).remove(instance2);
								HashSet<Instance> hashSet = new HashSet<Instance>();
								hashSet.add(instance);
								inOfNandInstances.put(instance0.in.get(0).parents.get(0), hashSet);
								inOfNandInstances.put(instance0.in.get(1).parents.get(0), hashSet);
								supernode.triFusion(inOfNandInstances);
							}
						}
					}
				}
			}
		}
		if(inOfNandInstances.containsKey(this)){//in0 of nand
			for(Instance instance:inOfNandInstances.get(this)){
				instance.out.get(0).triFusion(inOfNandInstances);
			}
		}
		for(Node child:this.childrenSubnodes){
			child.triFusion(inOfNandInstances);
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
		for(Node child:this.childrenSubnodes){
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
//	public void splice(Node childMid) {
//		if(childMid.children.size()==3&&childMid.children.get(0).parents.size()==1){
//			this.splice(childMid.children.get(0));
//			this.splice(childMid.children.get(1));
//			this.splice(childMid.children.get(2));
//		}else{
//			childMid.add(this);
//		}
//	}
//	public void addSubnodes(Node node) {
//		node.children.get(0).add(this);
//		if(!node.children.get(1).children.isEmpty()&&node.children.get(1).children.get(0).parents.size()==1){
//			this.addSubnodes(node.children.get(1));
//		}
//		node.children.get(2).add(this);
//	}
//	public Node mapLeft(ArrayList<Node> midArray) {
////		if(this.parents.size()>1){
////			this.mapLeft(leftArray,midArray);
////			for(int i=1;i<this.parents.size();i++){
////				midArray.add(this.parents.get(i));
////			}
////		}else{
//			if(this.parents.size()==1&&(this.parents.get(0).childrenSubnodes.indexOf(this)==0||this.parents.get(0).childrenSubnodes.indexOf(this)==2)){
//				//if node is not divisible
//				return(this);
//			}else{
//				ArrayList<Node> nodeArray = new ArrayList<Node>();
//				this.splitChildren(nodeArray);
//				midArray.addAll(nodeArray.subList(1, nodeArray.size()));
//				return(nodeArray.get(0));
//				
////				if(!childrenArray.get(1).children.isEmpty()&&childrenArray.get(1).children.get(0).parents.size()==1){//node has subnodes
////					midArray.add(childrenArray.get(1));
////				}else{
////					childrenArray.get(1).add(newNode);
////				}
////				childrenArray.get(2).add(newNode);
//			}
////		}
//	}
//	public Node mapRight(ArrayList<Node> midArray) {
//		if(this.parents.size()==1&&(this.parents.get(0).childrenSubnodes.indexOf(this)==0||this.parents.get(0).childrenSubnodes.indexOf(this)==2)){
//			//if node is not divisible
//			return(this);
//		}else{
//			ArrayList<Node> nodeArray = new ArrayList<Node>();
//			this.splitChildren(nodeArray);
//			midArray.addAll(nodeArray.subList(0, nodeArray.size()-1));
//			return(nodeArray.get(nodeArray.size()-1));
//		}
//		
//	}
//	public Node supernodeParent() {
//		if(this.parents.size()==1){
//			return this.parents.get(0).supernodeParent();
//		}else{
//			return this;
//		}
//	}
	public void nodeMapFission(Definition definition,
			HashSet<Node> originalNodes) {
		for(Node parent:this.parents){
			if(!originalNodes.contains(parent)){
				originalNodes.add(parent);
				parent.nodeMapFission(definition,originalNodes);
			}
		}
		for(Node child:this.childrenSubnodes){
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
					inOutNodes.add(this.parents.get(0).childrenSubnodes.get(0));
					inOutNodes.add(this.parents.get(0).childrenSubnodes.get(1));
					inOutNodes.add(this.parents.get(0).childrenSubnodes.get(2));
				}
				int subnodes=0;
				for(Node child:this.childrenSubnodes){
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
			inOutNodes.add(this.parents.get(0).childrenSubnodes.get(0));
			inOutNodes.add(this.parents.get(0).childrenSubnodes.get(1));
			inOutNodes.add(this.parents.get(0).childrenSubnodes.get(2));
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
	public void updateNode(Definition definition,HashSet<Node> expandedNodes) {
		if(!expandedNodes.contains(this)){
			expandedNodes.add(this);
			if(this.outOfInstance!=null){
				this.outOfInstance.updateInstance(definition, expandedNodes);
			}else{
				for(Node parent:this.parents){
					parent.updateNode(definition,expandedNodes);
				}
			}
		}
	}
	public void breakSubnodes() {//removes link from childrenSubnodes to parent
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
	public void mapNode(Definition definition) {
		if(!definition.nodes.contains(this)){
			this.idForDefinition=definition.maxNode;//debugging only
			definition.maxNode++;
			this.definition=definition;
			definition.nodes.add(this);
		}
	}
	public void eval(HashMap<Node, FixedBitSet> valueMap, int depth) {
		if(!valueMap.containsKey(this)){
			if(!this.parents.isEmpty()){//deal with subnodes and supernodes
				if(this.parents.size()==1){
					this.parents.get(0).eval(valueMap, depth);
					if(valueMap.containsKey(this.parents.get(0))){
						if(parents.get(0).childrenSubnodes.get(0)==this){
							valueMap.put(this,valueMap.get(this.parents.get(0)).get(0,0));
						}else if(parents.get(0).childrenSubnodes.get(1)==this){
							valueMap.put(this,valueMap.get(this.parents.get(0)).get(1,valueMap.get(this.parents.get(0)).length()-2));
						}else if(parents.get(0).childrenSubnodes.get(2)==this){
							valueMap.put(this,valueMap.get(this.parents.get(0)).get(valueMap.get(this.parents.get(0)).length()-1,valueMap.get(this.parents.get(0)).length()-1));
						}
					}
				}else if(this.parents.size()==2&&this.parents.get(0).parents.size()==1&&this.parents.get(1).parents.size()==1&&this.parents.get(0).parents.get(0)==this.parents.get(1).parents.get(0)){// "rest" selection
					this.parents.get(0).parents.get(0).eval(valueMap, depth);
					if(valueMap.containsKey(this.parents.get(0).parents.get(0))){
						if(this.parents.get(0)==this.parents.get(0).parents.get(0).childrenSubnodes.get(0)&&this.parents.get(1)==this.parents.get(0).parents.get(0).childrenSubnodes.get(1)){//consecutive nodes
							valueMap.put(this,valueMap.get(this.parents.get(0).parents.get(0)).get(0,valueMap.get(this.parents.get(0).parents.get(0)).length()-2));
						}else if(this.parents.get(0)==this.parents.get(0).parents.get(0).childrenSubnodes.get(1)&&this.parents.get(1)==this.parents.get(0).parents.get(0).childrenSubnodes.get(2)){
							valueMap.put(this,valueMap.get(this.parents.get(0).parents.get(0)).get(1,valueMap.get(this.parents.get(0).parents.get(0)).length()-1));
						}
					}
				}else{//node with multiple parents
					FixedBitSet fixedBitSet = new FixedBitSet();
					boolean allExpanded=true;
					for (Node parent : this.parents) {
						parent.eval(valueMap, depth);
						if(valueMap.containsKey(parent)){
							fixedBitSet.concat(valueMap.get(parent));
						}else{
							allExpanded=false;
						}
					}
					if(allExpanded){
						valueMap.put(this, fixedBitSet);
					}
				}
			}else{
				if(this.outOfInstance!=null){
					boolean emptyIn=false;
					for (Node nodeIn : this.outOfInstance.in) {
						nodeIn.eval(valueMap, depth);
						if(valueMap.containsKey(nodeIn)){
							if(valueMap.get(nodeIn).length()==0){
								emptyIn=true;
							}
						}
					}
					if(!this.outOfInstance.definition.selfRecursiveInstances.isEmpty()&&emptyIn){//recursive instance with empty ins
						for (Node nodeOut : this.outOfInstance.out) {
							valueMap.put(nodeOut, new FixedBitSet());
						}
					}else{
						if(emptyIn||depth>0){
							HashMap<Node, FixedBitSet> newValueMap = new HashMap<Node, FixedBitSet>() ;
							for(int i=0;i<this.outOfInstance.in.size();i++){//copy forward
								if(valueMap.containsKey(this.outOfInstance.in.get(i))){
									newValueMap.put(this.outOfInstance.definition.in.get(i), valueMap.get(this.outOfInstance.in.get(i)));
								}
							}
							this.outOfInstance.definition.eval(newValueMap,depth-1);			
							for(int i=0;i<this.outOfInstance.out.size();i++){//copy back
								if(newValueMap.containsKey(this.outOfInstance.definition.out.get(i))){
									valueMap.put(this.outOfInstance.out.get(i), newValueMap.get(this.outOfInstance.definition.out.get(i)));
								} 
							}
						}
					}
				}
			}
		}
		
	}
	public void biFusion() {
		//fusion of two nandInstances where outs are consecutive childrenSubnodes of a same node
		if(this.parents.size()==2&&this.parents.get(0).outOfInstance!=null&&this.parents.get(1).outOfInstance!=null
				&&this.parents.get(0).outOfInstance.in.get(0).parents.size()==1&&this.parents.get(0).outOfInstance.in.get(1).parents.size()==1
				&&this.parents.get(1).outOfInstance.in.get(0).parents.size()==1&&this.parents.get(1).outOfInstance.in.get(1).parents.size()==1
				&&this.parents.get(0).outOfInstance.in.get(0).parents.get(0)==this.parents.get(1).outOfInstance.in.get(0).parents.get(0)
				&&this.parents.get(0).outOfInstance.in.get(1).parents.get(0)==this.parents.get(1).outOfInstance.in.get(1).parents.get(0)
				&&this.parents.get(0).outOfInstance.in.get(0).parents.get(0).childrenSubnodes.indexOf(this.parents.get(0).outOfInstance.in.get(0))==this.parents.get(1).outOfInstance.in.get(0).parents.get(0).childrenSubnodes.indexOf(this.parents.get(0).outOfInstance.in.get(0))
				&&this.parents.get(0).outOfInstance.in.get(1).parents.get(0).childrenSubnodes.indexOf(this.parents.get(0).outOfInstance.in.get(1))==this.parents.get(1).outOfInstance.in.get(1).parents.get(0).childrenSubnodes.indexOf(this.parents.get(0).outOfInstance.in.get(1))){
				Node nodeLeft = new Node();
				Node nodeRight= new Node();
				this.parents.get(0).outOfInstance.in.get(0).addChildSupernode(nodeLeft);
				this.parents.get(1).outOfInstance.in.get(0).addChildSupernode(nodeLeft);
				this.parents.get(0).outOfInstance.in.get(1).addChildSupernode(nodeRight);
				this.parents.get(1).outOfInstance.in.get(1).addChildSupernode(nodeRight);
				Node[] nodes={nodeLeft,nodeRight,this};
				this.definition.add(this.parents.get(0).outOfInstance.definition, nodes);
				this.definition.removeInstance(this.parents.get(0).outOfInstance);
				this.definition.removeInstance(this.parents.get(1).outOfInstance);
				this.parents.clear();
		}
		for(Node parent:this.parents){
			parent.biFusion();
		}
		if(this.outOfInstance!=null){//out of nand
			this.outOfInstance.in.get(0).biFusion();
			this.outOfInstance.in.get(1).biFusion();
		}
	}
//	public void recursivelyMapParentsMapping(
//			HashMap<Node, Node> definitionToInstanceNodes,
//			HashMap<Node, Node> expandedToDefinition) {
//		if(this.parents.size()==1){
//			Node parent=this.parents.get(0);
//			if(!definitionToInstanceNodes.containsKey(parent)){
//				Node newParent = new Node();
//				definitionToInstanceNodes.put(parent, newParent);
//				expandedToDefinition.put(newParent, parent);
//				parent.recursivelyMapParentsMapping(definitionToInstanceNodes, expandedToDefinition);
//			}
//			for(int j=0;j<3;j++){
//				Node child=parent.childrenSubnodes.get(j);
//				if(definitionToInstanceNodes.containsKey(child)){
//					definitionToInstanceNodes.get(parent).add(definitionToInstanceNodes.get(child));
//				}else{
//					Node newChild = new Node();
//					definitionToInstanceNodes.get(parent).add(newChild);
//					definitionToInstanceNodes.put(child, newChild);
//					expandedToDefinition.put(child, newChild);
//				}
//			}
//		}else{
//			for(Node parent:this.parents){//map parent nodes //think don't need to map children
//				if(definitionToInstanceNodes.containsKey(parent)){
//					definitionToInstanceNodes.get(parent).add(definitionToInstanceNodes.get(this));
//				}else{
//					Node newParent = new Node();
//					newParent.add(definitionToInstanceNodes.get(this));
//					definitionToInstanceNodes.put(parent, newParent);
//					expandedToDefinition.put(newParent, parent);
//					parent.recursivelyMapParentsMapping(definitionToInstanceNodes, expandedToDefinition);
//				}
//			}
//		}
//	}
	public Node addChildSubnode(Node childSubnode) {
		childSubnode.parents.add(this);
		this.childrenSubnodes.add(childSubnode);
		if(this.definition!=null){
			this.definition.add(childSubnode);
		}
		if(childSubnode.definition!=null){
			childSubnode.definition.add(this);
		}
		return childSubnode;
	}
	public Node removeRedundantSubnodes() {
		Node node = this;
		//FIMXE: make recusive, subnode parent 
		if(this.parents.size()==1&&!this.parents.get(0).childrenSubnodes.isEmpty()){
			if (this.parents.get(0).childrenSubnodes.get(0)==this){
				if(this.parents.get(0).parents.size()>1){
					if(this.parents.get(0).parents.get(0).parents.size()==1&&(this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(0)==parents.get(0).parents.get(0)||this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(0))){
						node=this.parents.get(0).parents.get(0).removeRedundantSubnodes();
					}else{
						this.parents.get(0).parents.get(0).splitChildrenSubnodes();
						node=this.parents.get(0).parents.get(0).childrenSubnodes.get(0).removeRedundantSubnodes();
					}
				}
			}else if (this.parents.get(0).childrenSubnodes.get(1)==this){
				if(this.parents.get(0).parents.size()>1){
					ArrayList<Node> nodes = new ArrayList<Node>();
					if(this.parents.get(0).parents.get(0).parents.size()==1&&(this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(0)==parents.get(0).parents.get(0)||this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(0))){
					}else{
						this.parents.get(0).parents.get(0).splitChildrenSubnodes();
						nodes.add(this.parents.get(0).parents.get(0).childrenSubnodes.get(1));
						nodes.add(this.parents.get(0).parents.get(0).childrenSubnodes.get(2));
					}
					nodes.addAll(this.parents.get(0).parents.subList(1,this.parents.get(0).parents.size()-1));
					if(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.size()==1&&(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(0)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1)||this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1))){
					}else{
						this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).splitChildrenSubnodes();
						nodes.add(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenSubnodes.get(0));
						nodes.add(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenSubnodes.get(1));
					}
					node.parents.clear();
					for(Node parent:nodes){
						parent.addChildSupernode(node);
					}
				}
			}else if (this.parents.get(0).childrenSubnodes.get(2)==this){
				if(this.parents.get(0).parents.size()>1){
					if(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.size()==1&&(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(0)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1)||this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1))){
						node=this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).removeRedundantSubnodes();
					}else{
						this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).splitChildrenSubnodes();
						node=this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenSubnodes.get(2).removeRedundantSubnodes();
					}
				}
			}
		}
		for(int i=0;i<this.parents.size();i++){
			this.parents.set(i,this.parents.get(i).removeRedundantSubnodes());
		}
		if(this.outOfInstance!=null){
			for(int i=0;i<this.outOfInstance.in.size();i++){
				this.outOfInstance.in.set(i, this.outOfInstance.in.get(i).removeRedundantSubnodes());
			}
		}
		return node;
	}
	public Node removeRedundantSubnodesMapping(
			HashMap<Node, Node> expandedToSelf) {
		Node node = this;
		if(this.parents.size()==1){//!this.parents.get(0).childrenSubnodes.isEmpty()
			if (this.parents.get(0).childrenSubnodes.get(0)==this){
				if(this.parents.get(0).parents.size()>1){
					if(this.parents.get(0).parents.get(0).parents.size()==1&&(this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(0)==parents.get(0).parents.get(0)||this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(0))){
						node=this.parents.get(0).parents.get(0).removeRedundantSubnodesMapping(expandedToSelf);
					}else{
						this.parents.get(0).parents.get(0).splitChildrenSubnodes();
						node=this.parents.get(0).parents.get(0).childrenSubnodes.get(0).removeRedundantSubnodesMapping(expandedToSelf);
						if(expandedToSelf.containsKey(this)){
							expandedToSelf.put(node, expandedToSelf.get(this));
						}
					}
				}
			}else if (this.parents.get(0).childrenSubnodes.get(1)==this){
				if(this.parents.get(0).parents.size()>1){
					ArrayList<Node> nodes = new ArrayList<Node>();
					if(this.parents.get(0).parents.get(0).parents.size()==1&&(this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(0)==parents.get(0).parents.get(0)||this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(0))){
					}else{
						this.parents.get(0).parents.get(0).splitChildrenSubnodes();
						nodes.add(this.parents.get(0).parents.get(0).childrenSubnodes.get(1));
						nodes.add(this.parents.get(0).parents.get(0).childrenSubnodes.get(2));
					}
					nodes.addAll(this.parents.get(0).parents.subList(1,this.parents.get(0).parents.size()-1));
					if(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.size()==1&&(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(0)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1)||this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1))){
					}else{
						this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).splitChildrenSubnodes();
						nodes.add(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenSubnodes.get(0));
						nodes.add(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenSubnodes.get(1));
					}
					node.parents.clear();
					for(Node parent:nodes){
						parent.addChildSupernode(node);
					}
				}
			}else if (this.parents.get(0).childrenSubnodes.get(2)==this){
				if(this.parents.get(0).parents.size()>1){
					if(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.size()==1&&(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(0)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1)||this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1))){
						node=this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).removeRedundantSubnodesMapping(expandedToSelf);
					}else{
						this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).splitChildrenSubnodes();
						node=this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenSubnodes.get(2).removeRedundantSubnodesMapping(expandedToSelf);
						if(expandedToSelf.containsKey(this)){
							expandedToSelf.put(node, expandedToSelf.get(this));
						}
					}
				}
			}
		}
		for(int i=0;i<this.parents.size();i++){
			this.parents.set(i,this.parents.get(i).removeRedundantSubnodesMapping(expandedToSelf));
		}
		if(this.outOfInstance!=null){
			for(int i=0;i<this.outOfInstance.in.size();i++){
				this.outOfInstance.in.set(i, this.outOfInstance.in.get(i).removeRedundantSubnodesMapping(expandedToSelf));
			}
		}
		return node;
	}
	public void mapOutParentsMapping(HashMap<Node, NandNode> nodeToNand,
			NandForest nandForest, ArrayList<Node> nandToNodeOut, HashMap<NandNode, Node> nandToNode) {
			ArrayList<NandNode> nandNodes = new ArrayList<NandNode> ();
			NandNode nandNode;
			if(nodeToNand.containsKey(this)){
				nandNode = nandForest.setOut(nodeToNand.get(this));
				nandToNodeOut.add(this);
				nandNodes.add(nandNode);
			}else if(this.outOfInstance!=null){
				nandNode = nandForest.setOut(this.toNandsMapping(nodeToNand,nandForest,nandToNode));
				nandToNodeOut.add(this);
				nandNodes.add(nandNode);
				nodeToNand.put(this, nandNode);
				nandToNode.put(nandNode, this);
			}else{
				for(Node parent:this.parents){
					parent.mapOutParentsMapping(nodeToNand, nandForest, nandToNodeOut,nandToNode);
				}
			}
		
	}
	private NandNode toNandsMapping(HashMap<Node, NandNode> nodeToNand,
			NandForest nandForest, HashMap<NandNode, Node> nandToNode) {
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
					NandNode nandNode1=node1.toNandsMapping(nodeToNand,nandForest,nandToNode);
					NandNode nandNode2=node2.toNandsMapping(nodeToNand,nandForest,nandToNode);
					nandNode=nandForest.add(nandNode1,nandNode2);
					nodeToNand.put(this, nandNode);
					nandToNode.put(nandNode, this);
				}
				return nandNode;
	}
	public void toNandDefinitions(HashSet<Node> expandedNodes) {
		if(!expandedNodes.contains(this)){
			expandedNodes.add(this);
			if(this.outOfInstance!=null){
				for(Node inOfInstance:this.outOfInstance.in){
					inOfInstance.toNandDefinitions(expandedNodes);
				}
				if(this.outOfInstance.definition.name=="nand"){
					Node[] nodes ={this.outOfInstance.in.get(0),this.outOfInstance.in.get(1),this.outOfInstance.out.get(0)};
					this.definition.add(this.outOfInstance.definition,nodes);
				}else{
					this.expandInstanceToNandInstances(this.outOfInstance);
				}
			}else{
				for(Node parent:this.parents){
					parent.toNandDefinitions(expandedNodes);
				}
			}
		}
	}
	private void expandInstanceToNandInstances(Instance instance) {
		Definition instanceDefinition=instance.definition.copy();
		instanceDefinition.toNandInstances();
		HashMap<Node,Node> definitionToInstanceNodes = new HashMap<Node,Node>();
		for (int i = 0; i < instance.in.size(); i++) {//map in nodes
			definitionToInstanceNodes.put(instanceDefinition.in.get(i), instance.in.get(i));
		}
		for (int i = 0; i < instance.out.size(); i++) {//map from out nodes way up
			this.definition.addNandInstances(instanceDefinition.out.get(i), instance.out.get(i),definitionToInstanceNodes);
		}
	}
	public int getDepth() {
		int depth=-1;
		if(this.outOfInstance!=null){
			depth=this.outOfInstance.depth;
		}else{
			for(Node parent:this.parents){
				int parentDepth=parent.getDepth();
				if(parentDepth>depth){
					depth=parentDepth;
				}
			}
		}
		return depth;
	}
	public Node findLeftChild(Node midChild) {
		Node leftChild;
		if(this.parents.isEmpty()){
			this.splitChildrenSubnodes();
			leftChild=this.childrenSubnodes.get(0);
			this.childrenSubnodes.get(1).addChildSupernode(midChild);
			this.childrenSubnodes.get(2).addChildSupernode(midChild);
		}else if(this.parents.size()==1){
			if(this.parents.get(0).childrenSubnodes.get(0)==this||this.parents.get(0).childrenSubnodes.get(2)==this){
				//indivisible node
				leftChild=this;
			}else{
				this.childrenSubnodes=this.getChildrenSubnodes();
				leftChild=this.childrenSubnodes.get(0);
				this.childrenSubnodes.get(1).addChildSupernode(midChild);
				this.childrenSubnodes.get(2).addChildSupernode(midChild);
			}
		}else{
			leftChild=this.parents.get(0).findLeftChild(midChild);
			for(int i=1;i<this.parents.size();i++){
				this.parents.get(i).addChildSupernode(midChild);
			}
		}
		return leftChild;
	}
	public Node findRightChild(Node midChild) {
		Node rightChild;
		if(this.parents.isEmpty()){
			this.splitChildrenSubnodes();
			this.childrenSubnodes.get(0).addChildSupernode(midChild);
			this.childrenSubnodes.get(1).addChildSupernode(midChild);
			rightChild=this.childrenSubnodes.get(2);
		}else if(this.parents.size()==1){
			if(this.parents.get(0).childrenSubnodes.get(0)==this||this.parents.get(0).childrenSubnodes.get(2)==this){
				//indivisible node
				rightChild=this;
			}else{
				this.childrenSubnodes=this.getChildrenSubnodes();
				this.childrenSubnodes.get(0).addChildSupernode(midChild);
				this.childrenSubnodes.get(1).addChildSupernode(midChild);
				rightChild=this.childrenSubnodes.get(2);
			}
		}else{
			for(int i=0;i<this.parents.size()-1;i++){
				this.parents.get(i).addChildSupernode(midChild);
			}
			rightChild=this.parents.get(this.parents.size()-1).findRightChild(midChild);
		}
		return rightChild;
	}
	public void fusion(HashSet<Node> expandedNodes) {
		//fusion of three nandInstances where outs are the trhee childrenSubnodes of a same node
		if(!expandedNodes.contains(this)){
			expandedNodes.add(this);
			if(!this.childrenSubnodes.isEmpty()){
				if(this.childrenSubnodes.get(0).outOfInstance!=null||this.childrenSubnodes.get(1).outOfInstance!=null||this.childrenSubnodes.get(2).outOfInstance!=null){
					Definition nandDefinition = null;
					Node nodeLeft = new Node();
					Node nodeRight= new Node();
					if(this.childrenSubnodes.get(0).outOfInstance!=null){
						nandDefinition=this.childrenSubnodes.get(0).outOfInstance.definition;
						nodeLeft.addChildSubnode(this.childrenSubnodes.get(0).outOfInstance.in.get(0));
						if(this.childrenSubnodes.get(0).outOfInstance.in.get(0)==this.childrenSubnodes.get(0).outOfInstance.in.get(1)){
							nodeRight=nodeLeft;
						}else{
							nodeRight.addChildSubnode(this.childrenSubnodes.get(0).outOfInstance.in.get(1));
						}
						this.definition.removeInstance(this.childrenSubnodes.get(0).outOfInstance);
					}else{
						nodeLeft.addChildSubnode(new Node());
						nodeRight.addChildSubnode(new Node());
					}
					if(this.childrenSubnodes.get(1).outOfInstance!=null){
						nandDefinition=this.childrenSubnodes.get(1).outOfInstance.definition;
						nodeLeft.addChildSubnode(this.childrenSubnodes.get(1).outOfInstance.in.get(0));
						if(this.childrenSubnodes.get(1).outOfInstance.in.get(0)==this.childrenSubnodes.get(1).outOfInstance.in.get(1)){
							nodeRight=nodeLeft;
						}else{
							nodeRight.addChildSubnode(this.childrenSubnodes.get(1).outOfInstance.in.get(1));
						}
						this.definition.removeInstance(this.childrenSubnodes.get(1).outOfInstance);
					}else{
						nodeLeft.addChildSubnode(new Node());
						nodeRight.addChildSubnode(new Node());
					}
					if(this.childrenSubnodes.get(2).outOfInstance!=null){
						nandDefinition=this.childrenSubnodes.get(2).outOfInstance.definition;
						nodeLeft.addChildSubnode(this.childrenSubnodes.get(2).outOfInstance.in.get(0));
						if(this.childrenSubnodes.get(2).outOfInstance.in.get(0)==this.childrenSubnodes.get(2).outOfInstance.in.get(1)){
							nodeRight=nodeLeft;
						}else{
							nodeRight.addChildSubnode(this.childrenSubnodes.get(2).outOfInstance.in.get(1));
						}
						this.definition.removeInstance(this.childrenSubnodes.get(2).outOfInstance);
					}else{
						nodeLeft.addChildSubnode(new Node());
						nodeRight.addChildSubnode(new Node());
					}
					Node[] nodes={nodeLeft,nodeRight,this};
					this.definition.add(nandDefinition, nodes);
					nodeLeft.fusion(expandedNodes);
					nodeRight.fusion(expandedNodes);
				}
			}
			for(Node parent:this.parents){
				parent.fusion(expandedNodes);
			}
		}
	}
}
