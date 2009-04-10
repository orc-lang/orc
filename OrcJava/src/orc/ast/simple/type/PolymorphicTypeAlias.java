package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.tycon.Variance;

/**
 * 
 * A syntactic type representing an aliased type with type parameters.
 * 
 * @author dkitchin
 *
 */
public class PolymorphicTypeAlias extends Type {

	public Type type;
	public List<String> formals;
	
	public PolymorphicTypeAlias(Type type, List<String> formals) {
		this.type = type;
		this.formals = formals;
	}

	@Override
	public orc.type.Type convert(Env<String> env) throws TypeException {
		
		// Add the type parameters to the context
		for (String formal : formals) {
			env = env.extend(formal);
		}
		
		// Convert the syntactic type to a true type
		orc.type.Type newType = type.convert(env);
		
		// Infer the variance of each type parameter
		Variance[] V = new Variance[formals.size()];
		for (int i = 0; i < V.length; i++) {
			V[i] = newType.findVariance(i);
		}
		
		List<Variance> vs = new LinkedList<Variance>();
		for(Variance v : V) {
			vs.add(0,v);
		}
		
		return new orc.type.tycon.PolymorphicAliasedType(newType, vs);
	}

}
