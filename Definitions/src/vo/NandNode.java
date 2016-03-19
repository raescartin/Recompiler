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
		if(in1!=null)string+=("("+in1.id);
		if(in2!=null)string+=(","+in2.id+")=");
		string+=this.id;
		return string;
	}
	public String printNode(ArrayList<NandNode> in){
		String string = new String();

		if(this.in1!=null&&this.in2!=null){
//			string+=this.id; //used only to debug
			string+=("(");
			string+=this.in1.printNode(in);
			string+=(") nand (");
			string+=this.in2.printNode(in);
			string+=(")");
		}else{
			string+=this.id;
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
	public void extractNewRecursionNandIO(
			NandForest expandedDefinitionNandForest,
			HashSet<NandNode> originalNandNodes,
			ArrayList<NandNode> newRecursiveDefinitionNandIn,
			ArrayList<NandNode> newRecursiveDefinitionNandOut) {
		if(originalNandNodes.contains(this)){
			if(this.in1!=null&&!originalNandNodes.contains(this.in1)||this.in2!=null&&!originalNandNodes.contains(this.in2)){
				if(!newRecursiveDefinitionNandOut.contains(this)){
					newRecursiveDefinitionNandOut.add(this);
				}
				if(this.in1!=null){
					this.in1.extractNewRecursionNandIO(expandedDefinitionNandForest,originalNandNodes,newRecursiveDefinitionNandIn,newRecursiveDefinitionNandOut);
				}
				if(this.in2!=null){
					this.in2.extractNewRecursionNandIO(expandedDefinitionNandForest,originalNandNodes,newRecursiveDefinitionNandIn,newRecursiveDefinitionNandOut);
				}
			}
		}else{
			if((this.in1!=null)&&(originalNandNodes.contains(this.in1))){
				if(!newRecursiveDefinitionNandIn.contains(this.in1)){
					newRecursiveDefinitionNandIn.add(this.in1);
				}
			}
			if(this.in1!=null) this.in1.extractNewRecursionNandIO(expandedDefinitionNandForest,originalNandNodes,newRecursiveDefinitionNandIn,newRecursiveDefinitionNandOut);
			if((this.in2!=null)&&(originalNandNodes.contains(this.in2))){
				if(!newRecursiveDefinitionNandIn.contains(this.in2)){
					newRecursiveDefinitionNandIn.add(this.in2);
				}
			}
			if(this.in2!=null) this.in2.extractNewRecursionNandIO(expandedDefinitionNandForest,originalNandNodes,newRecursiveDefinitionNandIn,newRecursiveDefinitionNandOut);
		}
		
	}
}

