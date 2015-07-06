/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import utils.FixedBitSet;

//DESCRIPTION
//-Defines an operation, composed by binary trees of nands; a forest of nands
//-The trees may (and should) have nodes in common
//-implicit "parallel" nand (structure isn't exactly an array of arrays, more diffuse ordering)
//-A NAND FOREST IS NOT TURING COMPLETE, AND LIMITED IN HORIZONTAL LENGTH

//IMPLEMENTATION: 
// TODO:
//-NAND fusion (& fision ?)
//-node can't be his own parent/child NO RECURSIVITY INSIDE
//////////////////////////////////////////////////////
//HERE BE DRAGONS (maybe not in Java from here)
//////////////////////////////////////////////////////
//-transform to ParallelNandForest?
//-change data structure to optimal:
//	-treat inputs as one big, merged input (same with outputs)
//	-parallelization (reduce memory on evaluation using a "matrix" of nodes, only an array of parallel values at a time)
//-!=sizes on eval?

//DOUBTS:
//-Short-circuit evaluation?

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
	public HashMap<NandNode,NandNode>  nodes = new HashMap<NandNode,NandNode>();//used to keep record of UNIQUE nodes
	public NandForest(int numberOfInputs) {
		for (int i = 0; i < numberOfInputs; i++) {//add in nodes to nandForest
			this.in.add(new NandNode(BigInteger.valueOf(i)));
			this.nodes.put(this.in.get(i), this.in.get(i));
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
					
		}else{//add new node really needed
			node = new NandNode(new BigInteger(String.valueOf(in1.id) + String.valueOf(in2.id)));
			node.in1=in1;
			node.in2=in2;
			if(this.nodes.containsKey(node)){//if a node with same childs already exists return existing node, else return new node
				return this.nodes.get(node);
			}
			this.nodes.put(node,node);
		}
		return node;
	}
	public void addOuts(ArrayList<NandNode> nodes){
		this.out.addAll(nodes);
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
		if(in.size()>0)string=("(0");
		for (int i = 1; i < in.size(); i++) {
			string+=(","+i);
		}
		string+=(";");
		if(out.size()>0)string+=((in.size()));
		for (int i = 1; i < out.size(); i++) {
			string+=(","+(i+in.size()));
		}
		string+=(")=");
		string+=out.get(0).printNode(this.in);
		for (int i = 1; i < out.size(); i++) {
			string+=("|");
			string+=out.get(i).printNode(this.in);
		}
		string+="\n";
		return string;
	}
	public void optimize(){
		//add nodes from leafs (in) to roots (out)
		this.nodes.clear();
		for (NandNode node : this.in) {
			this.nodes.put(node,node);
		}
		for (NandNode node : this.out) {
			this.optimizeByLevel(node);
		}
		
	}
	private NandNode optimizeByLevel(NandNode node) {
		if(this.nodes.containsKey(node)){
			return node;
		}else{
			return this.add(optimizeByLevel(node.in1), optimizeByLevel(node.in2));
		}
		
	}
}
