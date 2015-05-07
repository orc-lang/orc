package orc.type.ground;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;
import orc.type.TupleType;
import orc.type.Type;

/**
 * 
 * The special type of the let site.
 * 
 * @author dkitchin
 *
 */
public class LetType extends Type {

	// TODO: Add subtype, join, and meet relationships to Let
	
	public Type call(List<Type> args) throws TypeException {
		return condense(args);
	}
	
	/* By default, use the class name as the type's string representation */
	public String toString() {
		return "let";
	}
	
	/**
	 * Classic 'let' functionality, at the type level. 
	 * Reduce a list of types into a single type as follows:
	 * 
	 * Zero arguments: return Top
	 * One argument: return that type
	 * Two or more arguments: return a tuple of the types
	 * 
	 */
	public static Type condense(List<Type> types) {
		if (types.size() == 0) {
			return Type.TOP;
		} else if (types.size() == 1) {
			return types.get(0);
		} else {
			return new TupleType(types);
		}
	}
	
}
