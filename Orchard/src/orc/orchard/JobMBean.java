package orc.orchard;

import java.util.Date;

public interface JobMBean {
	public Date getStartDate();
	public String getState();
	public void halt();
	public void finish();
	public int getNumPublications();
}
