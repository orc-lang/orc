
package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.instrumentation.ProvidedTags;

import orc.util.ExpressionTag;

@Instrumentable(factory = ExpressionWrapper.class)
public abstract class Expression extends PorcENode {
	@CompilationFinal
	protected boolean isTail = false;
	
	public void setTail(boolean v) {
		isTail = v;
	}
	
	@Override
	protected boolean isTaggedWith(Class<?> tag) {
		// TODO: Provide tail information as a Tag.
		if (tag == ExpressionTag.class) {
			return true;
		} else {
			return super.isTaggedWith(tag);
		}
	}
}
