package orc.lib.state.types;

import java.util.List;

import orc.type.ArrowType;
import orc.type.DotType;
import orc.type.MutableContainerType;
import orc.type.Type;

public class SemaphoreType extends DotType {

	public String toString() {
		return "Semaphore";
	}
	
	public SemaphoreType() {
		Type t = new ArrowType(Type.TOP); /* A method which takes no arguments and returns a signal */
		addField("acquire", t);
		addField("acquirenb", t);
		addField("release", t);
		addField("snoop", t);
		addField("snoopnb", t);
	}
	
}
