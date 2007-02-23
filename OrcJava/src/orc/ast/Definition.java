/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.*;

import orc.runtime.values.Tuple;

/**
 * Abstract syntax for a single defintion (which can be nested) with the form
 * <pre>
 *    def name(formals) = body
 * </pre> 
 * 
 * @author mbickford
 */

public class Definition {

	String name;
	List<String> formals;
	OrcProcess body;

	public Definition(String name, List<String> formals, OrcProcess body) {
		this.name = name;
		this.formals = formals;
		this.body = body;
	}
	/** 
	 * To resolve names in a definition, treat the formals as bound variables
	 * in the body. Since we give precedence to the bound vars over the vals, we
	 * don't have to remove the formals from the vals.
	 */
	public void resolveNames(List<String> bound, List<String> vals){
		List<String> bound_on_right = new ArrayList<String>();
		bound_on_right.addAll(bound);
		bound_on_right.addAll(formals);	
		body = body.resolveNames(bound_on_right,vals); //_in_body);
	}
	
	public String toString() {
		return "def " + name + Tuple.format('(', formals, ", ", ')') + " =\n   "
				+ body + "\n";
	}

}

