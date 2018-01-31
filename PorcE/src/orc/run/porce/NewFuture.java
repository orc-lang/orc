
package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;

@Instrumentable(factory = NewFutureWrapper.class)
public class NewFuture extends Expression {
    private final boolean raceFreeResolution;

    public NewFuture(final boolean raceFreeResolution) {
        super();
        this.raceFreeResolution = raceFreeResolution;
    }

    protected NewFuture(final NewFuture orig) {
        this(orig.raceFreeResolution);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        return executeFuture(frame);
    }

    @Override
    public orc.Future executeFuture(final VirtualFrame frame) {
        return new orc.run.porce.runtime.Future(raceFreeResolution);
    }

    public static NewFuture create(final boolean raceFreeResolution) {
        return new NewFuture(raceFreeResolution);
    }
}
