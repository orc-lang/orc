
package orc.run.porce.call;

import com.oracle.truffle.api.frame.VirtualFrame;

import orc.run.porce.Expression;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.TailCallException;

public class CatchTailCall extends Expression {
	@Child
    protected TailCallLoop loop;
	@Child
	protected Expression body;
	
	protected final PorcEExecutionRef execution; 

	@Override
    public void setTail(boolean v) {
		super.setTail(v);
		body.setTail(v);
	}

    protected CatchTailCall(final Expression body, final PorcEExecutionRef execution) {
    	this.body = body;
		this.execution = execution;
		this.loop = TailCallLoop.create(execution);
    }
    
    public void executePorcEUnit(VirtualFrame frame) {
    	try {
    		body.executePorcEUnit(frame);
    	} catch (TailCallException e) {
			loop.executeTailCalls(frame, e);
    	}
    }

    public static CatchTailCall create(final Expression body, final PorcEExecutionRef execution) {
        return new CatchTailCall(body, execution);
    }
}
