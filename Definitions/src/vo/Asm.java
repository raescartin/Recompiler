/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
//not used
public class Asm {
	public byte opcode;
	public byte adress_a;
	public byte adress_b;
	public byte adress_c;
	public String toString() {
		return Byte.toString(this.opcode)
				+Byte.toString(this.adress_a)
				+Byte.toString(this.adress_b)
				+Byte.toString(this.adress_c);
	}
	public Definition toDefinition(ArrayList<Definition> opcodeDefinitions) {
		return opcodeDefinitions.get(this.opcode);
	}
	public static ArrayList<Asm> toCode(Definition codeDefinition,
			ArrayList<Definition> opcodeDefinitions, DefinitionDB definitionDB) {
		// TODO Auto-generated method stub
		return null;
	}
}
