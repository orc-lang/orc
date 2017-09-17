
package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public abstract class SpecializationConfiguration {
	@CompilationFinal
    public static int GetFieldMaxCacheSize = 4;
	@CompilationFinal
    public static int InternalCallMaxCacheSize = 4;
	@CompilationFinal
    public static int ExternalDirectCallMaxCacheSize = 4;
	@CompilationFinal
    public static int ExternalCPSCallMaxCacheSize = 4;
}
