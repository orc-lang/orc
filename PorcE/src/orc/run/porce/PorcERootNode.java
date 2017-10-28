
package orc.run.porce;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicLong;

import scala.Option;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import orc.ast.ASTWithIndex;
import orc.ast.porc.PorcAST;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.HaltException;
import orc.run.porce.call.CatchSelfTailCall;
import orc.run.porce.runtime.KilledException;
import orc.run.porce.runtime.SourceSectionFromPorc;

public class PorcERootNode extends RootNode implements HasPorcNode, HasId {
    private final static boolean assertionsEnabled = false;
    
    
    // FIXME: All these counters should probably just be volatile and let the accesses be racy (like the JVM does for call counters). 
    private final AtomicLong totalSpawnedTime = new AtomicLong(0);
    private final AtomicLong totalSpawnedCalls = new AtomicLong(0);
    
    private final AtomicLong totalTime = new AtomicLong(0);
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong totalSpawns = new AtomicLong(0);
    private final AtomicLong totalBindSingle = new AtomicLong(0);
    private final AtomicLong totalBindJoin = new AtomicLong(0);
    private final AtomicLong totalHalt = new AtomicLong(0);
    private final AtomicLong totalPublication = new AtomicLong(0);
    @CompilationFinal
    private long timePerCall = -1;
    
    // FIXME: Do to how the time value is collected (see addRunTime and the calls to it). Calls will be multiple counted in some cases (recursive calls).
   
    final public void addSpawnedCall(long time) {
        totalSpawnedTime.getAndAdd(time);
        totalSpawnedCalls.getAndIncrement();        
    }
    
    final public long getTimePerCall() {
    	if (timePerCall < 0 || CompilerDirectives.inInterpreter()) {
//        	long t = totalTime.get();
//        	long n = totalCalls.get();
        	long t = totalSpawnedTime.get();
        	long n = totalSpawnedCalls.get();
    		
    		if (n >= SpecializationConfiguration.MinCallsForTimePerCall) {
        		CompilerDirectives.transferToInterpreterAndInvalidate();
        		timePerCall = t / n;
    		} else {
    			return Long.MAX_VALUE;
    		}
    	}
    	/*{
        	long t = totalTime.get();
        	long n = totalCalls.get();
        	Logger.info(() -> "getTimePerCall " + t + "  /  " + n);
    	}*/
		return timePerCall;
    }
    
    final public void incrementSpawn() {
    	if (CompilerDirectives.inInterpreter()) {
    		totalSpawns.getAndIncrement();
    	}
    }

    final public void incrementHalt() {
    	if (CompilerDirectives.inInterpreter()) {
    		totalHalt.getAndIncrement();
    	}
    }

    final public void incrementPublication() {
    	if (CompilerDirectives.inInterpreter()) {
    		totalPublication.getAndIncrement();
    	}
    }

    final public void incrementBindSingle() {
    	if (CompilerDirectives.inInterpreter()) {
    		totalBindSingle.getAndIncrement();
    	}
    }

    final public void incrementBindJoin() {
    	if (CompilerDirectives.inInterpreter()) {
    		totalBindJoin.getAndIncrement();
    	}
    }


	final public void addRunTime(long t) {
    	if (CompilerDirectives.inInterpreter()) {
    		totalTime.getAndAdd(t);
    	}
	}

	public scala.Tuple9<Long, Long, Long, Long, Long, Long, Long, Long, Long> getCollectedCallInformation() {
		return new scala.Tuple9<>(
				totalTime.get(), totalCalls.get(), 
				totalSpawns.get(), totalBindSingle.get(), totalBindJoin.get(), totalHalt.get(), totalPublication.get(),
				totalSpawnedTime.get(), totalSpawnedCalls.get()        
				);
    }

    private Option<PorcAST> porcNode = Option.apply(null);

	public void setPorcAST(final PorcAST ast) {
		CompilerAsserts.neverPartOfCompilation();
		porcNode = Option.apply(ast);
		section = SourceSectionFromPorc.apply(ast);
	}

    @Override
    public Option<PorcAST> porcNode() {
        return porcNode;
    }
    
    @CompilationFinal
    private SourceSection section = null;

    @Override
    public SourceSection getSourceSection() {
        return section;
    }
    
    @Override
    public String getName() {
        String name = "<no AST>";
        scala.Option<PorcAST> optAst = porcNode();
        if (optAst.isDefined()) {
            final PorcAST ast = optAst.get();
            name = "<N/A>";
            if (ast instanceof orc.ast.hasOptionalVariableName) {
                scala.Option<String> optName = ((orc.ast.hasOptionalVariableName) ast).optionalVariableName();
                if (optName.isDefined()) {
                    name = optName.get();
                } else {
                    name = "<unset>";
                }
            }
        }
        return name;
    }

    @Override
    public int getId() {
        return ((Integer) ((ASTWithIndex) porcNode().get()).optionalIndex().get()).intValue();
    }

    protected @Child Expression body;
    private final int nArguments;
    private final int nCaptured;

    public PorcERootNode(final PorcELanguage language, final FrameDescriptor descriptor, final Expression body, final int nArguments, final int nCaptured) {
        super(language, descriptor);
        this.body = body;
        this.nArguments = nArguments;
        this.nCaptured = nCaptured;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        if (assertionsEnabled) {
            final Object[] arguments = frame.getArguments();
            if (arguments.length != nArguments + 1) {
                throwArityException(arguments.length - 1, nArguments);
            }
            final Object[] captureds = (Object[]) arguments[0];
            if (captureds.length != nCaptured) {
                InternalPorcEError.capturedLengthError(nCaptured, captureds.length);
            }
        }

        long startTime = 0;
        if (CompilerDirectives.inInterpreter())
        	startTime = System.nanoTime();
        
        try {
            final Object ret = body.execute(frame);
            return ret;
        } catch (KilledException | HaltException e) {
            transferToInterpreter();
            Logger.log(Level.WARNING, () -> "Caught " + e + " in root node " + this, e);
            return PorcEUnit.SINGLETON;
        } finally {
        	if (CompilerDirectives.inInterpreter() && startTime > 0) {
        		totalTime.getAndAdd(System.nanoTime() - startTime);
        		totalCalls.getAndIncrement();
        	}
        }
    }

    @TruffleBoundary(allowInlining = true)
    private static void throwArityException(final int nReceived, final int nExpected) {
        throw new ArityMismatchException(nExpected, nReceived);
    }

    public static PorcERootNode create(final PorcELanguage language, final FrameDescriptor descriptor, final Expression body, final int nArguments, final int nCaptured) {
    	// Add self tail call catcher to the body during construction.
        return new PorcERootNode(language, descriptor, CatchSelfTailCall.create(body), nArguments, nCaptured);
    }

    @Override
    public String toString() {
        return String.format("PorcE.%s", getName());
    }
}
