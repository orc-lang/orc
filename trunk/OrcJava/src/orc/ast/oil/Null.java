package orc.ast.oil;

import java.util.HashSet;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

public class Null extends Expr {

	@Override
	public Node compile(Node output) {
		return new orc.runtime.nodes.Silent();
	}
	
	public String toString() {
		return "null";
	}
	
	@Override
	public orc.orchard.oil.Expression marshal() {
		return new orc.orchard.oil.Null();
	}
}
