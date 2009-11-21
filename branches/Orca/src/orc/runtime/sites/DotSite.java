/**
 * 
 */
package orc.runtime.sites;

import java.util.Map;
import java.util.TreeMap;

import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.error.runtime.UncallableValueException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.transaction.Transaction;
import orc.trace.values.RecordValue;
import orc.trace.values.Marshaller;
import orc.trace.values.TraceableValue;
import orc.trace.values.Value;

/**
 * @author dkitchin
 * 
 * Dot-accessible sites should extend this class and declare their Orc-available
 * methods using {@link #addMembers()}. The code is forward-compatible with many possible
 * optimizations on the field lookup strategy.
 * 
 * A dot site may also have a default behavior which allows it to behave like
 * a normal site. If its argument is not a message, it displays that 
 * default behavior, if implemented. If there is no default behavior, it
 * raises a type error.
 * 
 */
public abstract class DotSite extends TransactionalSite implements TraceableValue {

	Map<String,Object> methodMap;
	
	public DotSite()
	{
		methodMap = new TreeMap<String,Object>();
		this.addMembers();
	}
	
	
	/*
	 * A dotsite within a transaction behaves normally except that its default behavior must
	 * be transactional too.
	 */
	public void callSite(Args args, Token t, Transaction transaction) throws TokenException {
		
		String f;
		// Check if the argument is a message
		try {
			f = args.fieldName();
		}
		catch (TokenException e) {
			// If not, invoke the default behavior and return.
			defaultTo(args, t, transaction);
			return;
		}
		
		
		// If it is a message, look it up.
		Object m = getMember(f);
		if (m != null)
			{ t.resume(m); }
		else
			{ throw new MessageNotUnderstoodException(f); }
		
		
	}
	
	Object getMember(String f) {
		return methodMap.get(f);
	}

	protected abstract void addMembers();	
	
	protected void addMember(String f, Object s) {
		methodMap.put(f, s);
	}
	
	protected void defaultTo(Args args, Token token, Transaction transaction) throws TokenException {
		throw new UncallableValueException("This dot site has no default behavior; it only responds to messages.");
	}
	

	public Value marshal(Marshaller tracer) {
		RecordValue out = new RecordValue(getClass());
		for (Map.Entry<String, Object> entry : methodMap.entrySet()) {
			out.put(entry.getKey(), tracer.marshal(entry.getValue()));
		}
		return out;
	}
}
