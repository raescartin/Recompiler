# Recompiler
A proposal for object code optimization through an intermediate language

A novel approach to code optimization by using circuit minimization techniques.
This project is centered around the backend of a recompiler: optimization of an intermediate language.
The intermediate language uses only the logic NAND function and recursion.
Two main data structures are used: a database of recursive functions and a forest of two input NAND functions.
The forest of NAND functions is very similar to the And Inverter Graphs used in state of the art circuit minimization methods like  Berkley's ABC.
A representation for functions has been developped that extends a netlist by adding recursivity.
A full adder is represented:

sum[A,B,Cin; S,Cout] = 
		xor[A,B; t1], and[A,B; t2], xor[A,B; t4]
    and[t1, Cin; t3], xor[t4,C; S] 
    or[t2,t3; Cout]
    
The addition of natural numbers algorithm (using full adders) is:

add[A,B; C(D{0}&E&F)] =
		and [A{0},B{0}; t1] xor [A{0},B{0}; F]
		sumR [A{n-1..1},1{n-1..1},t1; E,D]
sumR[A,B,C; D(E&F),G(H&I)] =
		sum [A{0},B{0},C; F,H]
	  sumR [A{n-1..1},B{n-1..1},H; E,I]
