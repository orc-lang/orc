package orc.progress;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.ProgressMonitor;
import static javax.swing.SwingUtilities.invokeLater;

/**
 * A ProgressListener which runs a ProgressMonitor to display progress.
 * @author quark
 */
public final class ProgressMonitorListener implements ProgressListener {
	private ProgressMonitor progress;
	private AtomicBoolean isCanceled = new AtomicBoolean(false);
	public ProgressMonitorListener(Component parent, Object message, String note) {
		progress = new ProgressMonitor(parent, message, note, 0, 100);
	}
	
	public boolean isCanceled() {
		return isCanceled.get();
	}
	
	public void setNote(final String note) {
		invokeLater(new Runnable() {
			public void run() {
				if (progress.isCanceled()) isCanceled.set(true);
				progress.setNote(note);
			}
		});
	}
	
	public void setProgress(final double v) {
		invokeLater(new Runnable() {
			public void run() {
				if (progress.isCanceled()) isCanceled.set(true);
				progress.setProgress((int)(v*100));
			}
		});
	}
}
