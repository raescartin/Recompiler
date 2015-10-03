/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import utils.AddedNodes;
import utils.FixedBitSet;

//DESCRIPTION
//-Defines a "Definition", procedure, function with multiple outputs
//-Recursive definition:
//	#A definition is composed by instances of definitions
//	#Nand is the basic definition
//-Definitions may be recursive and ARE TURING COMPLETE
//PRE: all inputs(and in consequence outpus) of a nand definition must be of the same size


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
			public int maxNode=0;//keep track for easy consistency while removing nodes
			public String name;//not needed, only for debug
			public HashMap<Node,Integer> tradInts;//not needed, only for debug
			public ArrayList<Node> in;//NEEDED
			public ArrayList<Node> out;//NEEDED
			public ArrayList<Instance> instances;//TODO: better data structure Hash AND list linkedhashmap?  - replaces def?
			public ArrayList<Definition> rootIn;//TODO: better data structure Hash AND list linkedhashmap? - verify definitions are in DB
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
				HashSet <Node> inOutNodes = new HashSet <Node>();
				//NODE FISSION
				this.getNodesSize(nodeSize);//get the size of all the definition nodes//FIXME not ok sizes with dec
				this.removeRedundantSubnodes(nodeSize);
				///Need to map subnodes of ins and outs to conserve references!!!
				this.mapIns(nodeSize,nodeToNands,nandForest,nandToNodeIn,inOutNodes);//from bottom to top//(mapping in and finding inputs size first is needed)
				this.mapOuts(nodeSize,nodeToNands,nandForest,nandToNodeOut,inOutNodes);
				//IN and OUTS mapped and in nandForest
				this.instances.clear();
				this.rootIn.clear();
				this.nodes.retainAll(inOutNodes);
				for(Node inNode:nandToNodeIn){
					inNode.children.clear();
					inNode.inOfInstances.clear();
				}
				for(Node outNode:nandToNodeOut){
					outNode.parents.clear();
					outNode.outOfInstance=null;
				}
				return nandForest;
			}
			private void removeRedundantSubnodes(HashMap<Node, Integer> nodeSize) {
				//remove redundant subnodes
				for(Node node:this.nodes){
					if(node.parents.size()==1){
						if(node.parents.get(0).parents.size()>1){//FIXME: needed?
							Node redundantNodeLeft = node.parents.get(0).children.get(0);//FIXME:not only extreme nodes ¿recursive?
							Node nodeLeft= node.parents.get(0).parents.get(0);
							if(nodeSize.get(nodeLeft)==1){
								for(Node child:redundantNodeLeft.children){
									Collections.replaceAll(child.parents,redundantNodeLeft,nodeLeft);
								}
								for(Instance instance:this.instances){
									Collections.replaceAll(instance.in,redundantNodeLeft,nodeLeft);
									Collections.replaceAll(instance.out,redundantNodeLeft,nodeLeft);
								}
							}
							Node redundantNodeRight = node.parents.get(0).children.get(node.parents.get(0).children.size()-1);
							Node nodeRight=node.parents.get(0).parents.get(node.parents.get(0).parents.size()-1);
							if(nodeSize.get(nodeRight)==1){
								for(Node child:redundantNodeRight.children){
									Collections.replaceAll(child.parents,redundantNodeRight,nodeRight);
								}
								for(Instance instance:this.instances){
									Collections.replaceAll(instance.in,redundantNodeRight,nodeRight);
									Collections.replaceAll(instance.out,redundantNodeRight,nodeRight);
								}
							}
							if(node.parents.get(0).children.size()==3){//TODO: recursive and for more than 3
								if(node.parents.get(0).parents.size()==3){
									Node redundantCenter = node.parents.get(0).children.get(1);
									Node nodeCenter=node.parents.get(0).parents.get(1);
									for(Node child:redundantCenter.children){
										Collections.replaceAll(child.parents,redundantCenter,nodeCenter);
									}
									for(Instance instance:this.instances){
										Collections.replaceAll(instance.in,redundantCenter,nodeCenter);
										Collections.replaceAll(instance.out,redundantCenter,nodeCenter);
									}
								}
							}
						}
					}
				}
				
			}
			private void mapIns(HashMap<Node, Integer> nodeSize, HashMap<Node, ArrayList<NandNode>> nodeToNands, NandForest nandForest, ArrayList<Node> nandToNodeIn, HashSet<Node> inOutNodes) {
				//map input nodes to nandNodes
				for(Node inNode:this.in){
					inNode.mapInChildren(nodeSize,nodeToNands,nandForest, nandToNodeIn,inOutNodes);
				}			
			}
			private void mapOuts(HashMap<Node, Integer> nodeSize,
					HashMap<Node, ArrayList<NandNode>> nodeToNands,
					NandForest nandForest, ArrayList<Node> nandToNodeOut, HashSet<Node> inOutNodes) {
				//map output nodes to nandNodes
				for(Node outNode:this.out){
					outNode.mapOutParents(nodeSize,nodeToNands,nandForest, nandToNodeOut,inOutNodes);
				}	
				
			}
			private void getNodesSize(HashMap<Node, Integer> nodeSize) {
				HashSet<Node> fixedSize1Nodes = new HashSet<Node>();
				for(Node node: this.out){
					this.getSize1Nodes(node,fixedSize1Nodes);
				}
				for(Node node: this.out){
					this.expand(node,nodeSize,fixedSize1Nodes);
				}
				
			}
			private void getSize1Nodes(Node node, HashSet<Node> fixedSize1Nodes) {
				if(node.parents.size()==1){
					if(node.parents.get(0).children.get(0)==node||node.parents.get(0).children.get(node.parents.get(0).children.size()-1)==node){
						fixedSize1Nodes.add(node);
					}
				}
				for(Node parent:node.parents){
					this.getSize1Nodes(parent, fixedSize1Nodes);
				}
				if(node.outOfInstance!=null){//the node is out of instance
					if(node.outOfInstance.definition.name=="nand"){//NAND //TODO: fix nand checking
						//this is out
						//expand fixed size 1 nodes
						if(fixedSize1Nodes.contains(node)||fixedSize1Nodes.contains(node.outOfInstance.in.get(0))||fixedSize1Nodes.contains(node.outOfInstance.in.get(1))){
							fixedSize1Nodes.add(node);
							fixedSize1Nodes.add(node.outOfInstance.in.get(0));
							fixedSize1Nodes.add(node.outOfInstance.in.get(1));
						}
						this.getSize1Nodes(node.outOfInstance.in.get(0),fixedSize1Nodes);
						this.getSize1Nodes(node.outOfInstance.in.get(1),fixedSize1Nodes);
						if(fixedSize1Nodes.contains(node)||fixedSize1Nodes.contains(node.outOfInstance.in.get(0))||fixedSize1Nodes.contains(node.outOfInstance.in.get(1))){
							fixedSize1Nodes.add(node);
							fixedSize1Nodes.add(node.outOfInstance.in.get(0));
							fixedSize1Nodes.add(node.outOfInstance.in.get(1));
						}
					}else{//the node is out of an instance different to NAND
						if(this.instances.contains(node.outOfInstance)){//check definition has not been removed (= is recursive)
							HashSet<Node> tempFixedSize1Nodes = new HashSet<Node>();
							//map outs to instance
							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
								if(fixedSize1Nodes.contains(node.outOfInstance.out.get(i))){
									tempFixedSize1Nodes.add(node.outOfInstance.definition.out.get(i));
								}
							}
							//expand instance
							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
								node.outOfInstance.definition.getSize1Nodes(node.outOfInstance.definition.out.get(i), tempFixedSize1Nodes);
							}
							//map ins from instance
							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
								if(tempFixedSize1Nodes.contains(node.outOfInstance.definition.in.get(i))){
									fixedSize1Nodes.add(node.outOfInstance.in.get(i));
								}
							}
							//expand ins
							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
								this.getSize1Nodes(node.outOfInstance.in.get(i),fixedSize1Nodes);
							}
							//map ins to instance
							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
								if(fixedSize1Nodes.contains(node.outOfInstance.in.get(i))){
									tempFixedSize1Nodes.add(node.outOfInstance.definition.in.get(i));
								}
							}
							//expand instance
							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
								node.outOfInstance.definition.getSize1Nodes(node.outOfInstance.definition.out.get(i), tempFixedSize1Nodes);
							}
							//expand outs from instance
							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
								if(tempFixedSize1Nodes.contains(node.outOfInstance.definition.out.get(i))){
									fixedSize1Nodes.add(node.outOfInstance.out.get(i));
								}
							}
						}
					}
				}
				
			}
			private void expand(Node node, HashMap<Node, Integer> nodeSize, HashSet<Node> fixedSize1Nodes) {
				//expand up, then expand down if needed
				if (!nodeSize.containsKey(node)){
					nodeSize.put(node, 1);//size 1 if not subnodes nor mapped
					if (nodeSize.get(node)<node.parents.size()) nodeSize.put(node, node.parents.size());
				}
				
				//expand ins
				int parentsSize=0;
				for(Node parent:node.parents){
					if(!nodeSize.containsKey(parent)){
						expand(parent,nodeSize, fixedSize1Nodes);
					}
					if(node.parents.size()!=1){
						parentsSize+=nodeSize.get(parent);
					}
				}
				if(nodeSize.get(node)<parentsSize){
					nodeSize.put(node, parentsSize);
				}
				//expand outs
				int childrenSize=0;
				for(Node child:node.children){
					if(!nodeSize.containsKey(child)){
						expand(child,nodeSize, fixedSize1Nodes);
					}
					if(child.parents.size()==1){
						childrenSize+=nodeSize.get(child);	
					}
				}
				if(nodeSize.get(node)<childrenSize){
					nodeSize.put(node, childrenSize);
				}
				if(node.parents.size()>1&&nodeSize.get(node)>parentsSize){
					if(!fixedSize1Nodes.contains(node.parents.get(0))){
						nodeSize.put(node.parents.get(0),(childrenSize-parentsSize)/2+3);//minimum size for a node divided in subnodes is 3
					}
					if(!fixedSize1Nodes.contains(node.parents.get(node.parents.size()-1))){
						nodeSize.put(node.parents.get(node.parents.size()-1),(childrenSize-parentsSize)/2+3);//minimum size for a node divided in subnodes is 3
					}
					if(!fixedSize1Nodes.contains(node.parents.get(0))){
						expand(node.parents.get(0),nodeSize, fixedSize1Nodes);
					}
					if(!fixedSize1Nodes.contains(node.parents.get(node.parents.size()-1))){
						expand(node.parents.get(node.parents.size()-1),nodeSize, fixedSize1Nodes);
					}
					//expand ins
					parentsSize=0;
					for(Node parent:node.parents){
						if(!nodeSize.containsKey(parent)){
							expand(parent,nodeSize, fixedSize1Nodes);
						}
						if(node.parents.size()!=1){
							parentsSize+=nodeSize.get(parent);
						}
					}
					if(nodeSize.get(node)<parentsSize){
						nodeSize.put(node, parentsSize);
					}
				}
				if(node.childrenAreSubnodes()){
					nodeSize.put(node.children.get(node.children.size()/2), nodeSize.get(node)-childrenSize+nodeSize.get(node.children.get(node.children.size()/2)));
					expand(node.children.get(node.children.size()/2),nodeSize, fixedSize1Nodes);
				}
				if(node.outOfInstance!=null){//the node is out of instance
					if(node.outOfInstance.definition.name=="nand"){//NAND //TODO: fix nand checking
						//this is out
						//expand ins
						if(!nodeSize.containsKey(node.outOfInstance.in.get(0))||nodeSize.get(node.outOfInstance.in.get(0))<nodeSize.get(node)){
							nodeSize.put(node.outOfInstance.in.get(0), nodeSize.get(node));
							this.expand(node.outOfInstance.in.get(0), nodeSize, fixedSize1Nodes);
						}
						if(!nodeSize.containsKey(node.outOfInstance.in.get(1))||nodeSize.get(node.outOfInstance.in.get(1))<nodeSize.get(node)){
							nodeSize.put(node.outOfInstance.in.get(1), nodeSize.get(node));
							this.expand(node.outOfInstance.in.get(1), nodeSize, fixedSize1Nodes);
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
							this.expand(node.outOfInstance.in.get(0), nodeSize, fixedSize1Nodes);
						}
						if (size1<nodeSize.get(node)){
							nodeSize.put(node.outOfInstance.in.get(1), nodeSize.get(node));
							this.expand(node.outOfInstance.in.get(1), nodeSize, fixedSize1Nodes);
						}
					}else{//the node is out of an instance different to NAND
						if(this.instances.contains(node.outOfInstance)){//check definition has not been removed (= is recursive)
							HashMap<Node, Integer> tempNodeSize = new HashMap<Node, Integer>();
							//expand outs
							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
								if(!nodeSize.containsKey(node.outOfInstance.out.get(i))){
									this.expand(node.outOfInstance.out.get(i),nodeSize, fixedSize1Nodes);
								}
								tempNodeSize.put(node.outOfInstance.definition.out.get(i),nodeSize.get(node.outOfInstance.out.get(i)));
							}
							//expand ins
							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
								if(!nodeSize.containsKey(node.outOfInstance.in.get(i))){
									this.expand(node.outOfInstance.in.get(i),nodeSize, fixedSize1Nodes);
								}
								tempNodeSize.put(node.outOfInstance.definition.in.get(i),nodeSize.get(node.outOfInstance.in.get(i)));
							}
						//expand definition
						for (int i = 0; i < node.outOfInstance.out.size(); i++) {
							node.outOfInstance.definition.expand(node.outOfInstance.definition.out.get(i), tempNodeSize, fixedSize1Nodes);
						}
						//expand outs if needed
						for (int i = 0; i < node.outOfInstance.out.size(); i++) {
							if(nodeSize.get(node.outOfInstance.out.get(i))<tempNodeSize.get(node.outOfInstance.definition.out.get(i))){
								nodeSize.put(node.outOfInstance.out.get(i), tempNodeSize.get(node.outOfInstance.definition.out.get(i)));
								if(node!=node.outOfInstance.definition.out.get(i)){
									this.expand(node.outOfInstance.definition.out.get(i), tempNodeSize, fixedSize1Nodes);
								}
							}
						}
						//expand ins if needed
						for (int i = 0; i < node.outOfInstance.in.size(); i++) {
							if(nodeSize.get(node.outOfInstance.in.get(i))<tempNodeSize.get(node.outOfInstance.definition.in.get(i))){
								nodeSize.put(node.outOfInstance.in.get(i), tempNodeSize.get(node.outOfInstance.definition.in.get(i)));
								expand(node.outOfInstance.in.get(i),nodeSize, fixedSize1Nodes);
							}
						}
						
					}
						
					}
					if(node.inOfInstances!=null){//the node is in of instance(s)
						for(Instance instance:node.inOfInstances){
							if(this.instances.contains(instance)){//check definition has not been removed (= is recursive)
								if(instance.definition.name=="nand"){//NAND //TODO: fix nand checking
									if(!nodeSize.containsKey(instance.out.get(0))||nodeSize.get(instance.out.get(0))<nodeSize.get(node)){
										nodeSize.put(instance.out.get(0), nodeSize.get(node));
										this.expand(instance.out.get(0), nodeSize, fixedSize1Nodes);
									}
									if(!nodeSize.containsKey(instance.in.get(0))||nodeSize.get(instance.in.get(0))<nodeSize.get(node)){
										nodeSize.put(instance.in.get(0), nodeSize.get(node));
										if(node!=instance.in.get(0)){
											this.expand(instance.in.get(0), nodeSize, fixedSize1Nodes);
										}
									}
									if(!nodeSize.containsKey(instance.in.get(1))||nodeSize.get(instance.in.get(1))<nodeSize.get(node)){
										nodeSize.put(instance.in.get(1), nodeSize.get(node));
										if(node!=instance.in.get(1)){
											this.expand(instance.in.get(1), nodeSize, fixedSize1Nodes);
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
											this.expand(instance.in.get(0), nodeSize, fixedSize1Nodes);
										}
									}
									if (size1<nodeSize.get(node)){
										nodeSize.put(instance.in.get(1), nodeSize.get(node));
										if(node!=instance.in.get(1)){
											this.expand(instance.in.get(1), nodeSize, fixedSize1Nodes);
										}
									}
									if(sizeOut<nodeSize.get(node)){
										nodeSize.put(instance.out.get(0), nodeSize.get(node));
										this.expand(instance.out.get(0), nodeSize, fixedSize1Nodes);
									}
								}else{//the node is in of an instance different to NAND
									HashMap<Node, Integer> tempNodeSize = new HashMap<Node, Integer>();
									//expand outs
									for (int i = 0; i < instance.out.size(); i++) {
										if(!nodeSize.containsKey(instance.out.get(i))){
											this.expand(instance.out.get(i),nodeSize, fixedSize1Nodes);
										}
										tempNodeSize.put(instance.definition.out.get(i),nodeSize.get(instance.out.get(i)));
									}
									//expand ins
									for (int i = 0; i < instance.in.size(); i++) {
										if(!nodeSize.containsKey(instance.in.get(i))){
											this.expand(instance.in.get(i),nodeSize, fixedSize1Nodes);
										}
										tempNodeSize.put(instance.definition.in.get(i),nodeSize.get(instance.in.get(i)));
									}
									//expand definition
									for (int i = 0; i < instance.out.size(); i++) {
										instance.definition.expand(instance.definition.out.get(i), tempNodeSize, fixedSize1Nodes);
									}
									//expand outs if needed
									for (int i = 0; i < instance.out.size(); i++) {
										if(nodeSize.get(instance.out.get(i))<tempNodeSize.get(instance.definition.out.get(i))){
											nodeSize.put(instance.out.get(i), tempNodeSize.get(instance.definition.out.get(i)));
											this.expand(instance.definition.out.get(i), tempNodeSize, fixedSize1Nodes);
										}
									}
									//expand ins if needed
									for (int i = 0; i < instance.in.size(); i++) {
										if(nodeSize.get(instance.in.get(i))<tempNodeSize.get(instance.definition.in.get(i))){
											nodeSize.put(instance.in.get(i), tempNodeSize.get(instance.definition.in.get(i)));
											if(node!=instance.definition.in.get(i)){
												expand(instance.in.get(i),nodeSize, fixedSize1Nodes);
											}
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
			public void printEval(String ... strings){
				
				HashMap<Node, FixedBitSet> valueMap = new HashMap<Node, FixedBitSet>() ;
				
				for(int i=0;i<this.in.size();i++){
					valueMap.put(this.in.get(i), FixedBitSet.fromString(strings[i]));
				}
				this.eval(valueMap);
				ArrayList<String> ins = new ArrayList<String>();
				ArrayList<String> outs = new ArrayList<String>();
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
			public void eval(HashMap<Node, FixedBitSet> valueMap){
				//eval first non-recursive instances, second self recursion
				if(this.in.size()==2&&this.out.size()==1&&valueMap.containsKey(this.in.get(0))&&valueMap.get(this.in.get(0)).length()==0&&valueMap.containsKey(this.in.get(1))){
						valueMap. put(this.out.get(0),valueMap.get(this.in.get(1)));
				}else if(this.in.size()==2&&this.out.size()==1&&valueMap.containsKey(this.in.get(1))&&valueMap.get(this.in.get(1)).length()==0&&valueMap.containsKey(this.in.get(0))){
						valueMap. put(this.out.get(0),valueMap.get(this.in.get(0)));
				}else if(this.name=="nand"){//NAND //TODO: fix nand checking
					//NAND (always 2 ins 1 out)
					if(valueMap.containsKey(this.in.get(1))//FIXME: If the non-evaluated node ends being empty, the result couldn't be precalculated
						&&valueMap.get(this.in.get(1)).length()!=0&&valueMap.get(this.in.get(1)).cardinality()==0){//one input not mapped
							ArrayList<String> ones = new ArrayList<String>();
							for(int i=0;i<valueMap.get(this.in.get(1)).length();i++){
								ones.add("1");
							}
							valueMap.put(this.out.get(0), FixedBitSet.fromString(String.join(", ", ones)));
					}else if(valueMap.containsKey(this.in.get(0))
						&&valueMap.get(this.in.get(0)).length()!=0&&valueMap.get(this.in.get(0)).cardinality()==0){//one input not mapped
							ArrayList<String> ones = new ArrayList<String>();
							for(int i=0;i<valueMap.get(this.in.get(0)).length();i++){
								ones.add("1");
							}
							valueMap.put(this.out.get(0), FixedBitSet.fromString(String.join(", ", ones)));
					}else{
						valueMap.put(this.out.get(0),valueMap.get(this.in.get(0)).nand(valueMap.get(this.in.get(1))));
					}
//					System.out.println(FixedBitSet.toString(this.out.get(0).value));
				}else{
					HashSet<Instance> recursiveInstances = new HashSet<Instance>();
					HashSet<Instance> instancesToExpand = new HashSet<Instance>();
					boolean outs;
					do{
						outs=true;
						for(Node nodeOut: this.out){
							nodeOut.eval(valueMap,recursiveInstances,instancesToExpand);
							if(!valueMap.containsKey(nodeOut)){
								outs=false;
							}
						}
						instancesToExpand.addAll(recursiveInstances);
						recursiveInstances.clear();
					}while(!outs&&!instancesToExpand.isEmpty());
					
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
					//TODO:
					//!!!TO OPTIMIZE RECUSIVE INTERSECTION!!!
					//1 add instances of 1st recursion (expand recursion like its done with instancesOfRecursiveDefinitions)
					//2 map out/in recursive nodes
					//3 keep track of these nodes
					//4 create new definition of the recursive part without intersections (def=x,defRwithoutIntersections,y defRwithoutIntersections=w,defRwithoutIntersections,z)
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
				this.instances.remove(instance);
				
			}
			public void nodeFusion() {
				HashMap<Definition,ArrayList<Instance>> instancesByDefinition = new HashMap<Definition,ArrayList<Instance>>();
				for(Instance instance:this.instances){
					boolean fusible=true;
					for(Node inNode:instance.in){
						if(inNode.parents.size()!=1){
							fusible=false;
						}
					}
					for(Node outNode:instance.out){
						if(outNode.children.size()!=1){
							fusible=false;
						}
					}
					if(fusible){
						ArrayList<Instance> instancesOfDefinition = new ArrayList<Instance>();
						if(instancesByDefinition.containsKey(instance.definition)){
							instancesOfDefinition=instancesByDefinition.get(instance.definition);
						}
						instancesOfDefinition.add(instance);
						instancesByDefinition.put(instance.definition, instancesOfDefinition);
					}
				}
				HashMap<Node,ArrayList<Instance>> instancesByNode = new HashMap<Node,ArrayList<Instance>>();
				for(Definition definition:instancesByDefinition.keySet()){
					for(Instance instance:instancesByDefinition.get(definition)){
						if(instance.in.get(0).parents.size()==1){
							ArrayList<Instance> instancesWithNode = new ArrayList<Instance>();
							if(instancesByNode.containsKey(instance.in.get(0))){
								instancesWithNode = instancesByNode.get(instance.in.get(0));
							}
							instancesWithNode.add(instance);
							instancesByNode.put(instance.in.get(0).parents.get(0), instancesWithNode);
						}
						
					}
				}
				for(Node node:instancesByNode.keySet()){//Heuristic
					ArrayList<Instance> instances=instancesByNode.get(node);
					for(Instance instance:instances){
						Instance superInstance = new Instance();
						superInstance.definition=instance.definition;
						for(Node inNode:instance.in){
							superInstance.in.add(inNode.parents.get(0));
						}
						for(Node outNode:instance.out){
							superInstance.out.add(outNode.children.get(0));
						}
						ArrayList<Instance> candidateInstances = new ArrayList<Instance>();
						for(Instance candidateInstance:instancesByNode.get(node)){
							boolean candidate=true;
							for (int i = 0; i < superInstance.in.size(); i++) {
								if(candidateInstance.in.get(i).parents.get(0)!=superInstance.in.get(i)){
									candidate=false;
								}
							}
							for (int i = 0; i < superInstance.out.size(); i++) {
								if(candidateInstance.out.get(i).children.get(0)!=superInstance.out.get(i)){
									candidate=false;
								}
							}
							if(candidate){
								candidateInstances.add(candidateInstance);
								instances.remove(candidate);
							}
							
						}
						//if we have all subinstances to this superinstance, replace subinstances with superinstance (fusion)
						if(candidateInstances.size()==node.children.size()){
							ArrayList<Node> nodes = new ArrayList<Node>();
							nodes.addAll(superInstance.in);
							nodes.addAll(superInstance.out);
							this.add(superInstance.definition, (Node[]) nodes.toArray());
							for(Instance candidateInstance:candidateInstances){
								this.remove(candidateInstance);
							}
						}
						
						
					}
				}
					
			}
		}
