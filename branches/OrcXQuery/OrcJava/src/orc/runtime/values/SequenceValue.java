package orc.runtime.values;

import java.util.Queue;

import orc.runtime.Args;
import orc.runtime.sites.PartialSite;


/**
 * A sequence of values, usable as an iterator
 * 
 * @author dkitchin
 */
public class SequenceValue extends PartialSite {

	Queue<Value> values;
	
	public SequenceValue(Queue<Value> values) {
		this.values = values;
	}
	

	public Value evaluate(Args args) {
		
		if (values.isEmpty()) {
			return null;			// remain silent
		}
		else {
			return values.remove();
		}
		
	}

}
