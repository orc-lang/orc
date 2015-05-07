package orc;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import orc.env.Env;
import orc.env.LookupFailureException;
import orc.error.OrcError;
import orc.error.compiletime.CompilationException;
import orc.ast.oil.*;
import orc.ast.oil.arg.*;

/**
 * An example of a custom compiler backend.
 * This backend invokes the compiler and then
 * translates the compiled Orc into a string
 * representation which it prints.
 * 
 * @author quark
 */
public final class BackendDemo {
	public static void main(String[] args) throws CompilationException, IOException {
		// read command-line arguments
		Config cfg = new Config();
		cfg.processArgs(args);
		// compile the specified input stream to OIL
		Expr e = Orc.compile(cfg.getInstream(), cfg);
		// use the visitor on the OIL to output the
		// new representation
		PrintWriter out = new PrintWriter(System.out);
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
	private Env<String> variableNames = new Env<String>();
	/** Next free variable identifier; used to generate unique names. */
	private int nextVariableId = 1;
	private PrintWriter out;
	
	public BackendVisitorDemo(PrintWriter out) {
		this.out = out;
	}
	
	/** Look up a variable name in the environment. */
	private String lookup(int offset) {
		try {
			return variableNames.lookup(offset);
		} catch (LookupFailureException e) {
			throw new OrcError(e);
		}
	}
	
	/** Enter the scope of n variable bindings. */
	private void enterScope(int n) {
		for (int i = 0; i < n; ++i) {
			variableNames.add("v"+nextVariableId);
			++nextVariableId;
		}
	}
	
	/** Leave the scope of n variable bindings. */
	private void leaveScope(int n) {
		variableNames.unwind(n);
		nextVariableId -= n;
	}
	
	/** Variables are translated into names using the environment. */
	public Void visit(Var arg) {
		out.print(arg.resolve(variableNames));
		return null;
	}

	/** Constants are represented by their string representations. */
	public Void visit(Constant arg) {
		out.print(arg);
		return null;
	}

	/** Fields are represented by their string representations. */
	public Void visit(Field arg) {
		out.print(arg);
		return null;
	}
	
	/** Silent is represented by its string representation. */
	public Void visit(Silent arg) {
		out.print(arg);
		return null;
	}
	
	/** Sites are represented by their string representations. */
	public Void visit(Site arg) {
		out.print(arg);
		return null;
	}

	public Void visit(Bar expr) {
		out.print("(");
		expr.left.accept(this);
		out.print(" | ");
		expr.right.accept(this);
		out.print(")");
		return null;
	}
	
	public Void visit(Pull expr) {
		out.print("(");
		this.enterScope(1);
		expr.left.accept(this);
		out.print(" <" + lookup(0) + "< ");
		this.leaveScope(1);
		expr.right.accept(this);
		out.print(")");
		return null;
	}

	public Void visit(Push expr) {
		out.print("(");
		expr.left.accept(this);
		this.enterScope(1);
		out.print(" >" + lookup(0) + "> ");
		expr.right.accept(this);
		this.leaveScope(1);
		out.print(")");
		return null;
	}

	public Void visit(Semi expr) {
		out.print("(");
		expr.left.accept(this);
		out.print(" ; ");
		expr.right.accept(this);
		out.print(")");
		return null;
	}

	public Void visit(Call expr) {
		expr.callee.accept(this);
		out.print("(");
		Iterator<Arg> argsi = expr.args.iterator();
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

	public Void visit(Defs expr) {
		out.println("(");
		// create a new binding for each definition in the group
		this.enterScope(expr.defs.size());
		// defi will track the index of the current definition in the group,
		// so we can find its name in the environment
		int defi = expr.defs.size() - 1;
		for (Def def : expr.defs) {
			out.print("def " + lookup(defi) + "(");
			// create new bindings for arguments
			this.enterScope(def.arity);
			if (def.arity > 0) {
				// look up argument names in the environment
				out.print(lookup(def.arity-1));
				for (int i = def.arity-2; i >= 0; --i) {
					out.print(", ");
					out.print(lookup(i));
				}
			}
			out.print(") = ");
			def.body.accept(this);
			this.leaveScope(def.arity);
			out.println();
			--defi;
		}
		expr.body.accept(this);
		this.leaveScope(expr.defs.size());
		out.println(")");
		return null;
	}
	
	public Void visit(Atomic atomic) {
		out.print("(atomic ");
		atomic.body.accept(this);
		out.print(")");
		return null;
	}
	
	public Void visit(Isolated expr) {
		out.print("(isolated ");
		expr.body.accept(this);
		out.print(")");
		return null;
	}

	/** Ignore source location tags */
	public Void visit(WithLocation expr) {
		return expr.expr.accept(this);
	}

	/** Ignore type assertions */
	public Void visit(HasType hasType) {
		return hasType.body.accept(this);
	}

	/** Ignore type declarations */
	public Void visit(TypeDecl typeDecl) {
		return typeDecl.body.accept(this);
	}
}