/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.math.BigInteger;

import utils.FixedBitSet;

public class NandNode{
	NandNode in1;
	NandNode in2;
	BigInteger id;
	public NandNode(BigInteger thisId) {
		this.id=thisId;
	}
	public FixedBitSet eval(FixedBitSet[] inValues, ArrayList<NandNode> in) {
		if(this.in1==null||this.in2==null){//leaf
			return inValues[in.indexOf(this)];
		}else{
			return (this.in1.eval(inValues,in)).nand(this.in2.eval(inValues,in));
		}
	}
	public String toString() {
		//for subnodes and supernodes instead of fathers and children
		String string = new String();
		string+=this.id;
		return string;
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
			string+=(this.id);
		}
		return string;
	}
	public void mapOutOfNandsByLevel(
			HashMap<NandNode, HashSet<NandNode>> outOfNands) {
		if(this.in1!=null){
			HashSet<NandNode> nands=outOfNands.get(this.in1);
			if(nands==null){
				nands= new HashSet<NandNode>();
				nands.add(this);
				outOfNands.put(this.in1, nands);
			}else{
				nands.add(this);
			}
			in1.mapOutOfNandsByLevel(outOfNands);
		}
		if(this.in2!=null){
			HashSet<NandNode> nands=outOfNands.get(this.in2);
			if(nands==null){
				nands= new HashSet<NandNode>();
				nands.add(this);
				outOfNands.put(this.in2, nands);
			}else{
				nands.add(this);
			}
			in2.mapOutOfNandsByLevel(outOfNands);
		}
	}
}

