package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;

/**
 * An arrow (lambda) type: lambda[X,...,X](T,...,T) :: T
 * 
 * @author dkitchin
 *
 */
public class ArrowType extends Type {

	public List<TypeVariable> typeParams;
	public List<Type> argTypes;
	public Type resultType;
		
	public ArrowType(List<Type> argTypes, Type resultType, List<TypeVariable> typeParams) {
		this.typeParams = typeParams;
		this.argTypes = argTypes;
		this.resultType = resultType;
	}

	public orc.ast.oil.type.Type convert(Env<TypeVariable> env) throws TypeException {
		Env<TypeVariable> newenv = env.extendAll(typeParams);
		return new orc.ast.oil.type.ArrowType(Type.convertAll(argTypes, newenv), resultType.convert(newenv), typeParams.size());
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(Type T, FreeTypeVariable X) {
		return new ArrowType(Type.substAll(argTypes, T, X), resultType.subst(T,X), typeParams);
	}	
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
			
		s.append('(');
			
			s.append("lambda ");
			s.append('[');
			for (int i = 0; i < typeParams.size(); i++) {
				if (i > 0) { s.append(", "); }
				s.append(typeParams.get(i));
			}
			s.append(']');
			s.append('(');
			for (int i = 0; i < argTypes.size(); i++) {
				if (i > 0) { s.append(", "); }
				s.append(argTypes.get(i));
			}
			s.append(')');
			s.append(" :: ");
			s.append(resultType);
			
		s.append(')');
		
		return s.toString();
	}

	
}
