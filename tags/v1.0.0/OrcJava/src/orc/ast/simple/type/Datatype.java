package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.tycon.DatatypeTycon;
import orc.type.tycon.Variance;

/**
 * 
 * A syntactic type encompassing all of the information associated with a datatype.
 * 
 * @author dkitchin
 *
 */
public class Datatype extends Type {

	public TypeVariable typename;
	public List<List<Type>> members;
	public List<TypeVariable> formals;
	
	public Datatype(TypeVariable typename, List<List<Type>> members, List<TypeVariable> formals) {
		this.typename = typename;
		this.members = members;
		this.formals = formals;
	}

	@Override
	public orc.ast.oil.type.Type convert(Env<TypeVariable> env) throws TypeException {
		
		// First, add the datatype name itself to the context
		Env<TypeVariable> newenv = env.extend(typename);
		
		// Then, add the type parameters
		newenv = newenv.extendAll(formals);
		
		List<List<orc.ast.oil.type.Type>> cs = new LinkedList<List<orc.ast.oil.type.Type>>();
		for (List<Type> args : members) {
			cs.add(Type.convertAll(args, newenv));
		}

		return new orc.ast.oil.type.Datatype(cs, formals.size(), typename.name);
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(Type T, FreeTypeVariable X) {
		
		List<List<Type>> cs = new LinkedList<List<Type>>();
		for (List<Type> args : members) {
			cs.add(Type.substAll(args,T,X));
		}
		
		return new Datatype(typename, cs, formals);
	}

}
