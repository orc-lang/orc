package orc.error;

import java.io.File;
import java.io.Serializable;

/**
 * 
 * A source location, with file, line, and column information.
 * 
 * @author quark, dkitchin
 *
 */
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
		public boolean isUnknown() {
			return true;
		}
	};
	
	public boolean isUnknown() {
		return false;
	}
	
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
	
	/**
	 * Create a new source location that overlaps both this location
	 * and the argument location. Both locations must be in the same file;
	 * returns UNKNOWN if the filenames are not equal. 
	 */
	public SourceLocation overlap(SourceLocation that) {
		
		if (!this.file.equals(that.file)) {
			return UNKNOWN;
		}
		
		Integer newBeginLine;
		Integer newBeginColumn;
		Integer newEndLine;
		Integer newEndColumn;
	
		// Find the lex order minimum
		if (this.line < that.line) {
			newBeginLine = this.line;
			newBeginColumn = this.column;
		}
		else if (that.line < this.line) {
			newBeginLine = that.line;
			newBeginColumn = that.column;
		}
		else {
			newBeginLine = this.line;
			newBeginColumn = (this.column < that.column ? this.column : that.column);
		}
		
		// Find the lex order maximum
		if (this.line > that.line) {
			newEndLine = this.line;
			newEndColumn = this.column;
		}
		else if (that.line > this.line) {
			newEndLine = that.line;
			newEndColumn = that.column;
		}
		else {
			newEndLine = this.line;
			newEndColumn = (this.column > that.column ? this.column : that.column);
		}
	
		return new SourceLocation(this.file, newBeginLine, newBeginColumn, newEndLine, newEndColumn);
	}
	
	public String toString() {
		return file + ":" + line + ":" + column + "-" +
			(endLine != line ? endLine + ":" : "") + endColumn;
	}
}
