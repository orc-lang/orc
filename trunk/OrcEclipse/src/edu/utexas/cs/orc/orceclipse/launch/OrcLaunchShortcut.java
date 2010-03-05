//
// OrcLaunchShortcut.java -- Java class OrcLaunchShortcut
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 5, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.launch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.Messages;

/**
 * Launch shortcut for Orc programs.
 * <p>
 * A launch shortcut is capable of launching a selection
 * or active editor in the workbench. The delegate is responsible for
 * interpreting the selection or active editor (if it applies), and launching
 * an application. This may require creating a new launch configuration
 * with default values, or re-using an existing launch configuration.
 * <p>
 * A launch shortcut is defined as an extension
 * of type <code>org.eclipse.debug.ui.launchShortcuts</code>.
 * A shortcut specifies the perspectives in which is should be available
 * from the "Run/Debug" cascade menus.
 *
 * @author jthywiss
 */
public class OrcLaunchShortcut implements ILaunchShortcut {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection, java.lang.String)
	 */
	public void launch(final ISelection selection, final String mode) {
		// Make sure the selection is the active resource, so the launch delegate knows who to launch.
		//Activator.getInstance().getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(part);

		try {
			final IFile file = (IFile) ((IAdaptable) ((IStructuredSelection) selection).getFirstElement()).getAdapter(IFile.class);
			launch(file, mode);
		} catch (final ClassCastException e) {
			// Ignore -- got something not launchable
			Activator.logErrorMessage("OrcLaunchShortcut.launch(ISelection,String): Got a selection that wasn't an IStructuredSelection with one element that is an IFile. selection=" + selection); //$NON-NLS-1$
			ErrorDialog.openError(Display.getCurrent().getActiveShell(), Messages.OrcLaunchShortcut_UnableToLaunchTitle, Messages.OrcLaunchShortcut_UnableToLaunchMessage, null);
		} catch (final NullPointerException e) {
			// Ignore -- got nothing
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart, java.lang.String)
	 */
	public void launch(final IEditorPart editor, final String mode) {
		launch((IFile) editor.getAdapter(IFile.class), mode);
	}

	/**
	 * @param file Orc file to launch
	 * @param mode Launch mode to use (run, debug, profile, etc.)
	 */
	public void launch(final IFile file, final String mode) {
		ILaunchConfiguration config = findLaunchConfiguration(file, mode);
		if (config == null) {
			config = createConfiguration(file, mode);
		}
		if (config != null) {
			DebugUITools.launch(config, mode);
		}
	}

	/**
	 * @param file
	 * @param mode
	 * @return
	 */
	protected ILaunchConfiguration findLaunchConfiguration(final IFile file, final String mode) {
		List candidateConfigs = Collections.EMPTY_LIST;
		try {
			final ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(OrcLaunchDelegate.getLaunchConfigType());
			candidateConfigs = new ArrayList(configs.length);
			for (final ILaunchConfiguration config : configs) {
				// FUTURE: If needed, filter here.
				candidateConfigs.add(config);
			}
		} catch (final CoreException e) {
			Activator.log(e);
		}
		final int candidateCount = candidateConfigs.size();
		if (candidateCount == 1) {
			return (ILaunchConfiguration) candidateConfigs.get(0);
		} else if (candidateCount > 1) {
			return chooseConfiguration(candidateConfigs);
		}
		return null;
	}

	/**
	 * @param configList
	 * @return
	 */
	protected ILaunchConfiguration chooseConfiguration(final List configList) {
		final IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		final ElementListSelectionDialog dialog = new ElementListSelectionDialog(Display.getCurrent().getActiveShell(), labelProvider);
		dialog.setElements(configList.toArray());
		dialog.setTitle(Messages.OrcLaunchShortcut_SelectLaunchConfigTitle);
		dialog.setMessage(Messages.OrcLaunchShortcut_SelectLaunchConfigMessage);
		dialog.setMultipleSelection(false);
		final int result = dialog.open();
		labelProvider.dispose();
		if (result == Window.OK) {
			return (ILaunchConfiguration) dialog.getFirstResult();
		}
		return null;
	}

	/**
	 * @param file
	 * @param mode
	 * @return
	 */
	protected ILaunchConfiguration createConfiguration(final IFile file, final String mode) {
		ILaunchConfiguration config = null;
		ILaunchConfigurationWorkingCopy wc = null;
		try {
			final ILaunchConfigurationType configType = OrcLaunchDelegate.getLaunchConfigType();
			wc = configType.newInstance(null, DebugPlugin.getDefault().getLaunchManager().generateUniqueLaunchConfigurationNameFrom(Messages.OrcLaunchShortcut_OrcProgramLaunchConfigName));
			OrcLaunchDelegate.setDefaults(wc);
			config = wc.doSave();
		} catch (final CoreException e) {
			Activator.log(e);
		}
		return config;
	}
}
