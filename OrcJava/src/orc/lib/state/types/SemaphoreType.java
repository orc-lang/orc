package orc.lib.state.types;

import java.util.List;

import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;
import orc.type.tycon.MutableContainerType;

public class SemaphoreType extends DotType {

	public String toString() {
		return "Semaphore";
	}
	
	public SemaphoreType() {
		super();
		Type t = new ArrowType(Type.TOP); /* A method which takes no arguments and returns a signal */
		addField("acquire", t);
		addField("acquirenb", t);
		addField("release", t);
		addField("snoop", t);
		addField("snoopnb", t);
	}
	
}
