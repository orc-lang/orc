//
// OrcPerspectiveFactory.java -- Java class OrcPerspectiveFactory
// Project OrcEclipse
//
// Created by jthywiss on Aug 5, 2009.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * The Orc Perspective factory.
 * <p>
 * A perspective factory generates the initial page layout and visible action
 * set for a page.
 * <p>
 * When a new page is created in the workbench a perspective is used to define
 * the initial page layout.
 * <p>
 * This perspective is wholly defined by the <code>plugin.xml</code> file, but a
 * class is required.
 *
 * @author jthywiss
 */
public class OrcPerspectiveFactory implements IPerspectiveFactory {

    @Override
    public void createInitialLayout(final IPageLayout layout) {
        addViews(layout);
        addActionSets(layout);
        addNewWizardShortcuts(layout);
        addPerspectiveShortcuts(layout);
        addViewShortcuts(layout);
    }

    private void addViews(final IPageLayout layout) {
        // defined in the org.eclipse.ui.perspectiveExtensions extension in
        // plugin.xml creates the overall folder layout.
        // Note that each new Folder uses a percentage of the remaining
        // EditorArea.
    }

    private void addActionSets(final IPageLayout layout) {
        // defined in the org.eclipse.ui.perspectiveExtensions extension in
        // plugin.xml
    }

    private void addPerspectiveShortcuts(final IPageLayout layout) {
        // defined in the org.eclipse.ui.perspectiveExtensions extension in
        // plugin.xml
    }

    private void addNewWizardShortcuts(final IPageLayout layout) {
        // defined in the org.eclipse.ui.perspectiveExtensions extension in
        // plugin.xml
    }

    private void addViewShortcuts(final IPageLayout layout) {
        // defined in the org.eclipse.ui.perspectiveExtensions extension in
        // plugin.xml
    }
}
