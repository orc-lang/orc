//
// SourceLocation.java -- Java class SourceLocation
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;

/**
 * A source location, with file, offset, line, and column information.
 * 
 * @author quark, dkitchin
 */
public class SourceLocation implements Serializable {
	public Integer offset;
	public Integer line;
	public Integer column;
	public Integer endOffset;
	public Integer endLine;
	public Integer endColumn;
	public File file;
	public static final SourceLocation UNKNOWN = new SourceLocation(null, -1, 0, 0, -2, 0, 0) {
		@Override
		public String toString() {
			return "<unknown source location>";
		}

		@Override
		public boolean isUnknown() {
			return true;
		}

		@Override
		public String getCaret() {
			return null;
		}
	};

	public boolean isUnknown() {
		return false;
	}

	/** No-arg constructor so that this can be serialized to XML by JAXB */
	public SourceLocation() {
	}

	public SourceLocation(final File filename, final Integer offset, final Integer line, final Integer column, final Integer endOffset, final Integer endLine, final Integer endColumn) {
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
	public SourceLocation overlap(final SourceLocation that) {
		if (this.equals(UNKNOWN)) {
			return that;
		}
		if (that.equals(UNKNOWN)) {
			return this;
		}

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
		} else if (that.line < this.line) {
			newBeginLine = that.line;
			newBeginColumn = that.column;
		} else {
			newBeginLine = this.line;
			newBeginColumn = this.column < that.column ? this.column : that.column;
		}

		// Find the lex order maximum
		if (this.line > that.line) {
			newEndLine = this.line;
			newEndColumn = this.column;
		} else if (that.line > this.line) {
			newEndLine = that.line;
			newEndColumn = that.column;
		} else {
			newEndLine = this.line;
			newEndColumn = this.column > that.column ? this.column : that.column;
		}

		return new SourceLocation(this.file, newBeginOffset, newBeginLine, newBeginColumn, newEndOffset, newEndLine, newEndColumn);
	}

	@Override
	public String toString() {
		return file + ":" + line + ":" + column + "-" + (endLine != line ? endLine + ":" : "") + endColumn;
	}

	/*
	 * returns a string which is the caret to denote the start of the error on the console.
	 * might return null to denote the file wasn't found.
	 */
	public String getCaret() {
		try {
			if (file == null) { throw new IOException(); }
			final BufferedReader input = new BufferedReader(new FileReader(file));
			String str = "";
			for (int i = 0; i < line; i++) {
				str = input.readLine();
			}
			String caretString = str + "\n";
			final int len = 0 + column;
			for (int i = 0; i < len - 1; i++) {
				caretString += " ";
			}
			caretString += "^";
			return caretString;
		} catch (final IOException e) {
			return null;
		}
	}
}
