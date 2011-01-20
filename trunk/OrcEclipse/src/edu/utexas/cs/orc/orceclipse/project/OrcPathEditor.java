//
// OrcPathEditor.java -- Java class OrcPathEditor
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Jan 19, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.project;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.ui.viewsupport.FilteredElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ArchiveFileFilter;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.FolderSelectionDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PathEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

import edu.utexas.cs.orc.orceclipse.Messages;

/**
 * 
 *
 * @author jthywiss
 */
@SuppressWarnings("restriction") //Using three JDT internal classes.
public class OrcPathEditor extends PathEditor {

	private static final String[] TYPE_DIALOG_BUTTON_LABELS = {Messages.OrcPathEditor_Folder, Messages.OrcPathEditor_JarFile, Messages.OrcPathEditor_ExternalFolder, Messages.OrcPathEditor_ExternalJarFile};
	private static final String WORKSPACE_PATH_PREFIX = "${workspace_loc}"; //$NON-NLS-1$
	private String pathDescriptionForDialogMessage;
	private String lastJarPath;

	/**
	 * Constructs an object of class OrcPathEditor.
	 *
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param pathDescriptionForDialogMessage text describing the path (used in UI after "add xxx to ")
     * @param parent the parent of the field editor's control
	 */
	public OrcPathEditor(String name, String labelText, String pathDescriptionForDialogMessage, Composite parent) {
		super(name, labelText, Messages.OrcPathEditor_ChooseFolder + pathDescriptionForDialogMessage, parent);
		this.pathDescriptionForDialogMessage = pathDescriptionForDialogMessage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PathEditor#getNewInputObject()
	 */
	@Override
	protected String getNewInputObject() {
		// propmpt dir or JAR
		MessageDialog typeDialog = new MessageDialog(getShell(), Messages.OrcPathEditor_TypeDialogTitle, null, Messages.OrcPathEditor_TypeDialogMessage1 + pathDescriptionForDialogMessage + Messages.OrcPathEditor_TypeDialogMessage2, MessageDialog.QUESTION, TYPE_DIALOG_BUTTON_LABELS, SWT.DEFAULT);
		switch (typeDialog.open()) {
		case 0: //workspace dir:
			return chooseWorkspaceFolder();
		case 1: //workspace JAR:
			return chooseWorkspaceJarFile();
		case 2: //external dir:
			String selectedFolder = super.getNewInputObject();
			if (selectedFolder != null) {
				return Path.fromOSString(selectedFolder).toPortableString();
			} else {
				return null;
			}
		case 3: //external JAR:
			return chooseExternalJarFile();
		default: //canceled
			return null;
		}
	}

	private String chooseWorkspaceFolder() {
		Class[] acceptedClasses = new Class[] { IProject.class, IFolder.class };

		ArrayList usedContainers = new ArrayList(getList().getItemCount());
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (String pathEntry : getList().getItems()) {
			if (pathEntry.startsWith(WORKSPACE_PATH_PREFIX))
				pathEntry = pathEntry.substring(WORKSPACE_PATH_PREFIX.length());
			IPath usedEntry = Path.fromPortableString(pathEntry);
			IResource resource = root.findMember(usedEntry);
			if (resource instanceof IContainer) {
				usedContainers.add(resource);
			}
		}

		Object[] used = usedContainers.toArray();

		FolderSelectionDialog dialog = new FolderSelectionDialog(getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		//dialog.setExisting(used);
		dialog.setTitle(Messages.OrcPathEditor_AddFolderTitle);
		dialog.setMessage(Messages.OrcPathEditor_AddFolderMessage1 + pathDescriptionForDialogMessage + Messages.OrcPathEditor_AddFolderMessage2);
		dialog.setHelpAvailable(false);
		dialog.addFilter(new TypedViewerFilter(acceptedClasses, used));
		dialog.setInput(root);
		dialog.setInitialSelection(null);

		if (dialog.open() == Window.OK) {
			return WORKSPACE_PATH_PREFIX + ((IResource) (dialog.getResult()[0])).getFullPath().toPortableString();
		}
		return null;
	}

	private String chooseWorkspaceJarFile() {
		Class[] acceptedClasses = new Class[] { IFile.class };
		TypedElementSelectionValidator validator = new TypedElementSelectionValidator(acceptedClasses, true);
		ArrayList usedJars = new ArrayList(getList().getItemCount());
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (String pathEntry : getList().getItems()) {
			if (pathEntry.startsWith(WORKSPACE_PATH_PREFIX))
				pathEntry = pathEntry.substring(WORKSPACE_PATH_PREFIX.length());
			IPath usedEntry = Path.fromPortableString(pathEntry);
			IResource resource = root.findMember(usedEntry);
			if (resource instanceof IFile) {
				usedJars.add(resource);
			}
		}

		FilteredElementTreeSelectionDialog dialog = new FilteredElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setHelpAvailable(false);
		dialog.setValidator(validator);
		dialog.setTitle(Messages.OrcPathEditor_JarFileDialogTitle);
		dialog.setMessage(Messages.OrcPathEditor_AddJarFileMessage1 + pathDescriptionForDialogMessage + Messages.OrcPathEditor_AddJarFileMessage2);
		dialog.setInitialFilter(ArchiveFileFilter.JARZIP_FILTER_STRING);
		dialog.addFilter(new ArchiveFileFilter(usedJars, true, true));
		dialog.setInput(root);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		dialog.setInitialSelection(null);
		dialog.setAllowMultiple(false);

		if (dialog.open() == Window.OK) {
			return WORKSPACE_PATH_PREFIX + ((IResource) (dialog.getResult()[0])).getFullPath().toPortableString();
		}
		return null;
	}

	private String chooseExternalJarFile() {
		FileDialog jarDialog = new FileDialog(getShell(), SWT.SHEET);
		if (lastJarPath != null) {
		    if (new File(lastJarPath).exists()) {
		    	jarDialog.setFilterPath(lastJarPath);
			}
		}
		jarDialog.setText(Messages.OrcPathEditor_JarFileDialogTitle); 
		jarDialog.setFilterExtensions(ArchiveFileFilter.ALL_ARCHIVES_FILTER_EXTENSIONS);
		jarDialog.setFilterPath(lastJarPath);
		String file = jarDialog.open();
		if (file != null) {
		    file = file.trim();
		    if (file.length() == 0) {
				return null;
			}
		    lastJarPath = file;
			return Path.fromOSString(file).toPortableString();
		} else {
			return null;
		}
	}

}
