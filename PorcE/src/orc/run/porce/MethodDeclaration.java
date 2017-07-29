package orc.run.porce;

import java.util.Arrays;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import orc.ast.porc.PorcAST;
import orc.run.porce.runtime.PorcEClosure;

public class MethodDeclaration {

	public static class MethodClosure {
		final Object[] capturedValues;
		final orc.run.porce.runtime.Terminator capturedTerminator;

		public MethodClosure(Object[] capturedValues, orc.run.porce.runtime.Terminator capturedTerminator) {
			this.capturedValues = capturedValues;
			this.capturedTerminator = capturedTerminator;
		}
	}

	public static class NewMethodClosure extends Expression {
		@Children
		protected final Expression[] capturedExprs;
		@Child
		protected Expression capturedTerminator;

		protected final int nMethods;

		public NewMethodClosure(Expression capturedTerminator, Expression[] capturedExprs, int nMethods) {
			this.capturedTerminator = capturedTerminator;
			this.capturedExprs = capturedExprs;
			this.nMethods = nMethods;
		}

		@ExplodeLoop
		public Object execute(VirtualFrame frame) {
			Object[] capturedValues = new Object[capturedExprs.length + nMethods];
			for (int i = 0; i < capturedExprs.length; i++) {
				capturedValues[i] = capturedExprs[i].execute(frame);
			}
			try {
				return new MethodClosure(capturedValues, capturedTerminator.executeTerminator(frame));
			} catch (UnexpectedResultException e) {
				throw InternalPorcEError.typeError(this, e);
			}
		}

		public static NewMethodClosure create(Expression capturedTerminator, Expression[] capturedExprs, int nMethods) {
			return new NewMethodClosure(capturedTerminator, capturedExprs, nMethods);
		}
	}

	@NodeChild("closure")
	@NodeField(name = "index", type = int.class)
	@NodeField(name = "callTarget", type = RootCallTarget.class)
	@NodeField(name = "isRoutine", type = boolean.class)
	public static class NewMethod extends Expression {

		@Specialization
		public PorcEClosure run(int index, RootCallTarget callTarget, boolean isRoutine, MethodClosure closure) {
			PorcEClosure m = new PorcEClosure(closure.capturedValues, callTarget, isRoutine,
					closure.capturedTerminator);
			closure.capturedValues[index] = m;
			return m;
		}

		public static NewMethod create(Expression closure, int index, PorcERootNode rootNode, boolean isRoutine) {
			RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
			return MethodDeclarationFactory.NewMethodNodeGen.create(closure, index, callTarget, isRoutine);
		}
	}

}