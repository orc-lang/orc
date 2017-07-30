package orc.run.porce;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.Terminator;
import scala.Option;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEObject;

import orc.ast.porc.PorcAST;

@NodeInfo(language = "PorcE")
@TypeSystemReference(PorcETypes.class)
public abstract class PorcENode extends Node implements HasPorcNode {
	private Option<PorcAST> porcNode = Option.apply(null);
	
	public void setPorcAST(PorcAST ast) {
		porcNode = Option.apply(ast);
	}
	
	public Option<PorcAST> porcNode() {
		return porcNode;
	}
	
	public Object execute(VirtualFrame frame) {
		executePorcEUnit(frame);
		return PorcEUnit.SINGLETON;
	}

	public void executePorcEUnit(VirtualFrame frame) {
		execute(frame);
	}

	public PorcEClosure executePorcEClosure(VirtualFrame frame)
			throws UnexpectedResultException {
		return PorcETypesGen.expectPorcEClosure(execute(frame));
	}

	public Counter executeCounter(VirtualFrame frame)
			throws UnexpectedResultException {
		return PorcETypesGen.expectCounter(execute(frame));
	}

	public Terminator executeTerminator(VirtualFrame frame)
			throws UnexpectedResultException {
		return PorcETypesGen.expectTerminator(execute(frame));
	}

	public orc.Future executeFuture(VirtualFrame frame)
			throws UnexpectedResultException {
		return PorcETypesGen.expectFuture(execute(frame));
	}

	public PorcEObject executePorcEObject(VirtualFrame frame)
			throws UnexpectedResultException {
		return PorcETypesGen.expectPorcEObject(execute(frame));
	}
	
	@Override
	protected void onReplace(Node newNode, CharSequence reason) {
		if(newNode instanceof PorcENode && porcNode().isDefined()) {
			((PorcENode)newNode).setPorcAST(porcNode().get());
		}
		super.onReplace(newNode, reason);
	}
}
