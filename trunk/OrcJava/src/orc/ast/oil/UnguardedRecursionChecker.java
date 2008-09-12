package orc.ast.oil;

import java.util.LinkedList;

import orc.ast.oil.arg.Constant;
import orc.ast.oil.arg.Field;
import orc.ast.oil.arg.Site;
import orc.ast.oil.arg.Var;
import orc.env.Env;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

/**
 * Check for unguarded recursion in function definitions.
 * TODO: check for unguarded mutual recursion as well.
 * @author quark
 */
public class UnguardedRecursionChecker extends Walker {
	public static void check(Expr expr) throws CompilationException {
		UnguardedRecursionChecker checker = new UnguardedRecursionChecker();
		expr.accept(checker);
		if (checker.problems.size() > 0)
			throw checker.problems.getFirst();
	}
	
	/** A binding is true if it is unguarded in the current scope. */
	private Env<Boolean> env = new Env<Boolean>();
	/** Accumulate problems. */
	private LinkedList<CompilationException> problems = new LinkedList<CompilationException>();
	private SourceLocation location;
	
	private UnguardedRecursionChecker() {}
	
	@Override
	public Void visit(Defs expr) {
		// save the environment
		Env<Boolean> outerEnv = env;
		int ndefs = expr.defs.size();
		int whichdef = 0;
		// check each def in turn
		for (Def def : expr.defs) {
			env = envForDef(ndefs, whichdef, def.arity, outerEnv);
			def.body.accept(this);
			++whichdef;
		}
		// check the body
		env = outerEnv;
		for (int i = 0; i < ndefs; ++i) {
			env = env.add(false);
		}
		expr.body.accept(this);
		// restore the environment
		env = outerEnv;
		return null;
	}
	
	@Override
	public Void visit(Pull expr) {
		// The pull adds a binding to the LHS,
		// but we don't care what it is.
		env = env.add(false);
		expr.left.accept(this);
		env = env.unwind(1);
		
		expr.right.accept(this);
		return null;
	}
	
	@Override
	public Void visit(Push expr) {
		expr.left.accept(this);
		
		// The push adds a binding to the RHS,
		// and changes all previous bindings to
		// false within the RHS
		Env<Boolean> outerEnv = env;
		env = envForPush(outerEnv);
		expr.right.accept(this);
		env = outerEnv;
		return null;
	}

	@Override
	public Void visit(Call expr) {
		// dispatch on the type of the callee
		expr.callee.accept(this);
		// unguarded recursion in call arguments is ok
		return null;
	}

	@Override
	public Void visit(Var arg) {
		// check whether the var refers to an unguarded outer
		// definition in the environment
		if (arg.resolve(env)) {
			CompilationException e = new CompilationException(
					"Unguarded recursion found.");
			e.setSourceLocation(location);
			problems.add(e);
		}
		return null;
	}
	
	@Override
	public Void visit(WithLocation expr) {
		// while we are visiting the annotated expression,
		// we need to use the given location for error messages
		SourceLocation outerLocation = location;
		location = expr.location;
		super.visit(expr);
		location = outerLocation;
		return null;
	}
	
	/**
	 * Generate a new environment for checking the body of a def. Specifically,
	 * the value for this def's binding is true, and for all sibling defs it's false.
	 */
	private static Env<Boolean> envForDef(int ndefs, int whichdef, int arity, Env<Boolean> env) {
		Env<Boolean> out = env;
		for (int i = 0; i < whichdef; ++i) {
			out = out.add(false);
		}
		out = out.add(true);
		for (int i = whichdef+1; i < ndefs; ++i) {
			out = out.add(false);
		}
		for (int i = 0; i < arity; ++i) {
			out = out.add(false);
		}
		return out;
	}
	
	/**
	 * Generate an environment for checking the RHS of a push.
	 * Specifically, all bindings are false (since they are guarded by the push).
	 */
	private static Env<Boolean> envForPush(Env<Boolean> env) {
		Env<Boolean> out = new Env<Boolean>();
		for (Boolean _ : env.items()) {
			out = out.add(false);
		}
		// plus one binding for the push itself
		return out.add(false);
	}
}
