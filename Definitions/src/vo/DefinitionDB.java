/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import utils.AddedNodes;

//DESCRIPTION
//-Database of unique definitions, recursively defined by other contained definitions
//-nand is the base definition
//-indexed by name
//-serialized:
//	-save definitions to file
//	-load definitions from file
//-doesn't support indirect recursion (non recursive definition with instances that call the definition)
//because it doesn't make sense to define such a function (calling a definition not yet defined)
//
//TODO:
//Metrics: bitwise nands used
//
//IMPLEMENTATION
//- fix recursion optimization
//- verify toBest (most critical and complex algorithm)
//-FIXME(maybe): can't have definitions outside of DB (or else the references get messed up)
//-take into account optimized in out
//////////////////////////////////////////////////////
//HERE BE DRAGONS (maybe not in Java from here)
//////////////////////////////////////////////////////
//-take into account synonyms
public class DefinitionDB {
	HashMap<String,Definition> definitions;
	public DefinitionDB(Definition nand){
		//PRE:nand definition as an argument to have it referenced
		//POST:constructor for DefinitionDB, definitions database
		definitions = new HashMap<String,Definition>();
		this.definitions.put("nand",nand);//nand definition needed as building block for the other definitions
	}
	public void put(String name, Definition definition){
		//POST:optimize, then add definition to database
		definition.clearRoot();
		this.optimize(definition);
		if(!definition.selfRecursiveInstances.isEmpty()){
			this.optimizeRecursiveIntersection(definition);		
		}
//		this.toHighestLevel(definition);//definition made of instances of nand definiition, to highest level possible
		definition.getRoot();
		this.definitions.put(name, definition);//insert optimized definition in database
		//TODO: Optimize all definitions where this new definition could be used
		//TODO: verify that order of adding definitions is irrelevant example: and then not-> not used in and
	}
	public Definition get(String name){
		//POST:find and return definition by name
		return this.definitions.get(name);
	}
	private Definition optimize(Definition definition) {
		//PRE: definition's instances are optimized 
		//POST: definition is optimized, using only instances of nand definition and instances of previously optimized  recursive definitions
		
		//definition may be recursive
		//optimize() maps recursions, transforms to non-recursive definition
		//add recursive outputs to in and recursive inputs as outs,
		//transform definition to NandForest,
		//return previous recursions 
		//remove added ins and outs from recursions
		//if recursive then optimize sub-definitions AND MERGE
		//if not recursive then fromNand to def -> to best
		//definition->nandtree node fission
		//with subnodes and recursion
		//TODO:
			//intersection optimization of recursive definitions
			//intersection optimization of definition with contained recursive definitions
			//intersection optimization between contained recursive definitions
		
		if(definition.selfRecursiveInstances.isEmpty()&&definition.instancesOfRecursiveDefinitions.isEmpty()){//definition has no recursion
			if(definition.name!="nand"){ //if definition is nand it's already optimized! (base case for recursion)
				HashMap <NandNode,Node> nandToNode = new HashMap <NandNode,Node>();
				HashMap<Node, NandNode> nodeToNand = new HashMap<Node, NandNode>();
				definition.toNandInstances();
				definition.nodeFission();//fission of nodes to minimum size needed, also removes redundant subnodes
				NandForest nandForest = definition.toNandForest(nandToNode,nodeToNand);//non recursive definition to nandforest
				this.fromNandForest(definition,nandForest,nandToNode);//definition using only instances of nand
//				definition.update();
				definition.fusion();//fusion of nodes 
			}	
		}else{//definition has recursion
			//Optimize the non recursive part of definition	
			AddedNodes addedNodes = new AddedNodes();
			HashSet<Instance> removedInstances = new HashSet<Instance>();
			definition.removeRecursion(addedNodes, removedInstances);
			this.optimize(definition);
			definition.recoverRecursion(addedNodes, removedInstances);//recover recursion
			
		}
		return definition;
	}
	private void optimizeRecursiveIntersection(Definition definition) {
		//PRE: recursion is optimized except self recursive intersection
		//POST: remove the operations that are repeated during recursion (recursive intersection)
		//		transforms definition to a new optimized definition containing a new recursive definition  (if needed)
		//!!!TO OPTIMIZE RECUSIVE INTERSECTION!!!
		//1 copy definition
		//2 expand recursive instances in copy
		//3 compare nodes in definition and copy, keep the nodes that are unchanged
		//4 create new definition of the recursive part without intersections (using the unchanged nodes as inputs/outputs)	
		//FIXME: map fusion of nodes
		HashMap<Node,Node> expandedToDefinition = new HashMap<Node,Node>();
		//expandedToDefinition includes both original copy to definition and expanded copy to definition nodes map
		HashMap<Node,Node> definitionToExpanded = new HashMap<Node,Node>();
		ArrayList <Node> recursiveIn1 = new ArrayList <Node>(); 
		ArrayList <Node> recursiveOut1 = new ArrayList <Node>(); 
		ArrayList <Node> recursiveIn0 = new ArrayList <Node>(); 
		ArrayList <Node> recursiveOut0 = new ArrayList <Node>(); 
		ArrayList <Node> recursiveInInstance = new ArrayList <Node>(); 
		ArrayList <Node> recursiveOutInstance = new ArrayList <Node>();
		HashMap<Node, NandNode> nodeToNand = new HashMap<Node, NandNode>();
		HashMap <NandNode,Node> nandToNode = new HashMap <NandNode,Node>();
		HashMap <NandNode,Node> nandToNewNode = new HashMap <NandNode,Node>();//not needed
		HashMap<Node, NandNode> newNodeToNand = new HashMap<Node, NandNode>();//not needed
		HashMap<Node,Node> nodes1to0 = new HashMap<Node,Node>();
		HashSet<Node> originalNodes = new HashSet<Node>();
		HashSet<Node> originalCopyNodes = new HashSet<Node>();
		ArrayList <Node> nandToNodeIn = new ArrayList <Node>(); //map of input nandnodes to nodes
		ArrayList <Node> nandToNodeOut = new ArrayList <Node>(); //map of output nandnodes to nodes
		HashSet<Instance> removedInstances = new HashSet<Instance>();
		AddedNodes addedNodes = new AddedNodes();
		ArrayList<NandNode> recursionInNandNodes = new ArrayList<NandNode>();
		ArrayList<NandNode> recursionOutNandNodes = new ArrayList<NandNode>();
		Definition expandedDefinition = definition.copyMapping(definitionToExpanded,expandedToDefinition);//freeze original for expansion
		expandedDefinition.mapNodes(originalNodes);
		expandedDefinition.expandInstancesMapping(definition,expandedToDefinition);

		//to nand
		expandedDefinition.removeRecursion(addedNodes, removedInstances);
		expandedDefinition.nodeFission();//fission of nodes to minimum size needed, also removes redundant subnodes
//		expandedDefinition.mapFission(originalNodes);//update originalNodes to keep track of fissed nodes
		NandForest expandingDefinitionNandForest = expandedDefinition.toNandForest(nandToNode,nodeToNand);//non recursive definition to nandforest
		this.fromNandForest(expandedDefinition, expandingDefinitionNandForest, nandToNode);
		for(Node originalNode:originalNodes){
			if(nodeToNand.containsKey(originalNode)){
				originalCopyNodes.add(nandToNewNode.get(nodeToNand.get(originalNode)));
			}
		}
		this.extractNewRecursionIO(recursiveIn1,recursiveOut1,originalCopyNodes,expandedDefinition);
		this.addOriginalRecursionIO(removedInstances,recursiveIn1,recursiveOut1);
		expandedDefinition.recoverRecursion(addedNodes, removedInstances);
//		this.nodeInFusion(recursiveIn1,expandedToDefinition.keySet());
//		this.nodeOutFusion(recursiveOut1,expandedToDefinition.keySet());
		expandedDefinition.update();//needed?
		for(Node node:expandedToDefinition.keySet()){
			nodes1to0.put(nandToNewNode.get(nodeToNand.get(node)), expandedToDefinition.get(nandToNewNode.get(nodeToNand.get(node))));//Fusion Mapping needed
		}
		for(Node node:recursiveIn1){
			recursiveIn0.add(nodes1to0.get(node));
		}
		for(Node node:recursiveOut1){
			recursiveOut0.add(nodes1to0.get(node));
		}
		ArrayList<Node> nodes = new ArrayList<Node>();
		//-apply to original definition the new outs (instance of newRecursiveDefinition) mapping outs from newRecursiveDefinition
		//-apply to newRecursiveDefinition the new ins mapping form original definition
		Definition tempRecursiveDefinition= new Definition(recursiveIn1.size(),recursiveOut1.size(),definition.name+"Recur");
		nodes.clear();
		nodes.addAll(recursiveIn1);
		nodes.addAll(recursiveOut1);
		for(Node out:recursiveOut1){
			out.parentSubnodes.clear();
			out.outOfInstance=null;
		}
		expandedDefinition.add(tempRecursiveDefinition, nodes.toArray(new Node[nodes.size()]));
		expandedDefinition.update();//needed?
		expandedDefinition.in=recursiveIn0;
		expandedDefinition.out=recursiveOut0;
		expandedDefinition.name=definition.name+"Recur";
		expandedDefinition.replaceDefinition(tempRecursiveDefinition, expandedDefinition);
		expandedDefinition.update();
		this.definitions.put(expandedDefinition.name, expandedDefinition);
		
		for(Node nodeIn:recursiveIn0){
			recursiveInInstance.add(nandToNode.get(newNodeToNand.get(nodeIn)));
		}
		for(Node nodeOut:recursiveOut0){
			recursiveOutInstance.add(nandToNode.get(newNodeToNand.get(nodeOut)));
		}
		nodes.clear();
		nodes.addAll(recursiveInInstance);
		nodes.addAll(recursiveOutInstance);
		for(Node out:recursiveOutInstance){
			out.parentSubnodes.clear();
			out.outOfInstance=null;
		}
		definition.add(expandedDefinition, nodes.toArray(new Node[nodes.size()]));
		definition.update();
	}
	private void addOriginalRecursionIO(HashSet<Instance> removedInstances,
			ArrayList<Node> recursiveIn, ArrayList<Node> recursiveOut) {
		for(Instance instance:removedInstances){
			instance.addOriginalRecursionIO(recursiveIn,recursiveOut);
		}
	}
	private void extractNewRecursionIO(ArrayList<Node> recursiveIn,
			ArrayList<Node> recursiveOut, HashSet<Node> originalNewNodes,
			Definition expandedDefinition) {
		for(SortedSet<Instance> instanceSet: expandedDefinition.instances){
			for(Instance instance: instanceSet){
				instance.extractNewRecursionIO(recursiveIn,recursiveOut,originalNewNodes,expandedDefinition);
			}
		}
		
	}
	private Definition fromNandForest(Definition definition, NandForest nandForest, HashMap <NandNode,Node> nandToNode) {
		//set existing Definition from NandForest without NandNode's repetition	
		definition.clearInstances();
		for (int i=0;i<nandForest.out.size();i++) {//node can be out node of definition OR a subnode from one
				this.addNandsMapping(nandForest.out.get(i),definition,nandToNode);
		}
		return definition;
		
	}
	private void addNandsMapping(NandNode nandNode, Definition def, HashMap<NandNode, Node> nandToNode) {
		Node in1;
		Node in2;
		if(nandNode.in1!=null&&nandNode.in2!=null){
			in1=nandToNode.get(nandNode.in1);
			addNandsMapping(nandNode.in1,def,nandToNode);
			in2 = nandToNode.get(nandNode.in2);
			addNandsMapping(nandNode.in2,def,nandToNode);
			Node[] nodes = new Node[]{in1,in2,nandToNode.get(nandNode)};
			def.add(this.get("nand"),nodes);
		}
	}
//	private void nodeOutFusion(ArrayList<Node> nodes, Set<Node> set) {
//		int i=0;
//		while(i<nodes.size()){
//			this.outFusion(i,nodes,set);
//			i++;
//		}
//		
//	}
//	private void outFusion(int i, ArrayList<Node> nodes, Set<Node> set) {
//		//TODO: test
//		Node node = nodes.get(i);
//		if(!node.childrenSupernodes.isEmpty()){
//			for(Node supernodeChildCandidate:set){
//				if(!supernodeChildCandidate.parents.isEmpty()&&nodes.containsAll(supernodeChildCandidate.parents)){
//					for(Node childrenSupernode:nodes.get(i).childrenSupernodes){
//						if(childrenSupernode!=supernodeChildCandidate&&!childrenSupernode.parents.isEmpty()&&childrenSupernode.parents.containsAll(supernodeChildCandidate.parents)){
//							childrenSupernode.parents.set(childrenSupernode.parents.indexOf(nodes.get(i)), supernodeChildCandidate);
//							childrenSupernode.parents.removeAll(supernodeChildCandidate.parents);
//						}
//					}
//					nodes.set(i, supernodeChildCandidate);
//					nodes.removeAll(supernodeChildCandidate.parents);
//				}
//			}
//		}
//		
//	}
//	private void nodeInFusion(ArrayList<Node> nodes, Set<Node> set) {
//		int i=0;
//		while(i<nodes.size()){
//			this.trifusion(i,nodes);
//			i++;
//		}
//		i=0;
//		while(i<nodes.size()){
//			this.bifusion(i,nodes,set);
//			i++;
//		}
////		i=0;
////		while(i<nodes.size()){
////			this.endfusion(i,nodes);
////			i++;
////		}
//	}
//	private void endfusion(int i, ArrayList<Node> nodes) {
//		if(nodes.get(i).parents.size()==2&&nodes.get(i).parents.get(0).parents.size()==1&&nodes.get(i).parents.get(1).parents.size()==1&&nodes.get(i).parents.get(0).parents.get(0)==nodes.get(i).parents.get(1).parents.get(0)
//				&&nodes.get(i).parents.get(0).parents.get(0).parents.size()==1&&nodes.get(i).parents.get(1).parents.get(0).parents.size()==1&&nodes.get(i).parents.get(0).parents.get(0).parents.get(0)==nodes.get(i).parents.get(1).parents.get(0).parents.get(0)){
//			if(nodes.contains(nodes.get(i).parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(0))){
//				Node supernode = new Node();
//				nodes.get(i).parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(0).add(supernode);
//				nodes.get(i).parents.get(0).childrenSubnodes.clear();
//				nodes.get(i).parents.get(0).add(supernode);
//				nodes.get(i).parents.get(1).childrenSubnodes.clear();
//				nodes.get(i).parents.get(1).add(supernode);
//				nodes.remove(nodes.get(i).parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(0));
//				nodes.set(i, supernode);
//			}else if(nodes.contains(nodes.get(i).parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(2))){
//				Node supernode = new Node();
//				nodes.get(i).parents.get(0).childrenSubnodes.clear();
//				nodes.get(i).parents.get(0).add(supernode);
//				nodes.get(i).parents.get(1).childrenSubnodes.clear();
//				nodes.get(i).parents.get(1).add(supernode);
//				supernode.add(nodes.get(i).parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(2));
//				nodes.remove(nodes.get(i).parents.get(0).parents.get(0).parents.get(0).childrenSubnodes.get(2));
//				nodes.set(i, supernode);
//			}
//		}
//	}
//	private void bifusion(int i, ArrayList<Node> nodes, Set<Node> set) {
//		//if there's two consecutive children of a node on the array of nodes, fuse them
//		Node node = nodes.get(i);
//		if(node.parents.size()==1){
//			Node parent = node.parents.get(0);
//			if(nodes.contains(parent.childrenSubnodes.get(0))&&nodes.contains(parent.childrenSubnodes.get(1))){
//				Node childSupernode=null;
//				for(Node supernodeCandidate:set){
//					if(supernodeCandidate.parents.size()==2){
//						if(supernodeCandidate.parents.get(0)==parent.childrenSubnodes.get(0)&&supernodeCandidate.parents.get(1)==parent.childrenSubnodes.get(1)){
//							childSupernode=supernodeCandidate;
//						}
//					}
//				}
//				parent.childrenSubnodes.get(0).childrenSupernodes.add(childSupernode);
//				parent.childrenSubnodes.get(1).childrenSupernodes.add(childSupernode);
//				nodes.set(i, childSupernode);
//				nodes.removeAll(parent.childrenSubnodes);
//			}else if(nodes.contains(parent.childrenSubnodes.get(1))&&nodes.contains(parent.childrenSubnodes.get(2))){
//				Node childSupernode=null;
//				for(Node supernodeCandidate:set){
//					if(supernodeCandidate.parents.size()==2){
//						if(supernodeCandidate.parents.get(0)==parent.childrenSubnodes.get(1)&&supernodeCandidate.parents.get(1)==parent.childrenSubnodes.get(2)){
//							childSupernode=supernodeCandidate;
//						}
//					}
//				}
//				parent.childrenSubnodes.get(1).childrenSupernodes.add(childSupernode);
//				parent.childrenSubnodes.get(2).childrenSupernodes.add(childSupernode);
//				nodes.set(i, childSupernode);
//				nodes.removeAll(parent.childrenSubnodes);
//			}
//		}
//	}
//	private void trifusion(int i, ArrayList<Node> nodes) {
//		if(nodes.get(i).parents.size()==1&&nodes.contains(nodes.get(i).parents.get(0).childrenSubnodes.get(0))&&nodes.contains(nodes.get(i).parents.get(0).childrenSubnodes.get(1))&&nodes.contains(nodes.get(i).parents.get(0).childrenSubnodes.get(2))){
//			Node node=nodes.get(i);
//			Node parent=node.parents.get(0);
//			Node childLeft = parent.childrenSubnodes.get(0);
//			Node childMid = parent.childrenSubnodes.get(1);
//			Node childRight = parent.childrenSubnodes.get(2);
//			nodes.set(i, parent);
//			nodes.remove(childLeft);
//			nodes.remove(childMid);
//			nodes.remove(childRight);
//		}
//	}
	
//	private void addRecursionNodesToNandIO(
//			HashSet<NandNode> originalNandNodes,
//			ArrayList<NandNode> recursionInNandNodes,
//			ArrayList<NandNode> recursionOutNandNodes,
//			ArrayList<NandNode> newRecursiveDefinitionNandIn,
//			ArrayList<NandNode> newRecursiveDefinitionNandOut) {
//		for(NandNode nandNode:recursionInNandNodes){
//			if(originalNandNodes.contains(nandNode)){
//				if(!newRecursiveDefinitionNandIn.contains(nandNode)){
//					newRecursiveDefinitionNandIn.add(nandNode);
//				}
//			}
//		}
//		for(NandNode nandNode:recursionOutNandNodes){
//			if(originalNandNodes.contains(nandNode)){
//				if(!newRecursiveDefinitionNandOut.contains(nandNode)){
//					newRecursiveDefinitionNandOut.add(nandNode);
//				}
//			}
//		}
//		
//	}

//	private Definition fromNandForest(Definition definition,
//			NandForest nandForest,
//			ArrayList<Node> nandToNodeIn, ArrayList<Node> nandToNodeOut, HashMap <NandNode,Node> nandToNode) {
//		//set existing Definition from NandForest without NandNode's repetition	
//				definition.clearInstances();
//				for (int i=0;i<nandToNodeIn.size();i++) {//we map only inputs because expanding is bottom to top
//					nandToNode.put(nandForest.in.get(i),nandToNodeIn.get(i));
//				}
//				for (int i=0;i<nandToNodeOut.size();i++) {//node can be out node of definition OR a subnode from one
//						nandToNodeOut.get(i).outOfInstance=null;
//						this.addNands(nandToNodeOut.get(i),nandForest.out.get(i),definition,nandForest,nandToNode);
//				}
//				return definition;
//	}
	public void addNands(Node outNode,NandNode nandNode, Definition def,
			NandForest nandForest, HashMap<NandNode, Node> nandToNode) {
		Node in1;
		Node in2;
		if(nandNode.in1!=null&&nandNode.in2!=null){
			if(!nandToNode.containsKey(nandNode.in1)){
				in1= new Node();
				addNands(in1,nandNode.in1,def,nandForest,nandToNode);
			}else{
				in1=nandToNode.get(nandNode.in1);
			}
			if(!nandToNode.containsKey(nandNode.in2)){
				in2= new Node();
				addNands(in2,nandNode.in2,def,nandForest,nandToNode);
			}else{
				in2 = nandToNode.get(nandNode.in2);
			}
			Node[] nodes = new Node[]{in1,in2,outNode};
			def.add(this.get("nand"),nodes);
			nandToNode.put(nandNode,outNode);
		}
	}
//	public void toHighestLevel(Definition definition) {
//		//PRE: definition may be recursive
//		//POST: apply definitions from definitionDB to transform the definition to the highest level possible
//		//TODO:prioritize intersection elimination, if more practical needed add one more stage
//		HashSet<Node> supernodeOuts= new HashSet<Node>();
//		//Use A* type algorithm to locate highest level definitions
//		//applying all definitions with same root
//		definition.mapSupernodeOuts(supernodeOuts);
//		int instanceIndex;
//		int rootIndex;
//		boolean appliedOnce;//at least one definition has been applied
//		Instance instance;
//		Definition appliedDefinition;
//		do{
//			appliedOnce=false;
//			instanceIndex=0;
//			while (instanceIndex<definition.instances.size()) {//one level up, while needed instead of for, since list can be modified on loop
//				instance=definition.instances.get(instanceIndex);
//				rootIndex=0;
//				boolean applied=false;
//				while (rootIndex<instance.definition.rootIn.size()&&applied==false) {//loop while not modified (if one rootIn used, rest worthless)
//					appliedDefinition=instance.definition.rootIn.get(rootIndex);
//					if(definition!=appliedDefinition){//prevent applying definition to self
//						applied=definition.apply(instance,appliedDefinition,supernodeOuts);
//						if (applied) {
//							instanceIndex=0;//restart index
//						}
//						appliedOnce=appliedOnce||applied;
//					}
//					rootIndex++;
//				}	
//				instanceIndex++;
//			}
//		}while(appliedOnce);//while at least one definition applied through all instances
//	}
	@Override
	public String toString(){
		String string = new String("DATABASE:\n");
		//create reverse dictionary (map) of definition id's to names
		//use said dictionary to print the names
		for (String name : this.definitions.keySet()) {
			string+=this.definitions.get(name).toString();
		}
		return string;
	}
}
