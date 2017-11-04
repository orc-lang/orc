
package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public abstract class SpecializationConfiguration {
	@CompilationFinal
	public static final int GetFieldMaxCacheSize = 4;

	@CompilationFinal
	public static final int InternalCallMaxCacheSize = 12;
	@CompilationFinal
	public static final int ExternalDirectCallMaxCacheSize = 8;
	@CompilationFinal
	public static final int ExternalCPSCallMaxCacheSize = 12;

	/**
	 * The maximum number of ns a functions must take and still have the spawn
	 * inlined.
	 * 
	 * The system property is in floating-point ms.
	 */
	@CompilationFinal
	public static final int InlineAverageTimeLimit = 
		(int) (Double.parseDouble(System.getProperty("orc.porce.inlineAverageTimeLimit", "1.0")) * 1000000);
	@CompilationFinal
	public static final int MinCallsForTimePerCall = 100;

	@CompilationFinal
	public static final boolean UniversalTCO = Boolean
			.parseBoolean(System.getProperty("orc.porce.universalTCO", "true"));
	
	@CompilationFinal
	public static final boolean TruffleASTInlining = Boolean
			.parseBoolean(System.getProperty("orc.porce.truffleASTInlining", "false"));
}
