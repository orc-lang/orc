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

package edu.utexas.cs.orc.orceclipse.edit;

import java.util.HashSet;
import java.util.Set;

import orc.ast.extended.ASTNode;
import orc.ast.extended.declaration.ClassDeclaration;
import orc.ast.extended.declaration.IncludeDeclaration;
import orc.ast.extended.declaration.SiteDeclaration;
import orc.ast.extended.declaration.ValDeclaration;
import orc.ast.extended.declaration.def.DefMemberClause;
import orc.ast.extended.declaration.def.DefMemberType;
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
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
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
 *
 * @see org.eclipse.imp.editor.ModelTreeNode
 */
public class OrcLabelProvider implements ILabelProvider {
	private final Set<ILabelProviderListener> fListeners = new HashSet<ILabelProviderListener>();

	private static ImageRegistry orcImageRegistry = Activator.getInstance().getImageRegistry();

	private static Image ORC_FILE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_FILE_OBJ);

	private static Image ORC_FILE_W_ERROR = new DecorationOverlayIcon(ORC_FILE_OBJ_IMAGE, orcImageRegistry.getDescriptor(OrcResources.ERROR_OVR), IDecoration.BOTTOM_LEFT).createImage();

	private static Image ORC_FILE_W_WARNING = new DecorationOverlayIcon(ORC_FILE_OBJ_IMAGE, orcImageRegistry.getDescriptor(OrcResources.WARNING_OVR), IDecoration.BOTTOM_LEFT).createImage();

	private static Image ORC_INCLUDE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_INCLUDE_OBJ);

	private static Image ORC_INCLUDE_W_ERROR = new DecorationOverlayIcon(ORC_INCLUDE_OBJ_IMAGE, orcImageRegistry.getDescriptor(OrcResources.ERROR_OVR), IDecoration.BOTTOM_LEFT).createImage();

	private static Image ORC_INCLUDE_W_WARNING = new DecorationOverlayIcon(ORC_INCLUDE_OBJ_IMAGE, orcImageRegistry.getDescriptor(OrcResources.WARNING_OVR), IDecoration.BOTTOM_LEFT).createImage();

	private static Image ORC_GENERIC_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_GENERIC_OBJ);

	private static Image ORC_DEF_TYPE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_DEF_TYPE_OBJ);

	private static Image ORC_DEF_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_DEF_OBJ);

	private static Image ORC_SITE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_SITE_OBJ);

	private static Image ORC_CLASS_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_CLASS_OBJ);

	private static Image ORC_VARIABLE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_VARIABLE_OBJ);

	private static Image ORC_TYPE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_TYPE_OBJ);

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(final Object element) {
		if (element instanceof IFile) {
			final IFile file = (IFile) element;

			Image elemImage = null;
			final int sev = MarkerUtils.getMaxProblemMarkerSeverity(file, IResource.DEPTH_ONE);
			if (!file.getName().toLowerCase().endsWith(".inc")) { //$NON-NLS-1$
				// Assume Orc file
				switch (sev) {
				case IMarker.SEVERITY_ERROR:
					elemImage = ORC_FILE_W_ERROR;
					break;
				case IMarker.SEVERITY_WARNING:
					elemImage = ORC_FILE_W_WARNING;
					break;
				default:
					elemImage = ORC_FILE_OBJ_IMAGE;
					break;
				}
			} else {
				// Include file
				switch (sev) {
				case IMarker.SEVERITY_ERROR:
					elemImage = ORC_INCLUDE_W_ERROR;
					break;
				case IMarker.SEVERITY_WARNING:
					elemImage = ORC_INCLUDE_W_WARNING;
					break;
				default:
					elemImage = ORC_INCLUDE_OBJ_IMAGE;
					break;
				}
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
		if (n instanceof IncludeDeclaration) {
			return ORC_INCLUDE_OBJ_IMAGE;
		}
		if (n instanceof DefMemberType) {
			return ORC_DEF_TYPE_OBJ_IMAGE;
		}
		if (n instanceof DefMemberClause) {
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
		if (n instanceof IncludeDeclaration) {
			final IncludeDeclaration idecl = (IncludeDeclaration) n;
			return idecl.sourceFile;
		}
		if (n instanceof DefMemberClause) {
			final DefMemberClause dmc = (DefMemberClause) n;
			return dmc.sigToString();
		}
		if (n instanceof DefMemberType) {
			final DefMemberType dmt = (DefMemberType) n;
			return dmt.sigToString();
		}
		if (n instanceof SiteDeclaration) {
			return ((SiteDeclaration) n).varname;
		}
		if (n instanceof ClassDeclaration) {
			return ((ClassDeclaration) n).varname;
		}
		if (n instanceof ValDeclaration) {
			return ((ValDeclaration) n).p.toString();
		}
		if (n instanceof TypeDeclaration) {
			return ((TypeDeclaration) n).varname;
		}
		if (n instanceof TypeAliasDeclaration) {
			return ((TypeAliasDeclaration) n).typename;
		}
		if (n instanceof DatatypeDeclaration) {
			return ((DatatypeDeclaration) n).typename;
		}
		// If we get here, someone forgot to add a case above....
		return "<" + n.getClass().getSimpleName() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
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
