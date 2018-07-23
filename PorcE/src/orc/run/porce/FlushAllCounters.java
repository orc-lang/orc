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

import orc.run.extensions.SimpleWorkStealingScheduler;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.Counter.CounterOffset;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

public class FlushAllCounters extends Expression {

    protected final PorcEExecution execution;
    protected final int flushPolarity;

    protected FlushAllCounters(final PorcEExecution execution, final int flushPolarity) {
        this.execution = execution;
        this.flushPolarity = flushPolarity;
    }

    @Specialization
    public PorcEUnit impl(VirtualFrame frame,
            @Cached("create(execution)") StackCheckingDispatch dispatch,
            @Cached("createBinaryProfile()") ConditionProfile hasContinuation) {
        final SimpleWorkStealingScheduler.Worker worker = (SimpleWorkStealingScheduler.Worker)Thread.currentThread();
        Counter.incrFlushAllCount();
        CounterOffset prev = null;
        CounterOffset current = (CounterOffset)worker.counterOffsets();

        while (current != null) {
            if ((flushPolarity > 0 && current.value() >= 0) || (flushPolarity == 0) || (flushPolarity < 0 && current.value() <= 0)) {
                //Logger.info(s"Flushing all ${Thread.currentThread()}: flushing: $current")
                if (prev != null) {
                    Counter.removeNextCounterOffset(prev);
                } else {
                    Counter.removeNextCounterOffset(worker);
                }

                current.counter().flushCounterOffsetAndHandle(current);

//                PorcEClosure c = current.counter().flushCounterOffsetAndHandleOptimized(current);
//
//                if (hasContinuation.profile(c != null)) {
//                    dispatch.executeDispatch(frame, c);
//                }

                // Step to the node that replaced this one.
                if (prev != null) {
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

    public static FlushAllCounters create(final int flushPolarity, final PorcEExecution execution) {
        return FlushAllCountersNodeGen.create(execution, flushPolarity);
    }
}
