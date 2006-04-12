/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.ArrayList;
import java.util.List;

import orc.runtime.nodes.Node;
import orc.runtime.nodes.Param;
import orc.runtime.values.Tuple;


/**
 * Abstract syntax for calls. Includes both site calls and definition calls
 * @author wcook
 */
public class Call extends OrcProcess {
	String name;
	List<OrcProcess> args;

	static int varNum;
	
	public Call(String name, List<OrcProcess> args) {
		this.name = name;
		if (args == null)
	  	 	args = new ArrayList<OrcProcess>();
		this.args = args;
	}

	/** 
	 * 
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output) {
		for (OrcProcess p : args)
			if (p.asParam() == null)
				return translate().compile(output);
		
		List<Param> params = new ArrayList<Param>();
		for (OrcProcess p : args)
			params.add(p.asParam());
		
		return new orc.runtime.nodes.Call(name, params, output); 
	}
	
	/**
	 * Translates nested calls:
	 * <pre>
	 * 	  M(A(), B())
	 * </pre>
	 * is interpreted as
	 * <pre>
	 *    M(a, b) where a = A(); b = B()
	 * </pre>
	 * @return process to be executed
	 */
	public OrcProcess translate() {
		List<OrcProcess> newArgs = new ArrayList<OrcProcess>();
		
		Call newCall = new Call(name, newArgs);
		AsymmetricParallelComposition where = 
			new AsymmetricParallelComposition(newCall);

		for (OrcProcess p : args)
			if (p.asParam() != null)
				newArgs.add(p);
			else {
				String newVar = "V" + varNum++;
				newArgs.add(new Variable(newVar));
				where.addBinding(newVar, p);
			}

		return where;
	}

	public String toString() {
		return name + Tuple.format('(', args, ", ", ')');
	}
}
