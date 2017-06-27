package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import orc.run.porce.runtime.Counter;

@NodeChild("counter")
public class Halt extends Expression {
	@Specialization
	public PorcEUnit run(Counter counter) {
		counter.halt();
		return PorcEUnit.SINGLETON;
	}
	
	public static Halt create(Expression parent) {
		return HaltNodeGen.create(parent);
	}
}
