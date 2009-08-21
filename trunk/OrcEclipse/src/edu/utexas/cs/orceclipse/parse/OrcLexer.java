//
// OrcLexer.java -- Java class OrcLexer
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 13, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orceclipse.parse;

import java.util.Iterator;
import java.util.NoSuchElementException;

import orc.error.Located;
import orc.error.SourceLocation;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Provides lexical scanning for the currently parsed string in the given ParseController.
 *
 * @author jthywiss
 */
public class OrcLexer implements Iterable<OrcLexer.OrcToken> {
	/**
	 * 
	 *
	 * @author jthywiss
	 */
	public enum TokenType {
		UNKNOWN, 
		NUMBER_LITERAL, 
		STRING_LITERAL, 
		BOOLEAN_LITERAL, 
		NULL_LITERAL, 
		IDENTIFIER_OR_KEYWORD, // When we don't know yet
		IDENTIFIER, // We've checked against keyword list
		KEYWORD, // We've checked against keyword list
		OPERATOR, 
		COMBINATOR, 
		BRACKET, 
		SEPERATOR, 
		COMMENT_ENDLINE, 
		COMMENT_MULTILINE, 
		WHITESPACE,
	}

	/**
	 * 
	 *
	 * @author jthywiss
	 */
	public static class OrcToken implements Located {
		public TokenType type;
		public String text;
		public SourceLocation location;

		public OrcToken(final TokenType type, final String text, final SourceLocation location) {
			this.type = type;
			this.text = text;
			this.location = location;
		}

		/* (non-Javadoc)
		 * @see orc.error.Located#getSourceLocation()
		 */
		public SourceLocation getSourceLocation() {
			return location;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "OrcToken " + type + " \"" + text + "\" " + location;
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
			while (currOffset <= regionOfInterest.getOffset() && hasNext()) {
				nextToken = getFirstTokenAt(currOffset);
				prevOffset = currOffset;
				currOffset += nextToken.text.length();
			}
			currOffset = prevOffset;

			// Position at the next interesting token
			while (hasNext()) {
				nextToken = getFirstTokenAt(currOffset);
				currOffset += nextToken.text.length();
				if (!(skipWhitespace && nextToken.type == TokenType.WHITESPACE && skipComments && (nextToken.type == TokenType.COMMENT_ENDLINE || nextToken.type == TokenType.COMMENT_MULTILINE))) {
					break;
				}
			}
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return currOffset < getParseController().getCurrentParseString().length() && currOffset < regionOfInterest.getOffset() + regionOfInterest.getLength();
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public OrcToken next() {
			if (!hasNext()) {
				throw new NoSuchElementException("OrcTokenIterator.next() called when no more tokens available");
			}
			final OrcToken currTok = nextToken;

			// Position at the next interesting token
			while (hasNext()) {
				nextToken = getFirstTokenAt(currOffset);
				currOffset += nextToken.text.length();
				if (!(skipWhitespace && nextToken.type == TokenType.WHITESPACE && skipComments && (nextToken.type == TokenType.COMMENT_ENDLINE || nextToken.type == TokenType.COMMENT_MULTILINE))) {
					break;
				}
			}

			return currTok;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() throws UnsupportedOperationException {
			throw new UnsupportedOperationException("OrcTokenIterator.remove() not permitted");
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
			// OPERATOR,
			new TokenRecord("=", TokenType.OPERATOR), new TokenRecord("/=", TokenType.OPERATOR), new TokenRecord("<:", TokenType.OPERATOR), new TokenRecord("<=", TokenType.OPERATOR), new TokenRecord(":>", TokenType.OPERATOR), new TokenRecord(">=", TokenType.OPERATOR), new TokenRecord(":=", TokenType.OPERATOR), new TokenRecord("?", TokenType.OPERATOR), new TokenRecord("&&", TokenType.OPERATOR), new TokenRecord("||", TokenType.OPERATOR), new TokenRecord("~", TokenType.OPERATOR),
			new TokenRecord(":", TokenType.OPERATOR), new TokenRecord("+", TokenType.OPERATOR), new TokenRecord("-", TokenType.OPERATOR), new TokenRecord("*", TokenType.OPERATOR), new TokenRecord("/", TokenType.OPERATOR), new TokenRecord("%", TokenType.OPERATOR), new TokenRecord("**", TokenType.OPERATOR), new TokenRecord("0-", TokenType.OPERATOR),
			// COMBINATOR,
			new TokenRecord(">", TokenType.COMBINATOR), new TokenRecord("|", TokenType.COMBINATOR), new TokenRecord("<", TokenType.COMBINATOR), new TokenRecord(";", TokenType.COMBINATOR),
			// BRACKET,
			new TokenRecord("(", TokenType.BRACKET), new TokenRecord(")", TokenType.BRACKET), new TokenRecord("[", TokenType.BRACKET), new TokenRecord("]", TokenType.BRACKET), new TokenRecord("{", TokenType.BRACKET), new TokenRecord("}", TokenType.BRACKET),
			// SEPERATOR,
			new TokenRecord(",", TokenType.SEPERATOR), new TokenRecord("::", TokenType.SEPERATOR), new TokenRecord(":!:", TokenType.SEPERATOR), new TokenRecord(".", TokenType.SEPERATOR), new TokenRecord("!", TokenType.SEPERATOR),
			// COMMENT_ENDLINE,
			new TokenRecord("--", TokenType.COMMENT_ENDLINE),
			// COMMENT_MULTILINE,
			new TokenRecord("{-", TokenType.COMMENT_MULTILINE), };

	private static String keywords[] = { "def", "val", "class", "site", "include", "type", "lambda", "atomic", "isolated", "try", "throw", "catch", "as", "if", "then", "else", "stop", "signal", };

	private static final String WHITESPACE_CHARS = " \t\f\r\n";
	private static final String IDENTIFIER_FIRST_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_";
	private static final String IDENTIFIER_FOLLOW_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_'";
	private static final String DECIMAL_DIGIT_CHARS = "0123456789";

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

			// Special case for our friends, < and > -- are they operators or combinators?  Guess based on whitespace.
			if ("<".equals(ttEntry.tokenText) || ">".equals(ttEntry.tokenText)) {
				// If whitespace on both sides or neither side, guess an operator
				if (isIn(safeCharAt(text, offset - 1), WHITESPACE_CHARS) == isIn(safeCharAt(text, offset + 1), WHITESPACE_CHARS)) {
					return newToken(TokenType.OPERATOR, text, offset, ttEntry.tokenText.length());
				}
			}

			return newToken(ttEntry.tokenType, text, offset, ttEntry.tokenText.length());
		}

		final char firstChar = text.charAt(0 + offset);
		final char secondChar = safeCharAt(text, 1 + offset);

		if (isIn(firstChar, IDENTIFIER_FIRST_CHARS)) {
			int tokenLength = 1;
			while (isIn(safeCharAt(text, offset + tokenLength), IDENTIFIER_FOLLOW_CHARS)) {
				++tokenLength;
			}
			final String word = text.substring(offset, offset + tokenLength);
			if ("null".equals(word)) {
				return newToken(TokenType.NULL_LITERAL, text, offset, tokenLength);
			}
			if ("true".equals(word)) {
				return newToken(TokenType.BOOLEAN_LITERAL, text, offset, tokenLength);
			}
			if ("false".equals(word)) {
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
			if (isIn(safeCharAt(text, offset + tokenLength), "Ee")) {
				++tokenLength;
				if (isIn(safeCharAt(text, offset + tokenLength), "+-")) {
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
			while ((safeCharAt(text, offset + tokenLength) != '\"' || safeCharAt(text, offset + tokenLength - 1) == '\\') && safeCharAt(text, offset + tokenLength) != '\0') {
				++tokenLength;
			}
			if (safeCharAt(text, offset + tokenLength) == '\"') {
				++tokenLength; // Include the close delimiter in the token, if we saw it
			}
			return newToken(TokenType.STRING_LITERAL, text, offset, tokenLength);
		}

		if (ttEntry != null && ttEntry.tokenType == TokenType.COMMENT_ENDLINE) {
			int tokenLength = ttEntry.length();
			while (safeCharAt(text, offset + tokenLength) != '\r' && safeCharAt(text, offset + tokenLength) != '\n' && safeCharAt(text, offset + tokenLength) != '\0') {
				++tokenLength;
			}
			return newToken(TokenType.COMMENT_ENDLINE, text, offset, tokenLength);
		}

		if (ttEntry != null && ttEntry.tokenType == TokenType.COMMENT_MULTILINE) {
			int tokenLength = ttEntry.length();
			//FUTURE: Hard-coded to use "-}" as close -- 'twould be nice to use the token table somehow.
			while ((safeCharAt(text, offset + tokenLength) != '-' || safeCharAt(text, offset + tokenLength + 1) != '}') && safeCharAt(text, offset + tokenLength) != '\0') {
				++tokenLength;
			}
			if (safeCharAt(text, offset + tokenLength) == '-' && safeCharAt(text, offset + tokenLength + 1) == '}') {
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
		return new OrcToken(tokenType, text.substring(startOffset, startOffset + length), new SourceLocation(parseController.getPath().toFile(),
		//TODO: line number/column tracking -- not needed yet
				startOffset, 0, 0, startOffset + length - 1, 0, 0));
	}
}
