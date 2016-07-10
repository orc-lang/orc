//
// EnableOrcNature.java -- Java class EnableOrcNature
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

package edu.utexas.cs.orc.orceclipse.project;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import edu.utexas.cs.orc.orceclipse.Messages;
import edu.utexas.cs.orc.orceclipse.build.OrcNature;

/**
 * Adds an Orc "nature" to the selected project's attributes.
 * <p>
 * (This class is a UI command handler. The command is defined in the
 * <code>plugin.xml</code> file.)
 */
public class EnableOrcNature extends AbstractHandler {

    /**
     * Executes the add Orc nature action.
     *
     * @param event An event containing event parameters, the event trigger, and
     *            the application context. Must not be <code>null</code>.
     * @return the result of the execution. Reserved for future use, must be
     *         <code>null</code>.
     * @throws ExecutionException if an exception occurred during execution.
     */
    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection ss = (IStructuredSelection) selection;

            for (final Iterator<?> selnIter = ss.iterator(); selnIter.hasNext();) {
                final Object currSelnElem = selnIter.next();

                IProject project = null;
                if (currSelnElem instanceof IProject) {
                    project = (IProject) currSelnElem;
                } else if (currSelnElem instanceof IJavaProject) {
                    project = ((IJavaProject) currSelnElem).getProject();
                } else if (currSelnElem instanceof IAdaptable) {
                    project = (IProject) ((IAdaptable) currSelnElem).getAdapter(IProject.class);
                }

                if (project != null) {
                    try {
                        if (project.getNature(JavaCore.NATURE_ID) != null) {
                            MessageDialog.openError(null, Messages.EnableOrcNature_AlreadyJavaErrorTitle, Messages.EnableOrcNature_AlreadJavaErrorMessage);
                            return null;
                        }
                    } catch (final CoreException e) {
                        /* This is OK, it means we don't have Java nature */
                    }
                    try {
                        OrcNature.addToProject(project);
                    } catch (final CoreException e) {
                        throw new ExecutionException("Failure when setting project nature for project: " + project, e); //$NON-NLS-1$
                    }
                }
            }
        }

        return null;
    }
}
