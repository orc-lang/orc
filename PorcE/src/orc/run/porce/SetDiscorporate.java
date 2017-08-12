package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import orc.run.porce.runtime.Counter;

@NodeChild(value = "c", type = Expression.class)
public abstract class SetDiscorporate extends Expression {
	@Specialization
	public PorcEUnit spawnBindFuture(Counter c) {
		c.setDiscorporate();
		return PorcEUnit.SINGLETON;
	}

	public static SetDiscorporate create(Expression c) {
		return SetDiscorporateNodeGen.create(c);
	}
}
