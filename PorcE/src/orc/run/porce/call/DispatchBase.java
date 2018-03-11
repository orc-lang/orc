//
// DispatchBase.java -- Java class DispatchBase
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import orc.run.porce.NodeBase;
import orc.run.porce.runtime.PorcEExecution;

public abstract class DispatchBase extends NodeBase {
	protected final PorcEExecution execution;

	protected DispatchBase(PorcEExecution execution) {
		this.execution = execution;
	}
}
