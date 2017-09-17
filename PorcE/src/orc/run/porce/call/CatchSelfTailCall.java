
package orc.run.porce.call;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.profiles.BranchProfile;

import orc.run.porce.Expression;
import orc.run.porce.PorcEUnit;
import orc.run.porce.runtime.SelfTailCallException;

public class CatchSelfTailCall extends Expression {
	@Child
    protected LoopNode loop;
    
    protected CatchSelfTailCall(final Expression body) {
    	this.loop = Truffle.getRuntime().createLoopNode(new CatchSelfTailCallRepeatingNode(body));
    }
	
    @Specialization
    public PorcEUnit run(VirtualFrame frame) {
    	loop.executeLoop(frame);
		return PorcEUnit.SINGLETON;
    }

    public static CatchSelfTailCall create(final Expression body) {
        return CatchSelfTailCallNodeGen.create(body);
    }
    
    protected static class CatchSelfTailCallRepeatingNode extends Node implements RepeatingNode {
    	@Child
		protected Expression body;
		private final BranchProfile selfTailCallProfile = BranchProfile.create();

		public CatchSelfTailCallRepeatingNode(Expression body) {
			this.body = body;
		}

		@Override
		public boolean executeRepeating(VirtualFrame frame) {
			try {
				body.execute(frame);
				return false;
			} catch (SelfTailCallException e) {
				selfTailCallProfile.enter();
				return true;
			}
		}

	}

}
