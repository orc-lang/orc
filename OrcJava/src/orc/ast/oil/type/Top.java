package orc.ast.oil.type;


/**
 * The syntactic type 'Top', supertype of all other types.
 * 
 * @author dkitchin
 *
 */
public class Top extends Type {

	@Override
	public orc.type.Type transform() {
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
