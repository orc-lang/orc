/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.*;

import orc.runtime.nodes.Node;
import orc.runtime.nodes.Return;


/**
 * Abstract syntax for defintions (which can be nested) with the form
 * <pre>
 *    (def name(formals) = body )*
 *    rest
 * </pre> 
 * Where rest is the program in which the names are bound
 * @author wcook
 */

public class Define extends OrcProcess {

	List<Definition> defs;
	OrcProcess rest;

	public Define(List<Definition> defs,OrcProcess rest) {
		this.defs = defs;
		this.rest = rest;
	}
	
	/** 
	 * To resolve names in a define, just resolve in each part.
	 */
	public OrcProcess resolveNames(List<String> bound, List<String> vals){
		for (Definition d : defs)
			d.resolveNames(bound,vals);
		rest = rest.resolveNames(bound,vals);
		return this;
	}

	/**
	 * Compiles the body with output to a return node.
	 * Creates a define node (which will created the binding) and
	 * then invoke the rest of the program.
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output,List<orc.ast.Definition> defsin) {
		List<orc.runtime.nodes.Definition> defNodes = new ArrayList<orc.runtime.nodes.Definition>();
		List<orc.ast.Definition> alldefs = new ArrayList<orc.ast.Definition>();
		alldefs.addAll(defsin);
		// When compiling the defs we need to already know the names of all the defs
		// not just the defs that are in front of a given def. That way we can recognize
		// recursion.
		alldefs.addAll(defs);
		for (Definition d : defs) {
			defNodes.add(new orc.runtime.nodes.Definition(d.name, d.formals, 
					                                      d.body.compile(new Return(),alldefs)));
			//alldefs.add(d); So don't add the defs one by one!
		}
				
		Node restNode = rest.compile(output,alldefs);
		return new orc.runtime.nodes.Define(defNodes, restNode);
	}

	public String toString() {
		String defstr = new String();
		for (Definition d : defs)
			defstr = defstr + d.toString();
		return defstr + rest;
	}
	public OrcProcess addDefs(OrcProcess p) {
		 return new Define(defs,rest.addDefs(p));
		}
}
