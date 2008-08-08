package orc.ast.oil;

import java.util.HashSet;
import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

public class Silent extends Expr {

	@Override
	public Node compile(Node output) {
		return new orc.runtime.nodes.Silent();
	}
	
	public String toString() {
		return ".";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
