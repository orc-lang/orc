package orc.ast.oil.type;

import orc.error.compiletime.typing.TypeException;
import orc.type.TypingContext;


/**
 * The type '_', a placeholder for an unknown type.
 * 
 * @author dkitchin
 *
 */
public class Blank extends Type {

	@Override
	public orc.type.Type transform(TypingContext ctx) throws TypeException {
		// FIXME: Support or deprecate blank type
		throw new TypeException("Blank type unsupported");
	}

	public String toString() { return "_"; }

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.Blank();
	}
}
