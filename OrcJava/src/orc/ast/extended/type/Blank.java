package orc.ast.extended.type;


/**
 * The type '_', a placeholder for an unknown type.
 * 
 * @author dkitchin
 *
 */
public class Blank extends Type {

	@Override
	public orc.ast.simple.type.Type simplify()  {
		return orc.ast.simple.type.Type.BLANK;
	}

	public String toString() { return "_"; }
}
