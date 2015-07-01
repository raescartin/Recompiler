/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;

import utils.FixedBitSet;

//DESCRIPTION
//-Defines an operation, composed by  binary trees of nands (one for each output)
//-The trees may (and should) have nodes in common

//IMPLEMENTATION: 
// TODO:
//-remove value from nodes
//-transform to ParallelNandTree
//-can't have two repeated nands
//-NAND fusion
//-Short-circuit evaluation
//-change data structure to optimal:
//	-treat inputs as one big, merged input (same with outputs)
//	-parallelization (reduce memory constraints using a matrix of nodes, only an array of parallel values at a time)
//-!=sizes on eval

//EXTRAS
//TODO:
//-print tree

//DATA STRUCTURE:
//Each NandTree has a variable number of "in" and "out" nodes
//each NandTree is formed by a variable number of trees of nands
//each out node is a root node of a nand tree
//nand trees may (and should be) interconnected (if there's dependencies, for efficiency sake, avoiding repetition)
//each tree of nands is composed by nand nodes
//each nand node points to two nand nodes

public class NandTree {//multiple nodes nand tree
	public ArrayList<Node> in = new ArrayList<Node>();
	public ArrayList<Node> out = new ArrayList<Node>();
	public String name;
	public class Node{
		public void printNode(){
			if(this.in1!=null&&this.in2!=null){
				System.out.print("(");
				this.in1.printNode();
				System.out.print(") nand (");
				this.in2.printNode();
				System.out.print(")");
			}else{
				System.out.print(this.value.toString());
			}
		}
		public FixedBitSet value; //added to nodes for simplicity, all nodes equal
		Node in1;
		Node in2;
	}
	public NandTree(String name, int numberOfInputs) {
		this.name=name;
		for (int i = 0; i < numberOfInputs; i++) {
			in.add(new Node());
		}
	}
	public NandTree() {
		// TODO Auto-generated constructor stub
	}
	public String toString(){
		return name;
	}
	public Node add(Node in1,Node in2){
		Node node;
		
		if(in1!=null&&in2!=null //Reductible node
				&&in1.in1!=null&&in1.in2!=null&&in2.in1!=null&&in2.in2!=null
				&&(in1.in1==in1.in2)&&(in1.in2==in2.in1)&&(in2.in1==in2.in2)){
			    //-( A nand A ) nand ( A nand A ) == A 
				node = in1.in1;// old nodes may remain for the garbage collector to deal
					
		}else{//new node really needed
			node = new Node();
			if(in1.hashCode()<=in2.hashCode()){//Order so A NAND B == B NAND A
				node.in1=in1;
				node.in2=in2;
			}else{
				node.in1=in2;
				node.in2=in1;
			}
		}
		return node;
	}
	public void addOut(Node node){
		this.out.add(node);
	}
	public ArrayList<FixedBitSet> evalAll(){//multiple NandTrees
		ArrayList<FixedBitSet> outs = new ArrayList<FixedBitSet>();
		for (int i = 0; i < out.size(); i++) {
			outs.add(eval(out.get(i)));
		}
		return outs;
	}
	private FixedBitSet eval(Node node){
		if(node.in1==null||node.in2==null){//leaf
			return node.value;
		}else{
			return eval(node.in1).nand(eval(node.in2));
		}
	}
	public void print(){
		for (int i = 0; i < out.size(); i++) {
			out.get(i).printNode();
		}
		System.out.println();
	}

}
