//
// Bot.java -- Java class Bot
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.extended.type;

/**
 * The type 'Bot', subtype of all other types.
 * 
 * @author dkitchin
 */
public class Bot extends Type {

	@Override
	public orc.ast.simple.type.Type simplify() {
		return orc.ast.simple.type.Type.BOT;
	}

	@Override
	public String toString() {
		return "Bot";
	}
}
