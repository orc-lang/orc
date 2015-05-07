//
// Compiler.java -- Java class Compiler
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.oil.expression.Atomic;
import orc.ast.oil.expression.Def;
import orc.ast.oil.expression.Parallel;
import orc.ast.oil.expression.Call;
import orc.ast.oil.expression.Catch;
import orc.ast.oil.expression.DeclareDefs;
import orc.ast.oil.expression.Expression;
import orc.ast.oil.expression.HasType;
import orc.ast.oil.expression.Isolated;
import orc.ast.oil.expression.Pruning;
import orc.ast.oil.expression.Sequential;
import orc.ast.oil.expression.Otherwise;
import orc.ast.oil.expression.Stop;
import orc.ast.oil.expression.Throw;
import orc.ast.oil.expression.DeclareType;
import orc.ast.oil.expression.WithLocation;
import orc.ast.oil.expression.argument.Constant;
import orc.ast.oil.expression.argument.Field;
import orc.ast.oil.expression.argument.Site;
import orc.ast.oil.expression.argument.Variable;
import orc.runtime.nodes.Assign;
import orc.runtime.nodes.Fork;
import orc.runtime.nodes.Leave;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.PopHandler;
import orc.runtime.nodes.Pub;
import orc.runtime.nodes.PushHandler;
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
	private final Node output;
	private final boolean isTail;

	private Compiler(final Node output) {
		this.output = output;
		isTail = output instanceof orc.runtime.nodes.Return || output.isTerminal();
	}

	private Node unwind(final int size) {
		// tail nodes will ignore the environment stack
		// so there's no need to unwind
		if (isTail) {
			return output;
		} else {
			return new Unwind(output, size);
		}
	}

	public static Node compile(final Expression expr) {
		return compile(expr, new Pub());
	}

	public static Node compile(final Expression expr, final Node output) {
		final Compiler compiler = new Compiler(output);
		return expr.accept(compiler);
	}

	public Node visit(Parallel expr) {
		return new Fork(expr.left.accept(this), expr.right.accept(this));
	}

	public Node visit(final Call expr) {
		return new orc.runtime.nodes.Call(expr.callee, expr.args, output);
	}

	public Node visit(final DeclareDefs expr) {
		// find variables free ONLY in the defs themselves
		// (unlike addIndices which includes the body)
		final Set<Variable> free = new TreeSet<Variable>();
		final Set<Integer> indices = new TreeSet<Integer>();
		final int depth = expr.defs.size();
		for (final Def d : expr.defs) {
			d.addIndices(indices, depth);
		}
		for (final Integer i : indices) {
			free.add(new Variable(i));
		}

		// compile the defs
		final List<orc.runtime.nodes.Def> newdefs = new LinkedList<orc.runtime.nodes.Def>();
		for (final Def d : expr.defs) {
			newdefs.add(compileDef(d));
		}

		final Node newbody = compile(expr.body, unwind(newdefs.size()));
		return new orc.runtime.nodes.Defs(newdefs, newbody, free);
	}

	private static orc.runtime.nodes.Def compileDef(final Def def) {
		// rename free variables in the body
		// so that when we construct closure environments
		// we can omit the non-free variables
		final Set<Variable> free = def.freeVars();
		final HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		int i = free.size() - 1;
		for (final Variable v : free) {
			map.put(v.index + def.arity, i-- + def.arity);
		}
		RenameVariables.rename(def.body, new RenameVariables.Renamer() {
			public int rename(final int var) {
				if (var < def.arity) {
					return var;
				}
				return map.get(var);
			}
		});

		final orc.runtime.nodes.Node newbody = compile(def.body, new orc.runtime.nodes.Return());
		return new orc.runtime.nodes.Def(def.arity, newbody, free, def.location);
	}

	public Node visit(final Stop expr) {
		return new orc.runtime.nodes.Silent();
	}

	public Node visit(final Pruning expr) {
		return new orc.runtime.nodes.Subgoal(
				compile(expr.left, unwind(1)),
				compile(expr.right, new orc.runtime.nodes.Store()));
	}

	public Node visit(final Sequential expr) {
		return compile(expr.left, new Assign(compile(expr.right, unwind(1))));
	}

	public Node visit(final Otherwise expr) {
		return new orc.runtime.nodes.Semi(
				compile(expr.left, new Leave(output)),
				expr.right.accept(this));
	}

	public Node visit(final WithLocation expr) {
		return new orc.runtime.nodes.WithLocation(expr.body.accept(this), expr.location);
	}

	public Node visit(final Constant arg) {
		return new orc.runtime.nodes.Let(arg, output);
	}

	public Node visit(final Field arg) {
		return new orc.runtime.nodes.Let(arg, output);
	}

	public Node visit(final Site arg) {
		return new orc.runtime.nodes.Let(arg, output);
	}

	public Node visit(final Variable arg) {
		return new orc.runtime.nodes.Let(arg, output);
	}

	public Node visit(final Atomic atomic) {
		return new orc.runtime.transaction.Atomic(compile(atomic.body, new Store()), output);
	}

	public Node visit(final Isolated expr) {
		return new orc.runtime.nodes.Isolate(compile(expr.body, new orc.runtime.nodes.Unisolate(output)));
	}

	public Node visit(final HasType hasType) {
		return hasType.body.accept(this);
	}

	public Node visit(final DeclareType typeDecl) {
		return typeDecl.body.accept(this);
	}

	public Node visit(final Throw throwExpr) {
		final Node throwNode = new orc.runtime.nodes.Throw();
		return compile(throwExpr.exception, throwNode);
	}

	public Node visit(final Catch catchExpr) {
		final Node popNode = new PopHandler(output);
		final Node tryBlock = compile(catchExpr.tryBlock, popNode);
		final orc.runtime.nodes.Def handler = compileDef(catchExpr.handler);
		return new PushHandler(handler, tryBlock, output);
	}
}
