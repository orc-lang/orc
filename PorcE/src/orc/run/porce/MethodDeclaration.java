
package orc.run.porce;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import orc.run.porce.runtime.PorcEClosure;

public class MethodDeclaration {

    public static class MethodClosure {
        final Object[] environment;

        public MethodClosure(final Object[] environment) {
            this.environment = environment;
        }
    }

    public static class NewMethodClosure extends Expression {
        @Children
        protected final Expression[] capturedExprs;
        @Child
        protected Expression capturedTerminator;

        protected final int nMethods;

        public NewMethodClosure(final Expression capturedTerminator, final Expression[] capturedExprs, final int nMethods) {
            this.capturedTerminator = capturedTerminator;
            this.capturedExprs = capturedExprs;
            this.nMethods = nMethods;
        }

        @Override
        @ExplodeLoop
        public Object execute(final VirtualFrame frame) {
            final Object[] capturedValues = new Object[capturedExprs.length + nMethods];
            for (int i = 0; i < capturedExprs.length; i++) {
                capturedValues[i] = capturedExprs[i].execute(frame);
            }
            return new MethodClosure(capturedValues);
        }

        public static NewMethodClosure create(final Expression capturedTerminator, final Expression[] capturedExprs, final int nMethods) {
            return new NewMethodClosure(capturedTerminator, capturedExprs, nMethods);
        }
    }

    @NodeChild("closure")
    @NodeField(name = "index", type = int.class)
    @NodeField(name = "callTarget", type = RootCallTarget.class)
    @NodeField(name = "isRoutine", type = boolean.class)
    public static class NewMethod extends Expression {

        @Specialization
        public PorcEClosure run(final int index, final RootCallTarget callTarget, final boolean isRoutine, final MethodClosure closure) {
            final PorcEClosure m = new PorcEClosure(closure.environment, callTarget, isRoutine);
            closure.environment[index] = m;
            return m;
        }

        public static NewMethod create(final Expression closure, final int index, final PorcERootNode rootNode, final boolean isRoutine) {
            final RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            return MethodDeclarationFactory.NewMethodNodeGen.create(closure, index, callTarget, isRoutine);
        }
    }

}
