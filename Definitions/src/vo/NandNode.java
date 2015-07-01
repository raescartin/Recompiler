/*******************************************************************************
 * Copyright (c) 2015 Rub�n Alejandro Escart�n Aparicio.
 * All rights reserved.
 *******************************************************************************/
package vo;

import java.util.ArrayList;

import utils.FixedBitSet;

public class NandNode{
	NandNode in1;
	NandNode in2;
	public FixedBitSet eval(FixedBitSet[] inValues, ArrayList<NandNode> in) {
		if(this.in1==null||this.in2==null){//leaf
			return inValues[in.indexOf(this)];
		}else{
			return (this.in1.eval(inValues,in)).nand(this.in2.eval(inValues,in));
		}
	}
	public String printNode(ArrayList<NandNode> in){
		String string = new String();
		if(this.in1!=null&&this.in2!=null){
			string+=("(");
			string+=this.in1.printNode(in);
			string+=(") nand (");
			string+=this.in2.printNode(in);
			string+=(")");
		}else{
			string+=(in.indexOf(this));
		}
		return string;
	}
}

