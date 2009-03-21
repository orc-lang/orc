package orc.ast.oil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.oil.arg.Constant;
import orc.ast.oil.arg.Field;
import orc.ast.oil.arg.Site;
import orc.ast.oil.arg.Var;
import orc.env.Env;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.runtime.nodes.Assign;
import orc.runtime.nodes.Fork;
import orc.runtime.nodes.Leave;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Store;
import orc.runtime.nodes.Unwind;

/**
 * Compiles an oil syntax tree into an execution graph.
 * Every node is compiled relative to an "output" node that represents
 * the "rest of the program". Thus the tree of compiled nodes is created bottom up.
 * @param output This is the node to which output (publications) will be directed.
 * @return A new node.
 */
public final class Compiler implements Visitor<Node> {
	private Node output;
	private boolean isTail;
	
	private Compiler(Node output) {
		this.output = output;
		isTail = (output instanceof orc.runtime.nodes.Return
				|| output.isTerminal());
	}
	
	private Node unwind(int size) {
		// tail nodes will ignore the environment stack
		// so there's no need to unwind
		if (isTail) return output;
		else return new Unwind(output, size);
	}
	
	public static Node compile(Expr expr, Node output) {
		Compiler compiler = new Compiler(output);
		return expr.accept(compiler);
	}

	public Node visit(Bar expr) {
		return new Fork(expr.left.accept(this), expr.right.accept(this));
	}

	public Node visit(Call expr) {
		return new orc.runtime.nodes.Call(expr.callee, expr.args, output);
	}

	public Node visit(Defs expr) {
		// find variables free ONLY in the defs themselves
		// (unlike addIndices which includes the body)
		Set<Var> free = new TreeSet<Var>();
		Set<Integer> indices = new TreeSet<Integer>();
		int depth = expr.defs.size();
		for (Def d : expr.defs) d.addIndices(indices, depth);
		for (Integer i : indices) free.add(new Var(i));
	
		// compile the defs
		List<orc.runtime.nodes.Def> newdefs = new LinkedList<orc.runtime.nodes.Def>();
		for (Def d : expr.defs) {
			newdefs.add(compileDef(d));	
		}

		Node newbody = compile(expr.body, unwind(newdefs.size()));
		return new orc.runtime.nodes.Defs(newdefs, newbody, free);
	}
	
	private static orc.runtime.nodes.Def compileDef(final Def def) {
		// rename free variables in the body
		// so that when we construct closure environments
		// we can omit the non-free variables
		Set<Var> free = def.freeVars();
		final HashMap<Integer,Integer> map = new HashMap<Integer, Integer>();
		int i = free.size()-1;
		for (Var v : free) map.put(v.index + def.arity, (i--) + def.arity);
		RenameVariables.rename(def.body, new RenameVariables.Renamer() {
			public int rename(int var) {
				if (var < def.arity) return var;
				return map.get(var);
			}
		});
		
		orc.runtime.nodes.Node newbody = compile(def.body, new orc.runtime.nodes.Return());
		return new orc.runtime.nodes.Def(def.arity, newbody, free, def.location);
	}

	public Node visit(Silent expr) {
		return new orc.runtime.nodes.Silent();
	}

	public Node visit(Pull expr) {
		return new orc.runtime.nodes.Subgoal(
				compile(expr.left, unwind(1)),
				compile(expr.right, new orc.runtime.nodes.Store()));
	}

	public Node visit(Push expr) {
		return compile(expr.left, new Assign(compile(expr.right, unwind(1))));
	}

	public Node visit(Semi expr) {
		return new orc.runtime.nodes.Semi(
				compile(expr.left, new Leave(output)),
				expr.right.accept(this));
	}

	public Node visit(WithLocation expr) {
		return new orc.runtime.nodes.WithLocation(
				expr.expr.accept(this),
				expr.location);
	}

	public Node visit(Constant arg) {
		return new orc.runtime.nodes.Let(arg, output);
	}

	public Node visit(Field arg) {
		return new orc.runtime.nodes.Let(arg, output);
	}

	public Node visit(Site arg) {
		return new orc.runtime.nodes.Let(arg, output);
	}

	public Node visit(Var arg) {
		return new orc.runtime.nodes.Let(arg, output);
	}

	public Node visit(Atomic atomic) {
		return new orc.runtime.transaction.Atomic(
				compile(atomic.body, new Store()),
				output);
	}
	
	public Node visit(Isolated expr) {
		return new orc.runtime.nodes.Isolate(
				compile(expr.body,
						new orc.runtime.nodes.Unisolate(output)));
	}

	public Node visit(HasType hasType) {
		return hasType.body.accept(this);
	}

	public Node visit(TypeDecl typeDecl) {
		return typeDecl.body.accept(this);
	}
}
