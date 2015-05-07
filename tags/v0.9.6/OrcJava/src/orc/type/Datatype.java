package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;

/**
 * Type constructor encompassing all type-level information associated with
 * a datatype. This tycon will be passed as a type parameter to the Datatype
 * site, which when typechecked will return a tuple of the constructor types.
 * 
 * This may seem slightly obtuse, but it makes the earlier translation steps
 * easier while still guaranteeing that all type information can be discarded
 * from an OIL file without affecting its runtime semantics. If datatype
 * declarations simultaneously had static and dynamic semantics, that would
 * not be true. So, this is the isolated static component of the datatype
 * declaration.
 * 
 * @author dkitchin
 *
 */
public class Datatype extends Tycon {

	String name;
	List<Variance> vs;
	List<List<Type>> cs;
	
	public Datatype(String name, List<Variance> vs, List<List<Type>> cs) {
		this.name = name;
		this.vs = vs;
		this.cs = cs;
	}
	
	public List<Variance> variances() { return vs; }
	
	
	public List<List<Type>> getConstructors() {
		
		/* Unfold this datatype once and return its list
		 * of constructors.
		 * 
		 * Note that each constructor will also have type
		 * parameters of the arity of this datatype.
		 */
		
		Env<Type> recctx = (new Env<Type>()).extend(this);
		
		// Protect the bound type variables of the constructors
		for(int i = 0; i < vs.size(); i++) {
			recctx = recctx.extend(null);
		}
		
		List<List<Type>> newcs = new LinkedList<List<Type>>();
		for (List<Type> c : cs) {
			newcs.add(Type.substAll(c, recctx));
		}
		
		return newcs;
	}
	
	
	public Type subst(Env<Type> ctx) {
		
		// A datatype has a recursive type binder,
		// and a binder for each type parameter.
		for(int i = 0; i < vs.size() + 1; i++) {
			ctx = ctx.extend(null);
		}
		
		/* Perform substitution on all constructors */
		List<List<Type>> newcs = new LinkedList<List<Type>>();
		for (List<Type> c : cs) {
			newcs.add(Type.substAll(c, ctx));
		}
		
		return new Datatype(name, vs, newcs);
	}
	
	public String toString() {
		return name;
	}
	
}
