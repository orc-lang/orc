package orc.type;

import java.util.List;

import orc.error.compiletime.typing.UncallableTypeException;

/**
 * 
 * The bottom type. Subtype of every other type.
 * 
 * Bot is the type of expressions which will never publish.
 * 
 * @author dkitchin
 *
 */

public class Bot extends Type {

	public boolean subtype(Type that) {
		return true;
	}
	
	public boolean equal(Type that) {
		return that instanceof Bot;
	}
	
	public Type join(Type that) {
		return that;
	}
	
	public Type meet(Type that) {
		return this;
	}
	
	public Type call(List<Type> args) {
		return this;
	}
	
	public String toString() { return "Bot"; }
}
