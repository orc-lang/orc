package orc.ast.simple.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.expression.argument.Site;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.compiletime.UnboundVariableException;
import orc.runtime.nodes.Node;
import orc.runtime.values.Value;

public class Let extends Expression {

	public List<Argument> args;
	
	public Let(List<Argument> args)
	{
		this.args = args;
	}
	
	/* Special constructor for singleton */
	public Let(Argument arg)
	{
		this.args = new LinkedList<Argument>(); 
		this.args.add(arg);
	}
	
	/* Special constructor for empty let */
	public Let()
	{
		this.args = new LinkedList<Argument>();
	}
	
	@Override
	public Expression subst(Argument a, FreeVariable x) {
		return new Let(Argument.substAll(args, a, x));
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.expression.Expression#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Expression subst(Type T, FreeTypeVariable X) {
		return this;
	}
	
	public Set<Variable> vars() {
		Set<Variable> freeset = new HashSet<Variable>();
		for(Argument a : args)	{
			a.addFree(freeset);
		}
		return freeset;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars) throws UnboundVariableException {
		if (args.size() == 1) {
			// If there is only one arg, use it directly as an expression
			return args.get(0).convert(vars);
		}
		else {
			// Otherwise, use the tuple creation site
			return new orc.ast.oil.expression.Call(new orc.ast.oil.expression.argument.Site(orc.ast.sites.Site.LET), 
												   Argument.convertAll(args,vars),
												   new LinkedList<orc.ast.oil.type.Type>());
		}
	}
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();

		s.append("let");
		s.append('(');
		for (int i = 0; i < args.size(); i++) {
			if (i > 0) { s.append(", "); }
			s.append(args.get(i));
		}
		s.append(')');
		
		return s.toString();
	}
}