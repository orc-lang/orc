package orc.ast.simple.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.expression.argument.Site;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
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
	public Expression subst(Argument a, NamedVariable x) {
		List<Argument> newargs = new LinkedList<Argument>();		
		for (Argument b : args) {
			newargs.add(b.subst(a, x));
		}
		return new Let(newargs);
	}
	
	public Set<Variable> vars() {
		Set<Variable> freeset = new HashSet<Variable>();
		for(Argument a : args)	{
			a.addFree(freeset);
		}
		return freeset;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<String> typevars) throws UnboundVariableException {
		if (args.size() == 1) {
			// If there is only one arg, use it directly as an expression
			return args.get(0).convert(vars);
		}
		
		// Otherwise, use the tuple creation site
		
		List<orc.ast.oil.expression.argument.Argument> newargs = new ArrayList<orc.ast.oil.expression.argument.Argument>();
		for(Argument arg : args) {
			newargs.add(arg.convert(vars));
		}
		
		return new orc.ast.oil.expression.Call(new orc.ast.oil.expression.argument.Site(orc.ast.sites.Site.LET), newargs);
	}
}