//
// OrcDocumentProvider.java -- Java class OrcDocumentProvider
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

import org.eclipse.ui.editors.text.FileDocumentProvider;

/**
 * An Orc document provider maps between Orc source code files and documents.
 * Text editors use document providers to bridge the gap between their input
 * elements and the documents they work on. A single document provider may be
 * shared between multiple editors; the methods take the editors' input elements
 * as a parameter.
 *
 * @author jthywiss
 */
public class OrcDocumentProvider extends FileDocumentProvider {

    /**
     * Creates and returns a new document provider.
     */
    public OrcDocumentProvider() {
        super();
    }

    /**
     * Returns the default character encoding used by this provider.
     *
     * @return the default character encoding used by this provider
     */
    @Override
    public String getDefaultEncoding() {
        return "UTF-8"; //$NON-NLS-1$
    }

}
