//
// OrcLexer.java -- Java class OrcLexer
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 13, 2009.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.parse;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import scala.util.parsing.input.NoPosition$;
import scala.util.parsing.input.OffsetPosition;
import scala.util.parsing.input.Position;
import scala.util.parsing.input.Positional;

/**
 * Provides lexical scanning for the currently parsed string in the given ParseController.
 *
 * @see OrcParseController
 * @author jthywiss
 */
public class OrcLexer implements Iterable<OrcLexer.OrcToken> {
	/**
	 * A categorization of Orc lexical tokens into token types
	 *
	 * @author jthywiss
	 */
	public enum TokenType {
		/** Character that is not any valid Orc lexeme */
		UNKNOWN,
		/** Numeric literal (interger or floating point) */
		NUMBER_LITERAL,
		/** String literal -- quoted character sequence */
		STRING_LITERAL,
		/** "true" or "false" */
		BOOLEAN_LITERAL,
		/** "null" */
		NULL_LITERAL,
		/** Lexeme that follows identifier rules, but it might be a keyword.
		 * This can be refined into IDENTIFIER or KEYWORD token types. */
		IDENTIFIER_OR_KEYWORD, // When we don't know yet
		/** An identifier, not a keyword */
		IDENTIFIER, // We've checked against keyword list
		/** An Orc keyword */
		KEYWORD, // We've checked against keyword list
		/** An operator that isn't a combinator. */
		OPERATOR,
		/** An Orc combinator. */
		COMBINATOR,
		/** Parenthesis, square brackets, or curly braces */
		BRACKET,
		/** Lexemes like ",", ";", etc. */
		SEPERATOR,
		/** Comment to end of line (a.k.a. "single line comment") */
		COMMENT_ENDLINE,
		/** Comment bracketed by "{-" "-}".
		 * These brackets are included in the token.
		 * Despite the name, the comment could be on one line. */
		COMMENT_MULTILINE,
		/** Spaces, tabs, end of line chars, etc. */
		WHITESPACE,
	}

	/**
	 * A single token in a file.
	 *
	 * @author jthywiss
	 */
	public static class OrcToken implements Positional {
		/** The type of this token */
		public TokenType type;
		/** The characters comprising this token. */
		public String text;
		/** The location of this token in its file. */
		public Position pos;

		protected OrcToken(final TokenType type, final String text, final Position pos) {
			this.type = type;
			this.text = text;
			this.pos = pos;
		}

		/* (non-Javadoc)
		 * @see orc.error.Located#getSourceLocation()
		 */
		@Override
		public Position pos() {
			return pos;
		}

		/* (non-Javadoc)
		 * @see scala.util.parsing.input.Positional#pos_$eq(scala.util.parsing.input.Position)
		 */
		@Override
		public void pos_$eq(final Position newPos) {
			pos = newPos;
		}

		/* (non-Javadoc)
		 * @see scala.util.parsing.input.Positional#setPos(scala.util.parsing.input.Position)
		 */
		@Override
		public Positional setPos(final Position newPos) {
			if (pos == NoPosition$.MODULE$) {
				pos = newPos;
			}
			return this;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "OrcToken " + type + " \"" + text + "\" " + pos; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

	}

	/**
	 * An Iterator over OrcTokens in the currently parsed string.
	 *
	 * @author jthywiss
	 */
	public class OrcTokenIterator implements Iterator<OrcToken> {
		private final IRegion regionOfInterest;
		private final boolean skipWhitespace;
		private final boolean skipComments;
		private int currOffset;
		private OrcToken nextToken;

		/**
		 * Constructs an object of class OrcTokenIterator.
		 *
		 * @param regionOfInterest Region of characters to scan (zero-based)
		 * @param skipWhitespace Flag, true to skip over whitespace
		 * @param skipComments Flag, true to skip over comments
		 */
		public OrcTokenIterator(final IRegion regionOfInterest, final boolean skipWhitespace, final boolean skipComments) {
			this.regionOfInterest = regionOfInterest;
			this.skipWhitespace = skipWhitespace;
			this.skipComments = skipComments;
			this.currOffset = 0;

			// Find the first token at or before the region start
			int prevOffset = 0;
			while (currOffset <= regionOfInterest.getOffset() && currOffsetInRegion()) {
				nextToken = getFirstTokenAt(currOffset);
				prevOffset = currOffset;
				currOffset += nextToken.text.length();
			}
			currOffset = prevOffset;

			// Position at the next interesting token
			prepNextToken(skipWhitespace, skipComments);
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return nextToken != null;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public OrcToken next() {
			if (!hasNext()) {
				throw new NoSuchElementException("OrcTokenIterator.next() called when no more tokens available"); //$NON-NLS-1$
			}
			final OrcToken currTok = nextToken;

			// Position at the next interesting token
			prepNextToken(skipWhitespace, skipComments);

			return currTok;
		}

		private boolean currOffsetInRegion() {
			// before EOF and in region
			return currOffset < getParseController().getCurrentParseString().length() && currOffset < regionOfInterest.getOffset() + regionOfInterest.getLength();
		}

		private void prepNextToken(final boolean skipWhitespace, final boolean skipComments) {
			nextToken = null;
			while (currOffsetInRegion()) {
				nextToken = getFirstTokenAt(currOffset);
				currOffset += nextToken.text.length();
				// If we found an "interesting" token, break
				if (!(skipWhitespace && nextToken.type == TokenType.WHITESPACE && skipComments && (nextToken.type == TokenType.COMMENT_ENDLINE || nextToken.type == TokenType.COMMENT_MULTILINE))) {
					break;
				}
				nextToken = null; // Not interesting, keep looking
			}
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		@Override
		public void remove() throws UnsupportedOperationException {
			throw new UnsupportedOperationException("OrcTokenIterator.remove() not permitted"); //$NON-NLS-1$
		}

	}

	private static class TokenRecord {
		public String tokenText;
		public TokenType tokenType;

		public TokenRecord(final String tokenText, final TokenType tokenType) {
			this.tokenText = tokenText;
			this.tokenType = tokenType;
		}

		public int length() {
			return tokenText.length();
		}
	}

	private static TokenRecord tokenTable[] = {
			// NUMBER_LITERAL,
			// STRING_LITERAL,
			// BOOLEAN_LITERAL,
			// NULL_LITERAL,
			// IDENTIFIER,
			// KEYWORD,
			new TokenRecord("_", TokenType.KEYWORD), //$NON-NLS-1$
			// OPERATOR,
			new TokenRecord("=", TokenType.OPERATOR), new TokenRecord("/=", TokenType.OPERATOR), new TokenRecord("<:", TokenType.OPERATOR), new TokenRecord("<=", TokenType.OPERATOR), new TokenRecord(":>", TokenType.OPERATOR), new TokenRecord(">=", TokenType.OPERATOR), new TokenRecord(":=", TokenType.OPERATOR), new TokenRecord("?", TokenType.OPERATOR), new TokenRecord("&&", TokenType.OPERATOR), new TokenRecord("||", TokenType.OPERATOR), new TokenRecord("~", TokenType.OPERATOR), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$
			new TokenRecord(":", TokenType.OPERATOR), new TokenRecord("+", TokenType.OPERATOR), new TokenRecord("-", TokenType.OPERATOR), new TokenRecord("*", TokenType.OPERATOR), new TokenRecord("/", TokenType.OPERATOR), new TokenRecord("%", TokenType.OPERATOR), new TokenRecord("**", TokenType.OPERATOR), new TokenRecord("0-", TokenType.OPERATOR), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
			// COMBINATOR,
			new TokenRecord(">", TokenType.COMBINATOR), new TokenRecord("|", TokenType.COMBINATOR), new TokenRecord("<", TokenType.COMBINATOR), new TokenRecord(";", TokenType.COMBINATOR), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			// BRACKET,
			new TokenRecord("(", TokenType.BRACKET), new TokenRecord(")", TokenType.BRACKET), new TokenRecord("[", TokenType.BRACKET), new TokenRecord("]", TokenType.BRACKET), new TokenRecord("{.", TokenType.BRACKET), new TokenRecord(".}", TokenType.BRACKET), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			// SEPERATOR,
			new TokenRecord(",", TokenType.SEPERATOR), new TokenRecord("#", TokenType.SEPERATOR), new TokenRecord("::", TokenType.SEPERATOR), new TokenRecord(":!:", TokenType.SEPERATOR), new TokenRecord(".", TokenType.SEPERATOR), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			// COMMENT_ENDLINE,
			new TokenRecord("--", TokenType.COMMENT_ENDLINE), //$NON-NLS-1$
			// COMMENT_MULTILINE,
			new TokenRecord("{-", TokenType.COMMENT_MULTILINE), }; //$NON-NLS-1$

	private static String keywords[] = { "as", "def", "else", "if", "import", "include", "lambda", "signal", "stop", "then", "type", "val", }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$

	private static final String NEWLINE_CHARS = "\n\r\f\u0085\u2028\u2029"; //$NON-NLS-1$
	private static final String WHITESPACE_CHARS = " \t" + NEWLINE_CHARS + "\u000B\u200E\u200F"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String DECIMAL_DIGIT_CHARS = "0123456789"; //$NON-NLS-1$

	private final OrcParseController parseController;

	/**
	 * Constructs an object of class OrcLexer, which will scan the currently parsed string in the given ParseController.
	 *
	 * @param parseController The ParseController for the file to be lex'ed
	 */
	public OrcLexer(final OrcParseController parseController) {
		super();
		this.parseController = parseController;
	}

	/**
	 * @return the ParseController associated with this OrcLexer
	 */
	public OrcParseController getParseController() {
		return parseController;
	}

	/**
	 * Get the first token found at a given offset in the currently parsed string.
	 *
	 * @param offset Character number at which to begin analyzing (zero-based)
	 * @return OrcToken of the found token
	 */
	public OrcToken getFirstTokenAt(final int offset) {
		final String text = parseController.getCurrentParseString();
		final TokenRecord ttEntry = findInTokenTable(text, offset);
		if (ttEntry != null && ttEntry.tokenType != TokenType.COMMENT_ENDLINE && ttEntry.tokenType != TokenType.COMMENT_MULTILINE) {
			return newToken(ttEntry.tokenType, text, offset, ttEntry.tokenText.length());
		}

		final char firstChar = text.charAt(0 + offset);
		final char secondChar = safeCharAt(text, 1 + offset);

		if (Character.isUnicodeIdentifierStart(firstChar)) {
			int tokenLength = 1;
			while (offset + tokenLength < text.length() && (Character.isUnicodeIdentifierPart(text.charAt(offset + tokenLength)) || text.charAt(offset + tokenLength) == '\'')) {
				++tokenLength;
			}
			final String word = text.substring(offset, offset + tokenLength);
			if ("null".equals(word)) { //$NON-NLS-1$
				return newToken(TokenType.NULL_LITERAL, text, offset, tokenLength);
			}
			if ("true".equals(word)) { //$NON-NLS-1$
				return newToken(TokenType.BOOLEAN_LITERAL, text, offset, tokenLength);
			}
			if ("false".equals(word)) { //$NON-NLS-1$
				return newToken(TokenType.BOOLEAN_LITERAL, text, offset, tokenLength);
			}
			if (isKeyword(word)) {
				return newToken(TokenType.KEYWORD, text, offset, tokenLength);
			}
			return newToken(TokenType.IDENTIFIER, text, offset, tokenLength);
		}

		if (isIn(firstChar, DECIMAL_DIGIT_CHARS) || firstChar == '-' && isIn(secondChar, DECIMAL_DIGIT_CHARS)) {
			int tokenLength = 1;
			while (isIn(safeCharAt(text, offset + tokenLength), DECIMAL_DIGIT_CHARS)) {
				++tokenLength;
			}
			if (safeCharAt(text, offset + tokenLength) == '.') {
				++tokenLength;
			}
			while (isIn(safeCharAt(text, offset + tokenLength), DECIMAL_DIGIT_CHARS)) {
				++tokenLength;
			}
			if (isIn(safeCharAt(text, offset + tokenLength), "Ee")) { //$NON-NLS-1$
				++tokenLength;
				if (isIn(safeCharAt(text, offset + tokenLength), "+-")) { //$NON-NLS-1$
					++tokenLength;
				}
				while (isIn(safeCharAt(text, offset + tokenLength), DECIMAL_DIGIT_CHARS)) {
					++tokenLength;
				}
			}
			return newToken(TokenType.NUMBER_LITERAL, text, offset, tokenLength);
		}

		if (firstChar == '\"') {
			int tokenLength = 1;
			char lastChar = safeCharAt(text, offset + tokenLength);
			while ((lastChar != '\"' && !isIn(lastChar, NEWLINE_CHARS) || safeCharAt(text, offset + tokenLength - 1) == '\\') && lastChar != '\0') {
				++tokenLength;
				lastChar = safeCharAt(text, offset + tokenLength);
			}
			if (safeCharAt(text, offset + tokenLength) == '\"') {
				++tokenLength; // Include the close delimiter in the token, if we saw it
			}
			return newToken(TokenType.STRING_LITERAL, text, offset, tokenLength);
		}

		if (ttEntry != null && ttEntry.tokenType == TokenType.COMMENT_ENDLINE) {
			int tokenLength = ttEntry.length();
			char lastChar = safeCharAt(text, offset + tokenLength);
			while (!isIn(lastChar, NEWLINE_CHARS) && lastChar != '\0') {
				++tokenLength;
				lastChar = safeCharAt(text, offset + tokenLength);
			}
			return newToken(TokenType.COMMENT_ENDLINE, text, offset, tokenLength);
		}

		if (ttEntry != null && ttEntry.tokenType == TokenType.COMMENT_MULTILINE) {
			int tokenLength = ttEntry.length();
			int commentNestLevel = 0;
			//FUTURE: Hard-coded to use "-}" as close -- 'twould be nice to use the token table somehow.
			char lastChar = safeCharAt(text, offset + tokenLength);
			char lookAhead = safeCharAt(text, offset + tokenLength + 1);
			while ((lastChar != '-' || lookAhead != '}' || commentNestLevel > 0) && lastChar != '\0') {
				if (lastChar == '{' && lookAhead == '-') {
					++commentNestLevel;
					++tokenLength; // consume two chars, not just one
				} else if (lastChar == '-' && lookAhead == '}' && commentNestLevel > 0) {
					--commentNestLevel;
					++tokenLength; // consume two chars, not just one
				}
				++tokenLength;
				lastChar = safeCharAt(text, offset + tokenLength);
				lookAhead = safeCharAt(text, offset + tokenLength + 1);
			}
			if (lastChar == '-' && lookAhead == '}') {
				tokenLength += 2; // Include the close delimiter in the token, if we saw it
			}
			return newToken(TokenType.COMMENT_MULTILINE, text, offset, tokenLength);
		}

		if (isIn(firstChar, WHITESPACE_CHARS)) {
			int tokenLength = 1;
			while (isIn(safeCharAt(text, offset + tokenLength), WHITESPACE_CHARS)) {
				++tokenLength;
			}
			return newToken(TokenType.WHITESPACE, text, offset, tokenLength);
		}

		return newToken(TokenType.UNKNOWN, text, offset, 1);
	}

	/**
	 * Returns an iterator over OrcTokens in the currently parsed string.
	 *
	 * This method skips whitespace, but returns comments.
	 *
	 * @return Iterator that returns tokens in the string
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<OrcToken> iterator() {
		return iterator(new Region(0, parseController.getCurrentParseString().length()));
	}

	/**
	 * Returns an iterator over OrcTokens in the currently parsed string.
	 *
	 * This method skips whitespace, but returns comments.
	 *
	 * @param regionOfInterest Region of characters to scan (zero-based)
	 * @return Iterator that returns tokens in the region
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<OrcToken> iterator(final IRegion regionOfInterest) {
		return iterator(regionOfInterest, true, false);
	}

	/**
	 * Returns an iterator over OrcTokens in the currently parsed string.
	 *
	 * @param skipWhitespace Flag, true to skip over whitespace
	 * @param skipComments Flag, true to skip over comments
	 * @return Iterator that returns tokens in the string
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<OrcToken> iterator(final boolean skipWhitespace, final boolean skipComments) {
		return iterator(new Region(0, parseController.getCurrentParseString().length()), skipWhitespace, skipComments);
	}

	/**
	 * Returns an iterator over OrcTokens in the currently parsed string.
	 *
	 * @param regionOfInterest Region of characters to scan (zero-based)
	 * @param skipWhitespace Flag, true to skip over whitespace
	 * @param skipComments Flag, true to skip over comments
	 * @return Iterator that returns tokens in the region
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<OrcToken> iterator(final IRegion regionOfInterest, final boolean skipWhitespace, final boolean skipComments) {
		return new OrcTokenIterator(regionOfInterest, skipWhitespace, skipComments);
	}

	/**
	 * @param word String to check for being an Orc keyword
	 * @return true if a match for an Orc keyword
	 */
	public static boolean isKeyword(final String word) {
		for (final String keyword : keywords) {
			if (keyword.equals(word)) {
				return true;
			}
		}
		return false;
	}

	private static TokenRecord findInTokenTable(final String text, final int offset) {
		TokenRecord match = null;
		for (final TokenRecord currTokenRecord : tokenTable) {
			if (text.startsWith(currTokenRecord.tokenText, offset)) {
				if (match == null || match.length() < currTokenRecord.length()) {
					match = currTokenRecord;
				}
			}
		}
		return match;
	}

	private static char safeCharAt(final String string, final int index) {
		if (index >= string.length()) {
			return '\0';
		}
		return string.charAt(index);
	}

	private static boolean isIn(final char ch, final String s) {
		return s.indexOf(ch) > -1;
	}

	private OrcToken newToken(final TokenType tokenType, final String text, final int startOffset, final int length) {
		return new OrcToken(tokenType, text.substring(startOffset, startOffset + length), new OffsetPosition(text, startOffset));
	}
}
