package orc.ast.extended.type;


/**
 * The type 'Bot', subtype of all other types.
 * 
 * @author dkitchin
 *
 */
public class Bot extends Type {

	@Override
	public orc.ast.simple.type.Type simplify() {
		return orc.ast.simple.type.Type.BOT;
	}

	public String toString() { return "Bot"; }
}
