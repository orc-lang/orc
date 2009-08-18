package orc.error;

import java.io.File;
import java.io.Serializable;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 
 * A source location, with file, offset, line, and column information.
 * 
 * @author quark, dkitchin
 *
 */
public class SourceLocation implements Serializable {
	public Integer offset;
	public Integer line;
	public Integer column;
	public Integer endOffset;
	public Integer endLine;
	public Integer endColumn;
	public File file;
	private Boolean fileFound = false;
	
	public static final SourceLocation UNKNOWN = new SourceLocation(null, -1, 0, 0, -1, 0, 0) {
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
			Integer offset, Integer line, Integer column,
			Integer endOffset, Integer endLine, Integer endColumn) {
		this.file = filename;
		this.offset = offset;
		this.line = line;
		this.column = column;
		this.endOffset = endOffset;
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
		
		Integer newBeginOffset;
		Integer newBeginLine;
		Integer newBeginColumn;
		Integer newEndOffset;
		Integer newEndLine;
		Integer newEndColumn;
		
		// Find minimum begin offset...
		newBeginOffset = this.offset < that.offset ? this.offset : that.offset;
		// ...and maximum end offset
		newEndOffset = this.endOffset > that.endOffset ? this.endOffset : that.endOffset;
	
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
	
		return new SourceLocation(this.file, newBeginOffset, newBeginLine, newBeginColumn, newEndOffset, newEndLine, newEndColumn);
	}
	
	public String toString() {
		FileReader reader;
		String locStr = file + ":" + line + ":" + column + "-" +
		(endLine != line ? endLine + ":" : "") + endColumn;
		try{
			reader = new FileReader(file);
		} catch (FileNotFoundException e){
			return locStr;
		}
		BufferedReader input = new BufferedReader(reader);
		String str = "";
		try{
			for(int i = 0; i < line; i++)
				str = input.readLine();
		} catch (IOException e){
			return locStr;
		}
		
		fileFound = true;
		return locStr + ": "+ str;
	}
	
	/*
	 * returns a string which is the caret to denote the start of the error on the console.
	 * might return null to denote the file wasn't found.
	 */
	public String getCaret(){
		
		if(!fileFound)
			return null;
		
		String locStr = file + ":" + line + ":" + column + "-" +
		(endLine != line ? endLine + ":" : "") + endColumn;
		int len = locStr.length();
		len += column;
		String caretString = "";
		for(int i = 0; i < len; i++){
			caretString += " ";
		}
		caretString += " ^";
		return caretString;
	}
}
