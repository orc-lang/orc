package orc.ast.oil.type;

import orc.error.compiletime.typing.TypeException;
import orc.type.TypingContext;


/**
 * The syntactic type 'Top', supertype of all other types.
 * 
 * @author dkitchin
 *
 */
public class Top extends Type {

	@Override
	public orc.type.Type transform(TypingContext ctx) throws TypeException {
		return orc.type.Type.TOP;
	}

	public String toString() { return "Top"; }

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.Top();
	}
}
