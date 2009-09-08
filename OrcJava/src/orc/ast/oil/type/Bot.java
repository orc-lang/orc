package orc.ast.oil.type;

import orc.error.compiletime.typing.TypeException;
import orc.type.TypingContext;


/**
 * The type 'Bot', subtype of all other types.
 * 
 * @author dkitchin
 *
 */
public class Bot extends Type {

	@Override
	public orc.type.Type transform(TypingContext ctx) throws TypeException {
		return orc.type.Type.BOT;
	}

	public String toString() { return "Bot"; }

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.Bot();
	}
}
