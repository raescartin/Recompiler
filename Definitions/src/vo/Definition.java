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
				//INITIALIZE VARIABLES
				this.in = new ArrayList<Node>();
				this.out = new ArrayList<Node>();
				this.instances = new ArrayList<Instance>();
				this.recursiveInstances = new HashSet<Instance>();
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
				this.recursiveInstances = new HashSet<Instance>();
				this.instancesOfRecursiveDefinitions = new HashSet<Instance>();
				this.rootIn = new ArrayList<Definition>();
				//DEBUGGING ONLY
				this.nodes = new HashSet<Node> ();
				//END OF DEBUGGING ONLY
			}
			public NandForest toNandForest(ArrayList<Node> nandToNodeIn, ArrayList<Node> nandToNodeOut){
				//PRE: this definition is not recursive and doesn't contain recursive definitions
				// the nodes have been split to the minimum needed 
				//POST: returns a NandForest equivalent to this definition, map of in and map of out nandnodes to nodes
				//more efficient thanks to HashMap's of unique nodes
				//TOPDOWN OR DOWNUP? DOWNUP less branches, UPDOWN less memory? -> DOWNUP needed to optimize and instance
				NandForest nandForest = new NandForest(0);
				HashMap<Node, NandNode> nodeToNand = new HashMap<Node, NandNode>();
				HashSet <Node> inOutNodes = new HashSet <Node>();
				///Need to map subnodes of ins and outs to conserve references!!!
				this.mapIns(nodeToNand,nandForest,nandToNodeIn,inOutNodes);
				this.mapOuts(nodeToNand,nandForest,nandToNodeOut,inOutNodes);
				//IN and OUTS mapped and in nandForest
				this.instances.clear();
				this.rootIn.clear();
				this.nodes.retainAll(inOutNodes);
				for(Node inNode:nandToNodeIn){
//					inNode.children.clear();//children shouldn't be erased to preserve consistency TODO:can this cause a problem?
				}
				for(Node outNode:nandToNodeOut){
					outNode.parents.clear();
					outNode.outOfInstance=null;
				}
				return nandForest;
			}
//			private void removeRedundantSubnodes(HashMap<Node, Integer> nodeSize) {
//				//remove redundant subnodes
//				//A(B&C){0}==B{0}
//				//A(B&C){2}==C{2}
//				for(Node node:this.nodes){
//					if(node.parents.size()==1){
//						if(node.parents.get(0).parents.size()>1){//FIXME: needed?
//							Node redundantNodeLeft = node.parents.get(0).children.get(0);//FIXME:not only extreme nodes ¿recursive?
//							Node nodeLeft= node.parents.get(0).parents.get(0);
//							if(nodeSize.get(nodeLeft)==1){
//								for(Node child:redundantNodeLeft.children){
//									Collections.replaceAll(child.parents,redundantNodeLeft,nodeLeft);
//								}
//								for(Instance instance:this.instances){
//									Collections.replaceAll(instance.in,redundantNodeLeft,nodeLeft);
//									Collections.replaceAll(instance.out,redundantNodeLeft,nodeLeft);
//								}
//							}
//							Node redundantNodeRight = node.parents.get(0).children.get(node.parents.get(0).children.size()-1);
//							Node nodeRight=node.parents.get(0).parents.get(node.parents.get(0).parents.size()-1);
//							if(nodeSize.get(nodeRight)==1){
//								for(Node child:redundantNodeRight.children){
//									Collections.replaceAll(child.parents,redundantNodeRight,nodeRight);
//								}
//								for(Instance instance:this.instances){
//									Collections.replaceAll(instance.in,redundantNodeRight,nodeRight);
//									Collections.replaceAll(instance.out,redundantNodeRight,nodeRight);
//								}
//							}
//							if(node.parents.get(0).children.size()==3){//TODO: recursive
//								if(node.parents.get(0).parents.size()==3){
//									Node redundantCenter = node.parents.get(0).children.get(1);
//									Node nodeCenter=node.parents.get(0).parents.get(1);
//									for(Node child:redundantCenter.children){
//										Collections.replaceAll(child.parents,redundantCenter,nodeCenter);
//									}
//									for(Instance instance:this.instances){
//										Collections.replaceAll(instance.in,redundantCenter,nodeCenter);
//										Collections.replaceAll(instance.out,redundantCenter,nodeCenter);
//									}
//								}
//							}
//						}
//					}
//				}
//				
//			}
			private void mapIns(HashMap<Node, NandNode> nodeToNand, NandForest nandForest, ArrayList<Node> nandToNodeIn, HashSet<Node> inOutNodes) {
				//map input nodes to nandNodes
				HashSet<Node> inNodes = new HashSet<Node>();
				inNodes.addAll(this.in);
				for(Node outNode:this.out){
					outNode.findIns(inNodes,nodeToNand,nandForest, nandToNodeIn,inOutNodes);
				}	
//				for(Node inNode:this.in){
//					inNode.mapInChildren(false, nodeToNand,nandForest, nandToNodeIn,inOutNodes);
//				}	
			}
			private void mapOuts(HashMap<Node, NandNode> nodeToNands,
					NandForest nandForest, ArrayList<Node> nandToNodeOut, HashSet<Node> inOutNodes) {
				//map output nodes to nandNodes
				for(Node outNode:this.out){
					outNode.mapOutParents(nodeToNands,nandForest, nandToNodeOut,inOutNodes);
				}	
				
			}
//			private void getNodesSize(HashMap<Node, Integer> nodeSize) {
//				HashSet<Node> fixedSize1Nodes = new HashSet<Node>();
//				for(Node node: this.out){
//					this.getSize1Nodes(node,fixedSize1Nodes);
//				}
//				for(Node node: this.out){
//					this.expand(node,nodeSize,fixedSize1Nodes);
//				}
//				
//			}
//			private void getSize1Nodes(Node node, HashSet<Node> fixedSize1Nodes) {
//				if(node.parents.size()==1){
//					if(node.parents.get(0).children.get(0)==node||node.parents.get(0).children.get(node.parents.get(0).children.size()-1)==node){
//						fixedSize1Nodes.add(node);
//					}
//				}
//				for(Node parent:node.parents){
//					this.getSize1Nodes(parent, fixedSize1Nodes);
//				}
//				if(node.outOfInstance!=null){//the node is out of instance
//					if(node.outOfInstance.definition.name=="nand"){//NAND //TODO: fix nand checking
//						//this is out
//						//expand fixed size 1 nodes
//						if(fixedSize1Nodes.contains(node)||fixedSize1Nodes.contains(node.outOfInstance.in.get(0))||fixedSize1Nodes.contains(node.outOfInstance.in.get(1))){
//							fixedSize1Nodes.add(node);
//							fixedSize1Nodes.add(node.outOfInstance.in.get(0));
//							fixedSize1Nodes.add(node.outOfInstance.in.get(1));
//						}
//						this.getSize1Nodes(node.outOfInstance.in.get(0),fixedSize1Nodes);
//						this.getSize1Nodes(node.outOfInstance.in.get(1),fixedSize1Nodes);
//						if(fixedSize1Nodes.contains(node)||fixedSize1Nodes.contains(node.outOfInstance.in.get(0))||fixedSize1Nodes.contains(node.outOfInstance.in.get(1))){
//							fixedSize1Nodes.add(node);
//							fixedSize1Nodes.add(node.outOfInstance.in.get(0));
//							fixedSize1Nodes.add(node.outOfInstance.in.get(1));
//						}
//					}else{//the node is out of an instance different to NAND
//						if(this.instances.contains(node.outOfInstance)){//check definition has not been removed (= is not recursive)
//							HashSet<Node> tempFixedSize1Nodes = new HashSet<Node>(); 
//							//map outs to instance
//							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
//								if(fixedSize1Nodes.contains(node.outOfInstance.out.get(i))){
//									tempFixedSize1Nodes.add(node.outOfInstance.definition.out.get(i));
//								}
//							}
//							//expand instance
//							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
//								node.outOfInstance.definition.getSize1Nodes(node.outOfInstance.definition.out.get(i), tempFixedSize1Nodes);
//							}
//							//map ins from instance
//							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
//								if(tempFixedSize1Nodes.contains(node.outOfInstance.definition.in.get(i))){
//									fixedSize1Nodes.add(node.outOfInstance.in.get(i));
//								}
//							}
//							//expand ins
//							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
//								this.getSize1Nodes(node.outOfInstance.in.get(i),fixedSize1Nodes);
//							}
//							//map ins to instance
//							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
//								if(fixedSize1Nodes.contains(node.outOfInstance.in.get(i))){
//									tempFixedSize1Nodes.add(node.outOfInstance.definition.in.get(i));
//								}
//							}
//							//expand instance
//							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
//								node.outOfInstance.definition.getSize1Nodes(node.outOfInstance.definition.out.get(i), tempFixedSize1Nodes);
//							}
//							//expand outs from instance
//							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
//								if(tempFixedSize1Nodes.contains(node.outOfInstance.definition.out.get(i))){
//									fixedSize1Nodes.add(node.outOfInstance.out.get(i));
//								}
//							}
//						}
//					}
//				}
//				
//			}
//			private void expand(Node node, HashMap<Node, Integer> nodeSize, HashSet<Node> fixedSize1Nodes) {
//				//expand up, then expand down if needed
//				if (!nodeSize.containsKey(node)){
//					nodeSize.put(node, 1);//size 1 if not subnodes nor mapped
//					if (nodeSize.get(node)<node.parents.size()) nodeSize.put(node, node.parents.size());
//				}
//				
//				//expand ins
//				int parentsSize=0;
//				for(Node parent:node.parents){
//					if(!nodeSize.containsKey(parent)){
//						expand(parent,nodeSize, fixedSize1Nodes);
//					}
//					if(node.parents.size()!=1){
//						parentsSize+=nodeSize.get(parent);
//					}
//				}
//				if(nodeSize.get(node)<parentsSize){
//					nodeSize.put(node, parentsSize);
//				}
//				//expand outs
//				int childrenSize=0;
//				for(Node child:node.children){
//					if(!nodeSize.containsKey(child)){
//						expand(child,nodeSize, fixedSize1Nodes);
//					}
//					if(child.parents.size()==1){
//						childrenSize+=nodeSize.get(child);	
//					}
//				}
//				if(nodeSize.get(node)<childrenSize){
//					nodeSize.put(node, childrenSize);
//				}
//				if(node.parents.size()>1&&nodeSize.get(node)>parentsSize){
//					if(!fixedSize1Nodes.contains(node.parents.get(0))){
//						nodeSize.put(node.parents.get(0),(childrenSize-parentsSize)/2+3);//minimum size for a node divided in subnodes is 3
//					}
//					if(!fixedSize1Nodes.contains(node.parents.get(node.parents.size()-1))){
//						nodeSize.put(node.parents.get(node.parents.size()-1),(childrenSize-parentsSize)/2+3);//minimum size for a node divided in subnodes is 3
//					}
//					if(!fixedSize1Nodes.contains(node.parents.get(0))){
//						expand(node.parents.get(0),nodeSize, fixedSize1Nodes);
//					}
//					if(!fixedSize1Nodes.contains(node.parents.get(node.parents.size()-1))){
//						expand(node.parents.get(node.parents.size()-1),nodeSize, fixedSize1Nodes);
//					}
//					//expand ins
//					parentsSize=0;
//					for(Node parent:node.parents){
//						if(!nodeSize.containsKey(parent)){
//							expand(parent,nodeSize, fixedSize1Nodes);
//						}
//						if(node.parents.size()!=1){
//							parentsSize+=nodeSize.get(parent);
//						}
//					}
//					if(nodeSize.get(node)<parentsSize){
//						nodeSize.put(node, parentsSize);
//					}
//				}
//				if(node.childrenAreSubnodes()){
//					nodeSize.put(node.children.get(node.children.size()/2), nodeSize.get(node)-childrenSize+nodeSize.get(node.children.get(node.children.size()/2)));
//					expand(node.children.get(node.children.size()/2),nodeSize, fixedSize1Nodes);
//				}
//				if(node.outOfInstance!=null){//the node is out of instance
//					if(node.outOfInstance.definition.name=="nand"){//NAND //TODO: fix nand checking
//						//this is out
//						//expand ins
//						if(!nodeSize.containsKey(node.outOfInstance.in.get(0))||nodeSize.get(node.outOfInstance.in.get(0))<nodeSize.get(node)){
//							nodeSize.put(node.outOfInstance.in.get(0), nodeSize.get(node));
//							this.expand(node.outOfInstance.in.get(0), nodeSize, fixedSize1Nodes);
//						}
//						if(!nodeSize.containsKey(node.outOfInstance.in.get(1))||nodeSize.get(node.outOfInstance.in.get(1))<nodeSize.get(node)){
//							nodeSize.put(node.outOfInstance.in.get(1), nodeSize.get(node));
//							this.expand(node.outOfInstance.in.get(1), nodeSize, fixedSize1Nodes);
//						}
//						int size0=nodeSize.get(node.outOfInstance.in.get(0));
//						int size1=nodeSize.get(node.outOfInstance.in.get(1));
//						if(nodeSize.get(node)<size0){
//							nodeSize.put(node, size0);
//						}
//						if(nodeSize.get(node)<size1){
//							nodeSize.put(node, size1);
//						}
//						if(size0<nodeSize.get(node)){
//							nodeSize.put(node.outOfInstance.in.get(0), nodeSize.get(node));
//							this.expand(node.outOfInstance.in.get(0), nodeSize, fixedSize1Nodes);
//						}
//						if (size1<nodeSize.get(node)){
//							nodeSize.put(node.outOfInstance.in.get(1), nodeSize.get(node));
//							this.expand(node.outOfInstance.in.get(1), nodeSize, fixedSize1Nodes);
//						}
//					}else{//the node is out of an instance different to NAND
//						if(this.instances.contains(node.outOfInstance)){//check definition has not been removed (= is recursive)
//							HashMap<Node, Integer> tempNodeSize = new HashMap<Node, Integer>();
//							//expand outs
//							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
//								if(!nodeSize.containsKey(node.outOfInstance.out.get(i))){
//									this.expand(node.outOfInstance.out.get(i),nodeSize, fixedSize1Nodes);
//								}
//								tempNodeSize.put(node.outOfInstance.definition.out.get(i),nodeSize.get(node.outOfInstance.out.get(i)));
//							}
//							//expand ins
//							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
//								if(!nodeSize.containsKey(node.outOfInstance.in.get(i))){
//									this.expand(node.outOfInstance.in.get(i),nodeSize, fixedSize1Nodes);
//								}
//								tempNodeSize.put(node.outOfInstance.definition.in.get(i),nodeSize.get(node.outOfInstance.in.get(i)));
//							}
//						//expand definition
//						for (int i = 0; i < node.outOfInstance.out.size(); i++) {
//							node.outOfInstance.definition.expand(node.outOfInstance.definition.out.get(i), tempNodeSize, fixedSize1Nodes);
//						}
//						//expand outs if needed
//						for (int i = 0; i < node.outOfInstance.out.size(); i++) {
//							if(nodeSize.get(node.outOfInstance.out.get(i))<tempNodeSize.get(node.outOfInstance.definition.out.get(i))){
//								nodeSize.put(node.outOfInstance.out.get(i), tempNodeSize.get(node.outOfInstance.definition.out.get(i)));
//								if(node!=node.outOfInstance.definition.out.get(i)){
//									this.expand(node.outOfInstance.definition.out.get(i), tempNodeSize, fixedSize1Nodes);
//								}
//							}
//						}
//						//expand ins if needed
//						for (int i = 0; i < node.outOfInstance.in.size(); i++) {
//							if(nodeSize.get(node.outOfInstance.in.get(i))<tempNodeSize.get(node.outOfInstance.definition.in.get(i))){
//								nodeSize.put(node.outOfInstance.in.get(i), tempNodeSize.get(node.outOfInstance.definition.in.get(i)));
//								expand(node.outOfInstance.in.get(i),nodeSize, fixedSize1Nodes);
//							}
//						}
//						
//					}
//						
//					}
//					if(node.inOfInstances!=null){//the node is in of instance(s)
//						for(Instance instance:node.inOfInstances){
//							if(this.instances.contains(instance)){//check definition has not been removed (= is recursive)
//								if(instance.definition.name=="nand"){//NAND //TODO: fix nand checking
//									if(!nodeSize.containsKey(instance.out.get(0))||nodeSize.get(instance.out.get(0))<nodeSize.get(node)){
//										nodeSize.put(instance.out.get(0), nodeSize.get(node));
//										this.expand(instance.out.get(0), nodeSize, fixedSize1Nodes);
//									}
//									if(!nodeSize.containsKey(instance.in.get(0))||nodeSize.get(instance.in.get(0))<nodeSize.get(node)){
//										nodeSize.put(instance.in.get(0), nodeSize.get(node));
//										if(node!=instance.in.get(0)){
//											this.expand(instance.in.get(0), nodeSize, fixedSize1Nodes);
//										}
//									}
//									if(!nodeSize.containsKey(instance.in.get(1))||nodeSize.get(instance.in.get(1))<nodeSize.get(node)){
//										nodeSize.put(instance.in.get(1), nodeSize.get(node));
//										if(node!=instance.in.get(1)){
//											this.expand(instance.in.get(1), nodeSize, fixedSize1Nodes);
//										}
//									}
//									int size0=nodeSize.get(instance.in.get(0));
//									int size1=nodeSize.get(instance.in.get(1));
//									int sizeOut=nodeSize.get(instance.out.get(0));
//									if(nodeSize.get(node)<size0){
//										nodeSize.put(node, size0);
//									}
//									if(nodeSize.get(node)<size1){
//										nodeSize.put(node, size1);
//									}
//									if(nodeSize.get(node)<sizeOut){
//										nodeSize.put(node, sizeOut);
//									}
//									if(size0<nodeSize.get(node)){
//										nodeSize.put(instance.in.get(0), nodeSize.get(node));
//										if(node!=instance.in.get(0)){
//											this.expand(instance.in.get(0), nodeSize, fixedSize1Nodes);
//										}
//									}
//									if (size1<nodeSize.get(node)){
//										nodeSize.put(instance.in.get(1), nodeSize.get(node));
//										if(node!=instance.in.get(1)){
//											this.expand(instance.in.get(1), nodeSize, fixedSize1Nodes);
//										}
//									}
//									if(sizeOut<nodeSize.get(node)){
//										nodeSize.put(instance.out.get(0), nodeSize.get(node));
//										this.expand(instance.out.get(0), nodeSize, fixedSize1Nodes);
//									}
//								}else{//the node is in of an instance different to NAND
//									HashMap<Node, Integer> tempNodeSize = new HashMap<Node, Integer>();
//									//expand outs
//									for (int i = 0; i < instance.out.size(); i++) {
//										if(!nodeSize.containsKey(instance.out.get(i))){
//											this.expand(instance.out.get(i),nodeSize, fixedSize1Nodes);
//										}
//										tempNodeSize.put(instance.definition.out.get(i),nodeSize.get(instance.out.get(i)));
//									}
//									//expand ins
//									for (int i = 0; i < instance.in.size(); i++) {
//										if(!nodeSize.containsKey(instance.in.get(i))){
//											this.expand(instance.in.get(i),nodeSize, fixedSize1Nodes);
//										}
//										tempNodeSize.put(instance.definition.in.get(i),nodeSize.get(instance.in.get(i)));
//									}
//									//expand definition
//									for (int i = 0; i < instance.out.size(); i++) {
//										instance.definition.expand(instance.definition.out.get(i), tempNodeSize, fixedSize1Nodes);
//									}
//									//expand outs if needed
//									for (int i = 0; i < instance.out.size(); i++) {
//										if(nodeSize.get(instance.out.get(i))<tempNodeSize.get(instance.definition.out.get(i))){
//											nodeSize.put(instance.out.get(i), tempNodeSize.get(instance.definition.out.get(i)));
//											this.expand(instance.definition.out.get(i), tempNodeSize, fixedSize1Nodes);
//										}
//									}
//									//expand ins if needed
//									for (int i = 0; i < instance.in.size(); i++) {
//										if(nodeSize.get(instance.in.get(i))<tempNodeSize.get(instance.definition.in.get(i))){
//											nodeSize.put(instance.in.get(i), tempNodeSize.get(instance.definition.in.get(i)));
//											if(node!=instance.definition.in.get(i)){
//												expand(instance.in.get(i),nodeSize, fixedSize1Nodes);
//											}
//										}
//									}
//									
//								}
//							}
//						}
//					}
//				}
//			}
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
				instance.out = new ArrayList<Node>(Arrays.asList(nodes).subList(def.in.size(), def.in.size()+def.out.size()));
				instance.definition=def;
				for (Node outNode:instance.out) {//nºinst outs = nºdef outs
					if(outNode==this.out.get(0)) def.rootIn.add(this);
					outNode.outOfInstance=instance;
				}
				this.instances.add(instance);
				return instance;
			}
			public Instance addWithoutRootIn(Definition def,Node ... nodes){
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
				if(this.name=="if"&&valueMap.containsKey(this.in.get(0))){//FIXME: this is a fix, better way?
					if(valueMap.get(this.in.get(0)).length()==0){
						valueMap.put(this.out.get(0),valueMap.get(this.in.get(0)));
					}else if(valueMap.get(this.in.get(0)).cardinality()==0){
						if(valueMap.containsKey(this.in.get(2))){
							valueMap.put(this.out.get(0),valueMap.get(this.in.get(2)));
						}
					}else{
						if(valueMap.containsKey(this.in.get(1))){
							valueMap.put(this.out.get(0),valueMap.get(this.in.get(1)));
						}
					}
					
				}else if(this.in.size()==2&&this.out.size()==1&&valueMap.containsKey(this.in.get(0))&&valueMap.get(this.in.get(0)).length()==0&&valueMap.containsKey(this.in.get(1))){
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
					}else if(valueMap.containsKey(this.in.get(0))&&valueMap.containsKey(this.in.get(1))){
						valueMap.put(this.out.get(0),valueMap.get(this.in.get(0)).nand(valueMap.get(this.in.get(1))));
					}
//					System.out.println(FixedBitSet.toString(this.out.get(0).value));
				}else{
					HashSet<Instance> recursiveInstances = new HashSet<Instance>();
					HashSet<Instance> instancesToExpand = new HashSet<Instance>();
					boolean outs;
					boolean nullins=false;
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
						for (int i = 0; i < this.in.size(); i++) {
							if(!valueMap.containsKey(this.in.get(i))){
								nullins=true;
							}
						}
					}while(!outs&&!instancesToExpand.isEmpty()&&!nullins);
					
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
				for(Instance instance : this.recursiveInstances){
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
				this.recursiveInstances.clear();
				this.instances.removeAll(this.instancesOfRecursiveDefinitions);
				HashSet<Instance> instances = new HashSet<Instance>();
				instances.addAll(this.instancesOfRecursiveDefinitions);
				this.instancesOfRecursiveDefinitions.clear();
				for(Instance instance:instances){//map all the instancesOfRecursiveDefinitions nodes
					this.expandRecursiveInstance(instance, addedNodes, removedInstances);
				}
			}
			private void expandRecursiveInstance(Instance instance, AddedNodes addedNodes, HashSet<Instance> removedInstances) {
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
								node.recursivelyMapParents(definitionToInstanceNodes);
							}
						}
						for (Node node: definitionInstance.out) {//map out nodes
							if(definitionToInstanceNodes.containsKey(node)){
								nodes.add(definitionToInstanceNodes.get(node));
							}else{
								Node defNode = new Node();
								definitionToInstanceNodes.put(node, defNode);
								nodes.add(defNode);	
								for(Node parent:node.parents){//map parent nodes //think don't need to map children//TODO:recursive
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
						Instance newInstance=this.add(definitionInstance.definition,nodes.toArray(new Node[nodes.size()]));
						if(newInstance.definition==instance.definition){
							this.instancesOfRecursiveDefinitions.remove(newInstance);
							this.removeRecursiveInstance(newInstance, addedNodes, removedInstances);
						}else if(!newInstance.definition.recursiveInstances.isEmpty()){//is recursive
							this.instancesOfRecursiveDefinitions.remove(newInstance);
							this.instances.remove(newInstance);
							this.expandRecursiveInstance(newInstance, addedNodes, removedInstances);
						}
				}
				
			}
			private void removeRecursiveInstance(Instance instance,AddedNodes addedNodes, HashSet<Instance> removedInstances) {
				removedInstances.add(instance);
				addedNodes.in+=instance.out.size();
				for (int i = 0; i < instance.out.size(); i++) {//add out nodes to def in
					instance.out.get(i).outOfInstance=null;
					this.in.add(instance.out.get(i));
				}
				addedNodes.out+=instance.in.size();
				for (int i = 0; i < instance.in.size(); i++) {//add in nodes to def out
					this.out.add(instance.in.get(i));
				}
				this.instances.remove(instance);
			}
			private void mapSubnodeParents(Node node, Node definitionNode, HashMap<Node, Node> definitionToNewNodes) {
				if(!definitionNode.parents.isEmpty()){
					if(node.parents.isEmpty()){
						for(Node parent:definitionNode.parents){
							Node newParent= new Node();
							newParent.add(node);
							definitionToNewNodes.put(parent, newParent);
							mapSubnodeParents(newParent,parent,definitionToNewNodes);
						}
					}else if(node.parents.size()==definitionNode.parents.size()){
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
					if(node.children.size()==definitionNode.children.size()){//==3
						for(int i=0;i<node.children.size();i++){
							definitionToInstanceNodes.put(definitionNode.children.get(i),node.children.get(i));
							
						}
						for(int i=0;i<node.children.size();i++){
							mapSubnodeChildren(node.children.get(i), definitionNode.children.get(i),definitionToInstanceNodes);
						}
					}else{
						if(definitionNode.children.size()==3){//supernode
							if(node.parents.size()>1){//prevent redundant subnodes
								//variables to conserve references
								Node parentLeft = node.parents.get(0);
								if(parentLeft.children.size()==1)parentLeft.children.clear();
								Node parentRight = node.parents.get(node.parents.size()-1);
								if(parentRight.children.size()==1)parentRight.children.clear();
								Node newNode = new Node();
								parentLeft=this.mapLeft(parentLeft,newNode);
								parentRight=this.mapRight(parentRight,newNode);
								definitionToInstanceNodes.put(definitionNode.children.get(0), parentLeft.children.get(0));
								mapSubnodeChildren(parentLeft.children.get(0),definitionNode.children.get(0),definitionToInstanceNodes);
								definitionToInstanceNodes.put(definitionNode.children.get(2), parentRight.children.get(2));
								mapSubnodeChildren(parentRight.children.get(2),definitionNode.children.get(2),definitionToInstanceNodes);
								definitionToInstanceNodes.put(definitionNode.children.get(1), newNode);
								mapSubnodeChildren(newNode,definitionNode.children.get(1),definitionToInstanceNodes);
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
			private Node mapLeft(Node parentLeft, Node newNode) {
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
						if(parentLeft.children.size()==1)parentLeft.children.clear();
							parentLeft.splitChildren();
							parentLeft.children.get(1).add(newNode);
							parentLeft.children.get(2).add(newNode);
					}
				}
				return parentLeft;
			}
			private Node mapRight(Node parentRight, Node newNode) {
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
						if(parentRight.children.size()==1)parentRight.children.clear();
						parentRight.splitChildren();
						parentRight.children.get(0).add(newNode);
						parentRight.children.get(1).add(newNode);
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
							this.add(superInstance.definition,nodes.toArray(new Node[nodes.size()]));
							for(Instance candidateInstance:candidateInstances){
								this.remove(candidateInstance);
							}
						}
						
						
					}
				}
					
			}
			public void nodeFission() {
				for(Node outNode:this.out){
					outNode.childrenFission();
				}
				for(Node outNode:this.out){
					outNode.parentsFission();
				}
			}
			public void chopEquals(Node node1, Node node2, Node node3) {
				
				if(node1.children.size()==3||node2.children.size()==3){
					node1.splitChildren();
					node2.splitChildren();
					node3.splitChildren();
				}else if(node1.parents.size()>1||node2.parents.size()>1){
					int nodeSize=0;
					if(node1.parents.size()==node2.parents.size()){
						nodeSize=node1.parents.size();
					}else if(node1.parents.size()==0){
						nodeSize=node2.parents.size();
					}else if(node2.parents.size()==0){
						nodeSize=node1.parents.size();
					}//TODO: else
					if(node3.parents.size()==0){
						for(int i=0;i<nodeSize;i++){
							Node newNode = new Node();
							newNode.add(node3);
						}
					}
				}
//				node2.splitParents(node1.parents.size());
//				node3.splitParents(node1.parents.size());
//				node1.splitParents(node2.parents.size());
//				node3.splitParents(node2.parents.size());
//				node1.splitParents(node3.parents.size());
//				node2.splitParents(node3.parents.size());
				
			}
			public Definition copy() {
				Definition copyDef = new Definition();
				copyDef.name=this.name;
				HashMap<Node,Node> nodeToCopy = new HashMap<Node,Node>();
				//map nodes
				for(Node node:this.nodes){
					Node copyNode = new Node();
					copyDef.add(copyNode);
					nodeToCopy.put(node, copyNode);
				}
				//copy subnodes
				for(Node node:this.nodes){
					for(Node child:node.children){
						nodeToCopy.get(node).children.add(nodeToCopy.get(child));
					}
					for(Node parent:node.parents){
						nodeToCopy.get(node).parents.add(nodeToCopy.get(parent));
					}
				}
				//copy inputs
				for(Node nodeIn:this.in){
					copyDef.in.add(nodeToCopy.get(nodeIn));
				}
				//copy outpus
				for(Node nodeOut:this.out){
					copyDef.out.add(nodeToCopy.get(nodeOut));
				}
				//copy instances
				for(Instance instance:this.instances){
					ArrayList<Node> copyNodes = new ArrayList<Node>();
					for(Node nodeIn:instance.in){
						copyNodes.add(nodeToCopy.get(nodeIn));
					}
					for(Node nodeOut:instance.out){
						copyNodes.add(nodeToCopy.get(nodeOut));
					}
					copyDef.addWithoutRootIn(instance.definition, copyNodes.toArray(new Node[copyNodes.size()]));
				}
				return copyDef;
			}
//			public void removeRedundantSubnodes() {
//				ArrayList<Node> out = new ArrayList<Node>();
//				for(Node nodeOut:this.out){
//					out.add(nodeOut.removeRedundantSubnodes());
//				}
//				this.out=out;
//			}
//			public void chopNand(Node nodeIn0, Node nodeIn1, Node nodeOut, Definition nand) {
//				if(nodeIn0.parents.size()<2&&nodeIn1.parents.size()<2){
//					if(nodeOut.outOfInstance==null){
//						ArrayList<Node> nodes = new ArrayList<Node>();
//						nodes.add(nodeIn0);
//						nodes.add(nodeIn1);
//						nodes.add(nodeOut);
//						this.add(nand,nodes.toArray(new Node[nodes.size()]));
//					}
//				}else if(nodeIn0.parents.size()==2){
//					if(nodeIn1.parents.size()!=2){
//						nodeIn1.splitParents();
//					}
//					nodeOut.splitParents();
//					chopNand(nodeIn0.parents.get(0),nodeIn1.parents.get(0),nodeOut.parents.get(0), nand);
//					chopNand(nodeIn0.parents.get(1),nodeIn1.parents.get(1),nodeOut.parents.get(1), nand);
//					this.remove(nodeOut.outOfInstance);
//					nodeOut.outOfInstance=null;
//				}
//				
//			}
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
							this.expandInstance(instance);
							this.remove(instance);
						}
					}
				}
				
				
			}
			private void expandInstance(Instance instance) {
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
		}
