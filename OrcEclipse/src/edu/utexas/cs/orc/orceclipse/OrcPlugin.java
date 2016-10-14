//
// OrcPlugin.java -- Java class OrcPlugin
// Project OrcEclipse
//
// Created by jthywiss on Jul 27, 2009.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;

import org.osgi.framework.BundleContext;

/**
 * Orc plug-in runtime class, represents the entire plug-in.
 * <p>
 * Contains a number of utility methods. This class is a singleton.
 *
 * @author jthywiss
 */
public class OrcPlugin extends AbstractUIPlugin {

    // These must correspond to the values in plugin.xml
    private static final String pluginID = "edu.utexas.cs.orc.orceclipse"; //$NON-NLS-1$

    /**
     * The unique instance of this plugin class
     */
    protected static OrcPlugin pluginInstance;

    /**
     * @return The unique instance of this plugin class
     */
    public static OrcPlugin getInstance() {
        if (pluginInstance == null) {
            pluginInstance = new OrcPlugin();
        }
        return pluginInstance;
    }

    /**
     * This class is a singleton -- call getInstance, not this constructor.
     */
    public OrcPlugin() {
        super();
        pluginInstance = this;
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        OrcConfigSettings.initDefaultPrefs();
    }

    /**
     * Get the plug-in ID, which is the OSGi Bundle-SymbolicName
     *
     * @return the plugin ID string
     */
    public static String getID() {
        return pluginID;
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
     * Logs an internal error with the specified throwable
     *
     * @param e the exception to be logged
     */
    public static void log(final Throwable e) {
        final String msg = e instanceof CoreException ? e.getMessage() : Messages.Activator_InternalError;
        log(new Status(IStatus.ERROR, getID(), 0, msg, e));
    }

    /**
     * Logs an internal error with the specified throwable
     *
     * @param e the exception to be logged
     */
    public static void logAndShow(final Throwable e) {
        log(e);
        String msg = Messages.Activator_InternalError;
        if (e instanceof CoreException) {
            msg = e.getLocalizedMessage();
        }
        StatusManager.getManager().handle(new Status(IStatus.ERROR, getID(), msg, e), StatusManager.SHOW);
    }

    @Override
    protected void initializeImageRegistry(final org.eclipse.jface.resource.ImageRegistry reg) {
        final org.osgi.framework.Bundle bundle = getBundle();
        OrcResources.initializeImageRegistry(bundle, reg);
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

    /**
     * Check if a file is an Orc source file (not including Orc includes).
     *
     * @param file the IFile to check
     * @return true iff file is an Orc source file
     * @see #isOrcIncludeFile(IFile)
     */
    public static boolean isOrcSourceFile(final IFile file) {
        // TODO: Use file content types (see below)
        return "orc".equals(file.getFileExtension()); //$NON-NLS-1$
        // orcSourceFileContentType = get content type for id "edu.utexas.cs.orc.source"
        // return file.getContentDescription().getContentType().isKindOf(orcSourceFileContentType);
    }

    /**
     * Check if a file is an Orc include file.
     *
     * @param file the IFile to check
     * @return true iff file is an Orc include file
     * @see #isOrcSourceFile(IFile)
     */
    public static boolean isOrcIncludeFile(final IFile file) {
        return "inc".equals(file.getFileExtension()); //$NON-NLS-1$
        // orcIncludeFileContentType = get content type for id "edu.utexas.cs.orc.include"
        // return file.getContentDescription().getContentType().isKindOf(orcIncludeFileContentType);
    }

//    public static final PrintWriter errWriter = new PrintWriter(new FileWriter(FileDescriptor.err), true);
//
//    public static void debugEnter(final Object... args) {
//        String argString = Arrays.deepToString(args);
//        argString = argString.substring(1, argString.length() - 1);
//        debugWrite("(" + argString + ")"); //$NON-NLS-1$ //$NON-NLS-2$
//    }
//
//    public static void debugMsg(final String message) {
//        debugWrite(": " + message); //$NON-NLS-1$
//    }
//
//    private static void debugWrite(final String string) {
//        final StackTraceElement caller = new Throwable("Get stack trace").getStackTrace()[2]; //$NON-NLS-1$
//        String className = caller.getClassName();
//        if (className.startsWith(pluginID)) {
//            className = className.substring(pluginID.length() + 1);
//        }
//        errWriter.println(className + "." + caller.getMethodName() + string); //$NON-NLS-1$
//    }

}
