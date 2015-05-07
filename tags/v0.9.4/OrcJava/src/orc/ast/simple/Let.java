package orc.ast.simple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.Expr;
import orc.ast.oil.arg.Arg;
import orc.ast.oil.arg.Site;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
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
	public Expression subst(Argument a, NamedVar x) {
		List<Argument> newargs = new LinkedList<Argument>();		
		for (Argument b : args) {
			newargs.add(b.subst(a, x));
		}
		return new Let(newargs);
	}
	
	public Set<Var> vars() {
		Set<Var> freeset = new HashSet<Var>();
		for(Argument a : args)	{
			a.addFree(freeset);
		}
		return freeset;
	}

	@Override
	public Expr convert(Env<Var> vars) throws UnboundVariableException {
		if (args.size() == 0) {
			// If there is no arg, use the signal value
			return new orc.ast.oil.arg.Constant(Value.signal());
		} else  if (args.size() == 1) {
			// If there is only one arg, use it directly as an expression
			return args.get(0).convert(vars);
		}
		
		List<Arg> newargs = new ArrayList<Arg>();
		for(Argument a : args) {
			Arg newa = a.convert(vars);
			newargs.add(newa);
		}
		
		// Otherwise, use the tuple creation site
		// TODO: Add an explicit zero-args case for unit 
		return new orc.ast.oil.Call(new orc.ast.oil.arg.Site(orc.ast.sites.Site.LET), newargs);
	}
}