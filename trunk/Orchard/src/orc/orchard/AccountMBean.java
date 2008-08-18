package orc.orchard;

import java.util.Set;

public interface AccountMBean {
	public Integer getLifespan();
	public void setLifespan(Integer lifespan);
	public Integer getQuota();
	public void setQuota(Integer quota);
	public boolean getIsGuest();
	public Set<String> getJobIDs();
	public int getNumNewJobs();
	public int getNumRunningJobs();
	public int getNumDeadJobs();
	public void finishOldJobs();
}
