//
// OrcContentProvider.java -- Java class OrcContentProvider
// Project OrcEclipse
//
// Created by jthywiss on Jul 8, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.edit;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import scala.collection.JavaConversions;

import orc.ast.AST;
import orc.ast.ext.ClassImport;
import orc.ast.ext.DefDeclaration;
import orc.ast.ext.Include;
import orc.ast.ext.SiteDeclaration;
import orc.ast.ext.TypeDeclaration;
import orc.ast.ext.Val;

/**
 * Builds an Outline view tree that is a subset of the Orc extended AST
 *
 * @see orc.ast.AST
 * @see org.eclipse.jface.viewers.TreeNodeContentProvider
 * @author jthywiss
 */
public class OrcContentProvider implements ITreeContentProvider {

    protected static final Object[] NO_CHILDREN = new Object[0];

    /**
     * Constructs an object of class OrcContentProvider.
     */
    public OrcContentProvider() {
        super();
    }

    /**
     * Returns the elements to display in the viewer when its input is set to
     * the given element. These elements can be presented as rows in a table,
     * items in a list, etc. The result is not modified by the viewer.
     * <p>
     * <b>NOTE:</b> The returned array must not contain the given
     * <code>inputElement</code>, since this leads to recursion issues in
     * <code>AbstractTreeViewer</code>.
     * </p>
     *
     * @param inputElement the input element
     * @return the array of elements to display in the viewer
     */
    @Override
    public Object[] getElements(final Object inputElement) {
        return getChildren(inputElement);
    }

    /**
     * Returns the child elements of the given parent element.
     * <p>
     * The difference between this method and
     * <code>IStructuredContentProvider.getElements</code> is that
     * <code>getElements</code> is called to obtain the tree viewer's root
     * elements, whereas <code>getChildren</code> is used to obtain the children
     * of a given parent element in the tree (including a root).
     * </p>
     * The result is not modified by the viewer.
     *
     * @param parentElement the parent element
     * @return an array of child elements
     */
    @Override
    public Object[] getChildren(final Object parentElement) {
        if (parentElement == null || parentElement instanceof IContainer && !((IContainer) parentElement).isAccessible()) {
            return NO_CHILDREN;
        }

        try {
            if (parentElement instanceof IContainer) {
                return ((IContainer) parentElement).members();
            }
        } catch (final CoreException e) {
            return NO_CHILDREN;
        }

        if (parentElement instanceof OutlineTreeNode) {
            return ((OutlineTreeNode) parentElement).getChildren();
        }

        return NO_CHILDREN;
    }

    /**
     * Returns the parent for the given element, or <code>null</code> indicating
     * that the parent can't be computed. In this case the tree-structured
     * viewer can't expand a given node correctly if requested.
     *
     * @param element the element
     * @return the parent element, or <code>null</code> if it has none or if the
     *         parent cannot be computed
     */
    @Override
    public Object getParent(final Object element) {
        if (element == null || element instanceof IResource && !((IResource) element).exists()) {
            return null;
        }
        if (element instanceof IResource) {
            return ((IResource) element).getParent();
        }

        if (element instanceof OutlineTreeNode) {
            return ((OutlineTreeNode) element).getParent();
        }

        return null;
    }

    /**
     * Returns whether the given element has children.
     * <p>
     * Intended as an optimization for when the viewer does not need the actual
     * children. Clients may be able to implement this more efficiently than
     * <code>getChildren</code>.
     * </p>
     *
     * @param element the element
     * @return <code>true</code> if the given element has children, and
     *         <code>false</code> if it has no children
     */
    @Override
    public boolean hasChildren(final Object element) {
        if (element == null || element instanceof IContainer && !((IContainer) element).isAccessible()) {
            return false;
        }

        if (element instanceof OutlineTreeNode) {
            return ((OutlineTreeNode) element).hasChildren();
        }

        try {
            if (element instanceof IContainer) {
                return ((IContainer) element).members().length > 0;
            }
        } catch (final CoreException e) {
            return false;
        }

        return false;
    }

    /**
     * Disposes of this content provider. This is called by the viewer when it
     * is disposed.
     * <p>
     * The viewer should not be updated during this call, as it is in the
     * process of being disposed.
     * </p>
     */
    @Override
    public void dispose() {
        // ....removeElementChangedListener(this);
    }

    /**
     * Notifies this content provider that the given viewer's input has been
     * switched to a different element. This notification occurs as a result of
     * setInput on the outline control.
     * <p>
     * A typical use for this method is registering the content provider as a
     * listener to changes on the new input (using model-specific means), and
     * deregistering the viewer from the old input. In response to these change
     * notifications, the content provider should update the viewer (see the
     * add, remove, update and refresh methods on the viewers).
     * </p>
     * <p>
     * The viewer should not be updated during this call, as it might be in the
     * process of being disposed.
     * </p>
     *
     * @param viewer the tree viewer
     * @param oldInput the old tree, or <code>null</code> if the viewer did not
     *            previously have an input
     * @param newInput the new tree, or <code>null</code> if the viewer does not
     *            have an input
     */
    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        /* Nothing needed */
    }

    /**
     * A node in the outline tree view
     *
     * @author jthywiss
     */
    public abstract static class OutlineTreeNode {
        /**
         * The list of child Orc content outline tree nodes for this tree node.
         * There should be no <code>null</code> children in the list.
         */
        private final List<OutlineTreeNode> children = new ArrayList<>();

        /**
         * The parent tree node for this Orc content outline tree node. This
         * value may be <code>null</code> if there is no parent.
         */
        private final OutlineTreeNode parent;

        /**
         * Constructs a new Orc content outline tree node.
         *
         * @param parent the parent node of this node. Is <code>null</code> for
         *            the root.
         */
        public OutlineTreeNode(final OutlineTreeNode parent) {
            this.parent = parent;
        }

        /**
         * Returns the child nodes. Empty arrays are converted to
         * <code>null</code> before being returned.
         *
         * @return The child nodes; may be <code>null</code>, but never empty.
         *         There should be no <code>null</code> children in the array.
         */
        public OutlineTreeNode[] getChildren() {
            return children.toArray(new OutlineTreeNode[children.size()]);
        }

        /**
         * Returns the parent node.
         *
         * @return the parent node of this node. Is <code>null</code> for the
         *         root.
         */
        public OutlineTreeNode getParent() {
            return parent;
        }

        /**
         * Returns whether this Orc content outline tree node has any children.
         *
         * @return <code>true</code> if its array of children is not
         *         <code>null</code> and is non-empty; <code>false</code>
         *         otherwise.
         */
        public boolean hasChildren() {
            return !children.isEmpty();
        }

        /**
         * Sets the children for this Orc content outline tree node.
         *
         * @param child the child nodes; may not be <code>null</code>.
         */
        public void addChild(final OutlineTreeNode child) {
            children.add(child);
        }
    }

    /**
     * A node in the outline tree view that corresponds to an IFile
     *
     * @author jthywiss
     */
    public static class OutlineTreeFileNode extends OutlineTreeNode {
        private final IFile file;

        /**
         * Constructs a new Orc content outline tree node.
         *
         * @param parent the parent node of this node. Is <code>null</code> for
         *            the root.
         * @param file the file held by this node
         */
        public OutlineTreeFileNode(final OutlineTreeNode parent, final IFile file) {
            super(parent);
            this.file = file;
        }

        /**
         * Returns the file held by this Orc content outline tree node.
         *
         * @return the file
         */
        public IFile getFile() {
            return file;
        }

    }

    /**
     * A node in the outline tree view that corresponds to an Orc AST node
     *
     * @author jthywiss
     */
    public static class OutlineTreeAstNode extends OutlineTreeNode implements IRegion {
        private final AST astNode;

        /**
         * Constructs a new Orc content outline tree node.
         *
         * @param parent the parent node of this node. Is <code>null</code> for
         *            the root.
         * @param astNode the AST node held by this node
         */
        public OutlineTreeAstNode(final OutlineTreeNode parent, final AST astNode) {
            super(parent);
            this.astNode = astNode;
        }

        /**
         * Returns the AST node held by this Orc content outline tree node.
         *
         * @return the AST node
         */
        public AST getAstNode() {
            return astNode;
        }

        /**
         * Returns the offset of the source code text corresponding to this tree
         * node.
         *
         * @return the offset of the corresponding text or -1 if there is no
         *         valid text information
         */
        @Override
        public int getOffset() {
            if (astNode.sourceTextRange().isEmpty()) {
                return -1;
            }
            return astNode.sourceTextRange().get().start().offset();
        }

        /**
         * Returns the length of the source code text corresponding to this tree
         * node.
         *
         * @return the length of the corresponding text or -1 if there is no
         *         valid text information
         */
        @Override
        public int getLength() {
            if (astNode.sourceTextRange().isEmpty()) {
                return -1;
            }
            return astNode.sourceTextRange().get().end().offset() - astNode.sourceTextRange().get().start().offset();
        }

        protected static void addSubtree(final OutlineTreeNode parent, final AST newSubtree) {
            if (newSubtree == null || newSubtree.sourceTextRange().isEmpty()) {
                return; // No position is confusing to the outline control
            }
            OutlineTreeNode currParent = parent;
            if (shouldShowInOutline(newSubtree)) {
                final OutlineTreeAstNode newNode = new OutlineTreeAstNode(parent, newSubtree);
                parent.addChild(newNode);
                if (createsNewOutlineLevel(newSubtree)) {
                    currParent = newNode;
                }
            }

            for (final AST currChild : JavaConversions.asJavaIterable(newSubtree.subtrees())) {
                addSubtree(currParent, currChild);
            }
        }

        /**
         * @param newSubtree
         * @return
         */
        protected static boolean shouldShowInOutline(final AST newSubtree) {
            /*
             * Don't forget to update OrcLabelProvider.getImageFor and
             * getLabelFor to handle new node types in the outline view.
             */
            return newSubtree instanceof ClassImport || newSubtree instanceof DefDeclaration || newSubtree instanceof Include && ((Include) newSubtree).origin() != null && ((Include) newSubtree).origin().length() > 0 || newSubtree instanceof SiteDeclaration || newSubtree instanceof TypeDeclaration || newSubtree instanceof Val;
        }

        /**
         * @param newSubtree
         * @return
         */
        protected static boolean createsNewOutlineLevel(final AST newSubtree) {
            return newSubtree instanceof DefDeclaration;
        }
    }

    // public Object findNode(final Object ast, final int startOffset, final int
    // endOffset) {
    // Object theWinner = null;
    // if (startOffset < 0 || endOffset < 0) {
    // return null;
    // } else {
    // for (final AST child : JavaConversions.asJavaIterable(((AST)
    // ast).subtrees())) {
    // if (child.pos() instanceof OffsetPosition) {
    // final int childOffset = ((OffsetPosition) child.pos()).offset();
    // if (childOffset >= startOffset && childOffset <= endOffset && (theWinner
    // == null || childOffset <= ((OffsetPosition) ((Positional)
    // theWinner).pos()).offset())) {
    // theWinner = findNode(child, startOffset, endOffset);
    // }
    // }
    // }
    // }
    //
    // return theWinner;
    // }
}
