//
// DispatchBase.java -- Java class DispatchBase
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import orc.run.porce.NodeBase;
import orc.run.porce.runtime.PorcEExecution;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;

public abstract class DispatchBase extends NodeBase {
    protected final PorcEExecution execution;

    @CompilationFinal
    protected boolean inliningForced;

    protected DispatchBase(PorcEExecution execution) {
        this.execution = execution;
    }

    @SuppressWarnings("boxing")
    public void forceInline() {
        CompilerAsserts.compilationConstant(inliningForced);
        if (inliningForced == false) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            inliningForced = true;
            Iterable<Node> children = getChildren();
            for(Node c : children) {
                if(c instanceof DispatchBase) {
                    ((DispatchBase) c).forceInline();
                }
            }
        }
    }

    protected void ensureForceInline(DispatchBase n) {
        CompilerAsserts.compilationConstant(n);
        if (inliningForced == true) {
            n.forceInline();
        }
    }
}
