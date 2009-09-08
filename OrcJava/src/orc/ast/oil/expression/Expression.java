package orc.ast.oil.expression;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.oil.ContextualVisitor;
import orc.ast.oil.Visitor;
import orc.ast.oil.expression.argument.Variable;
import orc.ast.oil.type.Type;
import orc.ast.sites.JavaSite;
import orc.ast.sites.OrcSite;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Pub;
import orc.type.TypingContext;

/**
 * Base class for the portable (.oil, for Orc Intermediate Language) abstract syntax tree.
 * 
 * @author dkitchin
 *
 */

public abstract class Expression {
	/* Typechecking */
	
	/* Given a context, infer this expression's type */
	public abstract orc.type.Type typesynth(TypingContext ctx) throws TypeException;
	
	
	/* Check that this expression has type T in the given context. 
	 * 
	 * Some expressions will always have inferred types, so
	 * the default checking behavior is to infer the type and make
	 * sure that the inferred type is a subtype of the checked type.
	 */
	public void typecheck(TypingContext ctx, orc.type.Type T) throws TypeException {
		orc.type.Type S = typesynth(ctx);
		if (!S.subtype(T)) {
			throw new SubtypeFailureException(S,T);
		}
	}

	
	
	/**
	 * Find the set of free variables in this expression. 
	 * 
	 * @return 	The set of free variables.
	 */
	public final Set<Variable> freeVars() {
		Set<Integer> indices = new TreeSet<Integer>();
		this.addIndices(indices, 0);
		
		Set<Variable> vars = new TreeSet<Variable>();
		for (Integer i : indices) {
			vars.add(new Variable(i));
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
	public abstract void addIndices(Set<Integer> indices, int depth);
	
	public abstract <E> E accept(Visitor<E> visitor);
	//public abstract <E,C> E accept(ContextualVisitor<E,C> cvisitor, C initialContext);
	
	public abstract orc.ast.xml.expression.Expression marshal() throws CompilationException;
	
	/**
	 * Convenience method, to marshal a list of expressions.
	 */
	public static orc.ast.xml.expression.Expression[] marshalAll(List<Expression> es) throws CompilationException {
		
		orc.ast.xml.expression.Expression[] newes = new orc.ast.xml.expression.Expression[es.size()];
		int i = 0;
		for (Expression e : es) {
			newes[i++] = e.marshal();
		}
		
		return newes;
	}
	
}