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
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import org.graalvm.collections.Pair;

@NodeChild(value = "c", type = Expression.class)
@NodeChild(value = "t", type = Expression.class)
@NodeChild(value = "computation", type = Expression.class)
@Introspectable
public abstract class Spawn extends Expression implements HasCalledRoots {
    private final boolean mustSpawn;
    private final PorcEExecution execution;

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

    @Specialization(guards = { "!shouldInlineSpawn(computation)" })
    public PorcEUnit spawn(final VirtualFrame frame, final Counter c, final Terminator t,
            final PorcEClosure computation) {
        final PorcERuntime r = execution.runtime();
        t.checkLive();
        if (!moreTasksNeeded.profile(r.isWorkQueueUnderful(r.minQueueSize()))) {
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
        if (SpecializationConfiguration.UseExternalCallKindDecision) {
            return allowSpawnInlining &&
                    getCallKindDecision(computation) != CallKindDecision.SPAWN;
        } else {
            return allowSpawnInlining &&
                    computation.getTimePerCall(targetRootProfile) < SpecializationConfiguration.InlineAverageTimeLimit;
        }
    }

    public static Spawn create(final Expression c, final Expression t, final boolean mustSpawn,
            final Expression computation, final PorcEExecution execution) {
        return SpawnNodeGen.create(mustSpawn, execution, c, t, computation);
    }
}
