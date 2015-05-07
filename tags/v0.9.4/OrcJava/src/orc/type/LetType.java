package orc.type;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;

/**
 * 
 * The special type of the let site.
 * 
 * @author dkitchin
 *
 */
public class LetType extends Type {

	// TODO: Add subtype, join, and meet relationships to arrow types.
	
	
	/* By default, a type is not callable */
	public Type call(List<Type> args) throws TypeException {
		
		if (args.size() == 0) {
			return Type.TOP;
		}
		else if (args.size() == 1) {
			return args.get(0);
		}
		else {
			return new TupleType(args);
		}
		
	}
	
	/* By default, use the class name as the type's string representation */
	public String toString() {
		return "let";
	}
}
