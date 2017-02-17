//
// OrcLabelProvider.java -- Java class OrcLabelProvider
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

package edu.utexas.cs.orc.orceclipse.edit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import scala.collection.JavaConversions;

import orc.ast.AST;
import orc.ast.OrcSyntaxConvertible;
import orc.ast.ext.Callable;
import orc.ast.ext.CallableSig;
import orc.ast.ext.ClassDeclaration;
import orc.ast.ext.ClassImport;
import orc.ast.ext.Def;
import orc.ast.ext.DefSig;
import orc.ast.ext.Include;
import orc.ast.ext.New;
import orc.ast.ext.Site;
import orc.ast.ext.SiteImport;
import orc.ast.ext.SiteSig;
import orc.ast.ext.TypeDeclaration;
import orc.ast.ext.Val;
import orc.ast.ext.ValSig;

import edu.utexas.cs.orc.orceclipse.OrcPlugin;
import edu.utexas.cs.orc.orceclipse.OrcResources;

/**
 * Label provider for the Orc Language.
 * <p>
 * A label provider maps an element of a tree model to an optional image and
 * optional text string used to display the element in the user interface.
 */
public class OrcLabelProvider extends BaseLabelProvider implements ILabelProvider {
    private static ILabelProvider decoratingWorkbenchLabelProvider = WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider();

    private static ImageRegistry orcImageRegistry = OrcPlugin.getInstance().getImageRegistry();

    private static Image ORC_FILE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_FILE_OBJ);

    private static Image ORC_FILE_W_ERROR = new DecorationOverlayIcon(ORC_FILE_OBJ_IMAGE, orcImageRegistry.getDescriptor(OrcResources.ERROR_OVR), IDecoration.BOTTOM_LEFT).createImage();

    private static Image ORC_FILE_W_WARNING = new DecorationOverlayIcon(ORC_FILE_OBJ_IMAGE, orcImageRegistry.getDescriptor(OrcResources.WARNING_OVR), IDecoration.BOTTOM_LEFT).createImage();

    private static Image ORC_INCLUDE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_INCLUDE_OBJ);

    private static Image ORC_INCLUDE_W_ERROR = new DecorationOverlayIcon(ORC_INCLUDE_OBJ_IMAGE, orcImageRegistry.getDescriptor(OrcResources.ERROR_OVR), IDecoration.BOTTOM_LEFT).createImage();

    private static Image ORC_INCLUDE_W_WARNING = new DecorationOverlayIcon(ORC_INCLUDE_OBJ_IMAGE, orcImageRegistry.getDescriptor(OrcResources.WARNING_OVR), IDecoration.BOTTOM_LEFT).createImage();

    private static Image ORC_GENERIC_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_GENERIC_OBJ);

    private static Image ORC_DEF_TYPE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_DEF_TYPE_OBJ);

    private static Image ORC_DEF_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_DEF_OBJ);

    private static Image ORC_ORCSITE_TYPE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_ORCSITE_TYPE_OBJ);
    
    private static Image ORC_ORCSITE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_ORCSITE_OBJ);

    private static Image ORC_SITE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_SITE_OBJ);

    private static Image ORC_CLASS_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_CLASS_OBJ);

    private static Image ORC_VARIABLE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_VARIABLE_OBJ);

    private static Image ORC_VARIABLE_TYPE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_VARIABLE_TYPE_OBJ);
    
    private static Image ORC_TYPE_OBJ_IMAGE = orcImageRegistry.get(OrcResources.ORC_TYPE_OBJ);

    @Override
    public Image getImage(final Object element) {
        Image result;
        if (element instanceof OrcContentProvider.OutlineTreeFileNode) {
            result = getImageFor(((OrcContentProvider.OutlineTreeFileNode) element).getFile());
        } else if (element instanceof IFile) {
            result = getImageFor((IFile) element);
        } else if (element instanceof OrcContentProvider.OutlineTreeAstNode) {
            result = getImageFor(((OrcContentProvider.OutlineTreeAstNode) element).getAstNode());
        } else if (element instanceof AST) {
            result = getImageFor((AST) element);
        } else {
            result = decoratingWorkbenchLabelProvider.getImage(element);
        }
        return result;
    }

    /**
     * @param file the file to retrieve an image
     * @return Image representing the type of the given AST node
     */
    public static Image getImageFor(final IFile file) {
        Image elemImage = null;
        int sev = -1;
        try {
            sev = file.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
        } catch (final CoreException e) {
            OrcPlugin.log(e);
        }
        if (!OrcPlugin.isOrcIncludeFile(file)) {
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

    /**
     * @param n the AST node to retrieve an image
     * @return Image representing the type of the given AST node
     */
    public static Image getImageFor(final AST n) {
        if (n instanceof Include) {
            return ORC_INCLUDE_OBJ_IMAGE;
        }
        if (n instanceof DefSig) {
            return ORC_DEF_TYPE_OBJ_IMAGE;
        }
        if (n instanceof Def) {
            return ORC_DEF_OBJ_IMAGE;
        }
        if (n instanceof SiteSig) {
            return ORC_ORCSITE_TYPE_OBJ_IMAGE;
        }
        if (n instanceof Site) {
            return ORC_ORCSITE_OBJ_IMAGE;
        }
        if (n instanceof SiteImport) {
            return ORC_SITE_OBJ_IMAGE;
        }
        if (n instanceof ClassImport) {
            return ORC_CLASS_OBJ_IMAGE;
        }
        if (n instanceof Val) {
            return ORC_VARIABLE_OBJ_IMAGE;
        }
        if (n instanceof ValSig) {
            return ORC_VARIABLE_TYPE_OBJ_IMAGE;
        }
        if (n instanceof TypeDeclaration) {
            return ORC_TYPE_OBJ_IMAGE;
        }
        return ORC_GENERIC_OBJ_IMAGE;
    }

    @Override
    public String getText(final Object element) {
        String result;

        if (element instanceof OrcContentProvider.OutlineTreeAstNode) {
            result = getLabelFor(((OrcContentProvider.OutlineTreeAstNode) element).getAstNode());
        } else if (element instanceof AST) {
            result = getLabelFor((AST) element);
        } else if (element instanceof OrcContentProvider.OutlineTreeFileNode) {
            result = decoratingWorkbenchLabelProvider.getText(((OrcContentProvider.OutlineTreeFileNode) element).getFile());
        } else {
            result = decoratingWorkbenchLabelProvider.getText(element);
        }
        return result;
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
        if (n instanceof Callable) {
            final Callable dmc = (Callable) n;
            return sigToString(dmc);
        }
        if (n instanceof CallableSig) {
            final CallableSig dmt = (CallableSig) n;
            return sigToString(dmt);
        }
        if (n instanceof SiteImport) {
            return ((SiteImport) n).name();
        }
        if (n instanceof ClassImport) {
            return ((ClassImport) n).name();
        }
        if (n instanceof Val) {
            return ((Val) n).p().toOrcSyntax();
        }
        if (n instanceof ValSig) {
            ValSig sig = (ValSig) n;
            if (sig.t().isDefined()) {
                return sig.name() + " :: " + sig.t().get().toOrcSyntax(); //$NON-NLS-1$
            } else {
                return sig.name();
                
            }
        }
        if (n instanceof TypeDeclaration) {
            return ((TypeDeclaration) n).name();
        }
        if (n instanceof ClassDeclaration) {
            ClassDeclaration decl = (ClassDeclaration) n;
            if (decl.superclass().isDefined()) {
                return decl.name() + " extends " + decl.superclass().get().toInterfaceString(); //$NON-NLS-1$
            } else {
                return decl.name();
            }
        }
        if (n instanceof New) {        
            return "new " + ((New) n).cls().toInterfaceString(); //$NON-NLS-1$
        }
        // If we get here, someone forgot to add a case above....
        return "<" + n.getClass().getSimpleName() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void dispose() {
        /* Nothing to do */
    }

    private static String sigToString(final Callable d) {
        final StringBuilder s = new StringBuilder();

        s.append(d.name());

        if (d.typeformals() != null && d.typeformals().isDefined()) {
            s.append('[');
            s.append(listMkString(JavaConversions.asJavaIterable(d.typeformals().get()), ", ")); //$NON-NLS-1$
            s.append(']');
        }

        s.append('(');
        s.append(listMkString(JavaConversions.asJavaIterable(d.formals()), ", ")); //$NON-NLS-1$
        s.append(')');

        if (d.returntype().isDefined()) {
            s.append(" :: "); //$NON-NLS-1$
            s.append(d.returntype().get().toOrcSyntax());
        }

        return s.toString();
    }

    private static String sigToString(final CallableSig d) {
        final StringBuilder s = new StringBuilder();

        s.append(d.name());

        if (d.typeformals() != null && d.typeformals().isDefined()) {
            s.append('[');
            s.append(listMkString(JavaConversions.asJavaIterable(d.typeformals().get()), ", ")); //$NON-NLS-1$
            s.append(']');
        }

        s.append('(');
        s.append(listMkString(JavaConversions.asJavaIterable(d.argtypes()), ", ")); //$NON-NLS-1$
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
