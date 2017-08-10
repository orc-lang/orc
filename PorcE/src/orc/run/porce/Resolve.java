package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.nodes.Node.Child;

import orc.run.porce.Force.Finish;
import orc.run.porce.Force.Future;
import orc.run.porce.Force.New;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.Terminator;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.Resolver;

public class Resolve extends Expression {
	public static boolean isNonFuture(Object v) {
		return !(v instanceof orc.Future);
	}

	@NodeChild(value = "p", type = Expression.class)
	@NodeChild(value = "c", type = Expression.class)
	@NodeChild(value = "t", type = Expression.class)
	@NodeField(name = "nFutures", type = int.class)
	@NodeField(name = "execution", type = PorcEExecutionRef.class)
	public static class New extends Expression {
		@Specialization
		public Object run(int nFutures, PorcEExecutionRef execution, PorcEClosure p, Counter c, Terminator t) {
			return new Resolver(p, c, t, nFutures, execution.get().runtime());
		}

		public static New create(Expression p, Expression c, Expression t, int nFutures, PorcEExecutionRef execution) {
			return ResolveFactory.NewNodeGen.create(p, c, t, nFutures, execution);
		}
	}

	@NodeChild(value = "join", type = Expression.class)
	@NodeChild(value = "future", type = Expression.class)
	@NodeField(name = "index", type = int.class)
	@ImportStatic({ Force.class })
	public static class Future extends Expression {
		@Specialization(guards = { "isNonFuture(future)" })
		public PorcEUnit nonFuture(int index, Resolver join, Object future) {
			join.set(index);
			return PorcEUnit.SINGLETON;
		}

		// TODO: PERFORMANCE: It may be worth playing with specializing by
		// future states. Futures that are always bound may be common.
		@Specialization
		public PorcEUnit porceFuture(int index, Resolver join, orc.run.porce.runtime.Future future) {
			join.force(index, future);
			return PorcEUnit.SINGLETON;
		}

		@Specialization(replaces = { "porceFuture" })
		public PorcEUnit unknown(int index, Resolver join, orc.Future future) {
			join.force(index, future);
			return PorcEUnit.SINGLETON;
		}

		public static Future create(Expression join, Expression future, int index) {
			return ResolveFactory.FutureNodeGen.create(join, future, index);
		}
	}

	@NodeChild(value = "join", type = Expression.class)
	@NodeField(name = "execution", type = PorcEExecutionRef.class)
	public static abstract class Finish extends Expression {
		volatile static int count = 0;
		@Child
		InternalArgArrayCallBase call = null;

		@Specialization(guards = { "join.isResolved()" })
		public PorcEUnit resolved(VirtualFrame frame, PorcEExecutionRef execution, Resolver join) {
			if (call == null) {
				CompilerDirectives.transferToInterpreterAndInvalidate();
				call = insert(InternalArgArrayCall.create(execution));
			}
			call.execute(frame, join.p(), new Object[] { null });
			return PorcEUnit.SINGLETON;
		}

		@Specialization(guards = { "join.isBlocked()" })
		public PorcEUnit blocked(PorcEExecutionRef execution, Resolver join) {
			join.finish();
			return PorcEUnit.SINGLETON;
		}

		public static Finish create(Expression join, PorcEExecutionRef execution) {
			return ResolveFactory.FinishNodeGen.create(join, execution);
		}
	}
}
