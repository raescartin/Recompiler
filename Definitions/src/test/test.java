/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package test;

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
    	String A="1";
    	String B="0";
    	String C="1";
    	String D="1";
    	
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
    	System.out.print("New definition: ");
    	System.out.print(not.toString());
    	not.printCost();
    	not.printEval(A);
     	definitionDB.put("not",not);
    	System.out.print("Optimized definition: ");
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
    	System.out.print("New definition: ");
    	System.out.print(and.toString());
    	and.printCost();
    	and.printEval(A,B);
     	definitionDB.put("and",and);
     	System.out.print("Optimized definition: ");
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
    	System.out.print("New definition: ");
    	System.out.print(or.toString());
    	or.printCost();
    	or.printEval(A,B);
     	definitionDB.put("or",or);
     	System.out.print("Optimized definition: ");
    	System.out.print(or.toString());
     	or.printCost();
     	or.printEval(A,B);
     	System.out.print(definitionDB.toString());
    
     	//XOR definition//
    	Definition xor = new Definition(2,1,"xor");
    	Node xor0 = new Node();
    	Node xor1 = new Node();
    	xor.add(or, xor.in.get(0),xor.in.get(1),xor0);
    	xor.add(nand, xor.in.get(0),xor.in.get(1),xor1);
    	xor.add(and,xor0,xor1,xor.out.get(0)); 
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(xor.toString());
    	xor.printCost();
    	xor.printEval(A,B);
    	definitionDB.put("xor",xor);
     	System.out.print("Optimized definition: ");
    	System.out.print(xor.toString());
    	xor.printCost();
    	xor.printEval(A,B);
    	System.out.print(definitionDB.toString());
    
    	//IF definition//
    	//if a then b else c = (¬AvB)^(AvC) !!!ELSE ALWAYS NEEDED!!! 0=false 1=true
    	Definition ifDef = new Definition(3,1,"if");
    	Node ifdef0 = new Node();
    	Node ifdef1 = new Node();
    	Node ifdef2 = new Node();
    	ifDef.add(not,ifDef.in.get(0),ifdef0);
    	ifDef.add(or,ifdef0,ifDef.in.get(1),ifdef1);
    	ifDef.add(or,ifDef.in.get(0),ifDef.in.get(2),ifdef2);
    	ifDef.add(and,ifdef1,ifdef2,ifDef.out.get(0));
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(ifDef.toString());
    	ifDef.printCost();
    	ifDef.printEval(A,B,C);
    	definitionDB.put("if",ifDef);
     	System.out.print("Optimized definition: ");
    	System.out.print(ifDef.toString());
    	ifDef.printCost();
    	ifDef.printEval(A,B,C);
    	System.out.print(definitionDB.toString());
    	
    	//recursive IF definition//
    	//if a then b else c = (¬AvB)^(AvC)
    	//size(a)=1 size(b)=n size(c)=m
    	Definition rifDef = new Definition(3,1,"rif");//recursive ,bit a bit, def of if (0 is 1 bit long)
    	Node rifdef0 = new Node();
    	Node rifdef1 = new Node();
    	Node rifdef2 = new Node();
    	Node rifdef3 = new Node();
    	Node rifdef4 = new Node();
    	Node rifdef5 = new Node();
    	Node rifdef6 = new Node();
    	Node rifdef7 = new Node();
    	Node rifdef8 = new Node();
    	Node rifdef9 = new Node();
    	Node rifdef10 = new Node();
    	Node rifdef11 = new Node();
    	Node rifdef12 = new Node();
    	rifDef.in.get(1).addChildSubnode(rifdef3);
    	rifDef.in.get(1).addChildSubnode(rifdef4);
    	rifDef.in.get(1).addChildSubnode(rifdef5);
    	rifDef.in.get(2).addChildSubnode(rifdef6);
    	rifDef.in.get(2).addChildSubnode(rifdef7);
    	rifDef.in.get(2).addChildSubnode(rifdef8);
    	rifDef.add(not,rifDef.in.get(0),rifdef0);
    	rifDef.add(or,rifdef0,rifdef5,rifdef1);
    	rifDef.add(or,rifDef.in.get(0),rifdef8,rifdef2);
    	rifDef.add(and,rifdef1,rifdef2,rifdef9);//rightmost bit of if computed
    	rifdef3.addChildSupernode(rifdef10);
    	rifdef4.addChildSupernode(rifdef10);
    	rifdef6.addChildSupernode(rifdef11);
    	rifdef7.addChildSupernode(rifdef11);
    	rifDef.add(rifDef,rifDef.in.get(0),rifdef10,rifdef11,rifdef12);
    	rifdef12.addChildSupernode(rifDef.out.get(0));
    	rifdef9.addChildSupernode(rifDef.out.get(0));
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(rifDef.toString());
    	rifDef.printCost();
    	rifDef.printEval(D,B,C);
    	definitionDB.put("rif",rifDef);
     	System.out.print("Optimized definition: ");
    	System.out.print(rifDef.toString());
     	rifDef.printCost();
    	rifDef.printEval(D,B,C);
    	System.out.print(definitionDB.toString());
    	
//    	//RECURSIVE XOR //TODO: should optimize to simply XOR
//    	Definition rxor = new Definition(4);
//    	rxor.addOut(rxor.add(rxor,rxor.in.get(0),rxor.in.get(2)).out.get(0));
//    	rxor.addOut(rxor.add(xor,rxor.in.get(1),rxor.in.get(3)).out.get(0));
    	
    	//ADD definition//
    	// a add b = (a0..n-1 xor b0..n-1) add (a1..n and b1..n) && (an xor bn) 
    	Definition add = new Definition(2,1,"add");
    	add.in.get(0).splitChildrenSubnodes();
    	@SuppressWarnings("unused")
		Node A0 = add.in.get(0).childrenSubnodes.get(0);
    	Node Arest = add.in.get(0).childrenSubnodes.get(1);
    	Node An = add.in.get(0).childrenSubnodes.get(2);
    	add.in.get(1).splitChildrenSubnodes();
    	@SuppressWarnings("unused")
		Node B0 = add.in.get(1).childrenSubnodes.get(0);
    	Node Brest = add.in.get(1).childrenSubnodes.get(1);
    	Node Bn = add.in.get(1).childrenSubnodes.get(2);
    	Node Awithout0 = new Node();
    	Arest.addChildSupernode(Awithout0);
    	An.addChildSupernode(Awithout0);
    	Node Bwithout0 = new Node();
    	Brest.addChildSupernode(Bwithout0);
    	Bn.addChildSupernode(Bwithout0);
    	Node xorOut = new Node();
    	add.add(xor, add.in.get(0),add.in.get(1),xorOut);
    	Node addxor0 = new Node();
    	Node xorrest = new Node();
    	Node xorn = new Node();
    	xorOut.addChildSubnode(addxor0);
    	xorOut.addChildSubnode(xorrest);
    	xorOut.addChildSubnode(xorn);
    	Node xorwithoutN = new Node();
    	addxor0.addChildSupernode(xorwithoutN);
    	xorrest.addChildSupernode(xorwithoutN);
    	Node addAndOut = new Node();
    	add.add(and, Awithout0,Bwithout0,addAndOut);
    	Node addOut = new Node();
    	add.add(add, xorwithoutN,addAndOut,addOut);
    	addOut.addChildSupernode(add.out.get(0));
    	xorn.addChildSupernode(add.out.get(0));
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(add.toString());
    	add.printCost();
    	add.printEval(A,B);
    	definitionDB.put("add",add);
     	System.out.print("Optimized definition: ");
    	System.out.print(add.toString());
    	add.printCost();
    	add.printEval(A,B);//FIXME
    	System.out.print(definitionDB.toString());
    	
    	//ZEROS definition////logic definition of zero values
    	Definition zeros = new Definition(1,1,"zeros");
    	zeros.add(xor,zeros.in.get(0),zeros.in.get(0),zeros.out.get(0));
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(zeros.toString());
    	zeros.printCost();
    	zeros.printEval(A);
    	definitionDB.put("zeros",zeros);
     	System.out.print("Optimized definition: ");
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
    	System.out.print("New definition: ");
    	System.out.print(ones.toString());
    	ones.printCost();
    	ones.printEval(A);
    	definitionDB.put("ones",ones);
     	System.out.print("Optimized definition: ");
    	System.out.print(ones.toString());
    	ones.printCost();
    	ones.printEval(A);
    	System.out.print(definitionDB.toString());
    	
    	//DEC definition////definition to decrement an integer by one
    	//dec [0;1] = not[0{2},2] rif [0{2},(0{0}&0{1}&2),(8&2;1] dec [(0{0}&0{1});8]//TODO: fix to string incorrect subnodes + to look like this
    	Definition dec = new Definition(1,1,"dec");
    	dec.in.get(0).splitChildrenSubnodes();
    	Node dec0 = dec.in.get(0).childrenSubnodes.get(0);
    	Node decRest = dec.in.get(0).childrenSubnodes.get(1);
    	Node decN = dec.in.get(0).childrenSubnodes.get(2);
    	Node decNnot = new Node();
    	dec.add(not, decN,decNnot);
    	Node decR= new Node(); 
    	dec0.addChildSupernode(decR);
    	decRest.addChildSupernode(decR);
    	Node decRout= new Node();
    	dec.add(dec,decR,decRout);
    	Node decRif = new Node();
    	dec.add(rifDef,decN,decR,decRout,decRif);
    	decRif.addChildSupernode(dec.out.get(0));
    	decNnot.addChildSupernode(dec.out.get(0));
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(dec.toString());
    	dec.printCost();
    	dec.printEval(A);
    	definitionDB.put("dec",dec);
     	System.out.print("Optimized definition: ");
    	System.out.print(dec.toString());
    	dec.printCost();
//    	dec.printEval(A);//FIXME: intersection with recursion not optimized TODO: fix optimization
    	System.out.print(definitionDB.toString());
    	
    	//CMP definition////definition to test if two values are equal, returns a bit
    	Definition cmp = new Definition(2,1,"cmp");
    	cmp.in.get(0).splitChildrenSubnodes();
    	Node cmpA0 = cmp.in.get(0).childrenSubnodes.get(0);
    	Node cmpARest = cmp.in.get(0).childrenSubnodes.get(1);
    	Node cmpAN = cmp.in.get(0).childrenSubnodes.get(2);
    	cmp.in.get(1).splitChildrenSubnodes();
    	Node cmpB0 = cmp.in.get(1).childrenSubnodes.get(0);
    	Node cmpBRest = cmp.in.get(1).childrenSubnodes.get(1);
    	Node cmpBN = cmp.in.get(1).childrenSubnodes.get(2);
    	Node cmpAwithoutN = new Node();
    	cmpA0.addChildSupernode(cmpAwithoutN);
    	cmpARest.addChildSupernode(cmpAwithoutN);
    	Node cmpBwithoutN = new Node();
    	cmpB0.addChildSupernode(cmpBwithoutN);
    	cmpBRest.addChildSupernode(cmpBwithoutN);
    	Node cmpXor = new Node();
    	cmp.add(xor, cmpAN,cmpBN,cmpXor);
    	Node cmpR = new Node();
    	cmp.add(cmp, cmpAwithoutN,cmpBwithoutN,cmpR);
    	Node cmpXnor = new Node();
    	cmp.add(not, cmpXor,cmpXnor);
    	cmp.add(and, cmpXnor,cmpR,cmp.out.get(0)); //should cmp.add(ifDef, cmpXor,cmpXnor,cmpR,cmp.out.get(0)); work? cmp.add(and, cmpXnor,cmpR,cmp.out.get(0)); simple
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(cmp.toString());
    	cmp.printCost();
    	cmp.printEval(A,A);
    	cmp.printEval(A,B);
    	definitionDB.put("cmp",cmp);
     	System.out.print("Optimized definition: ");
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
    	System.out.print("New definition: ");
    	System.out.print(eq0.toString());
    	eq0.printCost();
    	eq0.printEval(A);
    	eq0.printEval(B);
    	definitionDB.put("eq0",eq0);
     	System.out.print("Optimized definition: ");
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
    	
    	//MUL definition//
    	Definition mul = new Definition(2,1,"mul");
    	Node mul0 = new Node();
    	Node mul1 = new Node();
    	Node mul2 = new Node();
    	Node mul3 = new Node();
    	mul.add(eq0,mul.in.get(0),mul0);
    	mul.add(dec,mul.in.get(0),mul1);
    	mul.add(mul,mul1,mul.in.get(1),mul2);
    	mul.add(add,mul2,mul.in.get(1),mul3);
    	mul.add(rifDef,mul0,mul.in.get(0),mul3,mul.out.get(0));//TODO:Test with ifdef and rifdef
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(mul.toString());
    	mul.printCost();
    	mul.printEval(B,B);
    	definitionDB.put("mul",mul);
     	System.out.print("Optimized definition: ");
    	System.out.print(mul.toString());
    	mul.printCost();
    	mul.printEval(B,B);
    	System.out.print(definitionDB.toString());
    	
//    	//SQRT definition//
//    	Definition sqrt = new Definition(1,1,"sqrt");
//    	Node counter = new Node();
//    	Node square = new Node();
//    	Node sqrtComp = new Node();
//    	sqrt.add(mul,counter,counter,square);
//    	sqrt.add(cmp,square,)
//    	sqrt.add(ifdef,);
//    	System.out.println();
//    	System.out.print("New definition: ");
//    	System.out.print(sqrt.toString());
////    	sqrt.printEval(B,B);
//    	definitionDB.put("sqrt",sqrt);
//    	sqrt.printEval(B,B);
//    	System.out.print(definitionDB.toString());
    	
    }
}
