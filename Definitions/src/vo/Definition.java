/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import utils.AddedNodes;
import utils.FixedBitSet;

//DESCRIPTION
//-Defines a "Definition", procedure, function with multiple outputs
//-Recursive definition:
//	#A definition is composed by instances of definitions
//	#Nand is the basic definition
//-Definitions may be recursive and ARE TURING COMPLETE


//FIXME: 
//-toString of lesser level components only works after definition.toString has been called
//-can't have definitions outside of DB (or else they're added to usedIn and things get messed up)
//-remove value
//TODO: 
//-Implement logical if
//-recursion -> implement simple assembler as definitions
//	-CMP A,B,flag Z (1 bit) / PC ++ (in parallel=extra input/output)
//	-ADD A,B,C / PC ++ (in parallel=extra input/output)

//////////////////////////////////////////////////////
//HERE BE DRAGONS (maybe not in Java from here)
//////////////////////////////////////////////////////
//-optimize definition while adding new node (efficiency)
//-parallelization of code
//-out 0 by defect
//-commands for interactive definition
//
//OPTIMIZATION
//-on eval conserve node values, don't recalculate (like separated functions)-> look toNand
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



public class Definition implements java.io.Serializable{ /**
	 * 
	 */
	private static final long serialVersionUID = -3804848616644678453L;
			public String name;//not needed, only for debug
			public HashMap<Node,Integer> tradInts;//not needed, only for debug
			public ArrayList<Node> in;//NEEDED
			public ArrayList<Node> out;//NEEDED
			public ArrayList<Instance> instances;//FIXME: better data structure Hash AND list linkedhashmap?  - replaces def?
			public ArrayList<Definition> rootIn;//FIXME: better data structure Hash AND list linkedhashmap? - verify definitions are in DB
			public HashSet<Instance> recursiveInstances;//Recursive instances of this definition, contained in this definition
			public HashSet<Instance> instancesOfRecursiveDefinitions;//Instances of other recursive definitions
			//DEBUGGING ONLY
			public HashSet<Node> nodes;
			//END OF DEBUGGING ONLY
			
			//CONSTRUCTOR
			public Definition(int numberOfInputs,int numberOfOutputs, String name){
				this.name=name;
				this.in = new ArrayList<Node>();
				this.out = new ArrayList<Node>();
				this.instances = new ArrayList<Instance>();
				this.recursiveInstances = new HashSet<Instance>();
				this.instancesOfRecursiveDefinitions = new HashSet<Instance>();
				this.rootIn = new ArrayList<Definition>();
				//DEBUGGING ONLY
				this.nodes = new HashSet<Node> ();
				//END OF DEBUGGING ONLY
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
				// TODO Auto-generated constructor stub
			}
			public NandForest toNandForest(ArrayList<Node> nandToNodeIn, ArrayList<Node> nandToNodeOut){
				//PRE: this definition is not recursive and doesn't contain recursive definitions
				//POST: returns a NandForest equivalent to this definition, map of in and map of out nandnodes to nodes
				//more efficient thanks to HashMap's of unique nodes
				//TOPDOWN OR DOWNUP? DOWNUP less branches, UPDOWN less memory? -> DOWNUP needed to optimize and instance
				NandForest nandForest = new NandForest(0);
				HashMap<Node, ArrayList<NandNode>> nodeToNands = new HashMap<Node, ArrayList<NandNode>>();
				HashMap<Node,Integer> nodeSize = new HashMap<Node,Integer>();
				//NODE FISSION
				this.getNodesSize(nodeSize);//get the size of all the definition nodes//FIXME not ok sizes with dec
				this.setIns(nodeSize,nodeToNands,nandForest);//from bottom to top//(mapping in and finding inputs size first is needed)
				///Need to map subnodes of ins to conserve references?
				for (Node node:this.out) {
					HashSet<Node> expandedNodes = new HashSet<Node>();
					nandForest.addOuts(node.toNands(nodeSize,expandedNodes,nodeToNands,nandForest));//FIXME
				}
				//IN and OUTS mapped and in nandForest
				//map in nandNodes to nodes
				this.nodes.clear();
				this.nodes.addAll(this.in);
				for (Node inNode:this.in){
					if(nodeSize.get(inNode)==1){
						nandToNodeIn.add(inNode);
					}else{
						for(Node child:inNode.children){//TODO: fix for recursive children?
							nandToNodeIn.add(child);
						}
						for (int i = 0; i < nodeSize.get(inNode)-inNode.children.size(); i++) {
							Node child = new Node();
							inNode.add(child);
							nandToNodeIn.add(child);
						}
					}
						
				}	
				this.nodes.addAll(this.out);
				for (Node outNode:this.out){
					if(nodeSize.get(outNode)==1){
						nandToNodeOut.add(outNode);
					}else{
						for(Node parent:outNode.parents){//TODO: fix for recursive parents?
							nandToNodeOut.add(parent);
						}
						for (int i = 0; i < nodeSize.get(outNode)-outNode.parents.size(); i++) {
							Node parent = new Node();
							parent.add(outNode);
							nandToNodeOut.add(parent);
						}
					}
					
				}
				return nandForest;
			}
			private void setIns(HashMap<Node, Integer> nodeSize, HashMap<Node, ArrayList<NandNode>> nodeToNands, NandForest nandForest) {
				//map input nodes to nandNodes
				for(Node inNode:this.in){
					ArrayList<NandNode> nandNodes = new ArrayList<NandNode>();
//					if(inNode.children.isEmpty()){
						nandNodes=nandForest.addIns(nodeSize.get(inNode));
//					}else{
//						for(Node child:inNode.children){
//							if(child.parents.size()==1){//subnode
//								ArrayList<NandNode> subNandNodes= new ArrayList<NandNode>();
//								subNandNodes=nandForest.addIns(nodeSize.get(child));
//								nodeToNands.put(child, subNandNodes);
//								nandNodes.addAll(subNandNodes);
//							}else{
//								nandNodes.addAll(nandForest.addIns(nodeSize.get(child)));
//							}
//						}
//					}
					nodeToNands.put(inNode, nandNodes);
				}
				
			}
			private void getNodesSize(HashMap<Node, Integer> nodeSize) {
				for(Node node: this.out){
					this.expand(node,nodeSize);
				}
				
			}
			private void expand(Node node, HashMap<Node, Integer> nodeSize) {
				//expand up, then expand down if needed
				if (!nodeSize.containsKey(node)) nodeSize.put(node, 1);//size 1 if not subnodes nor mapped
				int parentsSize=0;
				int childrenSize=0;
//				//expand outs
				for(Node child:node.children){
					if(!nodeSize.containsKey(child)){
						expand(child,nodeSize);
					}
					if(child.parents.size()==1){
						childrenSize+=nodeSize.get(child);	
					}
				}
				if(nodeSize.get(node)<childrenSize){
					nodeSize.put(node, childrenSize);
				}
				//expand ins
				for(Node parent:node.parents){
					if(!nodeSize.containsKey(parent)){
						expand(parent,nodeSize);
					}
					if(parent.children.size()==1){
						parentsSize+=nodeSize.get(parent);
					}
				}
				if(nodeSize.get(node)<parentsSize){
					nodeSize.put(node, parentsSize);
				}
				
				if(node.outOfInstance!=null){//the node is out of instance
					if(node.outOfInstance.definition.name=="nand"){//NAND //TODO: fix nand checking
						//this is out
						//expand ins
						if(!nodeSize.containsKey(node.outOfInstance.in.get(0))||nodeSize.get(node.outOfInstance.in.get(0))<nodeSize.get(node)){
							nodeSize.put(node.outOfInstance.in.get(0), nodeSize.get(node));
							this.expand(node.outOfInstance.in.get(0), nodeSize);
						}
						if(!nodeSize.containsKey(node.outOfInstance.in.get(1))||nodeSize.get(node.outOfInstance.in.get(1))<nodeSize.get(node)){
							nodeSize.put(node.outOfInstance.in.get(1), nodeSize.get(node));
							this.expand(node.outOfInstance.in.get(1), nodeSize);
						}
						int size0=nodeSize.get(node.outOfInstance.in.get(0));
						int size1=nodeSize.get(node.outOfInstance.in.get(1));
						if(nodeSize.get(node)<size0){
							nodeSize.put(node, size0);
						}
						if(nodeSize.get(node)<size1){
							nodeSize.put(node, size1);
						}
						if(size0<nodeSize.get(node)){
							nodeSize.put(node.outOfInstance.in.get(0), nodeSize.get(node));
							this.expand(node.outOfInstance.in.get(0), nodeSize);
						}
						if (size1<nodeSize.get(node)){
							nodeSize.put(node.outOfInstance.in.get(1), nodeSize.get(node));
							this.expand(node.outOfInstance.in.get(1), nodeSize);
						}
					}else{//the node is out of an instance different to NAND
						if(this.instances.contains(node.outOfInstance)){//check definition has not been removed (= is recursive)
							HashMap<Node, Integer> tempNodeSize = new HashMap<Node, Integer>();
							//expand outs
							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
								if(!nodeSize.containsKey(node.outOfInstance.out.get(i))){
									this.expand(node.outOfInstance.out.get(i),nodeSize);
								}
								tempNodeSize.put(node.outOfInstance.definition.out.get(i),nodeSize.get(node.outOfInstance.out.get(i)));
							}
							//expand ins
							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
								if(!nodeSize.containsKey(node.outOfInstance.in.get(i))){
									this.expand(node.outOfInstance.in.get(i),nodeSize);
								}
								tempNodeSize.put(node.outOfInstance.definition.in.get(i),nodeSize.get(node.outOfInstance.in.get(i)));
							}
							//expand definition
							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
								node.outOfInstance.definition.expand(node.outOfInstance.definition.out.get(i), tempNodeSize);
							}
							//expand outs if needed
							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
								if(nodeSize.get(node.outOfInstance.out.get(i))<tempNodeSize.get(node.outOfInstance.definition.out.get(i))){
									nodeSize.put(node.outOfInstance.out.get(i), tempNodeSize.get(node.outOfInstance.definition.out.get(i)));
									if(node!=node.outOfInstance.definition.out.get(i)){
										this.expand(node.outOfInstance.definition.out.get(i), tempNodeSize);
									}
								}
							}
							//expand ins if needed
							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
								if(nodeSize.get(node.outOfInstance.in.get(i))<tempNodeSize.get(node.outOfInstance.definition.in.get(i))){
									nodeSize.put(node.outOfInstance.in.get(i), tempNodeSize.get(node.outOfInstance.definition.in.get(i)));
									expand(node.outOfInstance.in.get(i),nodeSize);
								}
							}
						}
					}
				}
				if(node.inOfInstances!=null){//the node is in of instance
					for(Instance instance:node.inOfInstances){
						if(this.instances.contains(instance)){//check definition has not been removed (= is recursive)
							if(instance.definition.name=="nand"){//NAND //TODO: fix nand checking
								if(!nodeSize.containsKey(instance.out.get(0))||nodeSize.get(instance.out.get(0))<nodeSize.get(node)){
									nodeSize.put(instance.out.get(0), nodeSize.get(node));
									this.expand(instance.out.get(0), nodeSize);
								}
								if(!nodeSize.containsKey(instance.in.get(0))||nodeSize.get(instance.in.get(0))<nodeSize.get(node)){
									nodeSize.put(instance.in.get(0), nodeSize.get(node));
									if(node!=instance.in.get(0)){
										this.expand(instance.in.get(0), nodeSize);
									}
								}
								if(!nodeSize.containsKey(instance.in.get(1))||nodeSize.get(instance.in.get(1))<nodeSize.get(node)){
									nodeSize.put(instance.in.get(1), nodeSize.get(node));
									if(node!=instance.in.get(1)){
										this.expand(instance.in.get(1), nodeSize);
									}
								}
								int size0=nodeSize.get(instance.in.get(0));
								int size1=nodeSize.get(instance.in.get(1));
								int sizeOut=nodeSize.get(instance.out.get(0));
								if(nodeSize.get(node)<size0){
									nodeSize.put(node, size0);
								}
								if(nodeSize.get(node)<size1){
									nodeSize.put(node, size1);
								}
								if(nodeSize.get(node)<sizeOut){
									nodeSize.put(node, sizeOut);
								}
								if(size0<nodeSize.get(node)){
									nodeSize.put(instance.in.get(0), nodeSize.get(node));
									if(node!=instance.in.get(0)){
										this.expand(instance.in.get(0), nodeSize);
									}
								}
								if (size1<nodeSize.get(node)){
									nodeSize.put(instance.in.get(1), nodeSize.get(node));
									if(node!=instance.in.get(1)){
										this.expand(instance.in.get(1), nodeSize);
									}
								}
								if(sizeOut<nodeSize.get(node)){
									nodeSize.put(instance.out.get(0), nodeSize.get(node));
									this.expand(instance.out.get(0), nodeSize);
								}
							}else{//the node is in of an instance different to NAND
								HashMap<Node, Integer> tempNodeSize = new HashMap<Node, Integer>();
								//expand outs
								for (int i = 0; i < instance.out.size(); i++) {
									if(!nodeSize.containsKey(instance.out.get(i))){
										this.expand(instance.out.get(i),nodeSize);
									}
									tempNodeSize.put(instance.definition.out.get(i),nodeSize.get(instance.out.get(i)));
								}
								//expand ins
								for (int i = 0; i < instance.in.size(); i++) {
									if(!nodeSize.containsKey(instance.in.get(i))){
										this.expand(instance.in.get(i),nodeSize);
									}
									tempNodeSize.put(instance.definition.in.get(i),nodeSize.get(instance.in.get(i)));
								}
								//expand definition
								for (int i = 0; i < instance.out.size(); i++) {
									instance.definition.expand(instance.definition.out.get(i), tempNodeSize);
								}
								//expand outs if needed
								for (int i = 0; i < instance.out.size(); i++) {
									if(nodeSize.get(instance.out.get(i))<tempNodeSize.get(instance.definition.out.get(i))){
										nodeSize.put(instance.out.get(i), tempNodeSize.get(instance.definition.out.get(i)));
										this.expand(instance.definition.out.get(i), tempNodeSize);
									}
								}
								//expand ins if needed
								for (int i = 0; i < instance.in.size(); i++) {
									if(nodeSize.get(instance.in.get(i))<tempNodeSize.get(instance.definition.in.get(i))){
										nodeSize.put(instance.in.get(i), tempNodeSize.get(instance.definition.in.get(i)));
										if(node!=instance.definition.in.get(i)){
											expand(instance.in.get(i),nodeSize);
										}
									}
								}
								
							}
						}
					}
				}
			}
			public Instance add(Definition def,Node ... nodes){
				for(Node node:nodes){
					this.add(node);	
				}
				Instance instance = new Instance();//node == instance of a definition
				if(def==this){
					def.recursiveInstances.add(instance);
				}else if(!def.recursiveInstances.isEmpty()||!def.instancesOfRecursiveDefinitions.isEmpty()){
					this.instancesOfRecursiveDefinitions.add(instance);
				}
				instance.in = new ArrayList<Node>(Arrays.asList(nodes).subList(0, def.in.size()));
				for(Node node:instance.in){
					node.inOfInstances.add(instance);
				}
				instance.out = new ArrayList<Node>(Arrays.asList(nodes).subList(def.in.size(), def.in.size()+def.out.size()));
				instance.definition=def;
				for (Node outNode:instance.out) {//nºinst outs = nºdef outs
					if(outNode==this.out.get(0)) def.rootIn.add(this);
					outNode.outOfInstance=instance;
				}
				this.instances.add(instance);
				return instance;
			}
			public void add(Node node){
				if(!this.nodes.contains(node)){
					node.idForDefinition=this.nodes.size();//debugging only
					node.definition=this;
					this.nodes.add(node);
					for(Node parent:node.parents){
						parent.definition=this;
						this.add(parent);
					}
					for(Node child:node.children){
						child.definition=this;
						this.add(child);
					}	
				}
			}
			public String toString() {
				String string = new String();
				//Print this definition translating node id to integers
				//TODO: subnodes
				this.tradInts = new HashMap<Node,Integer>();
				//System.out.print(this.hashCode());
				string+=this.name;
				string+=(" [");
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
			public boolean apply(Instance instance, Definition appliedDefinition) {
				//TODO: expand out of nodes
				//TODO: remove from out of nodes
				//TODO: replace only non outside referenced instances
				//apply definition with instance as root(0) of appliedDefinition
				// and replaces existing instances with instance to an existing applied definition
				//includes reverse references to verify instance can be safely erased in O(1)
				//-each tree from out must have at least one node in common (else two separated definitions) FOR NOW?
				//TODO:MAYBE: verify that all roots are expanded (if needed, +++cost)
				Instance expandingInstance=instance;
				Instance expandingAppliedInstance=null;
				HashSet<Instance> toExpand = new HashSet<Instance>();//instances to expand
				HashMap<Instance,Instance> instanceMap = new HashMap<Instance,Instance>();//map of expanded instances
				HashMap<Node,Node> nodeMap = new HashMap<Node,Node>();//map of expanded nodes
				if(appliedDefinition.out!=null&&!appliedDefinition.out.isEmpty()&&this.name!="nand"){//if applied definition is not nand//FIXME: only third term needed?//FIX nand checking
					expandingAppliedInstance=appliedDefinition.out.get(0).outOfInstance;
					instanceMap.put(expandingAppliedInstance,expandingInstance);
					toExpand.add(expandingAppliedInstance);
					while(!toExpand.isEmpty()){
						if(expandingInstance==null||expandingInstance.definition!=expandingAppliedInstance.definition){//instance must be to same definition and nºins and nºout must be equal
							return false;
						}else{
							for (int i = 0; i < expandingAppliedInstance.in.size(); i++){//expand all in nodes
								if(nodeMap.containsKey(expandingAppliedInstance.in.get(i))){//already added node
									if(nodeMap.get(expandingAppliedInstance.in.get(i))!=expandingInstance.in.get(i)){//added as different node (non unique)
										return false;
									}
								}else{//non-added node
									nodeMap.put(expandingAppliedInstance.in.get(i),expandingInstance.in.get(i));//expand node
									if(instanceMap.containsKey(expandingAppliedInstance.in.get(i).outOfInstance)){//already added instance
										if(instanceMap.get(expandingAppliedInstance.in.get(i).outOfInstance)!=expandingInstance.in.get(i).outOfInstance){//added as instance to different definition
											return false;
										}
									}else{//non-added instance
										if(expandingAppliedInstance.in.get(i).outOfInstance!=null){
											instanceMap.put(expandingAppliedInstance.in.get(i).outOfInstance,expandingInstance.in.get(i).outOfInstance);
											toExpand.add(expandingAppliedInstance.in.get(i).outOfInstance);
										}
									}
								}
							}
							for (int i = 0; i < expandingAppliedInstance.out.size(); i++){//expand all out nodes
								if(nodeMap.containsKey(expandingAppliedInstance.out.get(i))){//already added node
									if(nodeMap.get(expandingAppliedInstance.out.get(i))!=expandingInstance.out.get(i)){//added as different node (non unique)
										return false;
									}
								}else{//non-added node
									nodeMap.put(expandingAppliedInstance.out.get(i),expandingInstance.out.get(i));//expand node
								}
							}
							toExpand.remove(expandingAppliedInstance);
							if(toExpand.iterator().hasNext()){
								expandingAppliedInstance=toExpand.iterator().next();
								expandingInstance=instanceMap.get(expandingAppliedInstance);
							}
							
						}
						
					}
					if(appliedDefinition.instances.size()!=instanceMap.size()){
						System.out.println("Error applying, different number of instances. Parrallel path.");
						return false;
					}
					ArrayList<Node> inArray = new ArrayList<Node>();
					ArrayList<Node> outArray = new ArrayList<Node>();
					for (Node node : appliedDefinition.in) {
						inArray.add(nodeMap.get(node));
					}
					for (Node node : appliedDefinition.out) {
						Node outNode=nodeMap.get(node);
						if(outNode==this.out.get(0)){//if change of root update rootIn
							outNode.outOfInstance.definition.rootIn.remove(this);//previous instance definition remove rootIn(this) //TODO: CHANGE TO HASH
							if(!appliedDefinition.rootIn.contains(this)){//FIXME:needed because not Hash
								appliedDefinition.rootIn.add(this);//new node add rootIn
							}
							
						}
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
						this.instances.remove(removableInstance);//TODO:change to ¿list?HASH¿map? to remove in O(1) instead O(n)?
					}
				}
				return true;
			}
			public void write(ObjectOutputStream stream,
					HashMap<Definition, Integer> defMap) throws IOException {
				//FIXME:subnodes
				//TODO: use least space writing definitions-> optimize parameters as nº
				HashMap<Node, Integer> nodeMap = new HashMap<Node, Integer>();
				int nodeIndex=0;
				stream.write(this.in.size());
				for(Node node : this.in){
					nodeIndex=node.write(stream, nodeMap, nodeIndex);
				}
				stream.write(this.out.size());
				for(Node node : this.out){
					nodeIndex=node.write(stream, nodeMap, nodeIndex);
				}
				stream.write(this.instances.size());
				for(Instance instance : this.instances){
					nodeIndex=instance.write(stream,nodeMap,nodeIndex,defMap);//fix primitive int pass by value
				}
			}
			public void read(ObjectInputStream stream,
					HashMap<Integer, Definition> defMap) throws IOException {
				//TODO: use least space writing definitions-> optimize parameters as nº
				HashMap<Integer, Node> nodeMap = new HashMap<Integer, Node>();
				Node node = null;
				int keyNode;
				Instance instance = null;
				int size = stream.read();//nº in nodes
				for (int i = 0; i < size; i++) {//add in nodes
					keyNode = stream.read();
					node = new Node();
					node.read(stream, nodeMap);
					this.in.add(node);
					nodeMap.put(keyNode,node);
				}
				size = stream.read();//nº out nodes
				for (int i = 0; i < size; i++) {//add out nodes
					keyNode=stream.read();
					node = new Node();
					node.read(stream, nodeMap);
					this.out.add(node);
					nodeMap.put(keyNode,node);
				}
				size = stream.read();//nº of instances
				for (int i = 0; i < size; i++) {//add instances
					instance = new Instance();
					this.instances.add(instance);
					instance.read(stream,nodeMap,defMap);
				}
				//TODO: find root in
				if(this.out.get(0).outOfInstance!=null){
					this.out.get(0).outOfInstance.definition.rootIn.add(this);
				}
			}
			//NEEDED?
			public void eval(HashMap<Node, FixedBitSet> valueMap){
				//TODO: keep only needed values in memory
				if(this.out.get(0).outOfInstance==null){//this.out.get(0).instance==null <=> nand
					//NAND (always 2 ins 1 out)
					valueMap.put(this.out.get(0),valueMap.get(this.in.get(0)).nand(valueMap.get(this.in.get(1))));
//					System.out.println(FixedBitSet.toString(this.out.get(0).value));
				}else{
					for (int i = 0; i < this.out.size(); i++) {
						this.out.get(i).eval(valueMap);
					}
				}
				
			}
			public Object clone(){  
			    try{  
			        return super.clone();  
			    }catch(Exception e){ 
			        return null; 
			    }
			}
//			public void copy(Definition definition,HashMap<Node,Node> defToTempNodes){
//				for(Node node : definition.nodes){
//					Node tempNode;
//					if(defToTempNodes.containsKey(node)){
//						tempNode =defToTempNodes.get(node);
//					
//					}else{
//						tempNode = new Node();
//					}
//					defToTempNodes.put(node, tempNode);
//					this.nodes.add(tempNode);
//					tempNode.definition=this;
//					tempNode.copy(node,defToTempNodes);
//				}
//				for(Node node:definition.in){//copy definition in nodes
//					this.in.add(defToTempNodes.get(node));
//				}
//				for(Node node:definition.out){//copy definition out nodes
//					this.out.add(defToTempNodes.get(node));
//				}
//				for(Instance copiedInstance:definition.instances){
//					ArrayList<Node> nodes = new ArrayList<Node>();
//					for(Node node:copiedInstance.in){
//						nodes.add(defToTempNodes.get(node));
//					}
//					for(Node node:copiedInstance.out){
//						nodes.add(defToTempNodes.get(node));
//					}
//					if(copiedInstance.definition==definition){//recursive instance
//						this.add(this,nodes.toArray(new Node[nodes.size()]));
//					}else{
//						this.add(copiedInstance.definition,nodes.toArray(new Node[nodes.size()]));
//					}
//				}
//			}
			public void removeRecursion(AddedNodes addedNodes,HashSet<Instance> removedInstances) {
				this.instances.removeAll(this.recursiveInstances);//remove all recursive instances, to make the definition not self recursive
				for(Instance instance : this.recursiveInstances){//add nodes from recursive instances
					removedInstances.add(instance);
					addedNodes.in+=instance.out.size();
					for (int i = 0; i < instance.out.size(); i++) {//add out nodes to tempDef in
						instance.out.get(i).outOfInstance=null;
						this.in.add(instance.out.get(i));
					}
					addedNodes.out+=instance.in.size();
					for (int i = 0; i < instance.in.size(); i++) {//add in nodes to nand out
						instance.in.get(i).inOfInstances.remove(instance);
						this.out.add(instance.in.get(i));
					}
				}
				this.recursiveInstances.clear();
				this.instances.removeAll(this.instancesOfRecursiveDefinitions);
				HashSet<Instance> instances = new HashSet<Instance>();
				instances.addAll(this.instancesOfRecursiveDefinitions);
				this.instancesOfRecursiveDefinitions.clear();
				for(Instance instance:instances){//map all the instancesOfRecursiveDefinitions nodes
					HashMap<Node,Node> definitionToNewNodes = new HashMap<Node,Node>();
					for (int i = 0; i < instance.in.size(); i++) {//map in nodes
						definitionToNewNodes.put(instance.definition.in.get(i), instance.in.get(i));
						instance.in.get(i).inOfInstances.remove(instance);
						for(Node child:instance.definition.in.get(i).children){//FIXME:recursive nodes
							Node newChild= new Node();
							instance.in.get(i).add(newChild);
							definitionToNewNodes.put(child, newChild);
						}
					}
					for (int i = 0; i < instance.out.size(); i++) {//map out nodes
						definitionToNewNodes.put(instance.definition.out.get(i), instance.out.get(i));
						instance.out.get(i).outOfInstance=null;
						for(Node parent:instance.definition.out.get(i).parents){//FIXME:recursive nodes
							Node newParent= new Node();
							newParent.add(instance.out.get(i));
							definitionToNewNodes.put(parent, newParent);
						}
					}
					for(Instance definitionInstance:instance.definition.instances){
							ArrayList<Node> nodes = new ArrayList<Node>();
							for (Node node: definitionInstance.in) {//map in nodes
								if(definitionToNewNodes.containsKey(node)){
									nodes.add(definitionToNewNodes.get(node));
								}else{
									Node newNode = new Node();
									definitionToNewNodes.put(node, newNode);
									nodes.add(newNode);
									for(Node parent:node.parents){//map parent nodes //think don't need to map children
										if(definitionToNewNodes.containsKey(parent)){
											definitionToNewNodes.get(parent).add(definitionToNewNodes.get(node));
										}else{
											Node newParent = new Node();
											newParent.add(definitionToNewNodes.get(node));
											definitionToNewNodes.put(parent, newParent);
										}
									}
								}
								
							}
							for (Node node: definitionInstance.out) {//map out nodes
								if(definitionToNewNodes.containsKey(node)){
									nodes.add(definitionToNewNodes.get(node));
								}else{
									Node defNode = new Node();
									definitionToNewNodes.put(node, defNode);
									nodes.add(defNode);
									for(Node parent:node.parents){//map parent nodes //think don't need to map children
										if(definitionToNewNodes.containsKey(parent)){
											definitionToNewNodes.get(parent).add(definitionToNewNodes.get(node));
										}else{
											Node newParent = new Node();
											newParent.add(definitionToNewNodes.get(node));
											definitionToNewNodes.put(parent, newParent);
										}
									}
								}
							}
							Instance newInstance=this.add(definitionInstance.definition,nodes.toArray(new Node[nodes.size()]));
							if(definitionInstance.definition==instance.definition){
								this.recursiveInstances.add(newInstance);
								this.instancesOfRecursiveDefinitions.remove(newInstance);
							}
					}
				}
				if(!this.recursiveInstances.isEmpty()||!this.instancesOfRecursiveDefinitions.isEmpty()){
					this.removeRecursion(addedNodes, removedInstances);
				}
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
			public void remove(Instance instance) {
				// TODO Auto-generated method stub
				
			}
		}
