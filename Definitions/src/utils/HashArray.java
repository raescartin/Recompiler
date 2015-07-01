/*******************************************************************************
 * Copyright (c) 2015 Rubén Alejandro Escartín Aparicio.
 * All rights reserved.
 *******************************************************************************/
package utils;

import java.util.ArrayList;
import java.util.HashSet;

//TODO: REMOVE is still O(n)
public class HashArray {
	HashSet<Object> hash;
	ArrayList<Object> array;
	public HashArray(){
		this.hash = new HashSet<Object>();
		this.array = new ArrayList<Object>();
	}
	public void add(Object object){
		if(!hash.contains(object)){
			this.hash.add(object);
			this.array.add(object);
		}
	}
	public int size(){
		return this.array.size();
	}
	public void remove(Object object){
		this.hash.remove(object);
		this.array.remove(object);
	}
	public void get(int i){
		this.array.get(i);
	}
	public boolean contains(Object object){
		return this.hash.contains(object);
	}
}
