//
// SourceLocation.java -- Java class SourceLocation
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (column == null ? 0 : column.hashCode());
		result = prime * result + (endColumn == null ? 0 : endColumn.hashCode());
		result = prime * result + (endLine == null ? 0 : endLine.hashCode());
		result = prime * result + (endOffset == null ? 0 : endOffset.hashCode());
		result = prime * result + (file == null ? 0 : file.hashCode());
		result = prime * result + (line == null ? 0 : line.hashCode());
		result = prime * result + (offset == null ? 0 : offset.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SourceLocation other = (SourceLocation) obj;
		if (column == null) {
			if (other.column != null) {
				return false;
			}
		} else if (!column.equals(other.column)) {
			return false;
		}
		if (endColumn == null) {
			if (other.endColumn != null) {
				return false;
			}
		} else if (!endColumn.equals(other.endColumn)) {
			return false;
		}
		if (endLine == null) {
			if (other.endLine != null) {
				return false;
			}
		} else if (!endLine.equals(other.endLine)) {
			return false;
		}
		if (endOffset == null) {
			if (other.endOffset != null) {
				return false;
			}
		} else if (!endOffset.equals(other.endOffset)) {
			return false;
		}
		if (file == null) {
			if (other.file != null) {
				return false;
			}
		} else if (!file.equals(other.file)) {
			return false;
		}
		if (line == null) {
			if (other.line != null) {
				return false;
			}
		} else if (!line.equals(other.line)) {
			return false;
		}
		if (offset == null) {
			if (other.offset != null) {
				return false;
			}
		} else if (!offset.equals(other.offset)) {
			return false;
		}
		return true;
	}

	public boolean isUnknown() {
		return false;
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
			if (file == null) {
				throw new IOException();
			}
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
