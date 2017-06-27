package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;

import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.Future;
import orc.run.porce.runtime.Terminator;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;

@NodeField(name="execution", type=PorcEExecution.class)
@NodeChild(value="future", type=Expression.class)
@NodeChild(value="c", type=Expression.class)
@NodeChild(value="t", type=Expression.class)
@NodeChild(value="computation", type=Expression.class)
public abstract class SpawnBindFuture extends Expression {
	@Specialization
	public PorcEUnit spawnBindFuture(PorcEExecution execution, Future future, Counter c, Terminator t, PorcEClosure computation) {
	    t.checkLive();
	    execution.runtime().spawnBindFuture(future, c, computation);
	    return PorcEUnit.SINGLETON;
	}
	
	public static SpawnBindFuture create(Expression future, Expression c, Expression t, Expression computation, PorcEExecution execution) {
		return SpawnBindFutureNodeGen.create(future, c, t, computation, execution);
	}
}
