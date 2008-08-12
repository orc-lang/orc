package orc.runtime.nodes;

import java.util.List;
import orc.ast.simple.arg.Var;

/**
 * 
 * A unit of syntax that encapsulates an expression definition.
 * 
 * Groups of mutually recursive definitions are embedded in the execution graph
 * by a Defs node.
 * 
 * @author dkitchin
 * 
 */

public class Def {

	public int arity;
	public Node body;

	public Def(int arity, Node body) {
		this.arity = arity;
		this.body = body;
	}
	
}
