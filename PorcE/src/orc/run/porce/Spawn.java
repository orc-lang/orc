package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;

import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.Terminator;

@NodeField(name="execution", type=PorcEExecution.class)
@NodeChild(value="c", type=Expression.class)
@NodeChild(value="t", type=Expression.class)
@NodeChild(value="computation", type=Expression.class)
public abstract class Spawn extends Expression {
	@Specialization
	public PorcEUnit spawnBindFuture(PorcEExecution execution, Counter c, Terminator t, PorcEClosure computation) {
	    t.checkLive();
	    execution.runtime().spawn(c, computation);
	    return PorcEUnit.SINGLETON;
	}
	
	public static Spawn create(Expression c, Expression t, Expression computation, PorcEExecution execution) {
		return SpawnNodeGen.create(c, t, computation, execution);
	}
}
