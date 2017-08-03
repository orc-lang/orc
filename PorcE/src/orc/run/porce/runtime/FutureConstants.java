package orc.run.porce.runtime;

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
