package orc.run.porce.call;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;

import orc.ast.ASTWithIndex;
import orc.ast.porc.PorcAST;
import orc.run.porce.HasPorcNode;
import orc.run.porce.NodeBase;
import orc.run.porce.runtime.PorcEExecution;

public abstract class DispatchBase extends NodeBase {
	protected final PorcEExecution execution;

	protected DispatchBase(PorcEExecution execution) {
		this.execution = execution;
	}

	@CompilerDirectives.CompilationFinal
	private int callSiteId = -1;

	protected int getCallSiteId() {
		if (callSiteId >= 0) {
			return callSiteId;
		} else {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			callSiteId = findCallSiteId(this);
			return callSiteId;
		}
	}

	/**
	 * Climb the Truffle AST searching for a node with a PorcAST with an index.
	 */
	private int findCallSiteId(final Node e) {
		if (e instanceof HasPorcNode) {
			HasPorcNode pn = (HasPorcNode) e;
			if (pn.porcNode().isDefined()) {
				final PorcAST ast = pn.porcNode().get();
				if (ast instanceof ASTWithIndex && ((ASTWithIndex) ast).optionalIndex().isDefined()) {
					return ((Integer) ((ASTWithIndex) ast).optionalIndex().get()).intValue();
				}
			}
		}
		final Node p = e.getParent();
		if (p instanceof DispatchBase) {
			return ((DispatchBase) p).getCallSiteId();
		} else if (p != null) {
			return findCallSiteId(p);
		}
		return -1;
	}
}
