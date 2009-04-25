package orc.ast.oil.xml.type;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import orc.Config;
import orc.env.Env;
import orc.env.SearchFailureException;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnboundTypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.TypeVariable;

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
	public orc.type.Type unmarshal(Config config) throws TypeException {
		 
		List<orc.type.Type> ts = new LinkedList<orc.type.Type>();
		for (Type t : params) {
			ts.add(t.unmarshal(config));
		}
				
		return new orc.type.TypeApplication(constructor.unmarshal(config), ts);
	}
}
