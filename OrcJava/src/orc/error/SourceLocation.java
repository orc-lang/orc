package orc.error;

public class SourceLocation {
	
	public Integer line;
	public Integer column;
	public String filename;
	
	public static final SourceLocation UNKNOWN = new UnknownLocation();
	
	public SourceLocation(Integer line, Integer column, String filename) {
		this.line = line;
		this.column = column;
		this.filename = filename;
	}
	
	public String toString() {
		return "line " + line + ", column " + column + ", source file " + filename;
	}
		
}

class UnknownLocation extends SourceLocation {

	public UnknownLocation() {
		super(-1, -1, "unknown");
		
	}
	
	public String toString() {
		return "unknown source location";
	}
	
}

