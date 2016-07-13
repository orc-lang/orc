//
// OrcGeneralLaunchConfigurationTab.java -- Java class OrcGeneralLaunchConfigurationTab
// Project OrcEclipse
//
// Created by jthywiss on 04 Aug 2009.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;

import edu.utexas.cs.orc.orceclipse.Messages;
import edu.utexas.cs.orc.orceclipse.OrcConfigSettings;
import edu.utexas.cs.orc.orceclipse.OrcPlugin;
import edu.utexas.cs.orc.orceclipse.OrcPluginIds;
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

    private static Image ORC_PLUGIN_ICON_IMAGE = OrcPlugin.getInstance().getImageRegistry().get(OrcResources.ORC_PLUGIN_ICON);

    private Spinner stackDepthSpinner;
    private Spinner tokenLimitSpinner;
    private Spinner maxSiteThreadsSpinner;
    private Combo logLevelList;
    private Button dumpStackButton;
    private Button noTcoButton;
    private Button echoOilButton;

    protected static final String LOG_LEVELS[] = { "OFF", "SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST", "ALL" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$

    /**
     * Constructs an OrcGeneralLaunchConfigurationTab instance.
     */
    public OrcGeneralLaunchConfigurationTab() {
        super();
    }

    @Override
    public String getName() {
        return Messages.OrcGeneralLaunchConfigurationTab_GeneralTabName;
    }

    @Override
    public Image getImage() {
        return ORC_PLUGIN_ICON_IMAGE;
    }

    @Override
    public String getId() {
        return OrcPluginIds.LaunchConfigurationTab.ORC_GENERAL;
    }

    @Override
    public void setDefaults(final ILaunchConfigurationWorkingCopy configuration) {
        configuration.removeAttribute(OrcConfigSettings.LOG_LEVEL_ATTR_NAME);
        configuration.removeAttribute(OrcConfigSettings.PRELUDE_ATTR_NAME);
        configuration.removeAttribute(OrcConfigSettings.INCLUDE_PATH_ATTR_NAME);
        configuration.removeAttribute(OrcConfigSettings.ADDITIONAL_INCLUDES_ATTR_NAME);
        configuration.removeAttribute(OrcConfigSettings.TYPE_CHECK_ATTR_NAME);
        configuration.removeAttribute(OrcConfigSettings.RECURSION_CHECK_ATTR_NAME);
        configuration.removeAttribute(OrcConfigSettings.ECHO_OIL_ATTR_NAME);
        //configuration.removeAttribute(OrcConfigSettings.OIL_OUT_ATTR_NAME);
        configuration.removeAttribute(OrcConfigSettings.SITE_CLASSPATH_ATTR_NAME);
        configuration.removeAttribute(OrcConfigSettings.SHOW_JAVA_STACK_TRACE_ATTR_NAME);
        configuration.removeAttribute(OrcConfigSettings.NO_TCO_ATTR_NAME);
        configuration.removeAttribute(OrcConfigSettings.MAX_STACK_DEPTH_ATTR_NAME);
        configuration.removeAttribute(OrcConfigSettings.MAX_TOKENS_ATTR_NAME);
        configuration.removeAttribute(OrcConfigSettings.MAX_SITE_THREADS_ATTR_NAME);
        try {
            OrcLaunchDelegate.setDefaults(configuration);
        } catch (final CoreException e) {
            OrcPlugin.logAndShow(e);
        }
    }

    @Override
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

//        final ModifyListener ourModifyListener = new ModifyListener() {
//            public void modifyText(final ModifyEvent e) {
//                widgetModifiedAction(e);
//            }
//        };

        final Composite labelWidgetComp = SWTFactory.createComposite(comp, comp.getFont(), 2, 1, GridData.FILL_HORIZONTAL, 0, 0);

        SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_StackSizeLabel, 1);

        stackDepthSpinner = new Spinner(labelWidgetComp, SWT.NONE);
        stackDepthSpinner.setFont(parent.getFont());
        stackDepthSpinner.setValues(OrcConfigSettings.MAX_STACK_DEPTH_DEFAULT, 0, Integer.MAX_VALUE, 0, 1, 100);
        stackDepthSpinner.addSelectionListener(ourSelectionAdapter);

        SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_TokenLimitLabel, 1);

        tokenLimitSpinner = new Spinner(labelWidgetComp, SWT.NONE);
        tokenLimitSpinner.setFont(parent.getFont());
        tokenLimitSpinner.setValues(OrcConfigSettings.MAX_TOKENS_DEFAULT, 0, Integer.MAX_VALUE, 0, 1, 100);
        tokenLimitSpinner.addSelectionListener(ourSelectionAdapter);

        SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_MaxSiteThreadsLabel, 1);

        maxSiteThreadsSpinner = new Spinner(labelWidgetComp, SWT.NONE);
        maxSiteThreadsSpinner.setFont(parent.getFont());
        maxSiteThreadsSpinner.setValues(OrcConfigSettings.MAX_SITE_THREADS_DEFAULT, 0, Integer.MAX_VALUE, 0, 1, 100);
        maxSiteThreadsSpinner.addSelectionListener(ourSelectionAdapter);

        SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_LogLevelLabel, 1);

        logLevelList = new Combo(labelWidgetComp, SWT.READ_ONLY);
        logLevelList.setItems(LOG_LEVELS);
        logLevelList.setFont(parent.getFont());
        logLevelList.select(indexOfLevel(OrcConfigSettings.LOG_LEVEL_DEFAULT, LOG_LEVELS));
        logLevelList.addSelectionListener(ourSelectionAdapter);

        dumpStackButton = new Button(labelWidgetComp, SWT.CHECK);
        dumpStackButton.setFont(parent.getFont());
        dumpStackButton.setSelection(OrcConfigSettings.SHOW_JAVA_STACK_TRACE_DEFAULT);
        dumpStackButton.setText(Messages.OrcGeneralLaunchConfigurationTab_DumpStackLabel);
        dumpStackButton.addSelectionListener(ourSelectionAdapter);
        final GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = false;
        dumpStackButton.setLayoutData(gd);

        noTcoButton = new Button(labelWidgetComp, SWT.CHECK);
        noTcoButton.setFont(parent.getFont());
        noTcoButton.setSelection(OrcConfigSettings.NO_TCO_DEFAULT);
        noTcoButton.setText(Messages.OrcGeneralLaunchConfigurationTab_NoTcoLabel);
        noTcoButton.addSelectionListener(ourSelectionAdapter);
        final GridData gd3 = new GridData(GridData.FILL_HORIZONTAL);
        gd3.horizontalSpan = 2;
        gd3.grabExcessHorizontalSpace = false;
        noTcoButton.setLayoutData(gd3);

        echoOilButton = new Button(labelWidgetComp, SWT.CHECK);
        echoOilButton.setFont(parent.getFont());
        echoOilButton.setSelection(OrcConfigSettings.ECHO_OIL_DEFAULT);
        echoOilButton.setText(Messages.OrcGeneralLaunchConfigurationTab_EchoOilLabel);
        echoOilButton.addSelectionListener(ourSelectionAdapter);
        final GridData gd2 = new GridData(GridData.FILL_HORIZONTAL);
        gd2.horizontalSpan = 2;
        gd2.grabExcessHorizontalSpace = false;
        echoOilButton.setLayoutData(gd2);

        SWTFactory.createLabel(labelWidgetComp, Messages.OrcGeneralLaunchConfigurationTab_runtimeVersion + orcVersionText(), 2);
    }

    /**
     * @param lookup
     * @param logLevels
     * @return
     */
    private int indexOfLevel(final String lookup, final String[] logLevels) {
        for (int i = 0; i < logLevels.length; i++) {
            if (logLevels[i].equals(lookup)) {
                return i;
            }
        }
        return 0;
    }

    protected String orcVersionText() {
        return orc.Main.orcImplName() + " " + orc.Main.orcVersion() + "\n" + orc.Main.orcURL() + "\n" + orc.Main.orcCopyright(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    protected void widgetSelectedAction(final SelectionEvent e) {
        updateLaunchConfigurationDialog();
    }

    protected void widgetModifiedAction(final ModifyEvent e) {
        updateLaunchConfigurationDialog();
    }

    @Override
    public void initializeFrom(final ILaunchConfiguration configuration) {
        try {
            stackDepthSpinner.setSelection(configuration.getAttribute(OrcConfigSettings.MAX_STACK_DEPTH_ATTR_NAME, OrcConfigSettings.MAX_STACK_DEPTH_DEFAULT));
            tokenLimitSpinner.setSelection(configuration.getAttribute(OrcConfigSettings.MAX_TOKENS_ATTR_NAME, OrcConfigSettings.MAX_TOKENS_DEFAULT));
            maxSiteThreadsSpinner.setSelection(configuration.getAttribute(OrcConfigSettings.MAX_SITE_THREADS_ATTR_NAME, OrcConfigSettings.MAX_SITE_THREADS_DEFAULT));
            logLevelList.select(indexOfLevel(configuration.getAttribute(OrcConfigSettings.LOG_LEVEL_ATTR_NAME, OrcConfigSettings.LOG_LEVEL_DEFAULT), LOG_LEVELS));
            dumpStackButton.setSelection(configuration.getAttribute(OrcConfigSettings.SHOW_JAVA_STACK_TRACE_ATTR_NAME, OrcConfigSettings.SHOW_JAVA_STACK_TRACE_DEFAULT));
            noTcoButton.setSelection(configuration.getAttribute(OrcConfigSettings.NO_TCO_ATTR_NAME, OrcConfigSettings.NO_TCO_DEFAULT));
            echoOilButton.setSelection(configuration.getAttribute(OrcConfigSettings.ECHO_OIL_ATTR_NAME, OrcConfigSettings.ECHO_OIL_DEFAULT));
        } catch (final CoreException e) {
            OrcPlugin.logAndShow(e);
        }
    }

    @Override
    public boolean canSave() {
        return super.canSave();
    }

    @Override
    public void performApply(final ILaunchConfigurationWorkingCopy configuration) {
        setOrUnsetIntAttr(configuration, OrcConfigSettings.MAX_STACK_DEPTH_ATTR_NAME, stackDepthSpinner.getSelection(), OrcConfigSettings.MAX_STACK_DEPTH_DEFAULT);
        setOrUnsetIntAttr(configuration, OrcConfigSettings.MAX_TOKENS_ATTR_NAME, tokenLimitSpinner.getSelection(), OrcConfigSettings.MAX_TOKENS_DEFAULT);
        setOrUnsetIntAttr(configuration, OrcConfigSettings.MAX_SITE_THREADS_ATTR_NAME, maxSiteThreadsSpinner.getSelection(), OrcConfigSettings.MAX_SITE_THREADS_DEFAULT);
        setOrUnsetTextAttr(configuration, OrcConfigSettings.LOG_LEVEL_ATTR_NAME, logLevelList.getText());
        setOrUnsetBoolAttr(configuration, OrcConfigSettings.SHOW_JAVA_STACK_TRACE_ATTR_NAME, dumpStackButton.getSelection(), OrcConfigSettings.SHOW_JAVA_STACK_TRACE_DEFAULT);
        setOrUnsetBoolAttr(configuration, OrcConfigSettings.NO_TCO_ATTR_NAME, noTcoButton.getSelection(), OrcConfigSettings.NO_TCO_DEFAULT);
        setOrUnsetBoolAttr(configuration, OrcConfigSettings.ECHO_OIL_ATTR_NAME, echoOilButton.getSelection(), OrcConfigSettings.ECHO_OIL_DEFAULT);
    }

    private void setOrUnsetBoolAttr(final ILaunchConfigurationWorkingCopy configuration, final String attrName, final boolean enteredBool, final boolean defaultValue) {
        if (enteredBool != defaultValue) {
            configuration.setAttribute(attrName, enteredBool);
        } else {
            configuration.removeAttribute(attrName);
        }
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

    @Override
    public boolean isValid(final ILaunchConfiguration launchConfig) {
        return super.isValid(launchConfig);
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
