//
// FlushAllCounters.java -- Truffle node FlushAllCounters
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

import orc.run.extensions.SimpleWorkStealingScheduler;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.Counter.CounterOffset;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;

@Introspectable
public class FlushAllCounters extends Expression {

    protected final PorcEExecution execution;
    protected final int flushPolarity;

    protected FlushAllCounters(final PorcEExecution execution, final int flushPolarity) {
        this.execution = execution;
        this.flushPolarity = flushPolarity;
    }

    @SuppressWarnings("null")
    @Specialization
    public PorcEUnit impl(VirtualFrame frame,
            @Cached("createCountingProfile()") LoopConditionProfile loopProfile,
            @Cached("createCountingProfile()") ConditionProfile isInterestingProfile,
            @Cached("createBinaryProfile()") ConditionProfile isInListProfile,
            @Cached("create(execution)") StackCheckingDispatch dispatch) {
        final SimpleWorkStealingScheduler.Worker worker = (SimpleWorkStealingScheduler.Worker)Thread.currentThread();
        Counter.incrFlushAllCount();
        CounterOffset prev = null;
        CounterOffset current = (CounterOffset)worker.counterOffsets();

        while (loopProfile.profile(current != null)) {
            boolean isInteresting = (flushPolarity > 0 && current.value() >= 0) ||
                    (flushPolarity == 0) ||
                    (flushPolarity < 0 && current.value() <= 0);
            if (isInterestingProfile.profile(isInteresting)) {
                /*if (flushPolarity < 0) {
                    final CounterOffset c = current;
                    Logger.info(() -> "Flushing all " + Thread.currentThread() + ": flushing: " + c);
                }*/
                if (isInListProfile.profile(prev != null)) {
                    Counter.removeNextCounterOffset(prev);
                } else {
                    Counter.removeNextCounterOffset(worker);
                }
                Counter.markCounterOffsetRemoved(current);

                PorcEClosure c = current.counter().flushCounterOffsetAndHandleOptimized(current);

                // Check flushPolarity here since if we are only flushing positive offsets then c can never be true.
                if (flushPolarity <= 0 && c != null) {
                    // This compiles to deopt if it has never been reached, so no reason to profile here.
                    dispatch.executeDispatch(frame, c);
                }

                // Step to the node that replaced this one.
                if (isInListProfile.profile(prev != null)) {
                    current = prev.nextCounterOffset();
                } else {
                    current = (CounterOffset)worker.counterOffsets();
                }
            } else {
                prev = current;
                current = current.nextCounterOffset();
            }
        }

        return PorcEUnit.SINGLETON;
    }

    @Override
    public Map<String, Object> getDebugProperties() {
        Map<String, Object> properties = super.getDebugProperties();
        properties.put("flushPolarity", flushPolarity);
        return properties;
    }

    public static FlushAllCounters create(final int flushPolarity, final PorcEExecution execution) {
        return FlushAllCountersNodeGen.create(execution, flushPolarity);
    }
}
