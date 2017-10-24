
package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.nodes.RootNode;

import orc.run.porce.call.TailCallLoop;
import orc.run.porce.runtime.TailCallException;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcEExecutionHolder;

public class InvokeWithTrampolineRootNode extends RootNode {
	@Child
    protected TailCallLoop loop;
	@Child
	protected DirectCallNode body;

    public InvokeWithTrampolineRootNode(final PorcELanguage language, final RootNode root, final PorcEExecution execution) {
    	super(language);
        final PorcEExecutionHolder holder = new PorcEExecutionHolder(execution);
    	this.body = DirectCallNode.create(root.getCallTarget());
		this.loop = TailCallLoop.create(holder.newRef());
    }
    
	@Override
	public Object execute(VirtualFrame frame) {
    	try {
    		body.call(frame.getArguments());
    	} catch (TailCallException e) {
    		loop.addSurroundingFunction(frame, ((RootCallTarget)body.getCallTarget()).getRootNode());
			loop.executeTailCalls(frame, e);
    	}
    	return PorcEUnit.SINGLETON;
    }
	
    @Override
    public String getName() {
        return ((RootCallTarget)body.getCallTarget()).getRootNode().getName() + "<trampoline>";
    }
    
    @Override
    public String toString() {
        return getName();
    }
}
