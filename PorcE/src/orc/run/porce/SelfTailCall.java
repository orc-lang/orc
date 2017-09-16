
package orc.run.porce;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import orc.run.porce.runtime.SelfTailCallException;

public abstract class SelfTailCall extends Node {
    @Children
    protected final Expression[] arguments;
    
    protected SelfTailCall(final Expression[] arguments) {
		this.arguments = arguments;
    }
    
    abstract void executeCall(VirtualFrame frame, Object[] environment);
	/*
    @Specialization
    @ExplodeLoop
    public void withoutEnv(VirtualFrame frame) {
        Object[] frameArguments = frame.getArguments();
    	// Overwrite the arguments with the new arguments. Leave the closure argument intact.
        for (int i = 0; i < arguments.length; i++) {
			frameArguments[i + 1] = arguments[i].execute(frame);
        }
        
        throw new SelfTailCallException();
    }
    */

    @Specialization
    @ExplodeLoop
    public void withEnv(VirtualFrame frame, Object[] environment) {
        Object[] frameArguments = frame.getArguments();
    	// Overwrite the arguments with the new arguments. Leave the closure argument intact.
        for (int i = 0; i < arguments.length; i++) {
			frameArguments[i + 1] = arguments[i].execute(frame);
        }
        
        frameArguments[0] = environment;
        
        throw new SelfTailCallException();
    }

    public static SelfTailCall create(final Expression[] arguments) {
        return SelfTailCallNodeGen.create(arguments);
    }
}
