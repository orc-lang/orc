
package orc.run.porce;

import java.util.concurrent.atomic.LongAdder;

import orc.run.porce.call.TailCallLoop;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.TailCallException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;

public class InvokeWithTrampolineRootNode extends RootNode {
	@Child
    protected TailCallLoop loop;
	@Child
	protected DirectCallNode body;
	
	final private PorcERootNode root;
	
	final private LongAdder callCount = new LongAdder();

    public InvokeWithTrampolineRootNode(final PorcELanguage language, final RootNode root, final PorcEExecution execution) {
    	super(language);
        if (root instanceof PorcERootNode)
        	this.root = (PorcERootNode) root;
        else
        	this.root = null;
    	this.body = DirectCallNode.create(root.getCallTarget());
		this.loop = TailCallLoop.create(execution);
    }
    
    @Override
    public boolean isInternal() {
      return true;
    }
    
    public long getCallCount() {
    	return callCount.sum();
    }
    
	public PorcERootNode getRoot() {
		return root;
	}

	@TruffleBoundary
	private void incrCallCount() {
        callCount.increment();
	}
	
	private boolean shouldTimeRoot() {
		return root != null && root.shouldTimeCall();
	}

	@Override
	public Object execute(VirtualFrame frame) {
        long startTime = 0;
        if (shouldTimeRoot())
        	startTime = System.nanoTime();
        if (CompilerDirectives.inInterpreter())
        	incrCallCount();
    	try {
    		body.call(frame.getArguments());
    	} catch (TailCallException e) {
    		loop.addSurroundingFunction(frame, ((RootCallTarget)body.getCallTarget()).getRootNode());
			loop.executeTailCalls(frame, e);
    	} finally {
        	if (shouldTimeRoot() && startTime > 0) {
        		root.addSpawnedCall(System.nanoTime() - startTime);
        	}
        }
    	return PorcEUnit.SINGLETON;
    }
	
    @Override
    public String getName() {
    	if (root == null) {
    		return hashCode() + "<trampoline>";
    	} else {
    		return root.getName() + "<trampoline>";
    	}
    }
    
    @Override
    public String toString() {
    	if (root == null) {
    		return hashCode() + "<trampoline>";
    	} else {
    		return root.toString() + "<trampoline>";
    	}
    }
}
