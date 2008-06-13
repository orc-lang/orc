package orc.ast.oil;

import java.util.Set;

import orc.runtime.nodes.Fork;
import orc.runtime.nodes.Node;

public class Bar extends Expr {

	Expr left;
	Expr right;
	
	public Bar(Expr left, Expr right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public Node compile(Node output) {
		return new Fork(left.compile(output), right.compile(output));
	}

	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		left.addIndices(indices, depth);
		right.addIndices(indices, depth);
	}
	
	public String toString() {
		return "(" + left.toString() + " | " + right.toString() + ")";
	}

	@Override
	public orc.orchard.oil.Expression marshal() {
		return new orc.orchard.oil.Bar(left.marshal(), right.marshal());
	}
}
