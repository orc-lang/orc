//
// OrcLabelProvider.java -- Java class OrcLabelProvider
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

package orc.imp.treeModelBuilder;

import java.util.HashSet;
import java.util.Set;

import orc.ast.extended.ASTNode;
import orc.ast.extended.declaration.ClassDeclaration;
import orc.ast.extended.declaration.SiteDeclaration;
import orc.ast.extended.declaration.ValDeclaration;
import orc.ast.extended.declaration.defn.Defn;
import orc.ast.extended.declaration.type.DatatypeDeclaration;
import orc.ast.extended.declaration.type.TypeAliasDeclaration;
import orc.ast.extended.declaration.type.TypeDeclaration;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.imp.editor.ModelTreeNode;
import org.eclipse.imp.services.ILabelProvider;
import org.eclipse.imp.utils.MarkerUtils;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.OrcResources;

/**
 * Label provider for the Orc Language.
 * <p>
 * A label provider maps an element of a tree model to
 * an optional image and optional text string used to display
 * the element in the user interface. 
 */
public class OrcLabelProvider implements ILabelProvider {
	private final Set<ILabelProviderListener> fListeners = new HashSet<ILabelProviderListener>();

	private static ImageRegistry sImageRegistry = Activator.getInstance().getImageRegistry();

	private static Image ORC_FILE_OBJ_IMAGE = sImageRegistry.get(OrcResources.ORC_FILE_OBJ);

	private static Image ORC_INCLUDE_OBJ_IMAGE = sImageRegistry.get(OrcResources.ORC_INCLUDE_OBJ);

	private static Image ORC_GENERIC_OBJ_IMAGE = sImageRegistry.get(OrcResources.ORC_GENERIC_OBJ);

	private static Image ORC_DEF_OBJ_IMAGE = sImageRegistry.get(OrcResources.ORC_DEF_OBJ);

	private static Image ORC_SITE_OBJ_IMAGE = sImageRegistry.get(OrcResources.ORC_SITE_OBJ);

	private static Image ORC_CLASS_OBJ_IMAGE = sImageRegistry.get(OrcResources.ORC_CLASS_OBJ);

	private static Image ORC_VARIABLE_OBJ_IMAGE = sImageRegistry.get(OrcResources.ORC_VARIABLE_OBJ);

	private static Image ORC_TYPE_OBJ_IMAGE = sImageRegistry.get(OrcResources.ORC_TYPE_OBJ);

	// private static Image ORC_OVR_IMAGE =
	// sImageRegistry.get(OrcResources.ORC_OVR);
	//
	// private static Image ERROR_OVR_IMAGE =
	// sImageRegistry.get(OrcResources.ERROR_OVR);
	//
	// private static Image WARNING_OVR_IMAGE =
	// sImageRegistry.get(OrcResources.WARNING_OVR);

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(final Object element) {
		if (element instanceof IFile) {
			final IFile file = (IFile) element;

			final Image elemImage = ORC_FILE_OBJ_IMAGE;
			// TODO: check file type, pick ORC_FILE_OBJ_IMAGE or
			// ORC_INCLUDE_OBJ_IMAGE

			final int sev = MarkerUtils.getMaxProblemMarkerSeverity(file, IResource.DEPTH_ONE);
			switch (sev) {
			case IMarker.SEVERITY_ERROR:
				// TODO: overlay ERROR_OVR_IMAGE
			case IMarker.SEVERITY_WARNING:
				// TODO: overlay WARNING_OVR_IMAGE
			default:
			}

			return elemImage;
		}
		final ASTNode n = element instanceof ModelTreeNode ? (ASTNode) ((ModelTreeNode) element).getASTNode() : (ASTNode) element;
		return getImageFor(n);
	}

	/**
	 * @param n AST node to retrieve an image
	 * @return Image representing the type of the given AST node 
	 */
	public static Image getImageFor(final ASTNode n) {
		if (n instanceof Defn) {
			return ORC_DEF_OBJ_IMAGE;
		}
		if (n instanceof SiteDeclaration) {
			return ORC_SITE_OBJ_IMAGE;
		}
		if (n instanceof ClassDeclaration) {
			return ORC_CLASS_OBJ_IMAGE;
		}
		if (n instanceof ValDeclaration) {
			return ORC_VARIABLE_OBJ_IMAGE;
		}
		if (n instanceof TypeDeclaration) {
			return ORC_TYPE_OBJ_IMAGE;
		}
		if (n instanceof TypeAliasDeclaration) {
			return ORC_TYPE_OBJ_IMAGE;
		}
		if (n instanceof DatatypeDeclaration) {
			return ORC_TYPE_OBJ_IMAGE;
		}
		return ORC_GENERIC_OBJ_IMAGE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(final Object element) {
		final ASTNode n = element instanceof ModelTreeNode ? (ASTNode) ((ModelTreeNode) element).getASTNode() : (ASTNode) element;

		return getLabelFor(n);
	}

	/**
	 * @param n AST node to label
	 * @return String representing a label of the given AST node 
	 */
	public static String getLabelFor(final ASTNode n) {
		if (n instanceof Defn) {
			final String name = ((Defn) n).name;
			return name.equals("") ? "lambda" : "def " + name;
		}
		if (n instanceof SiteDeclaration) {
			return "site " + ((SiteDeclaration) n).varname;
		}
		if (n instanceof ClassDeclaration) {
			return "class " + ((ClassDeclaration) n).varname;
		}
		if (n instanceof ValDeclaration) {
			return "val " + ((ValDeclaration) n).p;
		}
		if (n instanceof TypeDeclaration) {
			return "type " + ((TypeDeclaration) n).varname;
		}
		if (n instanceof TypeAliasDeclaration) {
			return "type " + ((TypeAliasDeclaration) n).typename;
		}
		if (n instanceof DatatypeDeclaration) {
			return "type " + ((DatatypeDeclaration) n).typename;
		}
		return "<" + n.getClass().getSimpleName() + ">";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(final ILabelProviderListener listener) {
		fListeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(final Object element, final String property) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(final ILabelProviderListener listener) {
		fListeners.remove(listener);
	}
}
