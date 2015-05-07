package orc.progress;

/**
 * Generic interface for things which track the progress
 * of tasks.
 * @author quark
 */
public interface ProgressListener {
		public boolean isCanceled();
		public void setNote(final String note);
		public void setProgress(double v);
}