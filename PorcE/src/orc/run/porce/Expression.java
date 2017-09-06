
package orc.run.porce;

import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.instrumentation.ProvidedTags;

import orc.util.ExpressionTag;

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
