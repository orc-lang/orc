package orc.progress;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.ProgressMonitor;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * Report progress of a subtask to a progress listener for a larger task.
 * @author quark
 */
public final class SubProgressListener implements ProgressListener {
	private ProgressListener parent;
	private double min;
	private double max;
	/**
	 * min and max represent the progress range tracked by this listener
	 * within the larger task tracked by parent.
	 */
	public SubProgressListener(ProgressListener parent, double min, double max) {
		assert min < max;
		this.parent = parent;
		this.min = min;
		this.max = max;
	}
	
	public boolean isCanceled() {
		return parent.isCanceled();
	}
	
	public void setNote(String note) {
		parent.setNote(note);
	}
	
	/**
	 * Progress reported here is interpreted as the percentage of
	 * the subtask complete, which we translate into the percentage
	 * of the overall task complete.
	 */
	public void setProgress(double v) {
		parent.setProgress(min + v*(max - min));
	}
}
