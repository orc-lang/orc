package orc.ast.xml.type;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.UncallableTypeException;

/**
 * A syntactic arrow type: lambda[X,...,X](T,...,T) :: T
 * 
 * @author quark, dkitchin
 */
public class ArrowType extends Type {
	@XmlElementWrapper(required=true)
	@XmlElement(name="argType")
	public Type[] argTypes;
	@XmlElement(required=true)
	public Type resultType;
	@XmlAttribute(required=true)
	public int typeArity;
	
	public ArrowType() {}
		
	public ArrowType(Type[] argTypes, Type resultType, int typeArity) {
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.ArrowType(Type.unmarshalAll(argTypes), 
											  resultType.unmarshal(), 
											  typeArity);
	}
}
