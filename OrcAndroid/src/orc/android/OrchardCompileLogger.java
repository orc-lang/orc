package orc.android;

import java.util.List;

import orc.ast.AST;
import orc.error.compiletime.CompileLogger;
import scala.util.parsing.input.Position;

/**
 * A CompileMessageRecorder that collects logged compile messages into a list
 *
 * @author jthywiss
 */
public class OrchardCompileLogger implements CompileLogger {
	public static class CompileMessage {

		public final Severity severity;
		public final int code;
		public final String message;
		public final Position position;
		public final AST astNode;
		public final Throwable exception;

		/**
		 * Constructs an object of class CompileMessage.
		 *
		 * @param severity
		 * @param code
		 * @param message
		 * @param position
		 * @param astNode
		 * @param exception
		 */
		public CompileMessage(final Severity severity, final int code, final String message, final Position position, final AST astNode, final Throwable exception) {
			this.severity = severity;
			this.code = code;
			this.message = message;
			this.position = position;
			this.astNode = astNode;
			this.exception = exception;
		}

		public String longMessage() {
			if (position != null) {
				return position.toString() + ": " + message + "\n" + position.longString();
			} else {
				return "<undefined argPosition>: " + message;
			}
		}

	}
	private Severity maxSeverity = Severity.UNKNOWN;
	private final List<CompileMessage> msgList;

	/**
	 * Constructs an object of class OrchardCompileLogger.
	 */
	public OrchardCompileLogger(final List<CompileMessage> msgList) {
		this.msgList = msgList;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#beginProcessing(java.lang.String)
	 */
	@Override
	public void beginProcessing(final String filename) {
		maxSeverity = Severity.UNKNOWN;
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#endProcessing(java.lang.String)
	 */
	@Override
	public void endProcessing(final String filename) {
		// Nothing needed
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(Severity, int, String, Position, AST, Throwable)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, final Position location, final AST astNode, final Throwable exception) {

		maxSeverity = severity.ordinal() > maxSeverity.ordinal() ? severity : maxSeverity;
		System.out.println(message);
		msgList.add(new CompileMessage(severity, code, message, location, astNode, exception));
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(Severity, int, String, Position, Throwable)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, final Position location, final Throwable exception) {
		recordMessage(severity, code, message, location, null, exception);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(Severity, int, String, Position, AST)
	 */
	@Override
	public void recordMessage(final Severity severity, final int code, final String message, final Position location, final AST astNode) {
		recordMessage(severity, code, message, location, astNode, null);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#recordMessage(orc.error.compiletime.CompileLogger.Severity, int, java.lang.String)
	 */
	public void recordMessage(final Severity severity, final int code, final String message) {
		recordMessage(severity, code, message, null, null, null);
	}

	/* (non-Javadoc)
	 * @see orc.error.compiletime.CompileLogger#getMaxSeverity()
	 */
	@Override
	public Severity getMaxSeverity() {
		return maxSeverity;
	}
}

