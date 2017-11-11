
package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public abstract class SpecializationConfiguration {
	@CompilationFinal
	public static final boolean TruffleASTInlining = Boolean
			.parseBoolean(System.getProperty("orc.porce.truffleASTInlining", "false"));
	
	@CompilationFinal
	public static final int GetFieldMaxCacheSize = 
		Integer.parseInt(System.getProperty("orc.porce.cache.getFieldMaxCacheSize", "4"));

	@CompilationFinal
	public static final int InternalCallMaxCacheSize =
		Integer.parseInt(System.getProperty("orc.porce.cache.internalCallMaxCacheSize", "12"));
	@CompilationFinal
	public static final int ExternalDirectCallMaxCacheSize =
		Integer.parseInt(System.getProperty("orc.porce.cache.externalDirectCallMaxCacheSize", "8"));
	@CompilationFinal
	public static final int ExternalCPSCallMaxCacheSize = 
		Integer.parseInt(System.getProperty("orc.porce.cache.externalCPSCallMaxCacheSize", "12"));

	/**
	 * The maximum number of ns a functions may take and still have the spawn
	 * inlined.
	 * 
	 * The system property is in floating-point ms.
	 */
	@CompilationFinal
	public static final int InlineAverageTimeLimit = 
		(int) (Double.parseDouble(System.getProperty("orc.porce.inlineAverageTimeLimit", "0.1")) * 1000000);
	@CompilationFinal
	public static final int MinCallsForTimePerCall = 100;

	@CompilationFinal
	public static final boolean UniversalTCO = Boolean
			.parseBoolean(System.getProperty("orc.porce.universalTCO", "true"));
		
	@CompilationFinal
	public static final boolean SelfTCO = Boolean
			.parseBoolean(System.getProperty("orc.porce.selfTCO", "true"));
	
	@CompilationFinal
	public static final boolean InlineForceResolved = Boolean
			.parseBoolean(System.getProperty("orc.porce.optimizations.inlineForceResolved", "true"));
	
	@CompilationFinal
	public static final boolean InlineForceHalted = Boolean
			.parseBoolean(System.getProperty("orc.porce.optimizations.inlineForceHalted", "true"));
	
	@CompilationFinal
	public static final boolean SpecializeOnCounterStates = Boolean
			.parseBoolean(System.getProperty("orc.porce.optimizations.specializeOnCounterStates", "true"));
	
	@CompilationFinal
	public static final boolean ExternalCPSDirectSpecialization = Boolean
			.parseBoolean(System.getProperty("orc.porce.optimizations.externalCPSDirectSpecialization", "true"));
	
	@CompilationFinal
	public static final boolean EnvironmentCaching = Boolean
			.parseBoolean(System.getProperty("orc.porce.optimizations.environmentCaching", "true"));
}
