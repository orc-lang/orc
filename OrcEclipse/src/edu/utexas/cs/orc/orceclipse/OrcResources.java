//
// OrcResources.java -- Java interface OrcResources
// Project OrcEclipse
//
// Created by jthywiss on Aug 5, 2009.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;

import org.osgi.framework.Bundle;

/**
 * A collection of shared resources (mostly images) for the Orc plug-in, along
 * with some convenience methods,
 *
 * @author jthywiss
 */
@SuppressWarnings("nls")
public class OrcResources {

    /**
     * Name of the 16x16 icon representing the Orc plug-in
     */
    public static final String ORC_PLUGIN_ICON = "orc16";

    /**
     * Name of the 16x16 icon representing an Orc file
     */
    public static final String ORC_FILE_OBJ = "orc_file_obj";

    /**
     * Name of the 16x16 icon representing an Orc include file
     */
    public static final String ORC_INCLUDE_OBJ = "orc_include_obj";

    /**
     * Name of the 16x16 icon representing a generic Orc object
     */
    public static final String ORC_GENERIC_OBJ = "orc_generic_obj";

    /**
     * Name of the 16x16 icon representing an Orc def type declaration
     */
    public static final String ORC_DEF_TYPE_OBJ = "orc_def_type_obj";

    /**
     * Name of the 16x16 icon representing an Orc def (or def clause)
     */
    public static final String ORC_DEF_OBJ = "orc_def_obj";

    /**
     * Name of the 16x16 icon representing an Orc internal site type declaration
     */
    public static final String ORC_ORCSITE_TYPE_OBJ = "orc_orcsite_type_obj";

    /**
     * Name of the 16x16 icon representing an Orc internal site
     */
    public static final String ORC_ORCSITE_OBJ = "orc_orcsite_obj";

    /**
     * Name of the 16x16 icon representing an Orc external site declaration
     */
    public static final String ORC_SITE_OBJ = "orc_site_obj";

    /**
     * Name of the 16x16 icon representing an Orc Java class declaration
     */
    public static final String ORC_CLASS_OBJ = "orc_class_obj";

    /**
     * Name of the 16x16 icon representing an Orc variable (val) declaration
     */
    public static final String ORC_VARIABLE_OBJ = "orc_variable_obj";

    /**
     * Name of the 16x16 icon representing an Orc variable (val) type or abstract declaration
     */
    public static final String ORC_VARIABLE_TYPE_OBJ = "orc_variable_type_obj";

    /**
     * Name of the 16x16 icon representing an Orc type declaration
     */
    public static final String ORC_TYPE_OBJ = "orc_type_obj";

    /**
     * Name of the 7x8 overlay image for the Orc project nature
     */
    public static final String ORC_OVR = "orc_ovr";

    /**
     * Name of the 7x8 overlay image for an object with an error
     */
    public static final String ERROR_OVR = "error_ovr";

    /**
     * Name of the 7x8 overlay image for an object with a warning
     */
    public static final String WARNING_OVR = "warning_ovr";

    /**
     * Bundle path to the icons
     */
    public static final IPath ICONS_PATH = new Path("icons/");

    protected static void initializeImageRegistry(final Bundle bundle, final ImageRegistry reg) {

        reg.put(OrcResources.ORC_PLUGIN_ICON, createImageDescriptor(bundle, new Path(ORC_PLUGIN_ICON + ".gif")));

        addImageDescriptor(bundle, OrcResources.ORC_FILE_OBJ, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ORC_INCLUDE_OBJ, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ORC_GENERIC_OBJ, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ORC_DEF_TYPE_OBJ, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ORC_DEF_OBJ, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ORC_ORCSITE_TYPE_OBJ, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ORC_ORCSITE_OBJ, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ORC_SITE_OBJ, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ORC_CLASS_OBJ, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ORC_VARIABLE_OBJ, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ORC_VARIABLE_TYPE_OBJ, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ORC_TYPE_OBJ, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ORC_OVR, "gif", reg);

        addImageDescriptor(bundle, OrcResources.ERROR_OVR, "gif", reg);

        addImageDescriptor(bundle, OrcResources.WARNING_OVR, "gif", reg);
    }

    private static void addImageDescriptor(final Bundle bundle, final String name, final String fileType, final ImageRegistry reg) {
        IPath path;
        ImageDescriptor imageDescriptor;
        path = ICONS_PATH.append(name + '.' + fileType);
        imageDescriptor = createImageDescriptor(bundle, path);
        reg.put(name, imageDescriptor);
    }

    /**
     * Create an image descriptor for the given path in a bundle. The path can
     * contain variables like $NL$. If no image could be found,
     * <code>null</code> is returned.
     *
     * @param bundle Plug-in to serh fo the image
     * @param path Path to the image file in the give plug-in
     * @return ImageDescriptor of image or null
     */
    public static ImageDescriptor createImageDescriptor(final Bundle bundle, final IPath path) {
        final URL url = FileLocator.find(bundle, path, null);
        if (url != null) {
            return ImageDescriptor.createFromURL(url);
        }
        return null;
    }

}
