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
//TODO: index of subnodes as 2 bits instead of subnode node
//TODO: implement subnodes in order to divide nodes (for both recursion and out flexibility) == concatenate and fision
//FIXME: print subnodes, write subnodes, read subnodes
//TODO: remove value
//TODO: no node can be linked 1 on 1 as supernode<->subnode (same node repeated)
//TODO: index of subnodes
public class Node {
	public ArrayList<Node> supernodes;//ArrayList, since there can be repetition //care:supernodes are nodes with subnodes, not fathers
	public ArrayList<Node> subnodes;//TODO:LinkedHashSet for order without repetition //needed?//size min 3?//care:subnodes are nodes part of a supernode, not children
	public Instance outOfInstance;
	public HashSet<Instance> inOfInstances;
	
	//DEBUGGING ONLY
	public HashMap<Definition,Integer> idForDefinition;//id of node for each definition where it's used
	//END OF DEBUGGING ONLY
	public Node() { 
		this.supernodes = new ArrayList<Node>();//care:supernodes are nodes with subnodes, not fathers
		this.subnodes = new ArrayList<Node>();//care:subnodes are nodes part of a supernode, not children
		this.inOfInstances = new HashSet<Instance>();//FIXME:needed?
		this.idForDefinition = new HashMap<Definition,Integer>();
	}
	public Node add(Node node) {//add subnode to supernode
		this.subnodes.add(node);
		node.supernodes.add(this);
		for(Definition def:this.idForDefinition.keySet()){
			def.add(node);
		}
		return node;
	}
	public ArrayList<NandNode> toNands(HashMap<Node, Integer> nodeSize, HashSet<Node> expandedNodes,HashMap<Node, ArrayList<NandNode>> nodeToNands, NandForest nandForest) {
		//TODO: this should probably be in definition
		//Nodes are mapped up to down
		//tree is traversed down to up (so recursive calls needed)
		//map only the smallest subnodes (NODE FISION)
		//evaluate node
		//there should be a better way to do find the smallest node and normalize the rest to it's size
		//-if a node is both in and out, there's no need to optimize it, nor take it to nandForest
		//-nand ins maybe set before toNands
		//-using "indexOf(this) seems unreliable
		ArrayList<NandNode> nandNodes = new ArrayList<NandNode>();
		expandedNodes.add(this);
		if(nodeToNands.containsKey(this)){//evaluated node //this map is useful to keep track of evaluated nodes
			nandNodes=nodeToNands.get(this);
		}else{//if(!nodeToNands.containsKey(this)){//non evaluated node //this map is useful to keep track of evaluated nodes
			if(this.outOfInstance!=null){//node is out of an instance
					if(this.outOfInstance.definition.name=="nand"){//instance of NAND definition //TODO: fix nand checking
						Node node1=this.outOfInstance.in.get(0);
						Node node2=this.outOfInstance.in.get(1);
						ArrayList<NandNode> nandNodes1=node1.toNands(nodeSize, expandedNodes, nodeToNands,nandForest);
						ArrayList<NandNode> nandNodes2=node2.toNands(nodeSize, expandedNodes, nodeToNands,nandForest);
						for(int i=0; i< nandNodes1.size(); i++){
							NandNode nandNode=nandForest.add(nandNodes1.get(i),nandNodes2.get(i));
							nandNodes.add(nandNode);
						}
					}else{//other definition
						//eval and map ALL instance inputs
						for (Node node : this.outOfInstance.in) {
							ArrayList<NandNode> inputNodes = node.toNands(nodeSize, expandedNodes, nodeToNands, nandForest);//maybe not expanded nodes since it's upper nodes
							nodeToNands.put(node, inputNodes);
						}
						//add all instance input nodes to new map definition input nodes
						HashMap<Node,ArrayList<NandNode>> tempMap = new HashMap <Node,ArrayList<NandNode>>();
						for (int i = 0; i < this.outOfInstance.in.size(); i++) {
							ArrayList<NandNode> instanceInNandNodes = nodeToNands.get(this.outOfInstance.in.get(i)); 
							tempMap.put(this.outOfInstance.definition.in.get(i),instanceInNandNodes );
						}
						HashSet<Node> tempExpandedNodes = new HashSet<Node>();//we don't want instance nodes recorded as expanded
						tempExpandedNodes.addAll(expandedNodes);
						//evaluate definition
						//evaluate ALL outputs since we already have all the inputs ( VS only THIS output (don't evaluate unused outputs)? or evaluate ALL outputs (faster, more confidant on user)? )
						for (int i = 0; i < this.outOfInstance.out.size(); i++) {
							ArrayList<NandNode> tempNandNodes = this.outOfInstance.definition.out.get(i).toNands(nodeSize, tempExpandedNodes, tempMap, nandForest);
						    nandNodes.addAll(tempNandNodes);
							nodeToNands.put(this.outOfInstance.out.get(i), tempNandNodes);//map outs so no need to find index
						}	
							
					}
				}
				//eval subnodes only traversing to parent //ensure traversal bot to top
				boolean traversed=false;
				for (Node subnode : this.subnodes) {
					if(expandedNodes.contains(subnode)){
						traversed=true;
					}
				}
				if(!traversed){
					for (Node subnode : this.subnodes) {
						if(!expandedNodes.contains(subnode)){
							nandNodes.addAll(subnode.toNands(nodeSize, expandedNodes, nodeToNands, nandForest));//add to output all the subnodes that form a node
						}
					}
				}

				nodeToNands.put(this, nandNodes);//before eval of supernodes
				
				for (Node supernode : this.supernodes) {
					if(!expandedNodes.contains(supernode)){//father supernode (up)
						ArrayList<NandNode> tempNodes=supernode.toNands(nodeSize, expandedNodes, nodeToNands, nandForest);//add to output all the supernodes of a node
						for(int i = 0; i < supernode.subnodes.size()/2; i++) {
							ArrayList<NandNode> leftNode = new ArrayList<NandNode>();
							leftNode.add(tempNodes.get(i));
							nodeToNands.put(supernode.subnodes.get(i),leftNode);
						}
						ArrayList<NandNode> centerNodes = new ArrayList<NandNode>();
						centerNodes.addAll(tempNodes.subList(supernode.subnodes.size()/2, tempNodes.size()-supernode.subnodes.size()/2));
						nodeToNands.put(supernode.subnodes.get(supernode.subnodes.size()/2),centerNodes);//center nodes
						for(int i = supernode.subnodes.size()/2+1; i < supernode.subnodes.size(); i++) {
							ArrayList<NandNode> rightNode = new ArrayList<NandNode>();
							rightNode.add(tempNodes.get(tempNodes.size()-supernode.subnodes.size()+i));//right node
							nodeToNands.put(supernode.subnodes.get(i),rightNode);
						}
						nandNodes=nodeToNands.get(this);		
					}
				}
		}
		return nandNodes;
	}
	public String toString() {
		//for subnodes and supernodes instead of fathers and children
		String string = new String();
		string+="(";
		for (Definition definition : idForDefinition.keySet()) {
			if(idForDefinition.containsKey(definition)){
				string+=definition.name+" ";
				string+=idForDefinition.get(definition);
			}
			string+=("/");
		}
		string=string.substring(0, string.length() - 1);//remove last enumeration "/"
		string+=")";
		if(!this.subnodes.isEmpty()){
			string+="(";
			for(Node node: this.subnodes){
				string+=node.toString();
				string+=("&");
			}
			string=string.substring(0, string.length() - 1);//remove last enumeration ","
			string+=")";
		}
		return string;
	}
	public String toString(Definition definition) {
		String string = new String();
		if(idForDefinition.containsKey(definition)){
			string+=idForDefinition.get(definition);
		}
		if(!this.subnodes.isEmpty()){
			string+="(";
			for(Node node: this.subnodes){
				string+=node.toString(definition);
				string+=("&");
			}
			string=string.substring(0, string.length() - 1);//remove last enumeration ","
			string+=")";
		}
		return string;
	}
	public int write(ObjectOutputStream stream, HashMap<Node, Integer> nodeMap, int nodeIndex) throws IOException {
		if(nodeMap.containsKey(this)){
			stream.write(nodeMap.get(this));
		}else{
			nodeMap.put(this, nodeIndex);
			stream.write(nodeIndex);
			nodeIndex++;
			stream.write(this.subnodes.size());
			for (Node node : this.subnodes) {
				nodeIndex=node.write(stream,nodeMap,nodeIndex);
			}
		}
		return nodeIndex;
	}
	public void read(ObjectInputStream stream, HashMap<Integer, Node> nodeMap) throws IOException {
		//TODO: read subnodes
		Node node;
		int keyNode;
		int size =stream.read();//subnodes size
		for (int i = 0; i < size; i++) {//add subnodes
			keyNode=stream.read();
			if(nodeMap.containsKey(keyNode)){
				node=nodeMap.get(keyNode);
				this.subnodes.add(node);
			}else{
				node = new Node();
				node.read(stream,nodeMap);
				this.subnodes.add(node);
				nodeMap.put(keyNode,node);
			}
		}
		
	}
	public void eval(HashMap<Node, FixedBitSet> valueMap) {
		if(!valueMap.containsKey(this)){//Non evaluated node
			if(!this.subnodes.isEmpty()){
				for (Node node : this.subnodes) {
					node.eval(valueMap);
				}
			}else{
				if(this.outOfInstance!=null){
					this.outOfInstance.eval(valueMap);
				}
			}
		}
	}
	public void setInSize(HashMap<Node, Integer> inNodeSize) {
		if(!this.supernodes.isEmpty()){
			for(Node supernode: this.supernodes){
				supernode.setInSize(inNodeSize);
			}	
		}else{
			if(this.outOfInstance!=null){//out node of instance
				if(this.outOfInstance.definition.name=="nand"){
					this.outOfInstance.in.get(0).setInSize(inNodeSize);
					if(this.outOfInstance.in.get(0)!=this.outOfInstance.in.get(1)){//TODO: maybe other instances also need this fix
						this.outOfInstance.in.get(1).setInSize(inNodeSize);
					}
				}else{//definition different from nand
					HashMap<Node, Integer> instanceNodeSize = new HashMap<Node, Integer>();
					this.outOfInstance.definition.out.get(this.outOfInstance.out.indexOf(this)).setInSize(instanceNodeSize);
					for(int i=0;i<this.outOfInstance.in.size();i++){
						if(instanceNodeSize.containsKey(this.outOfInstance.definition.in.get(i))){
							this.outOfInstance.in.get(i).setInSize(inNodeSize);
						}
					}
				}
			}else{//in node
				if(inNodeSize.containsKey(this)){
					inNodeSize.put(this, inNodeSize.get(this)+1);
				}else{
					inNodeSize.put(this, 1);
				}
			}
		}
	}
	public void mapOutNode(HashMap<Node, ArrayList<NandNode>> nodeToNands,
			HashMap<NandNode, Node> nandToNodeOut) {
		if(nodeToNands.get(this).size()==1){
			nandToNodeOut.put(nodeToNands.get(this).get(0), this);
		}else{
			if(this.subnodes.isEmpty()){
				for (Node node:this.supernodes) {
					node.mapOutNode(nodeToNands, nandToNodeOut);
				}
			}else{
				for (Node node:this.subnodes) {
					node.mapOutNode(nodeToNands, nandToNodeOut);
				}
			}
		}
		
	}
	public void copy(Node tempNode) {
		this.supernodes=tempNode.supernodes;
		this.subnodes=tempNode.subnodes;
		this.outOfInstance=tempNode.outOfInstance;
		this.inOfInstances=tempNode.inOfInstances;
		this.idForDefinition=tempNode.idForDefinition;
		
	}
	public void fusion(Definition definition,HashSet<Node> nodes) {
		//fusion of subnodes in a definition
		if(!nodes.contains(this)){
			nodes.add(this);
			if(!this.subnodes.isEmpty()){
				//for all the subnodes that are out of nand of same supernode in1 and supernode in2
				//fuse node(out)
				ArrayList<Node> inNodes = new ArrayList<Node> ();
				ArrayList<Node> outNodes = new ArrayList<Node> ();
				boolean fuse=true;
				int i=1;
				while(fuse&&i<this.subnodes.size()){//all the subnodes must be of the same definition
					if(this.subnodes.get(0).outOfInstance==null||this.subnodes.get(i).outOfInstance==null||this.subnodes.get(i).outOfInstance.definition!=this.subnodes.get(0).outOfInstance.definition){
						fuse=false;
					}
					i++;
				}
				i=1;
				while(fuse&&i<this.subnodes.size()){//all the instance inss must have the same supernode each
					int j=1;
					while(fuse&&j<this.subnodes.get(0).outOfInstance.in.size()){
						if(this.subnodes.get(0).outOfInstance.in.get(j).supernodes!=this.subnodes.get(i).outOfInstance.in.get(j).supernodes){
							fuse=false;
						}
						inNodes.addAll(this.subnodes.get(0).outOfInstance.in.get(j).supernodes);
						j++;
					}
					i++;
				}
				i=1;
				while(fuse&&i<this.subnodes.size()){//all the instance outs must have the same supernode each
					int j=1;
					while(fuse&&j<this.subnodes.get(0).outOfInstance.out.size()){
						if(this.subnodes.get(0).outOfInstance.out.get(j).supernodes!=this.subnodes.get(i).outOfInstance.out.get(j).supernodes){
							fuse=false;
						}
						outNodes.addAll(this.subnodes.get(0).outOfInstance.out.get(j).supernodes);
						j++;
					}
					i++;
				}
				if(fuse){
					definition.add(this.outOfInstance.definition,inNodes,outNodes);//add fusion instance
					//remove all the fused instances with their nodes
					for(Node subnode:this.subnodes){
						Instance instance=subnode.outOfInstance;
						definition.remove(instance);
					}
				}
			}else{//this.subnodes.isEmpty()
				if(this.outOfInstance!=null){//if this.subnodes.isEmpty()&&this.outOfInstance==null it's always an in node
					for(Node node:this.outOfInstance.in){//expand instance in nodes
						node.fusion(definition, nodes);
					}
				}
			}
		}
	}
}
