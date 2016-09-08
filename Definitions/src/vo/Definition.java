/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import utils.AddedNodes;
import utils.FixedBitSet;

//DESCRIPTION
//-Defines a "Definition", procedure, function with multiple outputs
//-Recursive definition:
//	-A definition is composed by instances of definitions
//	-Nand is the basic definition
//-Definitions may be recursive and ARE TURING COMPLETE (equivalent to algorithms)
//-Definitions don't accept duplicate instances, via a tree set
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
	//TODO: add id to definition? use instead of hash in instance ordering
	public int maxNode=0;//keep track for easy consistency while removing nodes
	public String name;
	public ArrayList<Node> in;
	public ArrayList<Node> out;
	public ArrayList<ArrayList<Instance>> instances;//TODO: better data structure Hash AND list linkedhashmap?  - replaces def?
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
		this.instances = new ArrayList<ArrayList<Instance>>();
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
		this.instances = new ArrayList<ArrayList<Instance>>();
		this.selfRecursiveInstances = new HashSet<Instance>();
		this.instancesOfRecursiveDefinitions = new HashSet<Instance>();
		this.rootIn = new ArrayList<Definition>();
		this.nodes = new HashSet<Node> ();
	}
	public NandForest toNandForest(HashMap<NandNode, Node> nandToNode,HashMap<Node, NandNode> nodeToNand, HashMap<Node, Node> equivalentNode){
		//PRE: this definition is not recursive and doesn't contain recursive definitions
		// the nodes have been split to the minimum needed 
		//POST: returns a NandForest equivalent to this definition, map of in and map of out nandnodes to nodes
		//more efficient thanks to HashMap's of unique nodes
		//TOPDOWN OR DOWNUP? DOWNUP less branches, UPDOWN less memory? -> DOWNUP needed to optimize and instance
		NandForest nandForest = new NandForest(0);
		///Need to map subnodes of ins and outs to conserve references!!!
		this.mapIns(nandToNode,nodeToNand,nandForest);
		this.mapOuts(nandToNode,nodeToNand,nandForest,equivalentNode);
		nandForest.optimize();
		return nandForest;
	}
	void mapIns(HashMap<NandNode, Node> nandToNode,HashMap<Node, NandNode> nodeToNand, NandForest nandForest) {
		//map input nodes to nandNodes
		for(Node inNode:this.in){
			inNode.mapInChildren(nandToNode,nodeToNand, nandForest);
		}
	}
	void mapOuts(HashMap<NandNode, Node> nandToNode,HashMap<Node, NandNode> nodeToNand,
			NandForest nandForest, HashMap<Node, Node> equivalentNode) {
		//map output nodes to nandNodes
		for(Node outNode:this.out){
			outNode.mapOutParents(nandToNode,nodeToNand,nandForest,equivalentNode);
		}	
	}
	public Instance add(Definition def,Node ... nodes){
		for(Node node:nodes){
			this.add(node);	
		}
		Instance instance = new Instance();//node == instance of a definition
		ArrayList<Node> inNodes = new ArrayList<Node>(Arrays.asList(nodes).subList(0, def.in.size()));
		instance.in = inNodes;
//		for(Node inNode:inNodes){
//			instance.in.add(inNode.equivalentNode());
//		}
		instance.out = new ArrayList<Node>(Arrays.asList(nodes).subList(def.in.size(), def.in.size()+def.out.size()));
		instance.definition=def;
		this.add(instance);
		return instance;
	}
	public void add(Node node){
		if(!this.nodes.contains(node)){
			node.idForDefinition=this.maxNode;//debugging only
			this.maxNode++;
			node.definition=this;
			this.nodes.add(node);
			if(node.parentSupernode!=null) this.add(node.parentSupernode);
			for(Node parent:node.parentSubnodes){
				this.add(parent);
			}
			for(Node child:node.childrenSubnodes){
				this.add(child);
			}
			for(Node child:node.childrenSupernodes){
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
		
		string+=(" =\n");
		for(ArrayList<Instance> setOfInstances:this.instances){
			for (Instance instance : setOfInstances) {//print instances
			    string+=" "+instance.toString(this);
			}
			string+=("\n");
		}
		string+=(" root in: ");
		for (Definition root : this.rootIn){
			string+=(root.name);
			string+=(",");
		}
		string+="\n";
		return string;
	}
//	public boolean apply(Instance instance, Definition appliedDefinition, HashSet<Node> supernodeOuts) {
//		//PRE: .this is the definition where instance is, instance is the root instance, appliedDefinition the definition applied
//		//POST:apply definition with instance as root of appliedDefinition
//		// and replaces existing instances with instance to an existing applied definition
//		//TODO: replace only non outside referenced instances (without intersections),if the replaced instances have any halfway node don't replace
//		//MAYBE: verify that all roots are expanded (if needed, +++cost)
//		Instance expandingInstance=instance;
//		Instance expandingAppliedInstance=null;
//		HashSet<Instance> toExpand = new HashSet<Instance>();//instances to expand
//		HashMap<Instance,Instance> instanceMap = new HashMap<Instance,Instance>();//map of expanded instances
//		HashMap<Node,Node> nodeMap = new HashMap<Node,Node>();//map of expanded nodes
//		if(appliedDefinition.out!=null&&!appliedDefinition.out.isEmpty()&&this.name!="nand"){//if applied definition is not nand//FIXME: only third term needed?//FIX nand checking
//			expandingAppliedInstance=appliedDefinition.out.get(0).findRootInstance();
//			instanceMap.put(expandingAppliedInstance,expandingInstance);
//			toExpand.add(expandingAppliedInstance);
//			while(!toExpand.isEmpty()){
//				if(expandingInstance==null||expandingInstance.definition!=expandingAppliedInstance.definition){//instance must be to same definition and nºins and nºout must be equal
//					return false;
//				}else{
//					for (int i = 0; i < expandingAppliedInstance.in.size(); i++){//expand all in nodes
//						if(!this.expandNodes(expandingAppliedInstance.in.get(i),expandingInstance.in.get(i),nodeMap,instanceMap,toExpand)){//added as different node (non unique)
//							return false;
//						}
//					}
//					for (int i = 0; i < expandingAppliedInstance.out.size(); i++){//expand all out nodes
//						if(!this.expandNodes(expandingAppliedInstance.out.get(i),expandingInstance.out.get(i),nodeMap,instanceMap,toExpand)){//added as different node (non unique)
//							return false;
//						}
//					}
//					toExpand.remove(expandingAppliedInstance);
//					if(toExpand.iterator().hasNext()){
//						expandingAppliedInstance=toExpand.iterator().next();
//						expandingInstance=instanceMap.get(expandingAppliedInstance);
//					}
//					
//				}
//				
//			}
//			ArrayList<Node> inArray = new ArrayList<Node>();
//			ArrayList<Node> outArray = new ArrayList<Node>();
//			for (Node node : appliedDefinition.in) {
//				inArray.add(nodeMap.get(node));
//			}
//			for (Node node : appliedDefinition.out) {
//				Node outNode=nodeMap.get(node);
//				outArray.add(outNode);
//				outNode.outOfInstance=instance;
//			}
//			//replace instance
//			instance.in=inArray;
//			instance.definition=appliedDefinition;
//			//Connect out
//			instance.out=outArray;
//			//remove replaced instances (except "instance" already overwritten) 
//			HashSet<Instance> expandedInstances = new HashSet<Instance>();//make a HashSet of all the expanded instances of definition
//			expandedInstances.addAll(instanceMap.values());//(part of this definition) for future verification
//			HashSet<Instance> removableInstances = new HashSet<Instance>(expandedInstances);//copy all the expanded instances
//			//select removable instances
//			for(Instance thisInstance : this.instances){
//				if(!expandedInstances.contains(thisInstance)){
//					for(Node node :thisInstance.in){
//						removableInstances.remove(node.outOfInstance);
//					}
//				}
//			}
//			//remove the already replaced instance (to applied definition)
//			removableInstances.remove(instance);
//			//remove the removable instances
//			for (Instance removableInstance : removableInstances) {
//				boolean containsInOutNode=false;//checking not to remove a non removable node
//				for(Node outNode:removableInstance.out){
//					if(supernodeOuts.contains(outNode.supernodeParent())) containsInOutNode=true;
//				}
//				if(!containsInOutNode) this.removeInstance(removableInstance);//TODO:change to ¿list?HASH¿map? to remove in O(1) instead O(n)?
//			}
//		}
//		return true;
//	}
//	private boolean expandNodes(Node appliedNode, Node node,
//			HashMap<Node, Node> nodeMap, HashMap<Instance, Instance> instanceMap, HashSet<Instance> toExpand) {
//		if(appliedNode.outOfInstance!=null){
//			if(instanceMap.containsKey(appliedNode.outOfInstance)){//already added instance
//				if(instanceMap.get(appliedNode.outOfInstance)!=node.outOfInstance){//added as instance to different definition
//					return false;
//				}
//			}else{//non-added instance
//				if(node.outOfInstance==null){
//					return false;
//				}else{
//					instanceMap.put(appliedNode.outOfInstance,node.outOfInstance);
//					toExpand.add(appliedNode.outOfInstance);
//				}
//			}
//		}
//		if(nodeMap.containsKey(appliedNode)){//already added node
//			if(nodeMap.get(appliedNode)!=node){
//				return false;
//			}
//		}else{//non-added node
//			nodeMap.put(appliedNode,node);//expand node
//			if(!appliedNode.childrenSupernodes.isEmpty()||!appliedNode.childrenSubnodes.isEmpty()||!appliedNode.parents.isEmpty()){
//				if(node.parents.size()!=appliedNode.parents.size()){
//					return false;
//				}
////				for(int i=0;i<appliedNode.parents.size();i++){
////					if(!expandNodes(appliedNode.parents.get(i),node.parents.get(i),nodeMap,instanceMap, toExpand)){
////						return false;
////					}
////				}
//				if(node.childrenSubnodes.size()!=appliedNode.childrenSubnodes.size()){
//					return false;
//				}
//				if(node.childrenSupernodes.size()!=appliedNode.childrenSupernodes.size()){
//					return false;
//				}
////				for(int i=0;i<appliedNode.childrenSubnodes.size();i++){
////					if(!expandNodes(appliedNode.childrenSubnodes.get(i),node.childrenSubnodes.get(i),nodeMap,instanceMap, toExpand)){
////						return false;
////					}
////				}
//			}
//		}
//		return true;
//	}
	public void printCost(){
		String strCost=this.cost();
		System.out.println("Definition cost in nands: "+strCost);
		String strParallelCost=this.parallelCost();
		System.out.println("Definition cost in parallel nands: "+strParallelCost);
	}
	private String parallelCost() {
		Definition copyDef=this.copy();
		copyDef.replaceDefinition(this, copyDef);
		String strCost = new String();
		if(copyDef.selfRecursiveInstances.isEmpty()&&copyDef.instancesOfRecursiveDefinitions.isEmpty()){//definition has no recursion
			copyDef.toNandInstances();
			copyDef.fission();
			int iterationCost=copyDef.instances.size();
			strCost=String.valueOf(iterationCost);
		}else{
			AddedNodes addedNodes = new AddedNodes();
			HashSet<Instance> removedInstances = new HashSet<Instance>();
			for(Instance instanceOfRecursiveDefinition:copyDef.instancesOfRecursiveDefinitions){
				strCost+=instanceOfRecursiveDefinition.definition.cost()+"+";
				copyDef.removeRecursiveInstance(instanceOfRecursiveDefinition, addedNodes, removedInstances);
			}
			copyDef.instancesOfRecursiveDefinitions.clear();
			copyDef.removeRecursion(addedNodes, removedInstances);
			copyDef.toNandInstances();
			copyDef.fission();
			int iterationCost=copyDef.instances.size();
			int nodesEvaluatedByIteration=1;//TODO: calculate nodes evaluated by iteration (index on recursive call)
			if(this.selfRecursiveInstances.isEmpty()){
				strCost+=String.valueOf(iterationCost);//TODO: n is a generic variable name, for now
			}else{
				if(nodesEvaluatedByIteration==1){
					if(this.selfRecursiveInstances.size()<2){
						strCost+=String.valueOf(iterationCost+"*n");//TODO: n is a generic variable name, for now
					}else{//log base number of selfRecursiveInstances
						strCost+=String.valueOf(iterationCost+"*n*log*"+String.valueOf(this.selfRecursiveInstances.size()));
					}
				}else{
					if(this.selfRecursiveInstances.size()<2){
						strCost+=String.valueOf(iterationCost+"*n/"+nodesEvaluatedByIteration);
					}else{//log base number of selfRecursiveInstances
						strCost+=String.valueOf(iterationCost+"*n*log*"+String.valueOf(this.selfRecursiveInstances.size())+"/"+nodesEvaluatedByIteration);
					}
				}
			}
		}
		return strCost;
	}
	private String cost() {
		Definition copyDef=this.copy();
		copyDef.replaceDefinition(this, copyDef);
		String strCost = new String();
		if(copyDef.selfRecursiveInstances.isEmpty()&&copyDef.instancesOfRecursiveDefinitions.isEmpty()){//definition has no recursion
			copyDef.toNandInstances();
			copyDef.fission();
			int iterationCost=0;
			for(ArrayList<Instance> instanceHashSet:this.instances){
				for(@SuppressWarnings("unused") Instance instance:instanceHashSet){
					iterationCost++;
				}
			}
			strCost=String.valueOf(iterationCost);
		}else{
			AddedNodes addedNodes = new AddedNodes();
			HashSet<Instance> removedInstances = new HashSet<Instance>();
			for(Instance instanceOfRecursiveDefinition:copyDef.instancesOfRecursiveDefinitions){
				strCost+=instanceOfRecursiveDefinition.definition.cost()+"+";
				copyDef.removeRecursiveInstance(instanceOfRecursiveDefinition, addedNodes, removedInstances);
			}
			copyDef.instancesOfRecursiveDefinitions.clear();
			copyDef.removeRecursion(addedNodes, removedInstances);
			copyDef.toNandInstances();
			copyDef.fission();
			int iterationCost=0;
			for(ArrayList<Instance> instanceHashSet:this.instances){
				for(@SuppressWarnings("unused") Instance instance:instanceHashSet){
					iterationCost++;
				}
			}
			int nodesEvaluatedByIteration=1;//TODO: calculate nodes evaluated by iteration (index on recursive call)
			if(this.selfRecursiveInstances.isEmpty()){
				strCost+=String.valueOf(iterationCost);//TODO: n is a generic variable name, for now
			}else{
				if(nodesEvaluatedByIteration==1){
					if(this.selfRecursiveInstances.size()<2){
						strCost+=String.valueOf(iterationCost+"*n");//TODO: n is a generic variable name, for now
					}else{//log base number of selfRecursiveInstances
						strCost+=String.valueOf(iterationCost+"*n*log*"+String.valueOf(this.selfRecursiveInstances.size()));
					}
				}else{
					if(this.selfRecursiveInstances.size()<2){
						strCost+=String.valueOf(iterationCost+"*n/"+nodesEvaluatedByIteration);
					}else{//log base number of selfRecursiveInstances
						strCost+=String.valueOf(iterationCost+"*n*log*"+String.valueOf(this.selfRecursiveInstances.size())+"/"+nodesEvaluatedByIteration);
					}
				}
			}
		}
		return strCost;
	}
	public void printEval(String ... strings){
		ArrayList<String> ins = new ArrayList<String>();
		ArrayList<String> outs = new ArrayList<String>();
		//eval out nodes using BFS
		HashMap<Node, FixedBitSet> valueMap = new HashMap<Node, FixedBitSet>() ;
		for(int i=0;i<this.in.size();i++){
			valueMap.put(this.in.get(i), FixedBitSet.fromString(strings[i]));
//			if(!this.in.get(i).parents.isEmpty()){//fix for in nodes as lone child
//				valueMap.put(this.in.get(i).parents.get(0), FixedBitSet.fromString(strings[i]));
//			}
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
			//eval out nodes
			boolean allOuts=false;
			int depth=0;
			while(!allOuts){//evaluation ends, when all outs are evaluated
				for(Node nodeOut:this.out){
					nodeOut.eval(valueMap, depth);
				}
				allOuts=true;
				for(Node outNode:this.out){
					allOuts&=valueMap.containsKey(outNode);
				}
				depth++;
			}
		}
		
	}
	public void eval(HashMap<Node, FixedBitSet> valueMap,int depth){
		if(this.name=="nand"){//NAND //TODO: fix nand checking
			//NAND (always 2 ins 1 out)
			if(valueMap.containsKey(this.in.get(1))&&valueMap.get(this.in.get(1)).length()==0){//if one in is empty, out is the other
				if(valueMap.containsKey(this.in.get(0))){
					valueMap.put(this.out.get(0), valueMap.get(this.in.get(0)));
				}
			}else if(valueMap.containsKey(this.in.get(0))&&valueMap.get(this.in.get(0)).length()==0){//if one in is empty, out is the other
				if(valueMap.containsKey(this.in.get(1))){
					valueMap.put(this.out.get(0), valueMap.get(this.in.get(1)));
				}
			}else if(valueMap.containsKey(this.in.get(1))//LAZY EVALUATION
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
			for(Node nodeOut:this.out){
				nodeOut.eval(valueMap, depth);
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
			this.removeInstance(instance);
			this.expandRecursiveInstance(instance, addedNodes, removedInstances);
		}
	}
	void expandRecursiveInstance(Instance instance, AddedNodes addedNodes, HashSet<Instance> removedInstances) {
		HashMap<Node,Node> definitionToInstanceNodes = new HashMap<Node,Node>();
		for (int i = 0; i < instance.in.size(); i++) {//map in nodes
			definitionToInstanceNodes.put(instance.definition.in.get(i), instance.in.get(i));
			mapSubnodeChildren(instance.in.get(i),instance.definition.in.get(i),definitionToInstanceNodes);	
		}
		for (int i = 0; i < instance.out.size(); i++) {//map out nodes
			definitionToInstanceNodes.put(instance.definition.out.get(i), instance.out.get(i));
			instance.out.get(i).outOfInstance=null;
			mapParents(instance.out.get(i),instance.definition.out.get(i),definitionToInstanceNodes);
		}
		ArrayList<Instance> instances = new ArrayList<Instance>();
		for(ArrayList<Instance> setOfInstances:this.instances){
			instances.addAll(setOfInstances);
		}
		for(Instance definitionInstance:instances){
			ArrayList<Node> nodes = new ArrayList<Node>();
			for (Node node: definitionInstance.in) {//map in nodes
				if(definitionToInstanceNodes.containsKey(node)){
					nodes.add(definitionToInstanceNodes.get(node));
				}else{
					Node newNode = new Node();
					definitionToInstanceNodes.put(node, newNode);
					nodes.add(newNode);
					mapParents(newNode,node,definitionToInstanceNodes);
				}
			}
			for (Node node: definitionInstance.out) {//map out nodes
				if(definitionToInstanceNodes.containsKey(node)){
					nodes.add(definitionToInstanceNodes.get(node));
				}else{
					Node newNode = new Node();
					definitionToInstanceNodes.put(node, newNode);
					nodes.add(newNode);
					mapParents(newNode,node,definitionToInstanceNodes);
				}
			}
			Instance newInstance=this.add(definitionInstance.definition,nodes.toArray(new Node[nodes.size()]));
			if(newInstance.definition==instance.definition){
				this.instancesOfRecursiveDefinitions.remove(newInstance);
				this.removeRecursiveInstance(newInstance, addedNodes, removedInstances);
			}else if(!newInstance.definition.selfRecursiveInstances.isEmpty()){//is recursive
				this.instancesOfRecursiveDefinitions.remove(newInstance);
				this.removeInstance(newInstance);
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
		this.removeInstance(instance);
	}
	private void mapParents(Node node, Node definitionNode, HashMap<Node, Node> definitionToInstanceNodes) {
		HashMap<Node, Node> expandedToDefinition = new HashMap<Node, Node>();
		this.mapParentsMapping(node, definitionNode, definitionToInstanceNodes, expandedToDefinition);
	}
	private void mapSubnodeChildren(Node node, Node definitionNode, HashMap<Node, Node> definitionToInstanceNodes) {
		HashMap<Node, Node> expandedToDefinition = new HashMap<Node, Node>();
		mapSubnodeChildrenMapping(node, definitionNode, definitionToInstanceNodes, expandedToDefinition);
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

	public void toNandInstances() {
		//PRE:definition is not recursive nor self recursive
		HashSet<Node> expandedNodes = new HashSet<Node>();
		expandedNodes.addAll(this.in);
		this.instances.clear();
		for(Node outNode:this.out){
			outNode.toNandDefinitions(expandedNodes);
		}
	}
//	void expandInstanceToNandInstances(Instance instance) {
//			HashMap<Node,Node> definitionToInstanceNodes = new HashMap<Node,Node>();
//			for (int i = 0; i < instance.in.size(); i++) {//map in nodes
//				definitionToInstanceNodes.put(instance.definition.in.get(i), instance.in.get(i));
//				mapSubnodeChildren(instance.in.get(i),instance.definition.in.get(i),definitionToInstanceNodes);
//			}
//			for (int i = 0; i < instance.out.size(); i++) {//map out nodes
//				definitionToInstanceNodes.put(instance.definition.out.get(i), instance.out.get(i));
//				instance.out.get(i).outOfInstance=null;
//				mapParents(instance.out.get(i),instance.definition.out.get(i),definitionToInstanceNodes);
//			}
//
//			for(ArrayList<Instance> setOfInstances:instance.definition.instances){
//				for (Instance definitionInstance : setOfInstances) {
//					ArrayList<Node> nodes = new ArrayList<Node>();
//					for (Node node: definitionInstance.in) {//map in nodes
//						if(definitionToInstanceNodes.containsKey(node)){
//							nodes.add(definitionToInstanceNodes.get(node));
//						}else{
//							Node newNode = new Node();
//							definitionToInstanceNodes.put(node, newNode);
//							nodes.add(newNode);
//							mapParents(newNode,node,definitionToInstanceNodes);
//						}
//					}
//					for (Node node: definitionInstance.out) {//map out nodes
//						if(definitionToInstanceNodes.containsKey(node)){
//							nodes.add(definitionToInstanceNodes.get(node));
//						}else{
//							Node newNode = new Node();
//							definitionToInstanceNodes.put(node, newNode);
//							nodes.add(newNode);
//							mapSubnodeChildren(newNode,node,definitionToInstanceNodes);
//						}
//					}
//					this.add(definitionInstance.definition,nodes.toArray(new Node[nodes.size()]));
//				}
//			}
//		}
//	public void fusion() {
//		HashSet<Node> inNodes = new HashSet<Node>();
//		HashMap<Node,ArrayList<Instance>> in0OfInstances = new HashMap<Node,ArrayList<Instance>>();
//		HashMap<Node,ArrayList<Instance>> in1OfInstances = new HashMap<Node,ArrayList<Instance>>();
//		inNodes.addAll(this.in);
//		for(Node outNode:this.out){
//			outNode.carryNodeIndexes(inNodes, in0OfInstances,in1OfInstances);
//		}
//	}
//	public void fusion() {
////		HashMap<Node,HashSet<Instance>> inOfNandInstances = new HashMap<Node,HashSet<Instance>>();
////		for(Node outNode:this.out){
////			outNode.mapInsOfNandInstances(inOfNandInstances);
////		}
////		for(Node inNode:this.in){
////			inNode.triFusion(inOfNandInstances);
////		}
//		HashSet<Node> expandedNodes = new HashSet<Node>();
////		for(Node outNode:this.out){
////			outNode.fusion(expandedNodes);
////		}
////		for(Node outNode:this.out){
////			outNode.recoverParentSupernodes(expandedNodes);
////		}
//		for(Node outNode:this.out){
//			outNode.biFusion();
//		}
////		for(Node outNode:this.out){
////			outNode.prune();
////		}
////		this.update();
//	}
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
		HashMap<Node,Node> copyToNode = new HashMap<Node,Node>();
		return copyMapping(nodeToCopy,copyToNode);
	}
//	private Node copyNode(Node node,
//			HashMap<Node, Node> nodeToCopy,
//			HashMap<Instance, Instance> instanceToCopy, Definition copyDef) {
//		Node copyNode;
//		if(!nodeToCopy.containsKey(node)){
//			copyNode=new Node();
//			nodeToCopy.put(node, copyNode);
//			if(!node.parents.isEmpty()){
//				if(node.parents.size()==1){
//						Node parent=node.parents.get(0);
//						Node copyParent;
//						if(!nodeToCopy.containsKey(parent)){
//							copyParent = this.copyNode(parent,nodeToCopy, instanceToCopy, copyDef);
//						}else{
//							copyParent = nodeToCopy.get(parent);
//						}
//						for(int i=0;i<parent.childrenSubnodes.size();i++){
//							if(!nodeToCopy.containsKey(parent.childrenSubnodes.get(i))){
//								Node copyChildren = new Node();
//								copyParent.addChildSubnode(copyChildren);
//								nodeToCopy.put(parent.childrenSubnodes.get(i),copyChildren);
//							}else{
//								copyParent.addChildSubnode(nodeToCopy.get(parent.childrenSubnodes.get(i)));
//							}
//						}
//				}else{
//					for(int i=0;i<node.parents.size();i++){
//						if(!nodeToCopy.containsKey(node.parents.get(i))){
//							Node copyParent = this.copyNode(node.parents.get(i), nodeToCopy, instanceToCopy, copyDef);
//							copyParent.addChildSupernode(copyNode);
//						}else{
//							nodeToCopy.get(node.parents.get(i)).addChildSupernode(copyNode);
//						}
//					}
//				}
//			}
//			if(node.outOfInstance!=null){
//				this.copyInstance(node.outOfInstance,nodeToCopy,instanceToCopy,copyDef);
//			}
//		}else{
//			copyNode=nodeToCopy.get(node);
//		}
//		return copyNode;
//	}
//	private void copyInstance(Instance instance,
//			HashMap<Node, Node> nodeToCopy,
//			HashMap<Instance, Instance> instanceToCopy, Definition copyDef) {
//		ArrayList<Node> nodes = new ArrayList<Node>();
//		
//		for(int i=0;i<instance.in.size();i++){
//			if(nodeToCopy.containsKey(instance.in.get(i))){
//				nodes.add(nodeToCopy.get(instance.in.get(i)));
//			}else{
//				nodes.add(this.copyNode(instance.in.get(i), nodeToCopy, instanceToCopy, copyDef));
//			}
//		}
//		for(int i=0;i<instance.out.size();i++){
//			if(nodeToCopy.containsKey(instance.out.get(i))){
//				nodes.add(nodeToCopy.get(instance.out.get(i)));
//			}else{
//				nodes.add(this.copyNode(instance.out.get(i), nodeToCopy, instanceToCopy, copyDef));
//			}
//		}
////		if(instance.definition!=this){
//
//		if(!instanceToCopy.containsKey(instance)){
//			instanceToCopy.put(instance,copyDef.add(instance.definition,nodes.toArray(new Node[nodes.size()])));
//		}
////		}else{
////			instanceToCopy.put(instance,copyDef.add(copyDef,nodes.toArray(new Node[nodes.size()])));
////		}
//	}
//	public void expandInstance(Instance instance) {
//		this.removeInstance(instance);
//		this.expandInstanceInstances(instance);
//	}
	void removeInstance(Instance instance) {
		for (int i = 0; i < instance.out.size(); i++) {//add out nodes to def in
			instance.out.get(i).outOfInstance=null;
		}
		ArrayList<ArrayList<Instance>> instances = new ArrayList<ArrayList<Instance>>();
		instances.addAll(this.instances);
		for(ArrayList<Instance> instanceHashSet:instances){
			instanceHashSet.remove(instance);
			if(instanceHashSet.isEmpty()) this.instances.remove(instanceHashSet);
		}
		this.instancesOfRecursiveDefinitions.remove(instance);
		this.selfRecursiveInstances.remove(instance);
	}
//	void expandInstanceInstances(Instance instance) {
//		HashMap<Node,Node> definitionToInstanceNodes = new HashMap<Node,Node>();
//		for (int i = 0; i < instance.in.size(); i++) {//map in nodes
//			definitionToInstanceNodes.put(instance.definition.in.get(i), instance.in.get(i));
//			mapSubnodeChildren(instance.in.get(i),instance.definition.in.get(i),definitionToInstanceNodes);	
//		}
//		for (int i = 0; i < instance.out.size(); i++) {//map out nodes
//			definitionToInstanceNodes.put(instance.definition.out.get(i), instance.out.get(i));
//			instance.out.get(i).outOfInstance=null;
//			mapParents(instance.out.get(i),instance.definition.out.get(i),definitionToInstanceNodes);
//		}
//		for(SortedSet<Instance> setOfInstances:instance.definition.instances){
//			for (Instance definitionInstance : setOfInstances) {
//				ArrayList<Node> nodes = new ArrayList<Node>();
//				for (Node node: definitionInstance.in) {//map in nodes
//					if(definitionToInstanceNodes.containsKey(node)){
//						nodes.add(definitionToInstanceNodes.get(node));
//					}else{
//						Node newNode = new Node();
//						definitionToInstanceNodes.put(node, newNode);
//						nodes.add(newNode);
//						mapParents(newNode,node,definitionToInstanceNodes);
//					}
//				}
//				for (Node node: definitionInstance.out) {//map out nodes
//					if(definitionToInstanceNodes.containsKey(node)){
//						nodes.add(definitionToInstanceNodes.get(node));
//					}else{
//						Node newNode = new Node();
//						definitionToInstanceNodes.put(node, newNode);
//						nodes.add(newNode);
//						mapSubnodeChildren(newNode,node,definitionToInstanceNodes);
//					}
//				}
//				this.add(definitionInstance.definition,nodes.toArray(new Node[nodes.size()]));
//			}
//		}
//	}
	public void replaceDefinition(Definition definition, Definition newDef) {
		for(ArrayList<Instance> setOfInstances:this.instances){
			for (Instance instance : setOfInstances) {
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
		
	}
//	public void locateUnchangedNodes(Definition tempDef,
//			ArrayList<Node> newRecursiveDefinitionIn,
//			ArrayList<Node> newRecursiveDefinitionOut) {
//		HashMap<Node,Node> definitionToTempDefNodes = new HashMap<Node,Node>();
//		this.locateUnchangedIns(tempDef,newRecursiveDefinitionIn,definitionToTempDefNodes);
//		this.locateUnchangedOuts(tempDef,newRecursiveDefinitionOut,definitionToTempDefNodes);
//	}
//	private void locateUnchangedIns(Definition tempDef,
//			ArrayList<Node> newRecursiveDefinitionIn,
//			HashMap<Node, Node> definitionToTempDefNodes) {
//		for(int i=0;i<this.in.size();i++){
//			this.mapUnchangedIns(this.in.get(i), tempDef.in.get(i),newRecursiveDefinitionIn,definitionToTempDefNodes);
//		}
//	}
	private void mapUnchangedIns(Node node, Node tempNode,
			ArrayList<Node> newRecursiveDefinitionIn,
			HashMap<Node, Node> definitionToTempDefNodes) {
		if(!definitionToTempDefNodes.containsKey(node)){
			definitionToTempDefNodes.put(node,tempNode);
		}
		
	}
//	private void locateUnchangedOuts(Definition tempDef,
//			ArrayList<Node> newRecursiveDefinitionOut,
//			HashMap<Node, Node> definitionToTempDefNodes) {
//		for(int i=0;i<this.out.size();i++){
//			this.mapUnchangedOuts(this.out.get(i), tempDef.out.get(i),newRecursiveDefinitionOut,definitionToTempDefNodes);
//		}
//		
//	}
//	private void mapUnchangedOuts(Node node, Node tempNode,
//			ArrayList<Node> newRecursiveDefinitionOut,
//			HashMap<Node, Node> definitionToTempDefNodes) {
//		if(!definitionToTempDefNodes.containsKey(node)){
//			definitionToTempDefNodes.put(node,tempNode);
//			if(node.parents.size()!=tempNode.parents.size()||(node.outOfInstance==null&&tempNode.outOfInstance!=null)||(node.outOfInstance!=null&&tempNode.outOfInstance==null)){
//				newRecursiveDefinitionOut.add(node);
//			}else{
//				for(int i=0;i<node.parents.size();i++){
//					this.mapUnchangedOuts(node.parents.get(i), tempNode.parents.get(i), newRecursiveDefinitionOut, definitionToTempDefNodes);
//				}
//				if(node.outOfInstance==null){
//					this.mapUnchangedOuts(node.outOfInstance.in.get(0), tempNode.outOfInstance.in.get(0), newRecursiveDefinitionOut, definitionToTempDefNodes);
//					this.mapUnchangedOuts(node.outOfInstance.in.get(1), tempNode.outOfInstance.in.get(1), newRecursiveDefinitionOut, definitionToTempDefNodes);
//				}
//			}
//		}
//		
//	}
//	public void expandInstances(Definition definition) {
//		ArrayList<Instance> instances = new ArrayList<Instance>();
//		for(SortedSet<Instance> setOfInstances:this.instances){
//			instances.addAll(setOfInstances);
//		}
//		for(Instance instance : instances){
//			if(instance.definition==definition){//need to expand on previous definition
//				this.expandInstance(instance);				
//			}
//		}
//	}
	public void mapNodes(HashSet<Node> originalNodes) {
		originalNodes.addAll(this.nodes);	
	}
//	NandForest toNandForestMapping(HashMap<Node, NandNode> nodeToNand, AddedNodes addedNodes, ArrayList<NandNode> recursionInNandNodes, ArrayList<NandNode> recursionOutNandNodes, HashMap<NandNode, Node> nandToNode) {
//			//PRE: this definition is not recursive and doesn't contain recursive definitions
//				// the nodes have been split to the minimum needed 
//				//POST: returns a NandForest equivalent to this definition, map of in and map of out nandnodes to nodes
//				//more efficient thanks to HashMap's of unique nodes
//				//TOPDOWN OR DOWNUP? DOWNUP less branches, UPDOWN less memory? -> DOWNUP needed to optimize and instance
//				NandForest nandForest = new NandForest(0);
//				
//				///Need to map subnodes of ins and outs to conserve references!!!
//				this.mapInsMapping(nodeToNand,nandForest,nandToNodeIn,addedNodes.in,recursionOutNandNodes,nandToNode);
//				this.mapOutsMapping(nodeToNand,nandForest,nandToNodeOut,addedNodes.out,recursionInNandNodes,nandToNode);
//				//IN and OUTS mapped and in nandForest
//				//can't optimize nandForest here since Maps will lose references
//				return nandForest;
//	}
//	private void mapOutsMapping(HashMap<Node, NandNode> nodeToNand,
//			NandForest nandForest, ArrayList<Node> nandToNodeOut, int out,
//			ArrayList<NandNode> recursionInNandNodes, HashMap<NandNode, Node> nandToNode) {
//		int nandOut = 0;
//		for(int i=0;i<this.out.size();i++){
//			if(i==this.out.size()-out){
//				nandOut=nandForest.out.size();
//			}
//			this.out.get(i).mapOutParentsMapping(nodeToNand,nandForest, nandToNodeOut,nandToNode);
//		}
//		recursionInNandNodes.addAll(nandForest.out.subList(nandOut, nandForest.out.size()));
//	}
//	private void mapInsMapping(HashMap<Node, NandNode> nodeToNand,
//			NandForest nandForest, ArrayList<Node> nandToNodeIn, int in,
//			ArrayList<NandNode> recursionOutNandNodes, HashMap<NandNode, Node> nandToNode) {
//		int nandIn = 0;
//		for(int i=0;i<this.in.size();i++){
//			if(i==this.in.size()-in){
//				nandIn=nandForest.in.size();
//			}
//			this.in.get(i).mapInChildrenMapping(nodeToNand, nandForest, nandToNodeIn,nandToNode);
//		}
//		recursionOutNandNodes.addAll(nandForest.in.subList(nandIn, nandForest.in.size()));
//	}
	void mapFission(HashSet<Node> originalNodes) {
		ArrayList <Node> nodes = new ArrayList<Node>();
		nodes.addAll(originalNodes);
		for(Node node:nodes){
			node.nodeMapFission(this,originalNodes);
		}
	}
	public void update() {
		//POST: resets every node id for this definition
		//update definition nodes and instances
		this.nodes.clear();
		this.maxNode=0;
		this.instances.clear();
		this.instancesOfRecursiveDefinitions.clear();
		this.selfRecursiveInstances.clear();
		HashSet<Node> expandedNodes = new HashSet<Node>();
		for(Node inNode:this.in){
			inNode.mapNode(this);
			expandedNodes.add(inNode);
		}
		for(Node outNode:this.out){
			outNode.updateNode(this,expandedNodes);
		}
	}
	public void clearInstances() {
		this.instances.clear();
		for(Node node:this.nodes){
			node.outOfInstance=null;
		}
		
	}
	public void fission() {
//				this.parentsFission();
//				this.childrenFission();
		for(Node outNode:this.out){
			outNode.expandBinodes();
		}
		for(Node outNode:this.out){
			outNode.childrenFission();
		}
		for(Node outNode:this.out){
			outNode.biFission();
		}
	}
	private void parentsFission() {//fission of nodes with multiple parents as in of nand instances
		for(Node outNode:this.out){
			outNode.parentsFission();
		}
	}
//	private void childrenFission() {//Fission of nodes with children subnodes as out of nand instances
//		HashSet<Node> expandedNodes= new HashSet<Node>();
//		for(Node outNode:this.out){
//			outNode.childrenFission(expandedNodes);
//		}
//	}
	public Definition copyMapping(
			HashMap<Node, Node> nodeToCopy, HashMap<Node, Node> copyToNode) {
		HashMap<Instance,Instance> instanceToCopy = new HashMap<Instance,Instance>();
		HashSet<Node> inNodes = new HashSet<Node>();
		inNodes.addAll(this.in);
		Definition copyDef = new Definition(0,0,this.name+"Copy");
		for(int i=0;i<this.in.size();i++){
//			Node inNode = this.copyNodeMapping(this.in.get(i), nodeToCopy, instanceToCopy,copyDef,copyToNode);
			Node copyNode;
			if(!nodeToCopy.containsKey(this.in.get(i))){
				copyNode=new Node();
				nodeToCopy.put(this.in.get(i), copyNode);
				copyToNode.put(copyNode,this.in.get(i));
			}else{
				copyNode=nodeToCopy.get(this.in.get(i));
			}
			copyDef.in.add(copyNode);
			copyDef.add(copyNode);
		}
		for(int i=0;i<this.out.size();i++){
			Node outNode = this.copyNodeMapping(this.out.get(i),nodeToCopy,instanceToCopy,copyDef,copyToNode);
			copyDef.out.add(outNode);
			copyDef.add(outNode);
		}
		return copyDef;
	}
	private Node copyNodeMapping(Node node,
			HashMap<Node, Node> nodeToCopy,
			HashMap<Instance, Instance> instanceToCopy, Definition copyDef,HashMap<Node, Node> copyToNode) {
		Node copyNode;
		if(!nodeToCopy.containsKey(node)){
			copyNode=new Node();
			nodeToCopy.put(node, copyNode);
			copyToNode.put(copyNode,node);
			if(node.parentSupernode!=null){
				Node parent=node.parentSupernode;
				Node copyParent;
				if(!nodeToCopy.containsKey(parent)){
					copyParent = this.copyNodeMapping(parent,nodeToCopy, instanceToCopy, copyDef,copyToNode);
				}else{
					copyParent = nodeToCopy.get(parent);
				}
				for(int i=0;i<parent.childrenSubnodes.size();i++){
					if(!nodeToCopy.containsKey(parent.childrenSubnodes.get(i))){
						Node copyChildren = new Node();
						copyParent.addChildSubnode(copyChildren);
						nodeToCopy.put(parent.childrenSubnodes.get(i),copyChildren);
						copyToNode.put(copyChildren, parent.childrenSubnodes.get(i));
					}else{
						copyParent.addChildSubnode(nodeToCopy.get(parent.childrenSubnodes.get(i)));
					}
				}
			}
			if(!node.parentSubnodes.isEmpty()){
				for(int i=0;i<node.parentSubnodes.size();i++){
					if(!nodeToCopy.containsKey(node.parentSubnodes.get(i))){
						Node copyParent = this.copyNodeMapping(node.parentSubnodes.get(i), nodeToCopy, instanceToCopy, copyDef, copyToNode);
						copyParent.addChildSupernode(copyNode);
					}else{
						nodeToCopy.get(node.parentSubnodes.get(i)).addChildSupernode(copyNode);
					}
				}
			}
			if(node.outOfInstance!=null){
				this.copyInstanceMapping(node.outOfInstance,nodeToCopy,instanceToCopy,copyDef,copyToNode);
			}
		}else{
			copyNode=nodeToCopy.get(node);
		}
		return copyNode;
	}
	private void copyInstanceMapping(Instance instance,
			HashMap<Node, Node> nodeToCopy,
			HashMap<Instance, Instance> instanceToCopy, Definition copyDef,HashMap<Node, Node> copyToNode) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		for(int i=0;i<instance.in.size();i++){
			if(nodeToCopy.containsKey(instance.in.get(i))){
				nodes.add(nodeToCopy.get(instance.in.get(i)));
			}else{
				nodes.add(this.copyNodeMapping(instance.in.get(i), nodeToCopy, instanceToCopy, copyDef,copyToNode));
			}
		}
		for(int i=0;i<instance.out.size();i++){
			if(nodeToCopy.containsKey(instance.out.get(i))){
				nodes.add(nodeToCopy.get(instance.out.get(i)));
			}else{
				nodes.add(this.copyNodeMapping(instance.out.get(i), nodeToCopy, instanceToCopy, copyDef,copyToNode));
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
	public void expandInstancesMapping(Definition definition,
			HashMap<Node, Node> expandedToDefinition, ArrayList<Instance> expandedInstances, AddedNodes addedNodes, HashSet<Instance> removedInstances) {
		//FIXME: should probably be done from out nodes, in order to go top to bottom
		for(ArrayList<Instance> setOfInstances:this.instances){
			for(Instance instance : setOfInstances){
				if(instance.definition==definition){
					expandedInstances.add(instance);
				}
			}
		}
		for(Instance instance: expandedInstances){
			this.removeInstance(instance);
		}
		for(Instance instance : expandedInstances){
			this.expandInstanceInstancesMapping(instance,expandedToDefinition,definition,addedNodes, removedInstances);		
		}
	}
//	private void expandInstanceMapping(Instance instance,
//			HashMap<Node, Node> expandedToDefinition) {
//		this.removeInstance(instance);
//		this.expandInstanceInstancesMapping(instance,expandedToDefinition);
//		
//	}
	private void expandInstanceInstancesMapping(Instance instance,
			HashMap<Node, Node> expandedToDefinition, Definition definition, AddedNodes addedNodes, HashSet<Instance> removedInstances) {
		HashMap<Node,Node> definitionToInstanceNodes = new HashMap<Node,Node>();
		for (int i = 0; i < instance.in.size(); i++) {//map in nodes
			instance.in.get(i).expandBinodes();
			instance.in.get(i).childrenFission();
//			HashSet<Node> expandedNodes= new HashSet<Node>();
//			instance.in.get(i).childrenFission(expandedNodes);
			definitionToInstanceNodes.put(instance.definition.in.get(i), instance.in.get(i));
			expandedToDefinition.put(instance.in.get(i),instance.definition.in.get(i));
			mapSubnodeChildrenMapping(instance.in.get(i),instance.definition.in.get(i),definitionToInstanceNodes,expandedToDefinition);	
		}
		for (int i = 0; i < instance.out.size(); i++) {//map out nodes
			definitionToInstanceNodes.put(instance.definition.out.get(i), instance.out.get(i));
			expandedToDefinition.put(instance.out.get(i),instance.definition.out.get(i));
			instance.out.get(i).outOfInstance=null;
			mapParentsMapping(instance.out.get(i),instance.definition.out.get(i),definitionToInstanceNodes,expandedToDefinition);
		}
		for(ArrayList<Instance> setOfInstances:instance.definition.instances){
			for (Instance definitionInstance : setOfInstances) {
				ArrayList<Node> nodes = new ArrayList<Node>();
				for (Node node: definitionInstance.in) {//map in nodes
					if(definitionToInstanceNodes.containsKey(node)){
						nodes.add(definitionToInstanceNodes.get(node));
					}else{
						Node newNode = new Node();
						definitionToInstanceNodes.put(node, newNode);
						expandedToDefinition.put(newNode,node);
						nodes.add(newNode);
						mapParentsMapping(newNode,node,definitionToInstanceNodes,expandedToDefinition);
					}
				}
				for (Node node: definitionInstance.out) {//map out nodes
					if(definitionToInstanceNodes.containsKey(node)){
						nodes.add(definitionToInstanceNodes.get(node));
					}else{
						Node newNode = new Node();
						definitionToInstanceNodes.put(node, newNode);
						expandedToDefinition.put(newNode,node);
						nodes.add(newNode);
						mapSubnodeChildrenMapping(newNode,node,definitionToInstanceNodes,expandedToDefinition);
					}
				}
				if(definitionInstance.definition==definition){
					Instance thisInstance=this.add(this,nodes.toArray(new Node[nodes.size()]));
					this.removeRecursiveInstance(thisInstance,addedNodes,removedInstances);	
				}else{
					Instance thisInstance=this.add(definitionInstance.definition,nodes.toArray(new Node[nodes.size()]));
					if(!definitionInstance.definition.instancesOfRecursiveDefinitions.isEmpty()){
						this.removeInstance(instance);
						this.expandRecursiveInstance(instance, addedNodes, removedInstances);
					}
				}
			}
		}
//		this.replaceDefinition(definition,this);//replace occurrences of originalDefinition to this, for recursion consistency
//		this.removeRecursion(addedNodes, removedInstances);
	}
	private void mapSubnodeChildrenMapping(Node node, Node definitionNode, HashMap<Node, Node> definitionToInstanceNodes, HashMap<Node, Node> expandedToDefinition) {
		if(!definitionNode.childrenSubnodes.isEmpty()){
			node.splitChildrenSubnodes();
			definitionToInstanceNodes.put(definitionNode.childrenSubnodes.get(0),node.childrenSubnodes.get(0));	
			expandedToDefinition.put(node.childrenSubnodes.get(0),definitionNode.childrenSubnodes.get(0));
			definitionToInstanceNodes.put(definitionNode.childrenSubnodes.get(1),node.childrenSubnodes.get(1));	
			expandedToDefinition.put(node.childrenSubnodes.get(1),definitionNode.childrenSubnodes.get(1));
			definitionToInstanceNodes.put(definitionNode.childrenSubnodes.get(2),node.childrenSubnodes.get(2));	
			expandedToDefinition.put(node.childrenSubnodes.get(2),definitionNode.childrenSubnodes.get(2));
			mapSubnodeChildrenMapping(node.childrenSubnodes.get(1), definitionNode.childrenSubnodes.get(1),definitionToInstanceNodes, expandedToDefinition);
		}
	}
	private void mapParentsMapping(Node node, Node definitionNode, HashMap<Node, Node> definitionToInstanceNodes, HashMap<Node, Node> expandedToDefinition) {
		if(!definitionNode.parentSubnodes.isEmpty()){
			for(Node parent:definitionNode.parentSubnodes){
				if(definitionToInstanceNodes.containsKey(parent)){
					definitionToInstanceNodes.get(parent).addChildSupernode(node);
				}else{
					Node newParent= new Node();
					newParent.addChildSupernode(node);
					definitionToInstanceNodes.put(parent, newParent);
					expandedToDefinition.put(newParent, parent);
					mapParentsMapping(newParent,parent,definitionToInstanceNodes,expandedToDefinition);
				}
			}
		}else if(definitionNode.parentSupernode!=null){
			if(!definitionToInstanceNodes.containsKey(definitionNode.parentSupernode)){
				Node newParent= new Node();
				definitionToInstanceNodes.put(definitionNode.parentSupernode, newParent);
				expandedToDefinition.put(newParent, definitionNode.parentSupernode);
				mapParentsMapping(newParent,definitionNode.parentSupernode,definitionToInstanceNodes,expandedToDefinition);
			}
//			if(definitionToInstanceNodes.get(definitionNode.parentSupernode).childrenSubnodes.isEmpty()){
				for(Node childSubnode:definitionNode.parentSupernode.childrenSubnodes){
					if(definitionToInstanceNodes.containsKey(childSubnode)){
						definitionToInstanceNodes.get(definitionNode.parentSupernode).addChildSubnode(definitionToInstanceNodes.get(childSubnode));
					}else{
						Node newChildSubnode= new Node();
						definitionToInstanceNodes.get(definitionNode.parentSupernode).addChildSubnode(newChildSubnode);
						definitionToInstanceNodes.put(childSubnode, newChildSubnode);
						expandedToDefinition.put(newChildSubnode, childSubnode);
//						mapParentsMapping(newChildSubnode,childSubnode,definitionToInstanceNodes,expandedToDefinition);
					}
				}
//			}else{
//				for(Node childSubnode:definitionNode.parentSupernode.childrenSubnodes){
//					if(!definitionToInstanceNodes.containsKey(childSubnode)){
//						definitionToInstanceNodes.put(childSubnode, newChildSubnode);
//						expandedToDefinition.put(newChildSubnode, childSubnode);
//						mapParentsMapping(newChildSubnode,childSubnode,definitionToInstanceNodes,expandedToDefinition);
//					}
//				}
//			}
		}
	}
	public void addNandInstances(Node definitionNode, Node instanceNode,
			HashMap<Node, Node> definitionToInstanceNodes) {
		if(!definitionToInstanceNodes.containsKey(definitionNode)){
			definitionToInstanceNodes.put(definitionNode, instanceNode);
			if(definitionNode.outOfInstance!=null){//out of nand instance
				Node in0;
				Node in1;
				if(definitionToInstanceNodes.containsKey(definitionNode.outOfInstance.in.get(0))){
					in0=definitionToInstanceNodes.get(definitionNode.outOfInstance.in.get(0));
				}else{
					in0=new Node();
				}
				this.addNandInstances(definitionNode.outOfInstance.in.get(0), in0, definitionToInstanceNodes);
				if(definitionToInstanceNodes.containsKey(definitionNode.outOfInstance.in.get(1))){
					in1=definitionToInstanceNodes.get(definitionNode.outOfInstance.in.get(1));
				}else{
					in1=new Node();
				}
				this.addNandInstances(definitionNode.outOfInstance.in.get(1), in1, definitionToInstanceNodes);
				Node[] nodes ={in0,in1,instanceNode};
				this.add(definitionNode.outOfInstance.definition,nodes);
			}else{
				Node instanceNodeParent;
				if(definitionNode.parentSupernode!=null){
					if(definitionToInstanceNodes.containsKey(definitionNode.parentSupernode)){
						instanceNodeParent=definitionToInstanceNodes.get(definitionNode.parentSupernode);
					}else{
						instanceNodeParent= new Node();
					}
					for(int i=0;i<definitionNode.parentSupernode.childrenSubnodes.size();i++){
						Node instanceChildNode;
						if(definitionToInstanceNodes.containsKey(definitionNode.parentSupernode.childrenSubnodes.get(i))){//needed because instanceNode may be instanceChildNode 
							instanceChildNode=definitionToInstanceNodes.get(definitionNode.parentSupernode.childrenSubnodes.get(i));
						}else{
							instanceChildNode=new Node();
							definitionToInstanceNodes.put(definitionNode.parentSupernode.childrenSubnodes.get(i), instanceChildNode);
						}
						instanceNodeParent.addChildSubnode(instanceChildNode);
					}
					this.addNandInstances(definitionNode.parentSupernode, instanceNodeParent, definitionToInstanceNodes);
				}else{
					for(int i=0;i<definitionNode.parentSubnodes.size();i++){
						if(definitionToInstanceNodes.containsKey(definitionNode.parentSubnodes.get(i))){
							instanceNodeParent=definitionToInstanceNodes.get(definitionNode.outOfInstance.in.get(0));
						}else{
							instanceNodeParent=new Node();
						}
						instanceNodeParent.addChildSupernode(instanceNode);
						this.addNandInstances(definitionNode.parentSubnodes.get(i), instanceNodeParent, definitionToInstanceNodes);
					}
				}
			}
			
		}
	}
	public void chooseFromEquivalentNodes(
			HashMap<NandNode, HashSet<Node>> nandToNodes,
			HashMap<Node, Node> equivalentNode, HashSet<Node> nodeIO) {
		for(NandNode nandNode:nandToNodes.keySet()){
			HashSet<Node> nodes=nandToNodes.get(nandNode);
			if(nodes.size()>1){
				Node selectedNode=nodes.iterator().next();
				for(Node node: nodes){
					if(node.parentSupernode!=null){
						selectedNode=node;
					}
				}
				for(Node node: nodes){
					if(nodeIO.contains(node)){
						selectedNode=node;
					}
				}
				for(Node node: nodes){
					equivalentNode.put(node, selectedNode);
				}
			}
		}
	}
	public void clean(HashMap<Node, Node> equivalentNode) {
//		ArrayList<Instance> usedInstances = new ArrayList<Instance>();//can't be a hashSet since instances are modified
		ArrayList<Instance> instancesToRemove = new ArrayList<Instance>();
		for(ArrayList<Instance> setOfInstances:this.instances){
			instancesToRemove.addAll(setOfInstances);
		}
		HashSet<Node> expandedNodes = new HashSet<Node>();
		for(Node outNode:this.out){
			outNode.replaceNodes(expandedNodes, equivalentNode,instancesToRemove);
		}
		for(Instance instance:instancesToRemove){
			this.removeInstance(instance);
		}
	}
	public Instance add(Instance instance) {
		for(Node node:instance.in){
			this.add(node);	
		}
		for(Node node:instance.out){
			this.add(node);	
		}
		if(instance.definition==this){
			this.selfRecursiveInstances.add(instance);
		}else if(!instance.definition.selfRecursiveInstances.isEmpty()||!instance.definition.instancesOfRecursiveDefinitions.isEmpty()){
			this.instancesOfRecursiveDefinitions.add(instance);
		}
		for (Node outNode:instance.out) {//nºinst outs = nºdef outs
			outNode.outOfInstance=instance;
		}
		instance.depth=0;
		for(Node nodeIn:instance.in){
			int nodeDepth=nodeIn.getDepth(new HashSet<Node>());
			if(nodeDepth+1>instance.depth){
				instance.depth=nodeDepth+1;
			}
		}
		if(this.instances.size()<instance.depth+1){
			this.instances.add(new ArrayList<Instance>());
		}
		this.instances.get(instance.depth).add(instance); 
		return instance;
	}
}
