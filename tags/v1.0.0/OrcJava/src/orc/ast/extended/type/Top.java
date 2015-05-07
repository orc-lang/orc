package orc.ast.extended.type;


/**
 * The type 'Top', supertype of all other types.
 * 
 * @author dkitchin
 *
 */
public class Top extends Type {

	@Override
	public orc.ast.simple.type.Type simplify() {
		return orc.ast.simple.type.Type.TOP;
	}

	public String toString() { return "Top"; }
}
