/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

import java.util.Arrays;
import java.util.List;

/**
 * A tuple value container
 * @author wcook
 */
public class Tuple extends BaseValue {

	Object[] values;

	public Tuple(Object[] values) {
		this.values = values;
	}

	public Object at(int i) {
		return values[i];
	}
	
	public String toString() {
		return format('[', Arrays.asList(values), ", ", ']');
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
		return values.length;
	}
}
