//
// OrcGeneralLaunchConfigurationTab.java -- Java class OrcGeneralLaunchConfigurationTab
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on 04 Aug 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.OrcResources;

/**
 * The "General" tab of the Orc launch configuration options user interface.
 * 
 * @author jthywiss
 * @see org.eclipse.debug.ui.ILaunchConfigurationTab
 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab
 */
public class OrcGeneralLaunchConfigurationTab extends AbstractLaunchConfigurationTab {

	private static Image ORC_PLUGIN_ICON_IMAGE = Activator.getInstance().getImageRegistry().get(OrcResources.ORC_PLUGIN_ICON);

	private Button typeCheckCheckButton;
	private Button noPreludeCheckButton;

	/**
	 * Constructs an OrcGeneralLaunchConfigurationTab instance.
	 */
	public OrcGeneralLaunchConfigurationTab() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return "General";
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		return ORC_PLUGIN_ICON_IMAGE;
	}

	/*
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
	 */
	@Override
	public String getId() {
		return "edu.utexas.cs.orc.orceclipse.launch.orcGeneralTab";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(final ILaunchConfigurationWorkingCopy configuration) {
		OrcLaunchDelegate.setDefaults(configuration);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(final Composite parent) {
		final Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		comp.setLayout(new GridLayout());

		typeCheckCheckButton = createCheckButton(comp, "Type check");
		typeCheckCheckButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				widgetSelectedAction(e);
			}
		});

		noPreludeCheckButton = createCheckButton(comp, "Do not include standard prelude");
		noPreludeCheckButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				widgetSelectedAction(e);
			}
		});

		// TODO: Finish adding controls for the remaining attributes
	}

	protected void widgetSelectedAction(final SelectionEvent e) {
		updateLaunchConfigurationDialog();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(final ILaunchConfiguration configuration) {
		try {
			typeCheckCheckButton.setSelection(configuration.getAttribute(OrcLaunchDelegate.TYPE_CHECK_ATTR_NAME, OrcLaunchDelegate.TYPE_CHECK_ATTR_DEFAULT));
			noPreludeCheckButton.setSelection(configuration.getAttribute(OrcLaunchDelegate.NO_PRELUDE_ATTR_NAME, OrcLaunchDelegate.NO_PRELUDE_ATTR_DEFAULT));
			// TODO: Finish initializing controls for the remaining attributes
		} catch (final CoreException e) {
			Activator.log(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#canSave()
	 */
	@Override
	public boolean canSave() {
		return super.canSave();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(final ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(OrcLaunchDelegate.TYPE_CHECK_ATTR_NAME, typeCheckCheckButton.getSelection());
		configuration.setAttribute(OrcLaunchDelegate.NO_PRELUDE_ATTR_NAME, noPreludeCheckButton.getSelection());
		// TODO: Finish setting the remaining attributes
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public boolean isValid(final ILaunchConfiguration launchConfig) {
		return super.isValid(launchConfig);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
	}
}
