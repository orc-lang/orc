//
// NodeBase.java -- Truffle abstract node class NodeBase
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import scala.Option;

import orc.ast.ASTWithIndex;
import orc.ast.porc.PorcAST;
import orc.run.porce.instruments.ProfiledPorcENodeTag;
import orc.run.porce.instruments.ProfiledPorcNodeTag;
import orc.run.porce.instruments.TailTag;
import orc.run.porce.runtime.SourceSectionFromPorc;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

public abstract class NodeBase extends Node implements HasPorcNode, NodeBaseInterface {
    @CompilationFinal
    private RootNode rootNode = null;

    public RootNode getCachedRootNode() {
        if (rootNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rootNode = getRootNode();
        }
        return rootNode;
    }

    /**
     * @return The PorcE root node containing this node.
     */
    @Override
    public ProfilingScope getProfilingScope() {
        if (this instanceof ProfilingScope) {
            return (ProfilingScope) this;
        } else {
            return getProfilingScopeHelper(this.getParent());
        }
    }

    private static ProfilingScope getProfilingScopeHelper(Node n) {
        if (n == null) {
            return ProfilingScope.DISABLED;
        } else if (n instanceof NodeBaseInterface) {
            return ((NodeBaseInterface) n).getProfilingScope();
        } else {
            Node parent = n.getParent();
            return getProfilingScopeHelper(parent);
        }
    }

    @Override
    public String getContainingPorcCallableName() {
        return getContainingPorcCallableNameHelper(this);
    }

    private static String getContainingPorcCallableNameHelper(Node n) {
        Node parent = n.getParent();
        if(parent instanceof NodeBaseInterface) {
            return ((NodeBaseInterface)parent).getContainingPorcCallableName();
        } else {
            return getContainingPorcCallableNameHelper(parent);
        }
    }

    @CompilationFinal
    private Option<PorcAST.Z> porcNode = Option.apply(null);

    @Override
    public void setPorcAST(final PorcAST.Z ast) {
        CompilerAsserts.neverPartOfCompilation();
        porcNode = Option.apply(ast);
        section = SourceSectionFromPorc.apply(ast);
        getChildren().forEach((n) -> {
            if (n instanceof NodeBase) {
                final NodeBase e = (NodeBase) n;
                if (e.porcNode().isEmpty()) {
                    e.setPorcAST(ast);
                }
            }
        });
    }

    @Override
    public Option<PorcAST.Z> porcNode() {
        return porcNode;
    }

    @CompilationFinal
    private SourceSection section = null;

    @Override
    public SourceSection getSourceSection() {
        return section;
    }

    @SuppressWarnings("boxing")
    @Override
    public Map<String, Object> getDebugProperties() {
        Map<String, Object> properties = super.getDebugProperties();
        if (isTail) {
            properties.put("tail", true);
        }
        if (section != null) {
            properties.put("sourceSection", section);
        }
        if (porcNode.isDefined()) {
            Object nodePrefixStub = new PorcASTDebugStub((PorcAST)porcNode.get().value());
            properties.put("porcNode", nodePrefixStub);
        }
        return properties;
    }

    /**
     * Wrapper class for PorcAST to perform the correct toString conversion when needed.
     *
     * @author amp
     */
    private static final class PorcASTDebugStub {
        private final PorcAST porcAST;

        public PorcASTDebugStub(PorcAST porcAST) {
            this.porcAST = porcAST;
        }

        @Override
        public String toString() {
            return porcAST.prettyprintWithoutNested();
        }
    }

    @Override
    protected void onReplace(final Node newNode, final CharSequence reason) {
        if (newNode instanceof PorcENode) {
            PorcENode n = (PorcENode) newNode;
            if (porcNode().isDefined()) {
                n.setPorcAST(porcNode().get());
            }
            n.setTail(n.isTail);
        }
        super.onReplace(newNode, reason);
    }

    @Override
    public Node copy() {
        Node n = super.copy();
        // ((NodeBase)n).porcNode = Option.apply(null);
        return n;
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == TailTag.class) {
            return isTail;
        } else if (tag == ProfiledPorcNodeTag.class) {
            return porcNode().isDefined() && ProfiledPorcNodeTag.isProfiledPorcNode((PorcAST)porcNode().get().value());
        } else if (tag == ProfiledPorcENodeTag.class) {
            return ProfiledPorcENodeTag.isProfiledPorcENode(this);
        } else {
            return super.isTaggedWith(tag);
        }
    }

    @CompilationFinal
    protected boolean isTail = false;

    public void setTail(boolean v) {
        isTail = v;
    }

    protected void ensureTail(NodeBase n) {
        CompilerDirectives.interpreterOnly(() -> {
            if (n.isTail != isTail) {
                n.setTail(isTail);
            }
        });
    }

    @CompilerDirectives.CompilationFinal
    private int callSiteId = -1;

    protected int getCallSiteId() {
        CompilerAsserts.compilationConstant(this);
        if (callSiteId >= 0) {
            return callSiteId;
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callSiteId = findCallSiteId(this);
            return callSiteId;
        }
    }

    /**
     * Climb the Truffle AST searching for a node with a PorcAST with an index.
     */
    private int findCallSiteId(final Node e) {
        if (e instanceof HasPorcNode) {
            HasPorcNode pn = (HasPorcNode) e;
            if (pn.porcNode().isDefined()) {
                final PorcAST ast = (PorcAST) pn.porcNode().get().value();
                if (ast instanceof ASTWithIndex && ((ASTWithIndex) ast).optionalIndex().isDefined()) {
                    return ((Integer) ((ASTWithIndex) ast).optionalIndex().get()).intValue();
                }
            }
        }
        final Node p = e.getParent();
        if (p instanceof NodeBase) {
            return ((NodeBase) p).getCallSiteId();
        } else if (p != null) {
            return findCallSiteId(p);
        }
        return -1;
    }

    @SuppressWarnings("boxing")
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        Map<String, Object> properties = getDebugProperties();
        boolean hasProperties = false;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            sb.append(hasProperties ? "," : "<");
            hasProperties = true;
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        if (hasProperties) {
            sb.append(">");
        }
        sb.append(String.format("@%08x", hashCode()));
        return sb.toString();
    }

    /**
     * Compute a value if it is currently null. This operation is performed atomically w.r.t. the RootNode of this
     * Truffle AST.
     *
     * This function should never be called from Truffle compiled code. For some reason, it causes a really opaque
     * truffle error (a FrameWithoutBoxing is materialized, but it's not clear why).
     *
     * @param read
     *            a function to get the current value.
     * @param write
     *            a function to store a computed value.
     * @param compute
     *            a function to compute the value when needed.
     */
    protected <T> void computeAtomicallyIfNull(Supplier<T> read, Consumer<T> write, Supplier<T> compute) {
        CompilerAsserts
                .neverPartOfCompilation("computeAtomicallyIfNull is called from compiled code");
        atomic(() -> {
            if (read.get() == null) {
                T v = compute.get();
                // TODO: Use the new Java 9 fence when we start requiring Java 9
                // for PorcE.
                UNSAFE.fullFence();
                write.accept(v);
            }
        });
    }

    protected static sun.misc.Unsafe UNSAFE;
    static {
        try {
            java.lang.reflect.Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
