//
// DocExamplesTest.java -- Scala class/trait/object DocExamplesTest
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Apr 04, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import java.io.File;

import junit.framework.Test;
import orc.script.OrcBindings;

/**
 * 
 *
 * @author dkitchin
 */
public class DocExamplesTest extends ExamplesTest {

  public static Test suite() {
    OrcBindings bindings = new OrcBindings();
    return buildSuite(bindings, new File("../OrcDocs/build/examples"));
  }
  
}
