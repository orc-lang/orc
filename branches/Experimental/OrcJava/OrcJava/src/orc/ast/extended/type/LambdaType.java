package orc.ast.extended.type;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import orc.ast.extended.expression.Expression;
import orc.ast.extended.expression.Lambda;
import orc.ast.extended.pattern.Pattern;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.UncallableTypeException;

/**
 * A lambda type: lambda[X,...,X](T,...,T) :: T
 * 
 * @author dkitchin
 *
 */
public class LambdaType extends Type {

	public List<String> typeParams;
	public List<List<Type>> argTypes;
	public Type resultType;
		
	public LambdaType(List<List<Type>> argTypes, Type resultType, List<String> typeParams) {
		this.typeParams = typeParams;
		this.argTypes = argTypes;
		this.resultType = resultType;
	}

	@Override
	public orc.ast.simple.type.Type simplify() {
		
		LambdaType newLambda = this.uncurry();
		
		
		Map<FreeTypeVariable, orc.ast.simple.type.Type> bindings = new TreeMap<FreeTypeVariable, orc.ast.simple.type.Type>();
		
		List<TypeVariable> newTypeParams = new LinkedList<TypeVariable>();
		if (newLambda.typeParams != null) {
			for (String s : newLambda.typeParams) {
				TypeVariable Y = new TypeVariable();
				FreeTypeVariable X = new FreeTypeVariable(s);
				newTypeParams.add(Y);
				bindings.put(X, Y);
			}
		}
		
		List<orc.ast.simple.type.Type> newArgTypes = null;
		List<Type> lamArgTypes = newLambda.argTypes.get(0);
		if (lamArgTypes != null) {
			newArgTypes = new LinkedList<orc.ast.simple.type.Type>();
			for (Type t : lamArgTypes) {
				newArgTypes.add(t.simplify().subMap(bindings));
			}
		}
			
		orc.ast.simple.type.Type newResultType;		 
		if (newLambda.resultType == null) {
			newResultType = null;
		}
		else {
			newResultType = newLambda.resultType.simplify().subMap(bindings);
		}		
		
		return new orc.ast.simple.type.ArrowType(newArgTypes, newResultType, newTypeParams);
	}
	
	/**
	 * From a lambda type with multiple type argument groups:
	 * 
	 * lambda[X](T)(T)(T) :: T
	 * 
	 * create a chain of lambdas each with a single such group:
	 * 
	 * lambda[X](T) :: (lambda(T) :: (lambda(T) :: T))
	 * 
	 * If there is only one type group, just return an identical copy.
	 * 
	 */
	public LambdaType uncurry() {
		
		ListIterator<List<Type>> it = argTypes.listIterator(argTypes.size());
		
		List<List<Type>> singleton = new LinkedList<List<Type>>();
		singleton.add(it.previous());
		LambdaType result = new LambdaType(singleton, resultType, new LinkedList<String>());
		
		while (it.hasPrevious()) {
			singleton = new LinkedList<List<Type>>();
			singleton.add(it.previous());
			result = new LambdaType(singleton, result, new LinkedList<String>());
		}
		
		result.typeParams = typeParams;
		return result;
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
