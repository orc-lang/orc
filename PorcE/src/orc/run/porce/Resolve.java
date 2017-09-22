
package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import orc.run.porce.call.Dispatch;
import orc.run.porce.call.InternalCPSDispatch;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.Resolver;
import orc.run.porce.runtime.Terminator;

public class Resolve extends Expression {
    public static boolean isNonFuture(final Object v) {
        return !(v instanceof orc.Future);
    }

    @NodeChild(value = "p", type = Expression.class)
    @NodeChild(value = "c", type = Expression.class)
    @NodeChild(value = "t", type = Expression.class)
    @NodeField(name = "nFutures", type = int.class)
    @NodeField(name = "execution", type = PorcEExecutionRef.class)
    public static class New extends Expression {
        @Specialization
        public Object run(final int nFutures, final PorcEExecutionRef execution, final PorcEClosure p, final Counter c, final Terminator t) {
            return new Resolver(p, c, t, nFutures, execution.get().runtime());
        }

        public static New create(final Expression p, final Expression c, final Expression t, final int nFutures, final PorcEExecutionRef execution) {
            return ResolveFactory.NewNodeGen.create(p, c, t, nFutures, execution);
        }
    }

    @NodeChild(value = "join", type = Expression.class)
    @NodeChild(value = "future", type = Expression.class)
    @NodeField(name = "index", type = int.class)
    @ImportStatic({ Force.class })
    public static class Future extends Expression {
        @Specialization(guards = { "isNonFuture(future)" })
        public PorcEUnit nonFuture(final int index, final Resolver join, final Object future) {
            join.set(index);
            return PorcEUnit.SINGLETON;
        }

        // TODO: PERFORMANCE: It may be worth playing with specializing by
        // future states. Futures that are always bound may be common.
        @Specialization
        public PorcEUnit porceFuture(final int index, final Resolver join, final orc.run.porce.runtime.Future future) {
            join.force(index, future);
            return PorcEUnit.SINGLETON;
        }

        @Specialization(replaces = { "porceFuture" })
        public PorcEUnit unknown(final int index, final Resolver join, final orc.Future future) {
            join.force(index, future);
            return PorcEUnit.SINGLETON;
        }

        public static Future create(final Expression join, final Expression future, final int index) {
            return ResolveFactory.FutureNodeGen.create(join, future, index);
        }
    }

    @NodeChild(value = "join", type = Expression.class)
    @NodeField(name = "execution", type = PorcEExecutionRef.class)
    public static abstract class Finish extends Expression {
        volatile static int count = 0;
        @Child
        Dispatch call = null;

        @Specialization(guards = { "join.isResolved()" })
        public PorcEUnit resolved(final VirtualFrame frame, final PorcEExecutionRef execution, final Resolver join) {
        	if (call == null) {
        		CompilerDirectives.transferToInterpreterAndInvalidate();
	        	computeAtomicallyIfNull(() -> call, (v) -> call = v, () -> {
	        		Dispatch n = insert(InternalCPSDispatch.create(execution, isTail));
	        		n.setTail(isTail);
	        		return n;
	        	});
        	}
            call.executeDispatch(frame, join.p(), new Object[] {});
            return PorcEUnit.SINGLETON;
        }

        @Specialization(guards = { "join.isBlocked()" })
        public PorcEUnit blocked(final PorcEExecutionRef execution, final Resolver join) {
            join.finish();
            return PorcEUnit.SINGLETON;
        }

        public static Finish create(final Expression join, final PorcEExecutionRef execution) {
            return ResolveFactory.FinishNodeGen.create(join, execution);
        }
    }
}
