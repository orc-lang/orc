
package orc.run.porce;

import com.oracle.truffle.api.frame.FrameDescriptor;

import orc.run.porce.call.Call;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcEExecutionHolder;

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
        final PorcEExecutionHolder holder = new PorcEExecutionHolder(execution);
        final Expression readTarget = Read.Argument.create(0);
        final Expression[] readArgs = new Expression[nArguments];
        for (int i = 0; i < nArguments; i++) {
            readArgs[i] = Read.Argument.create(i + 1);
        }
        return Call.CPS.create(readTarget, readArgs, holder.newRef(), false);
    }

    public InvokeCallRecordRootNode(final PorcELanguage language, final int nArguments, final PorcEExecution execution) {
        super(language, new FrameDescriptor(), buildBody(nArguments, execution), nArguments, 0);
    }
}
