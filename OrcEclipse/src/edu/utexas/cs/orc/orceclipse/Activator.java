//
// Activator.java -- Java class Activator
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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.imp.runtime.PluginBase;
import org.eclipse.ui.statushandlers.StatusManager;

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
    private static final String pluginID = "edu.utexas.cs.orc.orceclipse"; //$NON-NLS-1$
    private static final String languageName = "Orc"; //$NON-NLS-1$

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

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        OrcConfigSettings.initDefaultPrefs();
    }

    @Override
    public String getID() {
        return pluginID;
    }

    /*
     * This is really language NAME, not ID -- PluginBase named the method
     * incorrectly.
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
            getInstance().logException(e.getMessage(), e);
        } else {
            getInstance().logException(Messages.Activator_InternalError, e);
        }
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
        StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.getInstance().getID(), msg, e), StatusManager.SHOW);
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

    public static boolean isOrcSourceFile(final IPath file) {
        //TODO: Use extensions declared in plugin.xml?
        return "orc".equals(file.getFileExtension()); //$NON-NLS-1$
    }

    public static boolean isOrcIncludeFile(final IPath file) {
        return "inc".equals(file.getFileExtension()); //$NON-NLS-1$
    }

//    public static final PrintWriter errWriter = new PrintWriter(new FileWriter(FileDescriptor.err), true);
//    public static final PrintStream errStream = new PrintStream(new FileOutputStream(FileDescriptor.err), true);
//    public static void debugEnter(Object... args) {
//        String argString = Arrays.deepToString(args);
//        argString = argString.substring(1, argString.length()-1);
//        debugWrite("("+argString+")"); //$NON-NLS-1$ //$NON-NLS-2$
//    }
//    public static void debugMsg(String message) {
//        debugWrite(": "+message); //$NON-NLS-1$
//    }
//    private static void debugWrite(String string) {
//        final StackTraceElement caller = new Throwable("Get stack trace").getStackTrace()[2]; //$NON-NLS-1$
//        String className = caller.getClassName();
//        if (className.startsWith(pluginID)) className = className.substring(pluginID.length()+1);
//        errWriter.println(className+"."+caller.getMethodName()+string); //$NON-NLS-1$
//    }
}
