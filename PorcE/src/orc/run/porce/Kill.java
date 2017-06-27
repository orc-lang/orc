package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

import orc.run.porce.runtime.Terminator;

@NodeChild("terminator")
public class Kill extends Expression {
	@Specialization
	public PorcEUnit run(Terminator terminator) {
		terminator.kill();
		return PorcEUnit.SINGLETON;
	}
	
	public static Kill create(Expression parent) {
		return KillNodeGen.create(parent);
	}
}
