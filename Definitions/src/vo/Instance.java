/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import utils.FixedBitSet;

public class Instance implements java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 804522577555261305L;
	public ArrayList<Node> in;
	public ArrayList<Node> out;
	public Definition definition;
	//CONSTRUCTOR
	public Instance() {
		in = new ArrayList<Node>();
		out = new ArrayList<Node>();
	}
	//METHODS
	public void eval(HashMap<Node, FixedBitSet> valueMap, HashSet<Instance> recursiveInstances, HashSet<Instance> instancesToExpand) {
			HashMap<Node, FixedBitSet> tempValueMap = new HashMap<Node, FixedBitSet>();
			boolean empties = false;
			boolean ins = false;
			for (int i = 0; i < this.in.size(); i++) {
				this.in.get(i).eval(valueMap, recursiveInstances, instancesToExpand);
				if(valueMap.containsKey(this.in.get(i))){
					ins=true;
					tempValueMap.put(this.definition.in.get(i), valueMap.get(this.in.get(i)));
					if(valueMap.get(this.in.get(i)).length()==0){
						empties=true;
					}
				}
			}
			//recursive and all inputs or not recursive and some ins
			if((!this.definition.selfRecursiveInstances.isEmpty()&&!empties)||(this.definition.selfRecursiveInstances.isEmpty()&&ins)){
				this.definition.eval(tempValueMap);//eval
				for (int i = 0; i < this.out.size(); i++) {
					if(tempValueMap.containsKey(this.definition.out.get(i))){
						valueMap.put(this.out.get(i),tempValueMap.get(this.definition.out.get(i)));
					}
				}
			}else{
				if(!this.definition.selfRecursiveInstances.isEmpty()){
					for (Node outNode: this.out) {
						valueMap.put(outNode, new FixedBitSet());
					}
				}
			}
	}
	public String toString(){
		String string = new String();
		string+=(this.definition.name+" [");
		for (int i = 0; i < this.in.size(); i++) {//print in nodes
			string+=this.in.get(i).toString();
			string+=(",");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration ","
		string+=(";");
		for (int i = 0; i < this.out.size(); i++) {//print out nodes
			string+=this.out.get(i).toString();
			string+=(",");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration ","
		string+=("] ");
		return string;
	}
	public String toString(Definition definition){
		String string = new String();
		string+=(this.definition.name+" [");
		for (int i = 0; i < this.in.size(); i++) {//print in nodes
			string+=this.in.get(i).toString();
			string+=(",");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration ","
		string+=(";");
		for (int i = 0; i < this.out.size(); i++) {//print out nodes
			string+=this.out.get(i).toString();
			string+=(",");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration ","
		string+=("] ");
		return string;
	}
	public void updateDefinition(Definition definition) {
		if(!definition.instances.contains(this)){
			definition.instances.add(this);
			if(this.definition==definition){
				definition.selfRecursiveInstances.add(this);
			}else if(!this.definition.selfRecursiveInstances.isEmpty()||!this.definition.instancesOfRecursiveDefinitions.isEmpty()){
				definition.instancesOfRecursiveDefinitions.add(this);
			}
			for(Node inNode:this.in){
				inNode.updateDefinition(definition);
			}
		}
		
	}
}
