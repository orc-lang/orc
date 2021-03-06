//
// Bind.java -- Truffle node Bind
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.FutureReader;
import orc.run.porce.call.Dispatch;
import orc.run.porce.profiles.MaximumValueProfile;
import orc.run.porce.runtime.CallClosureSchedulable;
import orc.run.porce.runtime.Future;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcEFutureReader;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

@NodeChild(value = "future", type = Expression.class)
@NodeChild(value = "value", type = Expression.class)
@Introspectable
public abstract class Bind extends Expression {
    protected final PorcEExecution execution;

    protected Bind(final PorcEExecution execution) {
      this.execution = execution;
    }

    protected static boolean exactlyFuture(Future f) {
      return f != null && f.getClass() == Future.class;
    }

    @Specialization(guards = { "exactlyFuture(_future)" })
    public PorcEUnit bindExactlyFuture(VirtualFrame frame, final Future _future, final Object value,
        @Cached("create()") MaximumValueProfile readersLengthProfile,
        @Cached("createBinaryProfile()") ConditionProfile lastBindProfile,
        @Cached("createCallReaderPublish()") CallReaderPublish callPublish) {
        final Future future = CompilerDirectives.castExact(_future, Future.class);
        FutureReader[] readers = future.fastLocalBind(value);
        if (lastBindProfile.profile(readers != null)) {
            @SuppressWarnings("null")
            int max = readersLengthProfile.max(readers.length);
            if (max <= 8) {
              callAllPublishExplode(frame, value, callPublish, readers, max);
            } else {
              callAllPublishLoop(frame, value, callPublish, readers);
            }
        }
        return PorcEUnit.SINGLETON;
    }

    @SuppressWarnings("boxing")
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
    private void callAllPublishExplode(VirtualFrame frame, final Object value, CallReaderPublish callPublish, FutureReader[] readers, int max) {
      CompilerAsserts.compilationConstant(max);
      int i = 0;
      while (i < max && i < readers.length && readers[i] != null) {
        FutureReader r = readers[i];
        callPublish.execute(frame, i, r, value);
        i += 1;
      }
    }

    private void callAllPublishLoop(VirtualFrame frame, final Object value, CallReaderPublish callPublish, FutureReader[] readers) {
      int i = 0;
      while (i < readers.length && readers[i] != null) {
        FutureReader r = readers[i];
        callPublish.execute(frame, i, r, value);
        i += 1;
      }
    }

    @Specialization()
    public PorcEUnit bindFuture(final Future future, final Object value) {
        future.bind(value);
        return PorcEUnit.SINGLETON;
    }

    public static Bind create(final Expression future, final Expression value, final PorcEExecution execution) {
        return BindNodeGen.create(execution, future, value);
    }

    protected CallReaderPublish createCallReaderPublish() {
        return BindNodeGen.CallReaderPublishNodeGen.create(execution);
    }

    @Introspectable
    public static abstract class CallReaderPublish extends Node {
        protected final PorcEExecution execution;

        protected CallReaderPublish(final PorcEExecution execution) {
          this.execution = execution;
        }

        public abstract void execute(VirtualFrame frame, int index, final FutureReader reader, final Object value);

	private void porce(VirtualFrame frame, final PorcEFutureReader reader, final Object value,
		ValueProfile readerClassProfile, Dispatch dispatch, ConditionProfile inlineProfile,
		BranchProfile callProfile) {
            CallClosureSchedulable call = readerClassProfile.profile(reader).fastPublish(value);
            if (call != null) {
                callProfile.enter();
                dispatch.executeDispatchWithEnvironment(frame, call.closure(),
                        call.arguments() != null ? call.arguments() : new Object[] { null });
            }
	}

        @Specialization(guards = {"index == cachedIndex"}, limit = "4")
        public void porceSeparateCaches(VirtualFrame frame, int index, final PorcEFutureReader reader, final Object value,
            @Cached("index") int cachedIndex,
            @Cached("createClassProfile()") ValueProfile readerClassProfile,
            @Cached("createInternal(execution)") Dispatch dispatch,
            @Cached("createBinaryProfile()") ConditionProfile inlineProfile,
            @Cached("create()") BranchProfile callProfile) {
            porce(frame, reader, value, readerClassProfile, dispatch, inlineProfile, callProfile);
        }

        @Specialization(replaces = "porceSeparateCaches")
        public void porceSharedCache(VirtualFrame frame, int index, final PorcEFutureReader reader, final Object value,
            @Cached("createClassProfile()") ValueProfile readerClassProfile,
            @Cached("createInternal(execution)") Dispatch dispatch,
            @Cached("createBinaryProfile()") ConditionProfile inlineProfile,
            @Cached("create()") BranchProfile callProfile) {
            porce(frame, reader, value, readerClassProfile, dispatch, inlineProfile, callProfile);
        }

        @Specialization
        public void orc(int index, final FutureReader reader, final Object value) {
            reader.publish(value);
        }
    }
}
