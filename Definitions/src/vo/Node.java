/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import utils.FixedBitSet;
//Each node can have 1 supernode parent and multiple parents, if a node has 1 parent it's a subnode of this one parent, if a node has multiple parents it's a supernode of these
//Each node can have supernode AND subnode children
//childrenSubnodes serve as indexes: if a node has subnode children, the leftmost represents the leftmost bit, the rightmost the rightmost bit and the middle one the rest
public class Node {
	public Node parentSupernode;
	public ArrayList<Node> parentSubnodes;//ArrayList, since there can be repetition
	public ArrayList<Node> childrenSubnodes;
	public ArrayList<Node> childrenSupernodes;
	public Instance outOfInstance;
	public Definition definition;
	//DEBUGGING ONLY
	public int idForDefinition;//id of node for the definition where it's used
	//END OF DEBUGGING ONLY
	
	public Node() { 
		this.parentSupernode=null;
		this.parentSubnodes = new ArrayList<Node>();
		this.childrenSubnodes = new ArrayList<Node>();
		this.childrenSupernodes = new ArrayList<Node>();
	}
	public Node addChildSupernode(Node childSuperNode) {//add children supernode to node		
		childSuperNode.parentSupernode=this;
		this.childrenSupernodes.add(childSuperNode);
		if(this.definition!=null){
			this.definition.add(childSuperNode);
		}
		if(childSuperNode.definition!=null){
			childSuperNode.definition.add(this);
		}
		return childSuperNode;
	}
	public NandNode toNands(HashMap<NandNode, Node> nandToNode,HashMap<Node, NandNode> nodeToNand, NandForest nandForest) {
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
			NandNode nandNode1=node1.toNands(nandToNode,nodeToNand,nandForest);
			NandNode nandNode2=node2.toNands(nandToNode,nodeToNand,nandForest);
			nandNode=nandForest.add(nandNode1,nandNode2);
			nodeToNand.put(this, nandNode);
			if(!nandToNode.containsKey(nandNode))nandToNode.put(nandNode,this);
		}
		return nandNode;
	}
	public String toString() {
		String string = new String();
		
		string+=this.idForDefinition;
		if(this.parentSupernode!=null){
			string+="[";
			string+=this.parentSupernode.idForDefinition;//string+=this.parentSupernode.toString(); if more detail needed
			string+="{";
			string+=this.parentSupernode.childrenSubnodes.indexOf(this);
			string+="}";
			string+="]";
		}
		if(!this.parentSubnodes.isEmpty()){
			string+="(";
			for(Node node: this.parentSubnodes){
				string+=node.toString();
				string+=("&");
			}
			string=string.substring(0, string.length() - 1);//remove last enumeration "&"
			string+=")";
		}
			
		return string;
	}
	public void mapInChildren(HashMap<NandNode, Node> nandToNode,HashMap<Node, NandNode> nodeToNand, NandForest nandForest) {		
		if(this.childrenSubnodes.isEmpty()){
			if(!nodeToNand.containsKey(this)){
				NandNode nandNode = nandForest.addIn();
				nandToNode.put(nandNode,this);
				nodeToNand.put(this, nandNode);
			}
		}else{
			for(Node child:this.childrenSubnodes){
				child.mapInChildren(nandToNode,nodeToNand, nandForest);
			}
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
	public void mapOutParents(HashMap<NandNode, Node> nandToNode,HashMap<Node, NandNode> nodeToNand,
			NandForest nandForest) {
		NandNode nandNode;
		if(nodeToNand.containsKey(this)){
			nandNode = nandForest.setOut(nodeToNand.get(this));
		}else if(this.outOfInstance!=null){
			nandNode = nandForest.setOut(this.toNands(nandToNode,nodeToNand,nandForest));
		}else{
			for(Node parent:this.parentSubnodes){
				parent.mapOutParents(nandToNode,nodeToNand, nandForest);
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
		if(!this.parentSubnodes.isEmpty()){
			parentNodes.addAll(this.parentSubnodes);
			for(Node parent:parentNodes){
				parent.childrenFission();
			}
		}else if(parentSupernode!=null){
			parentSupernode.childrenFission();
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
		//Must return an array of childrenSubnodes, since they are a representation of it's children,
		//not necessarily it's children
		ArrayList<Node> childrenSubnodes = new ArrayList<Node>();
		if(!this.parentSubnodes.isEmpty()){
			Node leftSubnode;
			Node midSubnode;
			Node rightSubnode;
			midSubnode=new Node();
			leftSubnode=this.parentSubnodes.get(0).findLeftChild(midSubnode);
			for(int i=1;i<this.parentSubnodes.size()-1;i++){
				this.parentSubnodes.get(i).addChildSupernode(midSubnode);
			}
			rightSubnode=this.parentSubnodes.get(this.parentSubnodes.size()-1).findRightChild(midSubnode);
			childrenSubnodes.add(leftSubnode);
			childrenSubnodes.add(midSubnode);
			childrenSubnodes.add(rightSubnode);
		}else if(this.parentSupernode!=null){//can't be an indivisible node //NEEDED?
			Node parentMidNode = this.parentSupernode.getChildrenSubnodes().get(1);
			if(parentMidNode==this){
				this.splitChildrenSubnodes();
				childrenSubnodes=this.childrenSubnodes;
			}else{
				childrenSubnodes=parentMidNode.getChildrenSubnodes();
			}
		}else{
			this.splitChildrenSubnodes();
			childrenSubnodes=this.childrenSubnodes;
		}
		return childrenSubnodes;
	}
	public void parentsFission() {
		if(!this.parentSubnodes.isEmpty()){
		//if in of nand has  multiple parents, separate in multiple nands
			for(Node parent:this.parentSubnodes){
				parent.parentsFission();
			}
		}else if(this.parentSupernode!=null){
			this.parentSupernode.parentsFission();
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
		if(!in0.parentSubnodes.isEmpty()||!in1.parentSubnodes.isEmpty()){
			if(in0.parentSubnodes.size()==in1.parentSubnodes.size()){//Needed?
				for(int i=0;i<in0.parentSubnodes.size();i++){//should be recursive into parents
					Node newNode = new Node();
					Node[] nodes={in0.parentSubnodes.get(i),in1.parentSubnodes.get(i),newNode};
					this.definition.add(this.outOfInstance.definition, nodes);
					newNode.addChildSupernode(this);
					newNode.nandParentFission();
				}
				this.definition.removeInstance(this.outOfInstance);
			}
		}
	}
//	public ArrayList<Node> flattenParents() {//needed?
//		ArrayList<Node> nodes = new ArrayList<Node>();
//		if(this.parents.size()==0){
//			nodes.add(this);
//		}else if(this.parents.size()==1){	
//			this.parents.get(0).flattenParents();
//			nodes.add(this);
//		}else{
//			for(Node parent:this.parents){
//				nodes.addAll(parent.flattenParents());
//			}
//			this.parents.clear();
//			for(Node parent:nodes){
//				if(parent.childrenSupernodes.contains(this)){
//					parents.add(parent);
//				}else{
//					parent.addChildSupernode(this);
//				}
//			}
//		}
//		if(this.outOfInstance!=null){//out of nand
//			this.outOfInstance.in.get(0).flattenParents();
//			this.outOfInstance.in.get(1).flattenParents();
//		}
//		return nodes;
//	}
//	public void nodeFussion() {
//		for(int i=0;i<this.parents.size();i++){
//			if(this.parents.size()-i>=3){//posible subnodes(indexes) of the same node
//				if(this.parents.get(i).parents.size()==1){
//					Node grandfather=this.parents.get(i).parents.get(0);
//					if(grandfather.childrenSubnodes.get(0)==this.parents.get(i)){
//						if(grandfather.childrenSubnodes.get(1)==this.parents.get(i+1)){
//							if(grandfather.childrenSubnodes.get(2)==this.parents.get(i+2)){
//								this.parents.remove(i);
//								this.parents.remove(i);
//								this.parents.remove(i);
////								grandfather.children.add(this);//error, cant do this because we don't know if the subnodes are used//a node can't have both subnode and supernode children
//								this.parents.add(i,grandfather);
//							}
//						}
//						
//					}
//				}
//			}
//		}
//		for(Node parent:this.parents){
//			parent.nodeFussion();
//		}
//		if(this.outOfInstance!=null){//out of nand
//			this.outOfInstance.in.get(0).nodeFussion();
//			this.outOfInstance.in.get(1).nodeFussion();
//		}
//		
//	}
//	public void mapInsOfNandInstances(HashMap<Node, HashSet<Instance>> inOfInstances) {
//		if(!this.parentSubnodes.isEmpty()){
//			for(Node parent:this.parentSubnodes){
//				parent.mapInsOfNandInstances(inOfInstances);
//			}
//		}else{
//			if(this.parentSupernode!=null){
//				parentSupernode.mapInsOfNandInstances(inOfInstances);
//			}
//		}
//		if(this.outOfInstance!=null){//out of nand
//			if(!inOfInstances.containsKey(this.outOfInstance.in.get(0))){
//				HashSet<Instance> hashSet = new HashSet<Instance>();
//				hashSet.add(this.outOfInstance);
//				inOfInstances.put(this.outOfInstance.in.get(0), hashSet);
//			}else{
//				HashSet<Instance> hashSet = inOfInstances.get(this.outOfInstance.in.get(0));
//				hashSet.add(this.outOfInstance);
//			}
//			if(!inOfInstances.containsKey(this.outOfInstance.in.get(1))){
//				HashSet<Instance> hashSet = new HashSet<Instance>();
//				hashSet.add(this.outOfInstance);
//				inOfInstances.put(this.outOfInstance.in.get(1), hashSet);
//			}else{
//				HashSet<Instance> hashSet = inOfInstances.get(this.outOfInstance.in.get(1));
//				hashSet.add(this.outOfInstance);
//			}
//			this.outOfInstance.in.get(0).mapInsOfNandInstances(inOfInstances);
//			this.outOfInstance.in.get(1).mapInsOfNandInstances(inOfInstances);
//		}
//	}
	public void mapSupernodeParents(HashSet<Node> supernodeParents) {
		if(this.parentSupernode!=null){
			this.parentSupernode.mapSupernodeParents(supernodeParents);
		}else{
			supernodeParents.add(this);
			for(Node parent:this.parentSubnodes){
				parent.mapSupernodeParents(supernodeParents);
			}	
		}
	}
	public void mapChildren(HashSet<Node> inOutNodes) {
		inOutNodes.add(this);
		for(Node child:this.childrenSubnodes){
			if(child.parentSupernode!=null){
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
			if(!this.parentSubnodes.isEmpty()){
				instance=this.parentSubnodes.get(0).findRootInstance();
			}else if(this.parentSupernode!=null){
				instance=this.parentSupernode.findRootInstance();
			}else{
				instance=null;
			}
		}
		return instance;
	}
//	public void nodeMapFission(Definition definition,
//			HashSet<Node> originalNodes) {
//		for(Node parent:this.parents){
//			if(!originalNodes.contains(parent)){
//				originalNodes.add(parent);
//				parent.nodeMapFission(definition,originalNodes);
//			}
//		}
//		for(Node child:this.childrenSubnodes){
//			if(!originalNodes.contains(child)){
//				originalNodes.add(child);
//			}
//			child.nodeMapFission(definition,originalNodes);
//		}
//		
//	}
//	public void findInsMapping(HashSet<Node> inNodes,
//			HashMap<Node, NandNode> nodeToNand, NandForest nandForest,
//			ArrayList<Node> nandToNodeIn, HashSet<Node> inOutNodes,
//			int addedInNodes, ArrayList<NandNode> originalDefinitionNandIn,
//			ArrayList<NandNode> originalAddedDefinitionNandIn) {
//		if(inNodes.contains(this)){
//			if(this.definition.in.indexOf(this)<this.definition.in.size()-addedInNodes){
//				this.mapInChildrenMapping(nodeToNand, nandForest, nandToNodeIn, inOutNodes, originalDefinitionNandIn);	
//			}else{
//				this.mapInChildrenMapping(nodeToNand, nandForest, nandToNodeIn, inOutNodes, originalAddedDefinitionNandIn);	
//			}
//		}else{
//			for(Node parent:this.parents){
//				parent.findInsMapping(inNodes, nodeToNand, nandForest, nandToNodeIn, inOutNodes, addedInNodes, originalDefinitionNandIn, originalAddedDefinitionNandIn);	
//			}
//			if(this.outOfInstance!=null){
//				this.outOfInstance.in.get(0).findInsMapping(inNodes, nodeToNand, nandForest, nandToNodeIn, inOutNodes, addedInNodes, originalDefinitionNandIn, originalAddedDefinitionNandIn);
//				this.outOfInstance.in.get(1).findInsMapping(inNodes, nodeToNand, nandForest, nandToNodeIn, inOutNodes, addedInNodes, originalDefinitionNandIn, originalAddedDefinitionNandIn);
//			}
//		}
//	
//}
//	void mapInChildrenMapping(HashMap<Node, NandNode> nodeToNand,
//			NandForest nandForest, ArrayList<Node> nandToNodeIn,
//			HashSet<Node> inOutNodes, ArrayList<NandNode> originalDefinitionNandIn) {
//		//Only maps nodes that are used in the definition		
//				inOutNodes.add(this);//keep track of nodes previous to the nodes mapped to NandForest in order to not erase them
//				if(this.parents.size()==1){
//					inOutNodes.add(this.parents.get(0));
//					inOutNodes.add(this.parents.get(0).childrenSubnodes.get(0));
//					inOutNodes.add(this.parents.get(0).childrenSubnodes.get(1));
//					inOutNodes.add(this.parents.get(0).childrenSubnodes.get(2));
//				}
//				int subnodes=0;
//				for(Node child:this.childrenSubnodes){
//					if(child.parents.size()==1){//subnode  
//						subnodes++;
//						child.mapInChildrenMapping(nodeToNand, nandForest, nandToNodeIn, inOutNodes, originalDefinitionNandIn);	
//					}
//				}
//				if(subnodes==0){
//					NandNode nandNode;
//					if(nandToNodeIn.contains(this)){
//						nandNode=nodeToNand.get(this);
//					}else{
//						nandNode = nandForest.addIn();
//						nandToNodeIn.add(this);
//						originalDefinitionNandIn.add(nandNode);
//					}
//					nodeToNand.put(this, nandNode);
//				}
//		
//	}
//	public void mapOutParentsMapping(HashMap<Node, NandNode> nodeToNand,
//			NandForest nandForest, ArrayList<Node> nandToNodeOut,
//			HashSet<Node> inOutNodes,
//			ArrayList<NandNode> originalDefinitionNandOut) {
//		ArrayList<NandNode> nandNodes = new ArrayList<NandNode> ();
//		inOutNodes.add(this);
//		if(this.parents.size()==1){
//			inOutNodes.add(this.parents.get(0));
//			inOutNodes.add(this.parents.get(0).childrenSubnodes.get(0));
//			inOutNodes.add(this.parents.get(0).childrenSubnodes.get(1));
//			inOutNodes.add(this.parents.get(0).childrenSubnodes.get(2));
//		}
//		if(this.outOfInstance!=null){
//			NandNode nandNode;
//			if(nandToNodeOut.contains(this)){
//				nandNode = nodeToNand.get(this);
//			}else{
//				nandNode = nandForest.setOut(this.toNands(nodeToNand,nandForest));
//				nandToNodeOut.add(this);
//				originalDefinitionNandOut.add(nandNode);
//			}
//			nandNodes.add(nandNode);
//		}else{
//			for(Node parent:this.parents){
//				parent.mapOutParentsMapping(nodeToNand, nandForest, nandToNodeOut, inOutNodes, originalDefinitionNandOut);
//			}
//		}
//	}
	public void updateNode(Definition definition,HashSet<Node> expandedNodes) {
		if(!expandedNodes.contains(this)){
			expandedNodes.add(this);
			if(this.outOfInstance!=null){
				this.outOfInstance.updateInstance(definition, expandedNodes);
			}else{
				if(!this.parentSubnodes.isEmpty()){
					for(Node parent:this.parentSubnodes){
						parent.updateNode(definition,expandedNodes);
					}
				}else if(this.parentSupernode!=null){
					this.parentSupernode.updateNode(definition, expandedNodes);
				}
			}
		}
	}
//	 
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
			if(!this.parentSubnodes.isEmpty()){//deal with subnodes and supernodes
				if(this.parentSubnodes.size()==2&&this.parentSubnodes.get(0).parentSupernode!=null&&this.parentSubnodes.get(1).parentSupernode!=null&&this.parentSubnodes.get(0).parentSupernode==this.parentSubnodes.get(1).parentSupernode){// "rest" selection
					this.parentSubnodes.get(0).parentSupernode.eval(valueMap, depth);
					if(valueMap.containsKey(this.parentSubnodes.get(0).parentSupernode)){
						if(this.parentSubnodes.get(0)==this.parentSubnodes.get(0).parentSupernode.childrenSubnodes.get(0)&&this.parentSubnodes.get(1)==this.parentSubnodes.get(0).parentSupernode.childrenSubnodes.get(1)){//consecutive nodes
							valueMap.put(this,valueMap.get(this.parentSubnodes.get(0).parentSupernode).get(0,valueMap.get(this.parentSubnodes.get(0).parentSupernode).length()-2));
						}else if(this.parentSubnodes.get(0)==this.parentSubnodes.get(0).parentSupernode.childrenSubnodes.get(1)&&this.parentSubnodes.get(1)==this.parentSubnodes.get(0).parentSupernode.childrenSubnodes.get(2)){
							valueMap.put(this,valueMap.get(this.parentSubnodes.get(0).parentSupernode).get(1,valueMap.get(this.parentSubnodes.get(0).parentSupernode).length()-1));
						}
					}
				}else{//node with multiple parents
					FixedBitSet fixedBitSet = new FixedBitSet();
					boolean allExpanded=true;
					for (Node parent : this.parentSubnodes) {
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
		}else if(this.parentSupernode!=null){
			this.parentSupernode.eval(valueMap, depth);
			if(valueMap.containsKey(this.parentSupernode)){
				if(parentSupernode.childrenSubnodes.get(0)==this){
					valueMap.put(this,valueMap.get(this.parentSupernode).get(0,0));
				}else if(parentSupernode.childrenSubnodes.get(1)==this){
					valueMap.put(this,valueMap.get(this.parentSupernode).get(1,valueMap.get(this.parentSupernode).length()-2));
				}else if(parentSupernode.childrenSubnodes.get(2)==this){
					valueMap.put(this,valueMap.get(this.parentSupernode).get(valueMap.get(this.parentSupernode).length()-1,valueMap.get(this.parentSupernode).length()-1));
				}
			}
		}
		
	}
	public void biFusion() {
		//fusion of two nandInstances where outs are consecutive childrenSubnodes of a same node
		//TODO: simplify check
		if(this.parentSubnodes.size()==2&&this.parentSubnodes.get(0).outOfInstance!=null&&this.parentSubnodes.get(1).outOfInstance!=null
				&&this.parentSubnodes.get(0).outOfInstance.in.get(0).parentSupernode!=null&&this.parentSubnodes.get(1).outOfInstance.in.get(0).parentSupernode!=null
				&&this.parentSubnodes.get(0).outOfInstance.in.get(0).parentSupernode==this.parentSubnodes.get(1).outOfInstance.in.get(0).parentSupernode
				&&this.parentSubnodes.get(0).outOfInstance.in.get(1).parentSupernode!=null&&this.parentSubnodes.get(1).outOfInstance.in.get(1).parentSupernode!=null
				&&this.parentSubnodes.get(0).outOfInstance.in.get(1).parentSupernode==this.parentSubnodes.get(1).outOfInstance.in.get(1).parentSupernode
				&&this.parentSubnodes.get(0).outOfInstance.in.get(0).parentSupernode.childrenSubnodes.indexOf(this.parentSubnodes.get(0).outOfInstance.in.get(0))==this.parentSubnodes.get(1).outOfInstance.in.get(0).parentSupernode.childrenSubnodes.indexOf(this.parentSubnodes.get(0).outOfInstance.in.get(0))
				&&this.parentSubnodes.get(0).outOfInstance.in.get(1).parentSupernode.childrenSubnodes.indexOf(this.parentSubnodes.get(0).outOfInstance.in.get(1))==this.parentSubnodes.get(1).outOfInstance.in.get(1).parentSupernode.childrenSubnodes.indexOf(this.parentSubnodes.get(0).outOfInstance.in.get(1))){
				Node nodeLeft = new Node();
				Node nodeRight= new Node();
				this.parentSubnodes.get(0).outOfInstance.in.get(0).addChildSupernode(nodeLeft);
				this.parentSubnodes.get(1).outOfInstance.in.get(0).addChildSupernode(nodeLeft);
				if(this.parentSubnodes.get(0).outOfInstance.in.get(0)==this.parentSubnodes.get(0).outOfInstance.in.get(1)){
					nodeRight=nodeLeft;
				}else{
					this.parentSubnodes.get(0).outOfInstance.in.get(1).addChildSupernode(nodeRight);
					this.parentSubnodes.get(1).outOfInstance.in.get(1).addChildSupernode(nodeRight);
				}
				Node[] nodes={nodeLeft,nodeRight,this};
				this.definition.add(this.parentSubnodes.get(0).outOfInstance.definition, nodes);
				this.definition.removeInstance(this.parentSubnodes.get(0).outOfInstance);
				this.definition.removeInstance(this.parentSubnodes.get(1).outOfInstance);
//				this.parents.clear();FIXME: remove subnodes?
		}
		if(!this.parentSubnodes.isEmpty()){
			for(Node parent:this.parentSubnodes){
				parent.biFusion();
			}
		}else if(this.parentSupernode!=null){
			this.parentSupernode.biFusion();
		}
		if(this.outOfInstance!=null){//out of nand
			this.outOfInstance.in.get(0).biFusion();
			this.outOfInstance.in.get(1).biFusion();
		}
	}
	public Node addChildSubnode(Node childSubnode) {
		childSubnode.parentSupernode=this;
		this.childrenSubnodes.add(childSubnode);
		if(this.definition!=null){
			this.definition.add(childSubnode);
		}
		if(childSubnode.definition!=null){
			childSubnode.definition.add(this);
		}
		return childSubnode;
	}
//	public Node removeRedundantSubnodes() {
//		Node node = this;
//		//FIMXE: make recusive, subnode parent 
//		if(this.parents.size()==1&&!this.parents.get(0).childrenSubnodes.isEmpty()){
//			if (this.parents.get(0).childrenSubnodes.get(0)==this){
//				if(this.parents.get(0).parents.size()>1){
//					if(this.parents.get(0).parents.get(0).parents.size()==1&&(this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(0)==parents.get(0).parents.get(0)||this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(0))){
//						node=this.parents.get(0).parents.get(0).removeRedundantSubnodes();
//					}else{
//						this.parents.get(0).parents.get(0).splitChildrenSubnodes();
//						node=this.parents.get(0).parents.get(0).childrenSubnodes.get(0).removeRedundantSubnodes();
//					}
//				}
//			}else if (this.parents.get(0).childrenSubnodes.get(1)==this){
//				if(this.parents.get(0).parents.size()>1){
//					ArrayList<Node> nodes = new ArrayList<Node>();
//					if(this.parents.get(0).parents.get(0).parents.size()==1&&(this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(0)==parents.get(0).parents.get(0)||this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(0))){
//					}else{
//						this.parents.get(0).parents.get(0).splitChildrenSubnodes();
//						nodes.add(this.parents.get(0).parents.get(0).childrenSubnodes.get(1));
//						nodes.add(this.parents.get(0).parents.get(0).childrenSubnodes.get(2));
//					}
//					nodes.addAll(this.parents.get(0).parents.subList(1,this.parents.get(0).parents.size()-1));
//					if(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.size()==1&&(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(0)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1)||this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1))){
//					}else{
//						this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).splitChildrenSubnodes();
//						nodes.add(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenSubnodes.get(0));
//						nodes.add(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenSubnodes.get(1));
//					}
//					node.parents.clear();
//					for(Node parent:nodes){
//						parent.addChildSupernode(node);
//					}
//				}
//			}else if (this.parents.get(0).childrenSubnodes.get(2)==this){
//				if(this.parents.get(0).parents.size()>1){
//					if(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.size()==1&&(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(0)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1)||this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1))){
//						node=this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).removeRedundantSubnodes();
//					}else{
//						this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).splitChildrenSubnodes();
//						node=this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenSubnodes.get(2).removeRedundantSubnodes();
//					}
//				}
//			}
//		}
//		for(int i=0;i<this.parents.size();i++){
//			this.parents.set(i,this.parents.get(i).removeRedundantSubnodes());
//		}
//		if(this.outOfInstance!=null){
//			for(int i=0;i<this.outOfInstance.in.size();i++){
//				this.outOfInstance.in.set(i, this.outOfInstance.in.get(i).removeRedundantSubnodes());
//			}
//		}
//		return node;
//	}
//	public Node removeRedundantSubnodesMapping(
//			HashMap<Node, Node> expandedToSelf) {
//		Node node = this;
//		if(this.parents.size()==1){//!this.parents.get(0).childrenSubnodes.isEmpty()
//			if (this.parents.get(0).childrenSubnodes.get(0)==this){
//				if(this.parents.get(0).parents.size()>1){
//					if(this.parents.get(0).parents.get(0).parents.size()==1&&(this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(0)==parents.get(0).parents.get(0)||this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(0))){
//						node=this.parents.get(0).parents.get(0).removeRedundantSubnodesMapping(expandedToSelf);
//					}else{
//						this.parents.get(0).parents.get(0).splitChildrenSubnodes();
//						node=this.parents.get(0).parents.get(0).childrenSubnodes.get(0).removeRedundantSubnodesMapping(expandedToSelf);
//						if(expandedToSelf.containsKey(this)){
//							expandedToSelf.put(node, expandedToSelf.get(this));
//						}
//					}
//				}
//			}else if (this.parents.get(0).childrenSubnodes.get(1)==this){
//				if(this.parents.get(0).parents.size()>1){
//					ArrayList<Node> nodes = new ArrayList<Node>();
//					if(this.parents.get(0).parents.get(0).parents.size()==1&&(this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(0)==parents.get(0).parents.get(0)||this.parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(0))){
//					}else{
//						this.parents.get(0).parents.get(0).splitChildrenSubnodes();
//						nodes.add(this.parents.get(0).parents.get(0).childrenSubnodes.get(1));
//						nodes.add(this.parents.get(0).parents.get(0).childrenSubnodes.get(2));
//					}
//					nodes.addAll(this.parents.get(0).parents.subList(1,this.parents.get(0).parents.size()-1));
//					if(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.size()==1&&(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(0)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1)||this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1))){
//					}else{
//						this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).splitChildrenSubnodes();
//						nodes.add(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenSubnodes.get(0));
//						nodes.add(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenSubnodes.get(1));
//					}
//					node.parents.clear();
//					for(Node parent:nodes){
//						parent.addChildSupernode(node);
//					}
//				}
//			}else if (this.parents.get(0).childrenSubnodes.get(2)==this){
//				if(this.parents.get(0).parents.size()>1){
//					if(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.size()==1&&(this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(0)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1)||this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).parents.get(0).childrenSubnodes.get(2)==this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1))){
//						node=this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).removeRedundantSubnodesMapping(expandedToSelf);
//					}else{
//						this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).splitChildrenSubnodes();
//						node=this.parents.get(0).parents.get(this.parents.get(0).parents.size()-1).childrenSubnodes.get(2).removeRedundantSubnodesMapping(expandedToSelf);
//						if(expandedToSelf.containsKey(this)){
//							expandedToSelf.put(node, expandedToSelf.get(this));
//						}
//					}
//				}
//			}
//		}
//		for(int i=0;i<this.parents.size();i++){
//			this.parents.set(i,this.parents.get(i).removeRedundantSubnodesMapping(expandedToSelf));
//		}
//		if(this.outOfInstance!=null){
//			for(int i=0;i<this.outOfInstance.in.size();i++){
//				this.outOfInstance.in.set(i, this.outOfInstance.in.get(i).removeRedundantSubnodesMapping(expandedToSelf));
//			}
//		}
//		return node;
//	}
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
				if(!this.parentSubnodes.isEmpty()){
					for(Node parent:this.parentSubnodes){
						parent.mapOutParentsMapping(nodeToNand, nandForest, nandToNodeOut,nandToNode);
					}
				}else if(this.parentSupernode!=null){
					this.parentSupernode.mapOutParentsMapping(nodeToNand, nandForest, nandToNodeOut,nandToNode);
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
				if(!this.parentSubnodes.isEmpty()){
					for(Node parent:this.parentSubnodes){
						parent.toNandDefinitions(expandedNodes);
					}
				}else if(this.parentSupernode!=null){
					this.parentSupernode.toNandDefinitions(expandedNodes);
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
			if(!this.parentSubnodes.isEmpty()){
				for(Node parent:this.parentSubnodes){
					int parentDepth=parent.getDepth();
					if(parentDepth>depth){
						depth=parentDepth;
					}
				}
			}else if(this.parentSupernode!=null){
				int parentDepth=this.parentSupernode.getDepth();
				if(parentDepth>depth){
					depth=parentDepth;
				}
			}
			
		}
		return depth;
	}
	public Node findLeftChild(Node midChild) {
		Node leftChild;
		if(!this.parentSubnodes.isEmpty()){
			leftChild=this.parentSubnodes.get(0).findLeftChild(midChild);
			for(int i=1;i<this.parentSubnodes.size();i++){
				this.parentSubnodes.get(i).addChildSupernode(midChild);
			}
		}else if(this.parentSupernode!=null){
			if(this.parentSupernode.childrenSubnodes.get(0)==this||this.parentSupernode.childrenSubnodes.get(2)==this){
				//indivisible node
				leftChild=this;
			}else{
				this.childrenSubnodes=this.getChildrenSubnodes();
				leftChild=this.childrenSubnodes.get(0);
				this.childrenSubnodes.get(1).addChildSupernode(midChild);
				this.childrenSubnodes.get(2).addChildSupernode(midChild);
			}
		}else{
			this.splitChildrenSubnodes();
			leftChild=this.childrenSubnodes.get(0);
			this.childrenSubnodes.get(1).addChildSupernode(midChild);
			this.childrenSubnodes.get(2).addChildSupernode(midChild);
		}
		return leftChild;
	}
	public Node findRightChild(Node midChild) {
		Node rightChild;
		if(!this.parentSubnodes.isEmpty()){
			for(int i=0;i<this.parentSubnodes.size()-1;i++){
				this.parentSubnodes.get(i).addChildSupernode(midChild);
			}
			rightChild=this.parentSubnodes.get(this.parentSubnodes.size()-1).findRightChild(midChild);
		}else if(this.parentSupernode!=null){
			if(this.parentSupernode.childrenSubnodes.get(0)==this||this.parentSupernode.childrenSubnodes.get(2)==this){
				//indivisible node
				rightChild=this;
			}else{
				this.childrenSubnodes=this.getChildrenSubnodes();
				this.childrenSubnodes.get(0).addChildSupernode(midChild);
				this.childrenSubnodes.get(1).addChildSupernode(midChild);
				rightChild=this.childrenSubnodes.get(2);
			}
		}else{
			this.splitChildrenSubnodes();
			this.childrenSubnodes.get(0).addChildSupernode(midChild);
			this.childrenSubnodes.get(1).addChildSupernode(midChild);
			rightChild=this.childrenSubnodes.get(2);
		}
		return rightChild;
	}
	public void fusion(HashSet<Node> expandedNodes) {
		//fusion of three nandInstances where outs are the three childrenSubnodes of a same node
		//TODO: simplify
		if(!expandedNodes.contains(this)){
			expandedNodes.add(this);
			int realDepth=0;
			int instanceDepth;
			if(!this.childrenSubnodes.isEmpty()){
				if(this.childrenSubnodes.get(0).outOfInstance!=null&&this.childrenSubnodes.get(1).outOfInstance!=null&&this.childrenSubnodes.get(2).outOfInstance!=null){
					Definition nandDefinition = null;
					Node nodeLeft = new Node();
					Node nodeRight= new Node();
					HashSet<Instance> instancesToRemove = new HashSet<Instance>();
					instanceDepth=this.childrenSubnodes.get(0).outOfInstance.depth;
					if(instanceDepth>realDepth) realDepth=instanceDepth;
					nandDefinition=this.childrenSubnodes.get(0).outOfInstance.definition;
					if(this.childrenSubnodes.get(0).outOfInstance.in.get(0).parentSupernode!=null){
						nodeLeft=this.childrenSubnodes.get(0).outOfInstance.in.get(0).parentSupernode;
					}else{
						nodeLeft.addChildSubnode(this.childrenSubnodes.get(0).outOfInstance.in.get(0));
					}
					if(this.childrenSubnodes.get(0).outOfInstance.in.get(0)==this.childrenSubnodes.get(0).outOfInstance.in.get(1)){
						nodeRight=nodeLeft;
					}else{
						if(this.childrenSubnodes.get(0).outOfInstance.in.get(1).parentSupernode!=null){
							nodeRight=this.childrenSubnodes.get(0).outOfInstance.in.get(1).parentSupernode;
						}else{
							nodeRight.addChildSubnode(this.childrenSubnodes.get(0).outOfInstance.in.get(1));
						}
					}
					instancesToRemove.add(this.childrenSubnodes.get(0).outOfInstance);
					instanceDepth=this.childrenSubnodes.get(1).outOfInstance.depth;
					if(instanceDepth>realDepth) realDepth=instanceDepth;
					nandDefinition=this.childrenSubnodes.get(1).outOfInstance.definition;
					if(this.childrenSubnodes.get(1).outOfInstance.in.get(0).parentSupernode!=null){
						nodeLeft=this.childrenSubnodes.get(1).outOfInstance.in.get(0).parentSupernode;
					}else{
						nodeLeft.addChildSubnode(this.childrenSubnodes.get(1).outOfInstance.in.get(0));
					}
					if(this.childrenSubnodes.get(1).outOfInstance.in.get(0)==this.childrenSubnodes.get(1).outOfInstance.in.get(1)){
						nodeRight=nodeLeft;
					}else{
						if(this.childrenSubnodes.get(1).outOfInstance.in.get(1).parentSupernode!=null){
							nodeRight=this.childrenSubnodes.get(1).outOfInstance.in.get(1).parentSupernode;
						}else{
							nodeRight.addChildSubnode(this.childrenSubnodes.get(1).outOfInstance.in.get(1));
						}
					}
					instancesToRemove.add(this.childrenSubnodes.get(1).outOfInstance);
					instanceDepth=this.childrenSubnodes.get(2).outOfInstance.depth;
					if(instanceDepth>realDepth) realDepth=instanceDepth;
					nandDefinition=this.childrenSubnodes.get(2).outOfInstance.definition;
					if(this.childrenSubnodes.get(2).outOfInstance.in.get(0).parentSupernode!=null){
						nodeLeft=this.childrenSubnodes.get(2).outOfInstance.in.get(0).parentSupernode;
					}else{
						nodeLeft.addChildSubnode(this.childrenSubnodes.get(2).outOfInstance.in.get(0));
					}
					if(this.childrenSubnodes.get(2).outOfInstance.in.get(0)==this.childrenSubnodes.get(2).outOfInstance.in.get(1)){
						nodeRight=nodeLeft;
					}else{
						if(this.childrenSubnodes.get(2).outOfInstance.in.get(1).parentSupernode!=null){
							nodeRight=this.childrenSubnodes.get(2).outOfInstance.in.get(1).parentSupernode;
						}else{
							nodeRight.addChildSubnode(this.childrenSubnodes.get(2).outOfInstance.in.get(1));
						}
					}
					instancesToRemove.add(this.childrenSubnodes.get(2).outOfInstance);
					Node[] nodes={nodeLeft,nodeRight,this};
					Instance fusedInstance = this.definition.add(nandDefinition, nodes);
					this.definition.instances.get(fusedInstance.depth).remove(fusedInstance);
					fusedInstance.depth=realDepth;
					if(this.definition.instances.size()<realDepth+1){
						this.definition.instances.add(new TreeSet<Instance>());
					}
					this.definition.instances.get(realDepth).add(fusedInstance);
					for(Instance instanceToRemove:instancesToRemove){
						this.definition.removeInstance(instanceToRemove);
					}
					nodeLeft.fusion(expandedNodes);
					nodeRight.fusion(expandedNodes);
				}
			}
			if(this.parentSupernode!=null){
				this.parentSupernode.fusion(expandedNodes);
			}else if(!this.parentSubnodes.isEmpty()){
				for(Node parent:this.parentSubnodes){
					parent.fusion(expandedNodes);
				}
			}
		}
	}
	public void addOriginalRecursionIn(ArrayList<Node> recursiveIn) {
		if(!this.parentSubnodes.isEmpty()){
			for(Node parent:this.parentSubnodes){
				parent.addOriginalRecursionIn(recursiveIn);
			}
		}else{
			if(!recursiveIn.contains(this)) recursiveIn.add(this);
		}
	}
	public void addOriginalRecursionOut(ArrayList<Node> recursiveOut) {
		if(!recursiveOut.contains(this)) recursiveOut.add(this);
	}
}
