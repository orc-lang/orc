package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import orc.run.porce.runtime.Future;

@NodeChild(value = "future", type = Expression.class)
@NodeChild(value = "value", type = Expression.class)
public abstract class Bind extends Expression {
	@Specialization
	public PorcEUnit bindFuture(Future future, Object value) {
		future.bind(value);
		return PorcEUnit.SINGLETON;
	}

	public static Bind create(Expression future, Expression value) {
		return BindNodeGen.create(future, value);
	}
}
