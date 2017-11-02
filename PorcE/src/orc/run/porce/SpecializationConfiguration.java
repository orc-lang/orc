
package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public abstract class SpecializationConfiguration {
	@CompilationFinal
    public static final int GetFieldMaxCacheSize = 4;
	
	@CompilationFinal
    public static final int InternalCallMaxCacheSize = 50;
	@CompilationFinal
    public static final int ExternalDirectCallMaxCacheSize = 4;
	@CompilationFinal
    public static final int ExternalCPSCallMaxCacheSize = 4;
	
	@CompilationFinal
    public static final int InlineAverageTimeLimit = 1 * 1000000; // In ns
	@CompilationFinal
    public static final int MinCallsForTimePerCall = 100;
}
