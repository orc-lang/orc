package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.env.SearchFailureException;
import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnboundTypeException;
import orc.error.compiletime.typing.UncallableTypeException;

/**
 * A type instantiation with explicit type parameters: T[T,..,T]
 * 
 * @author dkitchin
 *
 */
public class TypeApplication extends Type {

	public Type typeOperator;
	public List<Type> params;
	
	public TypeApplication(Type typeOperator, List<Type> params) {
		this.typeOperator = typeOperator;
		this.params = params;
	}
	
	@Override
	public orc.ast.oil.type.Type convert(Env<orc.ast.simple.type.TypeVariable> env) throws TypeException { 				
		return new orc.ast.oil.type.TypeApplication(typeOperator.convert(env), Type.convertAll(params, env));
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(Type T, FreeTypeVariable X) {
		return new TypeApplication(typeOperator.subst(T,X), Type.substAll(params, T, X));
	}
	
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append(typeOperator);
		s.append('[');
		for (int i = 0; i < params.size(); i++) {
			if (i > 0) { s.append(", "); }
			s.append(params.get(i));
		}
		s.append(']');
		
		return s.toString();
	}	
	
}
