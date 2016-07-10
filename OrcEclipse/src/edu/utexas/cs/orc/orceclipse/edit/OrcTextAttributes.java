//
// OrcTextAttributes.java -- Java class OrcTextAttributes
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

package edu.utexas.cs.orc.orceclipse.edit;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import edu.utexas.cs.orc.orceclipse.parse.OrcTokenType;

/**
 * Map from OrcTokenTypes to the TextAttribute to use when rendering them.
 *
 * @author jthywiss
 */
public class OrcTextAttributes {

    private static OrcTextAttributes instance;

    private final Map<OrcTokenType, TextAttribute> tokenTextAttributeMap = new HashMap<OrcTokenType, TextAttribute>();

    /**
     * Constructs an instance of OrcTextAttributes, and initialize it.
     */
    private OrcTextAttributes() {
        final Display display = Display.getDefault();

        tokenTextAttributeMap.put(OrcTokenType.UNKNOWN, new TextAttribute(display.getSystemColor(SWT.COLOR_WHITE), display.getSystemColor(SWT.COLOR_RED), SWT.NORMAL));
        tokenTextAttributeMap.put(OrcTokenType.NUMBER_LITERAL, new TextAttribute(display.getSystemColor(SWT.COLOR_BLUE), null, SWT.NORMAL));
        tokenTextAttributeMap.put(OrcTokenType.STRING_LITERAL, new TextAttribute(display.getSystemColor(SWT.COLOR_BLUE), null, SWT.NORMAL));
        tokenTextAttributeMap.put(OrcTokenType.BOOLEAN_LITERAL, new TextAttribute(display.getSystemColor(SWT.COLOR_DARK_MAGENTA), null, SWT.BOLD));
        tokenTextAttributeMap.put(OrcTokenType.NULL_LITERAL, new TextAttribute(display.getSystemColor(SWT.COLOR_DARK_MAGENTA), null, SWT.BOLD));
        tokenTextAttributeMap.put(OrcTokenType.IDENTIFIER, new TextAttribute(display.getSystemColor(SWT.COLOR_DARK_BLUE), null, SWT.ITALIC));
        tokenTextAttributeMap.put(OrcTokenType.KEYWORD, new TextAttribute(display.getSystemColor(SWT.COLOR_DARK_MAGENTA), null, SWT.BOLD));
        tokenTextAttributeMap.put(OrcTokenType.OPERATOR, new TextAttribute(display.getSystemColor(SWT.COLOR_BLACK), null, SWT.NORMAL));
        tokenTextAttributeMap.put(OrcTokenType.COMBINATOR, new TextAttribute(display.getSystemColor(SWT.COLOR_RED), null, SWT.BOLD));
        tokenTextAttributeMap.put(OrcTokenType.BRACKET, new TextAttribute(display.getSystemColor(SWT.COLOR_BLACK), null, SWT.NORMAL));
        tokenTextAttributeMap.put(OrcTokenType.SEPARATOR, new TextAttribute(display.getSystemColor(SWT.COLOR_BLACK), null, SWT.NORMAL));
        tokenTextAttributeMap.put(OrcTokenType.COMMENT_ENDLINE, new TextAttribute(display.getSystemColor(SWT.COLOR_DARK_GREEN), null, SWT.NORMAL));
        tokenTextAttributeMap.put(OrcTokenType.COMMENT_MULTILINE, new TextAttribute(display.getSystemColor(SWT.COLOR_DARK_GREEN), null, SWT.NORMAL));
        tokenTextAttributeMap.put(OrcTokenType.WHITESPACE, new TextAttribute(display.getSystemColor(SWT.COLOR_BLACK), null, SWT.NORMAL));
        tokenTextAttributeMap.put(OrcTokenType.EOF, new TextAttribute(display.getSystemColor(SWT.COLOR_BLACK), null, SWT.NORMAL));
    }

    /**
     * Get the singleton OrcTextAttributes instance.
     *
     * @return the singleton OrcTextAttributes instance
     */
    public static OrcTextAttributes getInstance() {
        if (instance == null) {
            instance = new OrcTextAttributes();
        }
        return instance;
    }

    /**
     * Get a display TextAttribute for a given OrcTokenType.
     *
     * @param tokenType the token type to look up TextAttribute for
     * @return the TextAttribute for the given OrcTokenType
     */
    public TextAttribute getDisplayTextAttribute(final OrcTokenType tokenType) {
        return tokenTextAttributeMap.get(tokenType);
    }
}
