package orc.error;

import java.io.File;
import java.io.Serializable;

public class SourceLocation implements Serializable {
	public Integer line;
	public Integer column;
	public Integer endLine;
	public Integer endColumn;
	public File file;
	
	public static final SourceLocation UNKNOWN = new SourceLocation(null, 0, 0, 0, 0) {
		public String toString() {
			return "<unknown source location>";
		}
	};
	
	/** No-arg constructor so that this can be serialized to XML by JAXB */
	public SourceLocation() {}
	
	public SourceLocation(File filename,
			Integer line, Integer column,
			Integer endLine, Integer endColumn) {
		this.file = filename;
		this.line = line;
		this.column = column;
		this.endLine = endLine;
		this.endColumn = endColumn;
	}
	
	public String toString() {
		return file + ":" + line + ":" + column + "-" +
			(endLine != line ? endLine + ":" : "") + endColumn;
	}
}
