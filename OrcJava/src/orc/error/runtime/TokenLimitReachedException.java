package orc.error.runtime;

public class TokenLimitReachedException extends TokenException {
	public TokenLimitReachedException() {
		super("Token limit reached");
	}
}
