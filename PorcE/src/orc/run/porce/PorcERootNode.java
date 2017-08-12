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

public class PorcERootNode extends RootNode implements HasPorcNode, HasId {
	private final static boolean assertionsEnabled = false;

	private Option<PorcAST> porcNode = Option.apply(null);

	public void setPorcAST(PorcAST ast) {
		porcNode = Option.apply(ast);
		//Logger.fine(() -> this + " is " + getName());
	}

	public Option<PorcAST> porcNode() {
		return porcNode;
	}
	
	public String getName() {
		String name = "<no AST>";
		scala.Option<PorcAST> optAst = porcNode();
		if (optAst.isDefined()) {
			PorcAST ast = optAst.get();
			name = "<N/A>";
			if (ast instanceof orc.ast.hasOptionalVariableName) {
				scala.Option<String> optName = ((orc.ast.hasOptionalVariableName)ast).optionalVariableName();
				if(optName.isDefined())
					name = optName.get();
				else
					name = "<unset>";
			}
		}
		return name;
	}
	
	public int getId() {
		return (int) ((ASTWithIndex)porcNode().get()).optionalIndex().get();		
	}

	protected @Child Expression body;
	private final int nArguments;
	private final int nCaptured;

	public PorcERootNode(FrameDescriptor descriptor, Expression body, int nArguments, int nCaptured) {
		super(null, descriptor);
		this.body = body;
		this.nArguments = nArguments;
		this.nCaptured = nCaptured;
	}

	@Override
	public Object execute(VirtualFrame frame) {
		if (assertionsEnabled) {
			Object[] arguments = frame.getArguments();
			if (arguments.length != nArguments + 1) {
				throwArityException(arguments.length - 1, nArguments);
			}
			Object[] captureds = (Object[]) arguments[0];
			if (captureds.length != nCaptured) {
				InternalPorcEError.capturedLengthError(nCaptured, captureds.length);
			}
		}

		try {
			Object ret = body.execute(frame);
			return ret;
		} catch (KilledException | HaltException e) {
			transferToInterpreter();
			Logger.log(Level.WARNING, () -> "Caught " + e + " from root node.", e);
			return PorcEUnit.SINGLETON;
		}
	}

	@TruffleBoundary(allowInlining = true)
	private static void throwArityException(int nReceived, int nExpected) {
		throw new ArityMismatchException(nExpected, nReceived);
	}

	public static PorcERootNode create(FrameDescriptor descriptor, Expression body, int nArguments, int nCaptured) {
		return new PorcERootNode(descriptor, body, nArguments, nCaptured);
	}
	
	@Override
	public String toString() {
		return "PorcE." + getName();
	}
}
