package orc.lib.state.types;

import java.util.List;

import orc.type.ArrowType;
import orc.type.DotType;
import orc.type.MutableContainerType;
import orc.type.Type;

public class CounterType extends DotType {

	public String toString() {
		return "Counter";
	}
	
	public CounterType() {
		Type t = new ArrowType(Type.TOP); /* A method which takes no arguments and returns a signal */
		addField("inc", t);
		addField("dec", t);
		addField("onZero", t);
		addField("value", new ArrowType(Type.INTEGER));
	}
	
}
