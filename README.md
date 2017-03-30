# Recompiler
A proposal for object code optimization through an intermediate language

A novel approach to code optimization by using circuit minimization techniques.  
This project is centered around the backend of a recompiler: optimization of an intermediate language.  
The intermediate language uses only the logic NAND function and recursion.  
Two main data structures are used: a database of recursive functions and a forest of two input NAND functions.  
The forest of NAND functions is very similar to the And Inverter Graphs used in state of the art circuit minimization methods, like  Berkley's ABC.  
A representation for functions has been developped that extends a netlist by adding recursivity.  
A full adder:
![Alt text](https://upload.wikimedia.org/wikipedia/commons/4/48/1-bit_full-adder.svg)  
Is represented:

sum[A,B,Cin; S,Cout] =  
&nbsp;&nbsp;&nbsp;&nbsp;xor[A,B; t1], and[A,B; t2], xor[A,B; t4]  
&nbsp;&nbsp;&nbsp;&nbsp;and[t1, Cin; t3], xor[t4,C; S]  
&nbsp;&nbsp;&nbsp;&nbsp;or[t2,t3; Cout]  
    
The addition of natural numbers algorithm (using full adders) is:

add[A,B; C(D{0}&E&F)] =  
&nbsp;&nbsp;&nbsp;&nbsp;and [A{0},B{0}; t1] xor [A{0},B{0}; F]   
&nbsp;&nbsp;&nbsp;&nbsp;sumR [A{n-1..1},1{n-1..1},t1; E,D]  
sumR[A,B,C; D(E&F),G(H&I)] =  
&nbsp;&nbsp;&nbsp;&nbsp;sum [A{0},B{0},C; F,H]  
&nbsp;&nbsp;&nbsp;&nbsp;sumR [A{n-1..1},B{n-1..1},H; E,I]  

