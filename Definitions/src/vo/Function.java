/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;

import utils.FixedBitSet;

//DESCRIPTION
//-Extends a tree of nands (more nodes instead of only nand)
//-Defines a function (only one output), as a tree of functions
//-Nand is the basic function
//MAYBE MORE INTUITIVE THAN DEFINITION (HUMAN READABLE/PRINT) 1 DEFINITION -> OUT*FUNCTIONS

//IMPLEMENTATION: 
//TODO: 
//-implement if 
//-cc and set implemented in nodes: to nandtree, from nandtree, print
//-recursion to NAND? -> should be impossible without sizes or modifying NandForest with "set" "cc"
//-translate from NAND structure
//-Disable infinite self recursion (also indirect) = no CICLES
//////////////////////////////////////////////////////
//HERE BE DRAGONS (maybe not in Java from here)
//////////////////////////////////////////////////////
//-conserve node values, don't recalculate
//-Short-circuit evaluation?
//-!=sizes on eval

//DOUBTS:
//-include set in instance nodes? NO -> because it goes per inputs
//-separate functions and instances? (if possible with cc,leafs...) NO -> more simple

//DATA STRUCTURE:
//each function is equivalent to a tree of nands (NandForest)
//each function has a variable number of "in" nodes and a "name"
//each function is her own output
//each funtion points "out" to an instance of a function
//each instance points "out" to a function (with same numbers of "in" nodes == PARAMETERS )
//instances form a tree structure connecting each other via "in" nodes
//creation order, from leafs to roots in order to use "add" optimizations
//acces order, form roots to leafs

public class Function{
			public ArrayList<Function> in = new ArrayList<Function>();//NEEDED
			public Function out;//NEEDED
			public String name;
			int x; //starting index of bits from input -1=last //FIXME
			int y; //ending index of bits from input -1=last //FIXME
			public Function() {
			}
			public Function(String name, int numberOfInputs){
				this.name=name;
				for (int i = 0; i < numberOfInputs; i++) {
					in.add(new Function());
				}
			}
			public String toString(){
				return name;
			}
			public NandForest toNandForest(){
				NandForest nandForest = new NandForest(this.in.size());
				nandForest.out.add(this.functionToNands(nandForest.in,nandForest));
				return nandForest;
			}
			public NandNode functionToNands(ArrayList<NandNode> parameters,NandForest nandForest){
				if(this.out==null){//NAND
					return nandForest.add(parameters.get(0), parameters.get(1));
				}else{//FUNCTION DIFFERENT FROM NAND
					return this.out.instanceToNands(this,parameters,nandForest);
				}
			}
			public NandNode instanceToNands(Function fct,ArrayList<NandNode> functionParameters,NandForest nandForest){
				if(this.out!=null){//instance
					ArrayList<NandNode> parameters = new ArrayList<NandNode>();
					for (int i = 0; i < this.in.size(); i++) {
						parameters.add(this.in.get(i).instanceToNands(fct,functionParameters,nandForest));
					}//parameters
					return this.out.functionToNands(parameters, nandForest);
				}else{//leaf
					NandNode node=null;
					for (int i = 0; i < fct.in.size(); i++) {
						if(this==fct.in.get(i)) node=functionParameters.get(i);
					}
					return node;
				}
			}
			public Function add(Function f,Function ... functions){
				ArrayList<Function> in = new ArrayList<Function>();
				for (int i = 0; i < functions.length; i++){
					in.add( functions [i]);
				}
				Function node = new Function(); //node == instance of a function
				node.in=in;
				node.out=f;
				this.out=node;
				return node;
			}
			public FixedBitSet eval(ArrayList<Function> inFct,FixedBitSet ... inValues){			
				if(this.out==null){//NAND or LEAF
					if(this.name=="nand"){ //NAND
							return inValues[0].nand(inValues[1]);
					}else{//LEAF
						return inValues[inFct.indexOf(this)];
					}
				}else{
					if(this.name==null){//container node = instance of function
						int nulls=0;
						ArrayList<FixedBitSet> ins = new ArrayList<FixedBitSet>();
						FixedBitSet fbs = null;
						for (int i = 0; i < this.in.size(); i++) {
							fbs=this.in.get(i).eval(inFct,inValues);
							if(fbs==null) nulls++;
							ins.add(fbs);
						}
						if(nulls==ins.size()){ //all values null -> recursion end
							return null;
						}else if(ins.size()==2&&nulls==1){
							if(ins.get(0)==null){
								return ins.get(1);
							}else{
								return ins.get(0);
							}
						}else{
							FixedBitSet[] insArray = new FixedBitSet[0];
							insArray=ins.toArray(insArray);
							return this.out.eval(null,insArray);
						}
					}else if(this.name=="concatenator"){
						FixedBitSet outFBS= new FixedBitSet(0);
						for (int i = 0; i < this.in.size(); i++) {
							outFBS.concat(this.in.get(i).eval(inFct,inValues));
						}
						return outFBS;
					}else if(this.name=="selector"){
						int tx=this.x;
						int ty=this.y;
						FixedBitSet outFBS = this.out.eval(inFct,inValues);;
						if(outFBS!=null){
							if (tx<0) tx=outFBS.length()+tx;//negative bit selects indexing from beginning or end
							if (ty<0) ty=outFBS.length()+ty;//-1=last,-2=previous...
							return outFBS.get(tx,ty);
						}else{
							return null;
						}
					}else{//function (different from nand)
						return this.out.eval(this.in,inValues);
					}
						
				}
			}
			public void print(){//FIXME: add selectors,concatenators
				if(this.out!=null){
					if(this.in.size()==2){
						System.out.print("(");
						this.in.get(0).printNode(this.in);
						System.out.print(") ");
						System.out.print(this.name);
						System.out.print(" (");
						this.in.get(1).printNode(this.in);
						System.out.print(") = ");
					}else if(this.in.size()==1){
						System.out.print(this.name);
						System.out.print(" (");
						this.in.get(0).printNode(this.in);
						System.out.print(") = ");
					}
					this.out.printNode(this.in);
				}else{
					System.out.print("(");
					this.in.get(0).printNode(this.in);
					System.out.print(") ");
					System.out.print(this.name);
					System.out.print(" (");
					this.in.get(1).printNode(this.in);
					System.out.print(")");
				}
				System.out.println();
			}
			public void printNode(ArrayList<Function> in){
				if(this.out!=null){
					if(this.in.size()==2){
						System.out.print("(");
						this.in.get(0).printNode(in);
						System.out.print(") ");
						System.out.print(this.out.name);
						System.out.print(" (");
						this.in.get(1).printNode(in);
						System.out.print(")");
					}else if(this.in.size()==1){
						System.out.print(this.out.name);
						System.out.print(" (");
						this.in.get(0).printNode(in);
						System.out.print(")");
					}
				}else{
					System.out.print(in.indexOf(this));
				}
			}
			public Function set(int i, int j) {
				//create selector node for get(x,y) of bitset
				Function fct = new Function();
				fct.x=i;
				fct.y=j;
				fct.name="selector";
				fct.out=this;
				return fct;
			}
			public Function cc(Function ... functions){
				//create concatenator node
				ArrayList<Function> in = new ArrayList<Function>();
				for (int i = 0; i < functions.length; i++){
					in.add( functions [i]);
				}
				Function fct = new Function();
				fct.in=in;
				fct.out=fct;//FIXME: hack: not null so not identified as nand in evaluation
				fct.name="concatenator";
				this.out=fct;
				return fct;
			}
		}
