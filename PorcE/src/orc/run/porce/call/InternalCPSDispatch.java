package orc.run.porce.call;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.nodes.Node.Child;

import orc.run.porce.Expression;
import orc.run.porce.PorcERootNode;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.SelfTailCallException;
import orc.run.porce.runtime.TailCallException;


public final class InternalCPSDispatch extends Dispatch {
	@Child
	protected InternalCPSDispatchInternal internal;
	@Child
	protected InternalCPSDispatchTailInternal tail;
	
	protected InternalCPSDispatch(final boolean forceInline, final PorcEExecutionRef execution) {
		super(execution);
		internal = InternalCPSDispatchInternal.createBare(forceInline, execution);
		tail = InternalCPSDispatchTailInternal.createBare(forceInline, execution);
	}
	 
	@Override
	public void setTail(boolean v) {
		super.setTail(v);
		internal.setTail(v);
		tail.setTail(v);
	}
	
	public void executeDispatchWithEnvironment(VirtualFrame frame, Object target, Object[] arguments) {
		arguments[0] = ((PorcEClosure)target).environment;
		if (isTail) {
			tail.execute(frame, target, arguments, true);
		} else {
			internal.execute(frame, (PorcEClosure)target, arguments);
		}
	}
	
	public void executeDispatch(VirtualFrame frame, Object target, Object[] arguments) {
		if (isTail) {
			tail.execute(frame, target, arguments, false);
		} else {
			internal.execute(frame, (PorcEClosure)target, buildArguments((PorcEClosure)target, arguments));
		}
	}
	
	protected static Object[] buildArguments(PorcEClosure target, Object[] arguments) {
		//CompilerAsserts.compilationConstant(arguments.length);
		Object[] newArguments = new Object[arguments.length + 1];
		newArguments[0] = target.environment;
		System.arraycopy(arguments, 0, newArguments, 1, arguments.length);
		return newArguments;
	}
	
	static InternalCPSDispatch createBare(final boolean forceInline, PorcEExecutionRef execution) {
		return new InternalCPSDispatch(forceInline, execution);
	}
	
	static InternalCPSDispatch createBare(PorcEExecutionRef execution) {
		return new InternalCPSDispatch(false, execution);
	}
	
	public static Dispatch create(final PorcEExecutionRef execution, boolean isTail) {
		return create(false, execution, isTail);
	}
	
	public static Dispatch create(final boolean forceInline, final PorcEExecutionRef execution, boolean isTail) {
		if (isTail)
			return createBare(forceInline, execution);
		else
			return CatchTailDispatch.create(createBare(forceInline, execution), execution);
	}
}

@ImportStatic({ SpecializationConfiguration.class })
@Introspectable
abstract class InternalCPSDispatchTailInternal extends DispatchBase {
	protected final boolean forceInline;

	protected InternalCPSDispatchTailInternal(final boolean forceInline, final PorcEExecutionRef execution) {
		super(execution);
		this.forceInline = forceInline;
    }
	
	@CompilationFinal
	private RootNode rootNode;
	
	public RootNode getRootNodeCached() {
		if (CompilerDirectives.inInterpreter()) {
			rootNode = getRootNode();
		}
		return rootNode;
	}

	public abstract void execute(VirtualFrame frame, Object target, Object[] arguments, boolean hasEnvironment);

	// Tail calls
	
	@Specialization(guards = { "SelfTCO", "isTail", "getRootNodeCached() == target.body.getRootNode()" })
	public void selfTail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments, boolean hasEnvironment) {
		Object[] frameArguments = frame.getArguments();
		if (hasEnvironment) {
		    System.arraycopy(arguments, 0, frameArguments, 0, arguments.length);
		} else {
			frameArguments[0] = target.environment;
		    System.arraycopy(arguments, 0, frameArguments, 1, arguments.length);
		}
	    throw new SelfTailCallException();
	}
	
	// The RootNode guard is required so that selfTail can be activated even
	// after tail has activated.
	// TODO: PERFORMANCE: I think the TCO loop might perform way better if I had this specialize on the body in target,
	//           any maybe break up the closure so it can be traced by the compiler. This would allow the compiler
	//           to more easily determine that this exception will always have a specific body to call meaning that
	//           it can be handled in the same place in the trampoline.
	@Specialization(guards = { "UniversalTCO", "isTail", "getRootNodeCached() != target.body.getRootNode()", "target.body == expected" }, 
			limit = "InternalCallMaxCacheSize")
	public void tailSpecific(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments, boolean hasEnvironment,
			@Cached("target.body") RootCallTarget expected,
			@Cached("createBinaryProfile()") ConditionProfile reuseTCE) {
    	Object[] thisArguments = frame.getArguments();
    	if (reuseTCE.profile(
    			/*arguments.length <= 16 &&*/ thisArguments.length == 17 && thisArguments[16] instanceof TailCallException)) {
    		TailCallException tce = (TailCallException)thisArguments[16];
    		if (hasEnvironment) {
    			System.arraycopy(arguments, 0, tce.arguments, 0, Math.min(arguments.length, 16));
    		} else {
    			tce.arguments[0] = target.environment;
    			System.arraycopy(arguments, 0, tce.arguments, 1, Math.min(arguments.length, 15));    			
    		}
    		tce.target = expected;
    		throw tce;
    	} else {
    		if (hasEnvironment) {
    			throw TailCallException.create(expected, arguments);
    		} else {
    			TailCallException tce = TailCallException.create(target.body);
    			tce.arguments[0] = target.environment;
    			System.arraycopy(arguments, 0, tce.arguments, 1, Math.min(arguments.length, 15));    			
    			throw tce;
    		}
    	}
	}
	
	@Specialization(guards = { "UniversalTCO", "isTail", "getRootNodeCached() != target.body.getRootNode()" })
	public void tail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments, boolean hasEnvironment,
			@Cached("createBinaryProfile()") ConditionProfile reuseTCE) {
    	Object[] thisArguments = frame.getArguments();
    	if (reuseTCE.profile(
    			/*arguments.length <= 16 &&*/ thisArguments.length == 17 && thisArguments[16] instanceof TailCallException)) {
    		TailCallException tce = (TailCallException)thisArguments[16];
    		if (hasEnvironment) {
    			System.arraycopy(arguments, 0, tce.arguments, 0, Math.min(arguments.length, 16));
    		} else {
    			tce.arguments[0] = target.environment;
    			System.arraycopy(arguments, 0, tce.arguments, 1, Math.min(arguments.length, 15));    			
    		}
    		tce.target = target.body;
    		throw tce;
    	} else {
    		if (hasEnvironment) {
    			throw TailCallException.create(target.body, arguments);
    		} else {
    			TailCallException tce = TailCallException.create(target.body);
    			tce.arguments[0] = target.environment;
    			System.arraycopy(arguments, 0, tce.arguments, 1, Math.min(arguments.length, 15));    			
    			throw tce;
    		}
    	}
	}
		
	static InternalCPSDispatchTailInternal createBare(final boolean forceInline, final PorcEExecutionRef execution) {
		return InternalCPSDispatchTailInternalNodeGen.create(forceInline, execution);
	}	
}


@ImportStatic({ SpecializationConfiguration.class })
@Introspectable
abstract class InternalCPSDispatchInternal extends DispatchBase {
	protected final boolean forceInline;

	protected InternalCPSDispatchInternal(final boolean forceInline, final PorcEExecutionRef execution) {
		super(execution);
		this.forceInline = forceInline;
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
	
//	// Tail calls
//		
//    @Specialization(guards = { "SelfTCO", "isTail", "getRootNodeCached() == target.body.getRootNode()" })
//    public void selfTail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments) {
//        Object[] frameArguments = frame.getArguments();
//        System.arraycopy(arguments, 0, frameArguments, 0, arguments.length);
//        throw new SelfTailCallException();
//    }
//    
//	// The RootNode guard is required so that selfTail can be activated even
//	// after tail has activated.
//    // TODO: PERFORMANCE: I think the TCO loop might perform way better if I had this specialize on the body in target,
//    //           any maybe break up the closure so it can be traced by the compiler. This would allow the compiler
//    //           to more easily determine that this exception will always have a specific body to call meaning that
//    //           it can be handled in the same place in the trampoline.
//    @Specialization(guards = { "UniversalTCO", "isTail", "getRootNodeCached() != target.body.getRootNode()", "target.body == expected" }, 
//    		limit = "InternalCallMaxCacheSize")
//    public void tailSpecific(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
//    		@Cached("target.body") RootCallTarget expected,
//    		@Cached("createBinaryProfile()") ConditionProfile reuseTCE) {
//    	throw TailCallException.throwReused(frame, reuseTCE, expected, arguments);
//    }
//    
//    @Specialization(guards = { "UniversalTCO", "isTail", "getRootNodeCached() != target.body.getRootNode()" })
//    public void tail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
//    		@Cached("createBinaryProfile()") ConditionProfile reuseTCE) {
//    	throw TailCallException.throwReused(frame, reuseTCE, target.body, arguments);
//    }
//    
    // Non-tail calls
 
	@Specialization(guards = { "TruffleASTInlining", "forceInline", "body != null", "matchesSpecific(target, expected)" }, 
			limit = "InternalCallMaxCacheSize")
    public void specificInline(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
    		@Cached("target") PorcEClosure expected, 
    		@Cached("getPorcEBody(target)") Expression body, @Cached("getPorcEFrameDescriptor(target)") FrameDescriptor fd) {
		final VirtualFrame nestedFrame = Truffle.getRuntime().createVirtualFrame(arguments, fd);
		body.execute(nestedFrame);
    }
	
	@Specialization(guards = { "matchesSpecific(target, expected)" }, limit = "InternalCallMaxCacheSize")
    public void specific(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
    		@Cached("target") PorcEClosure expected, @Cached("create(expected.body)") DirectCallNode call) {
		CompilerDirectives.interpreterOnly(() -> {
			if (forceInline)
				call.forceInlining();
		});
		
        call.call(arguments);
    }
	
	@Specialization(replaces = { "specific", "specificInline" })
    public void universal(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments, 
    		@Cached("create()") IndirectCallNode call) {
        call.call(target.body, arguments);
    }
		
	static InternalCPSDispatchInternal createBare(final boolean forceInline, final PorcEExecutionRef execution) {
		return InternalCPSDispatchInternalNodeGen.create(forceInline, execution);
	}	
	
	
	/* Utilties */

	protected static boolean matchesSpecific(PorcEClosure target, PorcEClosure expected) {
		return expected.body == target.body;
	}
	
	protected static Expression getPorcEBody(PorcEClosure target) {
		RootNode r = target.body.getRootNode();
		if (r instanceof PorcERootNode) {
			return (Expression)((PorcERootNode)r).getBody().copy();
		} else {
			return null;
		}
	}
	
	protected static FrameDescriptor getPorcEFrameDescriptor(PorcEClosure target) {
		RootNode r = target.body.getRootNode();
		if (r instanceof PorcERootNode) {
			return r.getFrameDescriptor();
		} else {
			return null;
		}
	}
}
