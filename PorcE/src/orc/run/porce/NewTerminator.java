
package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import orc.run.porce.runtime.TerminatorNested;
import orc.run.porce.runtime.Terminator;

public class NewTerminator extends Expression {
    @Child
    protected Expression parent;

    public NewTerminator(final Expression parent) {
        this.parent = parent;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        return executeTerminator(frame);
    }

    @Override
    public Terminator executeTerminator(final VirtualFrame frame) {
        try {
            return new TerminatorNested(parent.executeTerminator(frame));
        } catch (final UnexpectedResultException e) {
            throw new Error(e);
        }
    }

    public static NewTerminator create(final Expression parent) {
        return new NewTerminator(parent);
    }
}
