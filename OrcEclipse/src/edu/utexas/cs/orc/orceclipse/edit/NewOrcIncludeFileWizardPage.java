//
// NewOrcIncludeFileWizardPage.java -- Java class NewOrcIncludeFileWizardPage
// Project OrcEclipse
//
// Created by jthywiss on Mar 28, 2019.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

import edu.utexas.cs.orc.orceclipse.Messages;

/**
 * The single page of the New Orc Include File UI "wizard". Instantiated by
 * NewOrcIncludeFileWizard.
 *
 * @author jthywiss
 */
public class NewOrcIncludeFileWizardPage extends WizardNewFileCreationPage {

    /** This wizard page's name (id) */
    public static final String PAGE_NAME = "newFilePage1"; //$NON-NLS-1$

    /**
     * Constructs an object of class NewOrcIncludeFileWizardPage.
     *
     * @param selection the resources currently selected
     */
    public NewOrcIncludeFileWizardPage(final IStructuredSelection selection) {
        super(PAGE_NAME, selection);
        setTitle(Messages.NewOrcIncludeFileWizardPage_Title);
        setDescription(Messages.NewOrcIncludeFileWizardPage_Descrption);
        setFileExtension("inc"); //$NON-NLS-1$
    }

    @Override
    protected InputStream getInitialContents() {
        //TODO: Use a user-config'ed template, with variables.
        final String contents = "{- " + getFileName() + " -- Orc include file " + getFileName().replaceFirst("\\.inc$", "") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                " -\n" + //$NON-NLS-1$
                " - Created by " + System.getProperty("user.name") + " on " + DateFormat.getDateTimeInstance().format(new Date()) + "\n" + //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                " -}\n\n"; //$NON-NLS-1$
        return new ByteArrayInputStream(contents.getBytes());
    }

}
