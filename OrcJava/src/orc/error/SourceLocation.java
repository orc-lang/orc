package orc.error;

import java.io.Serializable;

public class SourceLocation implements Serializable {
	public Integer line;
	public Integer column;
	public String filename;
	
	public static final SourceLocation UNKNOWN = new SourceLocation("<unknown>", 0, 0) {
		public String toString() {
			return "<unknown source location>";
		}
	};
	
	/** No-arg constructor so that this can be serialized to XML by JAXB */
	public SourceLocation() {}
	
	public SourceLocation(String filename, Integer line, Integer column) {
		this.filename = filename;
		this.line = line;
		this.column = column;
	}
	
	public String toString() {
		return filename + ":" + line + ":" + column;
	}
}
