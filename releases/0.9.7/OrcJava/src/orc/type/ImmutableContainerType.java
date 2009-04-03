package orc.type;

import java.util.LinkedList;
import java.util.List;

public abstract class ImmutableContainerType extends Tycon {

	public List<Variance> variances() {
		List<Variance> vs = new LinkedList<Variance>();
		vs.add(Variance.COVARIANT);
		return vs;
	}
	
}
