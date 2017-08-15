
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

    public Kill(final PorcEExecutionRef execution) {
        this.execution = execution;
    }

    @Specialization
    public PorcEUnit run(final VirtualFrame frame, final Terminator terminator, final PorcEClosure continuation, @Cached("create(execution)") final InternalArgArrayCallBase callNode) {
        if (terminator.kill(continuation)) {
            callNode.execute(frame, continuation, new Object[] { null });
        }

        return PorcEUnit.SINGLETON;
    }

    public static Kill create(final Expression terminator, final Expression continuation, final PorcEExecutionRef execution) {
        return KillNodeGen.create(execution, terminator, continuation);
    }
}
