/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * All rights reserved.
 *******************************************************************************/
package vo;

import java.util.ArrayList;


//A program is a sequence of asm instructions
public class Program extends Definition{
	public Program(String name, int numberOfInputs) {
		super(numberOfInputs, name);
		// TODO Auto-generated constructor stub
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = -950432496747699320L;
	ArrayList<Asm> instructions = new ArrayList<Asm>();
}
