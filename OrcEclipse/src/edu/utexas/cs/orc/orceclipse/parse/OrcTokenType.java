//
// OrcTokenType.java -- Java class OrcTokenType
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

import org.eclipse.jface.text.rules.IToken;

import edu.utexas.cs.orc.orceclipse.edit.OrcTextAttributes;

/**
 * A type of Orc token for use in Eclipse's JFace text rules. This implements
 * the misleadingly named IToken interface. Note that an IToken, as used by by
 * the JFace text rules, is a type of a token, not the token itself. Note also
 * that there is bizarre coupling: the getData on an IToken returns the text
 * display attributes for the token.
 *
 * @author jthywiss
 */
public class OrcTokenType implements IToken {
    private final String name;

    /**
     * No instances beyond the fixed set of instances defined here.
     */
    private OrcTokenType(final String tokenTypeName) {
        name = tokenTypeName;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Return whether this token is undefined, meaning unrecognized by the
     * scanner.
     *
     * @return <code>true</code>if this token is undefined
     */
    @Override
    public boolean isUndefined() {
        return this == UNKNOWN;
    }

    /**
     * Return whether this token represents a whitespace.
     *
     * @return <code>true</code>if this token represents a whitespace
     */
    @Override
    public boolean isWhitespace() {
        return this == WHITESPACE;
    }

    /**
     * Return whether this token represents End Of File.
     *
     * @return <code>true</code>if this token represents EOF
     */
    @Override
    public boolean isEOF() {
        return this == EOF;
    }

    /**
     * Return whether this token is neither undefined, nor whitespace, nor EOF.
     *
     * @return <code>true</code>if this token is not undefined, not a
     *         whitespace, and not EOF
     */
    @Override
    public boolean isOther() {
        return !(isUndefined() || isWhitespace() || isEOF());
    }

    /**
     * Return the display TextAttribute for this token.
     *
     * @return the data attached to this token.
     * @see OrcTextAttributes#getDisplayTextAttribute(OrcTokenType)
     */
    @Override
    public Object getData() {
        return OrcTextAttributes.getInstance().getDisplayTextAttribute(this);
    }

    /** Character that is not any valid Orc lexeme */
    public static OrcTokenType UNKNOWN = new OrcTokenType("UNKNOWN"); //$NON-NLS-1$
    /** Numeric literal (integer or floating point) */
    public static OrcTokenType NUMBER_LITERAL = new OrcTokenType("NUMBER_LITERAL"); //$NON-NLS-1$
    /** String literal -- quoted character sequence */
    public static OrcTokenType STRING_LITERAL = new OrcTokenType("STRING_LITERAL"); //$NON-NLS-1$
    /** "true" or "false" */
    public static OrcTokenType BOOLEAN_LITERAL = new OrcTokenType("BOOLEAN_LITERAL"); //$NON-NLS-1$
    /** "null" */
    public static OrcTokenType NULL_LITERAL = new OrcTokenType("NULL_LITERAL"); //$NON-NLS-1$
    /** An identifier, not a keyword */
    public static OrcTokenType IDENTIFIER = new OrcTokenType("IDENTIFIER"); //$NON-NLS-1$
    /** An Orc keyword */
    public static OrcTokenType KEYWORD = new OrcTokenType("KEYWORD"); //$NON-NLS-1$
    /** An operator that isn't a combinator. */
    public static OrcTokenType OPERATOR = new OrcTokenType("OPERATOR"); //$NON-NLS-1$
    /** An Orc combinator. */
    public static OrcTokenType COMBINATOR = new OrcTokenType("COMBINATOR"); //$NON-NLS-1$
    /** Parenthesis, square brackets, or curly braces */
    public static OrcTokenType BRACKET = new OrcTokenType("BRACKET"); //$NON-NLS-1$
    /** Lexemes like ",", ";", etc. */
    public static OrcTokenType SEPARATOR = new OrcTokenType("SEPARATOR"); //$NON-NLS-1$
    /** Comment to end of line (a.k.a. "single line comment") */
    public static OrcTokenType COMMENT_ENDLINE = new OrcTokenType("COMMENT_ENDLINE"); //$NON-NLS-1$
    /**
     * Comment bracketed by "{-" "-}". These brackets are included in the token.
     * Despite the name, the comment could be on one line.
     */
    public static OrcTokenType COMMENT_MULTILINE = new OrcTokenType("COMMENT_MULTILINE"); //$NON-NLS-1$
    /** Spaces, tabs, end of line chars, etc. */
    public static OrcTokenType WHITESPACE = new OrcTokenType("WHITESPACE"); //$NON-NLS-1$
    /** End of file */
    public static OrcTokenType EOF = new OrcTokenType("EOF"); //$NON-NLS-1$

}
