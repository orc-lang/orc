//
// AccountsMBean.java -- Java interface AccountsMBean
// Project Orchard
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.util.Set;

import orc.orchard.jmx.JMXDescription;
import orc.orchard.jmx.JMXParam;

public interface AccountsMBean {
	@JMXDescription("List cached account IDs")
	public Set<Integer> getAccountIDs();

	@JMXDescription("Get the number of cached account IDs")
	public int getNumActiveAccounts();

	@JMXDescription("Create a new account")
	public boolean createAccount(@JMXParam("accountTypeID") int accountTypeID, @JMXParam("username") String username, @JMXParam("password") String password, @JMXParam("email") String email);

	@JMXDescription("Delete an existing account")
	public boolean dropAccount(@JMXParam("username") String username);

	@JMXDescription("Change an account's password")
	public boolean changePassword(@JMXParam("username") String username, @JMXParam("password") String password);

	@JMXDescription("Change an account's developer key")
	public boolean changeDeveloperKey(@JMXParam("username") String username);
}
