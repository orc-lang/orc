//
// Graft.java -- Truffle node Graft
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import java.util.concurrent.atomic.AtomicBoolean;

import orc.ast.porc.Variable;
import orc.compiler.porce.PorcToPorcE;
import orc.error.runtime.HaltException;
import orc.run.porce.call.Call;
import orc.run.porce.runtime.Future;
import orc.run.porce.runtime.KilledException;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.PorcERuntime$;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

@Introspectable
public abstract class Graft extends Expression {
    protected static final boolean TRUE = true;

    protected final PorcEExecution execution;

    protected final ValueProfile targetRootProfile = ValueProfile.createIdentityProfile();

    @Child
    protected Expression fullFuture;
    @Child
    protected Expression noFuture;
    @Child
    protected Expression v;

    protected Graft(PorcEExecution execution, Expression v, Expression fullFuture, Expression noFuture) {
        this.execution = execution;
        this.v = v;
        this.fullFuture = fullFuture;
        this.noFuture = noFuture;
    }

    protected boolean shouldInlineSpawn(final PorcEClosure computation) {
        return Spawn.shouldInlineSpawn(this, targetRootProfile, computation);
    }

    protected boolean shouldInlineSpawn(VirtualFrame frame) {
        try {
            PorcEClosure computation = v.executePorcEClosure(frame);
            return shouldInlineSpawn(computation);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new InternalPorcEError(e);
        }
    }

    @Specialization(guards = { "!shouldInlineSpawn" })
    public Object fullFuture(final VirtualFrame frame,
            @Cached("shouldInlineSpawn(frame)") boolean shouldInlineSpawn) {
        ensureTail(fullFuture);
        return fullFuture.execute(frame);
    }

    @Specialization(guards = { "shouldInlineSpawn" }, replaces = { "fullFuture" })
    public Object noFuture(final VirtualFrame frame,
            @Cached("shouldInlineSpawn(frame)") boolean shouldInlineSpawn) {
        ensureTail(noFuture);
        return noFuture.execute(frame);
    }

    // This duplication of "fullFuture" allows this node to specialize to only inline and then switch back to both later
    // by adding this specialization.
    @Specialization()
    public Object fullAfterNoFuture(final VirtualFrame frame) {
        return fullFuture(frame, false);
    }

    protected PorcERootNode getPorcERootNode() {
        return (PorcERootNode) Graft.this.getCachedRootNode();
    }

    public static Graft create(PorcEExecution execution, Expression v, Expression fullFuture, Expression noFuture) {
        return GraftNodeGen.create(execution, v, fullFuture, noFuture);
    }
}
