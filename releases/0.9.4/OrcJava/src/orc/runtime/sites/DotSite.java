/**
 * 
 */
package orc.runtime.sites;

import java.util.Map;
import java.util.TreeMap;

import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.trace.values.RecordValue;
import orc.trace.values.Marshaller;
import orc.trace.values.TraceableValue;
import orc.trace.values.Value;

/**
 * @author dkitchin
 * 
 * Dot-accessible sites should extend this class and declare their Orc-available
 * methods using addMethods. The code is forward-compatible with many possible
 * optimizations on the field lookup strategy.
 */
public abstract class DotSite extends EvalSite implements TraceableValue {

	Map<String,Object> methodMap;
	
	public DotSite()
	{
		methodMap = new TreeMap<String,Object>();
		this.addMethods();
	}
	
	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(Args args) throws TokenException {
		
		String f = args.fieldName();
		Object m = getMethod(f);
		
		if (m != null)
			{ return m; }
		else
			{ throw new MessageNotUnderstoodException(f); } 
	}
	
	Object getMethod(String f) {
		return methodMap.get(f);
	}

	// Subclasses implement this method with a sequence of addMethod calls.
	protected abstract void addMethods();	
	
	protected void addMethod(String f, Object s) {
		methodMap.put(f, s);
	}

	public Value marshal(Marshaller tracer) {
		RecordValue out = new RecordValue(getClass());
		for (Map.Entry<String, Object> entry : methodMap.entrySet()) {
			out.put(entry.getKey(), tracer.marshal(entry.getValue()));
		}
		return out;
	}
}
