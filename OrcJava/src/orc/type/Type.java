package orc.type;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;

/**
 * 
 * Abstract superclass of all types for the Orc typechecker.
 * 
 * @author dkitchin
 *
 */

public abstract class Type {

	/* Create singleton representatives for some common types */
	public static Type TOP = new Top();
	public static Type BOT = new Bot();
	public static final Type NUMBER = new NumberType();
	public static final Type STRING = new StringType();
	public static final Type BOOLEAN = new BooleanType();
	public static final Type INTEGER = new IntegerType();
	public static final Type LET = new LetType();

	
	/* We use the Java inheritance hierarchy as a default */
	public boolean subtype(Type that) {
		
		if (that instanceof Top) {
			return true;
		}
		else {
			return that.getClass().isAssignableFrom(this.getClass());
		}
	}

	public boolean supertype(Type that) {
		return that.subtype(this);
	}
	
	/* By default, equality is based on mutual subtyping.
	 * TODO: This may not be correct in the presence of bounded quantification.
	 */
	public boolean equal(Type that) {
		return this.subtype(that) && that.subtype(this);
	}
	
	/* Find the join (least upper bound) in the subtype lattice
	 * of this type and another type.
	 */
	public Type join(Type that) {		
		if (this.subtype(that)) {
			return that;
		}
		else if (that.subtype(this)) {
			return this;
		}
		else {
			return TOP;
		}
	}
	
	/* Find the meet (greatest lower bound) in the subtype lattice
	 * of this type and another type.
	 */
	public Type meet(Type that) {
		if (this.subtype(that)) {
			return that;
		}
		else if (that.subtype(this)) {
			return this;
		}
		else {
			return BOT;
		}
		
	}
	
	/* By default, a type is not callable */
	public Type call(List<Type> args) throws TypeException {
		throw new UncallableTypeException(this);
	}
	
	/* By default, use the class name as the type's string representation */
	public String toString() {
		return this.getClass().toString();
	}
	
}
