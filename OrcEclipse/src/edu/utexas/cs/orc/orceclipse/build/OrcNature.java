//
// OrcNature.java -- Java class OrcNature
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

package edu.utexas.cs.orc.orceclipse.build;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

import edu.utexas.cs.orc.orceclipse.OrcPlugin;

/**
 * Orc project nature. When a project is configured with the Orc nature, it will
 * have the Orc builder, Orc project actions, and so on.
 *
 * @see org.eclipse.core.resources.IProjectNature
 */
public class OrcNature implements IProjectNature {
    private static final String natureID = OrcPlugin.getID() + ".project.orcNature"; //$NON-NLS-1$

    /** The project to which this project nature applies. */
    private IProject project;

    /**
     * Constructs an OrcNature. Nature instances are managed by Eclipse, which
     * requires a public, no-argument constructor.
     */
    public OrcNature() {
        super();
    }

    /**
     * Get the nature ID for the Orc project nature.
     *
     * @return the nature ID string
     */
    static public String getNatureID() {
        return natureID;
    }

    /**
     * Get the builder ID for the Orc project builder.
     *
     * @return the builder ID string
     */
    static public String getBuilderID() {
        return OrcBuilder.BUILDER_ID;
    }

    /**
     * Configures this nature for its project. This is called by the workspace
     * when natures are added to the project using
     * <code>IProject.setDescription</code> and should not be called directly by
     * clients. The nature extension id is added to the list of natures before
     * this method is called, and need not be added here. Exceptions thrown by
     * this method will be propagated back to the caller of
     * <code>IProject.setDescription</code>, but the nature will remain in the
     * project description.
     *
     * @exception CoreException if this method fails.
     */
    @Override
    public void configure() throws CoreException {
        final IProjectDescription desc = project.getDescription();
        final ICommand[] commands = desc.getBuildSpec();

        for (final ICommand command : commands) {
            if (command.getBuilderName().equals(getBuilderID())) {
                return;
            }
        }

        final ICommand[] newCommands = new ICommand[commands.length + 1];
        System.arraycopy(commands, 0, newCommands, 0, commands.length);
        final ICommand command = desc.newCommand();
        command.setBuilderName(getBuilderID());
        newCommands[newCommands.length - 1] = command;
        desc.setBuildSpec(newCommands);
        project.setDescription(desc, null);
    }

    /**
     * De-configures this nature for its project. This is called by the
     * workspace when natures are removed from the project using
     * <code>IProject.setDescription</code> and should not be called directly by
     * clients. The nature extension id is removed from the list of natures
     * before this method is called, and need not be removed here. Exceptions
     * thrown by this method will be propagated back to the caller of
     * <code>IProject.setDescription</code>, but the nature will still be
     * removed from the project description. *
     *
     * @exception CoreException if this method fails.
     */
    @Override
    public void deconfigure() throws CoreException {
        final IProjectDescription description = getProject().getDescription();
        final ICommand[] commands = description.getBuildSpec();
        for (int i = 0; i < commands.length; ++i) {
            if (commands[i].getBuilderName().equals(getBuilderID())) {
                final ICommand[] newCommands = new ICommand[commands.length - 1];
                System.arraycopy(commands, 0, newCommands, 0, i);
                System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
                description.setBuildSpec(newCommands);
                project.setDescription(description, null);
                return;
            }
        }
    }

    /**
     * Returns the project to which this project nature applies.
     *
     * @return the project handle
     */
    @Override
    public IProject getProject() {
        return project;
    }

    /**
     * Sets the project to which this nature applies. Used when instantiating
     * this project nature runtime. This is called by
     * <code>IProject.create()</code> or <code>IProject.setDescription()</code>
     * and should not be called directly by clients.
     *
     * @param project the project to which this nature applies
     */
    @Override
    public void setProject(final IProject project) {
        this.project = project;
    }

    /**
     * Adds the Orc project nature ID to the given project.
     *
     * @param project the IProject to add the Orc project nature ID to.
     * @throws CoreException if getting or setting the ProjectDescription fails
     */
    static public void addToProject(final IProject project) throws CoreException {
        final IProjectDescription description = project.getDescription();
        final String[] natures = description.getNatureIds();

        for (final String nature : natures) {
            if (getNatureID().equals(nature)) {
                // Already set
                return;
            }
        }

        // Add the nature
        final String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        newNatures[natures.length] = getNatureID();
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
    }

}
