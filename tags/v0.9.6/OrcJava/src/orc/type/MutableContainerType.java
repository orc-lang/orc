package orc.type;

import java.util.LinkedList;
import java.util.List;

public abstract class MutableContainerType extends Tycon {

	public List<Variance> variances() {
		List<Variance> vs = new LinkedList<Variance>();
		vs.add(Variance.INVARIANT);
		return vs;
	}
	
}
