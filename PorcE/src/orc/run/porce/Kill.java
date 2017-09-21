
package orc.run.porce;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import orc.run.porce.call.Dispatch;
import orc.run.porce.call.InternalCPSDispatch;
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
    		@Cached("createCallNode(execution, isTail)") final Dispatch callNode) {
    	// TODO: PERFORMANCE: This should speculate on the the result of terminator to avoid creating the callNode if it's not needed.
    	// Token: This passes a token on counter to the continuation if kill returns false.
        if (terminator.kill(counter, continuation)) {
            callNode.executeDispatch(frame, continuation, new Object[] { });
        }

        return PorcEUnit.SINGLETON;
    }
    
    protected static Dispatch createCallNode(final PorcEExecutionRef execution, boolean isTail) {
    	return InternalCPSDispatch.create(execution, isTail);
    }

    public static Kill create(final Expression counter, final Expression terminator, final Expression continuation, final PorcEExecutionRef execution) {
        return KillNodeGen.create(execution, counter, terminator, continuation);
    }
}
