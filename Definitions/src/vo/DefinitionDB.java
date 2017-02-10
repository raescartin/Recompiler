/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
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
		
		if(definition.selfRecursiveInstances.isEmpty()&&definition.instancesContainingRecursion.isEmpty()){//definition has no recursion
			if(definition.name!="nand"){ //if definition is nand it's already optimized! (base case for recursion)
				HashMap <NandNode,Node> nandToNode = new HashMap <NandNode,Node>();
				HashMap <Node,Node> equivalentNodes = new HashMap <Node,Node>();
				HashMap<Node, NandNode> nodeToNand = new HashMap<Node, NandNode>();
//				HashSet<Node> nodeIO = new HashSet<Node>();
//				nodeIO.addAll(definition.in);
//				nodeIO.addAll(definition.out);
				definition.toNandInstances();
				definition.fission();//fission of nodes to minimum size needed, also removes redundant subnodes
//				definition.mapFission(nodeIO);
				NandForest nandForest = definition.toNandForest(nandToNode,nodeToNand,equivalentNodes);//non recursive definition to nandforest
//				definition.chooseFromEquivalentNodes(nandToNodes,equivalentNode,nodeIO);
				this.getEquivalentParentSupernodes(equivalentNodes);//TODO: do this in toNands for efficiency
				this.getEquivalentChildSupernodes(equivalentNodes);
				definition.replaceNodes(equivalentNodes);
				definition.fusion();
//				this.fromNandForest(definition,nandForest,nandToNode);//definition using only instances of nand
//				definition.fusion();//fusion of nodes 
				definition.update();
			}	
		}else{//definition has recursion
			this.optimizeRecursiveIntersection(definition);	
		}
		return definition;
	}
	private void optimizeRecursiveIntersection(Definition definition) {
		//POST: remove the operations that are repeated during recursion (recursive intersection)
		//		transforms definition to a new optimized definition containing a new recursive definition  (if needed)
		//!!!TO OPTIMIZE RECUSIVE INTERSECTION!!!
		//1 copy definition
		//2 expand recursive instances in copy
		//3 compare nodes in definition and copy, keep the nodes that are unchanged
		//4 create new definition of the recursive part without intersections (using the unchanged nodes as inputs/outputs)	
		//FIXME: map fusion of nodes
		HashMap<Node,Node> definitionToCopy = new HashMap<Node,Node>();
		HashMap<Node,Node> copyToDefinition = new HashMap<Node,Node>();
		HashMap<Node,Node> expandedToDefinition = new HashMap<Node,Node>();
		HashSet<Node> originalNodes = new HashSet<Node>();
		HashSet<Instance> removedInstances = new HashSet<Instance>();
		AddedNodes addedNodes = new AddedNodes();
		HashMap <NandNode,Node> nandToNode = new HashMap <NandNode,Node>();
		HashMap<Node, NandNode> nodeToNand = new HashMap<Node, NandNode>();
		HashMap <Node,Node> equivalentNodes = new HashMap <Node,Node>();
		ArrayList <Node> recursiveIn1 = new ArrayList <Node>(); 
		ArrayList <Node> recursiveOut1 = new ArrayList <Node>(); 
		ArrayList <Node> recursiveIn0 = new ArrayList <Node>(); 
		ArrayList <Node> recursiveOut0 = new ArrayList <Node>(); 
		ArrayList <Node> recursiveInInstance = new ArrayList <Node>(); 
		ArrayList <Node> recursiveOutInstance = new ArrayList <Node>();
		ArrayList<Node> nodes = new ArrayList<Node>();
		definition.toNandInstances();//expands all non recursive instances
		Definition expandedDefinition = definition.copyMapping(definitionToCopy,copyToDefinition);//freeze original for expansion
//		expandedDefinition.toNandInstances();//to nands before mapping so all posible subnodes are mapped
		expandedDefinition.mapNodes(originalNodes);
		ArrayList<Instance> expandedInstances = new ArrayList<Instance>();
 		expandedDefinition.expandInstancesMapping(definition,expandedToDefinition,expandedInstances,addedNodes, removedInstances);
		//expandedToDefinition includes both original copy to definition and expanded copy to definition nodes map
// 		expandedDefinition.toNandInstances();//to nands after expanding
		expandedDefinition.fission();//fission of nodes to minimum size needed, also removes redundant subnodes
		//TODO: optimize fision with expandedNodes
		NandForest expandingDefinitionNandForest = expandedDefinition.toNandForest(nandToNode,nodeToNand, equivalentNodes);//non recursive definition to nandforest
		if(!equivalentNodes.isEmpty()){
			this.getEquivalentParentSupernodes(equivalentNodes);//TODO: do this in toNands for efficiency
			this.getEquivalentChildSupernodes(equivalentNodes);
			this.replaceNodes(originalNodes,equivalentNodes);
			this.replaceNodes(definitionToCopy,copyToDefinition,expandedToDefinition,equivalentNodes);
			expandedDefinition.replaceNodes(equivalentNodes);
			expandedDefinition.fusion();
	//		expandedDefinition.mapNewOriginalNodes(originalNodes);//update originalNodes to keep track of new nodes derived from originalNodes
			expandedDefinition.recoverRecursion(addedNodes, removedInstances);
			this.extractIO(recursiveIn1,recursiveOut1,originalNodes,expandedDefinition,removedInstances);
			this.extractIOparentSupernodes(recursiveIn1,recursiveOut1);
			this.extractIOchildrenSupernodes(recursiveIn1,recursiveOut1,originalNodes,expandedDefinition,expandedInstances);
			Definition tempRecursiveDefinition = new Definition(recursiveIn1.size(),recursiveOut1.size(),definition.name+"Recur");
			nodes.clear();
			nodes.addAll(recursiveIn1);
			nodes.addAll(recursiveOut1);
			for(Node out:recursiveOut1){
				out.parentSubnodes.clear();
				out.outOfInstance=null;
			}
			
			expandedDefinition.add(tempRecursiveDefinition, nodes.toArray(new Node[nodes.size()]));
	//		expandedDefinition.update();
			for(Node node:recursiveIn1){
				if(expandedToDefinition.containsKey(node)){
					recursiveIn0.add(definitionToCopy.get(expandedToDefinition.get(node)));
				}else{
					recursiveIn0.add(node);
				}
			}
			for(Node node:recursiveOut1){
				if(expandedToDefinition.containsKey(node)){
					recursiveOut0.add(definitionToCopy.get(expandedToDefinition.get(node)));
				}else{
					recursiveOut0.add(node);
				}
			}
			tempRecursiveDefinition.in=recursiveIn0;
			tempRecursiveDefinition.out=recursiveOut0;
			Definition recursiveDefinition=tempRecursiveDefinition.copy();
			recursiveDefinition.replaceDefinition(tempRecursiveDefinition, recursiveDefinition);
			recursiveDefinition.name=definition.name+"Recur";
			recursiveDefinition.update();
			for(Node node:recursiveIn1){
				if(expandedToDefinition.containsKey(node)){
					recursiveInInstance.add(expandedToDefinition.get(node));
				}else{
					recursiveInInstance.add(copyToDefinition.get(node));
				}
			}
			for(Node node:recursiveOut1){
				if(expandedToDefinition.containsKey(node)){
					recursiveOutInstance.add(expandedToDefinition.get(node));
				}else{
					recursiveOutInstance.add(copyToDefinition.get(node));
				}
			}
			nodes.clear();
			nodes.addAll(recursiveInInstance);
			nodes.addAll(recursiveOutInstance);
			for(Node out:recursiveOutInstance){
				out.parentSubnodes.clear();
				out.outOfInstance=null;
			}
			definition.add(recursiveDefinition, nodes.toArray(new Node[nodes.size()]));
			definition.update();
	//		if(definition.name=="add"){
	//			this.put(recursiveDefinition.name, recursiveDefinition);
	//		}
			this.definitions.put(recursiveDefinition.name, recursiveDefinition);
//		}else{
//			this.recursionUnrolling(definition);//not much sense
		}
	}
//	public void recursionUnrolling(Definition definition) {
//		HashMap<Node,Node> definitionToCopy = new HashMap<Node,Node>();
//		HashMap<Node,Node> copyToDefinition = new HashMap<Node,Node>();
//		Definition definitionCopy = definition.copyMapping(definitionToCopy,copyToDefinition);//freeze original for expansion
//		
//		int thisDepth = definition.depth()+1;
//		float iterationDepth = thisDepth;
//		int i = 1;
//		boolean optimizes=false;
//		do{
//			HashSet<Instance> removedInstances = new HashSet<Instance>();
//			AddedNodes addedNodes = new AddedNodes();
//			HashMap<Node,Node> expandedToDefinition = new HashMap<Node,Node>();
//			ArrayList<Instance> expandedInstances = new ArrayList<Instance>();
//			i++;
//			definitionCopy.expandInstancesMapping(definition,expandedToDefinition,expandedInstances,addedNodes, removedInstances);
//			definitionCopy.recoverRecursion(addedNodes, removedInstances);
//			definitionCopy.replaceDefinition(definitionCopy, definition);
//			float thisIterationDepth=(float)(definitionCopy.depth()+1)/i;
//			if(thisIterationDepth<iterationDepth){
//				iterationDepth=thisIterationDepth;
//				optimizes=true;
//			}else{
//				optimizes=false;
//			}
//		}while(optimizes);
//		if(iterationDepth<thisDepth){
//			int x=0;
//		}
//	}
	private void remove(Definition definition) {
		this.definitions.remove(definition);
		
	}
	private void extractIOfromRecursiveInstances(HashSet<Instance> removedInstances, ArrayList<Node> recursiveIn, ArrayList<Node> recursiveOut, HashSet<Node> originalNodes) {
		for(Instance instance:removedInstances){
			for(Node nodeIn:instance.in){
				nodeIn.extractIn(recursiveIn, originalNodes);
//				if(originalNodes.contains(nodeIn)) recursiveIn.add(nodeIn);
			}
			for(Node nodeOut:instance.out){
				if(originalNodes.contains(nodeOut)) recursiveOut.add(nodeOut);
			}
		}
	}
	private void replaceNodes(HashMap<Node, Node> definitionToCopy,
			HashMap<Node, Node> copyToDefinition,
			HashMap<Node, Node> expandedToDefinition, HashMap<Node, Node> equivalentNodes) {
		
			HashSet<Node> nodesCopy = new HashSet<Node>();
			nodesCopy.addAll(copyToDefinition.keySet());
			for(Node node:nodesCopy){
				if(equivalentNodes.containsKey(node)){
					copyToDefinition.put(equivalentNodes.get(node), copyToDefinition.get(node));
					definitionToCopy.remove(copyToDefinition.get(node));
					definitionToCopy.put(copyToDefinition.get(node), equivalentNodes.get(node));
					copyToDefinition.remove(node);
				}
			}
			nodesCopy.clear();
			nodesCopy.addAll(expandedToDefinition.keySet());
			for(Node node:nodesCopy){
				if(equivalentNodes.containsKey(node)){
					expandedToDefinition.put(equivalentNodes.get(node), expandedToDefinition.get(node));
				}
			}
		
	}
	private void replaceNodes(HashSet<Node> originalNodes,
			HashMap<Node, Node> equivalentNodes) {
		HashSet<Node> nodesCopy = new HashSet<Node>();
		nodesCopy.addAll(originalNodes);
		for(Node node:nodesCopy){
			if(equivalentNodes.containsKey(node)){
				originalNodes.remove(node);
				originalNodes.add(equivalentNodes.get(node));
			}
		}
		
	}
	private void extractIOparentSupernodes(ArrayList<Node> recursiveIn,
			ArrayList<Node> recursiveOut) {
		Queue<Node> queueIn = new LinkedList<Node>();
		queueIn.addAll(recursiveIn);
		while (!queueIn.isEmpty()) {
			queueIn.peek().mergeParentSupernode(queueIn,recursiveIn);
		}
		Queue<Node> queueOut = new LinkedList<Node>();
		queueOut.addAll(recursiveOut);
		while (!queueOut.isEmpty()) {
			queueOut.peek().mergeParentSupernode(queueOut,recursiveOut);
		}
//		ArrayList<Node> nodes = new ArrayList<Node>();
//		boolean posibleSupernodes;
//		do{
//			posibleSupernodes=false;
//			nodes.addAll(recursiveIn);
//			for(Node node:nodes){
//				posibleSupernodes=posibleSupernodes||node.extractIOparentSupernodes(recursiveIn);
//			}
//		}while(posibleSupernodes);
//		do{
//			posibleSupernodes=false;
//			nodes.clear();
//			nodes.addAll(recursiveOut);
//			for(Node node:nodes){
//				node.extractIOparentSupernodes(recursiveOut);
//			}
//		}while(posibleSupernodes);
	}
	private void extractIOchildrenSupernodes(ArrayList<Node> recursiveIn, ArrayList<Node> recursiveOut, HashSet<Node> originalNodes, Definition expandedDefinition, ArrayList<Instance> expandedInstances) {

		Queue<Node> queueCandidatesIn = new LinkedList<Node>();
		queueCandidatesIn.addAll(recursiveIn);
		while (!queueCandidatesIn.isEmpty()) {
			queueCandidatesIn.peek().mergeChildSupernode(queueCandidatesIn,recursiveIn);
		}
//		this.mergeChildrenSupernodes(queueCandidatesIn);
		Queue<Node> queueCandidatesOut = new LinkedList<Node>();
		queueCandidatesOut.addAll(recursiveOut);
		while (!queueCandidatesOut.isEmpty()) {
			queueCandidatesOut.peek().mergeChildSupernode(queueCandidatesOut,recursiveOut);
		}
//		this.mergeChildrenSupernodes(queueCandidatesOut);
//		HashSet<Node> evaluatedNodes = new HashSet<Node>();
//		for(Node outNode:expandedDefinition.out){
//			outNode.extractIOchildrenSupernodes(evaluatedNodes,recursiveIn,recursiveOut,originalNodes);
//		}
//		for(Instance instance:expandedInstances){
//			for(Node inNode:instance.in){
//				inNode.extractIOchildrenSupernodes(evaluatedNodes, recursiveIn, recursiveOut, originalNodes);
//			}
//		}
		
	}
//	private void addOriginalRecursionIO(HashSet<Instance> removedInstances,
//			ArrayList<Node> recursiveIn, ArrayList<Node> recursiveOut) {
//		for(Instance instance:removedInstances){
//			instance.addOriginalRecursionIO(recursiveIn,recursiveOut);
//		}
//	}
//	private void extractNewRecursionIO(ArrayList<Node> recursiveIn,
//			ArrayList<Node> recursiveOut, HashSet<Node> originalNewNodes,
//			Definition expandedDefinition) {
//		for(SortedSet<Instance> instanceSet: expandedDefinition.instances){
//			for(Instance instance: instanceSet){
//				instance.extractNewRecursionIO(recursiveIn,recursiveOut,originalNewNodes,expandedDefinition);
//			}
//		}
//		
//	}
	private void extractIO(ArrayList<Node> recursiveIn,
			ArrayList<Node> recursiveOut, HashSet<Node> originalNodes,
			Definition expandedDefinition, HashSet<Instance> removedInstances) {
		for(Node outNode: expandedDefinition.out){
			outNode.extractOut(recursiveIn,recursiveOut,originalNodes);
		}
	}
	private Definition fromNandForest(Definition definition, NandForest nandForest, HashMap <NandNode,Node> nandToNode) {
		//set existing Definition from NandForest without NandNode's repetition	
		for (int i=0;i<nandForest.out.size();i++) {//node can be out node of definition OR a subnode from one
				this.recorverNandInstances(nandForest.out.get(i),definition,nandToNode);
		}
		return definition;
		
	}
	private void recorverNandInstances(NandNode nandNode, Definition def, HashMap<NandNode, Node> nandToNode) {
		if(nandNode.in1!=null&&nandNode.in2!=null){
			Node in1=nandToNode.get(nandNode.in1);
			recorverNandInstances(nandNode.in1,def,nandToNode);
			Node in2 = nandToNode.get(nandNode.in2);
			recorverNandInstances(nandNode.in2,def,nandToNode);
			Node out = nandToNode.get(nandNode);
			out.outOfInstance.in.set(0, in1);
			out.outOfInstance.in.set(1, in2);
		}
	}
	private void nodeOutFusion(ArrayList<Node> nodes, Set<Node> set) {
		int i=0;
		while(i<nodes.size()){
			this.outFusion(i,nodes,set);
			i++;
		}
		
	}
	private void outFusion(int i, ArrayList<Node> nodes, Set<Node> set) {
		//TODO: test
		Node node = nodes.get(i);
		if(!node.childSupernodes.isEmpty()){
			for(Node supernodeChildCandidate:set){
				if(!supernodeChildCandidate.parentSubnodes.isEmpty()&&nodes.containsAll(supernodeChildCandidate.parentSubnodes)){
					for(Node childrenSupernode:nodes.get(i).childSupernodes){
						if(childrenSupernode!=supernodeChildCandidate&&!childrenSupernode.parentSubnodes.isEmpty()&&childrenSupernode.parentSubnodes.containsAll(supernodeChildCandidate.parentSubnodes)){
							childrenSupernode.parentSubnodes.set(childrenSupernode.parentSubnodes.indexOf(nodes.get(i)), supernodeChildCandidate);
							childrenSupernode.parentSubnodes.removeAll(supernodeChildCandidate.parentSubnodes);
						}
					}
					nodes.set(i, supernodeChildCandidate);
					nodes.removeAll(supernodeChildCandidate.parentSubnodes);
				}
			}
		}
		
	}
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
//		i=0;
//		while(i<nodes.size()){
//			this.endfusion(i,nodes);
//			i++;
//		}
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
//		if(node.parentSupernode!=null){
//			if(node.parentnodes.contains(node.parentSupernode.first)&&nodes.contains(node.parentSupernode.restButFirst)){
//				Node childSupernode=null;
//				for(Node supernodeCandidate:set){
//					if(supernodeCandidate.parentSubnodes.size()==2){
//						if(supernodeCandidate.parentSubnodes.get(0)==node.parentSupernode.first&&supernodeCandidate.parentSubnodes.get(1)==node.parentSupernode.restButFirst){
//							childSupernode=supernodeCandidate;
//						}
//					}
//				}
//				node.parentSupernode.first.childSupernodes.add(childSupernode);
//				node.parentSupernode.restButFirst.childSupernodes.add(childSupernode);
//				nodes.set(i, childSupernode);
//				nodes.remove(node.parentSupernode.first);
//				nodes.remove(node.parentSupernode.restButFirst);
//			}else if(nodes.contains(node.parentSupernode.childSubnodes.get(1))&&nodes.contains(node.parentSupernode.childSubnodes.get(2))){
//				Node childSupernode=null;
//				for(Node supernodeCandidate:set){
//					if(supernodeCandidate.parentSubnodes.size()==2){
//						if(supernodeCandidate.parentSubnodes.get(0)==node.parentSupernode.childSubnodes.get(1)&&supernodeCandidate.parentSubnodes.get(1)==node.parentSupernode.childSubnodes.get(2)){
//							childSupernode=supernodeCandidate;
//						}
//					}
//				}
//				node.parentSupernode.childSubnodes.get(1).childSupernodes.add(childSupernode);
//				node.parentSupernode.childSubnodes.get(2).childSupernodes.add(childSupernode);
//				nodes.set(i, childSupernode);
//				nodes.removeAll(node.parentSupernode.childSubnodes);
//			}
//		}
//	}
//	private void trifusion(int i, ArrayList<Node> nodes) {
//		if(nodes.get(i).parentSupernode!=null&&nodes.contains(nodes.get(i).parentSupernode.childSubnodes.get(0))&&nodes.contains(nodes.get(i).parentSupernode.childSubnodes.get(1))&&nodes.contains(nodes.get(i).parentSupernode.childSubnodes.get(2))){
//			Node childLeft = nodes.get(i).parentSupernode.childSubnodes.get(0);
//			Node childMid = nodes.get(i).parentSupernode.childSubnodes.get(1);
//			Node childRight = nodes.get(i).parentSupernode.childSubnodes.get(2);
//			nodes.set(i, nodes.get(i).parentSupernode);
//			nodes.remove(childLeft);
//			nodes.remove(childMid);
//			nodes.remove(childRight);
//		}
//	}
//	
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
	private void getEquivalentChildSupernodes(HashMap<Node, Node> equivalentNodes) {
		Queue<Node> queue = new LinkedList<Node>();
		queue.addAll(equivalentNodes.keySet());
		while (!queue.isEmpty()) {
			queue.poll().getEquivalentChildSupernodes(equivalentNodes,queue);
		}
		
	}
	private void getEquivalentParentSupernodes(
			HashMap<Node, Node> equivalentNodes) {
		Queue<Node> queue = new LinkedList<Node>();
		queue.addAll(equivalentNodes.keySet());
		while (!queue.isEmpty()) {
			queue.poll().getEquivalentParentSupernode(equivalentNodes,queue);
		}
	}
}
