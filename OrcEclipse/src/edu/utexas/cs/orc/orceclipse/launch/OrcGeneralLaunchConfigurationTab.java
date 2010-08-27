//
// OrcGeneralLaunchConfigurationTab.java -- Java class OrcGeneralLaunchConfigurationTab
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on 04 Aug 2009.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;

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
	private Spinner tokenLimitSpinner;
	private Spinner stackDepthSpinner;
	//private Spinner numSiteThreadsSpinner;
	//private Text traceOutFilenameText;
	private Combo logLevelList;

	protected static final String LOG_LEVELS[] = {"OFF", "SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST", "ALL"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$

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
		configuration.removeAttribute(OrcConfigSettings.MAX_PUBS_ATTR_NAME);
		//configuration.removeAttribute(OrcConfigSettings.NUM_SITE_THREADS_ATTR_NAME);
		//configuration.removeAttribute(OrcConfigSettings.TRACE_OUT_ATTR_NAME);
		configuration.removeAttribute(OrcConfigSettings.LOG_LEVEL_ATTR_NAME);
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

		SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_TokenLimitLabel, 1);

		tokenLimitSpinner = new Spinner(labelWidgetComp, SWT.NONE);
		tokenLimitSpinner.setFont(parent.getFont());
		tokenLimitSpinner.setValues(OrcConfigSettings.TOKEN_LIMIT_DEFAULT, 0, Integer.MAX_VALUE, 0, 1, 100);
		tokenLimitSpinner.addSelectionListener(ourSelectionAdapter);

		SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_StackSizeLabel, 1);

		stackDepthSpinner = new Spinner(labelWidgetComp, SWT.NONE);
		stackDepthSpinner.setFont(parent.getFont());
		stackDepthSpinner.setValues(OrcConfigSettings.MAX_STACK_DEPTH_DEFAULT, 0, Integer.MAX_VALUE, 0, 1, 100);
		stackDepthSpinner.addSelectionListener(ourSelectionAdapter);

		//SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_NumSiteThreadsLabel, 1);
		//
		//numSiteThreadsSpinner = new Spinner(labelWidgetComp, SWT.NONE);
		//numSiteThreadsSpinner.setFont(parent.getFont());
		//numSiteThreadsSpinner.setValues(OrcConfigSettings.NUM_SITE_THREADS_DEFAULT, 1, Integer.MAX_VALUE, 0, 1, 10);
		//numSiteThreadsSpinner.addSelectionListener(ourSelectionAdapter);

		//SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_TraceOutFilenameLabel, 1);
		//
		//traceOutFilenameText = SWTFactory.createSingleText(labelWidgetComp, 1);
		//traceOutFilenameText.addModifyListener(ourModifyListener);

		SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_LogLevelLabel, 1);

		logLevelList = new Combo(labelWidgetComp, SWT.READ_ONLY);
		logLevelList.setItems(LOG_LEVELS);
		logLevelList.setFont(parent.getFont());
		logLevelList.select(indexOfLevel(OrcConfigSettings.LOG_LEVEL_DEFAULT, LOG_LEVELS));
		logLevelList.addSelectionListener(ourSelectionAdapter);

		SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_runtimeVersion + orcVersionText(), 2);
	}

	/**
	 * @param lookup
	 * @param logLevels
	 * @return
	 */
	private int indexOfLevel(String lookup, String[] logLevels) {
		for (int i = 0; i < logLevels.length; i++) {
			if (logLevels[i].equals(lookup)) return i;
		}
		return 0;
	}

	protected String orcVersionText() {
		return orc.Main.orcImplName()+" "+orc.Main.orcVersion()+"\n"+orc.Main.orcURL()+"\n"+(orc.Main.orcCopyright()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
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
			tokenLimitSpinner.setSelection(configuration.getAttribute(OrcConfigSettings.TOKEN_LIMIT_ATTR_NAME, OrcConfigSettings.TOKEN_LIMIT_DEFAULT));
			stackDepthSpinner.setSelection(configuration.getAttribute(OrcConfigSettings.MAX_STACK_DEPTH_ATTR_NAME, OrcConfigSettings.MAX_STACK_DEPTH_DEFAULT));
			//numSiteThreadsSpinner.setSelection(configuration.getAttribute(OrcConfigSettings.NUM_SITE_THREADS_ATTR_NAME, OrcConfigSettings.NUM_SITE_THREADS_DEFAULT));
			//traceOutFilenameText.setText(configuration.getAttribute(OrcConfigSettings.TRACE_OUT_ATTR_NAME, OrcConfigSettings.TRACE_OUT_DEFAULT));
			logLevelList.select(indexOfLevel(configuration.getAttribute(OrcConfigSettings.LOG_LEVEL_ATTR_NAME, OrcConfigSettings.LOG_LEVEL_DEFAULT), LOG_LEVELS));
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
		setOrUnsetIntAttr(configuration, OrcConfigSettings.TOKEN_LIMIT_ATTR_NAME, tokenLimitSpinner.getSelection(), OrcConfigSettings.MAX_PUBS_DEFAULT);
		setOrUnsetIntAttr(configuration, OrcConfigSettings.MAX_STACK_DEPTH_ATTR_NAME, stackDepthSpinner.getSelection(), OrcConfigSettings.MAX_PUBS_DEFAULT);
		//setOrUnsetIntAttr(configuration, OrcConfigSettings.NUM_SITE_THREADS_ATTR_NAME, numSiteThreadsSpinner.getSelection(), OrcConfigSettings.NUM_SITE_THREADS_DEFAULT);
		//setOrUnsetTextAttr(configuration, OrcConfigSettings.TRACE_OUT_ATTR_NAME, traceOutFilenameText.getText());
		setOrUnsetTextAttr(configuration, OrcConfigSettings.LOG_LEVEL_ATTR_NAME, logLevelList.getText());
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
