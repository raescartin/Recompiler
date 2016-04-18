/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import utils.AddedNodes;
import utils.FixedBitSet;

//DESCRIPTION
//-Defines a "Definition", procedure, function with multiple outputs
//-Recursive definition:
//	-A definition is composed by instances of definitions
//	-Nand is the basic definition
//-Definitions may be recursive and ARE TURING COMPLETE (equivalent to algorithms)
//PRE: all inputs(and in consequence outputs) of a nand definition must be of the same size


//FIXME: 
//-can't have definitions outside of DB (or else they're added to usedIn and things get messed up)
//TODO: 
//-add arrays

//////////////////////////////////////////////////////
//HERE BE DRAGONS (maybe not in Java from here)
//////////////////////////////////////////////////////
//-optimize definition while adding new node (efficiency)
//-parallelization of code
//
//OPTIMIZATION
//-Short-circuit evaluation
//-Disable infinite self recursion (also indirect) = no CICLES (infinite should only be possible with infinite BitSet)
//-!=sizes on eval -> fill with 0's

//DOUBTS
//-Branch elimination with logic?


//DATA STRUCTURE:
//a definition is an extension of a function with variable number of outputs
//each definition is equivalent to a variable number of trees of nands (NandForest)
//each definition has a variable number of "in" and "out" nodes and an unique "name"
//each out node of definition is out node of instance if not nand
//each out node of definition points "in" to  this instance of a definition
//each instance points "definition" to a definition (with same numbers of "in" and "out" nodes == PARAMETERS )
//instances form a tree/graph structure connecting each other via "in" and "out" nodes
//creation order, from leafs(in) to roots(out) in order to use "add" optimizations
//access order, form roots(out) to leafs(in)

//IMPORTANT
//-only pointers in structure, objects created outside (already in Java)
//-outputs are unique if optimized (else repeated calculation)



public class Definition {
	public int maxNode=0;//keep track for easy consistency while removing nodes
	public String name;
	public ArrayList<Node> in;
	public ArrayList<Node> out;
	public ArrayList<Instance> instances;//TODO: better data structure Hash AND list linkedhashmap?  - replaces def?
	public ArrayList<Definition> rootIn;//TODO: better data structure Hash AND list linkedhashmap? - verify definitions are in DB
	public HashSet<Instance> selfRecursiveInstances;//Recursive instances of this definition, contained in this definition
	public HashSet<Instance> instancesOfRecursiveDefinitions;//Instances of other recursive definitions
	//DEBUGGING ONLY
	public HashSet<Node> nodes;
	//END OF DEBUGGING ONLY
	
	//CONSTRUCTOR
	public Definition(int numberOfInputs,int numberOfOutputs, String name){
		//INITIALIZE VARIABLES
		this.in = new ArrayList<Node>();
		this.out = new ArrayList<Node>();
		this.instances = new ArrayList<Instance>();
		this.selfRecursiveInstances = new HashSet<Instance>();
		this.instancesOfRecursiveDefinitions = new HashSet<Instance>();
		this.rootIn = new ArrayList<Definition>();
		//DEBUGGING ONLY
		this.nodes = new HashSet<Node> ();
		//END OF DEBUGGING ONLY
		
		this.name=name;
		
		for (int i = 0; i < numberOfInputs; i++) {
			in.add(new Node());
			this.add(in.get(i));
		}
		for (int i = 0; i < numberOfOutputs; i++) {
			out.add(new Node());
			this.add(out.get(i));
		}			
	}
	public Definition() {
		//INITIALIZE VARIABLES
		this.in = new ArrayList<Node>();
		this.out = new ArrayList<Node>();
		this.instances = new ArrayList<Instance>();
		this.selfRecursiveInstances = new HashSet<Instance>();
		this.instancesOfRecursiveDefinitions = new HashSet<Instance>();
		this.rootIn = new ArrayList<Definition>();
		this.nodes = new HashSet<Node> ();
	}
	public NandForest toNandForest(ArrayList<Node> nandToNodeIn, ArrayList<Node> nandToNodeOut){
		//PRE: this definition is not recursive and doesn't contain recursive definitions
		// the nodes have been split to the minimum needed 
		//POST: returns a NandForest equivalent to this definition, map of in and map of out nandnodes to nodes
		//more efficient thanks to HashMap's of unique nodes
		//TOPDOWN OR DOWNUP? DOWNUP less branches, UPDOWN less memory? -> DOWNUP needed to optimize and instance
		NandForest nandForest = new NandForest(0);
		HashMap<Node, NandNode> nodeToNand = new HashMap<Node, NandNode>();
		///Need to map subnodes of ins and outs to conserve references!!!
		this.mapIns(nodeToNand,nandForest,nandToNodeIn);
		this.mapOuts(nodeToNand,nandForest,nandToNodeOut);
		nandForest.optimize();
		return nandForest;
	}
	void mapIns(HashMap<Node, NandNode> nodeToNand, NandForest nandForest, ArrayList<Node> nandToNodeIn) {
		//map input nodes to nandNodes
		HashSet<Node> inNodes = new HashSet<Node>();
		inNodes.addAll(this.in);
		for(Node inNode:this.in){
			inNode.mapInChildren(nodeToNand, nandForest, nandToNodeIn);
		}
	}
	void mapOuts(HashMap<Node, NandNode> nodeToNand,
			NandForest nandForest, ArrayList<Node> nandToNodeOut) {
		//map output nodes to nandNodes
		for(Node outNode:this.out){
			outNode.mapOutParents(nodeToNand,nandForest, nandToNodeOut);
		}	
	}
	public Instance add(Definition def,Node ... nodes){
		for(Node node:nodes){
			this.add(node);	
		}
		Instance instance = new Instance();//node == instance of a definition
		if(def==this){
			def.selfRecursiveInstances.add(instance);
		}else if(!def.selfRecursiveInstances.isEmpty()||!def.instancesOfRecursiveDefinitions.isEmpty()){
			this.instancesOfRecursiveDefinitions.add(instance);
		}
		instance.in = new ArrayList<Node>(Arrays.asList(nodes).subList(0, def.in.size()));
		instance.out = new ArrayList<Node>(Arrays.asList(nodes).subList(def.in.size(), def.in.size()+def.out.size()));
		instance.definition=def;
		for (Node outNode:instance.out) {//nºinst outs = nºdef outs
			outNode.outOfInstance=instance;
		}
		this.instances.add(instance);
		return instance;
	}
	public void add(Node node){
		if(!this.nodes.contains(node)){
			node.idForDefinition=this.maxNode;//debugging only
			this.maxNode++;
			node.definition=this;
			this.nodes.add(node);
			for(Node parent:node.parents){
				this.add(parent);
			}
			for(Node child:node.children){
				this.add(child);
			}	
		}
	}
	public String toString() {
		String string = new String();
		//Print this definition translating node id to integers
		string+=this.name;
		string+=("[");
		for (Node node: this.in) {
			string+=node.toString();
			string+=(",");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration ","
		string+=(";");
		for (Node node: this.out) {
			string+=node.toString();
			string+=(",");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration ","
		string+=("]");
		
		string+=(" = ");
		for (Instance instance : this.instances) {//print instances
		    string+=instance.toString(this);
		}
		string+=(" root in: ");
		for (Definition root : this.rootIn){
			string+=(root.name);
			string+=(",");
		}
		string+="\n";
		return string;
	}
	public boolean apply(Instance instance, Definition appliedDefinition, HashSet<Node> supernodeOuts) {
		//PRE: .this is the definition where instance is, instance is the root instance, appliedDefinition the definition applied
		//POST:apply definition with instance as root of appliedDefinition
		// and replaces existing instances with instance to an existing applied definition
		//TODO: replace only non outside referenced instances (without intersections),if the replaced instances have any halfway node don't replace
		//MAYBE: verify that all roots are expanded (if needed, +++cost)
		Instance expandingInstance=instance;
		Instance expandingAppliedInstance=null;
		HashSet<Instance> toExpand = new HashSet<Instance>();//instances to expand
		HashMap<Instance,Instance> instanceMap = new HashMap<Instance,Instance>();//map of expanded instances
		HashMap<Node,Node> nodeMap = new HashMap<Node,Node>();//map of expanded nodes
		if(appliedDefinition.out!=null&&!appliedDefinition.out.isEmpty()&&this.name!="nand"){//if applied definition is not nand//FIXME: only third term needed?//FIX nand checking
			expandingAppliedInstance=appliedDefinition.out.get(0).findRootInstance();
			instanceMap.put(expandingAppliedInstance,expandingInstance);
			toExpand.add(expandingAppliedInstance);
			while(!toExpand.isEmpty()){
				if(expandingInstance==null||expandingInstance.definition!=expandingAppliedInstance.definition){//instance must be to same definition and nºins and nºout must be equal
					return false;
				}else{
					for (int i = 0; i < expandingAppliedInstance.in.size(); i++){//expand all in nodes
						if(!this.expandNodes(expandingAppliedInstance.in.get(i),expandingInstance.in.get(i),nodeMap,instanceMap,toExpand)){//added as different node (non unique)
							return false;
						}
					}
					for (int i = 0; i < expandingAppliedInstance.out.size(); i++){//expand all out nodes
						if(!this.expandNodes(expandingAppliedInstance.out.get(i),expandingInstance.out.get(i),nodeMap,instanceMap,toExpand)){//added as different node (non unique)
							return false;
						}
					}
					toExpand.remove(expandingAppliedInstance);
					if(toExpand.iterator().hasNext()){
						expandingAppliedInstance=toExpand.iterator().next();
						expandingInstance=instanceMap.get(expandingAppliedInstance);
					}
					
				}
				
			}
			ArrayList<Node> inArray = new ArrayList<Node>();
			ArrayList<Node> outArray = new ArrayList<Node>();
			for (Node node : appliedDefinition.in) {
				inArray.add(nodeMap.get(node));
			}
			for (Node node : appliedDefinition.out) {
				Node outNode=nodeMap.get(node);
				outArray.add(outNode);
				outNode.outOfInstance=instance;
			}
			//replace instance
			instance.in=inArray;
			instance.definition=appliedDefinition;
			//Connect out
			instance.out=outArray;
			//remove replaced instances (except "instance" already overwritten) 
			HashSet<Instance> expandedInstances = new HashSet<Instance>();//make a HashSet of all the expanded instances of definition
			expandedInstances.addAll(instanceMap.values());//(part of this definition) for future verification
			HashSet<Instance> removableInstances = new HashSet<Instance>(expandedInstances);//copy all the expanded instances
			//select removable instances
			for(Instance thisInstance : this.instances){
				if(!expandedInstances.contains(thisInstance)){
					for(Node node :thisInstance.in){
						removableInstances.remove(node.outOfInstance);
					}
				}
			}
			//remove the already replaced instance (to applied definition)
			removableInstances.remove(instance);
			//remove the removable instances
			for (Instance removableInstance : removableInstances) {
				boolean containsInOutNode=false;//checking not to remove a non removable node
				for(Node outNode:removableInstance.out){
					if(supernodeOuts.contains(outNode.supernodeParent())) containsInOutNode=true;
				}
				if(!containsInOutNode) this.instances.remove(removableInstance);//TODO:change to ¿list?HASH¿map? to remove in O(1) instead O(n)?
			}
		}
		return true;
	}
	private boolean expandNodes(Node appliedNode, Node node,
			HashMap<Node, Node> nodeMap, HashMap<Instance, Instance> instanceMap, HashSet<Instance> toExpand) {
		if(appliedNode.outOfInstance!=null){
			if(instanceMap.containsKey(appliedNode.outOfInstance)){//already added instance
				if(instanceMap.get(appliedNode.outOfInstance)!=node.outOfInstance){//added as instance to different definition
					return false;
				}
			}else{//non-added instance
				if(node.outOfInstance==null){
					return false;
				}else{
					instanceMap.put(appliedNode.outOfInstance,node.outOfInstance);
					toExpand.add(appliedNode.outOfInstance);
				}
			}
		}
		if(nodeMap.containsKey(appliedNode)){//already added node
			if(nodeMap.get(appliedNode)!=node){
				return false;
			}
		}else{//non-added node
			nodeMap.put(appliedNode,node);//expand node
			if(!appliedNode.children.isEmpty()||!appliedNode.parents.isEmpty()){
				if(node.parents.size()<appliedNode.parents.size()){
					return false;
				}
				for(int i=0;i<appliedNode.parents.size();i++){
					if(!expandNodes(appliedNode.parents.get(i),node.parents.get(i),nodeMap,instanceMap, toExpand)){
						return false;
					}
				}
				if(node.children.size()<appliedNode.children.size()){
					return false;
				}
				for(int i=0;i<appliedNode.children.size();i++){
					if(!expandNodes(appliedNode.children.get(i),node.children.get(i),nodeMap,instanceMap, toExpand)){
						return false;
					}
				}
			}
		}
		return true;
	}
	public void printEval(String ... strings){
		ArrayList<String> ins = new ArrayList<String>();
		ArrayList<String> outs = new ArrayList<String>();
		//eval out nodes using BFS
		HashMap<Node, FixedBitSet> valueMap = new HashMap<Node, FixedBitSet>() ;
		for(int i=0;i<this.in.size();i++){
			valueMap.put(this.in.get(i), FixedBitSet.fromString(strings[i]));
		}
		this.coreEval(valueMap);
		for(int i=0;i<this.in.size();i++){
			ins.add(valueMap.get(this.in.get(i)).toString());
		}
		for(int i=0;i<this.out.size();i++){
			outs.add(valueMap.get(this.out.get(i)).toString());
		}
		System.out.print(ins);
		System.out.print(this.name);
		System.out.println(outs);
		
	}
	private void coreEval(HashMap<Node, FixedBitSet> valueMap) {
		if(this.name=="nand"){//NAND //TODO: fix nand checking
			//NAND (always 2 ins 1 out)
			if(valueMap.containsKey(this.in.get(1))//LAZY EVALUATION
				&&valueMap.get(this.in.get(1)).length()!=0&&valueMap.get(this.in.get(1)).cardinality()==0){//one input not mapped
					ArrayList<String> ones = new ArrayList<String>();
					for(int i=0;i<valueMap.get(this.in.get(1)).length();i++){
						ones.add("1");
					}
					valueMap.put(this.out.get(0), FixedBitSet.fromString(String.join(", ", ones)));
			}else if(valueMap.containsKey(this.in.get(0))//LAZY EVALUATION
				&&valueMap.get(this.in.get(0)).length()!=0&&valueMap.get(this.in.get(0)).cardinality()==0){//one input not mapped
					ArrayList<String> ones = new ArrayList<String>();
					for(int i=0;i<valueMap.get(this.in.get(0)).length();i++){
						ones.add("1");
					}
					valueMap.put(this.out.get(0), FixedBitSet.fromString(String.join(", ", ones)));
			}else if(valueMap.containsKey(this.in.get(0))&&valueMap.containsKey(this.in.get(1))){
				valueMap.put(this.out.get(0),valueMap.get(this.in.get(0)).nand(valueMap.get(this.in.get(1))));
			}
		}else{
			//eval out nodes using BFS
			boolean allOuts=true;
			for(Node outNode:this.out){
				allOuts&=valueMap.containsKey(outNode);
			}
			int depth=0;
			while(!allOuts){//evaluation ends, when all outs are evaluated
				Queue<Node> nodesToExpand = new LinkedList<Node>();//Queue for BFS
				ArrayList<HashSet<Node>> emptyNodesByDefinition = new ArrayList<HashSet<Node>>();
				HashSet<Node> emptyNodes = new HashSet<Node>();
				emptyNodesByDefinition.add(emptyNodes);
				ArrayList<HashSet<Node>> fullExpandedNodesByDefinition = new ArrayList<HashSet<Node>>();
				HashSet<Node> fullExpandedNodes = new HashSet<Node>();
				fullExpandedNodes.addAll(this.in);
				fullExpandedNodesByDefinition.add(fullExpandedNodes);
				for(Node nodeOut:this.out){
					nodesToExpand.add(nodeOut);
				}
				while(!nodesToExpand.isEmpty()&&!allOuts){
					nodesToExpand.poll().eval(valueMap,nodesToExpand,emptyNodesByDefinition,depth, fullExpandedNodesByDefinition);
					allOuts=true;
					for(Node outNode:this.out){
						allOuts&=valueMap.containsKey(outNode)||emptyNodes.contains(outNode);
					}
				}
				allOuts=true;
				for(Node outNode:this.out){
					allOuts&=valueMap.containsKey(outNode);
				}
				depth++;
			}
		}
		
	}
	public void eval(HashMap<Node, FixedBitSet> valueMap, ArrayList<HashSet<Node>> emptyNodesByDefinition, int depth, ArrayList<HashSet<Node>> fullExpandedNodesByDefinition){
		if(this.name=="nand"){//NAND //TODO: fix nand checking
			//NAND (always 2 ins 1 out)
			if(valueMap.containsKey(this.in.get(1))//LAZY EVALUATION
				&&valueMap.get(this.in.get(1)).length()!=0&&valueMap.get(this.in.get(1)).cardinality()==0){//one input not mapped
					ArrayList<String> ones = new ArrayList<String>();
					for(int i=0;i<valueMap.get(this.in.get(1)).length();i++){
						ones.add("1");
					}
					valueMap.put(this.out.get(0), FixedBitSet.fromString(String.join(", ", ones)));
					fullExpandedNodesByDefinition.get(fullExpandedNodesByDefinition.size()-1).add(this.out.get(0));
			}else if(valueMap.containsKey(this.in.get(0))//LAZY EVALUATION
				&&valueMap.get(this.in.get(0)).length()!=0&&valueMap.get(this.in.get(0)).cardinality()==0){//one input not mapped
					ArrayList<String> ones = new ArrayList<String>();
					for(int i=0;i<valueMap.get(this.in.get(0)).length();i++){
						ones.add("1");
					}
					valueMap.put(this.out.get(0), FixedBitSet.fromString(String.join(", ", ones)));
					fullExpandedNodesByDefinition.get(fullExpandedNodesByDefinition.size()-1).add(this.out.get(0));
			}else if(valueMap.containsKey(this.in.get(0))&&valueMap.containsKey(this.in.get(1))){
				valueMap.put(this.out.get(0),valueMap.get(this.in.get(0)).nand(valueMap.get(this.in.get(1))));
				fullExpandedNodesByDefinition.get(fullExpandedNodesByDefinition.size()-1).add(this.out.get(0));
			}
			for(HashSet<Node> emptyNodes:emptyNodesByDefinition){
				if(emptyNodes.contains(this.in.get(0))||emptyNodes.contains(this.in.get(1))){
					emptyNodes.add(this.out.get(0));
				}
			}
			for(HashSet<Node> fullExpandedNodes:fullExpandedNodesByDefinition){
				if(fullExpandedNodes.contains(this.in.get(0))&&fullExpandedNodes.contains(this.in.get(1))){
					fullExpandedNodes.add(this.out.get(0));
				}
			}
		}else{
			//eval out nodes using BFS
			Queue<Node> nodesToExpand = new LinkedList<Node>();//Queue for BFS
			boolean allOuts=false;
			for(Node node:this.out){
				nodesToExpand.add(node);
			}
			while(!nodesToExpand.isEmpty()&&!allOuts){
				ArrayList<HashSet<Node>> newEmptyNodesByDefinition = new ArrayList<HashSet<Node>>();//FIXME: need for temp array emptyNodesByDefinition or only one emptyNodes?
				for(HashSet<Node> emptyNodes:emptyNodesByDefinition){
					HashSet<Node> newEmptyNodes = new HashSet<Node>();
					newEmptyNodes.addAll(emptyNodes);
					newEmptyNodesByDefinition.add(newEmptyNodes);
				}
				nodesToExpand.poll().eval(valueMap,nodesToExpand,newEmptyNodesByDefinition,depth, fullExpandedNodesByDefinition);
				allOuts=true;
				for(Node outNode:this.out){
					allOuts&=valueMap.containsKey(outNode)||newEmptyNodesByDefinition.get(newEmptyNodesByDefinition.size()-1).contains(outNode);//TODO: check 
				}
				if(allOuts){
					for(int i=0;i<emptyNodesByDefinition.size();i++){//copyBack
						emptyNodesByDefinition.get(i).addAll(newEmptyNodesByDefinition.get(i));
					}
				}
			}
			
			
		}
	}
	public void removeRecursion(AddedNodes addedNodes,HashSet<Instance> removedInstances) {
		for(Instance instance : this.selfRecursiveInstances){
			//remove all recursive instances, to make the definition not self recursive
			//adding nodes from recursive instances
			//TODO:
			//!!!TO OPTIMIZE RECUSIVE INTERSECTION!!!
			//1 add instances of 1st recursion (expand recursion like its done with instancesOfRecursiveDefinitions)
			//2 map out/in recursive nodes
			//3 keep track of these nodes
			//4 create new definition of the recursive part without intersections (def=x,defRwithoutIntersections,y defRwithoutIntersections=w,defRwithoutIntersections,z)
			this.removeRecursiveInstance(instance,addedNodes,removedInstances);					
		}
		this.selfRecursiveInstances.clear();
		HashSet<Instance> instances = new HashSet<Instance>();
		instances.addAll(this.instancesOfRecursiveDefinitions);
		this.instancesOfRecursiveDefinitions.clear();
		for(Instance instance:instances){//map all the instancesOfRecursiveDefinitions nodes
			this.instances.remove(instance);
			this.expandRecursiveInstance(instance, addedNodes, removedInstances);
		}
	}
	void expandRecursiveInstance(Instance instance, AddedNodes addedNodes, HashSet<Instance> removedInstances) {
		HashMap<Node,Node> definitionToInstanceNodes = new HashMap<Node,Node>();
		for (int i = 0; i < instance.in.size(); i++) {//map in nodes
			definitionToInstanceNodes.put(instance.definition.in.get(i), instance.in.get(i));
			mapSubnodeParents(instance.in.get(i),instance.definition.in.get(i),definitionToInstanceNodes);
			mapSubnodeChildren(instance.in.get(i),instance.definition.in.get(i),definitionToInstanceNodes);	
			mapSupernodeChildren(instance.in.get(i),instance.definition.in.get(i),definitionToInstanceNodes);
		}
		for (int i = 0; i < instance.out.size(); i++) {//map out nodes
			definitionToInstanceNodes.put(instance.definition.out.get(i), instance.out.get(i));
			instance.out.get(i).outOfInstance=null;
			mapSubnodeParents(instance.out.get(i),instance.definition.out.get(i),definitionToInstanceNodes);
//			mapSubnodeChildren(instance.out.get(i),instance.definition.out.get(i),definitionToInstanceNodes);	
//			mapSupernodeChildren(instance.out.get(i),instance.definition.out.get(i),definitionToInstanceNodes);
		}
		for(Instance definitionInstance:instance.definition.instances){
				ArrayList<Node> nodes = new ArrayList<Node>();
				for (Node node: definitionInstance.in) {//map in nodes
					if(definitionToInstanceNodes.containsKey(node)){
						nodes.add(definitionToInstanceNodes.get(node));
					}else{
						Node newNode = new Node();
						definitionToInstanceNodes.put(node, newNode);
						nodes.add(newNode);
						node.recursivelyMapParents(definitionToInstanceNodes);
					}
				}
				for (Node node: definitionInstance.out) {//map out nodes
					if(definitionToInstanceNodes.containsKey(node)){
						nodes.add(definitionToInstanceNodes.get(node));
					}else{
						Node newNode = new Node();
						definitionToInstanceNodes.put(node, newNode);
						nodes.add(newNode);
						node.recursivelyMapParents(definitionToInstanceNodes);
					}
				}
				Instance newInstance=this.add(definitionInstance.definition,nodes.toArray(new Node[nodes.size()]));
				if(newInstance.definition==instance.definition){
					this.instancesOfRecursiveDefinitions.remove(newInstance);
					this.removeRecursiveInstance(newInstance, addedNodes, removedInstances);
				}else if(!newInstance.definition.selfRecursiveInstances.isEmpty()){//is recursive
					this.instancesOfRecursiveDefinitions.remove(newInstance);
					this.instances.remove(newInstance);
					this.expandRecursiveInstance(newInstance, addedNodes, removedInstances);
				}
		}
		
	}
	void removeRecursiveInstance(Instance instance,AddedNodes addedNodes, HashSet<Instance> removedInstances) {
		removedInstances.add(instance);
		addedNodes.in+=instance.out.size();
		for (int i = 0; i < instance.out.size(); i++) {//add out nodes to def in
			instance.out.get(i).outOfInstance=null;
			this.in.add(instance.out.get(i));
		}
		for (int i = 0; i < instance.in.size(); i++) {//add in nodes to def out
			if(!this.out.contains(instance.in.get(i))){
				this.out.add(instance.in.get(i));
				addedNodes.out++;
			}
		}
		this.instances.remove(instance);
	}
	private void mapSubnodeParents(Node node, Node definitionNode, HashMap<Node, Node> definitionToNewNodes) {
		if(!definitionNode.parents.isEmpty()){
			if(node.parents.isEmpty()){
				if(definitionNode.parents.size()==1){
					Node supernode= new Node();
					node.definition.add(supernode);
					supernode.splitChildren();
					this.nodes.remove(supernode.children.get(definitionNode.parents.get(0).children.indexOf(definitionNode)));
					supernode.children.set(definitionNode.parents.get(0).children.indexOf(definitionNode), node);
					node.parents.add(supernode);
					definitionToNewNodes.put(definitionNode.parents.get(0), supernode);
					definitionToNewNodes.put(definitionNode.parents.get(0).children.get(0), supernode.children.get(0));
					definitionToNewNodes.put(definitionNode.parents.get(0).children.get(1), supernode.children.get(1));
					definitionToNewNodes.put(definitionNode.parents.get(0).children.get(2), supernode.children.get(2));
					mapSubnodeParents(node,definitionNode,definitionToNewNodes);
				}else if(definitionNode.parents.size()>1){
					if(node.children.size()==3&&node.children.get(0).parents.size()==1){//prevent redundant subnodes
						Node childLeft=node.children.get(0);
						Node childMid=node.children.get(1);
						Node childRight=node.children.get(2);
						Node definitionParentLeft = definitionNode.parents.get(0);
						Node definitionParentRight = definitionNode.parents.get(definitionNode.parents.size()-1);
						Node parentLeft=new Node();
						Node parentRight=new Node();
						childLeft.parents.clear();
						parentLeft.add(childLeft);
						parentLeft.add(new Node());
						parentLeft.add(new Node());
						childRight.parents.clear();
						parentRight.add(new Node());
						parentRight.add(new Node());
						parentRight.add(childRight);
						childMid.parents.clear();
						if(definitionParentLeft.parents.size()==1){
							definitionParentLeft=definitionParentLeft.parents.get(0);
						}else{
							parentLeft.children.get(1).add(childMid);
							parentLeft.children.get(2).add(childMid);
						}
						for(int i=1;i<definitionNode.parents.size()-1;i++){
							Node parent=new Node();
							definitionToNewNodes.put(definitionNode.parents.get(i), parent);
//							mapSubnodeChildren(parent, definitionNode.parents.get(i), definitionToNewNodes);//FIXME:Needed?
							parent.add(childMid);
						}
						if(definitionParentRight.parents.size()==1){
							definitionParentRight=definitionParentRight.parents.get(0);
						}else{
							parentRight.children.get(0).add(childMid);
							parentRight.children.get(1).add(childMid);
						}
						definitionToNewNodes.put(definitionParentLeft, parentLeft);
						mapSubnodeChildren(parentLeft, definitionParentLeft, definitionToNewNodes);
						definitionToNewNodes.put(definitionParentRight, parentRight);
						mapSubnodeChildren(parentRight, definitionParentRight, definitionToNewNodes);//FIXME
						node.parents.clear();
						node.children.clear();
						childLeft.add(node);
						node.splice(childMid);
						childRight.add(node);
//						node.flattenParents();
////						node.nodeFussion();
//						for(Node child:childLeft.children){
//							child.flattenParents();
////							child.nodeFussion();
//						}
//						for(Node child:childMid.children){
//							child.flattenParents();
////							child.nodeFussion();
//						}
//						for(Node child:childRight.children){
//							child.flattenParents();
////							child.nodeFussion();
//						}
					}else{
						for(Node parent:definitionNode.parents){
							Node newParent= new Node();
							newParent.add(node);
							definitionToNewNodes.put(parent, newParent);
							mapSubnodeParents(newParent,parent,definitionToNewNodes);
						}
					}
				}
			}else if(node.parents.size()==definitionNode.parents.size()){//not sure if this check is enough
				for(int j=0;j<node.parents.size();j++){
					definitionToNewNodes.put(definitionNode.parents.get(j),node.parents.get(j));
					mapSubnodeParents(node.parents.get(j), definitionNode.parents.get(j),definitionToNewNodes);
				}
			}else{
				System.out.print("can happen?");
			}
		}
	}
	private void mapSubnodeChildren(Node node, Node definitionNode, HashMap<Node, Node> definitionToInstanceNodes) {
		if(!definitionNode.children.isEmpty()){
			if(definitionNode.children.get(0).parents.size()==1){//the children are subnodes
				if(node.children.size()==definitionNode.children.size()){
					for(int i=0;i<node.children.size();i++){
						definitionToInstanceNodes.put(definitionNode.children.get(i),node.children.get(i));	
					}
					for(int i=0;i<node.children.size();i++){
						mapSubnodeChildren(node.children.get(i), definitionNode.children.get(i),definitionToInstanceNodes);
					}
				}else{//FIXME: now children size is variable
					if(definitionNode.children.size()==3&&definitionNode.children.get(0).parents.size()==1){//supernode
						if(node.parents.size()>1){//prevent redundant subnodes
							//variables to conserve references
							Node parentLeft = node.parents.get(0);
							Node parentRight = node.parents.get(node.parents.size()-1);
							Node leftLeft = new Node();
							ArrayList<Node> midArray = new ArrayList<Node>();
							Node rightRight = new Node();
							leftLeft=parentLeft.mapLeft(midArray);//FIXME: remove mapLeft
							rightRight=parentRight.mapRight(midArray);//FIXME: remove mapRight
							definitionToInstanceNodes.put(definitionNode.children.get(0), leftLeft);
							mapSubnodeChildren(leftLeft,definitionNode.children.get(0),definitionToInstanceNodes);
							definitionToInstanceNodes.put(definitionNode.children.get(2), rightRight);
							mapSubnodeChildren(rightRight,definitionNode.children.get(2),definitionToInstanceNodes);						
							if(!midArray.isEmpty()){
								Node mid;
								if(midArray.size()==1){
									mid=midArray.get(0);
								}else{
									mid= new Node();
									for(Node midArrayNode:midArray){
										midArrayNode.add(mid);
									}
								}
								definitionToInstanceNodes.put(definitionNode.children.get(1), mid);
								mapSubnodeChildren(mid,definitionNode.children.get(1),definitionToInstanceNodes);
							}
						}else{
							if(node.children.isEmpty()){
								for(@SuppressWarnings("unused") Node child:definitionNode.children){
									Node newChild= new Node();
									node.add(newChild);
								}
							}else{//node.children.size()==1 splice
								Node supernode =node.children.get(0);
								node.children.clear();
								int insertIndex=supernode.parents.indexOf(node);
								supernode.parents.remove(insertIndex);
								for(int i=0;i<3;i++){
									Node newChild= new Node();
									node.add(newChild);
									supernode.parents.add(insertIndex+i, newChild);
									newChild.children.add(supernode);
									newChild.definition=supernode.definition;
									
								}
							}
							for(int i=0;i<3;i++){
								definitionToInstanceNodes.put(definitionNode.children.get(i), node.children.get(i));
								mapSubnodeChildren(node.children.get(i),definitionNode.children.get(i),definitionToInstanceNodes);
							}
						}
					}
				}
			}
		}
	}
	private void mapSupernodeChildren(Node node, Node definitionNode, HashMap<Node, Node> definitionToInstanceNodes) {
		if(!definitionNode.children.isEmpty()){
			if(definitionNode.children.get(0).parents.size()!=1){//the children nodes are supernodes
				for(Node definitionSupernode:definitionNode.children){
					if(!definitionToInstanceNodes.containsKey(definitionSupernode)){
						ArrayList<Node> parents = new ArrayList<Node>();
						for(Node definitionParent:definitionSupernode.parents){
							parents.add(definitionToInstanceNodes.get(definitionParent));
						}
						boolean contains=false;
						for(Node supernode: node.children){
							supernode.flattenParents();
							if(definitionSupernode.parents.size()==supernode.parents.size()&&supernode.parents.containsAll(parents)){
								definitionToInstanceNodes.put(definitionSupernode,supernode);
								contains=true;
							}
						}
						if(!contains){
							Node newSupernode = new Node();
							for(Node definitionParent:definitionSupernode.parents){
								if(!definitionToInstanceNodes.containsKey(definitionParent)){
									Node newParent = new Node();//FIXME: should be recursive //parents may be not defined yet
									definitionToInstanceNodes.put(definitionParent, newParent);
								}
								definitionToInstanceNodes.get(definitionParent).add(newSupernode);
							}
							definitionToInstanceNodes.put(definitionSupernode, newSupernode);
						}
					}
				}
			}else{
				for(int i=0;i<node.children.size();i++){
					mapSupernodeChildren(node.children.get(i), definitionNode.children.get(i), definitionToInstanceNodes);	
				}
			}
		}
	}
	Node mapLeft(Node parentLeft, Node newNode) {
		if(parentLeft.parents.size()>1){
			parentLeft=mapLeft(parentLeft.parents.get(0),newNode);
			for(int i=1;i<parentLeft.parents.size();i++){
				parentLeft.parents.get(i).children.clear();
				parentLeft.parents.get(i).add(newNode);
			}
		}else{
			if(parentLeft.parents.size()==1&&(parentLeft.parents.get(0).children.indexOf(parentLeft)==0||parentLeft.parents.get(0).children.indexOf(parentLeft)==2)){
				//if node is not divisible
				parentLeft=parentLeft.parents.get(0);
			}else{
//				if(parentLeft.children.size()==1)parentLeft.children.clear();
				parentLeft.splitChildren();
				if(!parentLeft.children.get(1).children.isEmpty()&&parentLeft.children.get(1).children.get(0).parents.size()==1){//node has subnodes
					newNode.addSubnodes(parentLeft.children.get(1));
				}else{
					parentLeft.children.get(1).add(newNode);
				}
				parentLeft.children.get(2).add(newNode);
			}
		}
		return parentLeft;
	}
	Node mapRight(Node parentRight, Node newNode) {
		if(parentRight.parents.size()>1){
			for(int i=0;i<parentRight.parents.size()-1;i++){
				parentRight.parents.get(i).children.clear();
				parentRight.parents.get(i).add(newNode);
			}
			parentRight=mapRight(parentRight.parents.get(parentRight.parents.size()-1),newNode);
		}else{
			if(parentRight.parents.size()==1&&(parentRight.parents.get(0).children.indexOf(parentRight)==0||parentRight.parents.get(0).children.indexOf(parentRight)==2)){
				//if node is not divisible
				parentRight=parentRight.parents.get(0);
			}else{
//				if(parentRight.children.size()==1)parentRight.children.clear();
				parentRight.splitChildren();
				parentRight.children.get(0).add(newNode);
				if(!parentRight.children.get(1).children.isEmpty()&&parentRight.children.get(1).children.get(0).parents.size()==1){//node has subnodes
					newNode.addSubnodes(parentRight.children.get(1));
				}else{
					parentRight.children.get(1).add(newNode);
				}
			}
		}
		return parentRight;
	}
	public void recoverRecursion(AddedNodes addedNodes,HashSet<Instance> removedInstances) {
		this.in.subList(this.in.size()-addedNodes.in, this.in.size()).clear();//remove added nodes
		this.out.subList(this.out.size()-addedNodes.out, this.out.size()).clear();//remove added nodes
		for(Instance instance : removedInstances){
			ArrayList<Node> nodes = new ArrayList<Node>();
			nodes.addAll(instance.in);
			nodes.addAll(instance.out);
			this.add(instance.definition,nodes.toArray(new Node[nodes.size()]));
		}
	}

	public void nodeFission() {
		//POST: nodeFission() doesn't remove any node, only transforms ADDING subnodes if needed
		for(Node outNode:this.out){
			outNode.childrenFission();
		}
		for(Node outNode:this.out){
			outNode.breakSubnodes();
		}
		this.update();
		for(Node outNode:this.out){
			outNode.parentsFission();
		}
	}
	public void toNandDefinitions() {
		boolean expanded=true;
		ArrayList<Instance> instances = new ArrayList<Instance>();
		while(expanded){
			instances.clear();
			instances.addAll(this.instances);
			expanded=false;
			for(Instance instance:instances){
				if(instance.definition.name!="nand"){
					expanded=true;
					this.expandInstanceToNandInstances(instance);
					this.instances.remove(instance);
				}
			}
		}
		
		
	}
	void expandInstanceToNandInstances(Instance instance) {
			HashMap<Node,Node> definitionToInstanceNodes = new HashMap<Node,Node>();
			for (int i = 0; i < instance.in.size(); i++) {//map in nodes
				definitionToInstanceNodes.put(instance.definition.in.get(i), instance.in.get(i));
				mapSubnodeChildren(instance.in.get(i),instance.definition.in.get(i),definitionToInstanceNodes);
				
			}
			for (int i = 0; i < instance.out.size(); i++) {//map out nodes
				definitionToInstanceNodes.put(instance.definition.out.get(i), instance.out.get(i));
				instance.out.get(i).outOfInstance=null;
				mapSubnodeParents(instance.out.get(i),instance.definition.out.get(i),definitionToInstanceNodes);
			}
			for(Instance definitionInstance:instance.definition.instances){
					ArrayList<Node> nodes = new ArrayList<Node>();
					for (Node node: definitionInstance.in) {//map in nodes
						if(definitionToInstanceNodes.containsKey(node)){
							nodes.add(definitionToInstanceNodes.get(node));
						}else{
							Node newNode = new Node();
							definitionToInstanceNodes.put(node, newNode);
							nodes.add(newNode);
							for(Node parent:node.parents){//map parent nodes //think don't need to map children
								if(definitionToInstanceNodes.containsKey(parent)){
									definitionToInstanceNodes.get(parent).add(definitionToInstanceNodes.get(node));
								}else{
									Node newParent = new Node();
									newParent.add(definitionToInstanceNodes.get(node));
									definitionToInstanceNodes.put(parent, newParent);
								}
							}
						}
						
					}
					for (Node node: definitionInstance.out) {//map out nodes
						if(definitionToInstanceNodes.containsKey(node)){
							nodes.add(definitionToInstanceNodes.get(node));
						}else{
							Node defNode = new Node();
							definitionToInstanceNodes.put(node, defNode);
							nodes.add(defNode);
							for(Node parent:node.parents){//map parent nodes //think don't need to map children
								if(definitionToInstanceNodes.containsKey(parent)){
									definitionToInstanceNodes.get(parent).add(definitionToInstanceNodes.get(node));
								}else{
									Node newParent = new Node();
									newParent.add(definitionToInstanceNodes.get(node));
									definitionToInstanceNodes.put(parent, newParent);
								}
							}
						}
					}
					this.add(definitionInstance.definition,nodes.toArray(new Node[nodes.size()]));
			}
			
		}
	public void fusion() {
		HashSet<Node> inNodes = new HashSet<Node>();
		HashMap<Node,ArrayList<Instance>> in0OfInstances = new HashMap<Node,ArrayList<Instance>>();
		HashMap<Node,ArrayList<Instance>> in1OfInstances = new HashMap<Node,ArrayList<Instance>>();
		inNodes.addAll(this.in);
		for(Node outNode:this.out){
			outNode.carryNodeIndexes(inNodes, in0OfInstances,in1OfInstances);
		}
	}
	void mapSupernodeOuts(HashSet<Node> supernodeParents) {
		for(Node outNode:this.out){
			outNode.mapSupernodeParents(supernodeParents);
		}
	}
	public void clearRoot() {
		Instance instance = this.out.get(0).findRootInstance();
		Definition def = null;
		if(instance!=null)def=instance.definition;
		if(def!=null)def.rootIn.remove(this);
		
	}
	public void getRoot() {
		Instance instance = this.out.get(0).findRootInstance();
		Definition def = null;
		if(instance!=null)def=instance.definition;
		if(def!=null&&def!=this){
			if(!def.rootIn.contains(this))def.rootIn.add(this);
		}
		
	}
	public Definition copy() {
		HashMap<Node,Node> nodeToCopy = new HashMap<Node,Node>();
		HashMap<Instance,Instance> instanceToCopy = new HashMap<Instance,Instance>();
		HashSet<Node> inNodes = new HashSet<Node>();
		inNodes.addAll(this.in);
		Definition copyDef = new Definition(0,0,this.name+"Copy");
		for(int i=0;i<this.in.size();i++){
			Node inNode = this.copyInNode(this.in.get(i), nodeToCopy, instanceToCopy,copyDef);
			copyDef.in.add(inNode);
			copyDef.add(inNode);
		}
		for(int i=0;i<this.out.size();i++){
			Node outNode = this.copyNode(this.out.get(i),nodeToCopy,instanceToCopy,copyDef);
			copyDef.out.add(outNode);
			copyDef.add(outNode);
		}
		return copyDef;
	}
	private Node copyInNode(Node node, HashMap<Node, Node> nodeToCopy,
			HashMap<Instance, Instance> instanceToCopy, Definition copyDef) {
		Node copyNode;
		if(!nodeToCopy.containsKey(node)){
			copyNode=new Node();
			nodeToCopy.put(node, copyNode);
			if(!node.parents.isEmpty()){
				if(node.parents.size()==1){
						Node parent=node.parents.get(0);
						Node copyParent;
						if(!nodeToCopy.containsKey(parent)){
							copyParent = this.copyInNode(parent,nodeToCopy, instanceToCopy, copyDef);
						}else{
							copyParent = nodeToCopy.get(parent);
						}
						for(int i=0;i<parent.children.size();i++){
							if(!nodeToCopy.containsKey(parent.children.get(i))){
								Node copyChildren = new Node();
								copyParent.add(copyChildren);
								nodeToCopy.put(parent.children.get(i),copyChildren);
							}else{
								copyParent.add(nodeToCopy.get(parent.children.get(i)));
							}
						}
				}else{
					for(int i=0;i<node.parents.size();i++){
						if(!nodeToCopy.containsKey(node.parents.get(i))){
							Node copyParent = this.copyInNode(node.parents.get(i), nodeToCopy, instanceToCopy, copyDef);
							copyParent.add(copyNode);
						}else{
							nodeToCopy.get(node.parents.get(i)).add(copyNode);
						}
					}
				}
			}
		}else{
			copyNode=nodeToCopy.get(node);
		}
		return copyNode;
	}
	private Node copyNode(Node node,
			HashMap<Node, Node> nodeToCopy,
			HashMap<Instance, Instance> instanceToCopy, Definition copyDef) {
		Node copyNode;
		if(!nodeToCopy.containsKey(node)){
			copyNode=new Node();
			nodeToCopy.put(node, copyNode);
			if(!node.parents.isEmpty()){
				if(node.parents.size()==1){
						Node parent=node.parents.get(0);
						Node copyParent;
						if(!nodeToCopy.containsKey(parent)){
							copyParent = this.copyNode(parent,nodeToCopy, instanceToCopy, copyDef);
						}else{
							copyParent = nodeToCopy.get(parent);
						}
						for(int i=0;i<parent.children.size();i++){
							if(!nodeToCopy.containsKey(parent.children.get(i))){
								Node copyChildren = new Node();
								copyParent.add(copyChildren);
								nodeToCopy.put(parent.children.get(i),copyChildren);
							}else{
								copyParent.add(nodeToCopy.get(parent.children.get(i)));
							}
						}
				}else{
					for(int i=0;i<node.parents.size();i++){
						if(!nodeToCopy.containsKey(node.parents.get(i))){
							Node copyParent = this.copyNode(node.parents.get(i), nodeToCopy, instanceToCopy, copyDef);
							copyParent.add(copyNode);
						}else{
							nodeToCopy.get(node.parents.get(i)).add(copyNode);
						}
					}
				}
			}
			if(node.outOfInstance!=null){
					this.copyInstance(node.outOfInstance,nodeToCopy,instanceToCopy,copyDef);
			}
		}else{
			copyNode=nodeToCopy.get(node);
		}
		return copyNode;
	}
	private void copyInstance(Instance instance,
			HashMap<Node, Node> nodeToCopy,
			HashMap<Instance, Instance> instanceToCopy, Definition copyDef) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		for(int i=0;i<instance.in.size();i++){
			if(nodeToCopy.containsKey(instance.in.get(i))){
				nodes.add(nodeToCopy.get(instance.in.get(i)));
			}else{
				nodes.add(this.copyNode(instance.in.get(i), nodeToCopy, instanceToCopy, copyDef));
			}
		}
		for(int i=0;i<instance.out.size();i++){
			if(nodeToCopy.containsKey(instance.out.get(i))){
				nodes.add(nodeToCopy.get(instance.out.get(i)));
			}else{
				nodes.add(this.copyNode(instance.out.get(i), nodeToCopy, instanceToCopy, copyDef));
			}
		}
//		if(instance.definition!=this){

		if(!instanceToCopy.containsKey(instance)){
			instanceToCopy.put(instance,copyDef.add(instance.definition,nodes.toArray(new Node[nodes.size()])));
		}
//		}else{
//			instanceToCopy.put(instance,copyDef.add(copyDef,nodes.toArray(new Node[nodes.size()])));
//		}
	}
	public void expandInstance(Instance instance) {
		this.removeInstance(instance);
		this.expandInstanceInstances(instance);
	}
	void removeInstance(Instance instance) {
		for (int i = 0; i < instance.out.size(); i++) {//add out nodes to def in
			instance.out.get(i).outOfInstance=null;
		}
		this.instances.remove(instance);
		this.instancesOfRecursiveDefinitions.remove(instance);
		this.selfRecursiveInstances.remove(instance);
	}
	void expandInstanceInstances(Instance instance) {
		HashMap<Node,Node> definitionToInstanceNodes = new HashMap<Node,Node>();
		for (int i = 0; i < instance.in.size(); i++) {//map in nodes
			definitionToInstanceNodes.put(instance.definition.in.get(i), instance.in.get(i));
			mapSubnodeParents(instance.in.get(i),instance.definition.in.get(i),definitionToInstanceNodes);
			mapSubnodeChildren(instance.in.get(i),instance.definition.in.get(i),definitionToInstanceNodes);	
			mapSupernodeChildren(instance.in.get(i),instance.definition.in.get(i),definitionToInstanceNodes);
		}
		for (int i = 0; i < instance.out.size(); i++) {//map out nodes
			definitionToInstanceNodes.put(instance.definition.out.get(i), instance.out.get(i));
			instance.out.get(i).outOfInstance=null;
			mapSubnodeParents(instance.out.get(i),instance.definition.out.get(i),definitionToInstanceNodes);
//			mapSubnodeChildren(instance.out.get(i),instance.definition.out.get(i),definitionToInstanceNodes);	
//			mapSupernodeChildren(instance.out.get(i),instance.definition.out.get(i),definitionToInstanceNodes);
		}
		for(Instance definitionInstance:instance.definition.instances){
				ArrayList<Node> nodes = new ArrayList<Node>();
				for (Node node: definitionInstance.in) {//map in nodes
					if(definitionToInstanceNodes.containsKey(node)){
						nodes.add(definitionToInstanceNodes.get(node));
					}else{
						Node newNode = new Node();
						definitionToInstanceNodes.put(node, newNode);
						nodes.add(newNode);
						node.recursivelyMapParents(definitionToInstanceNodes);
					}
				}
				for (Node node: definitionInstance.out) {//map out nodes
					if(definitionToInstanceNodes.containsKey(node)){
						nodes.add(definitionToInstanceNodes.get(node));
					}else{
						Node newNode = new Node();
						definitionToInstanceNodes.put(node, newNode);
						nodes.add(newNode);
						node.recursivelyMapParents(definitionToInstanceNodes);
					}
				}
				this.add(definitionInstance.definition,nodes.toArray(new Node[nodes.size()]));
		}
		
	}
	public void replaceDefinition(Definition definition, Definition newDef) {
		for(Instance instance :this.instances){
			if(instance.definition==definition){
				this.instancesOfRecursiveDefinitions.remove(instance);
				this.selfRecursiveInstances.remove(instance);
				instance.definition=newDef;
				if(this==newDef){
					this.selfRecursiveInstances.add(instance);
				}else if(!newDef.selfRecursiveInstances.isEmpty()||!newDef.instancesOfRecursiveDefinitions.isEmpty()){
					this.instancesOfRecursiveDefinitions.add(instance);
				}
			}
		}
		
	}
	public void locateUnchangedNodes(Definition tempDef,
			ArrayList<Node> newRecursiveDefinitionIn,
			ArrayList<Node> newRecursiveDefinitionOut) {
		HashMap<Node,Node> definitionToTempDefNodes = new HashMap<Node,Node>();
		this.locateUnchangedIns(tempDef,newRecursiveDefinitionIn,definitionToTempDefNodes);
		this.locateUnchangedOuts(tempDef,newRecursiveDefinitionOut,definitionToTempDefNodes);
	}
	private void locateUnchangedIns(Definition tempDef,
			ArrayList<Node> newRecursiveDefinitionIn,
			HashMap<Node, Node> definitionToTempDefNodes) {
		for(int i=0;i<this.in.size();i++){
			this.mapUnchangedIns(this.in.get(i), tempDef.in.get(i),newRecursiveDefinitionIn,definitionToTempDefNodes);
		}
	}
	private void mapUnchangedIns(Node node, Node tempNode,
			ArrayList<Node> newRecursiveDefinitionIn,
			HashMap<Node, Node> definitionToTempDefNodes) {
		if(!definitionToTempDefNodes.containsKey(node)){
			definitionToTempDefNodes.put(node,tempNode);
		}
		
	}
	private void locateUnchangedOuts(Definition tempDef,
			ArrayList<Node> newRecursiveDefinitionOut,
			HashMap<Node, Node> definitionToTempDefNodes) {
		for(int i=0;i<this.out.size();i++){
			this.mapUnchangedOuts(this.out.get(i), tempDef.out.get(i),newRecursiveDefinitionOut,definitionToTempDefNodes);
		}
		
	}
	private void mapUnchangedOuts(Node node, Node tempNode,
			ArrayList<Node> newRecursiveDefinitionOut,
			HashMap<Node, Node> definitionToTempDefNodes) {
		if(!definitionToTempDefNodes.containsKey(node)){
			definitionToTempDefNodes.put(node,tempNode);
			if(node.parents.size()!=tempNode.parents.size()||(node.outOfInstance==null&&tempNode.outOfInstance!=null)||(node.outOfInstance!=null&&tempNode.outOfInstance==null)){
				newRecursiveDefinitionOut.add(node);
			}else{
				for(int i=0;i<node.parents.size();i++){
					this.mapUnchangedOuts(node.parents.get(i), tempNode.parents.get(i), newRecursiveDefinitionOut, definitionToTempDefNodes);
				}
				if(node.outOfInstance==null){
					this.mapUnchangedOuts(node.outOfInstance.in.get(0), tempNode.outOfInstance.in.get(0), newRecursiveDefinitionOut, definitionToTempDefNodes);
					this.mapUnchangedOuts(node.outOfInstance.in.get(1), tempNode.outOfInstance.in.get(1), newRecursiveDefinitionOut, definitionToTempDefNodes);
				}
			}
		}
		
	}
	public void extractNewRecursiveDefinition(
			ArrayList<Node> newRecursiveDefinitionIn,
			ArrayList<Node> newRecursiveDefinitionOut) {
		// TODO Auto-generated method stub
		
	}
	public void expandInstances(Definition definition) {
		ArrayList<Instance> instances = new ArrayList<Instance>();
		instances.addAll(this.instances);
		for(Instance instance : instances){
			if(instance.definition==definition){//need to expand on previous definition
				this.expandInstance(instance);				
			}
		}
	}
	public void mapNodes(HashSet<Node> originalNodes) {
		originalNodes.addAll(this.nodes);	
	}
	NandForest toNandForestMapping(ArrayList<Node> nandToNodeIn,
			ArrayList<Node> nandToNodeOut, HashMap<Node, NandNode> nodeToNand, AddedNodes addedNodes, ArrayList<NandNode> recursionInNandNodes, ArrayList<NandNode> recursionOutNandNodes) {
			//PRE: this definition is not recursive and doesn't contain recursive definitions
				// the nodes have been split to the minimum needed 
				//POST: returns a NandForest equivalent to this definition, map of in and map of out nandnodes to nodes
				//more efficient thanks to HashMap's of unique nodes
				//TOPDOWN OR DOWNUP? DOWNUP less branches, UPDOWN less memory? -> DOWNUP needed to optimize and instance
				NandForest nandForest = new NandForest(0);
				
				///Need to map subnodes of ins and outs to conserve references!!!
				this.mapInsMapping(nodeToNand,nandForest,nandToNodeIn,addedNodes.in,recursionOutNandNodes);
				this.mapOutsMapping(nodeToNand,nandForest,nandToNodeOut,addedNodes.out,recursionInNandNodes);
				//IN and OUTS mapped and in nandForest
				//can't optimize nandForest here since Maps will lose references
				return nandForest;
	}
	private void mapOutsMapping(HashMap<Node, NandNode> nodeToNand,
			NandForest nandForest, ArrayList<Node> nandToNodeOut, int out,
			ArrayList<NandNode> recursionInNandNodes) {
		int nandOut = 0;
		for(int i=0;i<this.out.size();i++){
			if(i==this.out.size()-out){
				nandOut=nandForest.out.size();
			}
			this.out.get(i).mapOutParents(nodeToNand,nandForest, nandToNodeOut);
		}
		recursionInNandNodes.addAll(nandForest.out.subList(nandOut, nandForest.out.size()));
	}
	private void mapInsMapping(HashMap<Node, NandNode> nodeToNand,
			NandForest nandForest, ArrayList<Node> nandToNodeIn, int in,
			ArrayList<NandNode> recursionOutNandNodes) {
		int nandIn = 0;
		for(int i=0;i<this.in.size();i++){
			if(i==this.in.size()-in){
				nandIn=nandForest.in.size();
			}
			this.in.get(i).mapInChildren(nodeToNand, nandForest, nandToNodeIn);
		}
		recursionOutNandNodes.addAll(nandForest.in.subList(nandIn, nandForest.in.size()));
	}
	void mapFission(HashSet<Node> originalNodes) {
		ArrayList <Node> nodes = new ArrayList<Node>();
		nodes.addAll(originalNodes);
		for(Node node:nodes){
			node.nodeMapFission(this,originalNodes);
		}
	}
	public void update() {
		//update definition nodes and instances
		this.nodes.clear();
		this.maxNode=0;
		this.instances.clear();
		this.instancesOfRecursiveDefinitions.clear();
		this.selfRecursiveInstances.clear();
		for(Node inNode:this.in){
			inNode.mapIns(this);
		}
		for(Node outNode:this.out){
			outNode.updateDefinition(this);
		}
	}
	public void clearInstances() {
		this.instances.clear();
		for(Node node:this.nodes){
			node.outOfInstance=null;
		}
		
	}
	public void flattenParents() {
		//POST: flattenParents remove nodes
		for(Node outNode:this.out){
			outNode.flattenParents();
		}
	}
//	public void eval(HashMap<Node, FixedBitSet> valueMap) {
//		Queue<Node> nodesToExpand = new LinkedList<Node>();
//		Queue<Instance> instancesToExpand = new LinkedList<Instance>();
//		for(Node node:this.out){
//			nodesToExpand.add(node);
//		}
//		boolean allOuts=false;
//		while(!allOuts){//evaluation ends, when all outs are evaluated
//			allOuts=true;
//			for(Node outNode:this.out){
//				allOuts&=valueMap.containsKey(outNode);
//			}
//			if(!allOuts){
//				if(nodesToExpand.isEmpty()){
//					instancesToExpand.pop().eval(valueMap);//second priority queue: instances
//				}else{
//					nodesToExpand.poll().eval(valueMap,nodesToExpand,instancesToExpand);
//				}
//			}
//		}
//	}
}
