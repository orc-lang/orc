package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;

/**
 * A syntactic arrow type: lambda[X,...,X](T,...,T) :: T
 * 
 * @author dkitchin
 *
 */
public class ArrowType extends Type {

	public List<String> typeParams;
	public List<Type> argTypes;
	public Type resultType;
		
	public ArrowType(List<Type> argTypes, Type resultType, List<String> typeParams) {
		this.typeParams = typeParams;
		this.argTypes = argTypes;
		this.resultType = resultType;
	}

	public orc.type.Type convert(Env<String> env) throws TypeException {
		
		int arity = typeParams.size();
		
		Env<String> newenv = env;
		for (String X : typeParams) {
			newenv = newenv.extend(X);
		}
		
		List<orc.type.Type> newargs = new LinkedList<orc.type.Type>();
		for (Type T : argTypes) {
			newargs.add(T.convert(newenv));
		}
		orc.type.Type newresult = (resultType != null ? resultType.convert(newenv) : null);
		
		return new orc.type.ArrowType(newargs, newresult, arity);
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
