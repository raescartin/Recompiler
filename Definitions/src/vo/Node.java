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
	public ArrayList<Node> parents;//ArrayList, since there can be repetition
	public ArrayList<Node> children;//TODO:LinkedHashSet for order without repetition //needed?//size min 3?
	public Instance outOfInstance;
	public HashSet<Instance> inOfInstances;	
	public Definition definition;
	//DEBUGGING ONLY
	public int idForDefinition;//id of node for each definition where it's used
	//END OF DEBUGGING ONLY
	
	public Node() { 
		this.parents = new ArrayList<Node>();
		this.children = new ArrayList<Node>();
		this.inOfInstances = new HashSet<Instance>();//FIXME:needed?
	}
	public Node add(Node node) {//add subnode to supernode
		node.parents.add(this);
		this.children.add(node);
		if(this.definition!=null) this.definition.add(node);
		if(node.definition!=null) node.definition.add(this);
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
				//eval subnodes only traversing to parents
				if(this.parents.size()==1){//subnode of supernode parent, so map all the subnodes now
					Node supernode=this.parents.get(0);
					ArrayList<NandNode> tempNodes=supernode.toNands(nodeSize, expandedNodes, nodeToNands, nandForest);//add to output all the supernodes of a node
					for(int i = 0; i < supernode.children.size()/2; i++) {
						ArrayList<NandNode> leftNode = new ArrayList<NandNode>();
						leftNode.add(tempNodes.get(i));
						nodeToNands.put(supernode.children.get(i),leftNode);
					}
					ArrayList<NandNode> centerNodes = new ArrayList<NandNode>();
					centerNodes.addAll(tempNodes.subList(supernode.children.size()/2, tempNodes.size()-supernode.children.size()/2));
					nodeToNands.put(supernode.children.get(supernode.children.size()/2),centerNodes);//center nodes
					for(int i = supernode.children.size()/2+1; i < supernode.children.size(); i++) {
						ArrayList<NandNode> rightNode = new ArrayList<NandNode>();
						rightNode.add(tempNodes.get(tempNodes.size()-supernode.children.size()+i));//right node
						nodeToNands.put(supernode.children.get(i),rightNode);
					}
					nandNodes=nodeToNands.get(this);	
				}else{
					for (Node parent : this.parents) {
						if(!expandedNodes.contains(parent)){
							nandNodes.addAll(parent.toNands(nodeSize, expandedNodes, nodeToNands, nandForest));//add to output all the subnodes that form a node
						}
					}
				}
				nodeToNands.put(this, nandNodes);
		}
		return nandNodes;
	}
	public String toString() {
		//for subnodes and supernodes instead of fathers and children
		String string = new String();		
		if(!this.parents.isEmpty()){
			if(this.parents.size()==1){
				string+=this.parents.get(0).toString();
				string+="{";
				string+=this.parents.get(0).children.indexOf(this);
				string+="}";
			}else{
				string+=this.idForDefinition;
				string+="(";
				for(Node node: this.parents){
					string+=node.toString();
					string+=("&");
				}
				string=string.substring(0, string.length() - 1);//remove last enumeration ","
				string+=")";
			}
		}else{
			string+=this.idForDefinition;
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
			stream.write(this.parents.size());
			for (Node node : this.parents) {
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
				this.parents.add(node);
			}else{
				node = new Node();
				node.read(stream,nodeMap);
				this.parents.add(node);
				nodeMap.put(keyNode,node);
			}
		}
		
	}
	public void eval(HashMap<Node, FixedBitSet> valueMap) {
		if(!valueMap.containsKey(this)){//Non evaluated node
			if(!this.parents.isEmpty()){
				for (Node parent : this.parents) {
					parent.eval(valueMap);
				}
			}else{
				if(this.outOfInstance!=null){
					this.outOfInstance.eval(valueMap);
				}
			}
		}
	}
	public void setInSize(HashMap<Node, Integer> inNodeSize) {
		if(!this.parents.isEmpty()){
			for(Node parent: this.parents){
				parent.setInSize(inNodeSize);
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
//	public void mapOutNode(HashMap<Node, ArrayList<NandNode>> nodeToNands,
//			HashMap<NandNode, Node> nandToNodeOut) {
//		if(nodeToNands.get(this).size()==1){
//			nandToNodeOut.put(nodeToNands.get(this).get(0), this);
//		}else{
//			if(this.subnodes.isEmpty()){
//				for (Node node:this.supernodes) {
//					node.mapOutNode(nodeToNands, nandToNodeOut);
//				}
//			}else{
//				for (Node node:this.subnodes) {
//					node.mapOutNode(nodeToNands, nandToNodeOut);
//				}
//			}
//		}
//		
//	}
//	public void copy(Node node) {
//		this.parents=node.parents;
//		this.children=node.children;
//		this.outOfInstance=node.outOfInstance;
//		this.inOfInstances=node.inOfInstances;
//		this.idForDefinition=node.idForDefinition;
//		
//	}
//	public void copy(Node node,HashMap<Node,Node> defToTempNodes) {
//		this.idForDefinition=node.idForDefinition;
//		for(Node copiedSubnode:node.subnodes){
//			if(defToTempNodes.containsKey(copiedSubnode)){
//				this.add(defToTempNodes.get(copiedSubnode));
//			}else{
//				Node subnode = new Node();
//				defToTempNodes.put(copiedSubnode, subnode);
//				this.add(subnode);
//			}
//		}
//	}
//	public void fusion(Definition definition,HashSet<Node> nodes) {
//		//fusion of subnodes in a definition
//		if(!nodes.contains(this)){
//			nodes.add(this);
//			if(!this.subnodes.isEmpty()){
//				//for all the subnodes that are out of nand of same supernode in1 and supernode in2
//				//fuse node(out)
//				ArrayList<Node> inNodes = new ArrayList<Node> ();
//				ArrayList<Node> outNodes = new ArrayList<Node> ();
//				boolean fuse=true;
//				int i=1;
//				while(fuse&&i<this.subnodes.size()){//all the subnodes must be of the same definition
//					if(this.subnodes.get(0).outOfInstance==null||this.subnodes.get(i).outOfInstance==null||this.subnodes.get(i).outOfInstance.definition!=this.subnodes.get(0).outOfInstance.definition){
//						fuse=false;
//					}
//					i++;
//				}
//				i=1;
//				while(fuse&&i<this.subnodes.size()){//all the instance inss must have the same supernode each
//					int j=1;
//					while(fuse&&j<this.subnodes.get(0).outOfInstance.in.size()){
//						if(this.subnodes.get(0).outOfInstance.in.get(j).supernodes!=this.subnodes.get(i).outOfInstance.in.get(j).supernodes){
//							fuse=false;
//						}
//						inNodes.addAll(this.subnodes.get(0).outOfInstance.in.get(j).supernodes);
//						j++;
//					}
//					i++;
//				}
//				i=1;
//				while(fuse&&i<this.subnodes.size()){//all the instance outs must have the same supernode each
//					int j=1;
//					while(fuse&&j<this.subnodes.get(0).outOfInstance.out.size()){
//						if(this.subnodes.get(0).outOfInstance.out.get(j).supernodes!=this.subnodes.get(i).outOfInstance.out.get(j).supernodes){
//							fuse=false;
//						}
//						outNodes.addAll(this.subnodes.get(0).outOfInstance.out.get(j).supernodes);
//						j++;
//					}
//					i++;
//				}
//				if(fuse){
//					ArrayList<Node> inOutNodes = new ArrayList<Node>();
//					nodes.addAll(inNodes);
//					nodes.addAll(outNodes);
//					definition.add(this.outOfInstance.definition,inOutNodes.toArray(new Node[nodes.size()]));//add fusion instance
//					//remove all the fused instances with their nodes
//					for(Node subnode:this.subnodes){
//						Instance instance=subnode.outOfInstance;
//						definition.remove(instance);
//					}
//				}
//				//continue evaluation of superior nodes
//				for(Node node:this.subnodes){
//					if(node.outOfInstance!=null){
//						for(Node inNode:node.outOfInstance.in){
//							inNode.fusion(definition,nodes);
//						}
//					}
//				}
//			}else{//this.subnodes.isEmpty()
//				if(this.outOfInstance!=null){//if this.subnodes.isEmpty()&&this.outOfInstance==null it's always an in node
//					for(Node node:this.outOfInstance.in){//expand instance in nodes
//						node.fusion(definition, nodes);
//					}
//				}
//			}
//		}
//	}
}
