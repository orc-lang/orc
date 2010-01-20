package orc.ast.simple.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
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
	public Expression subst(Argument a, FreeVariable x) {
		return new Call(callee.subst(a, x), Argument.substAll(args, a, x), typeArgs);
	}
	
	public Expression subst(Type T, FreeTypeVariable X) {
		return new Call(callee, args, Type.substAll(typeArgs, T, X));
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
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars) throws CompilationException {
		return new orc.ast.oil.expression.Call(callee.convert(vars), 
											   Argument.convertAll(args, vars), 
											   Type.convertAll(typeArgs, typevars));
	}
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		

		s.append(callee);
		if (typeArgs != null) {
			s.append('[');
			for (int i = 0; i < typeArgs.size(); i++) {
				if (i > 0) { s.append(", "); }
				s.append(typeArgs.get(i));
			}
			s.append(']');
		}
		s.append('(');
		for (int i = 0; i < args.size(); i++) {
			if (i > 0) { s.append(", "); }
			s.append(args.get(i));
		}
		s.append(')');
		
		return s.toString();
	}
}
