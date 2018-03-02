
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
