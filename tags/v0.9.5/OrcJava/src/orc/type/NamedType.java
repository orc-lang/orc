package orc.type;

/**
 * Placeholder for named types which have not yet been resolved to
 * bound type variables.
 * 
 * @author dkitchin
 */
public class NamedType extends Type {
	
	String name;
	
	public NamedType(String name) {
		this.name = name;
	}
	
}
