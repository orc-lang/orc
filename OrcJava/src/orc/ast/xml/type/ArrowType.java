package orc.ast.xml.type;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import orc.Config;
import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;

/**
 * A syntactic arrow type: lambda[X,...,X](T,...,T) :: T
 * 
 * @author quark
 */
public class ArrowType extends Type {
	@XmlElementWrapper(required=true)
	@XmlElement(name="argType")
	public Type[] argTypes;
	@XmlElement(required=false)
	public Type resultType;
	@XmlAttribute(required=true)
	public int typeArity;
	
	public ArrowType() {}
		
	public ArrowType(Type[] argTypes, Type resultType, int typeArity) {
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	public orc.type.Type unmarshal(Config config) throws TypeException {
		LinkedList<orc.type.Type> newargs = new LinkedList<orc.type.Type>();
		for (Type t : argTypes) {
			newargs.add(t.unmarshal(config));
		}
		orc.type.Type newResultType = null;
		if (resultType != null) newResultType = resultType.unmarshal(config);
		
		return new orc.type.ArrowType(newargs, newResultType, typeArity);
	}
}
