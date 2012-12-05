package com.matburt.mobileorg.util;

import java.util.ArrayList;
import java.util.HashMap;

public class MultiMap<T> {

	private HashMap<Long, ArrayList<T>> entryMap = new HashMap<Long, ArrayList<T>>();

	public void put(Long key, T value) {	
		ArrayList<T> valueList = entryMap.get(key);
		
		if(valueList == null) {
			valueList = new ArrayList<T>();
			entryMap.put(key, valueList);
		}
		
		valueList.add(value);
	}
	
	public ArrayList<T> get(Long key) {
		return entryMap.get(key);
	}
}
