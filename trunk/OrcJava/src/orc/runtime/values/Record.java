/*
 * Copyright 2006, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.LinkedList;

/**
 * A record value container
 * @author dkitchin
 */
public class Record extends BaseValue {

	TreeMap<String,BaseValue> fields = new TreeMap<String,BaseValue>();

	public Record(Map<String,BaseValue> fs) {
		this.fields.putAll(fs);
	}

	public BaseValue field(String f) {
		if (fields.containsKey(f))
			return fields.get(f);
		else
			throw new Error("Field "+f+" not in the domain of record "+this.toString());
		
	}
	
	/*
	 * Edited by Pooja Gupta:
	 * Originally only records supported access to certain objects (fields in
	 * case of a Record) through a dot operator. Now, we have introduced a generic
	 * interface called DotAccessible that indicates that a class supports access
	 * to certain other objects through application of dot operator. 
	 * 
	 * Ssince Record class implements this interface we have added the following
	 * method.
	 * 
	 */
	public BaseValue dotAccessibleVal(String fieldName)
	{
		return field(fieldName);
	}
	
	public String toString() {
		List<String> l = new LinkedList<String>();
		
		for(Map.Entry<String,BaseValue> e : this.fields.entrySet())
		{
			l.add(e.getKey() + e.getValue().toString());
		}
		
		return format('{', l, ", ", '}');
	}

	public static String format(char left, List items, String sep, char right) {
		StringBuffer buf = new StringBuffer();
		buf.append(left);
		int i = 0;
		for (Object x : items) {
			if (i > 0)
				buf.append(sep);
			buf.append(x);
			i++;
		}
		buf.append(right);
		return buf.toString();
	}

	public int size() {
		return fields.size();
	}
}
