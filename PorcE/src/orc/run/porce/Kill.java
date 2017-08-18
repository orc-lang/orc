
package orc.run.porce;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.Terminator;

@NodeChild("counter")
@NodeChild("terminator")
@NodeChild("continuation")
public class Kill extends Expression {
    protected final PorcEExecutionRef execution;

    public Kill(final PorcEExecutionRef execution) {
        this.execution = execution;
    }

    @Specialization
    public PorcEUnit run(final VirtualFrame frame, final Counter counter, final Terminator terminator, final PorcEClosure continuation, 
    		@Cached("create(execution)") final InternalArgArrayCallBase callNode) {
    	// TODO: PERFORMANCE: This should speculate on the the result of terminator to avoid creating the callNode if it's not needed.
        if (terminator.kill(counter, continuation)) {
            callNode.execute(frame, continuation, new Object[] { null });
        }

        return PorcEUnit.SINGLETON;
    }

    public static Kill create(final Expression counter, final Expression terminator, final Expression continuation, final PorcEExecutionRef execution) {
        return KillNodeGen.create(execution, counter, terminator, continuation);
    }
}
