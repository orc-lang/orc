//
// BackendDemo.java -- Java class BackendDemo
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import orc.ast.oil.expression.Call;
import orc.ast.oil.expression.Catch;
import orc.ast.oil.expression.DeclareDefs;
import orc.ast.oil.expression.DeclareType;
import orc.ast.oil.expression.Def;
import orc.ast.oil.expression.Expression;
import orc.ast.oil.expression.HasType;
import orc.ast.oil.expression.Otherwise;
import orc.ast.oil.expression.Parallel;
import orc.ast.oil.expression.Pruning;
import orc.ast.oil.expression.Sequential;
import orc.ast.oil.expression.Stop;
import orc.ast.oil.expression.Throw;
import orc.ast.oil.expression.WithLocation;
import orc.ast.oil.expression.argument.Argument;
import orc.ast.oil.expression.argument.Constant;
import orc.ast.oil.expression.argument.Field;
import orc.ast.oil.expression.argument.Site;
import orc.ast.oil.expression.argument.Variable;
import orc.ast.oil.visitor.Visitor;
import orc.env.Env;
import orc.env.LookupFailureException;
import orc.error.OrcError;
import orc.error.compiletime.CompilationException;

/**
 * An example of a custom compiler backend.
 * This backend invokes the compiler and then
 * translates the compiled Orc into a string
 * representation which it prints.
 * 
 * @author quark
 */
public final class BackendDemo {
	public static void main(final String[] args) throws CompilationException, IOException {
		// read command-line arguments
		final Config cfg = new Config();
		cfg.processArgs(args);
		// compile the specified input stream to OIL
		final Expression e = Orc.compile(cfg);
		if (e == null) {
			return;
		}
		// use the visitor on the OIL to output the
		// new representation
		final PrintWriter out = new PrintWriter(System.out);
		e.accept(new BackendVisitorDemo(out));
		out.flush();
		out.close();
	}
}

/**
 * Write a string representation of OIL to a PrintWriter.
 * The hardest part is translating De Bruijn indices (OIL variables)
 * into named variables; to do this we keep an environment stack
 * mapping indices to variable names and carefully manage the stack
 * as we visit the various binding operations.
 */
final class BackendVisitorDemo implements Visitor<Void> {
	/** Environment of variable names */
	private final Env<String> variableNames = new Env<String>();
	/** Next free variable identifier; used to generate unique names. */
	private int nextVariableId = 1;
	private final PrintWriter out;

	public BackendVisitorDemo(final PrintWriter out) {
		this.out = out;
	}

	/** Look up a variable name in the environment. */
	private String lookup(final int offset) {
		try {
			return variableNames.lookup(offset);
		} catch (final LookupFailureException e) {
			throw new OrcError(e);
		}
	}

	/** Enter the scope of a variable bindingx. */
	private void enterScope(final String name) {
		// suffix names with a unique identifier since
		// they are not guaranteed to be unique in scope
		// after optimizations
		variableNames.add(name + "_" + nextVariableId);
		++nextVariableId;
	}

	/** Leave the scope of n variable bindings. */
	private void leaveScope(final int n) {
		variableNames.unwind(n);
		nextVariableId -= n;
	}

	/** Variables are translated into names using the environment. */
	public Void visit(final Variable arg) {
		out.print(arg.resolveGeneric(variableNames));
		return null;
	}

	/** Constants are represented by their string representations. */
	public Void visit(final Constant arg) {
		out.print(arg);
		return null;
	}

	/** Fields are represented by their string representations. */
	public Void visit(final Field arg) {
		out.print(arg);
		return null;
	}

	/** Silent is represented by its string representation. */
	public Void visit(final Stop arg) {
		out.print(arg);
		return null;
	}

	/** Sites are represented by their string representations. */
	public Void visit(final Site arg) {
		out.print(arg);
		return null;
	}

	public Void visit(final Parallel expr) {
		out.print("(");
		expr.left.accept(this);
		out.print(" | ");
		expr.right.accept(this);
		out.print(")");
		return null;
	}

	public Void visit(final Pruning expr) {
		out.print("(");
		enterScope(expr.name == null ? "v" : expr.name);
		expr.left.accept(this);
		out.print(" <" + lookup(0) + "< ");
		leaveScope(1);
		expr.right.accept(this);
		out.print(")");
		return null;
	}

	public Void visit(final Sequential expr) {
		out.print("(");
		expr.left.accept(this);
		enterScope(expr.name == null ? "v" : expr.name);
		out.print(" >" + lookup(0) + "> ");
		expr.right.accept(this);
		leaveScope(1);
		out.print(")");
		return null;
	}

	public Void visit(final Otherwise expr) {
		out.print("(");
		expr.left.accept(this);
		out.print(" ; ");
		expr.right.accept(this);
		out.print(")");
		return null;
	}

	public Void visit(final Call expr) {
		expr.callee.accept(this);
		out.print("(");
		final Iterator<Argument> argsi = expr.args.iterator();
		if (argsi.hasNext()) {
			argsi.next().accept(this);
			while (argsi.hasNext()) {
				out.print(", ");
				argsi.next().accept(this);
			}
		}
		out.print(")");
		return null;
	}

	public Void visit(final DeclareDefs expr) {
		out.println("(");
		// create a new binding for each definition in the group
		for (final Def def : expr.defs) {
			enterScope(def.name == null ? "d" : def.name);
		}
		// defi will track the index of the current definition in the group,
		// so we can find its name in the environment
		int defi = expr.defs.size() - 1;
		for (final Def def : expr.defs) {
			out.print("def " + lookup(defi) + "(");
			// create new bindings for arguments
			for (int i = 0; i < def.arity; ++i) {
				enterScope("a");
			}
			if (def.arity > 0) {
				// look up argument names in the environment
				out.print(lookup(def.arity - 1));
				for (int i = def.arity - 2; i >= 0; --i) {
					out.print(", ");
					out.print(lookup(i));
				}
			}
			out.print(") = ");
			def.body.accept(this);
			leaveScope(def.arity);
			out.println();
			--defi;
		}
		expr.body.accept(this);
		leaveScope(expr.defs.size());
		out.println(")");
		return null;
	}

	/** Ignore source location tags */
	public Void visit(final WithLocation expr) {
		return expr.body.accept(this);
	}

	/** Ignore type assertions */
	public Void visit(final HasType hasType) {
		return hasType.body.accept(this);
	}

	/** Ignore type declarations */
	public Void visit(final DeclareType typeDecl) {
		return typeDecl.body.accept(this);
	}

	//TODO:
	public Void visit(final Catch catchExpr) {
		return null;
	}

	//TODO:
	public Void visit(final Throw throwExpr) {
		return null;
	}
}
