package orc.error;

/**
 * 
 * Interface implemented by any construct (syntax tree node, exception, etc)
 * which is associated with a particular location in the source program. 
 * 
 * @author dkitchin
 *
 */
public interface Debuggable {
	
	public SourceLocation getSourceLocation();
		
}
