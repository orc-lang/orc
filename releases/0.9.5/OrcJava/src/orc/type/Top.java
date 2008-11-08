package orc.type;

/**
 * 
 * The Top type. Supertype of all other types.
 * 
 * All other types extend this type, so that we can use the Java
 * inheritance hierarchy to maintain a default subtyping relation.
 * 
 * The Top type can be ascribed to all values, and thus
 * necessarily carries no information.
 * 
 * @author dkitchin
 *
 */
public class Top extends Type {
	
	public boolean subtype(Type that) {
		return (that instanceof Top);
	}
	public String toString() { return "Top"; }
}
