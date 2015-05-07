package orc.ast.oil;

import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Store;
import orc.runtime.nodes.Unwind;

public class Pull extends Expr {

	public Expr left;
	public Expr right;
	
	public Pull(Expr left, Expr right)
	{
		this.left = left;
		this.right = right;
	}
	
	
	@Override
	public Node compile(Node output) {	
		return new orc.runtime.nodes.Subgoal(left.compile(new Unwind(output)), right.compile(new Store()));
	}

	
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		left.addIndices(indices,depth+1); // Pull binds a variable on the left
		right.addIndices(indices,depth);
	}

	public String toString() {
		return "(" + left.toString() + " << " + right.toString() + ")";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
