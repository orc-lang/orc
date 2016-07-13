//
// ActionStrings.java -- Java class ActionStrings
// Project OrcEclipse
//
// Created by jthywiss on Jul 12, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import java.util.ResourceBundle;

/**
 * References into resource bundle for Actions that use a resource bundle for
 * configuration.
 * <p>
 * The following keys, prepended by the given action prefix, are used for
 * retrieving resources from the resource bundle:
 * <ul>
 * <li><code>"label"</code> - <code>setText</code></li>
 * <li><code>"tooltip"</code> - <code>setToolTipText</code></li>
 * <li><code>"image"</code> - <code>setImageDescriptor</code></li>
 * <li><code>"description"</code> - <code>setDescription</code></li>
 * </ul>
 * </p>
 *
 * @see org.eclipse.ui.texteditor.ResourceAction
 * @author jthywiss
 */
@SuppressWarnings({ "javadoc", "nls" })
public class ActionStrings {
    private static final String BUNDLE_NAME = "edu.utexas.cs.orc.orceclipse.action-strings";
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    public static final String TOGGLE_INSERT_MODE = "Editor.ToggleInsertMode.";
    public static final String COMMENT = "Editor.Comment.";
    public static final String UNCOMMENT = "Editor.Unomment.";

    private ActionStrings() {
        /* No instance members */
    }

    public static ResourceBundle getResourceBundle() {
        return RESOURCE_BUNDLE;
    }

}
