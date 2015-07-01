/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * All rights reserved.
 *******************************************************************************/
package utils;

import java.util.HashMap;

public class Trie {
	private TrieNode root = new TrieNode();
	public class TrieNode{
		private HashMap<Character,TrieNode> children = new HashMap<Character,TrieNode>();//NOT OPTIMAL, but near?
		private Object object;
		public void add(Object object, String name) {
			if(name.isEmpty()){
				this.object=object;
			}else{
				TrieNode trieNode = new TrieNode();
				children.put(name.charAt(0), trieNode);
				trieNode.add(object,name.substring(1));
			}
		}
		public Object find(String name) {
			if(name.isEmpty()){
				return this.object;
			}else{
				return children.get(name.charAt(0)).find(name.substring(1));
			}
		}
	}
	public void add(Object object,String name) {
		root.add(object,name);
	}
	public Object find(String name) {
		return root.find(name);
	}
}
