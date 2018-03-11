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

public class InvokeCallRecordRootNode extends PorcERootNode {
    @Override
    public String getName() {
        return "InvokeCallRecordRootNode@" + hashCode();
    }

    @Override
    public int getId() {
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

    public InvokeCallRecordRootNode(final PorcELanguage language, final int nArguments, final PorcEExecution execution) {
        super(language, new FrameDescriptor(), buildBody(nArguments, execution), nArguments, 0);
    }
    
    @Override
    public boolean isInternal() {
      return true;
    }
}
