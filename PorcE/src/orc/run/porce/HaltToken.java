package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import orc.run.porce.runtime.Counter;

@NodeChild("counter")
public class HaltToken extends Expression {
	@Specialization
	public PorcEUnit run(Counter counter) {
		counter.haltToken();
		return PorcEUnit.SINGLETON;
	}
	
	public static HaltToken create(Expression parent) {
		return HaltTokenNodeGen.create(parent);
	}
}
