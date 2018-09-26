//
// CalledRootsProfile.java -- Scala class/trait/object CalledRootsProfile
// Project PorcE
//
// Created by amp on Sep 25, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.nodes.RootNode;
import org.graalvm.collections.Pair;

/**
 *
 *
 * @author amp
 */
public class CalledRootsProfile {

    private Set<PorcERootNode> calledRoots = null;

    private synchronized void initCalledRoots() {
        // This is known racy. The worst case is that a few initial entries will
        // be lost due to duplicate sets being allocated and replaced. The synchronized
        // just makes the race a little less likely, but doesn't prevent it.
        if (calledRoots == null) {
            calledRoots = ConcurrentHashMap.<PorcERootNode>newKeySet();
        }
    }

    public static Set<Pair<NodeBase, PorcERootNode>> getAllCalledRoots(Node self) {
        CompilerAsserts.neverPartOfCompilation(
                "getAllCalledRoots should only be called during reoptimization or profile dumping");
        Set<Pair<NodeBase, PorcERootNode>> ret = new HashSet<>();
        self.accept(new NodeVisitor() {
            @Override
            public boolean visit(Node node) {
                if (node instanceof HasCalledRoots) {
                    ((HasCalledRoots) node).getCalledRootsProfile().collectCalledRoots((NodeBase) node, ret);
                }
                return true;
            }
        });
        return ret;
    }

    public void addCalledRoot(HasCalledRoots self, CallTarget t) {
        if (self.getProfilingScope().isProfiling() && t instanceof RootCallTarget) {
            addCalledRoot(self, ((RootCallTarget) t).getRootNode());
        }
    }

    public void addCalledRoot(HasCalledRoots self, RootNode r) {
        if (self.getProfilingScope().isProfiling() && r instanceof PorcERootNode) {
            addCallRootImpl((PorcERootNode) r);
        }
    }

    @TruffleBoundary
    public void addCallRootImpl(PorcERootNode r) {
        if (calledRoots == null) {
            initCalledRoots();
        }
        calledRoots.add(r);
    }

    /**
     * @param node
     * @param set
     */
    public void collectCalledRoots(NodeBase node, Set<Pair<NodeBase, PorcERootNode>> set) {
        if (calledRoots != null) {
            for (PorcERootNode root : calledRoots) {
                set.add(Pair.create(node, root));
            }
        }
    }

}
