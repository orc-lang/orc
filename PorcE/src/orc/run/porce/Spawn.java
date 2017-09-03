
package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.Terminator;
import orc.run.porce.InternalArgArrayCallBase;

@NodeChild(value = "c", type = Expression.class)
@NodeChild(value = "t", type = Expression.class)
@NodeChild(value = "computation", type = Expression.class)
public abstract class Spawn extends Expression {
	
    @Child
    protected InternalArgArrayCallBase call = null;
    private final ConditionProfile canDirectCallProfile = ConditionProfile.createCountingProfile();
    private final PorcEExecutionRef execution;
    @SuppressWarnings("unused")
	private final boolean mustSpawn;
    
    protected Spawn(boolean mustSpawn, PorcEExecutionRef execution) {
		this.mustSpawn = mustSpawn;
		this.execution = execution;
	}


    @Specialization
    public PorcEUnit spawn(final VirtualFrame frame, final Counter c, final Terminator t, final PorcEClosure computation) {
    	// Here we can inline spawns speculatively if we have not done that too much on this stack.
    	// This is very heuristic and may cause load imbalance problems in some cases.
    	// TODO: Figure out how to make this principled.
        // TODO: PERFORMANCE: Track the runtime of the spawned closure in the interpreter. Then if it is below some constant (1ms say) call it directly if the stack is not too deep. This could address issues with load-imbalance at least in predictable cases.
    	// TODO: This could actually cause semantic problem in the case of incorrectly implemented sites which block the calling thread. Metadata is probably needed.
		boolean canDirectCall = canDirectCallProfile.profile(PorcERuntime.incrementAndCheckStackDepth());
		if (canDirectCall) {
			try {
				initializeCall();
				call.execute(frame, computation, new Object[] { null });
			} finally {
				PorcERuntime.decrementStackDepth();
			}
		} else {
			t.checkLive();
			execution.get().runtime().spawn(c, computation);
		}
        return PorcEUnit.SINGLETON;
    }
    
    private void initializeCall() {
        if (call == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            call = insert(InternalArgArrayCall.create(execution));
        }
    }

    public static Spawn create(final Expression c, final Expression t, final boolean mustSpawn, final Expression computation, final PorcEExecutionRef execution) {
        return SpawnNodeGen.create(mustSpawn, execution, c, t, computation);
    }
}
