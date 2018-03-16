//
// PorcEFutureReader.scala -- Scala trait PorcEFutureReader
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import orc.FutureReader

/** An extension of FutureReader which enables the call targets to be inlined.
 * 
 */
trait PorcEFutureReader extends FutureReader {
  /** Halt this reader and return a closure if one needs to be called to handle the halt.
   *  Otherwise, return null.
   */
  def fastHalt(): PorcEClosure
  /** Publish into this reader and return a CallClosureSchedulable if one needs to be called to handle the publication.
   *  Otherwise, return null.
   *  
   *  The CallClosureSchedulable includes both the closure and the arguments it expects.
   */
  def fastPublish(v: AnyRef): CallClosureSchedulable
}