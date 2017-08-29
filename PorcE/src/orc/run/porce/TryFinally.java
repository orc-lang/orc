
package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;

public class TryFinally extends Expression {
    @Child
    protected Expression body;
    @Child
    protected Expression handler;

    public TryFinally(final Expression body, final Expression handler) {
        this.body = body;
        this.handler = handler;
    }

    @Override
    public void executePorcEUnit(final VirtualFrame frame) {
        try {
            body.executePorcEUnit(frame);
        } finally {
            handler.executePorcEUnit(frame);
        }
    }

    public static TryFinally create(final Expression body, final Expression handler) {
        return new TryFinally(body, handler);
    }
}
