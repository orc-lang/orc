package orc.ast.simple.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.extended.type.Type;
import orc.ast.oil.expression.Expr;
import orc.ast.oil.expression.argument.Arg;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVar;
import orc.ast.simple.argument.Var;
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
	public Expression subst(Argument a, NamedVar x) {
		List<Argument> newargs = new LinkedList<Argument>();
		for (Argument b : args)	{
			newargs.add(b.subst(a, x));
		}
		return new Call(callee.subst(a, x), newargs, typeArgs);
	}

	public Set<Var> vars() {
		Set<Var> freeset = new HashSet<Var>();
		callee.addFree(freeset);
		for(Argument a : args) {
			a.addFree(freeset);
		}
		return freeset;
	}

	@Override
	public Expr convert(Env<Var> vars, Env<String> typevars) throws CompilationException {
		
		Arg newcallee = callee.convert(vars);
		
		List<Arg> newargs = new ArrayList<Arg>();
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
