
package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;
import orc.error.runtime.HaltException;
import orc.run.porce.runtime.KilledException;

public class TryOnException extends Expression {
    @Child
    protected Expression body;
    @Child
    protected Expression handler;

    public TryOnException(final Expression body, final Expression handler) {
        this.body = body;
        this.handler = handler;
    }

    @Override
    public void executePorcEUnit(final VirtualFrame frame) {
        try {
            body.executePorcEUnit(frame);
        } catch (HaltException | KilledException e) {
            handler.executePorcEUnit(frame);
        }
    }

    public static TryOnException create(final Expression body, final Expression handler) {
        return new TryOnException(body, handler);
    }
}
