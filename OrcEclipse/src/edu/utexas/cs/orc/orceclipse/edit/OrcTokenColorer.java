//
// OrcTokenColorer.java -- Java class OrcTokenColorer
// Project OrcEclipse
//
// Created by jthywiss on Jul 28, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.services.ITokenColorer;
import org.eclipse.imp.services.base.TokenColorerBase;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import edu.utexas.cs.orc.orceclipse.parse.OrcLexer.OrcToken;

/**
 * Provides token-coloring services for the Orc language to the IMP source code
 * editor.
 * <p>
 * Given a token, {@link #getColoring(IParseController, Object)} returns the
 * {@link TextAttribute} to use to display that token.
 *
 * @author jthywiss
 */
public class OrcTokenColorer extends TokenColorerBase implements ITokenColorer {
    /**
     *
     */
    protected final TextAttribute identifierAttribute, // keywordAttribute, (already in TokenColorerBase)
    numberAttribute, stringAttribute, commentAttribute, operatorAttribute, combinatorAttribute, bracketAttribute, badCharAttribute;

    /**
     * Constructs an object of class OrcTokenColorer.
     */
    public OrcTokenColorer() {
        super();
        final Display display = Display.getDefault();
        identifierAttribute = new TextAttribute(display.getSystemColor(SWT.COLOR_DARK_BLUE), null, SWT.ITALIC);
        keywordAttribute = new TextAttribute(display.getSystemColor(SWT.COLOR_DARK_MAGENTA), null, SWT.BOLD);
        numberAttribute = new TextAttribute(display.getSystemColor(SWT.COLOR_BLUE), null, SWT.NORMAL);
        stringAttribute = new TextAttribute(display.getSystemColor(SWT.COLOR_BLUE), null, SWT.NORMAL);
        commentAttribute = new TextAttribute(display.getSystemColor(SWT.COLOR_DARK_GREEN), null, SWT.NORMAL);
        operatorAttribute = new TextAttribute(display.getSystemColor(SWT.COLOR_BLACK), null, SWT.NORMAL);
        combinatorAttribute = new TextAttribute(display.getSystemColor(SWT.COLOR_RED), null, SWT.BOLD);
        bracketAttribute = new TextAttribute(display.getSystemColor(SWT.COLOR_BLACK), null, SWT.NORMAL);
        badCharAttribute = new TextAttribute(display.getSystemColor(SWT.COLOR_WHITE), display.getSystemColor(SWT.COLOR_RED), SWT.NORMAL);
    }

    @Override
    public TextAttribute getColoring(final IParseController controller, final Object o) {
        if (o == null) {
            return null;
        }
        final OrcToken token = (OrcToken) o;

        switch (token.type) {
        case NUMBER_LITERAL:
            return numberAttribute;
        case STRING_LITERAL:
            return stringAttribute;
        case BOOLEAN_LITERAL:
            return keywordAttribute;
        case NULL_LITERAL:
            return keywordAttribute;
        case IDENTIFIER:
            return identifierAttribute;
        case KEYWORD:
            return keywordAttribute;
        case OPERATOR:
            return operatorAttribute;
        case COMBINATOR:
            return combinatorAttribute;
        case BRACKET:
            return bracketAttribute;
        case SEPERATOR:
            return operatorAttribute;
        case COMMENT_ENDLINE:
        case COMMENT_MULTILINE:
            return commentAttribute;
        case UNKNOWN:
            return badCharAttribute;
        default:
            // Fall through
        }
        return super.getColoring(controller, token);
    }
}
