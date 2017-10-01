
package orc.run.porce;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;

import orc.run.porce.call.Dispatch;
import orc.run.porce.call.InternalCPSDispatch;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.CounterNested;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;

@NodeChild("counter")
@Introspectable
public class HaltToken extends Expression {
	
	private final PorcEExecutionRef execution;

	protected HaltToken(final PorcEExecutionRef execution) {
		this.execution = execution;
	}
	
	private static Object[] EMPTY_ARGS = new Object[0];
	
    @Specialization
    public PorcEUnit nested(VirtualFrame frame, final CounterNested counter, 
    		@Cached("createCall()") Dispatch call, @Cached("create()") BranchProfile hasContinuationProfile) {
        PorcEClosure cont = counter.haltTokenOptimized();
        if (cont != null) {
        	hasContinuationProfile.enter();
        	call.executeDispatch(frame, cont, EMPTY_ARGS);
        }
        return PorcEUnit.SINGLETON;
    }
    
    protected Dispatch createCall() {
    	Dispatch n = InternalCPSDispatch.create(execution, isTail);
		n.setTail(isTail);
		return n;
    }
    
    @Specialization
    public PorcEUnit any(final Counter counter) {
        counter.haltToken();
        return PorcEUnit.SINGLETON;
    }

    public static HaltToken create(final Expression parent, final PorcEExecutionRef execution) {
        return HaltTokenNodeGen.create(execution, parent);
    }
}
