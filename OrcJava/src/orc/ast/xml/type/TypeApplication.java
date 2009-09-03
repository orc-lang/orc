package orc.ast.xml.type;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import orc.env.Env;
import orc.env.SearchFailureException;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.UnboundTypeException;
import orc.error.compiletime.typing.UncallableTypeException;

/**
 * A type instantiation with explicit type parameters: T[T,..,T]
 * 
 * @author dkitchin
 *
 */
public class TypeApplication extends Type {
	@XmlElement(required=true)
	public Type constructor;
	@XmlElement(name="param", required=true)
	public Type[] params;
	
	public TypeApplication() {}
	public TypeApplication(Type ty, Type[] params) {
		this.constructor = ty;
		this.params = params;
	}
	
	@Override
	public orc.ast.oil.type.Type unmarshal() {				
		return new orc.ast.oil.type.TypeApplication(constructor.unmarshal(), 
													Type.unmarshalAll(params));
	}
}
