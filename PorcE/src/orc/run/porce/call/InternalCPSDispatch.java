
package orc.run.porce.call;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.nodes.Node.Child;

import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.SelfTailCallException;
import orc.run.porce.runtime.TailCallException;


public final class InternalCPSDispatch extends Dispatch {
	@Child
	protected InternalCPSDispatchInternal internal;
	
	protected InternalCPSDispatch(final PorcEExecutionRef execution) {
		super(execution);
		internal = InternalCPSDispatchInternal.createBare(execution);
	}
	 
	@Override
	public void setTail(boolean v) {
		super.setTail(v);
		internal.setTail(v);
	}
	
	public void executeDispatchWithEnvironment(VirtualFrame frame, Object target, Object[] arguments) {
		arguments[0] = ((PorcEClosure)target).environment;
		internal.execute(frame, (PorcEClosure)target, arguments);
	}
	
	public void executeDispatch(VirtualFrame frame, Object target, Object[] arguments) {
		internal.execute(frame, (PorcEClosure)target, buildArguments((PorcEClosure)target, arguments));
	}
	
	protected static Object[] buildArguments(PorcEClosure target, Object[] arguments) {
		//CompilerAsserts.compilationConstant(arguments.length);
		Object[] newArguments = new Object[arguments.length + 1];
		newArguments[0] = target.environment;
		System.arraycopy(arguments, 0, newArguments, 1, arguments.length);
		return newArguments;
	}
	
	static InternalCPSDispatch createBare(PorcEExecutionRef execution) {
		return new InternalCPSDispatch(execution);
	}
	
	public static Dispatch create(final PorcEExecutionRef execution, boolean isTail) {
		if (isTail)
			return createBare(execution);
		else
			return CatchTailDispatch.create(createBare(execution), execution);
	}
}


@ImportStatic({ SpecializationConfiguration.class })
@Introspectable
abstract class InternalCPSDispatchInternal extends DispatchBase {
	protected InternalCPSDispatchInternal(final PorcEExecutionRef execution) {
		super(execution);
    }
	
	@CompilationFinal
	private RootNode rootNode;
	
	public RootNode getRootNodeCached() {
		if (CompilerDirectives.inInterpreter()) {
			rootNode = getRootNode();
		}
		return rootNode;
	}
	
	public abstract void execute(VirtualFrame frame, Object target, Object[] arguments);

	// TODO: It would probably improve compile times to split tail and non-tail cases into separate classes so only one set has to be checked for any call.
	
	// Tail calls
		
    @Specialization(guards = { "isTail", "getRootNodeCached() == target.body.getRootNode()" })
    public void selfTail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments) {
        Object[] frameArguments = frame.getArguments();
        System.arraycopy(arguments, 0, frameArguments, 0, arguments.length);
        throw new SelfTailCallException();
    }
    
	// The RootNode guard is required so that selfTail can be activated even
	// after tail has activated.
    @Specialization(guards = { "isTail", "getRootNodeCached() != target.body.getRootNode()" })
    public void tail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
    		@Cached("createBinaryProfile()") ConditionProfile reuseTCE) {
    	Object[] thisArguments = frame.getArguments();
    	if (reuseTCE.profile(
    			/*arguments.length <= 16 &&*/ thisArguments.length == 17 && thisArguments[16] instanceof TailCallException)) {
    		TailCallException tce = (TailCallException)thisArguments[16];
    		System.arraycopy(arguments, 0, tce.arguments, 0, arguments.length);
    		tce.target = target;
    		throw tce;
    	}
    	
        throw TailCallException.create(target, arguments);
    }
    
    // Non-tail calls
    
	@Specialization(guards = { "matchesSpecific(target, expected)" }, limit = "InternalCallMaxCacheSize")
    public void specific(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
    		@Cached("target") PorcEClosure expected, @Cached("create(expected.body)") DirectCallNode call) {
        call.call(arguments);
    }
	
	@Specialization(replaces = { "specific" })
    public void universal(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments, 
    		@Cached("create()") IndirectCallNode call) {
        call.call(target.body, arguments);
    }
		
	static InternalCPSDispatchInternal createBare(final PorcEExecutionRef execution) {
		return InternalCPSDispatchInternalNodeGen.create(execution);
	}	
	
	
	/* Utilties */

	protected static boolean matchesSpecific(PorcEClosure target, PorcEClosure expected) {
		return expected.body == target.body;
	}	
}
