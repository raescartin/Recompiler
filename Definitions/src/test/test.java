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
        //Declare DATABASE
        DefinitionDB  definitionDB = new DefinitionDB(nand);
        nand.printEval(A,B);
        System.out.print(definitionDB.toString());
        
        //NOT definition//
    	Definition not = new Definition(1,1,"not");
    	not.add(nand,not.in.get(0),not.in.get(0),not.out.get(0));
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(not.toString());
    	not.printEval(A);
     	definitionDB.put("not",not);
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
    	and.printEval(A,B);
     	definitionDB.put("and",and);
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
    	or.printEval(A,B);
     	definitionDB.put("or",or);
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
    	xor.printEval(A,B);
    	definitionDB.put("xor",xor);
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
    	ifDef.printEval(A,B,C);
    	definitionDB.put("if",ifDef);
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
    	rifDef.in.get(1).add(rifdef3);
    	rifDef.in.get(1).add(rifdef4);
    	rifDef.in.get(1).add(rifdef5);
    	rifDef.in.get(2).add(rifdef6);
    	rifDef.in.get(2).add(rifdef7);
    	rifDef.in.get(2).add(rifdef8);
    	rifDef.add(not,rifDef.in.get(0),rifdef0);
    	rifDef.add(or,rifdef0,rifdef5,rifdef1);
    	rifDef.add(or,rifDef.in.get(0),rifdef8,rifdef2);
    	rifDef.add(and,rifdef1,rifdef2,rifdef9);//rightmost bit of if computed
    	rifdef3.add(rifdef10);
    	rifdef4.add(rifdef10);
    	rifdef6.add(rifdef11);
    	rifdef7.add(rifdef11);
    	rifDef.add(rifDef,rifDef.in.get(0),rifdef10,rifdef11,rifdef12);
    	rifdef12.add(rifDef.out.get(0));
    	rifdef9.add(rifDef.out.get(0));
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(rifDef.toString());
    	rifDef.printEval(D,B,C);
    	definitionDB.put("rif",rifDef);
    	rifDef.printEval(D,B,C);
    	System.out.print(definitionDB.toString());
    	
//    	//RECURSIVE XOR //TODO: should optimize to simply XOR
//    	Definition rxor = new Definition(4);
//    	rxor.addOut(rxor.add(rxor,rxor.in.get(0),rxor.in.get(2)).out.get(0));
//    	rxor.addOut(rxor.add(xor,rxor.in.get(1),rxor.in.get(3)).out.get(0));
    	
    	//ADD definition//
    	// a add b = (a0..n-1 xor b0..n-1) add (a1..n and b1..n) && (an xor bn) 
    	Definition add = new Definition(2,1,"add");
    	@SuppressWarnings("unused")
		Node A0 = add.in.get(0).add(new Node());
    	Node Arest = add.in.get(0).add(new Node());
    	Node An = add.in.get(0).add(new Node());
    	@SuppressWarnings("unused")
		Node B0 = add.in.get(1).add(new Node());
    	Node Brest = add.in.get(1).add(new Node());
    	Node Bn = add.in.get(1).add(new Node());
    	Node Awithout0 = new Node();
    	Arest.add(Awithout0);
    	An.add(Awithout0);
    	Node Bwithout0 = new Node();
    	Brest.add(Bwithout0);
    	Bn.add(Bwithout0);
    	Node xorOut = new Node();
    	add.add(xor, add.in.get(0),add.in.get(1),xorOut);
    	Node addxor0 = new Node();
    	Node xorrest = new Node();
    	Node xorn = new Node();
    	xorOut.add(addxor0);
    	xorOut.add(xorrest);
    	xorOut.add(xorn);
    	Node xorwithoutN = new Node();
    	addxor0.add(xorwithoutN);
    	xorrest.add(xorwithoutN);
    	Node addAndOut = new Node();
    	add.add(and, Awithout0,Bwithout0,addAndOut);
    	Node addOut = new Node();
    	add.add(add, xorwithoutN,addAndOut,addOut);
    	addOut.add(add.out.get(0));
    	xorn.add(add.out.get(0));
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(add.toString());
    	add.printEval(A,B);
    	definitionDB.put("add",add);
    	add.printEval(A,B);//FIXME
    	System.out.print(definitionDB.toString());
    	
    	//ZEROS definition////logic definition of zero values
    	Definition zeros = new Definition(1,1,"zeros");
    	zeros.add(xor,zeros.in.get(0),zeros.in.get(0),zeros.out.get(0));
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(zeros.toString());
    	definitionDB.put("zeros",zeros);
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
    	definitionDB.put("ones",ones);
    	ones.printEval(A);
    	System.out.print(definitionDB.toString());
    	
    	//DEC definition////definition to decrement an integer by one
    	//dec [0;1] = not[0{2},2] rif [0{2},(0{0}&0{1}&2),(8&2;1] dec [(0{0}&0{1});8]//TODO: fix to string incorrect subnodes + to look like this
    	Definition dec = new Definition(1,1,"dec");
    	Node dec0 = dec.in.get(0).add(new Node());
    	Node decRest = dec.in.get(0).add(new Node());
    	Node decN = dec.in.get(0).add(new Node());
    	Node decNnot = new Node();
    	dec.add(not, decN,decNnot);
    	Node decR= new Node();
    	dec0.add(decR);
    	decRest.add(decR);
    	Node decRout= new Node();
    	dec.add(dec,decR,decRout);
    	Node decRif = new Node();
    	dec.add(rifDef,decN,decR,decRout,decRif);
    	decRif.add(dec.out.get(0));
    	decNnot.add(dec.out.get(0));
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(dec.toString());
    	dec.printEval(A);
    	definitionDB.put("dec",dec);
//    	dec.printEval(A);//FIXME: intersection with recursion not optimized
    	System.out.print(definitionDB.toString());
    	
    	//CMP definition////definition to test if two values are equal, returns a bit
    	Definition cmp = new Definition(2,1,"cmp");
    	Node cmpA0 = cmp.in.get(0).add(new Node());
    	Node cmpARest = cmp.in.get(0).add(new Node());
    	Node cmpAN = cmp.in.get(0).add(new Node());
    	Node cmpB0 = cmp.in.get(1).add(new Node());
    	Node cmpBRest = cmp.in.get(1).add(new Node());
    	Node cmpBN = cmp.in.get(1).add(new Node());
    	Node cmpAwithoutN = new Node();
    	cmpA0.add(cmpAwithoutN);
    	cmpARest.add(cmpAwithoutN);
    	Node cmpBwithoutN = new Node();
    	cmpB0.add(cmpBwithoutN);
    	cmpBRest.add(cmpBwithoutN);
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
    	cmp.printEval(A,A);
    	cmp.printEval(A,B);
    	definitionDB.put("cmp",cmp);
//    	cmp.printEval(A,A);
//    	cmp.printEval(A,B);
    	System.out.print(definitionDB.toString());
    	
    	//EQ0 definition////definition to test if a value is zero
    	Definition eq0 = new Definition(1,1,"eq0");
    	Node eq=new Node();
    	eq0.add(zeros,eq0.in.get(0),eq);
    	eq0.add(cmp,eq,eq0.in.get(0),eq0.out.get(0));
    	System.out.println();
    	System.out.print("New definition: ");
    	System.out.print(eq0.toString());
//    	eq0.printEval(A);
//    	eq0.printEval(B);
    	definitionDB.put("eq0",eq0);
//    	eq0.printEval(A);
//    	eq0.printEval(B);
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
//    	mul.printEval(B,B);
    	definitionDB.put("mul",mul);
//    	mul.printEval(B,B);
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
