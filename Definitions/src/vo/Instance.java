/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * License: https://www.gnu.org/licenses/gpl-2.0.html GPL version 2
 *******************************************************************************/
package vo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
			if((!this.definition.recursiveInstances.isEmpty()&&!empties)||(this.definition.recursiveInstances.isEmpty()&&ins)){
				this.definition.eval(tempValueMap);//eval
				for (int i = 0; i < this.out.size(); i++) {
					if(tempValueMap.containsKey(this.definition.out.get(i))){
						valueMap.put(this.out.get(i),tempValueMap.get(this.definition.out.get(i)));
					}
				}
			}else{
				if(!this.definition.recursiveInstances.isEmpty()){
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
	public int write(ObjectOutputStream stream,
			HashMap<Node, Integer> nodeMap, int nodeIndex, HashMap<Definition, Integer> defMap) throws IOException {
		//FIXME: write subnodes
		stream.write(this.in.size());
		for(Node node : this.in){
			nodeIndex=node.write(stream, nodeMap, nodeIndex);
		}
		stream.write(this.out.size());
		for(Node node : this.out){
			nodeIndex=node.write(stream, nodeMap, nodeIndex);
		}
		stream.write(defMap.get(this.definition));
		return nodeIndex;
	}
	public void read(ObjectInputStream stream, HashMap<Integer, Node> nodeMap, HashMap<Integer, Definition> defMap) throws IOException {
		//FIXME: read subnodes
		Node node = null;
		int keyNode = 0;
		int size = stream.read();//nº in nodes
		for (int i = 0; i < size; i++) {//add in nodes
			keyNode=stream.read();
			if(nodeMap.containsKey(keyNode)){
				this.in.add(nodeMap.get(keyNode));
			}else{
				node = new Node();
				node.read(stream, nodeMap);
				this.in.add(node);
				nodeMap.put(keyNode,node);
			}
		}
		size = stream.read();//nº out nodes
		for (int i = 0; i < size; i++) {//add out nodes
			keyNode=stream.read();
			if(nodeMap.containsKey(keyNode)){
				node=nodeMap.get(keyNode);
				node.outOfInstance=this;
				this.out.add(node);
			}else{
				node = new Node();
				node.read(stream, nodeMap);
				node.outOfInstance=this;
				this.out.add(node);
				nodeMap.put(keyNode,node);
			}
		}
		keyNode=stream.read();
		this.definition=defMap.get(keyNode);
	}
}
