package orc.run.porce;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.Terminator;

@NodeChild("terminator")
@NodeChild("continuation")
public class Kill extends Expression {
	protected final PorcEExecutionRef execution;

	public Kill(PorcEExecutionRef execution) {
		this.execution = execution;
	}

	@Specialization
	public PorcEUnit run(VirtualFrame frame, Terminator terminator, PorcEClosure continuation, @Cached("create(execution)") InternalArgArrayCallBase callNode) {
		if(terminator.kill(continuation)) {
			callNode.execute(frame, continuation, new Object[] { null });
		}

		return PorcEUnit.SINGLETON;
	}

	public static Kill create(Expression terminator, Expression continuation, PorcEExecutionRef execution) {
		return KillNodeGen.create(execution, terminator, continuation);
	}
}
