package orc.error;

/**
 * 
 * Interface representing entities whose source location can be
 * assigned, not just observed. Typically used when an entity has
 * a source location associated with it, but that location cannot
 * be determined at the point where the entity is created, and
 * so is assigned later (cf TokenException).
 * 
 * @author dkitchin
 *
 */

public interface Locatable extends Located {

	public void setSourceLocation(SourceLocation location);
	
}
