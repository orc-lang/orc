//
// OrcInformationProvider.java -- Java class OrcInformationProvider
// Project OrcEclipse
//
// Created by jthywiss on Jul 10, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.IInformationProviderExtension2;

/**
 * Returns an object to be used in response to the source editor's
 * "Show Information" (also known as "Show Tooltip Description") command. This
 * command shows information about the document element at the cursor. For
 * example, the JDT's editor shows the declaration and Javadoc for the element.
 *
 * @author jthywiss
 */
public class OrcInformationProvider implements IInformationProvider, IInformationProviderExtension, IInformationProviderExtension2 {

    /**
     * Constructs an object of class OrcInformationProvider.
     */
    public OrcInformationProvider() {
    }

    /**
     * Returns the information control creator of this information provider.
     *
     * @return the information control creator or <code>null</code> if none is
     *         available
     */
    @Override
    public IInformationControlCreator getInformationPresenterControlCreator() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Returns the element for the given subject or <code>null</code> if no
     * element is available.
     *
     * @param textViewer the viewer in whose document the subject is contained
     * @param subject the text region constituting the information subject
     * @return the element for the subject
     */
    @Override
    public Object getInformation2(final ITextViewer textViewer, final IRegion subject) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Returns the region of the text viewer's document close to the given
     * offset that contains a subject about which information can be provided.
     * <p>
     * For example, if information can be provided on a per code block basis,
     * the offset should be used to find the enclosing code block and the source
     * range of the block should be returned.
     *
     * @param textViewer the text viewer in which information has been requested
     * @param offset the offset at which information has been requested
     * @return the region of the text viewer's document containing the
     *         information subject
     */
    @Override
    public IRegion getSubject(final ITextViewer textViewer, final int offset) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Returns the information about the given subject or <code>null</code> if
     * no information is available.
     * <p>
     * Callers should ignore the text returned by this, and use
     * {@link #getInformation2(ITextViewer, IRegion)} instead.
     * </p>
     * <p>
     * This implementation just returns
     * <code>getInformation2(textViewer, subject).toString()</code>.
     * </p>
     *
     * @param textViewer the viewer in whose document the subject is contained
     * @param subject the text region constituting the information subject
     * @return the information about the subject
     * @deprecated Replaced by {@link #getInformation2(ITextViewer, IRegion)}
     */
    @Deprecated
    @Override
    public String getInformation(final ITextViewer textViewer, final IRegion subject) {
        return getInformation2(textViewer, subject).toString();
    }

}
