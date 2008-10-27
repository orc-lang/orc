package orc.error.runtime;

public class TokenLimitReachedError extends TokenError {
	public TokenLimitReachedError() {
		super("Token limit reached");
	}
}
