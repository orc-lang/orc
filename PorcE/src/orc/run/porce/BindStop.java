package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import orc.run.porce.runtime.Future;

@NodeChild(value = "future", type = Expression.class)
public abstract class BindStop extends Expression {
	@Specialization
	public PorcEUnit bindFutureStop(Future future) {
		future.stop();
		return PorcEUnit.SINGLETON;
	}

	public static BindStop create(Expression future) {
		return BindStopNodeGen.create(future);
	}
}
