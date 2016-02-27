/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
		this.toHighestLevel(definition);//definition made of instances of nand definiition, to highest level possible
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
				definition.nodeFission();//fission of nodes to minimum size needed, also removes redundant subnodes
				NandForest nandForest = definition.toNandForest(nandToNodeIn,nandToNodeOut);//non recursive definition to nandforest
				nandForest.optimize();//to remove possible unused nodes
				this.fromNandForest(definition,nandForest,nandToNodeIn,nandToNodeOut);//definition using only instances of nand
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
		Definition newRecursiveDefinition= new Definition();
		this.extractNewDefinition(definition,newRecursiveDefinition);
		Definition placeholder = new Definition();
		this.extractNewDefinition(newRecursiveDefinition,placeholder);
		newRecursiveDefinition.replaceDefinition(placeholder, newRecursiveDefinition);
	}
	private Definition extractNewRecursiveDefinition(
			NandForest newDefinitionNandForest,
			HashSet<NandNode> newRecursiveDefinitionNandIn,
			HashSet<NandNode> newRecursiveDefinitionNandOut) {
		// TODO Auto-generated method stub
		return null;
	}
	private void extractNewDefinition(Definition definition,Definition newRecursiveDefinition) {
		HashSet<NandNode> originalDefinitionNandIn = new HashSet<NandNode>();
		HashSet<NandNode> originalDefinitionNandOut = new HashSet<NandNode>();
		HashSet<NandNode> originalAddedDefinitionNandIn = new HashSet<NandNode>();
		HashSet<NandNode> originalAddedDefinitionNandOut = new HashSet<NandNode>();
		HashSet<NandNode> newRecursiveDefinitionNandIn = new HashSet<NandNode>();
		HashSet<NandNode> newRecursiveDefinitionNandOut = new HashSet<NandNode>();

		HashSet<Node> originalNodes = new HashSet<Node>();
		HashSet<NandNode> originalNandNodes = new HashSet<NandNode>();
		ArrayList <Node> nandToNodeIn = new ArrayList <Node>(); //map of input nandnodes to nodes
		ArrayList <Node> nandToNodeOut = new ArrayList <Node>(); //map of output nandnodes to nodes
		AddedNodes addedNodes = new AddedNodes();
		HashSet<Instance> removedInstances = new HashSet<Instance>();
		//expand definition
		Definition expandedDefinition = definition.copy();//freeze original for expansion
		expandedDefinition.mapNodes(originalNodes);
		expandedDefinition.expandRecursiveInstances(definition);
		//to nand
		expandedDefinition.removeRecursion(addedNodes, removedInstances);
//		this.toNandDefinitionsMapping();
		expandedDefinition.nodeFission();//fission of nodes to minimum size needed, also removes redundant subnodes
		expandedDefinition.mapFission(originalNodes);//update originalNodes to keep track of fissed nodes
		NandForest expandedDefinitionNandForest = expandedDefinition.toNandForestMapping(nandToNodeIn,nandToNodeOut,originalNodes,originalNandNodes,addedNodes,originalDefinitionNandIn,originalDefinitionNandOut,originalAddedDefinitionNandIn,originalAddedDefinitionNandOut);//non recursive definition to nandforest
//		extractDefinitionNodes(nandToNodeIn,nandToNodeOut,addedNodes,originalDefinitionNandIn,originalDefinitionNandOut,originalAddedDefinitionNandIn,originalAddedDefinitionNandOut);
		extractNewRecursionNandIO(expandedDefinitionNandForest,originalNandNodes,newRecursiveDefinitionNandIn,newRecursiveDefinitionNandOut);
		NandForest newExpandedDefinitionNandForest = extractNewDefinitionNandForest(expandedDefinitionNandForest,addedNodes,newRecursiveDefinitionNandIn,newRecursiveDefinitionNandOut);
		ArrayList<Node> newRecursiveDefinitionIn = new ArrayList<Node>(newRecursiveDefinitionNandIn.size());
		ArrayList<Node> newRecursiveDefinitionOut = new ArrayList<Node>(newRecursiveDefinitionNandOut.size());
		this.setRecursionIO(expandedDefinition,nandToNodeIn,nandToNodeOut,addedNodes,newRecursiveDefinitionIn,newRecursiveDefinitionOut);
		this.fromNandForest(expandedDefinition, newExpandedDefinitionNandForest,nandToNodeIn,nandToNodeOut);
		expandedDefinition.in.subList(definition.in.size()-addedNodes.in,definition.in.size()).clear();
		expandedDefinition.out.subList(definition.out.size()-addedNodes.out,definition.out.size()).clear();
		ArrayList<Node> nodes = new ArrayList<Node>();
		nodes.addAll(newRecursiveDefinitionIn);
		nodes.addAll(newRecursiveDefinitionOut);
		expandedDefinition.add(newRecursiveDefinition,nodes.toArray(new Node[nodes.size()]));
		definition=expandedDefinition.copy();
//		this.extractNewRecursiveDefinition(newDefinitionNandForest,newRecursiveDefinitionNandIn,newRecursiveDefinitionNandOut);

	}
	private void setRecursionIO(Definition definition,
			ArrayList<Node> nandToNodeIn, ArrayList<Node> nandToNodeOut, AddedNodes addedNodes, ArrayList<Node> newRecursiveDefinitionIn, ArrayList<Node> newRecursiveDefinitionOut) {
		for(Node node:definition.in.subList(definition.in.size()-addedNodes.in,definition.in.size())){
			nandToNodeIn.remove(node);//TODO: check works ok when multiple nodes equal
		}
		definition.in.subList(definition.in.size()-addedNodes.in,definition.in.size()).clear();
		definition.in.addAll(newRecursiveDefinitionOut);
		addedNodes.in=newRecursiveDefinitionOut.size();
		for(Node node:definition.out.subList(definition.out.size()-addedNodes.out,definition.out.size())){
			nandToNodeIn.remove(node);
		}
		definition.out.subList(definition.out.size()-addedNodes.out,definition.out.size()).clear();
		definition.out.addAll(newRecursiveDefinitionIn);
		nandToNodeIn.addAll(newRecursiveDefinitionOut);
		nandToNodeOut.addAll(newRecursiveDefinitionIn);
	}
	private NandForest extractNewDefinitionNandForest(
			NandForest expandedDefinitionNandForest,
			AddedNodes addedNodes, HashSet<NandNode> newRecursiveDefinitionNandIn,
			HashSet<NandNode> newRecursiveDefinitionNandOut) {
		NandForest nandForest = new NandForest(0);
		for(int i=0;i<(expandedDefinitionNandForest.in.size()-addedNodes.in);i++){
			nandForest.in.add(expandedDefinitionNandForest.in.get(i));
		}
		for(NandNode nandNode:newRecursiveDefinitionNandOut){
			nandForest.in.add(nandNode);
		}
		for(int i=0;i<(expandedDefinitionNandForest.out.size()-addedNodes.out);i++){
			nandForest.out.add(expandedDefinitionNandForest.out.get(i));
		}
		for(NandNode nandNode:newRecursiveDefinitionNandIn){
			nandForest.out.add(nandNode);
		}
		nandForest.optimize();
		return nandForest;
	}
	private void extractNewRecursionNandIO(NandForest expandedNandTree,
			HashSet<NandNode> originalNandNodes, HashSet<NandNode> newRecursiveDefinitionNandIn,
			HashSet<NandNode> newRecursiveDefinitionNandOut) {
		ArrayList<NandNode> nandNodes = new ArrayList<NandNode>();
		nandNodes.addAll(expandedNandTree.nodes.values());
		for(NandNode nandNode:nandNodes){
			if(!originalNandNodes.contains(nandNode)){
				if(originalNandNodes.contains(nandNode.in1)){
					newRecursiveDefinitionNandIn.add(nandNode.in1);
				}
				if(originalNandNodes.contains(nandNode.in2)){
					newRecursiveDefinitionNandIn.add(nandNode.in2);
				}
			}else{
				if((!originalNandNodes.contains(nandNode.in1))&&(nandNode.in1!=null)){
					newRecursiveDefinitionNandOut.add(nandNode.in1);
				}
				if((!originalNandNodes.contains(nandNode.in2))&&(nandNode.in2!=null)){
					newRecursiveDefinitionNandOut.add(nandNode.in2);
				}
			}
		}
		
	}
	public Definition fromNandForest(Definition definition,NandForest nandForest, ArrayList<Node> nandToNodeIn,ArrayList<Node> nandToNodeOut){
		//set existing Definition from NandForest without NandNode's repetition	
		HashMap <NandNode,Node> nandToNode = new HashMap <NandNode,Node>();
		for (int i=0;i<nandToNodeIn.size();i++) {//we map only inputs because expanding is bottom to top
			nandToNode.put(nandForest.in.get(i),nandToNodeIn.get(i));
		}
		for (int i=0;i<nandToNodeOut.size();i++) {//node can be out node of definition OR a subnode from one
				for (int j = 0; j < nandForest.in.size(); j++) {//FIX FOR WHEN A NANDFOREST NODE IS BOTH IN AND OUT
					if(nandForest.in.get(j)==nandForest.out.get(i)){
						for (int k = 0; k < definition.out.size(); k++) {
							if(definition.out.get(k)==nandToNodeOut.get(i)){
								definition.out.set(k, nandToNode.get(nandForest.out.get(i)));//it's and out node
							}
						}
						for (Node parent :nandToNodeOut.get(i).parents){
							for (int k = 0; k < parent.parents.size(); k++) {
								if(parent.parents.get(k)==nandToNodeOut.get(i)){
									parent.parents.set(k, nandToNode.get(nandForest.out.get(i)));//it's a subnode
								}
							}
						}
					}
				}
				this.addNands(nandToNodeOut.get(i),nandForest.out.get(i),definition,nandForest,nandToNode);
		}
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
							instanceIndex-=appliedDefinition.instances.size()-1;//remove to instanceIndex the number of deleted instances
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
