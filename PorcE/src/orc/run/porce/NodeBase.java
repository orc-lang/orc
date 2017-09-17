package orc.run.porce;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

import orc.ast.porc.PorcAST;
import orc.run.porce.runtime.SourceSectionFromPorc;
import orc.run.porce.instruments.ProfiledPorcNodeTag;
import scala.Option;

public abstract class NodeBase extends Node implements HasPorcNode {
	@CompilationFinal
    private Option<PorcAST> porcNode = Option.apply(null);

	public void setPorcAST(final PorcAST ast) {
		CompilerAsserts.neverPartOfCompilation();
		porcNode = Option.apply(ast);
		section = SourceSectionFromPorc.apply(ast);
		/*getChildren().forEach((n) -> {
			if (n instanceof PorcENode) {
				final Expression e = (Expression) n;
				if (e.porcNode().isEmpty()) {
					e.setPorcAST(ast);
				}
			}
		});*/
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


    @Override
    protected void onReplace(final Node newNode, final CharSequence reason) {
        if (newNode instanceof PorcENode && porcNode().isDefined()) {
            ((PorcENode) newNode).setPorcAST(porcNode().get());
        }
        super.onReplace(newNode, reason);
    }
    
    @Override
    public Node copy() {
    	Node n = super.copy();
    	((NodeBase)n).porcNode = Option.apply(null);
    	return n;
    }
    
	@Override
	protected boolean isTaggedWith(Class<?> tag) {
		// TODO: Provide tail information as a Tag.
		if (tag == ProfiledPorcNodeTag.class) {
			return porcNode().isDefined() && ProfiledPorcNodeTag.isProfiledPorcNode(porcNode().get());
		} else {
			return super.isTaggedWith(tag);
		}
	}
	
	@CompilationFinal
	protected boolean isTail = false;
	
	public void setTail(boolean v) {
		isTail = v;
	}
}
