//
// OrcLabelProvider.java -- Java class OrcLabelProvider
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Aug 5, 2009.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import java.util.HashSet;
import java.util.Set;

import orc.ast.AST;
import orc.ast.OrcSyntaxConvertible;
import orc.ast.ext.ClassImport;
import orc.ast.ext.Def;
import orc.ast.ext.DefClass;
import orc.ast.ext.DefSig;
import orc.ast.ext.Include;
import orc.ast.ext.SiteDeclaration;
import orc.ast.ext.TypeDeclaration;
import orc.ast.ext.Val;

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

import scala.collection.JavaConversions;
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
	@Override
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
		final AST n = element instanceof ModelTreeNode ? (AST) ((ModelTreeNode) element).getASTNode() : (AST) element;
		return getImageFor(n);
	}

	/**
	 * @param n AST node to retrieve an image
	 * @return Image representing the type of the given AST node 
	 */
	public static Image getImageFor(final AST n) {
		if (n instanceof Include) {
			return ORC_INCLUDE_OBJ_IMAGE;
		}
		if (n instanceof DefSig) {
			return ORC_DEF_TYPE_OBJ_IMAGE;
		}
		if (n instanceof Def || n instanceof DefClass) {
			return ORC_DEF_OBJ_IMAGE;
		}
		if (n instanceof SiteDeclaration) {
			return ORC_SITE_OBJ_IMAGE;
		}
		if (n instanceof ClassImport) {
			return ORC_CLASS_OBJ_IMAGE;
		}
		if (n instanceof Val) {
			return ORC_VARIABLE_OBJ_IMAGE;
		}
		if (n instanceof TypeDeclaration) {
			return ORC_TYPE_OBJ_IMAGE;
		}
		return ORC_GENERIC_OBJ_IMAGE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(final Object element) {
		final AST n = element instanceof ModelTreeNode ? (AST) ((ModelTreeNode) element).getASTNode() : (AST) element;

		return getLabelFor(n);
	}

	/**
	 * @param n AST node to label
	 * @return String representing a label of the given AST node 
	 */
	public static String getLabelFor(final AST n) {
		if (n instanceof Include) {
			final Include idecl = (Include) n;
			return idecl.origin();
		}
		if (n instanceof Def) {
			final Def dmc = (Def) n;
			return sigToString(dmc);
		}
		if (n instanceof DefClass) {
			final DefClass dmc = (DefClass) n;
			return sigToString(dmc);
		}
		if (n instanceof DefSig) {
			final DefSig dmt = (DefSig) n;
			return sigToString(dmt);
		}
		if (n instanceof SiteDeclaration) {
			return ((SiteDeclaration) n).name();
		}
		if (n instanceof ClassImport) {
			return ((ClassImport) n).name();
		}
		if (n instanceof Val) {
			return ((Val) n).p().toOrcSyntax();
		}
		if (n instanceof TypeDeclaration) {
			return ((TypeDeclaration) n).name();
		}
		// If we get here, someone forgot to add a case above....
		return "<" + n.getClass().getSimpleName() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void addListener(final ILabelProviderListener listener) {
		fListeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		/* Nothing to do */
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isLabelProperty(final Object element, final String property) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void removeListener(final ILabelProviderListener listener) {
		fListeners.remove(listener);
	}

	private static String sigToString(final Def d) {
		final StringBuilder s = new StringBuilder();

		s.append(d.name());
		s.append('(');
		s.append(listMkString(JavaConversions.asIterable(d.formals()), ", ")); //$NON-NLS-1$
		s.append(')');

		return s.toString();
	}

	private static String sigToString(final DefClass d) {
		final StringBuilder s = new StringBuilder();

		s.append(d.name());

		if (d.typeformals() != null && d.typeformals().isDefined()) {
			s.append('[');
			s.append(listMkString(JavaConversions.asIterable(d.typeformals().get()), ", ")); //$NON-NLS-1$
			s.append(']');
		}

		s.append('(');
		s.append(listMkString(JavaConversions.asIterable(d.formals()), ", ")); //$NON-NLS-1$
		s.append(')');

		return s.toString();
	}

	private static String sigToString(final DefSig d) {
		final StringBuilder s = new StringBuilder();

		s.append(d.name());

		if (d.typeformals() != null && d.typeformals().isDefined()) {
			s.append('[');
			s.append(listMkString(JavaConversions.asIterable(d.typeformals().get()), ", ")); //$NON-NLS-1$
			s.append(']');
		}

		s.append('(');
		s.append(listMkString(JavaConversions.asIterable(d.argtypes()), ", ")); //$NON-NLS-1$
		s.append(')');

		s.append(" :: "); //$NON-NLS-1$
		s.append(d.returntype().toOrcSyntax());

		return s.toString();
	}

	private static String listMkString(final Iterable<?> theList, final String sep) {
		final StringBuilder sb = new StringBuilder();
		for (final Object o : theList) {
			if (o instanceof OrcSyntaxConvertible) {
				sb.append(((OrcSyntaxConvertible) o).toOrcSyntax());
			} else {
				sb.append(o.toString());
			}
			sb.append(sep);
		}
		if (sb.length() == 0) {
			return ""; //$NON-NLS-1$
		} else {
			return sb.substring(0, sb.length() - sep.length());
		}
	}

}
