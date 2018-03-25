//
// InvokeCallRecordRootNode.java -- Truffle root node InvokeCallRecordRootNode
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.porce.call.Call;
import orc.run.porce.runtime.PorcEExecution;

import com.oracle.truffle.api.frame.FrameDescriptor;

/**
 * A Truffle root node calls an arbitrary PorcE value given as the first argument with 
 * the rest of the arguments.
 * 
 * This root node handles any tail calls internally. The caller can treat this as a normal 
 * call with semantics similar to Java or Scala (other than all the spawning).
 *
 * @author amp
 */
public class InvokeCallRecordRootNode extends PorcERootNode {
    private final int callSiteId;

    @Override
    public int getId() {
        // FIXME: Clearly the inheritance hierarchy is messed up. This should be fixed if and when refactoring happens for anything related.
        throw new UnsupportedOperationException();
    }

    private static Expression buildBody(final int nArguments, final PorcEExecution execution) {
        final Expression readTarget = Read.Argument.create(0);
        final Expression[] readArgs = new Expression[nArguments];
        for (int i = 0; i < nArguments; i++) {
            readArgs[i] = Read.Argument.create(i + 1);
        }
        return Call.CPS.create(readTarget, readArgs, execution, false);
    }

    public InvokeCallRecordRootNode(final PorcELanguage language, final int nArguments, final int callSiteId, final PorcEExecution execution) {
        super(language, new FrameDescriptor(), buildBody(nArguments, execution), nArguments + 1, 0, execution);
        this.callSiteId = callSiteId;
    }
    
    @Override
    public boolean isInternal() {
        return true;
    }
    
    @Override
    public String getName() {
        return callSiteId + "<invokeCallRecord>";
    }
    
    @Override
    public String toString() {
        return callSiteId + "<invokeCallRecord>";
    }
}
