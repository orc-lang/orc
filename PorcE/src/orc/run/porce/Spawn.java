package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;

import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.Terminator;

@NodeField(name="execution", type=PorcERuntime.class)
@NodeChild(value="c", type=Expression.class)
@NodeChild(value="t", type=Expression.class)
@NodeChild(value="computation", type=Expression.class)
public abstract class Spawn extends Expression {
	@Specialization
	public PorcEUnit spawnBindFuture(PorcERuntime runtime, Counter c, Terminator t, PorcEClosure computation) {
	    t.checkLive();
	    // TODO: PERFORMANCE: Give Spawn a call node attached to the closure. Then have runtime.spawn throw a ControlFlowException if we have stack space and then we will call the continuation using the call node.
	    runtime.spawn(c, computation);
	    return PorcEUnit.SINGLETON;
	}
	
	public static Spawn create(Expression c, Expression t, Expression computation, PorcERuntime runtime) {
		return SpawnNodeGen.create(c, t, computation, runtime);
	}
}
