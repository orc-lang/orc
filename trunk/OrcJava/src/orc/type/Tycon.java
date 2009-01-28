package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;

/**
 * 
 * Root class for all type constructors.
 * 
 * Currently, the instance method for a tycon simply creates a TypeApplication 
 * type of that tycon to the instance parameters. The TypeApplication class handles
 * subtype, meet, and join for tycons.
 * 
 * It is assumed that all children of Tycon will implement variances() such that
 * its size() > 0, but there is no way to enforce this condition in Java.
 * 
 * @author dkitchin
 */
public abstract class Tycon extends Type {
	
	
	/* Create an instance of a polymorphic type at the given parameters.
	 * 
	 * Currently, this is done by checking the type arity to make sure
	 * the number of parameters is correct, and then creating a type
	 * application of that type to the parameters.
	 * 
	 * By default, types are assumed to have no parameters. Instantiation
	 * succeeds only on an empty list of parameters, and the result is
	 * just the type itself.
	 */
	public Type instance(List<Type> params) throws TypeException {
		
		if (variances().size() != params.size()) {
			throw new TypeArityException(variances().size(), params.size());
		}
		
		return new TypeApplication(this, params);
	}
	
	
	/* Convenience function for the common case: a single type parameter. */
	public Type instance(Type param) throws TypeException {
		List<Type> ts = new LinkedList<Type>();
		ts.add(param);
		return instance(ts);
	}
	
}
