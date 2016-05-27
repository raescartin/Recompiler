/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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
				ArrayList <Node> nandToNodeIn = new ArrayList <Node>(); //map of input nandnodes to nodes
				ArrayList <Node> nandToNodeOut = new ArrayList <Node>(); //map of output nandnodes to nodes
				definition.toNandDefinitions();
				definition.flattenParents();
				definition.nodeFission();//fission of nodes to minimum size needed, also removes redundant subnodes
				NandForest nandForest = definition.toNandForest(nandToNodeIn,nandToNodeOut);//non recursive definition to nandforest
				HashMap <NandNode,Node> nandToNode = new HashMap <NandNode,Node>();
				this.fromNandForest(definition,nandForest,nandToNodeIn,nandToNodeOut, nandToNode);//definition using only instances of nand
				definition.update();
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
		HashMap<Node,Node> definitionToExpanded = new HashMap<Node,Node>();
		HashMap<Node,Node> expandedToSelf = new HashMap<Node,Node>();
		ArrayList <Node> recursiveIn = new ArrayList <Node>(); 
		ArrayList <Node> recursiveOut = new ArrayList <Node>(); 
		HashMap<Node, NandNode> nodeToNand = new HashMap<Node, NandNode>();
		HashMap <NandNode,Node> nandToNode = new HashMap <NandNode,Node>();
		HashSet<Node> originalNodes = new HashSet<Node>();
		HashSet<NandNode> originalNandNodes = new HashSet<NandNode>();
		ArrayList <Node> nandToNodeIn = new ArrayList <Node>(); //map of input nandnodes to nodes
		ArrayList <Node> nandToNodeOut = new ArrayList <Node>(); //map of output nandnodes to nodes
		HashSet<Instance> removedInstances = new HashSet<Instance>();
		AddedNodes addedNodes = new AddedNodes();
		HashMap<Node,Node> expandedToDefinition = new HashMap<Node,Node>();
		ArrayList<NandNode> recursionInNandNodes = new ArrayList<NandNode>();
		ArrayList<NandNode> recursionOutNandNodes = new ArrayList<NandNode>();
		ArrayList<NandNode> newRecursiveDefinitionNandIn = new ArrayList<NandNode>();
		ArrayList<NandNode> newRecursiveDefinitionNandOut = new ArrayList<NandNode>();
		Definition expandedDefinition = definition.copyMapping(definitionToExpanded);//freeze original for expansion
		expandedDefinition.mapNodes(originalNodes);
		expandedDefinition.expandInstancesMapping(definition,expandedToDefinition);
		for(Node node:expandedToDefinition.keySet()){
			Node definitionNode = expandedToDefinition.get(node);
			Node selfNode= definitionToExpanded.get(definitionNode);
			expandedToSelf.put(node, selfNode);
		}
		expandedDefinition.mapFission(originalNodes);//update originalNodes to keep track of fissed nodes
		//to nand
		expandedDefinition.removeRecursion(addedNodes, removedInstances);
		expandedDefinition.nodeFissionMapping(originalNodes);//fission of nodes to minimum size needed, also removes redundant subnodes
		NandForest expandingDefinitionNandForest = expandedDefinition.toNandForestMapping(nandToNodeIn,nandToNodeOut,nodeToNand,addedNodes,recursionInNandNodes,recursionOutNandNodes);//non recursive definition to nandforest
		for(Node node:originalNodes){
			if(nodeToNand.containsKey(node)){
				originalNandNodes.add(nodeToNand.get(node));
			}
		}
		this.extractNewRecursionNandIO(expandingDefinitionNandForest,originalNandNodes,newRecursiveDefinitionNandIn,newRecursiveDefinitionNandOut);
		this.addRecursionNodesToNandIO(originalNandNodes,recursionInNandNodes,recursionOutNandNodes,newRecursiveDefinitionNandIn,newRecursiveDefinitionNandOut);	
		this.fromNandForest(expandedDefinition, expandingDefinitionNandForest, nandToNodeIn, nandToNodeOut,nandToNode);
		for(NandNode nandNode:newRecursiveDefinitionNandIn){
			recursiveIn.add(nandToNode.get(nandNode));
		}
		for(NandNode nandNode:newRecursiveDefinitionNandOut){
			recursiveOut.add(nandToNode.get(nandNode));
		}
		HashSet<Node> newKnownNodes = new HashSet<Node>();
		for(NandNode nandNode:originalNandNodes){
			newKnownNodes.add(nandToNode.get(nandNode));
		}
		expandedDefinition.recoverRecursion(addedNodes, removedInstances);
		expandedDefinition.update();//can update since it doesn't break references (to hashsets)
		expandedDefinition.replaceDefinition(expandedDefinition, definition);
		this.nodeInFusion(recursiveIn);
		this.nodeOutFusion(recursiveOut);
		//TODO: 
		//-apply to original definition the new outs (instance of newRecursiveDefinition) mapping outs from newRecursiveDefinition
		//-apply to newRecursiveDefinition the new ins mapping form original definition
//		Definition tempRecursiveDefinition= new Definition(recursiveIn.size(),recursiveOut.size(),expandedDefinition.name+"Recur");
//		ArrayList<Node> nodes = new ArrayList<Node>();
//		nodes.addAll(recursiveIn);
//		nodes.addAll(recursiveOut);
//		for(Node outNode:recursiveOut2){
//			outNode.parents.clear();
//			outNode.outOfInstance=null;
//		}
//		expandedDefinition.add(tempRecursiveDefinition, nodes.toArray(new Node[nodes.size()]));
//		expandedDefinition.update();
//		this.nodeFusion(recursiveIn1);
//		this.nodeFusion(recursiveOut1);
//		tempRecursiveDefinition.in=recursiveIn1;
//		tempRecursiveDefinition.out=recursiveOut1;
//		Definition newRecursiveDefinition=tempRecursiveDefinition.copy();
//		newRecursiveDefinition.replaceDefinition(tempRecursiveDefinition, newRecursiveDefinition);
//		expandedDefinition.replaceDefinition(tempRecursiveDefinition, newRecursiveDefinition);
//		nodes.clear();
//		nodes.addAll(recursiveIn1);
//		nodes.addAll(recursiveOut1);
//		for(Node outNode:recursiveOut1){
//			outNode.parents.clear();
//			outNode.outOfInstance=null;
//		}
//		expandedDefinition.add(newRecursiveDefinition, nodes.toArray(new Node[nodes.size()]));
//		expandedDefinition.update();
		definition.in=expandedDefinition.in;
		definition.out=expandedDefinition.out;
		definition.update();
	}
	private void nodeOutFusion(ArrayList<Node> nodes) {
		int i=0;
		while(i<nodes.size()){
			this.outFusion(i,nodes);
			i++;
		}
		
	}
	private void outFusion(int i, ArrayList<Node> nodes) {
		Node node = nodes.get(i);
		if(!node.childrenSupernodes.isEmpty()){
			for(Node supernodeChild :node.childrenSubnodes){
				if(nodes.containsAll(supernodeChild.parents)){
					nodes.set(i, supernodeChild);
					nodes.removeAll(supernodeChild.parents);
				}
			}
		}
		
	}
	private void nodeInFusion(ArrayList<Node> nodes) {
		int i=0;
		while(i<nodes.size()){
			this.trifusion(i,nodes);
			i++;
		}
		i=0;
		while(i<nodes.size()){
			this.bifusion(i,nodes);
			i++;
		}
//		i=0;
//		while(i<nodes.size()){
//			this.endfusion(i,nodes);
//			i++;
//		}
	}
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
	private void bifusion(int i, ArrayList<Node> nodes) {
		//if there's two consecutive children of a node on the array of nodes, fuse them
		Node node = nodes.get(i);
		if(node.parents.size()==1){
			Node parent = node.parents.get(0);
			if(nodes.contains(parent.childrenSubnodes.get(0))&&nodes.contains(parent.childrenSubnodes.get(1))){
				Node childSupernode = new Node();
//				if(!parent.childrenSubnodes.get(1).childrenSubnodes.isEmpty()){
//					childSupernode.splitChildrenSubnodes();
//					Node newSupernode = new Node();
//					parent.childrenSubnodes.get(1).children.get(0).parents.clear();
//					parent.childrenSubnodes.get(1).children.get(1).parents.clear();
//					parent.childrenSubnodes.get(1).children.get(2).parents.clear();
//					newSupernode.add(parent.childrenSubnodes.get(1).children.get(0));
//					newSupernode.add(parent.childrenSubnodes.get(1).children.get(1));
//					newSupernode.add(parent.childrenSubnodes.get(1).children.get(2));
//					parent.childrenSubnodes.get(1).children.clear();
//					childSupernode.childrenSubnodes.get(1).add(newSupernode);
//					childSupernode.childrenSubnodes.get(2).add(newSupernode);
//				}
				parent.childrenSubnodes.get(0).addChildSupernode(childSupernode);
				parent.childrenSubnodes.get(1).addChildSupernode(childSupernode);
				nodes.set(i, childSupernode);
				nodes.removeAll(parent.childrenSubnodes);
			}else if(nodes.contains(parent.childrenSubnodes.get(1))&&nodes.contains(parent.childrenSubnodes.get(2))){
				Node childSupernode = new Node();
//				if(parent.childrenSubnodes.get(1).children.size()==3&&parent.childrenSubnodes.get(1).children.get(0).parents.size()==1){
//					supernode.splitChildrenSubnodes();
//					Node newSupernode = new Node();
//					parent.childrenSubnodes.get(1).children.get(0).parents.clear();
//					parent.childrenSubnodes.get(1).children.get(1).parents.clear();
//					parent.childrenSubnodes.get(1).children.get(2).parents.clear();
//					newSupernode.add(parent.childrenSubnodes.get(1).children.get(0));
//					newSupernode.add(parent.childrenSubnodes.get(1).children.get(1));
//					newSupernode.add(parent.childrenSubnodes.get(1).children.get(2));
//					parent.childrenSubnodes.get(1).children.clear();
//					supernode.childrenSubnodes.get(0).add(newSupernode);
//					supernode.childrenSubnodes.get(1).add(newSupernode);
//				}
				parent.childrenSubnodes.get(1).addChildSupernode(childSupernode);
				parent.childrenSubnodes.get(2).addChildSupernode(childSupernode);
				nodes.set(i, childSupernode);
				nodes.removeAll(parent.childrenSubnodes);
			}
		}
	}
	private void trifusion(int i, ArrayList<Node> nodes) {
		if(nodes.get(i).parents.size()==1&&nodes.contains(nodes.get(i).parents.get(0).childrenSubnodes.get(0))&&nodes.contains(nodes.get(i).parents.get(0).childrenSubnodes.get(1))&&nodes.contains(nodes.get(i).parents.get(0).childrenSubnodes.get(2))){
			Node node=nodes.get(i);
			Node parent=node.parents.get(0);
			Node childLeft = parent.childrenSubnodes.get(0);
			Node childMid = parent.childrenSubnodes.get(1);
			Node childRight = parent.childrenSubnodes.get(2);
			nodes.set(i, parent);
			nodes.remove(childLeft);
			nodes.remove(childMid);
			nodes.remove(childRight);
		}
	}
	private Definition extractNewRecursiveDefinition(Definition expandedDefinition,
			ArrayList<Node> recursiveIn1, ArrayList<Node> recursiveOut1, ArrayList<Node> recursiveIn2, ArrayList<Node> recursiveOut2) {
		this.nodeInFusion(recursiveIn2);
		this.nodeInFusion(recursiveOut2);
		Definition tempRecursiveDefinition= new Definition(recursiveIn2.size(),recursiveOut2.size(),expandedDefinition.name+"Recur");
		ArrayList<Node> nodes = new ArrayList<Node>();
		nodes.addAll(recursiveIn2);
		nodes.addAll(recursiveOut2);
		for(Node outNode:recursiveOut2){
			outNode.parents.clear();
			outNode.outOfInstance=null;
		}
		expandedDefinition.add(tempRecursiveDefinition, nodes.toArray(new Node[nodes.size()]));
		expandedDefinition.update();
		this.nodeInFusion(recursiveIn1);
		this.nodeInFusion(recursiveOut1);
		tempRecursiveDefinition.in=recursiveIn1;
		tempRecursiveDefinition.out=recursiveOut1;
		Definition newRecursiveDefinition=tempRecursiveDefinition.copy();
		newRecursiveDefinition.replaceDefinition(tempRecursiveDefinition, newRecursiveDefinition);
		expandedDefinition.replaceDefinition(tempRecursiveDefinition, newRecursiveDefinition);
		nodes.clear();
		nodes.addAll(recursiveIn1);
		nodes.addAll(recursiveOut1);
		for(Node outNode:recursiveOut1){
			outNode.parents.clear();
			outNode.outOfInstance=null;
		}
		expandedDefinition.add(newRecursiveDefinition, nodes.toArray(new Node[nodes.size()]));
		expandedDefinition.update();
		return newRecursiveDefinition;
	}
	private void getRecursiveIO(Definition definition,Definition expandingDefinition,Definition expandedDefinition,
				ArrayList<Node> recursiveIn, ArrayList<Node> recursiveOut, HashMap<Node, NandNode> nodeToNand, HashMap<NandNode, Node> nandToNode) {
			HashSet<Node> knownNodes = new HashSet<Node>();
			HashSet<NandNode> knownNandNodes = new HashSet<NandNode>();
			ArrayList <Node> nandToNodeIn = new ArrayList <Node>(); //map of input nandnodes to nodes
			ArrayList <Node> nandToNodeOut = new ArrayList <Node>(); //map of output nandnodes to nodes
			HashSet<Instance> removedInstances = new HashSet<Instance>();
			AddedNodes addedNodes = new AddedNodes();
			expandingDefinition.mapNodes(knownNodes);
			HashMap<Node,Node> expandedToDefinition = new HashMap<Node,Node>();
			expandingDefinition.expandInstancesMapping(definition,expandedToDefinition);
			expandingDefinition.mapFission(knownNodes);//update originalNodes to keep track of fissed nodes
			expandingDefinition.replaceDefinition(definition,expandedDefinition);//replace occurrences of originalDefinition to this, for recursion consistency
			//to nand
			expandingDefinition.removeRecursion(addedNodes, removedInstances);
			expandingDefinition.nodeFissionMapping(knownNodes);//fission of nodes to minimum size needed, also removes redundant subnodes
			ArrayList<NandNode> recursionInNandNodes = new ArrayList<NandNode>();
			ArrayList<NandNode> recursionOutNandNodes = new ArrayList<NandNode>();
			
			NandForest expandingDefinitionNandForest = expandingDefinition.toNandForestMapping(nandToNodeIn,nandToNodeOut,nodeToNand,addedNodes,recursionInNandNodes,recursionOutNandNodes);//non recursive definition to nandforest
			for(Node node:knownNodes){
				if(nodeToNand.containsKey(node)){
					knownNandNodes.add(nodeToNand.get(node));
				}
			}
			ArrayList<NandNode> newRecursiveDefinitionNandIn = new ArrayList<NandNode>();
			ArrayList<NandNode> newRecursiveDefinitionNandOut = new ArrayList<NandNode>();
			this.extractNewRecursionNandIO(expandingDefinitionNandForest,knownNandNodes,newRecursiveDefinitionNandIn,newRecursiveDefinitionNandOut);
			this.addRecursionNodesToNandIO(knownNandNodes,recursionInNandNodes,recursionOutNandNodes,newRecursiveDefinitionNandIn,newRecursiveDefinitionNandOut);
			
			this.fromNandForest(expandingDefinition, expandingDefinitionNandForest, nandToNodeIn, nandToNodeOut,nandToNode);
			for(NandNode nandNode:newRecursiveDefinitionNandIn){
				recursiveIn.add(nandToNode.get(nandNode));
			}
			for(NandNode nandNode:newRecursiveDefinitionNandOut){
				recursiveOut.add(nandToNode.get(nandNode));
			}
			HashSet<Node> newKnownNodes = new HashSet<Node>();
			for(NandNode nandNode:knownNandNodes){
				newKnownNodes.add(nandToNode.get(nandNode));
			}
			expandingDefinition.recoverRecursion(addedNodes, removedInstances);
  			expandingDefinition.update();//can update since it doesn't break references (to hashsets)
	}

	private void addRecursionNodesToNandIO(
			HashSet<NandNode> originalNandNodes,
			ArrayList<NandNode> recursionInNandNodes,
			ArrayList<NandNode> recursionOutNandNodes,
			ArrayList<NandNode> newRecursiveDefinitionNandIn,
			ArrayList<NandNode> newRecursiveDefinitionNandOut) {
		for(NandNode nandNode:recursionInNandNodes){
			if(originalNandNodes.contains(nandNode)){
				if(!newRecursiveDefinitionNandIn.contains(nandNode)){
					newRecursiveDefinitionNandIn.add(nandNode);
				}
			}
		}
		for(NandNode nandNode:recursionOutNandNodes){
			if(originalNandNodes.contains(nandNode)){
				if(!newRecursiveDefinitionNandOut.contains(nandNode)){
					newRecursiveDefinitionNandOut.add(nandNode);
				}
			}
		}
		
	}
	private void extractNewRecursionNandIO(
			NandForest expandedDefinitionNandForest,
			HashSet<NandNode> originalNandNodes,
			ArrayList<NandNode> newRecursiveDefinitionNandIn,
			ArrayList<NandNode> newRecursiveDefinitionNandOut) {
		for(NandNode nandNode:expandedDefinitionNandForest.out){
			nandNode.extractNewRecursionNandIO(expandedDefinitionNandForest,originalNandNodes,newRecursiveDefinitionNandIn,newRecursiveDefinitionNandOut);
		}
		
	}
//	private NandForest extractNewDefinitionNandForest(
//			ArrayList<NandNode> originalDefinitionNandIn,
//			ArrayList<NandNode> newRecursiveDefinitionNandOut,
//			ArrayList<NandNode> originalDefinitionNandOut,
//			ArrayList<NandNode> newRecursiveDefinitionNandIn) {
//		NandForest nandForest = new NandForest(0);
//		nandForest.in.addAll(originalDefinitionNandIn);
//		nandForest.in.addAll(newRecursiveDefinitionNandOut);
//		nandForest.out.addAll(originalDefinitionNandOut);
//		nandForest.out.addAll(newRecursiveDefinitionNandIn);
//		nandForest.optimize();
//		return nandForest;
//	}
//	public Definition fromNandForest(Definition definition,NandForest nandForest, ArrayList<Node> nandToNodeIn,ArrayList<Node> nandToNodeOut){
//		//set existing Definition from NandForest without NandNode's repetition	
//		HashMap <NandNode,Node> nandToNode = new HashMap <NandNode,Node>();
//		definition.clearInstances();
//		for (int i=0;i<nandToNodeIn.size();i++) {//we map only inputs because expanding is bottom to top
//			nandToNode.put(nandForest.in.get(i),nandToNodeIn.get(i));
//		}
//		for (int i=0;i<nandToNodeOut.size();i++) {//node can be out node of definition OR a subnode from one
////				for (int j = 0; j < nandForest.in.size(); j++) {//FIX FOR WHEN A NANDFOREST NODE IS BOTH IN AND OUT
////					if(nandForest.in.get(j)==nandForest.out.get(i)){
////						for (int k = 0; k < definition.out.size(); k++) {
////							if(definition.out.get(k)==nandToNodeOut.get(i)){
////								definition.out.set(k, nandToNode.get(nandForest.out.get(i)));//it's and out node
////							}
////						}
////						for (Node parent :nandToNodeOut.get(i).parents){
////							for (int k = 0; k < parent.parents.size(); k++) {
////								if(parent.parents.get(k)==nandToNodeOut.get(i)){
////									parent.parents.set(k, nandToNode.get(nandForest.out.get(i)));//it's a subnode
////								}
////							}
////						}
////					}
////				}
//				nandToNodeOut.get(i).outOfInstance=null;
//				this.addNands(nandToNodeOut.get(i),nandForest.out.get(i),definition,nandForest,nandToNode);
//		}
//		
//		return definition;
//	}
	private Definition fromNandForest(Definition definition,
			NandForest nandForest,
			ArrayList<Node> nandToNodeIn, ArrayList<Node> nandToNodeOut, HashMap <NandNode,Node> nandToNode) {
		//set existing Definition from NandForest without NandNode's repetition	
				
				definition.clearInstances();
				for (int i=0;i<nandToNodeIn.size();i++) {//we map only inputs because expanding is bottom to top
					nandToNode.put(nandForest.in.get(i),nandToNodeIn.get(i));
				}
				for (int i=0;i<nandToNodeOut.size();i++) {//node can be out node of definition OR a subnode from one
//						for (int j = 0; j < nandForest.in.size(); j++) {//FIX FOR WHEN A NANDFOREST NODE IS BOTH IN AND OUT
//							if(nandForest.in.get(j)==nandForest.out.get(i)){
//								for (int k = 0; k < definition.out.size(); k++) {
//									if(definition.out.get(k)==nandToNodeOut.get(i)){
//										definition.out.set(k, nandToNode.get(nandForest.out.get(i)));//it's and out node
//									}
//								}
//								for (Node parent :nandToNodeOut.get(i).parents){
//									for (int k = 0; k < parent.parents.size(); k++) {
//										if(parent.parents.get(k)==nandToNodeOut.get(i)){
//											parent.parents.set(k, nandToNode.get(nandForest.out.get(i)));//it's a subnode
//										}
//									}
//								}
//							}
//						}
						nandToNodeOut.get(i).outOfInstance=null;
						this.addNands(nandToNodeOut.get(i),nandForest.out.get(i),definition,nandForest,nandToNode);
				}
				//can't update definition here since Maps will lose references
				return definition;
	}
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
			Instance nandInstance = new Instance();//instance of a definition
			nandInstance.in = new ArrayList<Node>();
			nandInstance.in.add(in1);
			nandInstance.in.add(in2);
			nandInstance.definition=this.get("nand");
			outNode.outOfInstance=nandInstance;
			def.add(outNode);
			nandInstance.out.add(outNode);
			def.instances.add(nandInstance );
			nandToNode.put(nandNode,outNode);
		}
	}
	public void toHighestLevel(Definition definition) {
		//PRE: definition may be recursive
		//POST: apply definitions from definitionDB to transform the definition to the highest level possible
		//TODO:prioritize intersection elimination, if more practical needed add one more stage
		HashSet<Node> supernodeOuts= new HashSet<Node>();
		//Use A* type algorithm to locate highest level definitions
		//applying all definitions with same root
		definition.mapSupernodeOuts(supernodeOuts);
		int instanceIndex;
		int rootIndex;
		boolean appliedOnce;//at least one definition has been applied
		Instance instance;
		Definition appliedDefinition;
		do{
			appliedOnce=false;
			instanceIndex=0;
			while (instanceIndex<definition.instances.size()) {//one level up, while needed instead of for, since list can be modified on loop
				instance=definition.instances.get(instanceIndex);
				rootIndex=0;
				boolean applied=false;
				while (rootIndex<instance.definition.rootIn.size()&&applied==false) {//loop while not modified (if one rootIn used, rest worthless)
					appliedDefinition=instance.definition.rootIn.get(rootIndex);
					if(definition!=appliedDefinition){//prevent applying definition to self
						applied=definition.apply(instance,appliedDefinition,supernodeOuts);
						if (applied) {
							instanceIndex=0;//restart index
						}
						appliedOnce=appliedOnce||applied;
					}
					rootIndex++;
				}	
				instanceIndex++;
			}
		}while(appliedOnce);//while at least one definition applied through all instances
	}
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
