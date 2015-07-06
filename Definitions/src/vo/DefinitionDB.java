/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

//DESCRIPTION
//-Database of unique definitions, recursively defined by other contained definitions
//-indexed by name
//-nand is the base definition
//-serialized:
//	-save definitions to file
//	-load definitions from file
//IMPLEMENTATION
//- fix recursion optimization
//- fusion of NandForest
//- verify toBest (most critical and complex algorithm)
//- suitable for in-memory use
//-FIXME(maybe): can't have definitions outside of DB (or else they're added to usedIn and things get messed up)
//-take into account optimized in out
//////////////////////////////////////////////////////
//HERE BE DRAGONS (maybe not in Java from here)
//////////////////////////////////////////////////////
//-take into account synonyms
//-optimize using intersections instead of brute force (merge already optimized definitions vs full nandtree of definition each time)
public class DefinitionDB implements java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6404880670897370982L;
	HashMap<String,Definition> definitions;
	public DefinitionDB(Definition nand){
		definitions = new HashMap<String,Definition>();
		this.definitions.put("nand",nand);//nand definition needed as building block for definitions
	}
	public void put(String name, Definition definition){
		this.optimize(definition);
		this.definitions.put(name, definition);//insert optimized definition in database
		//Optimize all definitions where this new definition could be used (implicit bigger than definition) (+++)
		//TODO: verify that order of adding definitions is irrelevant example: and then not-> not used in and
	}
	public Definition get(String name){
		return this.definitions.get(name);
	}
	private Definition optimize(Definition definition) {
		//PRE: definition's instances are optimized 
		//POST: definition is optimized, of the highest level possible
		
		//definition may be recursive
		//map recursions, transform to non-recursive definition
		//add recursive outputs to in and recursive inputs as outs,
		//transform definition to NandForest,
		//transform NandForest to best definitions
		//use map to return previous recursions 
		//remove added ins and outs from recursions
		//if recursive then optimize sub-definitions AND MERGE
		//if not recursive then fromNand to def -> to best
		//definition->nandtree node fission
		//TODO:
			//with subnodes and recursion
			//toBest to account recursion
			//nandtree->definition node fusion
			//intersection optimization of recursive definitions
			
		
		if(definition.recursive){
			//Optimize the non recursive part of definition as a temporary definition
			//TODO: take care of indirect recursion (definition can be non recursive, but contain instances of recursive definitions
			//TODO: add->multiply = simple recursion -> nested recursion
			Definition tempDef = new Definition(0,"temp");//(recursive is set to false)
			for(Node node : definition.nodes){
					node.idForDefinition.put(tempDef , tempDef.nodes.size());//debugging only
					tempDef.nodes.add(node);//All?
			}
			tempDef.in.addAll(definition.in);//copy definition in nodes
		    tempDef.out.addAll(definition.out);//copy definition out nodes
		    tempDef.instances.addAll(definition.instances);
		    tempDef.instances.removeAll(definition.recursiveInstances);//remove all recursive instances, to make the definition not self recursive
		    tempDef.recursive=false;
		    tempDef.containedDefinitions.putAll(definition.containedDefinitions);
		    tempDef.containedDefinitions.remove(definition);
			for(Instance instance : definition.recursiveInstances){//add nodes from recursive instances
				for (int i = 0; i < instance.out.size(); i++) {//map out nodes to tempDef in
					tempDef.in.add(instance.out.get(i));
				}
				for (int i = 0; i < instance.in.size(); i++) {//map in nodes to nand out
					tempDef.out.add(instance.in.get(i));
				}
			}
			this.optimize(tempDef);
			//copy back the optimized non recursive part of definition restoring recursion
			//in and out nodes don't change
			
			definition.instances=tempDef.instances;
			definition.instances.addAll(definition.recursiveInstances);//keep recursive instances
			definition.containedDefinitions=tempDef.containedDefinitions;
			definition.containedDefinitions.put(definition, definition.recursiveInstances);
			for(Node node:tempDef.nodes){
				definition.add(node);
			}
			//rootIn is not modified
//			this.nodeFusion(definition);//here for debugging, take out of if (recursive or not, do fusion)
		}else{//definition is not recursive
			if(definition.name!="nand"){ //if definition is nand already optimized!
				ArrayList <Node> nandToNodeIn = new ArrayList <Node>(); //map of input nandnodes to nodes
				ArrayList <Node> nandToNodeOut = new ArrayList <Node>(); //map of output nandnodes to nodes
				//NODE FISSION
				NandForest nandForest = definition.toNandForest(nandToNodeIn,nandToNodeOut);//non recursive definition to nandforest
				nandForest.optimize();//to remove possible unused nodes
				this.fromNandForest(definition,nandForest,nandToNodeIn,nandToNodeOut);//definition using only instances of nand
				this.toBest(definition);//nand definition to best definition (higher level)//TODO: subnodes
			}
		}
		
		
		
		return definition;
	}
	private void nodeFusion(Definition def) {
		HashSet<Node> nodes = new HashSet<Node>();
		for(Node node:def.out){
			node.fusion(def,nodes);
		}
		
	}
	public Definition fromNandForest(Definition definition,NandForest nandForest, ArrayList<Node> nandToNodeIn,ArrayList<Node> nandToNodeOut){
		//set existing Definition from NandForest without NandNode's repetition
		definition.nodes.clear();
		definition.instances.clear();
		definition.rootIn.clear();
		definition.containedDefinitions.clear();
		//FIXME: if a node is both in and out, it gets to in/=out
		for(Node node:definition.in){
			definition.add(node);
			for(Node subnode:node.subnodes){
				definition.add(subnode);
			}
		}
		for(Node node:definition.out){
			definition.add(node);
			for(Node subnode:node.subnodes){
				definition.add(subnode);
			}
		}
		
		HashMap <NandNode,Node> nandToNode = new HashMap <NandNode,Node>();
		int i=0;
		for (Node node:nandToNodeIn){//we map only inputs to map top to bottom
			nandToNode.put(nandForest.in.get(i),node);	
			i++;
		}
		i=0;
		for (Node node : nandToNodeOut) {//node can be out node of definition OR a subnode from one
				for (int j = 0; j < nandForest.in.size(); j++) {//FIX FOR WHEN AN NANDFOREST NODE IS BOTH IN AND OUT
					if(nandForest.in.get(j)==nandForest.out.get(i)){
						for (int k = 0; k < definition.out.size(); k++) {
							if(definition.out.get(k)==node){
								definition.out.set(k, nandToNode.get(nandForest.out.get(i)));//it's and out node
							}
						}
						for (Node supernode :node.supernodes){
							for (int k = 0; k < supernode.subnodes.size(); k++) {
								if(supernode.subnodes.get(k)==node){
									supernode.subnodes.set(k, nandToNode.get(nandForest.out.get(i)));//it's a subnode
								}
							}
						}
					}
				}
				this.addNands(node,nandForest.out.get(i),definition,nandForest,nandToNode);
				if(definition.out.isEmpty()&&node.outOfInstance!=null&&node.outOfInstance.definition!=null){//FIXME//avoid applying definition to self, first def->instance second def->definition 
					if(!node.outOfInstance.definition.rootIn.contains(definition)){//FIXME:needed because not hash
						node.outOfInstance.definition.rootIn.add(definition);
					}
				}
				i++;
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
			in1.inOfInstances.add(nandInstance);
			in2.inOfInstances.add(nandInstance);
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
	public void toBest(Definition definition) {
		//Use A* type algorithm to locate higher level definitions
		//Optimize/simplify definition applying all definitions with same root/out(0)
		int instanceIndex;
		int rootIndex;
		boolean appliedOnce;
		Instance instance;
		Definition appliedDefinition;
		do{
			appliedOnce=false;
			instanceIndex=0;
			while (instanceIndex<definition.instances.size()) {//one level up //while not for, since list can be modified on loop
				instance=definition.instances.get(instanceIndex);
				rootIndex=0;
				boolean applied=false;
				while (rootIndex<instance.definition.rootIn.size()&&applied==false) {//loop while not modified (if one rootIn used, rest worthless)
					appliedDefinition=instance.definition.rootIn.get(rootIndex);
					if(definition!=appliedDefinition){//prevent applying definition to self
						applied=definition.apply(instance,appliedDefinition);//TODO:apply should remove to instanceIndex the number of deleted instances
						appliedOnce=appliedOnce||applied;
					}
					rootIndex++;
				}	
				instanceIndex++;
				//definition.print();
			}
		}while(appliedOnce);//while at least one definition applied throught all instances
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
	public void write(java.io.ObjectOutputStream stream)
            throws IOException {
		HashMap<Definition,Integer> defMap = new HashMap<Definition,Integer>();
		int definitionIndex=0;
		stream.write(this.definitions.size());
		for(String name : this.definitions.keySet()){//map all definitions
			defMap.put(this.definitions.get(name),definitionIndex);
			definitionIndex++;
		}
		for(String name : this.definitions.keySet()){//write mapped definitions
				this.definitions.get(name).write(stream,defMap);
				stream.writeObject(name);
		}
    }

    public void read(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
    	HashMap<Integer,Definition> defMap = new HashMap<Integer,Definition>();
    	int numDefinitions=0;
		String name = null;
		numDefinitions=stream.read();
		for (int i = 0; i < numDefinitions; i++) {
			Definition definition = new Definition(0,name);
			defMap.put(i, definition);
		}
		for (int i = 0; i < numDefinitions; i++) {
			defMap.get(i).read(stream,defMap);
			name = (String)stream.readObject();
			this.definitions.put(name,defMap.get(i));
		}
    }
}
