package orc.ast.oil.type;

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.TypingContext;
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
	public int typeArity;
	
	public PolymorphicTypeAlias(Type type, int typeArity) {
		this.type = type;
		this.typeArity = typeArity;
	}

	@Override
	public orc.type.Type transform(TypingContext ctx) throws TypeException {
		
		// Convert the syntactic type to a true type
		orc.type.Type newType = type.transform(ctx);
		
		// Infer the variance of each type parameter
		Variance[] V = new Variance[typeArity];
		for (int i = 0; i < V.length; i++) {
			V[i] = newType.findVariance(i);
		}
		
		List<Variance> vs = new LinkedList<Variance>();
		for(Variance v : V) {
			vs.add(0,v);
		}
		
		return new orc.type.tycon.PolymorphicAliasedType(newType, vs);
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.PolymorphicTypeAlias(type.marshal(), typeArity);
	}

}
