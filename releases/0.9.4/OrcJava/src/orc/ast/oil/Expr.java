package orc.ast.oil;

import java.util.HashSet;
import java.util.Set;

import orc.ast.oil.arg.Var;
import orc.env.Env;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Pub;
import orc.type.Type;

/**
 * Base class for the portable (.oil, for Orc Intermediate Language) abstract syntax tree.
 * 
 * @author dkitchin
 *
 */

public abstract class Expr {
	/**
	 * Compiles an oil syntax tree into an execution graph.
	 * Every node is compiled relative to an "output" node that represents
	 * the "rest of the program". Thus the tree of compiled nodes is created bottom up.
	 * @param output This is the node to which output (publications) will be directed.
	 * @return A new node.
	 */
	public abstract orc.runtime.nodes.Node compile(orc.runtime.nodes.Node output);
	
	/* Typechecking */
	
	/* Given a context, infer this expression's type */
	public abstract Type typesynth(Env<Type> ctx) throws TypeException;
	
	
	/* Check that this expression has type t in the given context. 
	 * 
	 * Some expressions will always have inferred types, so
	 * the default checking behavior is to infer the type and make
	 * sure that the inferred type is a subtype of the checked type.
	 */
	public void typecheck(Type T, Env<Type> ctx) throws TypeException {
		Type S = typesynth(ctx);
		if (!S.subtype(T)) {
			throw new SubtypeFailureException(S,T);
		}
	}

	
	
	/**
	 * Find the set of free variables in this expression. 
	 * 
	 * @return 	The set of free variables.
	 */
	public Set<Var> freeVars() {
		Set<Integer> indices = new HashSet<Integer>();
		this.addIndices(indices, 0);
		
		Set<Var> vars = new HashSet<Var>();
		for (Integer i : indices) {
			vars.add(new Var(i));
		}
		
		return vars;
	}
	
	/**
	 * If this expression has any indices which are >= depth,
	 * add (index - depth) to the index set accumulator. The depth 
	 * increases each time this method recurses through a binder.
	 * 
	 * The default implementation is to assume the expression
	 * has no free variables, and thus do nothing. Expressions
	 * which contain variables or subexpressions override this
	 * behavior.
	 * 
	 * @param indices   The index set accumulator.
	 * @param depth    The minimum index for a free variable.
	 */
	public void addIndices(Set<Integer> indices, int depth) {
		return;
	}
	
	public abstract <E> E accept(Visitor<E> visitor);
}
