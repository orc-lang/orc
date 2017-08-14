package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.*;
import static com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import java.util.logging.Level;

import orc.ast.ASTWithIndex;
import orc.ast.porc.PorcAST;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.HaltException;
import orc.run.porce.runtime.KilledException;
import scala.Option;

public class InvokeCallRecordRootNode extends PorcERootNode {
	private Option<PorcAST> porcNode = Option.apply(null);

	@Override
    public String getName() {
		return "InvokeCallRecordRootNode#" + hashCode();
	}

	@Override
	public int getId() {
		throw new UnsupportedOperationException();
	}
	
	private static Expression buildBody(int nArguments, PorcEExecution execution) {
		PorcEExecutionHolder holder = new PorcEExecutionHolder(execution);
		Expression readTarget = Read.Argument.create(0);
		Expression[] readArgs = new Expression[nArguments];
		for(int i=0; i < nArguments, i++) {
			readArgs[i] = Read.Argument.create(i + 1);
		}
		Call.CPS.create(readTarget, readArgs, holder.newRef())
	}
	
	public InvokeCallRecordRootNode(int nArguments, PorcEExecution execution) {
		super(new FrameDescriptor(), buildBody(nArguments, execution), nArguments, 0);
	}
}
