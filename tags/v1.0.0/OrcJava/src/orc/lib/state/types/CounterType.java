package orc.lib.state.types;

import java.util.List;

import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;
import orc.type.tycon.MutableContainerType;

public class CounterType extends DotType {

	public String toString() {
		return "Counter";
	}
	
	public CounterType() {
		super();
		Type t = new ArrowType(Type.TOP); /* A method which takes no arguments and returns a signal */
		addField("inc", t);
		addField("dec", t);
		addField("onZero", t);
		addField("value", new ArrowType(Type.INTEGER));
	}
	
}
