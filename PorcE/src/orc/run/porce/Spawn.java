//
// Spawn.java -- Truffle node Spawn
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
import java.util.Set;

import orc.run.porce.call.Dispatch;
import orc.run.porce.runtime.CallClosureSchedulable;
import orc.run.porce.runtime.CallKindDecision;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.Terminator;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.utilities.AssumedValue;
import org.graalvm.collections.Pair;

@NodeChild(value = "c", type = Expression.class)
@NodeChild(value = "t", type = Expression.class)
@NodeChild(value = "computation", type = Expression.class)
@Introspectable
@ImportStatic(SpecializationConfiguration.class)
public abstract class Spawn extends Expression implements HasCalledRoots, ParallelismNode {
    private final boolean mustSpawn;
    private final PorcEExecution execution;

    private volatile long executionCount = 0;
    @SuppressWarnings("boxing")
    private final AssumedValue<Boolean> isParallel =
            new AssumedValue<Boolean>("Spawn.isParallel", SpecializationConfiguration.InitiallyParallel);

    @Override
    public boolean isParallelismChoiceNode() {
        return !mustSpawn;
    }

    @Override
    public long getExecutionCount() {
        return executionCount;
    }

    @Override
    public void incrExecutionCount() {
        if (isParallelismChoiceNode() && getProfilingScope().isProfiling()) {
            executionCount++;
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

    protected final ConditionProfile moreTasksNeeded = ConditionProfile.createCountingProfile();
    protected final ValueProfile targetRootProfile = ValueProfile.createIdentityProfile();

    @Child
    protected Dispatch dispatch;

    private final CalledRootsProfile calledRootsProfile = new CalledRootsProfile();

    @Override
    public void addCalledRoot(CallTarget t) {
        calledRootsProfile.addCalledRoot(this, t);
    }

    @Override
    public CalledRootsProfile getCalledRootsProfile() {
        return calledRootsProfile;
    }

    @Override
    public Set<Pair<HasCalledRoots, PorcERootNode>> getAllCalledRoots() {
        return CalledRootsProfile.getAllCalledRoots(this);
    }

    @Override
    public long getTotalCalls() {
        return calledRootsProfile.getTotalCalls();
    }

    @Override
    public boolean isScheduled() {
        return true;
    }

    protected Spawn(boolean mustSpawn, PorcEExecution execution) {
        this.mustSpawn = mustSpawn;
        this.execution = execution;
        this.dispatch = Dispatch.createInternal(execution);
        this.dispatch.forceInline();
    }

    @Override
    public void setTail(boolean v) {
        super.setTail(v);
        dispatch.setTail(v);
    }

    @Specialization(guards = { "UseControlledParallelism", "isParallelismChoiceNode()" })
    public PorcEUnit controlled(final VirtualFrame frame, final Counter c, final Terminator t,
            final PorcEClosure computation) {
        if (getParallel()) {
            return spawn(frame, c, t, computation);
        } else {
            return inline(frame, c, t, computation);
        }
    }

    @Specialization(guards = { "!shouldInlineSpawn(computation)" })
    public PorcEUnit spawn(final VirtualFrame frame, final Counter c, final Terminator t,
            final PorcEClosure computation) {
        final PorcERuntime r = execution.runtime();
        t.checkLive();
        incrExecutionCount();
        if (!moreTasksNeeded.profile(r.isWorkQueueUnderful(PorcERuntime.minQueueSize()))) {
            dispatch.dispatch(frame, computation);
        } else {
            addCalledRoot(computation.body);
            execution.runtime().schedule(CallClosureSchedulable.apply(computation, execution));
        }
        return PorcEUnit.SINGLETON;
    }

    @Specialization(guards = { "shouldInlineSpawn(computation)" }, replaces = { "spawn" })
    public PorcEUnit inline(final VirtualFrame frame, final Counter c, final Terminator t,
            final PorcEClosure computation) {
        incrExecutionCount();
        dispatch.dispatch(frame, computation);
        return PorcEUnit.SINGLETON;
    }

    // This duplication of "spawn" allows this node to specialize to only inline and
    // then switch back to both later by adding this specialization.
    @Specialization()
    public PorcEUnit spawnAfterInline(final VirtualFrame frame, final Object c, final Terminator t,
            final PorcEClosure computation) {
        return spawn(frame, (Counter) c, t, computation);
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
        return allowSpawnInlining && !mustSpawn && b;
    }

    @SuppressWarnings("boxing")
    @Override
    public Map<String, Object> getDebugProperties() {
        Map<String, Object> properties = super.getDebugProperties();
        if (isParallelismChoiceNode()) {
            properties.put("isParallel", getParallel());
            properties.put("executionCount", getExecutionCount());
        }
        return properties;
    }

    public static Spawn create(final Expression c, final Expression t, final boolean mustSpawn,
            final Expression computation, final PorcEExecution execution) {
        return SpawnNodeGen.create(mustSpawn, execution, c, t, computation);
    }
}
