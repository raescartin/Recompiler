# Recompiler
A proposal for object code optimization through an intermediate language
DATABASE:
add [0,1;2(17&14)] = xor [0{2},1{2};14] xor [0{0},1{0};12] xor [0{1},1{1};13] and [0{1},1{1};35] and [0{2},1{2};36] add [15(12&13),16(35&36);17]  root in: 
or [0,1;2] = not [0;5] not [1;6] nand [5,6;2]  root in: 
dec [0;1(8(15&16)&5)] = if [0{2},6(0{0}&0{1}){2},7{2};16] not [0{2};5] dec [6(0{0}&0{1});7] rif [0{2},17(0{0}&6(0{0}&0{1}){1}),18(7{0}&7{1});15]  root in: 
eq0 [0;1] = not [0{2};15] nand [0{2},15;16] nand [15,16;17] zeros [0{2};18] nand [0{2},18;19] nand [17,19;20] and [12,20;1] zeros [0{0};3] zeros [0{1};4] cmp [13(3&4),14(0{0}&0{1});12]  root in: 
cmp [0,1;2] = nand [0{2},1{2};14] or [0{2},1{2};17] nand [14,17;18] and [12,18;2] cmp [9(0{0}&0{1}),10(1{0}&1{1});12]  root in: eq0,
zeros [0;1] = not [0;2] and [0,2;1]  root in: 
rif [0,1,2;3(16&13)] = if [0,1{2},2{2};13] rif [0,14(1{0}&1{1}),15(2{0}&2{1});16]  root in: 
not [0;1] = nand [0,0;1]  root in: and,ones,
and [0,1;2] = nand [0,1;4] not [4;2]  root in: xor,if,zeros,cmp,eq0,
ones [0;1] = not [0;3] nand [0,3;1]  root in: 
nand [0,1;2] =  root in: not,or,
xor [0,1;2] = nand [0,1;5] or [0,1;8] and [5,8;2]  root in: zeros,
if [0,1,2;3] = not [1;7] nand [0,7;8] or [0,2;11] and [8,11;3]  root in: 
