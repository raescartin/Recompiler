/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.TreeSet;

import utils.FixedBitSet;
//Each node can have 1 supernode parent and multiple parents, if a node has 1 parent it's a subnode of this one parent, if a node has multiple parents it's a supernode of these
//Each node can have supernode AND subnode children
//childrenSubnodes serve as indexes: if a node has subnode children, the leftmost represents the leftmost bit, the rightmost the rightmost bit and the middle one the rest
public class Node {
	public Node parentSupernode;
	public ArrayList<Node> parentSubnodes;//ArrayList, since there can be repetition
	public ArrayList<Node> childSubnodes;
	public ArrayList<Node> childSupernodes;
	public Instance outOfInstance;
	public Definition definition;
	//DEBUGGING ONLY
	public int idForDefinition;//id of node for the definition where it's used
	//END OF DEBUGGING ONLY
	
	public Node() { 
		this.parentSupernode=null;
		this.parentSubnodes = new ArrayList<Node>();
		this.childSubnodes = new ArrayList<Node>();
		this.childSupernodes = new ArrayList<Node>();
	}
	public Node addChildSupernode(Node childSuperNode) {//add children supernode to node	
		childSuperNode.parentSubnodes.add(this);
		this.childSupernodes.add(childSuperNode);
		if(this.definition!=null){
			this.definition.add(childSuperNode);
		}
		if(childSuperNode.definition!=null){
			childSuperNode.definition.add(this);
		}
		return childSuperNode;
	}
	public NandNode toNands(HashMap<NandNode, Node> nandToNode,HashMap<Node, NandNode> nodeToNand, NandForest nandForest,HashMap<Node, Node> equivalentNode) {
		//PRE: Node fission
		//Nodes are mapped up to down
		//tree is traversed down to up (so recursive calls needed)
		//evaluate node
		//-if a node is both in and out, there's no need to optimize it, nor take it to nandForest
		//-nand ins maybe set before toNands
		//-using "indexOf(this) seems unreliable
		
		NandNode nandNode;
		if(this.parentSubnodes.size()==1){
			nandNode=this.parentSubnodes.get(0).toNands(nandToNode, nodeToNand, nandForest, equivalentNode);
		}else{
			if(nodeToNand.containsKey(this)){//this map is to keep track of evaluated nodes
				nandNode=nodeToNand.get(this);//evaluated node
			}else{//node is out of an instance of NAND definition
				Node node1=this.outOfInstance.in.get(0);
				Node node2=this.outOfInstance.in.get(1);
				NandNode nandNode1=node1.toNands(nandToNode,nodeToNand,nandForest,equivalentNode);
				NandNode nandNode2=node2.toNands(nandToNode,nodeToNand,nandForest,equivalentNode);
				nandNode=nandForest.add(nandNode1,nandNode2);
				nodeToNand.put(this, nandNode);
				if(nandToNode.containsKey(nandNode)){
					equivalentNode.put(this,nandToNode.get(nandNode));
				}else{
					nandToNode.put(nandNode, this);
				}
				if(this.parentSupernode!=null){
					if(this.parentSupernode.childSubnodes.get(0).outOfInstance!=null){
						this.parentSupernode.childSubnodes.get(0).toNands(nandToNode, nodeToNand, nandForest, equivalentNode);
					}
					if(this.parentSupernode.childSubnodes.get(1).outOfInstance!=null&&this.parentSupernode.childSubnodes.get(1).childSubnodes.isEmpty()&&this.parentSupernode.childSubnodes.get(1).parentSubnodes.isEmpty()){
						this.parentSupernode.childSubnodes.get(1).toNands(nandToNode, nodeToNand, nandForest, equivalentNode);
					}
					if(this.parentSupernode.childSubnodes.get(2).outOfInstance!=null){
						this.parentSupernode.childSubnodes.get(2).toNands(nandToNode, nodeToNand, nandForest, equivalentNode);
					}
				}
			}
		}
		return nandNode;
	}
	public String toString() {
		String string = new String();
		if(this.parentSupernode!=null){
			string+=this.parentSupernode.toString();
			string+="{";
			string+=this.parentSupernode.childSubnodes.indexOf(this);
			string+="}";
		}else{
			string+=this.idForDefinition;
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
		if(this.childSubnodes.isEmpty()){
			if(!nodeToNand.containsKey(this)){
				NandNode nandNode = nandForest.addIn();
				nandToNode.put(nandNode, this);
				nodeToNand.put(this, nandNode);
			}
		}else{
			for(Node child:this.childSubnodes){
				child.mapInChildren(nandToNode,nodeToNand, nandForest);
			}
		}
	}
	public void mapInChildrenMapping(HashMap<Node, NandNode> nodeToNand, NandForest nandForest,ArrayList<Node> nandToNodeIn, HashMap<NandNode, Node> nandToNode) {		
		int subnodes=0;
		for(Node child:this.childSubnodes){
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
			NandForest nandForest, HashMap<Node, Node> equivalentNode) {
		NandNode nandNode;
		if(nodeToNand.containsKey(this)){
			nandNode = nandForest.setOut(nodeToNand.get(this));
		}else if(!this.parentSubnodes.isEmpty()){
			for(Node parent:this.parentSubnodes){
				parent.mapOutParents(nandToNode,nodeToNand, nandForest, equivalentNode);
			}
		}else if(this.outOfInstance!=null){
			nandNode = nandForest.setOut(this.toNands(nandToNode,nodeToNand,nandForest,equivalentNode));
		}
	}
	public void splitChildrenSubnodes() {
		//split in 3 children/subnodes
		if((this.parentSupernode!=null&&this.parentSupernode.childSubnodes.get(1)==this)||this.parentSupernode==null){//exclude indivisible nodes
			if(this.childSubnodes.isEmpty()){
				Node leftNode = new Node();
				Node centerNode = new Node();
				Node rightNode = new Node();
				this.addChildSubnode(leftNode);
				this.addChildSubnode(centerNode);
				this.addChildSubnode(rightNode);
			}
			if(!this.parentSubnodes.isEmpty()){ //if(node.parentSupernode!=null) ok ?
				if(this.childSubnodes.get(0).parentSubnodes.isEmpty()){
					this.parentSubnodes.get(0).findLeftChild(this.childSubnodes.get(1)).addChildSupernode(this.childSubnodes.get(0));
				}
				if(this.childSubnodes.get(1).parentSubnodes.isEmpty()){
					for(int i=1;i<this.parentSubnodes.size()-1;i++){
						this.parentSubnodes.get(i).addChildSupernode(this.childSubnodes.get(1));
					}
				}
				if(this.childSubnodes.get(2).parentSubnodes.isEmpty()){
					this.parentSubnodes.get(this.parentSubnodes.size()-1).findRightChild(this.childSubnodes.get(1)).addChildSupernode(this.childSubnodes.get(2));
				}
			}	
		}
	}
//	public void childrenFission(HashSet<Node> expandedNodes) {
//		//if out of nand has  children subnodes, separate in multiple nands
//		//TODO: check best recursion
//		if(!expandedNodes.contains(this)){
//			expandedNodes.add(this);
//			if(this.outOfInstance!=null){
//				Node parentLeftIn=this.outOfInstance.in.get(0);
//				Node parentRightIn=this.outOfInstance.in.get(1);
//				if(!this.childrenSubnodes.isEmpty()){
//					this.nandInsFission(expandedNodes);
//					parentLeftIn.childrenFission(expandedNodes);
//					parentRightIn.childrenFission(expandedNodes);
//				}else{
//					parentLeftIn.childrenFission(expandedNodes);
//					parentRightIn.childrenFission(expandedNodes);
//					if(!parentLeftIn.childrenSubnodes.isEmpty()&&!parentRightIn.childrenSubnodes.isEmpty()){
//						this.nandOutFission();
//					}
//				}
//			}
//			ArrayList<Node> parentNodes = new ArrayList<Node>();
//			if(!this.parentSubnodes.isEmpty()){
//				parentNodes.addAll(this.parentSubnodes);
//				for(Node parent:parentNodes){
//					parent.childrenFission(expandedNodes);
//				}
//			}else if(parentSupernode!=null){
//				parentSupernode.childrenFission(expandedNodes);
//			}
//		}
//	}
private void nandInsFission(HashSet<Node> expandedNodes) {
		expandedNodes.add(this);
		Node parentLeftIn=this.outOfInstance.in.get(0);
		Node parentRightIn=this.outOfInstance.in.get(1);
		parentLeftIn.splitChildrenSubnodes();
		Node parentLeftLeftChild=parentLeftIn.childSubnodes.get(0);
		Node parentLeftMidChild=parentLeftIn.childSubnodes.get(1);
		Node parentLeftRightChild=parentLeftIn.childSubnodes.get(2);
		parentRightIn.splitChildrenSubnodes();
		Node parentRightLeftChild=parentRightIn.childSubnodes.get(0);
		Node parentRightMidChild=parentRightIn.childSubnodes.get(1);
		Node parentRightRightChild=parentRightIn.childSubnodes.get(2);
		this.splitChildrenSubnodes();
		Node leftChild = this.childSubnodes.get(0);
		Node midChild = this.childSubnodes.get(1);
		Node rightChild = this.childSubnodes.get(2);
		if(leftChild.outOfInstance==null){
			Node[] nodes0={parentLeftLeftChild,parentRightLeftChild,leftChild};
			this.definition.add(this.outOfInstance.definition, nodes0);
		}
		if(midChild.outOfInstance==null){
			Node[] nodes1={parentLeftMidChild,parentRightMidChild,midChild};
			this.definition.add(this.outOfInstance.definition, nodes1);
		}
		if(rightChild.outOfInstance==null){
			Node[] nodes2={parentLeftRightChild,parentRightRightChild,rightChild};
			this.definition.add(this.outOfInstance.definition, nodes2);
		}
		if(!this.childSubnodes.get(1).childSubnodes.isEmpty()){
			this.childSubnodes.get(1).nandInsFission(expandedNodes);
		}
	}
private void nandOutFission() {
	Node parentLeftIn=this.outOfInstance.in.get(0);
	Node parentRightIn=this.outOfInstance.in.get(1);
	Node parentLeftLeftChild=parentLeftIn.childSubnodes.get(0);
	Node parentLeftMidChild=parentLeftIn.childSubnodes.get(1);
	Node parentLeftRightChild=parentLeftIn.childSubnodes.get(2);
	Node parentRightLeftChild=parentRightIn.childSubnodes.get(0);
	Node parentRightMidChild=parentRightIn.childSubnodes.get(1);
	Node parentRightRightChild=parentRightIn.childSubnodes.get(2);
	this.splitChildrenSubnodes();
	Node leftChild = this.childSubnodes.get(0);
	Node midChild = this.childSubnodes.get(1);
	Node rightChild = this.childSubnodes.get(2);
	if(leftChild.outOfInstance==null){
		Node[] nodes0={parentLeftLeftChild,parentRightLeftChild,leftChild};
		this.definition.add(this.outOfInstance.definition, nodes0);
	}
	if(midChild.outOfInstance==null){
		Node[] nodes1={parentLeftMidChild,parentRightMidChild,midChild};
		this.definition.add(this.outOfInstance.definition, nodes1);
	}
	if(rightChild.outOfInstance==null){
		Node[] nodes2={parentLeftRightChild,parentRightRightChild,rightChild};
		this.definition.add(this.outOfInstance.definition, nodes2);
	}
	if(!parentLeftIn.childSubnodes.get(1).childSubnodes.isEmpty()&&!parentRightIn.childSubnodes.get(1).childSubnodes.isEmpty()){
		this.childSubnodes.get(1).nandOutFission();
	}
}
//private ArrayList<Node> mapSubnodeChildren() {
//	Node leftChild;
//	Node midChild;
//	Node rightChild;
//	if(this.parentSubnodes.isEmpty()){ //if(node.parentSupernode!=null) ok ?
//		this.splitChildrenSubnodes();
//		leftChild=this.childrenSubnodes.get(0);
//		midChild=this.childrenSubnodes.get(1);
//		rightChild=this.childrenSubnodes.get(2);
//	}else{
//		midChild = new Node();
//		leftChild= this.parentSubnodes.get(0).findLeftChild(midChild);
//		for(int i=1;i<this.parentSubnodes.size()-1;i++){
//			this.parentSubnodes.get(i).addChildSupernode(midChild);
//		}
//		rightChild = this.parentSubnodes.get(this.parentSubnodes.size()-1).findRightChild(midChild);
//	}
//	ArrayList<Node> childSubnodes = new ArrayList<Node>();
//	childSubnodes.add(leftChild);
//	childSubnodes.add(midChild);
//	childSubnodes.add(rightChild);
//	return childSubnodes;
//}
//private ArrayList<Node> getChildrenSubnodes() {
//		//Must return an array of childrenSubnodes, since they are a representation of it's children,
//		//not necessarily it's children
//		ArrayList<Node> childrenSubnodes = new ArrayList<Node>();
//		if(!this.parentSubnodes.isEmpty()){
//			Node leftSubnode;
//			Node midSubnode;
//			Node rightSubnode;
//			midSubnode=new Node();
//			leftSubnode=this.parentSubnodes.get(0).findLeftChild(midSubnode);
//			for(int i=1;i<this.parentSubnodes.size()-1;i++){
//				this.parentSubnodes.get(i).addChildSupernode(midSubnode);
//			}
//			rightSubnode=this.parentSubnodes.get(this.parentSubnodes.size()-1).findRightChild(midSubnode);
//			childrenSubnodes.add(leftSubnode);
//			childrenSubnodes.add(midSubnode);
//			childrenSubnodes.add(rightSubnode);
//		}else if(this.parentSupernode!=null){//can't be an indivisible node //NEEDED?
//			Node parentMidNode = this.parentSupernode.getChildrenSubnodes().get(1);
//			if(parentMidNode==this){
//				this.splitChildrenSubnodes();
//				childrenSubnodes=this.childrenSubnodes;
//			}else{
//				childrenSubnodes=parentMidNode.getChildrenSubnodes();
//			}
//		}else{
//			this.splitChildrenSubnodes();
//			childrenSubnodes=this.childrenSubnodes;
//		}
//		return childrenSubnodes;
//	}
	public void parentsFission() {
		if(this.outOfInstance!=null){//out of nand
			this.biFission();
		}else{
			if(!this.parentSubnodes.isEmpty()){
			//if in of nand has  multiple parents, separate in multiple nands
				for(Node parent:this.parentSubnodes){
					parent.parentsFission();
				}
			}else if(this.parentSupernode!=null){
				this.parentSupernode.parentsFission();
				this.parentSupernode.splitChildrenSubnodes();
			}
		}
		
	}
//	void biFission() {
//			Node in0=this.outOfInstance.in.get(0);
//			Node in1=this.outOfInstance.in.get(1);
//			in0.parentsFission();
//			in1.parentsFission();
//			if(in0.parentSubnodes.size()==2
//			&&in1.parentSubnodes.size()==2&&this.parentSubnodes.isEmpty()
//			&&in0.parentSubnodes.get(0).parentSupernode==in0.parentSubnodes.get(1).parentSupernode
//			&&in1.parentSubnodes.get(0).parentSupernode==in1.parentSubnodes.get(1).parentSupernode){
//				Node newParentSupernode = new Node();
//				newParentSupernode.splitChildrenSubnodes();
//				if(in0.parentSubnodes.get(0).parentSupernode.childrenSubnodes.get(0)==in0.parentSubnodes.get(0)){
//					Node[] nodes0={in0.parentSubnodes.get(0),in1.parentSubnodes.get(0),newParentSupernode.childrenSubnodes.get(0)};
//					this.definition.add(this.outOfInstance.definition, nodes0);
//					newParentSupernode.childrenSubnodes.get(0).addChildSupernode(this);
//					newParentSupernode.childrenSubnodes.get(0).biFission();
//					Node[] nodes1={in0.parentSubnodes.get(1),in1.parentSubnodes.get(1),newParentSupernode.childrenSubnodes.get(1)};
//					this.definition.add(this.outOfInstance.definition, nodes1);
//					newParentSupernode.childrenSubnodes.get(1).addChildSupernode(this);
//					newParentSupernode.childrenSubnodes.get(1).biFission();
//				}else{
//					Node[] nodes0={in0.parentSubnodes.get(0),in1.parentSubnodes.get(0),newParentSupernode.childrenSubnodes.get(1)};
//					this.definition.add(this.outOfInstance.definition, nodes0);
//					newParentSupernode.childrenSubnodes.get(1).addChildSupernode(this);
//					newParentSupernode.childrenSubnodes.get(1).biFission();
//					Node[] nodes1={in0.parentSubnodes.get(1),in1.parentSubnodes.get(1),newParentSupernode.childrenSubnodes.get(2)};
//					this.definition.add(this.outOfInstance.definition, nodes1);
//					newParentSupernode.childrenSubnodes.get(2).addChildSupernode(this);
//					newParentSupernode.childrenSubnodes.get(2).biFission();
//				}
//			}
//		}
//	private void nandParentFission() {
//		Node in0=this.outOfInstance.in.get(0);
//		Node in1=this.outOfInstance.in.get(1);
//		in0.parentsFission();
//		in1.parentsFission();
//		if(!in0.parentSubnodes.isEmpty()||!in1.parentSubnodes.isEmpty()){
//			if(in0.parentSubnodes.size()==in1.parentSubnodes.size()&&this.parentSubnodes.isEmpty()){//Needed?
//				for(int i=0;i<in0.parentSubnodes.size();i++){//should be recursive into parents
//					Node newNode = new Node();
//					Node[] nodes={in0.parentSubnodes.get(i),in1.parentSubnodes.get(i),newNode};
//					this.definition.add(this.outOfInstance.definition, nodes);
//					newNode.addChildSupernode(this);
//					newNode.nandParentFission();
//				}
////				this.definition.removeInstance(this.outOfInstance);
//			}
//		}
//	}
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
		for(Node child:this.childSubnodes){
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
	public void nodeMapFission(Definition definition,
		HashSet<Node> originalNodes) {
		for(Node parent:this.parentSubnodes){
			if(!originalNodes.contains(parent)){
				originalNodes.add(parent);
				parent.nodeMapFission(definition,originalNodes);
			}
		}
		for(Node child:this.childSubnodes){
			if(!originalNodes.contains(child)){
				originalNodes.add(child);
				child.nodeMapFission(definition,originalNodes);
			}
		}
		
	}
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
			if(!this.parentSubnodes.isEmpty()){
				for(Node parent:this.parentSubnodes){
					parent.updateNode(definition,expandedNodes);
				}
			}else if(this.parentSupernode!=null){
				this.parentSupernode.updateNode(definition, expandedNodes);
			}else if(this.outOfInstance!=null){
				this.outOfInstance.updateInstance(definition, expandedNodes);
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
			}else if(!this.parentSubnodes.isEmpty()){//deal with subnodes and supernodes
				if(this.parentSubnodes.size()==2&&this.parentSubnodes.get(0).parentSupernode!=null&&this.parentSubnodes.get(1).parentSupernode!=null&&this.parentSubnodes.get(0).parentSupernode==this.parentSubnodes.get(1).parentSupernode){// "rest" selection
					this.parentSubnodes.get(0).parentSupernode.eval(valueMap, depth);
					if(valueMap.containsKey(this.parentSubnodes.get(0).parentSupernode)){
						if(this.parentSubnodes.get(0)==this.parentSubnodes.get(0).parentSupernode.childSubnodes.get(0)&&this.parentSubnodes.get(1)==this.parentSubnodes.get(0).parentSupernode.childSubnodes.get(1)){//consecutive nodes
							valueMap.put(this,valueMap.get(this.parentSubnodes.get(0).parentSupernode).get(0,valueMap.get(this.parentSubnodes.get(0).parentSupernode).length()-2));
						}else if(this.parentSubnodes.get(0)==this.parentSubnodes.get(0).parentSupernode.childSubnodes.get(1)&&this.parentSubnodes.get(1)==this.parentSubnodes.get(0).parentSupernode.childSubnodes.get(2)){
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
			}else if(this.parentSupernode!=null){
				this.parentSupernode.eval(valueMap, depth);
				if(valueMap.containsKey(this.parentSupernode)){
					if(parentSupernode.childSubnodes.get(0)==this){
						valueMap.put(this,valueMap.get(this.parentSupernode).get(0,0));
					}else if(parentSupernode.childSubnodes.get(1)==this){
						valueMap.put(this,valueMap.get(this.parentSupernode).get(1,valueMap.get(this.parentSupernode).length()-2));
					}else if(parentSupernode.childSubnodes.get(2)==this){
						valueMap.put(this,valueMap.get(this.parentSupernode).get(valueMap.get(this.parentSupernode).length()-1,valueMap.get(this.parentSupernode).length()-1));
					}
				}
			}
		}
		
	}
//	public void parentsFusion() {
//		//fusion of two nandInstances where outs are consecutive childrenSubnodes of a same node
//		if(this.outOfInstance!=null&&this.parentSubnodes.size()==2&&this.parentSubnodes.get(0).equivalentNode().parentSupernode!=null&&this.parentSubnodes.get(1).equivalentNode().parentSupernode!=null&&this.parentSubnodes.get(0).equivalentNode().parentSupernode==this.parentSubnodes.get(1).equivalentNode().parentSupernode){
//				Node leftNode = this.parentSubnodes.get(0).equivalentNode();
//				Node rightNode = this.parentSubnodes.get(1).equivalentNode();
//				leftNode.fusion();
//				rightNode.fusion();
//				if(leftNode.outOfInstance!=null&&rightNode.outOfInstance!=null
//				&&leftNode.parentSupernode!=null&&rightNode.parentSupernode!=null
//				&&leftNode.parentSupernode==rightNode.parentSupernode
//				&&leftNode.outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&rightNode.outOfInstance.in.get(0).equivalentNode().parentSupernode!=null
//				&&leftNode.outOfInstance.in.get(0).equivalentNode().parentSupernode==rightNode.outOfInstance.in.get(0).equivalentNode().parentSupernode
//				&&leftNode.outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&rightNode.outOfInstance.in.get(1).equivalentNode().parentSupernode!=null
//				&&leftNode.outOfInstance.in.get(1).equivalentNode().parentSupernode==rightNode.outOfInstance.in.get(1).equivalentNode().parentSupernode
//				){
//					this.definition.removeInstance(leftNode.outOfInstance);
//					this.definition.removeInstance(rightNode.outOfInstance);
//					this.parentSubnodes.clear();
//				}else if(leftNode.childSubnodes.isEmpty()&&!rightNode.childSubnodes.isEmpty()
//				&&leftNode.outOfInstance!=null&&rightNode.childSubnodes.get(0).outOfInstance!=null&&rightNode.childSubnodes.get(1).outOfInstance!=null&&rightNode.childSubnodes.get(0).outOfInstance!=null
//				&&leftNode.parentSupernode!=null&&rightNode.parentSupernode!=null
//				&&leftNode.parentSupernode==rightNode.parentSupernode
//				&&leftNode.outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&rightNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&rightNode.childSubnodes.get(1).outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&rightNode.childSubnodes.get(2).outOfInstance.in.get(0).equivalentNode().parentSupernode!=null
//				&&rightNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode==rightNode.childSubnodes.get(1).outOfInstance.in.get(0).equivalentNode().parentSupernode&&rightNode.childSubnodes.get(1).outOfInstance.in.get(0).equivalentNode().parentSupernode==rightNode.childSubnodes.get(2).outOfInstance.in.get(0).equivalentNode().parentSupernode
//				&&rightNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode.parentSupernode!=null
//				&&leftNode.outOfInstance.in.get(0).equivalentNode().parentSupernode==rightNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode.parentSupernode
//				&&leftNode.outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&rightNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&rightNode.childSubnodes.get(1).outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&rightNode.childSubnodes.get(2).outOfInstance.in.get(1).equivalentNode().parentSupernode!=null
//				&&rightNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode==rightNode.childSubnodes.get(1).outOfInstance.in.get(1).equivalentNode().parentSupernode&&rightNode.childSubnodes.get(1).outOfInstance.in.get(1).equivalentNode().parentSupernode==rightNode.childSubnodes.get(2).outOfInstance.in.get(1).equivalentNode().parentSupernode
//				&&rightNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode.parentSupernode!=null
//				&&leftNode.outOfInstance.in.get(1).equivalentNode().parentSupernode==rightNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode.parentSupernode
//				){
//					this.definition.removeInstance(leftNode.outOfInstance);
//					this.definition.removeInstance(rightNode.childSubnodes.get(0).outOfInstance);
//					this.definition.removeInstance(rightNode.childSubnodes.get(1).outOfInstance);
//					this.definition.removeInstance(rightNode.childSubnodes.get(2).outOfInstance);;
//					this.parentSubnodes.clear();
//				}else if(!leftNode.childSubnodes.isEmpty()&&rightNode.childSubnodes.isEmpty()
//				&&leftNode.childSubnodes.get(0).outOfInstance!=null&&leftNode.childSubnodes.get(1).outOfInstance!=null&&leftNode.childSubnodes.get(2).outOfInstance!=null&&rightNode.outOfInstance!=null
//				&&leftNode.parentSupernode!=null&&rightNode.parentSupernode!=null
//				&&leftNode.parentSupernode==rightNode.parentSupernode
//				&&rightNode.outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&leftNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&leftNode.childSubnodes.get(1).outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&leftNode.childSubnodes.get(2).outOfInstance.in.get(0).equivalentNode().parentSupernode!=null
//				&&leftNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode==leftNode.childSubnodes.get(1).outOfInstance.in.get(0).equivalentNode().parentSupernode&&leftNode.childSubnodes.get(1).outOfInstance.in.get(0).equivalentNode().parentSupernode==leftNode.childSubnodes.get(2).outOfInstance.in.get(0).equivalentNode().parentSupernode
//				&&leftNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode.parentSupernode!=null
//				&&rightNode.outOfInstance.in.get(0).equivalentNode().parentSupernode==leftNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode.parentSupernode
//				&&rightNode.outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&leftNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&leftNode.childSubnodes.get(1).outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&leftNode.childSubnodes.get(2).outOfInstance.in.get(1).equivalentNode().parentSupernode!=null
//				&&leftNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode==leftNode.childSubnodes.get(1).outOfInstance.in.get(1).equivalentNode().parentSupernode&&leftNode.childSubnodes.get(1).outOfInstance.in.get(1).equivalentNode().parentSupernode==leftNode.childSubnodes.get(2).outOfInstance.in.get(1).equivalentNode().parentSupernode
//				&&leftNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode.parentSupernode!=null
//				&&rightNode.outOfInstance.in.get(1).equivalentNode().parentSupernode==leftNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode.parentSupernode
//				){
//					this.definition.removeInstance(rightNode.outOfInstance);
//					this.definition.removeInstance(leftNode.childSubnodes.get(0).outOfInstance);
//					this.definition.removeInstance(leftNode.childSubnodes.get(1).outOfInstance);
//					this.definition.removeInstance(leftNode.childSubnodes.get(2).outOfInstance);
//					this.parentSubnodes.clear();
//				}else{
//					this.parentSupernode=null;
//					this.definition.removeInstance(this.outOfInstance);
//				}
//				System.out.println(this.toString());
//				System.out.println(this.definition.toString());
//		}else{
//			for(Node parentSubnode:this.parentSubnodes){
//				parentSubnode.fusion();
//			}
//		}
//	}
	public Node addChildSubnode(Node childSubnode) {
		childSubnode.parentSupernode=this;
		this.childSubnodes.add(childSubnode);
		if(this.definition!=null){
			this.definition.add(childSubnode);
		}
		if(childSubnode.definition!=null){
			childSubnode.definition.add(this);
		}
		return childSubnode;
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
	public int getDepth(HashSet<Node> evaluatedNodes) {
		int depth=-1;
		if(!evaluatedNodes.contains(this)){
			evaluatedNodes.add(this);
			if(this.outOfInstance!=null){
				depth=this.outOfInstance.depth;
			}else{
				if(!this.parentSubnodes.isEmpty()){
					for(Node parent:this.parentSubnodes){
						int parentDepth=parent.getDepth(evaluatedNodes);
						if(parentDepth>depth){
							depth=parentDepth;
						}
					}
				}
				if(this.parentSupernode!=null){
					int parentDepth=this.parentSupernode.getDepth(evaluatedNodes);
					if(parentDepth>depth){
						depth=parentDepth;
					}
				}else if(!this.childSubnodes.isEmpty()){
					for(Node child:this.childSubnodes){
						int childrenDepth=child.getDepth(evaluatedNodes);
						if(childrenDepth>depth){
							depth=childrenDepth;
						}
					}
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
			if(this.parentSupernode.childSubnodes.get(0)==this||this.parentSupernode.childSubnodes.get(2)==this){
				//indivisible node
				leftChild=this;
			}else{
				this.splitChildrenSubnodes();
				leftChild=this.childSubnodes.get(0);
				this.childSubnodes.get(1).addChildSupernode(midChild);
				this.childSubnodes.get(2).addChildSupernode(midChild);
			}
		}else{
			this.splitChildrenSubnodes();
			leftChild=this.childSubnodes.get(0);
			this.childSubnodes.get(1).addChildSupernode(midChild);
			this.childSubnodes.get(2).addChildSupernode(midChild);
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
			if(this.parentSupernode.childSubnodes.get(0)==this||this.parentSupernode.childSubnodes.get(2)==this){
				//indivisible node
				rightChild=this;
			}else{
				this.splitChildrenSubnodes();
				this.childSubnodes.get(0).addChildSupernode(midChild);
				this.childSubnodes.get(1).addChildSupernode(midChild);
				rightChild=this.childSubnodes.get(2);
			}
		}else{
			this.splitChildrenSubnodes();
			this.childSubnodes.get(0).addChildSupernode(midChild);
			this.childSubnodes.get(1).addChildSupernode(midChild);
			rightChild=this.childSubnodes.get(2);
		}
		return rightChild;
	}
//	public void fusion(HashSet<Node> expandedNodes) {
//		//fusion of three nandInstances where outs are the three childrenSubnodes of a same node
//		//TODO: simplify
//		if(!expandedNodes.contains(this)){
//			expandedNodes.add(this);
//			if(!this.childrenSubnodes.isEmpty()){
//				if(this.childrenSubnodes.get(0).outOfInstance!=null&&this.childrenSubnodes.get(1).outOfInstance!=null&&this.childrenSubnodes.get(2).outOfInstance!=null){
//					Definition nandDefinition = null;
//					Node nodeLeft = new Node();
//					Node nodeRight= new Node();
//					HashSet<Instance> instancesToRemove = new HashSet<Instance>();
//					nandDefinition=this.childrenSubnodes.get(0).outOfInstance.definition;
//					if(this.childrenSubnodes.get(0).outOfInstance.in.get(0).parentSupernode!=null){
//						nodeLeft=this.childrenSubnodes.get(0).outOfInstance.in.get(0).parentSupernode;
//					}else{
//						nodeLeft.addChildSubnode(this.childrenSubnodes.get(0).outOfInstance.in.get(0));
//					}
//					if(this.childrenSubnodes.get(0).outOfInstance.in.get(0)==this.childrenSubnodes.get(0).outOfInstance.in.get(1)){
//						nodeRight=nodeLeft;
//					}else{
//						if(this.childrenSubnodes.get(0).outOfInstance.in.get(1).parentSupernode!=null){
//							nodeRight=this.childrenSubnodes.get(0).outOfInstance.in.get(1).parentSupernode;
//						}else{
//							nodeRight.addChildSubnode(this.childrenSubnodes.get(0).outOfInstance.in.get(1));
//						}
//					}
//					instancesToRemove.add(this.childrenSubnodes.get(0).outOfInstance);
//					nandDefinition=this.childrenSubnodes.get(1).outOfInstance.definition;
//					if(this.childrenSubnodes.get(1).outOfInstance.in.get(0).parentSupernode!=null){
//						nodeLeft=this.childrenSubnodes.get(1).outOfInstance.in.get(0).parentSupernode;
//					}else{
//						nodeLeft.addChildSubnode(this.childrenSubnodes.get(1).outOfInstance.in.get(0));
//					}
//					if(this.childrenSubnodes.get(1).outOfInstance.in.get(0)==this.childrenSubnodes.get(1).outOfInstance.in.get(1)){
//						nodeRight=nodeLeft;
//					}else{
//						if(this.childrenSubnodes.get(1).outOfInstance.in.get(1).parentSupernode!=null){
//							nodeRight=this.childrenSubnodes.get(1).outOfInstance.in.get(1).parentSupernode;
//						}else{
//							nodeRight.addChildSubnode(this.childrenSubnodes.get(1).outOfInstance.in.get(1));
//						}
//					}
//					instancesToRemove.add(this.childrenSubnodes.get(1).outOfInstance);
//					nandDefinition=this.childrenSubnodes.get(2).outOfInstance.definition;
//					if(this.childrenSubnodes.get(2).outOfInstance.in.get(0).parentSupernode!=null){
//						nodeLeft=this.childrenSubnodes.get(2).outOfInstance.in.get(0).parentSupernode;
//					}else{
//						nodeLeft.addChildSubnode(this.childrenSubnodes.get(2).outOfInstance.in.get(0));
//					}
//					if(this.childrenSubnodes.get(2).outOfInstance.in.get(0)==this.childrenSubnodes.get(2).outOfInstance.in.get(1)){
//						nodeRight=nodeLeft;
//					}else{
//						if(this.childrenSubnodes.get(2).outOfInstance.in.get(1).parentSupernode!=null){
//							nodeRight=this.childrenSubnodes.get(2).outOfInstance.in.get(1).parentSupernode;
//						}else{
//							nodeRight.addChildSubnode(this.childrenSubnodes.get(2).outOfInstance.in.get(1));
//						}
//					}
//					instancesToRemove.add(this.childrenSubnodes.get(2).outOfInstance);
//					Node[] nodes={nodeLeft,nodeRight,this};
//					Instance fusedInstance = this.definition.add(nandDefinition, nodes);
//					for(Instance instanceToRemove:instancesToRemove){
//						this.definition.removeInstance(instanceToRemove);
//					}
//					nodeLeft.fusion(expandedNodes);
//					nodeRight.fusion(expandedNodes);
//				}
//			}
//			if(this.parentSupernode!=null){
//				this.parentSupernode.fusion(expandedNodes);
//			}else if(!this.parentSubnodes.isEmpty()){
//				for(Node parent:this.parentSubnodes){
//					parent.fusion(expandedNodes);
//				}
//			}
//		}
//	}
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
	public void extractIOsubnodes(HashSet<Node> evaluatedNodes, ArrayList<Node> recursiveIn,
			ArrayList<Node> recursiveOut, HashSet<Node> originalNodes) {
		if(!evaluatedNodes.contains(this)){
			evaluatedNodes.add(this);
			if(this.outOfInstance!=null){//should have preference over everything //this preference may cause problems with fusion so maybe have to check for children here
				if(originalNodes.contains(this)){
					if(!originalNodes.contains(this.outOfInstance.in.get(0))||!originalNodes.contains(this.outOfInstance.in.get(1))){
						recursiveOut.add(this);
						this.outOfInstance.in.get(0).extractIn(recursiveIn, originalNodes);
						this.outOfInstance.in.get(1).extractIn(recursiveIn, originalNodes);
					}
				}else{//new out node
					this.outOfInstance.in.get(0).extractIn(recursiveIn, originalNodes);
					this.outOfInstance.in.get(1).extractIn(recursiveIn, originalNodes);
				}
				this.outOfInstance.in.get(0).extractIOsubnodes(evaluatedNodes, recursiveIn, recursiveOut, originalNodes);
				this.outOfInstance.in.get(1).extractIOsubnodes(evaluatedNodes, recursiveIn, recursiveOut, originalNodes);
			}else if(!this.parentSubnodes.isEmpty()){
				for(Node parentSubnode:this.parentSubnodes){
					parentSubnode.extractIOsubnodes(evaluatedNodes,recursiveIn, recursiveOut, originalNodes);
				}
			}else if(this.parentSupernode!=null){
				for(Node childSubnode:this.parentSupernode.childSubnodes){
					childSubnode.extractIOsubnodes(evaluatedNodes,recursiveIn, recursiveOut, originalNodes);
				}
			}
		}
	}
	void extractIn(ArrayList<Node> recursiveIn, HashSet<Node> originalNodes) {
		//test
		if(originalNodes.contains(this)){
			if(!recursiveIn.contains(this)) recursiveIn.add(this);
		}else{
			for(Node parentSubnode:this.parentSubnodes){
				parentSubnode.extractIn(recursiveIn, originalNodes);
			}
		}
	}
	public void extractIOchildrenSupernodes(HashSet<Node> evaluatedNodes, ArrayList<Node> recursiveIn,
			ArrayList<Node> recursiveOut, HashSet<Node> originalNodes) {
		if(!evaluatedNodes.contains(this)){
			evaluatedNodes.add(this);
			if(this.outOfInstance!=null){//should have preference over everything //this preference may cause problems with fusion so maybe have to check for children here
				for(Node inNode:this.outOfInstance.in){
					inNode.extractIOchildrenSupernodes(evaluatedNodes, recursiveIn, recursiveOut, originalNodes);
				}
			}else if(!this.parentSubnodes.isEmpty()){
				for(Node parentSubnode:this.parentSubnodes){
					parentSubnode.extractIOchildrenSupernodes(evaluatedNodes, recursiveIn, recursiveOut, originalNodes);
				}
				if(recursiveIn.containsAll(this.parentSubnodes)){
					recursiveIn.removeAll(this.parentSubnodes);
					recursiveIn.add(this);
				}else if(recursiveOut.containsAll(this.parentSubnodes)){
					recursiveOut.removeAll(this.parentSubnodes);
					recursiveOut.add(this);
				}
			}else if(this.parentSupernode!=null){
				for(Node childSubnode:this.parentSupernode.childSubnodes){
					childSubnode.extractIOchildrenSupernodes(evaluatedNodes, recursiveIn, recursiveOut, originalNodes);
				}
			}
		}
	}
	public boolean extractIOparentSupernodes(ArrayList<Node> recursiveIn) {
		if(this.parentSupernode!=null){
			if(recursiveIn.containsAll(this.parentSupernode.childSubnodes)){
				recursiveIn.removeAll(this.parentSupernode.childSubnodes);
				recursiveIn.add(this.parentSupernode);
				return true;
			}
		}
		return false;
	}

	private void fusion(Node nodeLeft, Node nodeRight) {
		if(!this.parentSubnodes.isEmpty()&&this.parentSubnodes.size()==nodeLeft.parentSubnodes.size()&&this.parentSubnodes.size()==nodeRight.parentSubnodes.size()){
			HashSet<Instance> instancesToRemove = new HashSet<Instance>();
			boolean allSupernodes=true;
			for(int i=0;i<this.parentSubnodes.size();i++){
				if(this.parentSubnodes.get(i).outOfInstance!=null
						&&this.parentSubnodes.get(i).outOfInstance.in.get(0)==nodeLeft.parentSubnodes.get(i)
						&&this.parentSubnodes.get(i).outOfInstance.in.get(1)==nodeRight.parentSubnodes.get(i)){
					instancesToRemove.add(this.parentSubnodes.get(i).outOfInstance);
				}else{
					allSupernodes=false;
				}
			}
			if(allSupernodes){
				Definition nandDefinition = this.parentSubnodes.get(0).outOfInstance.definition;
				Node[] nodes={nodeLeft,nodeRight,this};
				this.definition.add(nandDefinition, nodes);
				for(Instance instanceToRemove:instancesToRemove){
					this.definition.removeInstance(instanceToRemove);
				}
			}
		}
	}
	public void replaceNodes(HashSet<Node> expandedNodes,HashMap<Node, Node> equivalentNodes) {
		if(!expandedNodes.contains(this)){
			expandedNodes.add(this);
			if(this.parentSupernode!=null){
				if(equivalentNodes.containsKey(this.parentSupernode)){
					Node newSupernode=equivalentNodes.get(this.parentSupernode);
					newSupernode.childSubnodes.set(this.parentSupernode.childSubnodes.indexOf(this), this);
					this.parentSupernode=newSupernode;
				}
				this.parentSupernode.replaceNodes(expandedNodes, equivalentNodes);
			}
			if(!this.parentSubnodes.isEmpty()){
				for(int i=0;i<this.parentSubnodes.size();i++){
					if(equivalentNodes.containsKey(this.parentSubnodes.get(i))){
						this.parentSubnodes.set(i, equivalentNodes.get(this.parentSubnodes.get(i)));
						this.parentSubnodes.get(i).childSupernodes.add(this);
					}
					this.parentSubnodes.get(i).replaceNodes(expandedNodes, equivalentNodes);
				}
			}
			if(this.outOfInstance!=null){
				if(equivalentNodes.containsKey(this.outOfInstance.in.get(0))){
//					equivalentNodes.get(this.outOfInstance.in.get(0)).replaceNodes(expandedNodes, equivalentNodes);
					Instance instance=this.outOfInstance;
					this.definition.removeInstance(instance);
					instance.in.set(0, equivalentNodes.get(instance.in.get(0)));
					this.definition.add(instance);
				}else{
					this.outOfInstance.in.get(0).replaceNodes(expandedNodes, equivalentNodes);
				}
				if(equivalentNodes.containsKey(this.outOfInstance.in.get(1))){
//					equivalentNodes.get(this.outOfInstance.in.get(1)).replaceNodes(expandedNodes, equivalentNodes);
					Instance instance=this.outOfInstance;
					this.definition.removeInstance(instance);
					instance.in.set(1, equivalentNodes.get(instance.in.get(1)));
					this.definition.add(instance);
				}else{
					this.outOfInstance.in.get(1).replaceNodes(expandedNodes, equivalentNodes);
				}
			}
		}
	}
	public void clean(HashSet<Node> expandedNodes,ArrayList<Instance> instancesToKeep) {
		if(!expandedNodes.contains(this)){
			expandedNodes.add(this);
			if(this.parentSupernode!=null){
				this.parentSupernode.clean(expandedNodes,instancesToKeep);
			}else if(!this.parentSubnodes.isEmpty()){
				for(int i=0;i<this.parentSubnodes.size();i++){
					this.parentSubnodes.get(i).clean(expandedNodes, instancesToKeep);
				}
			}else if(this.outOfInstance!=null){
				instancesToKeep.add(this.outOfInstance);
				this.outOfInstance.in.get(0).clean(expandedNodes, instancesToKeep);
				this.outOfInstance.in.get(1).clean(expandedNodes, instancesToKeep);
				this.parentSubnodes.clear();
			}
		}
	}
//	public void recoverParentSupernodes(HashSet<Node> expandedNodes) {
//		//fuse 3 nands to recover parentSupernode
//		if(!expandedNodes.contains(this)){
//			expandedNodes.add(this);
//			if(!this.childrenSubnodes.isEmpty()){
//				if(this.childrenSubnodes.get(0).outOfInstance!=null&&this.childrenSubnodes.get(2).outOfInstance!=null
//					&&this.childrenSubnodes.get(0).outOfInstance.in.get(0).parentSupernode!=null&&this.childrenSubnodes.get(2).outOfInstance.in.get(0).parentSupernode!=null
//					&&this.childrenSubnodes.get(0).outOfInstance.in.get(0).parentSupernode==this.childrenSubnodes.get(2).outOfInstance.in.get(0).parentSupernode
//					&&this.childrenSubnodes.get(0).outOfInstance.in.get(1).parentSupernode!=null&&this.childrenSubnodes.get(2).outOfInstance.in.get(1).parentSupernode!=null
//					&&this.childrenSubnodes.get(0).outOfInstance.in.get(1).parentSupernode==this.childrenSubnodes.get(2).outOfInstance.in.get(1).parentSupernode){
//					Node nodeLeft = this.childrenSubnodes.get(0).outOfInstance.in.get(0).parentSupernode;
//					Node nodeRight= this.childrenSubnodes.get(0).outOfInstance.in.get(1).parentSupernode;
//					this.childrenSubnodes.get(1).recoverParentSupernodes(expandedNodes);
//					this.childrenSubnodes.get(1).fusion(nodeLeft.childrenSubnodes.get(1),nodeRight.childrenSubnodes.get(1));
//					if(this.childrenSubnodes.get(1).outOfInstance!=null
//						&&this.childrenSubnodes.get(1).outOfInstance.in.get(0).parentSupernode!=null
//						&&this.childrenSubnodes.get(0).outOfInstance.in.get(0).parentSupernode==this.childrenSubnodes.get(1).outOfInstance.in.get(0).parentSupernode
//						&&this.childrenSubnodes.get(0).outOfInstance.in.get(1).parentSupernode!=null
//						&&this.childrenSubnodes.get(0).outOfInstance.in.get(1).parentSupernode==this.childrenSubnodes.get(1).outOfInstance.in.get(1).parentSupernode){
//						HashSet<Instance> instancesToRemove = new HashSet<Instance>();
//						instancesToRemove.add(this.childrenSubnodes.get(0).outOfInstance);
//						instancesToRemove.add(this.childrenSubnodes.get(1).outOfInstance);
//						instancesToRemove.add(this.childrenSubnodes.get(2).outOfInstance);
//						for(Instance instanceToRemove:instancesToRemove){
//							this.definition.removeInstance(instanceToRemove);
//						}
//						this.parentSubnodes.clear();
//						nodeLeft.recoverParentSupernodes(expandedNodes);
//						nodeRight.recoverParentSupernodes(expandedNodes);
//					}
//				}
//			}
//			if(this.parentSupernode!=null){
//				this.parentSupernode.recoverParentSupernodes(expandedNodes);
//			}else if(!this.parentSubnodes.isEmpty()){
//				for(Node parent:this.parentSubnodes){
//					parent.recoverParentSupernodes(expandedNodes);
//				}
//			}
//		}
//		
//	}
//	public void prune() {
//		//removes unneeded bifusion instances
//		if(!this.parentSubnodes.isEmpty()){
//			if(this.outOfInstance!=null){
//				this.definition.removeInstance(this.outOfInstance);
//			}
//			for(Node parentSubnode:this.parentSubnodes){
//				parentSubnode.prune();
//			}
//		}else if(this.outOfInstance!=null){
//			this.outOfInstance.in.get(0).prune();
//			this.outOfInstance.in.get(1).prune();
//		}
//		if(this.parentSupernode!=null){
//			this.parentSupernode.prune();
//		}
//	}
	public void triFission() {
		//TODO: maybe need to fix nandIn mapping on nandTree, since this uses only needed subnodes
		if(this.parentSupernode!=null){
			if(this.parentSupernode.outOfInstance==null){
				this.parentSupernode.triFission();
			}
			if(this.parentSupernode.outOfInstance!=null){
				this.parentSupernode.outOfInstance.in.get(0).splitChildrenSubnodes();
				this.parentSupernode.outOfInstance.in.get(1).splitChildrenSubnodes();
				this.addEquivalentSubnodeInstance(this.parentSupernode.outOfInstance.in.get(0).childSubnodes.get(0),this.parentSupernode.outOfInstance.in.get(1).childSubnodes.get(0),this.parentSupernode.childSubnodes.get(0));				
				if(this.parentSupernode.childSubnodes.get(1).outOfInstance==null){
					Node[] nodes1={this.parentSupernode.outOfInstance.in.get(0).childSubnodes.get(1),this.parentSupernode.outOfInstance.in.get(1).childSubnodes.get(1),this.parentSupernode.childSubnodes.get(1)};
					this.definition.add(this.parentSupernode.outOfInstance.definition, nodes1);
				}
				this.addEquivalentSubnodeInstance(this.parentSupernode.outOfInstance.in.get(0).childSubnodes.get(2),this.parentSupernode.outOfInstance.in.get(1).childSubnodes.get(2),this.parentSupernode.childSubnodes.get(2));
				this.parentSupernode.outOfInstance.in.get(0).childSubnodes.get(1).triFission();
				this.parentSupernode.outOfInstance.in.get(1).childSubnodes.get(1).triFission();
			}
		}
		for(Node parentSubnode:this.parentSubnodes){
			parentSubnode.triFission();
		}
	}
	private void addEquivalentSubnodeInstance(Node in0, Node in1, Node out) {
		if(out.parentSubnodes.size()==1){
			addEquivalentSubnodeInstance(in0,in1,out.parentSubnodes.get(0));
		}else if(out.outOfInstance==null){
			Node[] nodes={in0,in1,out};
			this.definition.add(this.parentSupernode.outOfInstance.definition, nodes);
		}
	}
	public void expandBinodes(){
		if(this.parentSupernode!=null){
			this.parentSupernode.expandBinodes();
			this.parentSupernode.splitChildrenSubnodes();
		}
		for(Node parentSubnode:this.parentSubnodes){
			parentSubnode.expandBinodes();
		}
		if(this.outOfInstance!=null){
			this.outOfInstance.in.get(0).expandBinodes();
			this.outOfInstance.in.get(1).expandBinodes();
			this.expandBinodeSubnodeParents(this.outOfInstance.in.get(0),this.outOfInstance.in.get(1));
//			if(this.outOfInstance.in.get(0).parentSubnodes.size()==2&&this.outOfInstance.in.get(1).parentSubnodes.size()==2
//			&&this.parentSubnodes.isEmpty()
//			&&this.outOfInstance.in.get(0).parentSubnodes.get(0).parentSupernode==this.outOfInstance.in.get(0).parentSubnodes.get(1).parentSupernode
//			&&this.outOfInstance.in.get(1).parentSubnodes.get(0).parentSupernode==this.outOfInstance.in.get(1).parentSubnodes.get(1).parentSupernode){
//				Node newParentSupernode = new Node();
//				newParentSupernode.splitChildrenSubnodes();
//				if(this.outOfInstance.in.get(0).parentSubnodes.get(1).parentSupernode.childrenSubnodes.get(2)==this.outOfInstance.in.get(0).parentSubnodes.get(1)
//				&&this.outOfInstance.in.get(1).parentSubnodes.get(1).parentSupernode.childrenSubnodes.get(2)==this.outOfInstance.in.get(1).parentSubnodes.get(1)){
//					newParentSupernode.childrenSubnodes.get(1).addChildSupernode(this);
//					newParentSupernode.childrenSubnodes.get(2).addChildSupernode(this);
//					newParentSupernode.childrenSubnodes.get(1).expandBinodes();
//					newParentSupernode.childrenSubnodes.get(2).expandBinodes();
//				}else{
//					newParentSupernode.childrenSubnodes.get(0).addChildSupernode(this);
//					newParentSupernode.childrenSubnodes.get(1).addChildSupernode(this);
//					this.parentSubnodes.get(0).expandBinodeSubnodeParents(this.outOfInstance.in.get(0).parentSubnodes.get(0),this.outOfInstance.in.get(1).parentSubnodes.get(0));
//					this.parentSubnodes.get(0).expandBinodeSubnodeParents(this.outOfInstance.in.get(0).parentSubnodes.get(1),this.outOfInstance.in.get(1).parentSubnodes.get(1));
//				}
//			}
		}
	}

	private void expandBinodeSubnodeParents(Node in0, Node in1) {
		if(in0.parentSubnodes.size()==2&&in1.parentSubnodes.size()==2
				&&this.parentSubnodes.isEmpty()
				&&in0.parentSubnodes.get(0).parentSupernode==in0.parentSubnodes.get(1).parentSupernode
				&&in1.parentSubnodes.get(0).parentSupernode==in1.parentSubnodes.get(1).parentSupernode){
					Node newParentSupernode = new Node();
					newParentSupernode.splitChildrenSubnodes();
					if(in0.parentSubnodes.get(1).parentSupernode.childSubnodes.get(2)==in0.parentSubnodes.get(1)
					&&in1.parentSubnodes.get(1).parentSupernode.childSubnodes.get(2)==in1.parentSubnodes.get(1)){
						newParentSupernode.childSubnodes.get(1).addChildSupernode(this);
						newParentSupernode.childSubnodes.get(2).addChildSupernode(this);
						this.parentSubnodes.get(0).expandBinodeSubnodeParents(in0.parentSubnodes.get(0),in1.parentSubnodes.get(0));
						this.parentSubnodes.get(0).expandBinodeSubnodeParents(in0.parentSubnodes.get(1),in1.parentSubnodes.get(1));
					}else{
						newParentSupernode.childSubnodes.get(0).addChildSupernode(this);
						newParentSupernode.childSubnodes.get(1).addChildSupernode(this);
						this.parentSubnodes.get(0).expandBinodeSubnodeParents(in0.parentSubnodes.get(0),in1.parentSubnodes.get(0));
						this.parentSubnodes.get(0).expandBinodeSubnodeParents(in0.parentSubnodes.get(1),in1.parentSubnodes.get(1));
					}
		}
	}
	public void biFission() {
		if(this.parentSupernode!=null){
			this.parentSupernode.biFission();
		}
		for(Node parentSubnode:this.parentSubnodes){
			parentSubnode.biFission();
		}
		if(this.outOfInstance!=null){
			this.outOfInstance.in.get(0).biFission();
			this.outOfInstance.in.get(1).biFission();
			if(this.outOfInstance.in.get(0).parentSubnodes.size()==2&&this.outOfInstance.in.get(1).parentSubnodes.size()==2&&this.parentSubnodes.size()==2
			&&this.outOfInstance.in.get(0).parentSubnodes.get(0).parentSupernode==this.outOfInstance.in.get(0).parentSubnodes.get(1).parentSupernode
			&&this.outOfInstance.in.get(1).parentSubnodes.get(0).parentSupernode==this.outOfInstance.in.get(1).parentSubnodes.get(1).parentSupernode
			&&this.parentSubnodes.get(0).parentSupernode==this.parentSubnodes.get(1).parentSupernode
			&&this.parentSubnodes.get(0).outOfInstance==null){
//				if(this.outOfInstance.in.get(0).parentSubnodes.get(0).parentSupernode.childSubnodes.get(0)==this.outOfInstance.in.get(0).parentSubnodes.get(0)
//				&&this.outOfInstance.in.get(1).parentSubnodes.get(0).parentSupernode.childSubnodes.get(0)==this.outOfInstance.in.get(1).parentSubnodes.get(0)){
					if(this.childSubnodes.isEmpty()){//if node has been triFissed, subnodes already exist
						Node[] nodes0={this.outOfInstance.in.get(0).parentSubnodes.get(0),this.outOfInstance.in.get(1).parentSubnodes.get(0),this.parentSubnodes.get(0)};
						this.definition.add(this.outOfInstance.definition, nodes0);
						Node[] nodes1={this.outOfInstance.in.get(0).parentSubnodes.get(1),this.outOfInstance.in.get(1).parentSubnodes.get(1),this.parentSubnodes.get(1)};
						this.definition.add(this.outOfInstance.definition, nodes1);
					}
					this.parentSubnodes.get(0).biFission();
					this.parentSubnodes.get(1).biFission();
//				}else if(this.outOfInstance.in.get(0).parentSubnodes.get(1).parentSupernode.childSubnodes.get(2)==this.outOfInstance.in.get(0).parentSubnodes.get(1)
//				&&this.outOfInstance.in.get(1).parentSubnodes.get(1).parentSupernode.childSubnodes.get(2)==this.outOfInstance.in.get(1).parentSubnodes.get(1)){
//					if(this.childSubnodes.isEmpty()){//this.childrenSubnodes.isEmpty() is enough or need to check coming from childrenSubnode to parentSupernode?
//						Node[] nodes0={this.outOfInstance.in.get(0).parentSubnodes.get(0),this.outOfInstance.in.get(1).parentSubnodes.get(0),this.parentSubnodes.get(0)};
//						this.definition.add(this.outOfInstance.definition, nodes0);
//						Node[] nodes1={this.outOfInstance.in.get(0).parentSubnodes.get(1),this.outOfInstance.in.get(1).parentSubnodes.get(1),this.parentSubnodes.get(1)};
//						this.definition.add(this.outOfInstance.definition, nodes1);
//					}
//					this.parentSubnodes.get(0).biFission();
//					this.parentSubnodes.get(1).biFission();
//				}else if(this.outOfInstance.in.get(0).parentSubnodes.get(0).parentSupernode.childSubnodes.get(0)==this.outOfInstance.in.get(0).parentSubnodes.get(0)
//				&&this.outOfInstance.in.get(1).parentSubnodes.get(1).parentSupernode.childSubnodes.get(2)==this.outOfInstance.in.get(1).parentSubnodes.get(1)){
////					if(this.parentSubnodes.get(0).outOfInstance==null){
//						if(this.childSubnodes.isEmpty()){
//							Node[] nodes0={this.outOfInstance.in.get(0).parentSubnodes.get(0),this.outOfInstance.in.get(1).parentSubnodes.get(0),this.parentSubnodes.get(0)};
//							this.definition.add(this.outOfInstance.definition, nodes0);
//							Node[] nodes1={this.outOfInstance.in.get(0).parentSubnodes.get(1),this.outOfInstance.in.get(1).parentSubnodes.get(1),this.parentSubnodes.get(1)};
//							this.definition.add(this.outOfInstance.definition, nodes1);
//							this.parentSubnodes.get(0).biFission();
//							this.parentSubnodes.get(1).biFission();
//						}else{
////							this.outOfInstance.in.get(1).parentSubnodes.get(0).splitChildrenSubnodes();
////							this.outOfInstance.in.get(0).parentSubnodes.get(1).splitChildrenSubnodes();
////							this.parentSubnodes.get(1).splitChildrenSubnodes();
////							Node[] nodes0={this.outOfInstance.in.get(0).parentSubnodes.get(0),this.outOfInstance.in.get(1).parentSubnodes.get(0).childSubnodes.get(0),this.parentSubnodes.get(0)};
////							this.definition.add(this.outOfInstance.definition, nodes0);
////							Node[] nodes1={this.outOfInstance.in.get(0).parentSubnodes.get(1).childSubnodes.get(0),this.outOfInstance.in.get(1).parentSubnodes.get(0).childSubnodes.get(1),this.parentSubnodes.get(1).childSubnodes.get(0)};
////							this.definition.add(this.outOfInstance.definition, nodes1);
////							Node[] nodes2={this.outOfInstance.in.get(0).parentSubnodes.get(1).childSubnodes.get(1),this.outOfInstance.in.get(1).parentSubnodes.get(0).childSubnodes.get(2),this.parentSubnodes.get(1).childSubnodes.get(1)};
////							this.definition.add(this.outOfInstance.definition, nodes2);
////							Node[] nodes3={this.outOfInstance.in.get(0).parentSubnodes.get(1).childSubnodes.get(2),this.outOfInstance.in.get(1).parentSubnodes.get(1),this.parentSubnodes.get(1).childSubnodes.get(2)};
////							this.definition.add(this.outOfInstance.definition, nodes3);
////							this.parentSubnodes.get(0).biFission();
////							this.parentSubnodes.get(1).childSubnodes.get(0).biFission();
////							this.parentSubnodes.get(1).childSubnodes.get(1).biFission();
////							this.parentSubnodes.get(1).childSubnodes.get(2).biFission();
//						}
////					}
//				}else if(this.outOfInstance.in.get(0).parentSubnodes.get(1).parentSupernode.childSubnodes.get(2)==this.outOfInstance.in.get(0).parentSubnodes.get(1)
//				&&this.outOfInstance.in.get(1).parentSubnodes.get(0).parentSupernode.childSubnodes.get(0)==this.outOfInstance.in.get(1).parentSubnodes.get(0)){
////					if(this.parentSubnodes.get(0).outOfInstance==null){
//						if(this.childSubnodes.isEmpty()&&this.parentSubnodes.get(0).outOfInstance==null){
//							Node[] nodes0={this.outOfInstance.in.get(0).parentSubnodes.get(0),this.outOfInstance.in.get(1).parentSubnodes.get(0),this.parentSubnodes.get(0)};
//							this.definition.add(this.outOfInstance.definition, nodes0);
//							Node[] nodes1={this.outOfInstance.in.get(0).parentSubnodes.get(1),this.outOfInstance.in.get(1).parentSubnodes.get(1),this.parentSubnodes.get(1)};
//							this.definition.add(this.outOfInstance.definition, nodes1);
//							this.parentSubnodes.get(0).biFission();
//							this.parentSubnodes.get(1).biFission();
//						}else{
////							this.outOfInstance.in.get(0).parentSubnodes.get(0).splitChildrenSubnodes();
////							this.outOfInstance.in.get(1).parentSubnodes.get(1).splitChildrenSubnodes();
////							this.parentSubnodes.get(1).splitChildrenSubnodes();
////							Node[] nodes0={this.outOfInstance.in.get(0).parentSubnodes.get(0).childSubnodes.get(0),this.outOfInstance.in.get(1).parentSubnodes.get(0),this.parentSubnodes.get(0)};
////							this.definition.add(this.outOfInstance.definition, nodes0);
////							Node[] nodes1={this.outOfInstance.in.get(0).parentSubnodes.get(0).childSubnodes.get(1),this.outOfInstance.in.get(1).parentSubnodes.get(1).childSubnodes.get(0),this.parentSubnodes.get(1).childSubnodes.get(0)};
////							this.definition.add(this.outOfInstance.definition, nodes1);
////							Node[] nodes2={this.outOfInstance.in.get(0).parentSubnodes.get(0).childSubnodes.get(2),this.outOfInstance.in.get(1).parentSubnodes.get(1).childSubnodes.get(1),this.parentSubnodes.get(1).childSubnodes.get(1)};
////							this.definition.add(this.outOfInstance.definition, nodes2);
////							Node[] nodes3={this.outOfInstance.in.get(0).parentSubnodes.get(1),this.outOfInstance.in.get(1).parentSubnodes.get(1).childSubnodes.get(2),this.parentSubnodes.get(1).childSubnodes.get(2)};
////							this.definition.add(this.outOfInstance.definition, nodes3);
////							this.parentSubnodes.get(0).biFission();
////							this.parentSubnodes.get(1).childSubnodes.get(0).biFission();
////							this.parentSubnodes.get(1).childSubnodes.get(1).biFission();
////							this.parentSubnodes.get(1).childSubnodes.get(2).biFission();
//						}
////					}
//				}
			}
		}
	}
	public Node equivalentNode() {
		Node node;
		if(this.parentSubnodes.size()==1){
			node = this.parentSubnodes.get(0).equivalentNode();
		}else{
			node = this;
		}
		return node;
	}
	public void addEquivalentSubnodeInstances() {
		//TODO: merge this in .childrenFission()
		Node node=this;
		if(this.parentSupernode!=null){
			this.parentSupernode.addEquivalentSubnodeInstances();
		}
		for(Node parentSubnode:this.parentSubnodes){
			parentSubnode.addEquivalentSubnodeInstances();
		}
		if(this.outOfInstance!=null){
			Node in0 = this.outOfInstance.in.get(0);
			Node in1 = this.outOfInstance.in.get(1);
			in0.addEquivalentSubnodeInstances();
			in1.addEquivalentSubnodeInstances();
			if(this.parentSubnodes.size()==1&&this.parentSubnodes.get(0).outOfInstance==null){
				node=this.parentSubnodes.get(0);
//				node.addEquivalentSubnodeInstances();
//				addEquivalentSubnode(in0,in1,this.parentSubnodes.get(0));
				Node[] nodes={in0,in1,node};
				this.definition.add(this.parentSupernode.outOfInstance.definition, nodes);		
			}
		}
	}
//	private void addEquivalentSubnode(Node in0, Node in1, Node out) {
//		if(out.parentSubnodes.size()==1&&out.parentSubnodes.get(0).outOfInstance==null){
//			Node[] nodes={in0,in1,out.parentSubnodes.get(0)};
//			this.definition.add(this.parentSupernode.outOfInstance.definition, nodes);
//			addEquivalentSubnodes(in0,in1,out.parentSubnodes.get(0));
//		}
//		
//	}
//	public void triFusion() {
//		//FIXME
////		if(!this.parentSubnodes.isEmpty()){
//			for(Node parentSubnode:this.parentSubnodes){
//				parentSubnode.triFusion();
//			}
////		}else{
//			if(this.parentSupernode!=null){
//				if(this.parentSupernode.outOfInstance!=null){	
//					if(this.parentSupernode.childSubnodes.get(0).outOfInstance!=null
//					&&this.parentSupernode.childSubnodes.get(1).outOfInstance!=null
//					&&this.parentSupernode.childSubnodes.get(2).outOfInstance!=null){
//						Node in0 = this.parentSupernode.childSubnodes.get(1).outOfInstance.in.get(0);
//						Node in1 = this.parentSupernode.childSubnodes.get(1).outOfInstance.in.get(1);
//						this.definition.removeInstance(this.parentSupernode.childSubnodes.get(0).outOfInstance);
//						this.definition.removeInstance(this.parentSupernode.childSubnodes.get(1).outOfInstance);
//						this.definition.removeInstance(this.parentSupernode.childSubnodes.get(2).outOfInstance);
//						in0.triFusion();
//						in1.triFusion();
//					}
//				}
//				this.parentSupernode.triFusion();
//			}
////		}
//		if(this.outOfInstance!=null){
//			this.outOfInstance.in.get(0).triFusion();
//			this.outOfInstance.in.get(1).triFusion();
//		}
//	}
	public void getEquivalentParentSupernode(
			HashMap<Node, Node> equivalentNodes, Queue<Node> queue) {
		if(this.parentSupernode!=null){
			if(!equivalentNodes.containsKey(this.parentSupernode)&&equivalentNodes.keySet().containsAll(this.parentSupernode.childSubnodes)){
				queue.removeAll(this.parentSupernode.childSubnodes);
				equivalentNodes.put(this.parentSupernode, equivalentNodes.get(this).parentSupernode);
				queue.add(this.parentSupernode);
			}
		}
	}
	public void triFusion() {
		for(Node parentSubnode:this.parentSubnodes){
			parentSubnode.triFusion();
		}
		if(this.parentSupernode!=null){
			Node leftChild = this.parentSupernode.childSubnodes.get(0).equivalentNode();//need equivalent
			Node midChild = this.parentSupernode.childSubnodes.get(1).equivalentNode();//.equivalentNode();
			Node rightChild = this.parentSupernode.childSubnodes.get(2).equivalentNode();//.equivalentNode();
//			midChild.parentsFusion();
			if(leftChild.outOfInstance!=null&&midChild.outOfInstance!=null&&rightChild.outOfInstance!=null
			&&leftChild.outOfInstance.in.get(0).parentSupernode!=null&&midChild.outOfInstance.in.get(0).parentSupernode!=null&&rightChild.outOfInstance.in.get(0).parentSupernode!=null
			&&leftChild.outOfInstance.in.get(1).parentSupernode!=null&&midChild.outOfInstance.in.get(1).parentSupernode!=null&&rightChild.outOfInstance.in.get(1).parentSupernode!=null
			&&leftChild.outOfInstance.in.get(0).parentSupernode==midChild.outOfInstance.in.get(0).parentSupernode&&midChild.outOfInstance.in.get(0).parentSupernode==rightChild.outOfInstance.in.get(0).parentSupernode
			&&leftChild.outOfInstance.in.get(1).parentSupernode==midChild.outOfInstance.in.get(1).parentSupernode&&midChild.outOfInstance.in.get(1).parentSupernode==rightChild.outOfInstance.in.get(1).parentSupernode
			){
				midChild.outOfInstance.in.get(0).triFusion();
				midChild.outOfInstance.in.get(1).triFusion();
				this.definition.removeInstance(leftChild.outOfInstance);
				this.definition.removeInstance(midChild.outOfInstance);
//				midChild.parentSubnodes.clear();
				this.definition.removeInstance(rightChild.outOfInstance);
//				System.out.println(this.toString());
//				System.out.println(this.definition.toString());
			}else{
				this.parentSupernode.triFusion();
			}
		}
		if(this.outOfInstance!=null){
			this.outOfInstance.in.get(0).triFusion();
			this.outOfInstance.in.get(1).triFusion();
		}	
	}
	public void getEquivalentChildSupernodes(
			HashMap<Node, Node> equivalentNodes, Queue<Node> queue) {
		for(Node childSupernode:this.childSupernodes){
			childSupernode.getEquivalentChildSupernode(equivalentNodes,queue);
		}
	}
	private void getEquivalentChildSupernode(
			HashMap<Node, Node> equivalentNodes, Queue<Node> queue) {
		if(!this.parentSubnodes.isEmpty()){
			if(equivalentNodes.keySet().containsAll(this.parentSubnodes)){
				HashSet<Node>   equivalentParentSubnodes = new HashSet<Node>();
				for(Node parentSubnode:this.parentSubnodes){
					equivalentParentSubnodes.add(equivalentNodes.get(parentSubnode));
				}
				for(Node equivalentChildSupernode:equivalentNodes.get(this.parentSubnodes.get(0)).childSupernodes){
					if(equivalentChildSupernode.parentSubnodes.containsAll(equivalentParentSubnodes)&&equivalentChildSupernode.parentSubnodes.size()==equivalentParentSubnodes.size()){
						equivalentNodes.put(this, equivalentChildSupernode);
						queue.add(this);
					}
				}
			}
		}
		
	}
	public void biFusion() {
		if(this.parentSupernode!=null){
			this.parentSupernode.biFusion();
		}
		if(this.outOfInstance!=null){
			this.outOfInstance.in.get(0).biFusion();
			this.outOfInstance.in.get(1).biFusion();
		}
		if(this.outOfInstance!=null&&this.parentSubnodes.size()==2&&this.parentSubnodes.get(0).equivalentNode().parentSupernode!=null&&this.parentSubnodes.get(1).equivalentNode().parentSupernode!=null&&this.parentSubnodes.get(0).equivalentNode().parentSupernode==this.parentSubnodes.get(1).equivalentNode().parentSupernode){
			Node leftNode = this.parentSubnodes.get(0).equivalentNode();
			Node rightNode = this.parentSubnodes.get(1).equivalentNode();
			leftNode.biFusion();
			rightNode.biFusion();
			if(leftNode.parentSupernode.outOfInstance!=null){
				this.definition.removeInstance(this.outOfInstance);
			}else{
				if(leftNode.outOfInstance!=null&&rightNode.outOfInstance!=null
				&&leftNode.parentSupernode!=null&&rightNode.parentSupernode!=null
				&&leftNode.parentSupernode==rightNode.parentSupernode
				&&leftNode.outOfInstance.in.get(0).parentSupernode!=null&&rightNode.outOfInstance.in.get(0).parentSupernode!=null
				&&leftNode.outOfInstance.in.get(0).parentSupernode==rightNode.outOfInstance.in.get(0).parentSupernode
				&&leftNode.outOfInstance.in.get(1).parentSupernode!=null&&rightNode.outOfInstance.in.get(1).parentSupernode!=null
				&&leftNode.outOfInstance.in.get(1).parentSupernode==rightNode.outOfInstance.in.get(1).parentSupernode
				){
					this.definition.removeInstance(leftNode.outOfInstance);
					this.definition.removeInstance(rightNode.outOfInstance);
					this.parentSubnodes.clear();
//				}else if(leftNode.childSubnodes.isEmpty()&&!rightNode.childSubnodes.isEmpty()
//				&&leftNode.outOfInstance!=null&&rightNode.childSubnodes.get(0).outOfInstance!=null&&rightNode.childSubnodes.get(1).outOfInstance!=null&&rightNode.childSubnodes.get(0).outOfInstance!=null
//				&&leftNode.parentSupernode!=null&&rightNode.parentSupernode!=null
//				&&leftNode.parentSupernode==rightNode.parentSupernode
//				&&leftNode.outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&rightNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&rightNode.childSubnodes.get(1).outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&rightNode.childSubnodes.get(2).outOfInstance.in.get(0).equivalentNode().parentSupernode!=null
//				&&rightNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode==rightNode.childSubnodes.get(1).outOfInstance.in.get(0).equivalentNode().parentSupernode&&rightNode.childSubnodes.get(1).outOfInstance.in.get(0).equivalentNode().parentSupernode==rightNode.childSubnodes.get(2).outOfInstance.in.get(0).equivalentNode().parentSupernode
//				&&rightNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode.parentSupernode!=null
//				&&leftNode.outOfInstance.in.get(0).equivalentNode().parentSupernode==rightNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode.parentSupernode
//				&&leftNode.outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&rightNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&rightNode.childSubnodes.get(1).outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&rightNode.childSubnodes.get(2).outOfInstance.in.get(1).equivalentNode().parentSupernode!=null
//				&&rightNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode==rightNode.childSubnodes.get(1).outOfInstance.in.get(1).equivalentNode().parentSupernode&&rightNode.childSubnodes.get(1).outOfInstance.in.get(1).equivalentNode().parentSupernode==rightNode.childSubnodes.get(2).outOfInstance.in.get(1).equivalentNode().parentSupernode
//				&&rightNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode.parentSupernode!=null
//				&&leftNode.outOfInstance.in.get(1).equivalentNode().parentSupernode==rightNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode.parentSupernode
//				){
//					this.definition.removeInstance(leftNode.outOfInstance);
//					this.definition.removeInstance(rightNode.childSubnodes.get(0).outOfInstance);
//					this.definition.removeInstance(rightNode.childSubnodes.get(1).outOfInstance);
//					this.definition.removeInstance(rightNode.childSubnodes.get(2).outOfInstance);;
//					this.parentSubnodes.clear();
//				}else if(!leftNode.childSubnodes.isEmpty()&&rightNode.childSubnodes.isEmpty()
//				&&leftNode.childSubnodes.get(0).outOfInstance!=null&&leftNode.childSubnodes.get(1).outOfInstance!=null&&leftNode.childSubnodes.get(2).outOfInstance!=null&&rightNode.outOfInstance!=null
//				&&leftNode.parentSupernode!=null&&rightNode.parentSupernode!=null
//				&&leftNode.parentSupernode==rightNode.parentSupernode
//				&&rightNode.outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&leftNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&leftNode.childSubnodes.get(1).outOfInstance.in.get(0).equivalentNode().parentSupernode!=null&&leftNode.childSubnodes.get(2).outOfInstance.in.get(0).equivalentNode().parentSupernode!=null
//				&&leftNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode==leftNode.childSubnodes.get(1).outOfInstance.in.get(0).equivalentNode().parentSupernode&&leftNode.childSubnodes.get(1).outOfInstance.in.get(0).equivalentNode().parentSupernode==leftNode.childSubnodes.get(2).outOfInstance.in.get(0).equivalentNode().parentSupernode
//				&&leftNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode.parentSupernode!=null
//				&&rightNode.outOfInstance.in.get(0).equivalentNode().parentSupernode==leftNode.childSubnodes.get(0).outOfInstance.in.get(0).equivalentNode().parentSupernode.parentSupernode
//				&&rightNode.outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&leftNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&leftNode.childSubnodes.get(1).outOfInstance.in.get(1).equivalentNode().parentSupernode!=null&&leftNode.childSubnodes.get(2).outOfInstance.in.get(1).equivalentNode().parentSupernode!=null
//				&&leftNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode==leftNode.childSubnodes.get(1).outOfInstance.in.get(1).equivalentNode().parentSupernode&&leftNode.childSubnodes.get(1).outOfInstance.in.get(1).equivalentNode().parentSupernode==leftNode.childSubnodes.get(2).outOfInstance.in.get(1).equivalentNode().parentSupernode
//				&&leftNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode.parentSupernode!=null
//				&&rightNode.outOfInstance.in.get(1).equivalentNode().parentSupernode==leftNode.childSubnodes.get(0).outOfInstance.in.get(1).equivalentNode().parentSupernode.parentSupernode
//				){
//					this.definition.removeInstance(rightNode.outOfInstance);
//					this.definition.removeInstance(leftNode.childSubnodes.get(0).outOfInstance);
//					this.definition.removeInstance(leftNode.childSubnodes.get(1).outOfInstance);
//					this.definition.removeInstance(leftNode.childSubnodes.get(2).outOfInstance);
//					this.parentSubnodes.clear();
////				}else{
////					this.parentSupernode=null;
////					this.definition.removeInstance(this.outOfInstance);
				}
			}
//			System.out.println(this.toString());
//			System.out.println(this.definition.toString());
		}else{
			for(Node parentSubnode:this.parentSubnodes){
				parentSubnode.biFusion();
			}
		}
	}
	public void cleanBinodes() {
		if(this.parentSupernode!=null){
			this.parentSubnodes.clear();
			this.parentSupernode.cleanBinodes();
		}
		for(Node parentSubnode:this.parentSubnodes){
			parentSubnode.cleanBinodes();
		}
		if(this.outOfInstance!=null){
			this.parentSubnodes.clear();
			this.outOfInstance.in.get(0).cleanBinodes();
			this.outOfInstance.in.get(1).cleanBinodes();
		}
		
	}
	public void mergeParentSupernode(Queue<Node> queue, ArrayList<Node> nodes) {
		if(this.parentSupernode!=null){
			if(!queue.contains(this.parentSupernode)&&queue.containsAll(this.parentSupernode.childSubnodes)){
				queue.removeAll(this.parentSupernode.childSubnodes);
				nodes.removeAll(this.parentSupernode.childSubnodes);
				queue.add(this.parentSupernode);
				nodes.add(this.parentSupernode);
			}
		}
		queue.remove(this);
	}
	public void mergeChildSupernode(Queue<Node> queue, ArrayList<Node> recursiveIn) {
		for(Node childSupernode:this.childSupernodes){
			if(queue.containsAll(childSupernode.parentSubnodes)){
				queue.removeAll(childSupernode.parentSubnodes);//replace with queue.remove() if candidates needed because multiple childSupernodes are posible
				recursiveIn.removeAll(childSupernode.parentSubnodes);
				queue.add(childSupernode);
				recursiveIn.add(childSupernode);
			}
		}
		queue.remove(this);
	}
}
