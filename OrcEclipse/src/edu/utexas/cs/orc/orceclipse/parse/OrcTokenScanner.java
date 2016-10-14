//
// OrcTokenScanner.java -- Java class OrcTokenScanner
// Project OrcEclipse
//
// Created by jthywiss on Jul 7, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.parse;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;

/**
 * A token scanner scans a range of a document and reports about the token it
 * finds. A scanner has state. When asked, the scanner returns the offset and
 * the length of the last found token.
 *
 * @see org.eclipse.jface.text.rules.IToken
 * @author jthywiss
 */
public class OrcTokenScanner implements ITokenScanner {

    private IDocument scannedDocument = null;
    private int rangeEnd = -1;
    private int currentScanningOffset = -1;
    private int mostRecentTokenOffset = -1;
    private int mostRecentTokenLength = -1;

    /**
     * Constructs an OrcTokenScanner.
     */
    public OrcTokenScanner() {
        /* Nothing needed */
    }

    /**
     * Configures the scanner by providing access to the document range that
     * should be scanned.
     *
     * @param document the document to scan
     * @param offset the offset of the document range to scan
     * @param length the length of the document range to scan
     */
    @Override
    public void setRange(final IDocument document, final int offset, final int length) {
        if (offset < 0 || length < 0 || offset + length > document.getLength()) {
            throw new IllegalArgumentException("Illegal TokenScanner range: offset=" + offset + ", length=" + length + ", document.getLength()=" + document.getLength()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        scannedDocument = document;
        rangeEnd = offset + length;
        currentScanningOffset = offset;
    }

    /**
     * Returns the next token in the document.
     *
     * @return the next token in the document
     */
    @Override
    public IToken nextToken() {
        final int offset = currentScanningOffset;

        if (currentScanningOffset >= rangeEnd) {
            return newToken(OrcTokenType.EOF, offset, 0);
        }

        final char firstChar = docCharAt(0 + offset);
        final char secondChar = docCharAt(1 + offset);

        /*
         * Whitespace
         */
        if (isIn(firstChar, WHITESPACE_CHARS)) {
            int tokenLength = 1;
            while (isIn(docCharAt(offset + tokenLength), WHITESPACE_CHARS)) {
                ++tokenLength;
            }
            return newToken(OrcTokenType.WHITESPACE, offset, tokenLength);
        }

        /*
         * Single-line comment
         */
        if (firstChar == '-' && secondChar == '-') {
            int tokenLength = 2;
            char lastChar = docCharAt(offset + tokenLength);
            while (!isIn(lastChar, NEWLINE_CHARS) && lastChar != '\0') {
                ++tokenLength;
                lastChar = docCharAt(offset + tokenLength);
            }
            return newToken(OrcTokenType.COMMENT_ENDLINE, offset, tokenLength);
        }

        /*
         * Multi-line comment
         */
        if (firstChar == '{' && secondChar == '-') {
            int tokenLength = 2;
            int commentNestLevel = 0;
            char lastChar = docCharAt(offset + tokenLength);
            char lookAhead = docCharAt(offset + tokenLength + 1);
            while ((lastChar != '-' || lookAhead != '}' || commentNestLevel > 0) && lastChar != '\0') {
                if (lastChar == '{' && lookAhead == '-') {
                    ++commentNestLevel;
                    /* consume two chars, not just one */
                    ++tokenLength;
                } else if (lastChar == '-' && lookAhead == '}' && commentNestLevel > 0) {
                    --commentNestLevel;
                    /* consume two chars, not just one */
                    ++tokenLength;
                }
                ++tokenLength;
                lastChar = docCharAt(offset + tokenLength);
                lookAhead = docCharAt(offset + tokenLength + 1);
            }
            if (lastChar == '-' && lookAhead == '}') {
                /* Include the close delimiter in the token, if we saw it */
                tokenLength += 2;
            }
            return newToken(OrcTokenType.COMMENT_MULTILINE, offset, tokenLength);
        }

        /*
         * Keyword or Identifier
         */
        if (Character.isUnicodeIdentifierStart(firstChar)) {
            int tokenLength = 1;
            while (offset + tokenLength < rangeEnd && (Character.isUnicodeIdentifierPart(docCharAt(offset + tokenLength)) || docCharAt(offset + tokenLength) == '\'')) {
                ++tokenLength;
            }
            String word;
            try {
                word = scannedDocument.get(offset, tokenLength);
            } catch (final BadLocationException e) {
                throw new AssertionError("IDocument.get: BadLocationException: offset=" + offset + ", length=" + tokenLength, e); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if ("null".equals(word)) { //$NON-NLS-1$
                return newToken(OrcTokenType.NULL_LITERAL, offset, tokenLength);
            }
            if ("true".equals(word)) { //$NON-NLS-1$
                return newToken(OrcTokenType.BOOLEAN_LITERAL, offset, tokenLength);
            }
            if ("false".equals(word)) { //$NON-NLS-1$
                return newToken(OrcTokenType.BOOLEAN_LITERAL, offset, tokenLength);
            }
            if (isKeyword(word)) {
                return newToken(OrcTokenType.KEYWORD, offset, tokenLength);
            }
            return newToken(OrcTokenType.IDENTIFIER, offset, tokenLength);
        }

        /*
         * Number literal
         */
        if (isIn(firstChar, DECIMAL_DIGIT_CHARS) || firstChar == '-' && isIn(secondChar, DECIMAL_DIGIT_CHARS)) {
            int tokenLength = 1;
            while (isIn(docCharAt(offset + tokenLength), DECIMAL_DIGIT_CHARS)) {
                ++tokenLength;
            }
            if (docCharAt(offset + tokenLength) == '.') {
                ++tokenLength;
            }
            while (isIn(docCharAt(offset + tokenLength), DECIMAL_DIGIT_CHARS)) {
                ++tokenLength;
            }
            if (isIn(docCharAt(offset + tokenLength), "Ee")) { //$NON-NLS-1$
                ++tokenLength;
                if (isIn(docCharAt(offset + tokenLength), "+-")) { //$NON-NLS-1$
                    ++tokenLength;
                }
                while (isIn(docCharAt(offset + tokenLength), DECIMAL_DIGIT_CHARS)) {
                    ++tokenLength;
                }
            }
            return newToken(OrcTokenType.NUMBER_LITERAL, offset, tokenLength);
        }

        /*
         * String literal
         */
        if (firstChar == '\"') {
            int tokenLength = 1;
            char lastChar = docCharAt(offset + tokenLength);
            while ((lastChar != '\"' && !isIn(lastChar, NEWLINE_CHARS) || docCharAt(offset + tokenLength - 1) == '\\') && lastChar != '\0') {
                ++tokenLength;
                lastChar = docCharAt(offset + tokenLength);
            }
            if (docCharAt(offset + tokenLength) == '\"') {
                /* Include the close delimiter in the token, if we saw it */
                ++tokenLength;
            }
            return newToken(OrcTokenType.STRING_LITERAL, offset, tokenLength);
        }

        /*
         * Delimiter/operator
         */
        final TokenRecord ttEntry = findInDelimiterTable(offset);
        if (ttEntry != null) {
            return newToken(ttEntry.tokenType, offset, ttEntry.tokenText.length());
        }

        /*
         * Bad character
         */
        return newToken(OrcTokenType.UNKNOWN, offset, 1);
    }

    /**
     * Returns an IToken for the token type at the given position in the source
     * text.
     *
     * @param tokenType type of token recognized
     * @param offset offset into source document (number of chars)
     * @param length length of token (number of chars)
     * @return
     */
    private IToken newToken(final OrcTokenType tokenType, final int offset, final int length) {
        mostRecentTokenOffset = offset;
        mostRecentTokenLength = length;
        currentScanningOffset = offset + length;
        return tokenType;
    }

    /**
     * Returns the offset of the last token read by this scanner.
     *
     * @return the offset of the last token read by this scanner
     */
    @Override
    public int getTokenOffset() {
        return mostRecentTokenOffset;
    }

    /**
     * Returns the length of the last token read by this scanner.
     *
     * @return the length of the last token read by this scanner
     */
    @Override
    public int getTokenLength() {
        return mostRecentTokenLength;
    }

    /**
     * Safely retrieve the character at the given index. If the index is beyond
     * the set range, returns '\0'.
     *
     * @param index index in document of character
     * @return character or '\0'
     */
    private char docCharAt(final int index) {
        if (index >= rangeEnd) {
            return '\0';
        }
        try {
            return scannedDocument.getChar(index);
        } catch (final BadLocationException e) {
            throw new AssertionError("IDocument.getChar: BadLocationException: offset=" + index, e); //$NON-NLS-1$
        }
    }

    /**
     * Check if a given character is in a string.
     *
     * @param ch a character to check
     * @param s a string to check against
     * @return true if 'ch' is in 's'
     */
    private static boolean isIn(final char ch, final String s) {
        return s.indexOf(ch) > -1;
    }

    private static class TokenRecord {
        public String tokenText;
        public OrcTokenType tokenType;

        public TokenRecord(final String tokenText, final OrcTokenType tokenType) {
            this.tokenText = tokenText;
            this.tokenType = tokenType;
        }

        public int length() {
            return tokenText.length();
        }
    }

    private TokenRecord findInDelimiterTable(final int offset) {
        TokenRecord match = null;
        for (final TokenRecord currDelimRecord : delimiterTable) {
            String inputChars = null;
            if (offset + currDelimRecord.length() <= rangeEnd) {
                if (inputChars == null || inputChars.length() != currDelimRecord.length()) {
                    try {
                        inputChars = scannedDocument.get(offset, currDelimRecord.length());
                    } catch (final BadLocationException e) {
                        throw new AssertionError("IDocument.get: BadLocationException: offset=" + offset + ", length=" + currDelimRecord.length(), e); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
                if (inputChars.equals(currDelimRecord.tokenText)) {
                    if (match == null || match.length() < currDelimRecord.length()) {
                        match = currDelimRecord;
                    }
                }
            }
        }
        return match;
    }

    /**
     * Check if a String is an Orc keyword.
     *
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

    @SuppressWarnings("nls")
    private static TokenRecord delimiterTable[] = {
        // KEYWORD,
        new TokenRecord("_", OrcTokenType.KEYWORD),
        // OPERATOR,
        new TokenRecord("=", OrcTokenType.OPERATOR), 
        new TokenRecord("/=", OrcTokenType.OPERATOR), 
        new TokenRecord("<:", OrcTokenType.OPERATOR), 
        new TokenRecord("<=", OrcTokenType.OPERATOR), 
        new TokenRecord(":>", OrcTokenType.OPERATOR), 
        new TokenRecord(">=", OrcTokenType.OPERATOR), 
        new TokenRecord(":=", OrcTokenType.OPERATOR), 
        new TokenRecord("?", OrcTokenType.OPERATOR), 
        new TokenRecord("&&", OrcTokenType.OPERATOR), 
        new TokenRecord("||", OrcTokenType.OPERATOR), 
        new TokenRecord("~", OrcTokenType.OPERATOR), 
        new TokenRecord(":", OrcTokenType.OPERATOR), 
        new TokenRecord("+", OrcTokenType.OPERATOR), 
        new TokenRecord("-", OrcTokenType.OPERATOR), 
        new TokenRecord("*", OrcTokenType.OPERATOR), 
        new TokenRecord("/", OrcTokenType.OPERATOR), 
        new TokenRecord("%", OrcTokenType.OPERATOR), 
        new TokenRecord("**", OrcTokenType.OPERATOR), 
        new TokenRecord("0-", OrcTokenType.OPERATOR),
        // COMBINATOR,
        new TokenRecord(">", OrcTokenType.COMBINATOR), 
        new TokenRecord("|", OrcTokenType.COMBINATOR), 
        new TokenRecord("<", OrcTokenType.COMBINATOR), 
        new TokenRecord(";", OrcTokenType.COMBINATOR), 
        // BRACKET,
        new TokenRecord("(", OrcTokenType.BRACKET), new TokenRecord(")", OrcTokenType.BRACKET), 
        new TokenRecord("[", OrcTokenType.BRACKET), new TokenRecord("]", OrcTokenType.BRACKET), 
        new TokenRecord("{.", OrcTokenType.BRACKET), new TokenRecord(".}", OrcTokenType.BRACKET), 
        new TokenRecord("{|", OrcTokenType.BRACKET), new TokenRecord("|}", OrcTokenType.BRACKET), 
        new TokenRecord("{", OrcTokenType.BRACKET), new TokenRecord("}", OrcTokenType.BRACKET), 
        // SEPARATOR,
        new TokenRecord(",", OrcTokenType.SEPARATOR), 
        new TokenRecord("#", OrcTokenType.SEPARATOR), 
        new TokenRecord("::", OrcTokenType.SEPARATOR), 
        new TokenRecord(":!:", OrcTokenType.SEPARATOR), 
        new TokenRecord(".", OrcTokenType.SEPARATOR),
    };

    @SuppressWarnings("nls")
    private static String keywords[] = { "as", "def", "else", "if", "import", "include", "signal", 
        "stop", "then", "type", "val", "class", "new", "super", "extends", "with", "this", "site"};
    
    private static final String NEWLINE_CHARS = "\n\r\f\u0085\u2028\u2029"; //$NON-NLS-1$
    private static final String WHITESPACE_CHARS = " \t" + NEWLINE_CHARS + "\u000B\u200E\u200F"; //$NON-NLS-1$ //$NON-NLS-2$
    private static final String DECIMAL_DIGIT_CHARS = "0123456789"; //$NON-NLS-1$

}
