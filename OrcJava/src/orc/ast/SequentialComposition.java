/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.ast;

import java.util.ArrayList;
import java.util.List;

import orc.runtime.nodes.Assign;
import orc.runtime.nodes.Fork;
import orc.runtime.nodes.Node;

/**
 * Abstract syntax tree for
 * <pre>
 *    left >[!] [var]> right
 * </pre>
 * Both ! and var are optional.
 * If ! is present, then publish is true and the var should be output.
 * @author wcook
 */
public class SequentialComposition extends OrcProcess {
	OrcProcess left;
	String var;
	boolean publish;
	OrcProcess right;

	public SequentialComposition(OrcProcess left, String var,
			boolean publish, OrcProcess right) {
		this.left = left;
		this.var = var;
		this.publish = publish;
		this.right = right;
	}
	
	/** 
	 * To resolve names in a sequential composition, 
	 * add the var to the value variables for the process on the right.
	 * Also, if it is on the list of bound variable, we have to remove it
	 * from that list for the process on the right (otherwise it will be
	 * ambiguous, and we resolve ambiguity in favor of bound variable, which
	 * would be wrong in this case).
	 */
	public OrcProcess resolveNames(List<String> bound, List<String> vals){
		OrcProcess rightRes = null;
		List<String> vals_on_right = new ArrayList<String>();
		vals_on_right.addAll(bound);
		vals_on_right.add(var);
		
		if (bound.contains(var)){
			List<String> bound_on_right = new ArrayList<String>();
			bound_on_right.addAll(bound);
			bound_on_right.remove(var);
			rightRes = right.resolveNames(bound_on_right,vals_on_right);
		}
		else {
			rightRes = right.resolveNames(bound,vals_on_right);
		}
		OrcProcess leftRes = left.resolveNames(bound,vals);
		return new SequentialComposition(leftRes, var, publish, rightRes);
		
	}
	
	/** 
	 * Compile the right side relative to the overall program output.
	 * If the variable is present then create an 
	 * assign node.
	 * If the result should be published, create a fork.
	 * This is because
	 * <pre>
	 *    f >!v> g
	 * </pre>
	 * is equivalent to 
	 * <pre>
	 *    f >v> (let(x) | g)
	 * </pre>
	 * Finally, compile the left side and send its output 
	 * to the newly created node for the right side.
	 * @see orc.ast.OrcProcess#compile(orc.runtime.nodes.Node)
	 */
	public Node compile(Node output,List<orc.ast.Definition> defs) {
		Node node = right.compile(output,defs);
		if (var != null && var.length() > 0)
			node = new Assign(var, node);
		if (publish)
			node = new Fork(node, output);
		return left.compile(node,defs);
	}

	public String toString() {
		return "{" + left + "\n>" + (publish ? "!" : "") + var + "> " + right + "}";
	}
}
