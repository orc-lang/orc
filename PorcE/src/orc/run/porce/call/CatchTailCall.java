
package orc.run.porce.call;

import com.oracle.truffle.api.frame.VirtualFrame;

import orc.run.porce.Expression;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.TailCallException;

public class CatchTailCall extends Expression {
	@Child
    protected TailCallLoop loop;
	@Child
	protected Expression body;
	
	protected final PorcEExecution execution; 

	@Override
    public void setTail(boolean v) {
		super.setTail(v);
		body.setTail(v);
	}

    protected CatchTailCall(final Expression body, final PorcEExecution execution) {
    	this.body = body;
		this.execution = execution;
		this.loop = TailCallLoop.create(execution);
    }
    
    public void executePorcEUnit(VirtualFrame frame) {
    	try {
    		body.executePorcEUnit(frame);
    	} catch (TailCallException e) {
    		// TODO: This does not add an initial target to the set. It probably should in some cases, but it would be almost impossible to do that here.
			loop.executeTailCalls(frame, e);
    	}
    }

    public static CatchTailCall create(final Expression body, final PorcEExecution execution) {
        return new CatchTailCall(body, execution);
    }
}
