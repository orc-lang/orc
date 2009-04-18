package orc.progress;

/**
 * Progress listener which does nothing.
 * @author quark
 */
public final class NullProgressListener implements ProgressListener {
	public final static NullProgressListener singleton = new NullProgressListener();
	private NullProgressListener() {}

	public boolean isCanceled() {
		return false;
	}

	public void setNote(String note) {
		// do nothing
	}

	public void setProgress(double v) {
		// do nothing
	}
}