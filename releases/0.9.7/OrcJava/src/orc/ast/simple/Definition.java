package orc.ast.simple;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import orc.ast.oil.Def;
import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.ast.simple.type.ArrowType;
import orc.ast.simple.type.Type;
import orc.env.Env;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;
import orc.runtime.nodes.Node;

/**
 * 
 * A unit of syntax that encapsulates an expression definition. 
 * 
 * Groups of mutually recursive definitions are scoped in the simplified abstract syntax tree by Defs.
 * 
 * @author dkitchin
 *
 */

public class Definition {

	public Var name;
	public List<Var> formals;
	public Expression body;
	protected List<String> typeParams; /* Never null; if there are no type params, this will be an empty list */
	protected List<Type> argTypes; /* May be null, but only for defs derived from lambda, and only in a checking context */
	protected Type resultType; /* May be null to request inference */
	protected SourceLocation location;
	
	/**
	 * Note that the constructor takes a bound Var as a name parameter. This is because the
	 * binding of expression names occurs at the level of mutually recursive groups, not at
	 * the level of the individual definitions.
	 * @param location 
	 */
	public Definition(Var name, List<Var> formals, Expression body,
			List<String> typeParams, List<Type> argTypes, Type resultType, SourceLocation location) {
		this.name = name;
		this.formals = formals;
		this.body = body;
		this.typeParams = (typeParams != null ? typeParams : new LinkedList<String>());
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.location = location;
	}

	

	public Definition subst(Argument a, NamedVar x) {
		return new Definition(name, formals, body.subst(a, x), typeParams, argTypes, resultType, location);
	}
	
	public Definition suball(Map<NamedVar, ? extends Argument> m) {
		return new Definition(name, formals, body.suball(m), typeParams, argTypes, resultType, location);
	}
	
	// Does not validly compute the set of free vars if this definition is in a mutually recursive group.
	// That determination must be accounted for elsewhere.
	public Set<Var> vars()
	{
		Set<Var> freeset = body.vars();
		freeset.remove(name);
		freeset.removeAll(formals);
		return freeset;
	}

	public Def convert(Env<Var> vars, Env<String> typevars) throws CompilationException {
	
		Env<Var> newvars = vars.clone();
		newvars.addAll(formals);
		
		Env<String> newtypevars = typevars.clone();
		newtypevars.addAll(typeParams);
		
		List<orc.type.Type> newArgTypes = null;
		if (argTypes != null) {
			newArgTypes = new LinkedList<orc.type.Type>();
			for (Type t : argTypes) {
				newArgTypes.add(t.convert(newtypevars));
			}
		}
		
		orc.type.Type newResultType = (resultType != null ? resultType.convert(newtypevars) : null);
		
		return new orc.ast.oil.Def(formals.size(), 
								   body.convert(newvars, newtypevars),
								   typeParams.size(),
								   newArgTypes,
								   newResultType,
								   location);
									
	}
	
}
