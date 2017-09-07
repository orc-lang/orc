
package orc.run.porce;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

import java.util.logging.Level;

import scala.Option;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import orc.ast.ASTWithIndex;
import orc.ast.porc.PorcAST;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.HaltException;
import orc.run.porce.runtime.KilledException;
import orc.run.porce.runtime.SourceSectionFromPorc;

public class PorcERootNode extends RootNode implements HasPorcNode, HasId {
    private final static boolean assertionsEnabled = false;

    private Option<PorcAST> porcNode = Option.apply(null);

	public void setPorcAST(final PorcAST ast) {
		CompilerAsserts.neverPartOfCompilation();
		porcNode = Option.apply(ast);
		section = SourceSectionFromPorc.apply(ast);
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
    public String getName() {
        String name = "<no AST>";
        scala.Option<PorcAST> optAst = porcNode();
        if (optAst.isDefined()) {
            final PorcAST ast = optAst.get();
            name = "<N/A>";
            if (ast instanceof orc.ast.hasOptionalVariableName) {
                scala.Option<String> optName = ((orc.ast.hasOptionalVariableName) ast).optionalVariableName();
                if (optName.isDefined()) {
                    name = optName.get();
                } else {
                    name = "<unset>";
                }
            }
        }
        return name;
    }

    @Override
    public int getId() {
        return ((Integer) ((ASTWithIndex) porcNode().get()).optionalIndex().get()).intValue();
    }

    protected @Child Expression body;
    private final int nArguments;
    private final int nCaptured;

    public PorcERootNode(final PorcELanguage language, final FrameDescriptor descriptor, final Expression body, final int nArguments, final int nCaptured) {
        super(language, descriptor);
        this.body = body;
        this.nArguments = nArguments;
        this.nCaptured = nCaptured;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        if (assertionsEnabled) {
            final Object[] arguments = frame.getArguments();
            if (arguments.length != nArguments + 1) {
                throwArityException(arguments.length - 1, nArguments);
            }
            final Object[] captureds = (Object[]) arguments[0];
            if (captureds.length != nCaptured) {
                InternalPorcEError.capturedLengthError(nCaptured, captureds.length);
            }
        }

		// TODO: PERFORMANCE: Implement some form of TCO which generates a loop for 
		// self-tail calls by actually calling the body repeatedly.
		
        try {
            final Object ret = body.execute(frame);
            return ret;
        } catch (KilledException | HaltException e) {
            transferToInterpreter();
            Logger.log(Level.WARNING, () -> "Caught " + e + " from root node.", e);
            return PorcEUnit.SINGLETON;
        }
    }

    @TruffleBoundary(allowInlining = true)
    private static void throwArityException(final int nReceived, final int nExpected) {
        throw new ArityMismatchException(nExpected, nReceived);
    }

    public static PorcERootNode create(final PorcELanguage language, final FrameDescriptor descriptor, final Expression body, final int nArguments, final int nCaptured) {
        return new PorcERootNode(language, descriptor, body, nArguments, nCaptured);
    }

    @Override
    public String toString() {
        return "PorcE." + getName();
    }
}
