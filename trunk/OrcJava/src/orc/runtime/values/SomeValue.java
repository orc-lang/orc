package orc.runtime.values;

/* Common ancestor for the option values */

public class SomeValue<V extends Value> extends OptionValue<V> { 
	
	public V content;
	
	public SomeValue(V content) {
		this.content = content;
	}
	
	public boolean isSome() { return true; }
	
	public V untag() { return content; }
	
}
