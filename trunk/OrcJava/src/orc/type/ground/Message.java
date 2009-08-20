package orc.type.ground;

import java.util.List;

import orc.ast.oil.expression.argument.Field;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.Type;

public class Message extends Type {

	public Field f;
	
	public Message(Field f) {
		this.f = f;
	}
	
	/* Message types are subtypes only of Top and of equal message types */
	public boolean subtype(Type that) throws TypeException {
		return that.isTop() || that.equal(this);
	}
	
	public boolean equal(Type that) {
		return that.getClass().equals(Message.class) && this.f.equals(((Message)that).f);
	}
			
	public String toString() {
		return "." + f.key;
	}
	
}
