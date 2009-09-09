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
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.Messages;
import edu.utexas.cs.orc.orceclipse.OrcConfigSettings;
import edu.utexas.cs.orc.orceclipse.OrcResources;

/**
 * The "General" tab of the Orc launch configuration options user interface.
 * 
 * @author jthywiss
 * @see org.eclipse.debug.ui.ILaunchConfigurationTab
 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab
 */
@SuppressWarnings("restriction")
public class OrcGeneralLaunchConfigurationTab extends AbstractLaunchConfigurationTab {

	private static Image ORC_PLUGIN_ICON_IMAGE = Activator.getInstance().getImageRegistry().get(OrcResources.ORC_PLUGIN_ICON);

	private Spinner maxPubsSpinner;
	private Spinner numSiteThreadsSpinner;
	private Text traceOutFilenameText;
	private Spinner debugLevelSpinner;

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
		return Messages.OrcGeneralLaunchConfigurationTab_GeneralTabName;
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
		return "edu.utexas.cs.orc.orceclipse.launch.orcGeneralTab"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(final ILaunchConfigurationWorkingCopy configuration) {
		configuration.removeAttribute(OrcConfigSettings.TYPE_CHECK_ATTR_NAME);
		configuration.removeAttribute(OrcConfigSettings.NO_PRELUDE_ATTR_NAME);
		configuration.removeAttribute(OrcConfigSettings.INCLUDE_PATH_ATTR_NAME);
		configuration.removeAttribute(OrcConfigSettings.SITE_CLASSPATH_ATTR_NAME);
		configuration.removeAttribute(OrcConfigSettings.OIL_OUT_ATTR_NAME);
		configuration.removeAttribute(OrcConfigSettings.MAX_PUBS_ATTR_NAME);
		configuration.removeAttribute(OrcConfigSettings.NUM_SITE_THREADS_ATTR_NAME);
		configuration.removeAttribute(OrcConfigSettings.TRACE_OUT_ATTR_NAME);
		configuration.removeAttribute(OrcConfigSettings.DEBUG_LEVEL_ATTR_NAME);
		OrcLaunchDelegate.setDefaults(configuration);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(final Composite parent) {
		final Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		comp.setLayout(new GridLayout());
		final SelectionAdapter ourSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				widgetSelectedAction(e);
			}
		};
		final ModifyListener ourModifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				widgetModifiedAction(e);
			}
		};

		final Composite labelWidgetComp = SWTFactory.createComposite(comp, comp.getFont(), 2, 1, GridData.FILL_HORIZONTAL, 0, 0);

		SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_MaxPubsLabel, 1);

		maxPubsSpinner = new Spinner(labelWidgetComp, SWT.NONE);
		maxPubsSpinner.setFont(parent.getFont());
		maxPubsSpinner.setValues(OrcConfigSettings.MAX_PUBS_DEFAULT, 0, Integer.MAX_VALUE, 0, 1, 100);
		maxPubsSpinner.addSelectionListener(ourSelectionAdapter);

		SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_NumSiteThreadsLabel, 1);

		numSiteThreadsSpinner = new Spinner(labelWidgetComp, SWT.NONE);
		numSiteThreadsSpinner.setFont(parent.getFont());
		numSiteThreadsSpinner.setValues(OrcConfigSettings.NUM_SITE_THREADS_DEFAULT, 1, Integer.MAX_VALUE, 0, 1, 10);
		numSiteThreadsSpinner.addSelectionListener(ourSelectionAdapter);

		SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_TraceOutFilenameLabel, 1);

		traceOutFilenameText = SWTFactory.createSingleText(labelWidgetComp, 1);
		traceOutFilenameText.addModifyListener(ourModifyListener);

		SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_DebugLevelLabel, 1);

		debugLevelSpinner = new Spinner(labelWidgetComp, SWT.NONE);
		debugLevelSpinner.setFont(parent.getFont());
		debugLevelSpinner.setValues(OrcConfigSettings.DEBUG_LEVEL_DEFAULT, 0, 4, 0, 1, 1);
		debugLevelSpinner.addSelectionListener(ourSelectionAdapter);
	}

	protected void widgetSelectedAction(final SelectionEvent e) {
		updateLaunchConfigurationDialog();
	}

	protected void widgetModifiedAction(final ModifyEvent e) {
		updateLaunchConfigurationDialog();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(final ILaunchConfiguration configuration) {
		try {
			maxPubsSpinner.setSelection(configuration.getAttribute(OrcConfigSettings.MAX_PUBS_ATTR_NAME, OrcConfigSettings.MAX_PUBS_DEFAULT));
			numSiteThreadsSpinner.setSelection(configuration.getAttribute(OrcConfigSettings.NUM_SITE_THREADS_ATTR_NAME, OrcConfigSettings.NUM_SITE_THREADS_DEFAULT));
			traceOutFilenameText.setText(configuration.getAttribute(OrcConfigSettings.TRACE_OUT_ATTR_NAME, OrcConfigSettings.TRACE_OUT_DEFAULT));
			debugLevelSpinner.setSelection(configuration.getAttribute(OrcConfigSettings.DEBUG_LEVEL_ATTR_NAME, OrcConfigSettings.DEBUG_LEVEL_DEFAULT));
		} catch (final CoreException e) {
			Activator.logAndShow(e);
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
		setOrUnsetIntAttr(configuration, OrcConfigSettings.MAX_PUBS_ATTR_NAME, maxPubsSpinner.getSelection(), OrcConfigSettings.MAX_PUBS_DEFAULT);
		setOrUnsetIntAttr(configuration, OrcConfigSettings.NUM_SITE_THREADS_ATTR_NAME, numSiteThreadsSpinner.getSelection(), OrcConfigSettings.NUM_SITE_THREADS_DEFAULT);
		setOrUnsetTextAttr(configuration, OrcConfigSettings.TRACE_OUT_ATTR_NAME, traceOutFilenameText.getText());
		setOrUnsetIntAttr(configuration, OrcConfigSettings.DEBUG_LEVEL_ATTR_NAME, debugLevelSpinner.getSelection(), OrcConfigSettings.DEBUG_LEVEL_DEFAULT);
	}

	private void setOrUnsetIntAttr(final ILaunchConfigurationWorkingCopy configuration, final String attrName, final int enteredNum, final int defaultValue) {
		if (enteredNum != defaultValue) {
			configuration.setAttribute(attrName, enteredNum);
		} else {
			configuration.removeAttribute(attrName);
		}
	}

	private void setOrUnsetTextAttr(final ILaunchConfigurationWorkingCopy configuration, final String attrName, final String enteredText) {
		if (enteredText != null && enteredText.length() > 0) {
			configuration.setAttribute(attrName, enteredText);
		} else {
			configuration.removeAttribute(attrName);
		}
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
