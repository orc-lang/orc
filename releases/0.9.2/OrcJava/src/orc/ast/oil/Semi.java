package orc.ast.oil;

import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

public class Semi extends Expr {

	public Expr left;
	public Expr right;
	
	public Semi(Expr left, Expr right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public Node compile(Node output) {
		return new orc.runtime.nodes.Semi(left.compile(output), right.compile(output));
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		left.addIndices(indices, depth);
		right.addIndices(indices, depth);
	}
	
	public String toString() {
		return "(" + left.toString() + " ; " + right.toString() + ")";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
