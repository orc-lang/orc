package orc.type.ground;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.Type;

/**
 * 
 * The bottom type. Subtype of every other type.
 * 
 * Bot is the type of expressions which will never publish.
 * 
 * @author dkitchin
 *
 */

public final class Bot extends Type {

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
	
	public String toString() { return "Bot"; }
}
