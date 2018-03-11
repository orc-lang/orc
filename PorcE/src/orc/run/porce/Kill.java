//
// Kill.java -- Truffle node Kill
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.porce.call.Dispatch;
import orc.run.porce.call.InternalCPSDispatch;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.Terminator;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

@NodeChild("counter")
@NodeChild("terminator")
@NodeChild("continuation")
public class Kill extends Expression {
    protected final PorcEExecution execution;

    public Kill(final PorcEExecution execution) {
        this.execution = execution;
    }

    @Specialization
    public PorcEUnit run(final VirtualFrame frame, final Counter counter, final Terminator terminator, final PorcEClosure continuation, 
    		@Cached("createCallNode(execution, isTail)") final Dispatch callNode) {
    	// Token: This passes a token on counter to the continuation if kill returns false.
        if (terminator.kill(counter, continuation)) {
            callNode.executeDispatch(frame, continuation, new Object[] { });
        }

        return PorcEUnit.SINGLETON;
    }
    
    protected static Dispatch createCallNode(final PorcEExecution execution, boolean isTail) {
    	return InternalCPSDispatch.create(true, execution, isTail);
    }

    public static Kill create(final Expression counter, final Expression terminator, final Expression continuation, final PorcEExecution execution) {
        return KillNodeGen.create(execution, counter, terminator, continuation);
    }
}
