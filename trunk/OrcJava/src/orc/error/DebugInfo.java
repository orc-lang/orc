package orc.error;

import antlr.Token;


/**
 * 
 * Interface for objects encapsulating information needed to display reasonable error messages.
 * 
 * @author dkitchin
 *
 */
public class DebugInfo {

	public static final DebugInfo DEFAULT = new DebugInfo();
	
	String loc;
	
	public DebugInfo() {
		loc = "unknown";
	}
	
	public DebugInfo(Token t) {
		loc = "Line " + t.getLine() + ", column " + t.getColumn() + ", in file " + t.getFilename();
		
	}
	
	public String errorLocation() { return loc; }
}