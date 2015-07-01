/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;

import utils.FixedBitSet;

//IMPLEMENTATION: 
// TODO:
//-ALL
//-parallelization (reduce memory constraints using a matrix of nodes, only an array of parallel values at a time)
//-remove value from nodes?
//-NAND fusion
//-can't have two repeated nands (only as "out") DIFFICULT
//-Short-circuit evaluation
//-only pointers in structure, objects created outside
//-better node adding notation?
//-!=sizes on eval

//EXTRAS
//TODO:
//-print tree

public class ParallelNandTree {//multiple nodes nand tree with explicit parallelization
	public ArrayList<Node> in = new ArrayList<Node>();
	public ArrayList<Node> out = new ArrayList<Node>();
	ArrayList<ArrayList<Node>> v = new ArrayList<ArrayList<Node>>(); //matrix of nodes in each array parallel ops
	private String name;
	public class Node{
		public FixedBitSet value; //added to nodes for simplicity, all nodes equal
		Node in1;
		Node in2;
	}
	public ParallelNandTree(String name, int numberOfInputs) {
		this.name=name;
		for (int i = 0; i < numberOfInputs; i++) {
			in.add(new Node());
		}
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
	public ArrayList<FixedBitSet> evalAll(){
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
			FixedBitSet outFBS=(FixedBitSet)eval(node.in1).clone();
			outFBS.and(eval(node.in2));
			outFBS.flip(0,outFBS.length());
//			System.out.println(FixedBitSet.toString(outFBS));
		return outFBS;
		}
	}

}
