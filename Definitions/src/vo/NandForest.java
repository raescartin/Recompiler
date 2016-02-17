/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import utils.FixedBitSet;

//DESCRIPTION
//-Defines an operation, composed by binary trees of nands; a forest of nands
//-The trees may (and should) have nodes in common
//-implicit "parallel" nand (structure isn't exactly an array of arrays, more diffuse ordering)
//-node can't be his own parent/child NO RECURSIVITY(A NAND FOREST IS NOT TURING COMPLETE, ONLY A SEQUENCE OF OPERATIONS)
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
//nand trees may (and should) have nodes in common (for efficiency sake, avoiding repetition)
//each tree of nands is composed by nand nodes
//each nand node (parent) points to two child nand nodes

public class NandForest {//multiple nand trees
	public ArrayList<NandNode> in = new ArrayList<NandNode>(); //NEEDED
	public ArrayList<NandNode> out = new ArrayList<NandNode>(); //NEEDED
	public HashMap<BigInteger,NandNode>  nodes = new HashMap<BigInteger,NandNode>();//used to keep record of UNIQUE nodes
	public NandForest(int numberOfInputs) {
		for (int i = 0; i < numberOfInputs; i++) {//add in nodes to nandForest
			this.in.add(new NandNode(BigInteger.valueOf(i+1)));//must add 1 so all id's are ordered consistently 
			this.nodes.put(this.in.get(i).id, this.in.get(i));
		}
	}
	public NandNode add(NandNode in1,NandNode in2){
		NandNode node;
		if(in1.id.compareTo(in2.id) > 0) {//in1.id<in2.id){//Order so A NAND B == B NAND A, always represented as A NAND B
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
			node = new NandNode(new BigInteger(String.valueOf(in1.id) + String.valueOf(in2.id)));
			node.in1=in1;
			node.in2=in2;
			if(this.nodes.containsKey(node.id)){//check if node already exists
				return this.nodes.get(node.id);
			}
			this.nodes.put(node.id,node);
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
		for (int i = 0; i < in.size(); i++) {
			string+=(this.in.get(i).id+",");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration ","
		string+=(";");
		for (int i = 0; i < out.size(); i++) {
			string+=(this.out.get(i).id+",");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration ","
		string+=(")=");
		for (int i = 0; i < out.size(); i++) {
			string+=out.get(i).printNode(this.in);
			string+=("|");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration "|"
		string+="\n";
		return string;
	}
	public void optimize(){
		//add nodes from leafs (in) to roots (out)
		this.nodes.clear();
		for (NandNode node : this.in) {
			this.nodes.put(node.id,node);
		}
		for (NandNode node : this.out) {
			this.optimizeByLevel(node);
		}
		
	}
	private NandNode optimizeByLevel(NandNode node) {
		if(this.nodes.containsKey(node.id)){
			return node;
		}else{
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
		NandNode nandNode = new NandNode(BigInteger.valueOf(this.nodes.size()+1));
		this.in.add(nandNode);
		this.nodes.put(nandNode.id,nandNode);
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
	public NandForest unchangedNodes(NandForest expandedNandForest) {
		HashMap <NandNode,NandNode> thisToExpanded = new HashMap <NandNode,NandNode>();
		HashMap <NandNode,NandNode> thisToNand = new HashMap <NandNode,NandNode>();
		HashMap <NandNode,HashSet<NandNode>> thisOutOfNands = new HashMap <NandNode,HashSet<NandNode>>();
		HashMap <NandNode,HashSet<NandNode>> expandedOutOfNands = new HashMap <NandNode,HashSet<NandNode>>();
		int minIn=Math.min(this.in.size(),expandedNandForest.in.size());
		NandForest nandForest = new NandForest(minIn);//
		this.mapOutOfNands(thisOutOfNands);
		expandedNandForest.mapOutOfNands(expandedOutOfNands);
		for(int i=0;i<minIn;i++){
			nandForest.unchangedNode(nandForest.in.get(i),this.in.get(i), expandedNandForest.in.get(i),this,expandedNandForest,thisToNand,thisToExpanded,thisOutOfNands,expandedOutOfNands);
		}
		return nandForest;
	}
	private void unchangedNode(NandNode nandNode, NandNode thisNode, NandNode expandedNode, 
			NandForest nandForest,
			NandForest expandedNandForest,
			HashMap<NandNode, NandNode> thisToNand,
			HashMap<NandNode, NandNode> thisToExpanded,
			HashMap<NandNode, HashSet<NandNode>> thisOutOfNands,
			HashMap<NandNode, HashSet<NandNode>> expandedOutOfNands) {
		//TODO: verify preceding nodes are defined
		if(!thisToExpanded.containsKey(thisNode)){
			thisToExpanded.put(thisNode, expandedNode);
			thisToNand.put(thisNode,nandNode);
			for(NandNode thisNodeOut:thisOutOfNands.get(thisNode)){
				for(NandNode expandedNodeOut:expandedOutOfNands.get(expandedNode)){
					if(thisToExpanded.get(thisNodeOut.in1)==expandedNodeOut.in1&&thisToExpanded.get(thisNodeOut.in2)==expandedNodeOut.in2){
//						this.add(in1, in2);
						this.unchangedNode(nandNode,thisNodeOut, expandedNodeOut, nandForest, expandedNandForest, thisToNand, thisToExpanded, thisOutOfNands, expandedOutOfNands);
					}
				}
			}
		}
	}
	private void mapOutOfNands(
			HashMap<NandNode, HashSet<NandNode>> thisOutOfNands) {
		for (NandNode node : this.out) {
			node.mapOutOfNandsByLevel(thisOutOfNands);
		}
	}
}
