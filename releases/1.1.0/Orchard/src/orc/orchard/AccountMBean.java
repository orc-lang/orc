//
// AccountMBean.java -- Java interface AccountMBean
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.util.Set;

public interface AccountMBean {
	public Integer getLifespan();

	public void setLifespan(Integer lifespan);

	public Integer getQuota();

	public void setQuota(Integer quota);

	public boolean getCanSendMail();

	public void setCanSendMail(boolean value);

	public boolean getCanImportJava();

	public void setCanImportJava(boolean value);

	public boolean getIsGuest();

	public Set<String> getJobIDs();

	public int getNumNewJobs();

	public int getNumRunningJobs();

	public int getNumDeadJobs();

	public void finishOldJobs();
}
