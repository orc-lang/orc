
package orc.run.porce.call;

import com.oracle.truffle.api.frame.VirtualFrame;

import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.TailCallException;

public class CatchTailDispatch extends Dispatch {
	@Child
    protected TailCallLoop loop;
	@Child
	protected Dispatch body;
	
	@Override
    public void setTail(boolean v) {
		super.setTail(v);
		body.setTail(v);
	}

    protected CatchTailDispatch(final Dispatch body, final PorcEExecutionRef execution) {
    	super(execution);
    	this.body = body;
		this.loop = TailCallLoop.create(execution);
    }
    
	@Override
	public void executeDispatch(VirtualFrame frame, Object target, Object[] arguments) {
    	try {
    		body.executeDispatch(frame, target, arguments);
    	} catch (TailCallException e) {
			loop.executeTailCalls(frame, e);
    	}
    }

    public static CatchTailDispatch create(final Dispatch body, final PorcEExecutionRef execution) {
        return new CatchTailDispatch(body, execution);
    }
}
