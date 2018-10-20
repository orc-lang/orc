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

import orc.run.porce.runtime.CallClosureSchedulable;
import orc.run.porce.runtime.CallKindDecision;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.PorcERuntime$;
import orc.run.porce.runtime.Terminator;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
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
    protected StackCheckingDispatch dispatch;

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
        this.dispatch = StackCheckingDispatch.create(execution);
        this.dispatch.forceInline();
    }

    @Override
    public void setTail(boolean v) {
        super.setTail(v);
        dispatch.setTail(v);
    }

    @Specialization(guards = { "!shouldInline" })
    public PorcEUnit spawn(final VirtualFrame frame, final Counter c, final Terminator t,
            final PorcEClosure computation,
            @Cached("shouldInlineSpawn(computation)") boolean shouldInline) {
        final PorcERuntime r = execution.runtime();
        // The incrementAndCheckStackDepth call should not go in shouldInlineSpawn because it has side effects and
        // we can't guarantee that guards are not called multiple times.
        t.checkLive();
        if (!moreTasksNeeded.profile(r.isWorkQueueUnderful(r.minQueueSize()))) {
            dispatch.dispatch(frame, computation);
        } else {
            addCalledRoot(computation.body);
            execution.runtime().schedule(CallClosureSchedulable.apply(computation, execution));
        }
        return PorcEUnit.SINGLETON;
    }

    @Specialization(guards = { "shouldInline" }, replaces = { "spawn" })
    public PorcEUnit inline(final VirtualFrame frame, final Counter c, final Terminator t,
            final PorcEClosure computation,
            @Cached("shouldInlineSpawn(computation)") boolean shouldInline) {
        // Here we can inline spawns speculatively if we have not done that too much on this stack.
        // This is very heuristic and may cause load imbalance problems in some cases.
        final PorcERuntime r = execution.runtime();

        if (r.actuallySchedule()) {
            dispatch.dispatch(frame, computation);
        } else {
            dispatch.executeInline(frame, computation);
        }
        return PorcEUnit.SINGLETON;
    }

    // This duplication of "spawn" allows this node to specialize to only inline and
    // then switch back to both later by adding this specialization.
    @Specialization()
    public PorcEUnit spawnAfterInline(final VirtualFrame frame, final Object c, final Terminator t,
            final PorcEClosure computation) {
        return spawn(frame, (Counter) c, t, computation, false);
    }

    private static final boolean allowSpawnInlining = PorcERuntime$.MODULE$.allowSpawnInlining();
    private static final boolean allowAllSpawnInlining = PorcERuntime$.MODULE$.allowAllSpawnInlining();

    protected boolean shouldInlineSpawn(final PorcEClosure computation) {
        return shouldInlineSpawn(this, targetRootProfile, computation);
    }

    static boolean shouldInlineSpawn(NodeBase self, ValueProfile targetRootProfile, final PorcEClosure computation) {
        if (SpecializationConfiguration.UseExternalCallKindDecision) {
            CompilerAsserts.neverPartOfCompilation();
            CallKindDecision decision = CallKindDecision.get(self, (PorcERootNode) computation.body.getRootNode());
            return allowSpawnInlining &&
                    decision != CallKindDecision.SPAWN;
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
