package orc.error;

import java.io.Serializable;

public class SourceLocation implements Serializable {
	public Integer line;
	public Integer column;
	public Integer endLine;
	public Integer endColumn;
	public String filename;
	
	public static final SourceLocation UNKNOWN = new SourceLocation("<unknown>", 0, 0, 0, 0) {
		public String toString() {
			return "<unknown source location>";
		}
	};
	
	/** No-arg constructor so that this can be serialized to XML by JAXB */
	public SourceLocation() {}
	
	public SourceLocation(String filename,
			Integer line, Integer column,
			Integer endLine, Integer endColumn) {
		this.filename = filename;
		this.line = line;
		this.column = column;
		this.endLine = endLine;
		this.endColumn = endColumn;
	}
	
	public String toString() {
		return filename + ":" + line + ":" + column + "-" + endLine + ":" + endColumn;
	}
}
