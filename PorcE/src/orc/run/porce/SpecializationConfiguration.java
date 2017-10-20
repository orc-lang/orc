
package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public abstract class SpecializationConfiguration {
	@CompilationFinal
    public static final int GetFieldMaxCacheSize = 4;
	@CompilationFinal
    public static final int InternalCallMaxCacheSize = 12;
	@CompilationFinal
    public static final int ExternalDirectCallMaxCacheSize = 4;
	@CompilationFinal
    public static final int ExternalCPSCallMaxCacheSize = 4;
	@CompilationFinal
    public static final int InlineAverageTimeLimit = 1;
}
