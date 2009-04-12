package orc.type.tycon;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

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
public class DatatypeTycon extends Tycon {

	String name;
	List<Variance> vs;
	List<List<Type>> cs;
	Object id; // A unique identifier used for equality
	
	
	public DatatypeTycon(String name, List<Variance> vs, List<List<Type>> cs, Object id) {
		this.name = name;
		this.vs = vs;
		this.cs = cs;
		this.id = id;
	}
	
	
	public List<Variance> variances() throws TypeException { 
		return vs;
	}
	
	
	public List<List<Type>> getConstructors() throws TypeException {
		
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
	
	
	/*
	 * As an invariant, datatype tycons have no free variables,
	 * so substitution does nothing.
	 * 
	 * This preserves object equality for tycons, since otherwise
	 * we would need to create a new datatype tycon.
	 */
	public Type subst(Env<Type> ctx) throws TypeException {
		return new DatatypeTycon(name, vs, cs, id);
	}
	
	public String toString() {
		return name;
	}
	
	public boolean equals(Object that) {
		return that.getClass().equals(DatatypeTycon.class) 
			&& ((DatatypeTycon)that).id == id;
	}

}
