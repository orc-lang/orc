//
// OrcLaunchConfigurationTabGroup.java -- Java class OrcLaunchConfigurationTabGroup
// Project OrcEclipse
//
// Created by jthywiss on Aug 4, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/**
 * The group of tabs comprising the Orc launch configuration options user
 * interface.
 *
 * @author jthywiss
 */
public class OrcLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    @Override
    public void createTabs(final ILaunchConfigurationDialog dialog, final String mode) {
        final ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { new OrcGeneralLaunchConfigurationTab(),
                //new JavaMainTab(),
                //new JavaArgumentsTab(),
                //new JavaJRETab(),
                new OrcRuntimeClasspathTab(),
                //new SourceLookupTab(),
                //new EnvironmentTab(),
                new CommonTab(), };
        setTabs(tabs);
    }

}
