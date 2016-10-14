//
// NewOrcFileWizardPage.java -- Java class NewOrcFileWizardPage
// Project OrcEclipse
//
// Created by jthywiss on Feb 26, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
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
 * The single page of the New Orc File UI "wizard". Instantiated by
 * NewOrcFileWizard.
 *
 * @author jthywiss
 */
public class NewOrcFileWizardPage extends WizardNewFileCreationPage {

    /**
     * Constructs an object of class NewOrcFileWizardPage.
     *
     * @param pageName name of new page
     * @param selection the resources currently selected
     */
    public NewOrcFileWizardPage(final String pageName, final IStructuredSelection selection) {
        super(pageName, selection);
        setTitle(Messages.NewOrcFileWizardPage_Title);
        setDescription(Messages.NewOrcFileWizardPage_Descrption);
        setFileExtension("orc"); //$NON-NLS-1$
    }

    @Override
    protected InputStream getInitialContents() {
        //TODO: Use a user-config'ed template, with variables.
        final String contents = "{- " + getFileName() + " -- Orc program " + getFileName().replaceFirst("\\.orc$", "") + "\n" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                " -\n" + //$NON-NLS-1$
                " - Created by " + System.getProperty("user.name") + " on " + DateFormat.getDateTimeInstance().format(new Date()) + "\n" + //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                " -}\n\n"; //$NON-NLS-1$
        return new ByteArrayInputStream(contents.getBytes());
    }

}
