//
// CatchTailCall.java -- Java class CatchTailCall
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
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.TailCallException;

import com.oracle.truffle.api.frame.VirtualFrame;

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
    
    @Override
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
