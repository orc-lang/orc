package orc.type;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.type.ground.Bot;

/**
 * FIXME: implement this as a DotType or similar
 * @author quark
 */
public class ClassnameType extends Type {
	public String classname;
	
	public ClassnameType(String classname) {
		this.classname = classname;
	}
	
	public boolean subtype(Type that) throws TypeException {
		return true;
	}
	
	public boolean equal(Type that) {
		return that.isBot();
	}
	
	public Type join(Type that) throws TypeException {
		return that;
	}
	
	public Type meet(Type that) {
		return this;
	}
	
	public Type call(List<Type> args) {
		return this;
	}
	
	public boolean isBot() {
		return true;
	}

	@Override
	public orc.ast.xml.type.Type marshal() throws UnrepresentableTypeException {
		return new orc.ast.xml.type.ClassnameType(classname);
	}
}
