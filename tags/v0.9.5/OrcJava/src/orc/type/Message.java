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
		return Type.TOP.equals(that) || this.equals(that);
	}
	
	/* Message types are equal only if they carry the same message */
	public boolean equal(Type that) {
		
		return that.getClass().equals(Message.class) && this.f.equals(((Message)that).f);
	}
	
	public Type join(Type that) {
		return (this.equals(that) ? this : Type.TOP);
	}
	
	public Type meet(Type that) {
		return (this.equals(that) ? this : Type.BOT);
	}
		
	public String toString() {
		return "." + f.key;
	}
	
}
