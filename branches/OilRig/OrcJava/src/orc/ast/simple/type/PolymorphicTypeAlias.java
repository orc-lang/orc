package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.tycon.Variance;

/**
 * 
 * A representation of an aliased type with type parameters.
 * 
 * @author dkitchin
 *
 */
public class PolymorphicTypeAlias extends Type {

	public Type type;
	public List<TypeVariable> formals;
	
	public PolymorphicTypeAlias(Type type, List<TypeVariable> formals) {
		this.type = type;
		this.formals = formals;
	}

	@Override
	public orc.ast.oil.type.Type convert(Env<TypeVariable> env) throws TypeException {
		return new orc.ast.oil.type.PolymorphicTypeAlias(type.convert(env.extendAll(formals)), formals.size());
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(Type T, FreeTypeVariable X) {
		return this;
	}

}
