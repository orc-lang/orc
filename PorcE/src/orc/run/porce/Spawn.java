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

import orc.run.porce.runtime.CallClosureSchedulable;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.PorcERuntime$;
import orc.run.porce.runtime.Terminator;

import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

@NodeChild(value = "c", type = Expression.class)
@NodeChild(value = "t", type = Expression.class)
@NodeChild(value = "computation", type = Expression.class)
@Introspectable
public abstract class Spawn extends Expression {
    private final boolean mustSpawn;
    private final PorcEExecution execution;

    protected final ConditionProfile moreTasksNeeded = ConditionProfile.createCountingProfile();
    protected final ValueProfile targetRoot = ValueProfile.createIdentityProfile();

    @Child
    protected StackCheckingDispatch dispatch;

    protected Spawn(boolean mustSpawn, PorcEExecution execution) {
	this.mustSpawn = mustSpawn;
	this.execution = execution;
	this.dispatch = StackCheckingDispatch.create(execution);
    }

    @Override
    public void setTail(boolean v) {
	super.setTail(v);
	dispatch.setTail(v);
    }

    @Specialization(guards = { "!shouldInlineSpawn(computation)" } )
    public PorcEUnit spawn(final VirtualFrame frame, final Counter c, final Terminator t, final PorcEClosure computation) {
	final PorcERuntime r = execution.runtime();
        // The incrementAndCheckStackDepth call should not go in shouldInlineSpawn because it has side effects and I don't think we can guarantee that guards are not called multiple times.
        if(!moreTasksNeeded.profile(r.isWorkQueueUnderful(r.minQueueSize())) && dispatch.spawnProfile.profile(r.incrementAndCheckStackDepth())) {
            dispatch.executeInline(frame, computation, true);
        } else {
            t.checkLive();
            execution.runtime().schedule(CallClosureSchedulable.apply(computation, execution));
        }
        return PorcEUnit.SINGLETON;
    }

    @Specialization(guards = { "shouldInlineSpawn(computation)" }, replaces = { "spawn" })
    public PorcEUnit inline(final VirtualFrame frame, final Counter c, final Terminator t, final PorcEClosure computation) {
    	// Here we can inline spawns speculatively if we have not done that too much on this stack.
    	// This is very heuristic and may cause load imbalance problems in some cases.
	final PorcERuntime r = execution.runtime();

	if (r.actuallySchedule()) {
	    dispatch.dispatch(frame, computation);
	} else {
	    dispatch.executeInline(frame, computation, false);
	}
	return PorcEUnit.SINGLETON;
    }

    // This duplication of "spawn" allows this node to specialize to only inline and
    // then switch back to both later by adding this specialization.
    @Specialization()
    public PorcEUnit spawnAfterInline(final VirtualFrame frame, final Object c, final Terminator t, final PorcEClosure computation) {
	return spawn(frame, (Counter) c, t, computation);
    }

    private static final boolean allowSpawnInlining = PorcERuntime$.MODULE$.allowSpawnInlining();
    private static final boolean allowAllSpawnInlining = PorcERuntime$.MODULE$.allowAllSpawnInlining();

    protected boolean shouldInlineSpawn(final PorcEClosure computation) {
		return allowSpawnInlining && (!mustSpawn || allowAllSpawnInlining) &&
			computation.getTimePerCall(targetRoot) < SpecializationConfiguration.InlineAverageTimeLimit;
    }

    public static Spawn create(final Expression c, final Expression t, final boolean mustSpawn, final Expression computation, final PorcEExecution execution) {
	return SpawnNodeGen.create(mustSpawn, execution, c, t, computation);
    }
}

