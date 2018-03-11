//
// CatchSelfTailCall.java -- Java class CatchSelfTailCall
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import orc.run.porce.Expression;
import orc.run.porce.PorcERootNode;
import orc.run.porce.PorcEUnit;
import orc.run.porce.runtime.SelfTailCallException;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;

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
    
    public Expression getBody() {
    	CatchSelfTailCallRepeatingNode rep = (CatchSelfTailCallRepeatingNode) loop.getRepeatingNode();
    	return rep.body;
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
		
		@Override
		public String toString() {
			RootNode root = getRootNode();
			if (root instanceof PorcERootNode) {
		        return ((PorcERootNode)root).toString() + "<selftailrecloop>";
			} else {
				return hashCode() + "<selftailrecloop>";
			}
		}
	}

}
