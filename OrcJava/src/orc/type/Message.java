package orc.type;

import java.util.List;

import orc.ast.oil.arg.Field;
import orc.error.compiletime.typing.UncallableTypeException;

public class Message extends Type {

	public Field f;
	
	public Message(Field f) {
		this.f = f;
	}
	
	/* Message types are subtypes only of Top and of equal message types */
	public boolean subtype(Type that) {
		return that.isTop() || that.equal(this);
	}
	
	public boolean equal(Type that) {
		return that.getClass().equals(Message.class) && this.f.equals(((Message)that).f);
	}
			
	public String toString() {
		return "." + f.key;
	}
	
}
