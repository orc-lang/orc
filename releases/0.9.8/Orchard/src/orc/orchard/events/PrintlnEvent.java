package orc.orchard.events;

/**
 * Whenever a "print" or "println" site is called,
 * the output is buffered and sent to the client with
 * println events. Well-written Orc programs should
 * use publications to communicate with the client,
 * but using prints is convenient for short scripts.
 * 
 * @author quark
 */
public class PrintlnEvent extends JobEvent {
	/**
	 * The newline terminator is implicit, so that the client can use whatever
	 * newlines are appropriate for their environment.
	 */
	public String line;
	public PrintlnEvent() {}
	public PrintlnEvent(String line) {
		this.line = line;
	}
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}
