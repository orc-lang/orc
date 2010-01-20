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
	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.PolymorphicTypeAlias(type.unmarshal(), arity);
	}
}
