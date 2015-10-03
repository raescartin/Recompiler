/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import vo.Definition;
import vo.DefinitionDB;
import vo.Node;

//IMPLEMENTATION:
//-choice to encumber classes with attributes not always needed, for simplicity and readability
//-start with sequential code, without jumps (propositional vs first order logic), then maybe recursive
//-pending first optimization step: locate E/S
//-optimize to assembler including always nand for simplicity (possible in any turing-complete language)

//TODO
//-subnodes need supernode attribute?
//-use only one bit in if condition
//-FIXME: can't have definition outside of DefinitionDB (or else inconsistencies)
//-add find, use: Definition -> DefinitionDB
//-Asm to Code (more abstraction) zoom out
//-serialize Definition: use only needed variables
//-save/load file definitions
//-selectors, concatenators needed?
//-fix definition removing values?
//-add recursion to definition (like in function)
//-print recursive function
//-print definition? ->function?
//////////////////////////////////////////////////////
//HERE BE DRAGONS (maybe not in Java from here)
//////////////////////////////////////////////////////
//-optimize data structures (memory): separar nand con nombre (definicion) y nand sin nombre (->database), lo mismo con nºnands (atributos no siempre necesarios)
//namedNand namedDefitinion extends...
//-project: estructuras de datos óptimas en espacio (eficiencia)

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
    	not.printEval(A);
     	definitionDB.put("not",not);
     	not.printEval(A);
     	System.out.print(definitionDB.toString());
        //AND definition//
    	Definition and = new Definition(2,1,"and");
    	Node and0= new Node();
    	and.add(nand,and.in.get(0),and.in.get(1),and0);
    	and.add(not,and0,and.out.get(0));
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
    	ifDef.printEval(A,B,C);
    	definitionDB.put("if",ifDef);
    	ifDef.printEval(A,B,C);
    	System.out.print(definitionDB.toString());
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
    	Node A0 = add.in.get(0).add(new Node());
    	Node Arest = add.in.get(0).add(new Node());
    	Node An = add.in.get(0).add(new Node());
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
    	add.printEval(A,B);
    	definitionDB.put("add",add);
    	add.printEval(A,B);
    	System.out.print(definitionDB.toString());
    	//zeros definition////logic definition of zero values
    	Definition zeros = new Definition(1,1,"zeros");
    	zeros.add(xor,zeros.in.get(0),zeros.in.get(0),zeros.out.get(0));
    	definitionDB.put("zeros",zeros);
    	zeros.printEval(A);
    	System.out.print(definitionDB.toString());
    	//ones definition////logic definition of one values
    	Definition ones = new Definition(1,1,"ones");
    	Node zeroNode = new Node();
    	ones.add(zeros, ones.in.get(0),zeroNode);
    	ones.add(not,zeroNode,ones.out.get(0));
    	definitionDB.put("ones",ones);
    	ones.printEval(A);
    	System.out.print(definitionDB.toString());
    	//dec definition////definition to decrement an integer by one
    	//dec [0;1] = not[0{2},2] rif [0{2},(0{0}&0{1}&2),(8&2;1] dec [(0{0}&0{1});8]//TODO: fix to string incorrect subnodes + to look like this
//    	Definition dec = new Definition(1,1,"dec");
//    	Node dec0 = dec.in.get(0).add(new Node());
//    	Node decRest = dec.in.get(0).add(new Node());
//    	Node decN = dec.in.get(0).add(new Node());
//    	Node decNnot = new Node();
//    	dec.add(not, decN,decNnot);
//    	Node decThen = new Node();
//    	dec0.add(decThen);
//    	decRest.add(decThen);
//    	decNnot.add(decThen);
//    	Node decR= new Node();
//    	dec0.add(decR);
//    	decRest.add(decR);
//    	Node decRout= new Node();
//    	dec.add(dec,decR,decRout);
//    	Node decElse = new Node();
//    	decRout.add(decElse);
//    	decNnot.add(decElse);
//    	dec.add(rifDef,decN,decThen,decElse,dec.out.get(0));
//    	dec.printEval(A);
//    	definitionDB.put("dec",dec);
//    	dec.printEval(A);
//    	System.out.print(definitionDB.toString());
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
    	dec.printEval(A);
    	definitionDB.put("dec",dec);//FIXME: intersection with recursion not optimized
    	dec.printEval(A);//FIXME: intersection with recursion not optimized
    	System.out.print(definitionDB.toString());
    	//cmp definition////definition to test if two values are equal, returns a bit
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
    	cmp.printEval(A,A);
    	cmp.printEval(A,B);
    	definitionDB.put("cmp",cmp);
    	cmp.printEval(A,A);
    	cmp.printEval(A,B);
    	System.out.print(definitionDB.toString());
    	//EQ0 definition////definition to test if a value is zero
    	Definition eq0 = new Definition(1,1,"eq0");
    	Node eq=new Node();
    	eq0.add(zeros,eq0.in.get(0),eq);
    	eq0.add(cmp,eq,eq0.in.get(0),eq0.out.get(0));
    	definitionDB.put("eq0",eq0);
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
    	Definition mul = new Definition(2,1,"mul");
    	Node mul0 = new Node();
    	Node mul1 = new Node();
    	Node mul2 = new Node();
    	Node mul3 = new Node();
    	Node mul4 = new Node();
    	mul.add(eq0,mul.in.get(0),mul0);
    	mul.add(dec,mul.in.get(0),mul2);
    	mul.add(mul,mul2,mul.in.get(1),mul3);
    	mul.add(add,mul3,mul.in.get(1),mul4);
    	mul.add(rifDef,mul0,mul.in.get(0),mul4,mul.out.get(0));//TODO:Test with ifdef and rifdef
    	mul.printEval(B,B);
    	definitionDB.put("mul",mul);
    	mul.printEval(B,B);
    	System.out.print(definitionDB.toString());
    	
    	//ASSEMBLER
    	//Declare instruction set
//        DefinitionDB  instructionSet = new DefinitionDB(nand);
//        System.out.println("INSTRUCTION SET");
//        instructionSet.toString();
// 		Minimum for Turing complete: OISC subnz
//        subnz a, b, c   ; Mem[b] = Mem[b] - Mem[a]
//                ; if (Mem[b] != 0) goto c
        //NEEDED: sub,!=,goto,if (assign, compare, branch)
//        Definition subnz = new Definition(3,"subnz");//TODO: fix bitslenght=8bits
//        Definition sub = new Definition(2,"sub");//TODO: fix bitslenght=8bits
//        Definition nz = new Definition(1,"nz");//TODO: fix bitslenght=8bits
//        
//        //test program: add 8 bits
//        int numProgramInputs=2;
//        Node zero = new Node();
//        Definition program = new Definition(numProgramInputs,"program");
//        program.add(subnz,zero,program.in.get(1));
//        program.add(subnz,program.in.get(0),program.in.get(1));
        //add instructions
        //optimize program
        //TODO: pending: parallelize at instruction level using optimized program information
        
        
        

    	
    	
    	//=========================================================================================================================================
        //serialization to file
        //=========================================================================================================================================
        //write to file : "definitions.dat"
        try{
        	FileOutputStream fileOut=new FileOutputStream("definitions.dat");
  	        ObjectOutputStream out=new ObjectOutputStream(fileOut);
  	        definitionDB.write(out);
  	        out.close();
  	        fileOut.close();
        }catch(IOException i)
        {
            i.printStackTrace();
        }

        //read from file : "definitions.dat"
        try
        {
           FileInputStream fileIn = new FileInputStream("definitions.dat");
           ObjectInputStream in = new ObjectInputStream(fileIn);
           definitionDB.read(in);
           in.close();
           fileIn.close();
        }catch(IOException i)
        {
           i.printStackTrace();
           return;
        }catch(ClassNotFoundException cE)
        {
           System.out.println("Class not found");
           cE.printStackTrace();
           return;
        }
        System.out.println("loaded DATABASE");
        definitionDB.toString();
        
//    	Definition asg = new Definition("asg",1);
//    	asg.addOut(asg.add(not, asg.add(not,asg.in.get(0)).out.get(0)).out.get(0));
//    	//CMP definition// (1 bit flag)
//    	Definition cmp = new Definition("cmp",2);
    	//cmp.addOut(cmp.add(and,cmp.add(xor,cmp.in.get(0).value.set(0,0),cmp.in.get(1).set(0,0)),cmp.add(cmp, cmp.in.get(0).set(1,n),cmp.in.get(1).set(1,n)));
    	// a cmp b = (a0 xor b0) and (a1..n cmp b1..n)
    	//cmp.add(and,cmp.add(xor,cmp.in.get(0).value   ));
    	//needs: 
    	//-use of bitSet.get() X
    	//-call with input parameters X
    	//-recursion base (bitset.length()==0 then noop)
    	

    	
    	
//    	Definition def;
//    	def=asg;
//    	System.out.println("TEST DEFINITION");
//    	System.out.print(A+" "+def.name+" "+B+"=");
//    	def.in.get(0).value=(FixedBitSet.fromString(A));
//    	if (def.in.size()>1) def.in.get(1).value=(FixedBitSet.fromString(B));
//    	def.eval();
//        System.out.println(def.out.get(0).value.toString()); // Display the string.
//        

      
        
        
      
//    	//=========================================================================================================================================
//    	//NANDFORESTS//
//    	//=========================================================================================================================================
//    	//nand
//    	NandForest nandForest;
//    	NandForest nandT= new NandForest("nand",2);
//    	nandT.addOut(nandT.add(nandT.in.get(0), nandT.in.get(1)));
//		//not
//		NandForest notT= new NandForest("not",1);
//		notT.addOut(notT.add(notT.in.get(0), notT.in.get(0)));
//		//and
//		NandForest andT= new NandForest("and",2);
//		andT.addOut(andT.add(andT.add(andT.in.get(0), andT.in.get(1)),andT.add(andT.in.get(0), andT.in.get(1))));//DEBERÍA BORRAR uno!!!
//		//or
//		NandForest orT= new NandForest("or",2);
//		orT.addOut(orT.add(orT.add(orT.in.get(0), orT.in.get(0)),orT.add(orT.in.get(1), orT.in.get(1))));
//		//evaluation
//		nandForest=nandT;
//		System.out.println("TEST NANDFOREST");
//		if (nandForest.in.size()==1){
//			System.out.print(nandForest.name+" "+A+"=");
//			System.out.println(nandForest.eval(FixedBitSet.fromString(A)).get(0).toString()); // Display the string.
//		}else if (nandForest.in.size()==2){
//			System.out.print(A+" "+nandForest.name+" "+B+"=");
//			System.out.println(nandForest.eval(FixedBitSet.fromString(A),FixedBitSet.fromString(B)).get(0).toString()); // Display the string.
//		}

//        //=========================================================================================================================================
//        //FUNCTIONS//
//        //=========================================================================================================================================
//        Function nandF = new Function("nand",2);
//        
//        Function notF = new Function("not",1);
//        notF.add(nandF, notF.in.get(0),notF.in.get(0));
//        
//        Function andF = new Function("and",2);
//        andF.add(notF,andF.add(nandF, andF.in.get(0),andF.in.get(1)));
//        
//        Function orF = new Function("or",2);
//        orF.add(nandF, orF.add(notF, orF.in.get(0)),orF.add(notF,orF.in.get(1)));
//            
//        Function xorF = new Function("xor",2);
//    	xorF.add(andF, xorF.add(orF, xorF.in.get(0),xorF.in.get(1)),xorF.add(nandF, xorF.in.get(0),xorF.in.get(1)));
//    	
//    	Function cmpF = new Function("cmp",2);//output: 0 if in0==in1, 1 if in0!=in1
//    	//(a0 xor b0)
//    	//cmpF.add(xorF, cmpF.in.get(0).set(0,0), cmpF.in.get(1).set(0,0));
//    	//(a(1,n) cmp b(1,n))
//    	//cmpF.add(xorF, cmpF.in.get(0).set(0,0), cmpF.add(cmpF,cmpF.in.get(0).set(1,-1)));
//    	//cmp a = (a0 nand cmp a(1,n))
//    	//cmp 0 = 0
//    	//cmp 01 = 0 nand 1
//    	//cmp 012 = 0 nand ( cmp 12) = 0 nand ( 1 nand 2)
//    	cmpF.add(orF, cmpF.add(xorF, cmpF.in.get(0).set(0,0), cmpF.in.get(1).set(0,0)),cmpF.add(cmpF, cmpF.in.get(0).set(1,-1), cmpF.in.get(1).set(1,-1)));
//    	// a cmp b = (a0 xor b0) or (a(1,n) cmp b(1,n))
//    	
//
//    	
//    	Function addF = new Function("add",2);//adds in0 and in1
//    	//addF.cc(addF.in.get(0),addF.in.get(1));
//    	//addF.add(xorF,addF.in.get(0).set(0,-2), addF.in.get(1).set(0,-2));//(a0..n-1 xor b0..n-1)
//    	//addF.add(andF, addF.in.get(0).set(1,-1), addF.in.get(1).set(1,-1));//(a1..n and b1..n)
//    	//addF.add(xorF, addF.in.get(0).set(-1, -1),addF.in.get(1).set(-1, -1));//(an xor bn)
//    	//addF.cc(addF.add(andF, addF.in.get(0).set(1,-1), addF.in.get(1).set(1,-1)),addF.add(xorF, addF.in.get(0).set(-1, -1),addF.in.get(1).set(-1, -1)));
//    	// a add b = (a1..n and b1..n) && (an xor bn)
//    	//addF.cc(addF.add(xorF,addF.in.get(0).set(0,-2), addF.in.get(1).set(0,-2)),addF.add(xorF, addF.in.get(0).set(-1, -1),addF.in.get(1).set(-1, -1)));
//    	// a add b = (a0..n-1 xor b0..n-1) && (an xor bn) = a xor b
//    	//addF.cc(addF,addF.add(xorF,addF.in.get(0).set(0,-2), addF.in.get(1).set(0,-2)),addF.add(andF, addF.in.get(0).set(1,-1), addF.in.get(1).set(1,-1)));//FIXME: stackoverflow
//    	// a add b = (a0..n-1 xor b0..n-1) add (a1..n and b1..n)
//    	addF.cc(addF.add(addF, addF.add(xorF,addF.in.get(0).set(0,-2), addF.in.get(1).set(0,-2)), addF.add(andF, addF.in.get(0).set(1,-1), addF.in.get(1).set(1,-1))),addF.add(xorF, addF.in.get(0).set(-1, -1),addF.in.get(1).set(-1, -1)));
//    	// a add b = (a0..n-1 xor b0..n-1) add (a1..n and b1..n) && (an xor bn)
//    	
//        Function fct;
//        fct=addF;//recursive functions:add fails because recursion overwrites "add" input data, cmp works
//        System.out.println("TEST FUNCTION");
//        if (fct.in.size()==1){
//        	System.out.print(fct.name+" "+A+"=");
//        	System.out.println(fct.eval(null,FixedBitSet.fromString(A)).toString()); // Display the string.
//    	}else if (fct.in.size()==2){
//    		System.out.print(A+" "+fct.name+" "+B+"=");
//        	System.out.println(fct.eval(null,FixedBitSet.fromString(A),FixedBitSet.fromString(B)).toString()); // Display the string.
//    	}        
//      
//        
//        
//        
//        //Function to NandForest test
//        NandForest ndf = orF.toNandForest();
//        System.out.println("TEST FUNCTION TO NANDFOREST");
//        if (ndf .in.size()==1){
//        	System.out.print(ndf.name+" "+A+"=");
//			System.out.println(ndf .eval(FixedBitSet.fromString(A)).get(0).toString()); // Display the string.
//		}else if (ndf .in.size()==2){
//			System.out.print(A+" "+ndf.name+" "+B+"=");
//			System.out.println(ndf .eval(FixedBitSet.fromString(A),FixedBitSet.fromString(B)).get(0).toString()); // Display the string.
//		}
//        
//        //Definition to NandForest test
//        ndf = nand.toNandForest();
//        System.out.println("TEST DEFINITION TO NANDFOREST");
//        if (ndf .in.size()==1){
//        	System.out.print(ndf.name+" "+A+"=");
//			System.out.println(ndf .eval(FixedBitSet.fromString(A)).get(0).toString()); // Display the string.
//		}else if (ndf .in.size()==2){
//			System.out.print(A+" "+ndf.name+" "+B+"=");
//			System.out.println(ndf .eval(FixedBitSet.fromString(A),FixedBitSet.fromString(B)).get(0).toString()); // Display the string.
//		}
//        
//        //NandForest print test
//        System.out.println("NANDFOREST PRINT TEST");
//        nandForest.print();
//        
//        //Function print test
//        fct=cmpF;
//        System.out.println("FUNCTION PRINT TEST");
//        fct.print();
//        
//        //Definition print test
//        def=nand;
//        System.out.println("DEFINITION PRINT TEST");
//        def.print();
//        
//        //Definition from db print test
//        System.out.println("DEFINITION from db PRINT TEST");
//        definitionDB.get("nand").print();
//        
//        //NandForest to Definition test
//        Definition df = new Definition();
//        definitionDB.fromNandForest(df,ndf);
//        System.out.println("TEST DEFINITION TO NANDFOREST");
//        df.in.get(0).value=(FixedBitSet.fromString(A));
//    	if (df.in.size()>1){
//    		System.out.print(A+" "+df.name+" "+B+"=");
//    		df.in.get(1).value=(FixedBitSet.fromString(B));
//    	}else{
//    		System.out.print(df.name+" "+A+"=");
//    	}
//    	df.eval();
//        System.out.println(df.out.get(0).value.toString()); // Display the string.
        
        
        
//        //Program test
//        Definition program = new Definition("program",2);
        
//        //testing file creation
//        Asm instruction = new Asm();
//        instruction.opcode=1;
//        instruction.adress_a=0;
//        instruction.adress_b=1;
//        instruction.adress_c=2;
//        ArrayList<Asm> code = new ArrayList<Asm>();
//        code.add(instruction);
//        try{
//  	      byte bWrite [] = new byte[4];
//  	      OutputStream os = new FileOutputStream("asmSource.bin");
//  	      for (int i = 0; i < code.size(); i++) {
//  	    	  bWrite[0]=code.get(i).opcode;
//  	    	  bWrite[1]=code.get(i).adress_a;
//  	    	  bWrite[2]=code.get(i).adress_b;
//  	    	  bWrite[3]=code.get(i).adress_c;	  
//  	    	  os.write(bWrite); // writes the bytes
//  	      }
//  	      os.close();
//  	     
//  	      //READ ALL ASSEMBLER CODE
//  	      code = new ArrayList<Asm>();
//  	      byte bRead [] = new byte[4];
//  	      InputStream is = new FileInputStream("asmSource.bin");
//  	      int size = is.available();
//  	      //FIXME: reliable? System.out.print(" "+size/4+" ");
//  	      for(int i=0; i< size/4; i++){
//  	    	is.read(bRead);
//  	    	instruction = new Asm();
//  	    	instruction.opcode=bRead[0];
//  	    	instruction.adress_a=bRead[1];
//  	    	instruction.adress_b=bRead[2];
//  	    	instruction.adress_c=bRead[3];
//  	    	code.add(instruction);
//  	      }
//  	      is.close();
//  	   }catch(IOException e){
//  	      System.out.print("File Exception");
//  	   }
//       for (int i = 0; i < code.size(); i++) {
//    	   System.out.print(code.get(i)+" ");
//       }
//       //creation of ASM definitions
//       ArrayList<Definition> opcodeDefinitions = new ArrayList<Definition>();// Definitions by opcode
//       //add definitions by opcode order
//       //Definition 0 MOV
//       Definition movDef = new Definition("mov",1);
//       //TODO: definition B=NOT(NOT(A))
//       opcodeDefinitions.add(movDef);
//       //Definition 1 CMP
//       Definition cmpDef = new Definition("cmp",2);
//       //TODO: definition C=(A==B)
//       opcodeDefinitions.add(cmpDef);
//       //Definition 2 JMP
//       Definition jmpDef = new Definition("jmp",3);
//     	//TODO: definition
//       opcodeDefinitions.add(jmpDef);
//       //Definition 3 ADD
//     	Definition addDef = new Definition("add",2);
//     	//TODO: definition
//     	opcodeDefinitions.add(addDef);
//       //Translate each assembler instruction to its definition
//       ArrayList<Definition> codeDefinitions = new ArrayList<Definition>();
//       for (int i = 0; i < code.size(); i++) {
//    	   codeDefinitions.add(code.get(i).toDefinition(opcodeDefinitions));
//       }
//       //Consolidate all definitions in one definition
//       Definition codeDefinition = new Definition();
//       codeDefinition.add(code,codeDefinitions);//FIXME: chicha aquí
//       //Translate this code to optimized definition
//       definitionDB.put("code",codeDefinition);
//       codeDefinition=definitionDB.get("code");
//       //Translate definition to assembler
//       code=Asm.toCode(codeDefinition,opcodeDefinitions,definitionDB);//FIXME: chicha aquí
       
      //TEST DATABASE
        //=========================================================================================================================================
        // DataBase of definitions
        //=========================================================================================================================================
 
     	

//     	dfdb=definitionDB.get(dfdb.name);
//     	System.out.println("TEST DEFINITION FROM DATABASE");
//     	dfdb.in.get(0).value=(FixedBitSet.fromString(A));
//    	if (dfdb.in.size()>1) {
//    		System.out.print(A+" "+dfdb.name+" "+B+"=");
//    		dfdb.in.get(1).value=(FixedBitSet.fromString(B));
//    	}else{
//    		System.out.print(dfdb.name+" "+A+"=");
//    	}
//    	dfdb.eval();
//        System.out.println(dfdb.out.get(0).value.toString()); // Display the string.
    }

	
}
