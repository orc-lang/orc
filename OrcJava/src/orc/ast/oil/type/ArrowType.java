package orc.ast.oil.type;

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.UncallableTypeException;

/**
 * A syntactic arrow type: lambda[X,...,X](T,...,T) :: T
 * 
 * @author dkitchin
 *
 */
public class ArrowType extends Type {

	public List<Type> argTypes;
	public Type resultType;
	public int typeArity;
		
	public ArrowType(List<Type> argTypes, Type resultType, int typeArity) {
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	public orc.type.Type transform() {
		
		List<orc.type.Type> newargs = new LinkedList<orc.type.Type>();
		for (Type T : argTypes) {
			newargs.add(T.transform());
		}
		orc.type.Type newresult = resultType.transform();
		
		return new orc.type.structured.ArrowType(newargs, newresult, typeArity);
	}
	
	
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.ArrowType(Type.marshalAll(argTypes),
											  resultType.marshal(),
											  typeArity);
	}
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
			
		s.append('(');
			
			s.append("lambda ");
			s.append('[');
			for (int i = 0; i < typeArity; i++) {
				if (i > 0) { s.append(", "); }
				s.append(".");
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
