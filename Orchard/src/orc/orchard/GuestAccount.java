//
// GuestAccount.java -- Java class GuestAccount
// Project Orchard
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

/**
 * The guest account is special.
 * 
 * @author quark
 */
public class GuestAccount extends Account {
    public GuestAccount() {
        // set properties
        setQuota(OrchardProperties.getInteger("orc.orchard.GuestAccount.quota"));
        setLifespan(OrchardProperties.getInteger("orc.orchard.GuestAccount.lifespan"));
        setCanSendMail(OrchardProperties.getBoolean("orc.orchard.GuestAccount.canSendMail", getCanSendMail()));
        setCanImportJava(OrchardProperties.getBoolean("orc.orchard.GuestAccount.canImportJava", getCanImportJava()));
        setStackSize(OrchardProperties.getInteger("orc.orchard.GuestAccount.stackSize", getStackSize()));
        setTokenPoolSize(OrchardProperties.getInteger("orc.orchard.GuestAccount.tokenPoolSize", getTokenPoolSize()));
        setMaxThreads(OrchardProperties.getInteger("orc.orchard.GuestAccount.maxThreads", getMaxThreads()));
    }

    @Override
    public String getUsername() {
        return "(guest)";
    }

    @Override
    public boolean getIsGuest() {
        return true;
    }

    @Override
    protected void onNoMoreJobs() {
        // the guest account never needs to be deleted
    }
}
