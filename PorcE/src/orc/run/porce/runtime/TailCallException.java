package orc.run.porce.runtime;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.profiles.ConditionProfile;

@SuppressWarnings("serial")
public final class TailCallException extends ControlFlowException {
	public RootCallTarget target;
	public Object[] arguments;

	private TailCallException(RootCallTarget target, Object[] arguments) {
		this.target = target;
		this.arguments = arguments;
	}

	
	public void set(final RootCallTarget target, final Object[] arguments) {
		//assert arguments.length <= 16;
		System.arraycopy(arguments, 0, this.arguments, 0, Math.min(arguments.length, 16));
		this.target = target;
	}
	
	public RootCallTarget getTarget() {
		return target;
	}

	public Object[] getArguments() {
		return arguments;
	}
	
	public static TailCallException throwReused(final Frame frame, final ConditionProfile reuseProfile, 
			final RootCallTarget target, final Object[] arguments) {
		// FIXME: This sets a maximum working function arity to 16.
    	Object[] thisArguments = frame.getArguments();
    	if (reuseProfile.profile(
    			/*arguments.length <= 16 &&*/ thisArguments.length == 17 && thisArguments[16] instanceof TailCallException)) {
    		TailCallException tce = (TailCallException)thisArguments[16];
    		tce.set(target, arguments);
    		throw tce;
    	} else {
    		throw TailCallException.create(target, arguments);
    	}
	}

	public static TailCallException create(RootCallTarget target) {
		// FIXME: This sets a maximum working function arity to 16.
		TailCallException tce = new TailCallException(target, new Object[17]);
		tce.arguments[16] = tce;
		return tce;
	}
	
	public static TailCallException create(RootCallTarget target, Object[] arguments) {
		// FIXME: This sets a maximum working function arity to 16.
		TailCallException tce = new TailCallException(target, new Object[17]);
		System.arraycopy(arguments, 0, tce.arguments, 0, arguments.length);
		tce.arguments[16] = tce;
		return tce;
	}
}
