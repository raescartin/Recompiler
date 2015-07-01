/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * All rights reserved.
 *******************************************************************************/
package utils;

import java.util.ArrayList;
import java.util.HashMap;

import vo.NandNode;
//TODO: ALL
public class NandNodeMatrix{
	HashMap<NandNode,NandNode> hash;
	ArrayList<ArrayList<NandNode>> matrix;
	HashMap<NandNode,NandNode> lastLine;
	public NandNodeMatrix(){
		hash = new HashMap<NandNode,NandNode>();
		matrix = new ArrayList<ArrayList<NandNode>>();
	}
	public void add(NandNode object){
		if(!hash.containsValue(object)){
			hash.put(object,object);
			if(lastLine.containsValue(object)){
				
			}else{
				matrix.get(matrix.size()-1).add(object);//FIXME
			}
		}
	}
	public NandNode get(NandNode object){
		return hash.get(object);
	}
	public void newline(){
		matrix.add(new ArrayList<NandNode>());
	}
	public boolean contains(NandNode object){
		return hash.containsValue(object);
	}
}
