package orc.run.porce;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;

import orc.run.porce.runtime.PorcEExecutionRef;


@Instrumentable(factory = InternalArgArrayCallBaseWrapper.class)
public abstract class InternalArgArrayCallBase extends Expression {
    protected final PorcEExecutionRef execution;

    protected InternalArgArrayCallBase(InternalArgArrayCallBase orig) {
		this(orig.execution);
	}
	

    public InternalArgArrayCallBase(final PorcEExecutionRef execution) {
        this.execution = execution;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        CompilerDirectives.bailout("InternalArgArrayCall cannot be executed this way.");
        throw new IllegalStateException("InternalArgArrayCall cannot be executed this way.");
    }

    public abstract Object execute(VirtualFrame frame, Object target, Object[] arguments);

    protected static InternalArgArrayCallBase findCacheRoot(final InternalArgArrayCallBase n) {
        CompilerAsserts.neverPartOfCompilation();
        if (n.getParent() instanceof InternalArgArrayCallBase) {
            return findCacheRoot((InternalArgArrayCallBase) n.getParent());
        } else {
            return n;
        }
    }

    public static InternalArgArrayCallBase create(final PorcEExecutionRef execution) {
        return InternalArgArrayCall.create(execution);
    }
}

