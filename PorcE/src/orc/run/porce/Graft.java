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

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import orc.run.porce.runtime.CallKindDecision;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.ParallelismController;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.utilities.AssumedValue;

@Introspectable
@ImportStatic(SpecializationConfiguration.class)
public abstract class Graft extends Expression implements ParallelismNode {
    protected final PorcEExecution execution;

    private final AtomicLong executionCount = new AtomicLong(0);
    @SuppressWarnings("boxing")
    private final AssumedValue<Boolean> isParallel =
            new AssumedValue<>("Graft.isParallel", ParallelismController.initiallyParallel());

    @Override
    public boolean isParallelismChoiceNode() {
        return true;
    }

    @Override
    public long getExecutionCount() {
        return executionCount.get();
    }

    @Override
    public void incrExecutionCount() {
        if (isParallelismChoiceNode() && getProfilingScope().isProfiling()) {
            executionCount.incrementAndGet();
        }
    }

    @Override
    @SuppressWarnings("boxing")
    public void setParallel(boolean isParallel) {
        if (isParallel != getParallel()) {
            this.isParallel.set(isParallel);
        }
    }

    @Override
    @SuppressWarnings("boxing")
    public boolean getParallel() {
        return isParallel.get();
    }

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

    protected boolean shouldInlineSpawn(VirtualFrame frame) {
        try {
            PorcEClosure computation = v.executePorcEClosure(frame);
            return shouldInlineSpawn(computation);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new InternalPorcEError(e);
        }
    }

    @Specialization(guards = { "UseControlledParallelism" })
    public Object controlled(final VirtualFrame frame) {
        if (getParallel()) {
            return fullFuture(frame);
        } else {
            return noFuture(frame);
        }
    }

    @Specialization(guards = { "!shouldInlineSpawn(frame)" })
    public Object fullFuture(final VirtualFrame frame) {
        ensureTail(fullFuture);
        incrExecutionCount();
        return fullFuture.execute(frame);
    }

    @Specialization(guards = { "shouldInlineSpawn(frame)" }, replaces = { "fullFuture" })
    public Object noFuture(final VirtualFrame frame) {
        ensureTail(noFuture);
        incrExecutionCount();
        return noFuture.execute(frame);
    }

    // This duplication of "fullFuture" allows this node to specialize to only inline and then switch back to both later
    // by adding this specialization.
    @Specialization()
    public Object fullAfterNoFuture(final VirtualFrame frame) {
        return fullFuture(frame);
    }

    private static final boolean allowSpawnInlining = PorcERuntime.allowSpawnInlining();

    @CompilationFinal
    private CallKindDecision callKindDecision;

    /**
     * @param computation
     * @return the call kind computing it if needed.
     */
    protected CallKindDecision getCallKindDecision(PorcEClosure computation) {
        if (callKindDecision == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            computeAtomicallyIfNull(() -> callKindDecision, (v) -> callKindDecision = v,
                    () -> CallKindDecision.get(this, (PorcERootNode) computation.body.getRootNode()));
        }
        return callKindDecision;
    }

    protected boolean shouldInlineSpawn(final PorcEClosure computation) {
        boolean b;
        if (SpecializationConfiguration.UseExternalCallKindDecision) {
            b = getCallKindDecision(computation) != CallKindDecision.SPAWN;
        } else {
            b = computation.getTimePerCall(targetRootProfile) < SpecializationConfiguration.InlineAverageTimeLimit;
        }
        return allowSpawnInlining && b;
    }

    @SuppressWarnings("boxing")
    @Override
    public Map<String, Object> getDebugProperties() {
        Map<String, Object> properties = super.getDebugProperties();
        properties.put("isParallel", getParallel());
        properties.put("executionCount", getExecutionCount());
        return properties;
    }

    public static Graft create(PorcEExecution execution, Expression v, Expression fullFuture, Expression noFuture) {
        return GraftNodeGen.create(execution, v, fullFuture, noFuture);
    }
}
