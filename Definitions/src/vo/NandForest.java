/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import utils.FixedBitSet;

//DESCRIPTION
//-Defines an operation, composed by binary trees of nands; a forest of nands
//-The trees may (and should) have nodes in common
//-implicit "parallel" nand (structure isn't exactly an array of arrays, more diffuse ordering)
//-node can't be his own parent/child NO RECURSIVITY(A NAND FOREST IS NOT TURING COMPLETE, ONLY A SEQUENCE OF OPERATIONS)
//IMPLEMENTATION
//constant MAX_NANDFOREST_INS is the maximum number of NandForest inputs so theres no duplicate id's for NandNodes
//////////////////////////////////////////////////////
//HERE BE DRAGONS (maybe not in Java from here)
//////////////////////////////////////////////////////
//-parallelization:transform to ParallelNandForest
//-optimize data structure

//DOUBTS:
//-Short-circuit evaluation?
//-!=sizes on eval?

//DATA STRUCTURE:
//Each NandForest has a variable number of "in" and "out" nodes
//each NandForest is formed by a variable number of trees of nands
//each out node is a root node of a nand tree
//internal nand trees may (and should) have nodes in common (for efficiency sake, avoiding repetition)
//each tree of nands is composed by nand nodes
//each nand node (parent) points to two child nand nodes

public class NandForest {//multiple nand trees
	public ArrayList<NandNode> in = new ArrayList<NandNode>(); //NEEDED
	public ArrayList<NandNode> out = new ArrayList<NandNode>(); //NEEDED
	public HashMap<NandNode,HashMap<NandNode,NandNode>>  outNodes = new HashMap<NandNode,HashMap<NandNode,NandNode>>();//used to keep record of UNIQUE nodes
    public HashSet<NandNode>  nodes = new HashSet<NandNode>();
    public int numNodes;
	public NandForest(int numberOfInputs) {
		this.numNodes=0;
		for (int i = 0; i < numberOfInputs; i++) {//add in nodes to nandForest
			NandNode nandNode = new NandNode();
			this.in.add(nandNode);
			this.add(nandNode);
		}
	}
	private void add(NandNode nandNode) {
		this.nodes.add(nandNode);
		nandNode.id=this.numNodes;
		this.numNodes++;
	}
	public NandNode add(NandNode in1,NandNode in2){
		NandNode node;
		if(in2.id<in1.id) {//Order nodes so A NAND B == B NAND A, always represented as A NAND B
			node=in1;
			in1=in2;
			in2=node;
		}
		if(in1!=null&&in2!=null //Reductible node
				&&in1.in1!=null&&in1.in2!=null&&in2.in1!=null&&in2.in2!=null
				&&(in1.in1==in1.in2)&&(in1.in2==in2.in1)&&(in2.in1==in2.in2)){
			    //-( A nand A ) nand ( A nand A ) == A 
				node = in1.in1;// old nodes may remain for the garbage collector to deal//FIXME:remove node from this.nodes if unused
					
		}else{//non simplified node
			if(outNodes.containsKey(in1)&&outNodes.get(in1).containsKey(in2)){//check if node already exists
				node=this.outNodes.get(in1).get(in2);
			}else{
				node = new NandNode();
				node.in1=in1;
				node.in2=in2;
				this.add(node);
				if(this.outNodes.containsKey(in1)){
					this.outNodes.get(in1).put(in2,node);
				}else{
					HashMap<NandNode,NandNode> hashMap = new HashMap<NandNode,NandNode>();
					hashMap.put(in2, node);
					outNodes.put(in1, hashMap);
				}
			}
		}
		return node;
	}

	public ArrayList<FixedBitSet> eval(FixedBitSet ... inValues){
		ArrayList<FixedBitSet> outs = new ArrayList<FixedBitSet>();
		for (int i = 0; i < out.size(); i++) {
			outs.add(out.get(i).eval(inValues,this.in));
		}
		return outs;
	}
	public String toString() {
		String string = new String();
		if(in.size()>0)string=("(");
		for (NandNode nandNode: this.in) {
			string+=(nandNode.id+",");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration ","
		string+=(";");
		for (NandNode nandNode: this.out) {
			string+=(nandNode.id+",");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration ","
		string+=(")=");
		for (int i = 0; i < out.size(); i++) {
			string+=this.printNode(out.get(i),this.in);
			string+=("|");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration "|"
		string+="\n";
		return string;
	}
	public String printNode(NandNode node,ArrayList<NandNode> in){
		String string = new String();
		if(node.in1!=null&&node.in2!=null){
//			string+=this.id; //used only to debug
			string+=("(");
			string+=this.printNode(node.in1,in);
			string+=(") nand (");
			string+=this.printNode(node.in2,in);
			string+=(")");
		}else{
			string+=node.id;
		}
		return string;
	}
	public void optimize(){
		//add nodes from leafs (in) to roots (out)
		this.nodes.clear();
		this.numNodes=0;
		for (NandNode nandNode : this.in) {
			this.add(nandNode);
		}
		for (NandNode node : this.out) {
			this.optimizeByLevel(node);
		}
		
	}
	private NandNode optimizeByLevel(NandNode node) {
		if(this.nodes.contains(node)){
			return node;
		}else{
			this.add(node);
			return this.add(optimizeByLevel(node.in1), optimizeByLevel(node.in2));
		}
	}
	public ArrayList<NandNode> addIns(Integer integer) {
		ArrayList<NandNode> nandNodes = new ArrayList<NandNode>();
		for (int i = 0; i < integer; i++) {
			nandNodes.add(this.addIn());
		}
		return nandNodes;
	}
	public NandNode addIn() {
		NandNode nandNode = new NandNode();
		this.in.add(nandNode);
		this.add(nandNode);
		return nandNode;
	}
	public ArrayList<NandNode> setOuts(ArrayList<NandNode> nandNodes){
		this.out.addAll(nandNodes);
		return nandNodes;
	}
	public NandNode setOut(NandNode nandNode) {
		this.out.add(nandNode);
		return nandNode;
	}
//	public NandForest unchangedNodes(NandForest expandedNandForest) {
//		HashMap <NandNode,NandNode> thisToExpanded = new HashMap <NandNode,NandNode>();
//		HashMap <NandNode,NandNode> thisToNand = new HashMap <NandNode,NandNode>();
//		HashMap <NandNode,HashSet<NandNode>> thisOutOfNands = new HashMap <NandNode,HashSet<NandNode>>();
//		HashMap <NandNode,HashSet<NandNode>> expandedOutOfNands = new HashMap <NandNode,HashSet<NandNode>>();
//		int minIn=Math.min(this.in.size(),expandedNandForest.in.size());
//		NandForest nandForest = new NandForest(minIn);//
//		this.mapOutOfNands(thisOutOfNands);
//		expandedNandForest.mapOutOfNands(expandedOutOfNands);
//		for(int i=0;i<minIn;i++){
//			nandForest.unchangedNode(nandForest.in.get(i),this.in.get(i), expandedNandForest.in.get(i),this,expandedNandForest,thisToNand,thisToExpanded,thisOutOfNands,expandedOutOfNands);
//		}
//		return nandForest;
//	}
//	private void unchangedNode(NandNode nandNode, NandNode thisNode, NandNode expandedNode, 
//			NandForest nandForest,
//			NandForest expandedNandForest,
//			HashMap<NandNode, NandNode> thisToNand,
//			HashMap<NandNode, NandNode> thisToExpanded,
//			HashMap<NandNode, HashSet<NandNode>> thisOutOfNands,
//			HashMap<NandNode, HashSet<NandNode>> expandedOutOfNands) {
//		//TODO: verify preceding nodes are defined
//		if(!thisToExpanded.containsKey(thisNode)){
//			thisToExpanded.put(thisNode, expandedNode);
//			thisToNand.put(thisNode,nandNode);
//			for(NandNode thisNodeOut:thisOutOfNands.get(thisNode)){
//				for(NandNode expandedNodeOut:expandedOutOfNands.get(expandedNode)){
//					if(thisToExpanded.get(thisNodeOut.in1)==expandedNodeOut.in1&&thisToExpanded.get(thisNodeOut.in2)==expandedNodeOut.in2){
////						this.add(in1, in2);
//						this.unchangedNode(nandNode,thisNodeOut, expandedNodeOut, nandForest, expandedNandForest, thisToNand, thisToExpanded, thisOutOfNands, expandedOutOfNands);
//					}
//				}
//			}
//		}
//	}
//	private void mapOutOfNands(
//			HashMap<NandNode, HashSet<NandNode>> thisOutOfNands) {
//		for (NandNode node : this.out) {
//			node.mapOutOfNandsByLevel(thisOutOfNands);
//		}
//	}
}
