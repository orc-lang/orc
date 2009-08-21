package orc.ast.simple.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.extended.type.Type;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.runtime.nodes.Node;

public class Call extends Expression {

	public Argument callee;
	public List<Argument> args;
	public List<Type> typeArgs;
	
	public Call(Argument callee, List<Argument> args, List<Type> typeArgs)
	{
		this.callee = callee;
		this.args = args;
		this.typeArgs = typeArgs;
	}
	
	public Call(Argument callee, List<Argument> args)
	{
		this.callee = callee;
		this.args = args;
	}
	
	/* Binary call constructor */
	public Call(Argument callee, Argument arga, Argument argb)
	{
		this.callee = callee;
		this.args = new LinkedList<Argument>();
		this.args.add(arga);
		this.args.add(argb);
	}
	
	/* Unary call constructor */
	public Call(Argument callee, Argument arg)
	{
		this.callee = callee;
		this.args = new LinkedList<Argument>();
		this.args.add(arg);
	}
	
	/* Nullary call constructor */
	public Call(Argument callee)
	{
		this.callee = callee;
		this.args = new LinkedList<Argument>();
	}
	
	@Override
	public Expression subst(Argument a, NamedVariable x) {
		List<Argument> newargs = new LinkedList<Argument>();
		for (Argument b : args)	{
			newargs.add(b.subst(a, x));
		}
		return new Call(callee.subst(a, x), newargs, typeArgs);
	}

	public Set<Variable> vars() {
		Set<Variable> freeset = new HashSet<Variable>();
		callee.addFree(freeset);
		for(Argument a : args) {
			a.addFree(freeset);
		}
		return freeset;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<String> typevars) throws CompilationException {
		
		orc.ast.oil.expression.argument.Argument newcallee = callee.convert(vars);
		
		List<orc.ast.oil.expression.argument.Argument> newargs = new ArrayList<orc.ast.oil.expression.argument.Argument>();
		for(Argument arg : args) {
			newargs.add(arg.convert(vars));
		}
		
		List<orc.type.Type> newTypeArgs = null; 
		if (typeArgs != null) {
			newTypeArgs = new LinkedList<orc.type.Type>();
			for (Type t : typeArgs) {
				newTypeArgs.add(t.convert(typevars));
			}
		}
		
		return new orc.ast.oil.expression.Call(newcallee, newargs, newTypeArgs);
	}
}
