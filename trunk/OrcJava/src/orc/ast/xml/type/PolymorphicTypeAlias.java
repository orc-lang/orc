package orc.ast.xml.type;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.tycon.Variance;

/**
 * A syntactic type representing an aliased type with type parameters.
 * @author quark
 */
public class PolymorphicTypeAlias extends Type {
	@XmlElement(required=true)
	public Type type;
	@XmlAttribute(required=true)
	public int arity;
	
	public PolymorphicTypeAlias() {}
	public PolymorphicTypeAlias(Type type, int arity) {
		this.type = type;
		this.arity = arity;
	}

	@Override
	public orc.type.Type unmarshal(Config config) throws TypeException {
		// Convert the syntactic type to a true type
		orc.type.Type newType = type.unmarshal(config);
		
		// Infer the variance of each type parameter
		Variance[] V = new Variance[arity];
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
