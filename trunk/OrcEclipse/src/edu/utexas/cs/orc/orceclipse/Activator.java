//
// Activator.java -- Java class Activator
// Project OrcEclipse
//
// $Id: Activator.java 1230 2009-08-18 14:58:16Z jthywissen $
//
// Created by jthywiss on Jul 27, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.imp.runtime.PluginBase;
import org.osgi.framework.BundleContext;

/**
 * Orc plug-in runtime class, represents the entire plug-in.
 * <p>
 * Contains a number of utility methods. This class is a singleton.
 * 
 * @author jthywiss
 */
public class Activator extends PluginBase {

	// These must correspond to the values in plugin.xml
	private static final String pluginID = "edu.utexas.cs.orc.orceclipse";
	private static final String languageName = "Orc";

	/**
	 * The unique instance of this plugin class
	 */
	protected static Activator pluginInstance;

	/**
	 * @return The unique instance of this plugin class
	 */
	public static Activator getInstance() {
		if (pluginInstance == null) {
			pluginInstance = new Activator();
		}
		return pluginInstance;
	}

	/**
	 * This class is a singleton -- call getInstance, not this constructor.
	 */
	public Activator() {
		super();
		pluginInstance = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.imp.runtime.PluginBase#getID()
	 */
	@Override
	public String getID() {
		return pluginID;
	}

	/*
	 * (non-Javadoc) 
	 * This is really language NAME, not ID -- PluginBase named the method incorrectly.
	 * @see org.eclipse.imp.runtime.PluginBase#getLanguageID()
	 */
	@Override
	public String getLanguageID() {
		return languageName;
	}

	/**
	 * Logs the specified status with this plug-in's log.
	 * 
	 * @param status status to log
	 */
	public static void log(final IStatus status) {
		getInstance().getLog().log(status);
	}

	/**
	 * Logs an internal error with the specified message.
	 * 
	 * @param message the error message to log
	 */
	public static void logErrorMessage(final String message) {
		getInstance().writeErrorMsg(message);
	}

	/**
	 * Logs an internal error with the specified throwable
	 * 
	 * @param e the exception to be logged
	 */
	public static void log(final Throwable e) {
		if (e instanceof CoreException) {
			getInstance().logException(e.getMessage(), e.getCause());
		} else {
			getInstance().logException("Internal Error", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse
	 * .jface.resource.ImageRegistry)
	 */
	@Override
	protected void initializeImageRegistry(final org.eclipse.jface.resource.ImageRegistry reg) {
		final org.osgi.framework.Bundle bundle = getBundle();
		OrcResources.initializeImageRegistry(bundle, reg);
	}

	/**
	 * @param file java.io.File to convert to an Eclipse IFile
	 * @return org.eclipse.core.resources.IFile converted from Java File
	 * @see java.io.File
	 * @see org.eclipse.core.resources.IFile
	 */
	public static IFile iFileFromFile(final File file) {
		// FIXME: This is broken for absolute paths!
		return ResourcesPlugin.getWorkspace().getRoot().getFile(Path.fromOSString(file.getPath()));
	}

	/**
	 * Reads the entire contents of a file into a String
	 * 
	 * @param file IFile to read contents from
	 * @return String containing contents of file
	 * @throws CoreException If an InputStream cannot be created for
	 *             <code>file</code>
	 * @throws IOException If an I/O error occurs
	 */
	public static String stringFromFileContents(final IFile file) throws CoreException, IOException {
		final InputStream fileStream = file.getContents();
		final InputStreamReader fileReader = new InputStreamReader(fileStream, file.getCharset(true));
		final char fileContentsBuffer[] = new char[fileStream.available()];
		final int fileSize = fileReader.read(fileContentsBuffer);
		return new String(fileContentsBuffer, 0, fileSize);
	}
}
