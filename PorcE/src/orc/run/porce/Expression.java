
package orc.run.porce;

import orc.util.ExpressionTag;

import com.oracle.truffle.api.instrumentation.Instrumentable;

@Instrumentable(factory = ExpressionWrapper.class)
public abstract class Expression extends PorcENode {
	@Override
	protected boolean isTaggedWith(Class<?> tag) {
		if (tag == ExpressionTag.class) {
			return true;
		} else {
			return super.isTaggedWith(tag);
		}
	}
}
