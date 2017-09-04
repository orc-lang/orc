
package orc.run.porce;

import scala.Option;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;

import orc.ast.porc.PorcAST;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEObject;
import orc.run.porce.runtime.Terminator;
import orc.run.porce.runtime.SourceSectionFromPorc;

@NodeInfo(language = "PorcE")
@TypeSystemReference(PorcETypes.class)
public abstract class PorcENode extends Node implements HasPorcNode {
    private Option<PorcAST> porcNode = Option.apply(null);

	public void setPorcAST(final PorcAST ast) {
		CompilerAsserts.neverPartOfCompilation();
		porcNode = Option.apply(ast);
		section = SourceSectionFromPorc.apply(ast);
		getChildren().forEach((n) -> {
			if (n instanceof PorcENode) {
				final Expression e = (Expression) n;
				if (e.porcNode().isEmpty()) {
					e.setPorcAST(ast);
				}
			}
		});
	}

    @Override
    public Option<PorcAST> porcNode() {
        return porcNode;
    }
    
    @CompilationFinal
    private SourceSection section = null;

    @Override
    public SourceSection getSourceSection() {
        return section;
    }
    
    public Object execute(final VirtualFrame frame) {
        executePorcEUnit(frame);
        return PorcEUnit.SINGLETON;
    }

    public void executePorcEUnit(final VirtualFrame frame) {
        execute(frame);
    }

    public PorcEClosure executePorcEClosure(final VirtualFrame frame) throws UnexpectedResultException {
        return PorcETypesGen.expectPorcEClosure(execute(frame));
    }

    public Counter executeCounter(final VirtualFrame frame) throws UnexpectedResultException {
        return PorcETypesGen.expectCounter(execute(frame));
    }

    public Terminator executeTerminator(final VirtualFrame frame) throws UnexpectedResultException {
        return PorcETypesGen.expectTerminator(execute(frame));
    }

    public orc.Future executeFuture(final VirtualFrame frame) throws UnexpectedResultException {
        return PorcETypesGen.expectFuture(execute(frame));
    }

    public PorcEObject executePorcEObject(final VirtualFrame frame) throws UnexpectedResultException {
        return PorcETypesGen.expectPorcEObject(execute(frame));
    }

    @Override
    protected void onReplace(final Node newNode, final CharSequence reason) {
        if (newNode instanceof PorcENode && porcNode().isDefined()) {
            ((PorcENode) newNode).setPorcAST(porcNode().get());
        }
        super.onReplace(newNode, reason);
    }
}
