package orc.ast.extended.expression;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import orc.ast.extended.Visitor;
import orc.ast.extended.declaration.ValDeclaration;
import orc.ast.extended.declaration.type.Constructor;
import orc.ast.extended.declaration.type.DatatypeDeclaration;
import orc.ast.extended.pattern.CallPattern;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.pattern.PatternSimplifier;
import orc.ast.extended.pattern.TuplePattern;
import orc.ast.extended.pattern.VariablePattern;
import orc.ast.extended.type.NamedType;
import orc.ast.extended.type.Type;
import orc.ast.simple.expression.WithLocation;
import orc.error.OrcError;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.PatternException;
import orc.runtime.ReverseListIterator;

public class Choice extends Expression {

	public List<Expression> choices;

	public Choice(List<Expression> choices)
	{
		this.choices = choices;
	}
	
	@Override
	public orc.ast.simple.expression.Expression simplify() throws CompilationException {
		orc.ast.simple.expression.Expression expr;
				
		/*
		 * Break up the expressions into their constituents.
		 * Each expression must be of the form:
		 * 
		 * val p = F
		 * G
		 * 
		 * Then, reform those constituents into the components
		 * of the encoded form:
		 *
		 * type branch[a,b...] = A(a) | B(b) ...
		 * val temp = A(F) | B(F') ...
		 * temp >A(p)> G | temp >B(p')> G' ...
		 * 
		 * 
		 */
		
				
		// branch
		// (manufacture a unique variable name)
		String branch = "_branch" + choices.hashCode();
		
		// temp
		// (manufacture a unique variable name)
		String temp = "_temp" + choices.hashCode();
		
		// [a,b,...]
		List<String> typevars = new LinkedList<String>();
		
		// A(a) | B(b) ...
		List<Constructor> constructors = new LinkedList<Constructor>();
		
		// A(F) | B(F') ...
		Expression competitors = new Stop();
		
		// temp >A(p)> G | temp >B(p')> G' ...
		Expression consequents = new Stop();
		
		for (Expression c : choices) {
			Declare decl;
			ValDeclaration vald;
			try {
				decl = (Declare)c;
				vald = (ValDeclaration)decl.d;
			}
			catch (ClassCastException cce) {
				throw new CompilationException("Subexpressions of ++ must be a val declaration followed by an expression.");
			}
			
			Pattern p = vald.p;
			Expression F = vald.e;
			Expression G = decl.e;
			
			// Manufacture unique variable names
			String tag = ("_tag" + c.hashCode());
			String typevar = ("_tv" + c.hashCode());
			
			typevars.add(typevar);
			
			List<Type> ts = new LinkedList<Type>();
			ts.add(new NamedType(typevar));
			constructors.add(new Constructor(tag, ts));
			
			List<Expression> es = new LinkedList<Expression>();
			es.add(F);
			Expression competitor = new Call(new Name(tag), es);
			competitors = new Parallel(competitor, competitors);
			
			List<Pattern> ps = new LinkedList<Pattern>();
			ps.add(p);
			Expression consequent = new Sequential(new Name(temp), G, new CallPattern(tag, ps));
			consequents = new Parallel(consequent, consequents);
		}
		
		Expression body = consequents;
		body = new Declare(new ValDeclaration(new VariablePattern(temp), competitors), body);
		body = new Declare(new DatatypeDeclaration(branch, constructors, typevars), body);
		
		return new WithLocation(body.simplify(), getSourceLocation());
	}
	
	public String toString() {
		return "(" + join(choices, " ++ ") + ")";
	}	


	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
