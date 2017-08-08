package orc.run.porce.runtime;

/**
 * A java class to hold the JIT compile time constants used by the Truffle code.
 * 
 * Sadly, the Graal compiler doesn't seem to be able to see Scala constants as constants.
 * 
 * @author amp
 *
 */
public abstract class FutureConstants {
	public final static class Sentinal {
		private String name;

		public Sentinal(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	public static final Object Unbound = new Sentinal("Unbound");
	public static final Object Halt = new Sentinal("Halt");
	
	public static final orc.FutureState Orc_Stopped = orc.FutureState.Stopped$.MODULE$;
	public static final orc.FutureState Orc_Unbound = orc.FutureState.Unbound$.MODULE$;
}
