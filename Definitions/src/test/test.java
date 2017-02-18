/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package test;

import java.util.BitSet;

import vo.Definition;
import vo.DefinitionDB;
import vo.Node;

//IMPLEMENTATION:
//-choice to encumber classes with attributes not always needed, for simplicity and readability
//-focus on algorithmic optimization
//-can't have definition outside of DefinitionDB (or else inconsistencies)

//////////////////////////////////////////////////////
//HERE BE DRAGONS (maybe not in Java from here)
//////////////////////////////////////////////////////
//-optimize data structures

class test {
    public static void main(String[] args) {
    	//PROBANDO
//    	String A="0";
//    	String B="1";
//    	String C="0";
//    	String A="00";
//    	String B="01";
//    	String C="01";
    	String A="00001111";
    	String B="00110011";
    	String C="01010101";
    	String D="1";
    	String E="01";
    	
		//=========================================================================================================================================
    	//DEFINITIONS AND DEFINITIONDB//
		//=========================================================================================================================================
    	
    	//NAND definition//
    	Definition nand = new Definition(2,1,"nand");
    	nand.printEval(A,B);      
    	nand.printCost();
        //Declare DATABASE
        DefinitionDB  definitionDB = new DefinitionDB(nand);
//        nand.printEval(A,B);
//        System.out.print(definitionDB.toString());
        
        //NOT definition//
    	Definition not = new Definition(1,1,"not");
    	not.add(nand,not.in.get(0),not.in.get(0),not.out.get(0));
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(not.toString());
    	not.printCost();
    	not.printEval(A);
     	definitionDB.put("not",not);
    	System.out.print("Optimized definition: \n");
    	System.out.print(not.toString());
     	not.printCost();
     	not.printEval(A);
     	System.out.print(definitionDB.toString());
       
     	//AND definition//
    	Definition and = new Definition(2,1,"and");
    	Node and0= new Node();
    	and.add(nand,and.in.get(0),and.in.get(1),and0);
    	and.add(not,and0,and.out.get(0));
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(and.toString());
    	and.printCost();
    	and.printEval(A,B);
     	definitionDB.put("and",and);
     	System.out.print("Optimized definition: \n");
    	System.out.print(and.toString());
     	and.printCost();
     	and.printEval(A,B);
     	System.out.print(definitionDB.toString());
     
     	//OR definition//
    	Definition or = new Definition(2,1,"or");
    	Node or0 = new Node();
    	Node or1 = new Node();
    	or.add(not,or.in.get(0),or0);
    	or.add(not,or.in.get(1),or1);
    	or.add(nand, or0,or1,or.out.get(0));
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(or.toString());
    	or.printCost();
    	or.printEval(A,B);
     	definitionDB.put("or",or);
     	System.out.print("Optimized definition: \n");
    	System.out.print(or.toString());
     	or.printCost();
     	or.printEval(A,B);
     	System.out.print(definitionDB.toString());
    
//     	//XOR definition// v1
//    	Definition xor = new Definition(2,1,"xor");
//    	Node xor0 = new Node();
//    	Node xor1 = new Node();
//    	xor.add(or, xor.in.get(0),xor.in.get(1),xor0);
//    	xor.add(nand, xor.in.get(0),xor.in.get(1),xor1);
//    	xor.add(and,xor0,xor1,xor.out.get(0)); 
//    	System.out.println();
//    	System.out.print("New definition: \n");
//    	System.out.print(xor.toString());
//    	xor.printCost();
//    	xor.printEval(A,B);
//    	definitionDB.put("xor",xor);
//     	System.out.print("Optimized definition: \n");
//    	System.out.print(xor.toString());
//    	xor.printCost();
//    	xor.printEval(A,B);
//    	System.out.print(definitionDB.toString());
    	
    	//XOR definition// v2
    	Definition xor = new Definition(2,1,"xor");
    	Node xor0 = new Node();
    	Node xor1 = new Node();
    	Node xor2 = new Node();
    	xor.add(nand, xor.in.get(0),xor.in.get(1),xor0);
    	xor.add(nand,xor0,xor.in.get(0),xor1); 
    	xor.add(nand,xor0,xor.in.get(1),xor2); 
    	xor.add(nand,xor1,xor2,xor.out.get(0));
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(xor.toString());
    	xor.printCost();
    	xor.printEval(A,B);
    	definitionDB.put("xor",xor);
     	System.out.print("Optimized definition: \n");
    	System.out.print(xor.toString());
    	xor.printCost();
    	xor.printEval(A,B);
    	System.out.print(definitionDB.toString());
    
    	//IF definition//
    	//MUX
    	//if a then b else c = (¬A^B)v(A^C) !!!ELSE ALWAYS NEEDED!!! 0=false 1=true //THERE WAS AN ERROR HERE
    	Definition ifDef = new Definition(3,1,"if");
    	Node ifdef0 = new Node();
    	Node ifdef1 = new Node();
    	Node ifdef2 = new Node();
    	ifDef.add(not,ifDef.in.get(0),ifdef0);
    	ifDef.add(and,ifdef0,ifDef.in.get(1),ifdef1);
    	ifDef.add(and,ifDef.in.get(0),ifDef.in.get(2),ifdef2);
    	ifDef.add(or,ifdef1,ifdef2,ifDef.out.get(0));
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(ifDef.toString());
    	ifDef.printCost();
    	ifDef.printEval(A,B,C);
    	definitionDB.put("if",ifDef);
     	System.out.print("Optimized definition: \n");
    	System.out.print(ifDef.toString());
    	ifDef.printCost();
    	ifDef.printEval(A,B,C);
    	System.out.print(definitionDB.toString());
    	
    	//recursive IF definition//
    	//if a then b else c = (¬AvB)^(AvC)
    	//size(a)=1 size(b)=n size(c)=m
    	Definition rifDef = new Definition(3,1,"rif");//recursive ,bit a bit, def of if (conditional is 1 bit long)
    	Node restButLastA = new Node();
    	Node lastA = new Node();
    	Node restButLastB = new Node();
    	Node lastB = new Node();
    	Node rifdef1 = new Node();
    	Node rifdef2 = new Node();
    	rifDef.in.get(1).setRestChildren(restButLastA);
    	rifDef.in.get(1).setLastChild(lastA);
    	rifDef.in.get(2).setRestChildren(restButLastB);
    	rifDef.in.get(2).setLastChild(lastB);
    	rifDef.add(rifDef,rifDef.in.get(0),restButLastA,restButLastB,rifdef1);
    	rifDef.add(ifDef,rifDef.in.get(0),lastA,lastB,rifdef2);
    	rifDef.out.get(0).setRestParents(rifdef1);
    	rifDef.out.get(0).setLastParent(rifdef2);
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(rifDef.toString());
    	rifDef.printCost();
    	rifDef.printEval(D,B,C);
    	definitionDB.put("rif",rifDef);
     	System.out.print("Optimized definition: \n");
    	System.out.print(rifDef.toString());
     	rifDef.printCost();
    	rifDef.printEval(D,B,C);
    	System.out.print(definitionDB.toString());
    	
//    	//RECURSIVE XOR //TODO: should optimize to simply XOR
//    	Definition rxor = new Definition(4);
//    	rxor.addOut(rxor.add(rxor,rxor.in.get(0),rxor.in.get(2)).out.get(0));
//    	rxor.addOut(rxor.add(xor,rxor.in.get(1),rxor.in.get(3)).out.get(0));
    	
    	//SUM full adder definition
    	Definition sum = new Definition(3,2,"sum");
    	Node t1 = new Node();
    	Node t2 = new Node();
    	Node t3 = new Node();
    	Node t4 = new Node();
    	sum.add(xor, sum.in.get(0),sum.in.get(1),t1);
    	sum.add(and, sum.in.get(0),sum.in.get(1),t2);
    	sum.add(xor, sum.in.get(0),sum.in.get(1),t4);
    	sum.add(and, t1,sum.in.get(2),t3);
    	sum.add(xor, t4,sum.in.get(2),sum.out.get(0));
    	sum.add(or, t2,t3,sum.out.get(1));
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(sum.toString());
    	sum.printCost();
    	sum.printEval(A,B,C);
    	definitionDB.put("sum",sum);
     	System.out.print("Optimized definition: \n");
    	System.out.print(sum.toString());
    	sum.printCost();
    	sum.printEval(A,B,C);
    	System.out.print(definitionDB.toString());
    	
    	//sumR definition
    	Definition sumR = new Definition(3,2,"sumR");
    	Node op1SumR = sumR.in.get(0);
    	Node op2SumR = sumR.in.get(1);
    	Node cIn = sumR.in.get(2);
    	Node sSumR = sumR.out.get(0);
    	Node cSumR = sumR.out.get(1);
    	Node cRest = new Node();
    	Node cn = new Node();
    	Node srestSumR = new Node();
    	Node snSumR = new Node();
    	sSumR.setRestParents(srestSumR);
    	sSumR.setLastParent(snSumR);
    	cSumR.setRestParents(cRest);
    	cSumR.setLastParent(cn);
    	sumR.in.get(0).setRestChildren(new Node());
    	sumR.in.get(0).setLastChild(new Node());
    	sumR.in.get(1).setRestChildren(new Node());
    	sumR.in.get(1).setLastChild(new Node());
    	sumR.add(sum,sumR.in.get(0).getLastChild(),sumR.in.get(1).getLastChild(),cIn,snSumR,cRest);
    	sumR.add(sumR,sumR.in.get(0).getRestChildren(),sumR.in.get(1).getRestChildren(),cRest,srestSumR,cn);
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(sumR.toString());
    	sumR.printCost();
    	sumR.printEval(A,B,D);
    	definitionDB.put("sumR",sumR);
     	System.out.print("Optimized definition: \n");
    	System.out.print(sumR.toString());
    	sumR.printCost();
    	sumR.printEval(A,B,D);
    	System.out.print(definitionDB.toString());
    	
    	//ADD definition
    	Definition add = new Definition(2,1,"add");
    	add.in.get(0).setRestChildren(new Node());
    	add.in.get(0).setLastChild(new Node());
    	add.in.get(1).setRestChildren(new Node());
    	add.in.get(1).setLastChild(new Node());
    	Node cin = new Node();
    	Node sn = new Node();
    	Node s = new Node();
    	Node cout= new Node();
    	Node rest = new Node();
    	cout.setLastChild(new Node());
    	rest.setRestParents(cout.getLastChild());
    	rest.setLastParent(s);
    	add.out.get(0).setRestParents(rest);
    	add.out.get(0).setLastParent(sn);
    	add.add(and,add.in.get(0).getLastChild(),add.in.get(1).getLastChild(),cin);
    	add.add(xor,add.in.get(0).getLastChild(),add.in.get(1).getLastChild(),sn);
    	add.add(sumR,add.in.get(0).getRestChildren(),add.in.get(1).getRestChildren(),cin,s,cout);
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(add.toString());
    	add.printCost();
    	add.printEval(A,B);
    	add.printEval(D,E);
//    	add.testExpand();
    	definitionDB.put("add",add);
     	System.out.print("Optimized definition: \n");
    	System.out.print(add.toString());
    	add.printCost();
    	add.printEval(A,B);
    	add.printEval(D,E);
    	System.out.print(definitionDB.toString());
    	
    	//andR definition
    	//andR[0,1;2(3&4)]=
    	//	and[0,1{n};4] andR[0,1{1..n-1};3]
    	Definition andR = new Definition(2,1,"andR");
    	Node andAndR = new Node();
    	Node andRandR = new Node();
    	andR.out.get(0).setRestParents(andRandR);
    	andR.out.get(0).setLastParent(andAndR);
    	andR.in.get(1).setRestChildren(new Node());
    	andR.in.get(1).setLastChild(new Node());
    	andR.add(and,andR.in.get(0),andR.in.get(1).getLastChild(),andAndR);
    	andR.add(andR,andR.in.get(0),andR.in.get(1).getRestChildren(),andRandR);
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(andR.toString());
    	andR.printCost();
    	andR.printEval(A,D);
    	definitionDB.put("andR",andR);
     	System.out.print("Optimized definition: \n");
    	System.out.print(andR.toString());
    	andR.printCost();
    	andR.printEval(A,D);
    	System.out.print(definitionDB.toString());
    	
    	//mulR definition
//    	mulR[0,1,2;3(7&6{1..n-1}),4(8&6{n})]=
//    			andR[1{n},0;5]
//    			add[2,5;6]  
//    			mulR[0,1{1..n-1},6{1..n-1};7,8]
    	Definition mulR = new Definition(3,2,"mulR");
    	Node op1MulR = mulR.in.get(0);
    	Node op2MulR = mulR.in.get(1);
    	Node cInMulR = mulR.in.get(2);
    	Node outArrayMulR = mulR.out.get(0);
    	Node mulMulR = mulR.out.get(1);
    	Node mulRestMulR = new Node();
    	Node andMulR = new Node();
    	Node addMulR = new Node();
    	Node restArray = new Node();
    	addMulR.setRestChildren(new Node());
    	addMulR.setLastChild(new Node());
    	op2MulR.setRestChildren(new Node());
    	op2MulR.setLastChild(new Node());
    	mulMulR.setRestParents(mulRestMulR);
    	mulMulR.setLastParent(addMulR.getLastChild());
    	outArrayMulR.setRestParents(restArray);
    	outArrayMulR.setLastParent(addMulR.getLastChild());
    	
    	mulR.add(and,op2MulR.getLastChild(),op1MulR,andMulR);
    	mulR.add(add,cInMulR,andMulR,addMulR);
    	mulR.add(mulR,op1MulR,op2MulR.getRestChildren(),addMulR.getRestChildren(),restArray,mulRestMulR);
    	
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(mulR.toString());
    	mulR.printCost();
    	mulR.printEval(A,B,D);
    	definitionDB.put("mulR",mulR);
     	System.out.print("Optimized definition: \n");
    	System.out.print(mulR.toString());
    	mulR.printCost();
    	mulR.printEval(A,B,D);
    	System.out.print(definitionDB.toString());
    	
    	//mul definition
//    	mul[0,1,2(4{n}&5&3{n})] =
//    			andR[1{n},0;3]
//    			mulR[0,1{1..n-1},3{1..n-1};4,5]
    	Definition mul = new Definition(3,1,"mul");
    	Node op1Mul = mul.in.get(0);
    	Node op2Mul = mul.in.get(1);
    	Node mulMul = mul.out.get(0);
    	Node mulRestMul = new Node();
    	Node cOutArrayMul = new Node();
    	Node andMul = new Node();
    	Node mulRest = new Node();
    	op2Mul.setRestChildren(new Node());
    	op2Mul.setLastChild(new Node());
    	andMul.setRestChildren(new Node());
    	andMul.setLastChild(new Node());
    	cOutArrayMul.setLastChild(new Node());
    	mulRest.setRestChildren(cOutArrayMul);
    	mulRest.setLastChild(mulRestMul);
    	mulMul.setRestParents(mulRest);
    	mulMul.setLastParent(andMul.getLastChild());
    	
    	mul.add(andR, op1Mul,op2Mul.getLastChild(),andMul);
    	mul.add(mulR,op1Mul,op2Mul.getRestChildren(),andMul.getRestChildren(),cOutArrayMul,mulRestMul);
    	
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(mul.toString());
    	mul.printCost();
    	mul.printEval(A,B,D);
    	definitionDB.put("mul",mul);
     	System.out.print("Optimized definition: \n");
    	System.out.print(mul.toString());
    	mul.printCost();
    	mul.printEval(A,B,D);
    	System.out.print(definitionDB.toString());
    	
    	
    	//mADD modular add definition//
    	// add[0,1;2(17&11{2})] =
    	// xor [0,1;11]  and [0{1..n-1},1{1..n-1};16] 
    	//		 add [11{1..n-1},16;17] 
    	Definition mAdd = new Definition(2,1,"mAdd");
    	Node xorOut = new Node();
    	mAdd.add(xor, mAdd.in.get(0),mAdd.in.get(1),xorOut);
    	Node addXorRest = new Node();
    	xorOut.setRestChildren(addXorRest);
    	Node addXorLast = new Node();
    	xorOut.setLastChild(addXorLast);
    	Node addAndOut = new Node();
    	mAdd.add(and, mAdd.in.get(0), mAdd.in.get(1),addAndOut);
    	Node addOut = new Node();
    	mAdd.add(mAdd, addXorRest,addAndOut,addOut);
    	mAdd.out.get(0).setRestChildren(addOut);
    	mAdd.out.get(0).setLastChild(addXorLast);
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(mAdd.toString());
    	mAdd.printCost();
    	mAdd.printEval(A,B);
    	definitionDB.put("mAdd",mAdd);
     	System.out.print("Optimized definition: \n");
    	System.out.print(mAdd.toString());
    	mAdd.printCost();
    	mAdd.printEval(A,B);//FIXME
    	System.out.print(definitionDB.toString());
    	
    	//ZEROS definition////logic definition of zero values
    	Definition zeros = new Definition(1,1,"zeros");
    	zeros.add(xor,zeros.in.get(0),zeros.in.get(0),zeros.out.get(0));
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(zeros.toString());
    	zeros.printCost();
    	zeros.printEval(A);
    	definitionDB.put("zeros",zeros);
     	System.out.print("Optimized definition: \n");
    	System.out.print(zeros.toString());
    	zeros.printCost();
    	zeros.printEval(A);
    	System.out.print(definitionDB.toString());
    	
    	//ONES definition////logic definition of one values
    	Definition ones = new Definition(1,1,"ones");
    	Node zeroNode = new Node();
    	ones.add(zeros, ones.in.get(0),zeroNode);
    	ones.add(not,zeroNode,ones.out.get(0));
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(ones.toString());
    	ones.printCost();
    	ones.printEval(A);
    	definitionDB.put("ones",ones);
     	System.out.print("Optimized definition: \n");
    	System.out.print(ones.toString());
    	ones.printCost();
    	ones.printEval(A);//FIXME!!!
    	System.out.print(definitionDB.toString());
    	
    	//DEC definition////definition to decrement an integer by one
    	//dec [0;1] = not[0{2},2] rif [0{2},(0{0}&0{1}&2),(8&2;1] dec [(0{0}&0{1});8]//TODO: fix to string incorrect subnodes + to look like this
    	Definition dec = new Definition(1,1,"dec");
//    	Node dec0 = dec.in.get(0).childSubnodes.get(0);
//    	Node decRest = dec.in.get(0).childSubnodes.get(1);
    	Node decRestButLast = new Node();
    	dec.in.get(0).setRestChildren(decRestButLast);
    	Node decLast =  new Node();
    	dec.in.get(0).setLastChild(decLast);
    	Node decNnot = new Node();
    	dec.add(not, decLast,decNnot);
    	Node decRout= new Node();
    	dec.add(dec,decRestButLast,decRout);
    	Node decRif = new Node();
    	dec.add(rifDef,decLast,decRout,decRestButLast,decRif);
    	dec.out.get(0).setRestChildren(decRif);
    	dec.out.get(0).setLastChild(decNnot);
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(dec.toString());
    	dec.printCost();
    	dec.printEval(A);
    	definitionDB.put("dec",dec);
     	System.out.print("Optimized definition: \n");
    	System.out.print(dec.toString());
    	dec.printCost();
//    	dec.printEval(A);//FIXME: intersection with recursion not optimized TODO: fix optimization
    	System.out.print(definitionDB.toString());
    	
    	//CMP definition////definition to test if two values are equal, returns a bit
    	Definition cmp = new Definition(2,1,"cmp");
    	Node cmpAlast =  new Node();
    	cmp.in.get(0).setLastChild(cmpAlast);
    	Node cmpArest =  new Node();
    	Node cmpBlast =  new Node();
    	cmp.in.get(1).setLastChild(cmpBlast);
    	Node cmpBrest =  new Node();
    	cmp.in.get(1).setRestChildren(cmpBrest);
    	Node cmpXor = new Node();
    	cmp.add(xor, cmpAlast,cmpBlast,cmpXor);
    	Node cmpR = new Node();
    	cmp.add(cmp, cmpArest,cmpBrest,cmpR);
    	Node cmpXnor = new Node();
    	cmp.add(not, cmpXor,cmpXnor);
    	cmp.add(and, cmpXnor,cmpR,cmp.out.get(0)); //should cmp.add(ifDef, cmpXor,cmpXnor,cmpR,cmp.out.get(0)); work? cmp.add(and, cmpXnor,cmpR,cmp.out.get(0)); simple
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(cmp.toString());
    	cmp.printCost();
    	cmp.printEval(A,A);
    	cmp.printEval(A,B);
    	definitionDB.put("cmp",cmp);
     	System.out.print("Optimized definition: \n");
    	System.out.print(cmp.toString());
    	cmp.printCost();
    	cmp.printEval(A,A);
    	cmp.printEval(A,B);
    	System.out.print(definitionDB.toString());
    	
    	//EQ0 definition////definition to test if a value is zero
    	Definition eq0 = new Definition(1,1,"eq0");
    	Node eq=new Node();
    	eq0.add(zeros,eq0.in.get(0),eq);
    	eq0.add(cmp,eq,eq0.in.get(0),eq0.out.get(0));
    	System.out.println();
    	System.out.print("New definition: \n");
    	System.out.print(eq0.toString());
    	eq0.printCost();
    	eq0.printEval(A);
    	eq0.printEval(B);
    	definitionDB.put("eq0",eq0);
     	System.out.print("Optimized definition: \n");
    	System.out.print(eq0.toString());
    	eq0.printCost();
    	eq0.printEval(A);
    	eq0.printEval(B);
    	System.out.print(definitionDB.toString());
    	
//    	//forDef definition////for loop definition
//    	Definition forDef = new Definition(2,1,"for");
//    	Node forDef0 = new Node();
//    	Node forDef1 = new Node();
//    	forDef.add(zeros,forDef.in.get(1),forDef1);
//    	forDef.add(forDef,forDef.in.get(0),forDef.in.get(1),forDef0);
//    	forDef.add(ifDef,forDef.in.get(0),forDef0,forDef1,forDef.out.get(0));
//    	definitionDB.put("for",forDef);
//    	System.out.print(definitionDB.toString());
    	
//    	//MUL definition//
//    	Definition mul = new Definition(2,1,"mul");
//    	Node mul0 = new Node();
//    	Node mul1 = new Node();
//    	Node mul2 = new Node();
//    	Node mul3 = new Node();
//    	mul.add(eq0,mul.in.get(0),mul0);
//    	mul.add(dec,mul.in.get(0),mul1);
//    	mul.add(mul,mul1,mul.in.get(1),mul2);
//    	mul.add(add,mul2,mul.in.get(1),mul3);
//    	mul.add(rifDef,mul0,mul.in.get(0),mul3,mul.out.get(0));//TODO:Test with ifdef and rifdef
//    	System.out.println();
//    	System.out.print("New definition: \n");
//    	System.out.print(mul.toString());
//    	mul.printCost();
//    	mul.printEval(B,B);
//    	definitionDB.put("mul",mul);
//     	System.out.print("Optimized definition: \n");
//    	System.out.print(mul.toString());
//    	mul.printCost();
//    	mul.printEval(B,B);
//    	System.out.print(definitionDB.toString());
    	
//    	//SQRT definition//
//    	Definition sqrt = new Definition(1,1,"sqrt");
//    	Node counter = new Node();
//    	Node square = new Node();
//    	Node sqrtComp = new Node();
//    	sqrt.add(mul,counter,counter,square);
//    	sqrt.add(cmp,square,)
//    	sqrt.add(ifdef,);
//    	System.out.println();
//    	System.out.print("New definition: \n");
//    	System.out.print(sqrt.toString());
////    	sqrt.printEval(B,B);
//    	definitionDB.put("sqrt",sqrt);
//    	sqrt.printEval(B,B);
//    	System.out.print(definitionDB.toString());
    	
    }
}
