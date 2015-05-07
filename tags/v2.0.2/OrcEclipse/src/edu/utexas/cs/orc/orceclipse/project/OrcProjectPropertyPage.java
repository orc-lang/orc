//
// OrcProjectPropertyPage.java -- Java class OrcProjectPropertyPage
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Sep 6, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.Messages;
import edu.utexas.cs.orc.orceclipse.OrcConfigSettings;

/**
 * Property page for editing Orc project properties.
 *
 * @see org.eclipse.ui.IWorkbenchPropertyPage
 * @author jthywiss
 */
public class OrcProjectPropertyPage extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {
	private IProject project;

	/**
	 * Constructs an object of class OrcProjectPropertyPage.
	 *
	 */
	public OrcProjectPropertyPage() {
		super();
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPropertyPage#getElement()
	 */
	@Override
	public IAdaptable getElement() {
		return project;
	}

	/**
	 * Sets the element that owns properties shown on this page.
	 * <p>
	 * In the case of <code>OrcProjectPropertyPage</code>, this must be a project.
	 * A <code>ClassCastException</code> will result from setting an object that
	 * does not implement {@link IProject}.
	 * 
	 * @param element the project
	 */
	@Override
	public void setElement(final IAdaptable element) {
		this.project = (IProject) element;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(OrcConfigSettings.TYPE_CHECK_ATTR_NAME, Messages.OrcProjectPropertyPage_TypeCheckLabel, BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor(OrcConfigSettings.RECURSION_CHECK_ATTR_NAME, Messages.OrcProjectPropertyPage_RecursionCheckLabel, BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor(OrcConfigSettings.PRELUDE_ATTR_NAME, Messages.OrcProjectPropertyPage_UseStdPreludeLabel, BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new OrcPathEditor(OrcConfigSettings.INCLUDE_PATH_ATTR_NAME, Messages.OrcProjectPropertyPage_IncludePathLabel, Messages.OrcProjectPropertyPage_IncludePathDescription, getFieldEditorParent()));
		addField(new OrcPathEditor(OrcConfigSettings.SITE_CLASSPATH_ATTR_NAME, Messages.OrcProjectPropertyPage_SiteClassPathLabel, Messages.OrcProjectPropertyPage_SitePathDescription, getFieldEditorParent()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
	 */
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return new ScopedPreferenceStore(new ProjectScope(project), Activator.getInstance().getLanguageID());
	}
}
