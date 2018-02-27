
package orc.run.porce;

import orc.error.runtime.HaltException;
import orc.run.porce.runtime.KilledException;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;

public class TryOnException extends Expression {
    @Child
    protected Expression body;
    @Child
    protected Expression handler;
    
    private final BranchProfile haltCatchProfile = BranchProfile.create();
    private final BranchProfile killCatchProfile = BranchProfile.create();

    public TryOnException(final Expression body, final Expression handler) {
        this.body = body;
        this.handler = handler;
    }

    @Override
    public void executePorcEUnit(final VirtualFrame frame) {
        try {
            body.executePorcEUnit(frame);
        } catch (HaltException e) {
            haltCatchProfile.enter();
            handler.executePorcEUnit(frame);
        } catch (KilledException e) {
            killCatchProfile.enter();
            handler.executePorcEUnit(frame);
        }
    }

    public static TryOnException create(final Expression body, final Expression handler) {
        return new TryOnException(body, handler);
    }
}
