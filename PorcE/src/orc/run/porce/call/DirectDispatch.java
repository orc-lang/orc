//
// DirectDispatch.java -- Java class DirectDispatch
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import orc.run.porce.runtime.PorcEExecution;

import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class DirectDispatch extends Dispatch {
	protected DirectDispatch(PorcEExecution execution) {
		super(execution);
	}

	/**
	 * Dispatch the direct call to the target with the given arguments.
	 * 
	 * @param frame
	 *            The frame we are executing in.
	 * @param target
	 *            The call target.
	 * @param arguments
	 *            The arguments to the call. This will include (P, C, T) as a
	 *            prefix for external calls and will NOT have a gap for the
	 *            environment for internal calls.
	 * @return The return value of the underlying call if it has one. Only
	 *         direct call dispatchers will actually provide a value.
	 * 
	 */
	public abstract Object executeDirectDispatch(VirtualFrame frame, Object target, Object[] arguments);
	
	@Override
	public void executeDispatch(VirtualFrame frame, Object target, Object[] arguments) {
		executeDirectDispatch(frame, target, arguments);
	}
}
