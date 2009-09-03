package orc.ast.oil.type;


/**
 * The syntactic type '_', a placeholder for an unknown type.
 * 
 * @author dkitchin
 *
 */
public class Blank extends Type {

	@Override
	public orc.type.Type transform() {
		return orc.type.Type.BLANK;
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
