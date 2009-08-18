//
// OrcResources.java -- Java interface OrcResources
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

package edu.utexas.cs.orc.orceclipse;

/**
 * A collection of shared resources (mostly images) for the Orc plug-in,
 * along with some convenience methods,
 *
 * @author jthywiss
 */
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
	public static final String ORC_INCLUDE_OBJ = "orc_inclue_obj";

	/**
	 * Name of the 16x16 icon representing a generic Orc object
	 */
	public static final String ORC_GENERIC_OBJ = "orc_generic_obj";

	/**
	 * Name of the 16x16 icon representing an Orc def
	 */
	public static final String ORC_DEF_OBJ = "orc_def_obj";

	/**
	 * Name of the 16x16 icon representing an Orc site declaration
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
	public static final org.eclipse.core.runtime.IPath ICONS_PATH = new org.eclipse.core.runtime.Path("icons/"); //$NON-NLS-1$("icons/"); 

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	protected static void initializeImageRegistry(final org.osgi.framework.Bundle bundle, final org.eclipse.jface.resource.ImageRegistry reg) {

		org.eclipse.core.runtime.IPath path = new org.eclipse.core.runtime.Path("orc16.gif");//$NON-NLS-1$
		org.eclipse.jface.resource.ImageDescriptor imageDescriptor = createImageDescriptor(bundle, path);
		reg.put(OrcResources.ORC_PLUGIN_ICON, imageDescriptor);

		path = ICONS_PATH.append("orc_file_obj.gif");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(bundle, path);
		reg.put(OrcResources.ORC_FILE_OBJ, imageDescriptor);

		path = ICONS_PATH.append("orc_inclue_obj.gif");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(bundle, path);
		reg.put(OrcResources.ORC_INCLUDE_OBJ, imageDescriptor);

		path = ICONS_PATH.append("orc_generic_obj.gif");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(bundle, path);
		reg.put(OrcResources.ORC_GENERIC_OBJ, imageDescriptor);

		path = ICONS_PATH.append("orc_def_obj.gif");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(bundle, path);
		reg.put(OrcResources.ORC_DEF_OBJ, imageDescriptor);

		path = ICONS_PATH.append("orc_site_obj.gif");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(bundle, path);
		reg.put(OrcResources.ORC_SITE_OBJ, imageDescriptor);

		path = ICONS_PATH.append("orc_class_obj.gif");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(bundle, path);
		reg.put(OrcResources.ORC_CLASS_OBJ, imageDescriptor);

		path = ICONS_PATH.append("orc_variable_obj.gif");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(bundle, path);
		reg.put(OrcResources.ORC_VARIABLE_OBJ, imageDescriptor);

		path = ICONS_PATH.append("orc_type_obj.gif");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(bundle, path);
		reg.put(OrcResources.ORC_TYPE_OBJ, imageDescriptor);

		path = ICONS_PATH.append("orc_ovr.gif");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(bundle, path);
		reg.put(OrcResources.ORC_OVR, imageDescriptor);

		path = ICONS_PATH.append("error_ovr.gif");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(bundle, path);
		reg.put(OrcResources.ERROR_OVR, imageDescriptor);

		path = ICONS_PATH.append("warning_ovr.gif");//$NON-NLS-1$
		imageDescriptor = createImageDescriptor(bundle, path);
		reg.put(OrcResources.WARNING_OVR, imageDescriptor);
	}

	/**
	 * Create an image descriptor for the given path in a bundle. The path can contain variables
	 * like $NL$.
	 * If no image could be found, <code>null</code> is returned.
	 * 
	 * @param bundle Plug-in to serh fo the image
	 * @param path Path to the image file in the give plug-in
	 * @return ImageDescriptor of image or null
	 */
	public static org.eclipse.jface.resource.ImageDescriptor createImageDescriptor(final org.osgi.framework.Bundle bundle, final org.eclipse.core.runtime.IPath path) {
		final java.net.URL url = org.eclipse.core.runtime.FileLocator.find(bundle, path, null);
		if (url != null) {
			return org.eclipse.jface.resource.ImageDescriptor.createFromURL(url);
		}
		return null;
	}

}
