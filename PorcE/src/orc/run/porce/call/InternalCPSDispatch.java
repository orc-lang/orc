
package orc.run.porce.call;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.SelfTailCallException;

@ImportStatic({ SpecializationConfiguration.class })
public abstract class InternalCPSDispatch extends Dispatch {
	protected InternalCPSDispatch(final PorcEExecutionRef execution) {
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
	
		
    @Specialization(guards = { "isTail", "getRootNodeCached() == target.body.getRootNode()" })
    public void selfTail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments) {
        Object[] frameArguments = frame.getArguments();
        System.arraycopy(arguments, 0, frameArguments, 1, arguments.length);
        frameArguments[0] = target.environment;        
        throw new SelfTailCallException();
    }
    
	@Specialization(guards = { "matchesSpecific(target, expected)" }, limit = "InternalCallMaxCacheSize")
    public void specific(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
    		@Cached("target") PorcEClosure expected, @Cached("create(expected.body)") DirectCallNode call) {
        call.call(buildArguments(target, arguments));
    }

	// The RootNode guard is required so that selfTail can be activated even
	// after universe has activated; without this universal would end up
	// handling those cases and cause TCO to fail based on optimizations.
	@Specialization(replaces = { "specific" }, guards = { "getRootNode() != target.body.getRootNode()" })
    public void universal(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments, 
    		@Cached("create()") IndirectCallNode call) {
        call.call(target.body, buildArguments(target, arguments));
    }
		
	public static InternalCPSDispatch create(final PorcEExecutionRef execution) {
		return InternalCPSDispatchNodeGen.create(execution);
	}
	
	/* Utilties */

	protected static boolean matchesSpecific(PorcEClosure target, PorcEClosure expected) {
		return expected.body == target.body;
	}	
	
	protected static Object[] buildArguments(PorcEClosure target, Object[] arguments) {
		//CompilerAsserts.compilationConstant(arguments.length);
		Object[] newArguments = new Object[arguments.length + 1];
		newArguments[0] = target.environment;
		System.arraycopy(arguments, 0, newArguments, 1, arguments.length);
		return newArguments;
	}
}
