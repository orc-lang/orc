//
// ProgSwap.scala -- Scala object ProgSwap
// Project OrcScala
//
// Created by jthywiss on Sep 30, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.progswap

import java.io.{ File, FileInputStream }
import java.io.PrintWriter
import scala.util.control.Breaks.{ break, breakable }
import orc.Handle
import orc.OrcCompilationOptions
import orc.compile.StandardOrcCompiler
import orc.compile.parse.OrcFileInputContext
import orc.error.compiletime.PrintWriterCompileLogger
import orc.progress.NullProgressMonitor
import orc.ast.oil.nameless.Expression
import orc.run.Orc
import orc.run.extensions.SwappableASTs
import orc.values.Signal
import orc.values.sites.{ Site, UntypedSite }
import orc.error.runtime.{ ArgumentTypeMismatchException, ArityMismatchException }
import orc.run.core.Execution
import orc.run.core.Token
import orc.run.core.SiteCallHandle

/** Update a running Orc program to the supplied OIL program. One argument is
  * expected, an OIL file name.
  *
  * @author jthywiss
  */
object ProgSwap extends Site with UntypedSite {

  override def call(args: List[AnyRef], callHandle: Handle) {
    def handleCracker(callHandle: Handle): Token = callHandle.asInstanceOf[SiteCallHandle].caller
    val execGroup: Execution = handleCracker(callHandle).getGroup().root
    var updateSuceeded = false
    args match {
      case List(filename: String) => updateSuceeded = update(execGroup, new File(filename))
      case List(a) => throw new ArgumentTypeMismatchException(0, "String", if (a != null) a.getClass().toString() else "null")
      case _ => throw new ArityMismatchException(1, args.size)
    }
    //    } catch {
    //      case e: CompilationException => throw new SiteException(e.getMessage(), e)
    //      case e: IOException => throw new SiteException(e.getMessage(), e)
    //    }
    if (updateSuceeded) {
      callHandle.publish(Signal)
    } else {
      callHandle.halt
    }
  }

  /** Update the OIL AST running in the execution group <code>execGroup</code>
    * to the AST found in OIL file <code>newOilFile</code>,
    * by moving currently executing tokens to the new AST. This will result
    * in the engine being suspended during the update. When the update
    * successfully completes, the engine's config will reflect the new source
    * file.
    *
    * @param execGroup Orc program execution group in which the update is to occur (not null)
    * @param oldOilAst The root of the OIL AST currently running in the engine (not null)
    * @param newOilFile File for the new OIL AST (not null)
    * @return true if the update succeeded, false for "unsafe now, try later"
    * @throws IOException If new OIL file cannot be read
    * @throws CompilationException If new OIL file fails to unmarshal or resolve
    * @throws NullPointerException If any param is null
    */
  def update(execGroup: Execution, newOilFile: File): Boolean = {
    val oldOilAst = execGroup.node
    Console.err.println(">>loading new program...")
    val newOilAst = loadNewProgram(newOilFile, execGroup)
    Console.err.println(">>matching ASTs...")
    val matching = FastMatchCRGMW96.matchAsts(oldOilAst, newOilAst)
    Console.err.println(">>matching generated " + matching.size + " node matches")
    Console.err.println(">>computing edit script...")
    val editList = EditScriptCRGMW96.computeEditScript(oldOilAst, newOilAst, matching)
    Console.err.println(">>editList: (" + editList.size + " operations):")
    Console.err.println(editList.mkString("- ", "\n- ", "\n>>End edit list"))
    if (editList != null && !editList.isEmpty) {
      Console.err.println(">>suspending execution group...")
      suspendEngine(execGroup)
      if (!isSafeState(execGroup, oldOilAst, newOilAst, editList)) {
        Console.err.println(">>not safe to update, resuming execution group...")
        resumeEngine(execGroup)
        return false
      }
      Console.err.println(">>migrating tokens to new...")
      migrateTokens(execGroup, oldOilAst, newOilAst, editList)
      Console.err.println(">>moving execution group to new AST...")
      changeAst(execGroup, oldOilAst, newOilAst, editList)
      Console.err.println(">>updating execution group configuration to reflect new program...")
      updateConfig(execGroup, newOilFile.toString)
      Console.err.println(">>resuming execution group...")
      resumeEngine(execGroup)
    } else {
      Console.err.println("No changes")
    }
    return true
  }

  /** @param newOilFile File for the new OIL AST (not null)
    * @param execGroup Orc program execution group in which the update is to occur (not null)
    * @return the new OIL AST
    * @throws IOException If new OIL file cannot be read
    * @throws CompilationException If new OIL file fails to unmarshal or resolve
    */
  protected def loadNewProgram(newOilFile: File, execGroup: Execution): Expression = {
    Console.err.println(">>Load new Orc file " + newOilFile)
    //orc.ast.oil.nameless.OrcXML.readOilFromStream(new FileInputStream(newOilFile))
    val compiler = new StandardOrcCompiler()
    val result = compiler(new OrcFileInputContext(newOilFile, "UTF-8"), execGroup.options.asInstanceOf[OrcCompilationOptions], new PrintWriterCompileLogger(new PrintWriter(Console.err, true)), NullProgressMonitor)
    if (result == null) {
      throw new NullPointerException("Compilation failed");
    }
    result
  }

  /** @param execGroup Orc program execution group to be suspended
    */
  protected def suspendEngine(execGroup: Execution) {
    execGroup.suspend()
    //FIXME: Need to wait for token schedule queue to be empty
    //FIXME: Need to handle blocked tokens (currently, we silently fail to suspend)
  }

  /** @param execGroup Orc program execution group in which the update is to occur (not null)
    * @param oldOilAst
    * @param newOilAst
    * @param editList
    * @return
    */
  protected def isSafeState(execGroup: Execution, oldOilAst: Expression, newOilAst: Expression, editList: AstEditScript): Boolean = {
    execGroup.inhabitants forall { tok => editList forall { editOp => editOp.isTokenSafe(tok) } }
  }

  /** @param execGroup Orc program execution group in which the update is to occur (not null)
    * @param oldOilAst
    * @param newOilAst
    * @param editList
    */
  protected def migrateTokens(execGroup: Execution, oldOilAst: Expression, newOilAst: Expression, editList: AstEditScript) {
    for (token <- execGroup.inhabitants)
      breakable {
        for (editOp <- editList) {
          if (editOp.migrateToken(token)) {
            editOp.migrateClosures(token, editList);
            editOp.migrateFrameStack(token, editList)
            break
          }
        }
        // ALL tokens in the old tree MUST be covered by some edit operation.
        // Assuming tokens not migrated belong to other ASTs.
        Console.err.println("AstEditScript did not migrate Token " + token + ", which is at node " + token.getNode());
      }
  }

  /** @param execGroup Orc program execution group in which the update is to occur (not null)
    * @param oldOilAst
    * @param newOilAst
    * @param editList
    */
  protected def changeAst(execGroup: Execution, oldOilAst: Expression, newOilAst: Expression, editList: AstEditScript) {
    SwappableASTs.setExecutionNode(execGroup, newOilAst)
  }

  /** @param execGroup Orc program execution group to update (not null)
    * @param newOilFileName
    */
  protected def updateConfig(execGroup: Execution, newOilFileName: String) {
    execGroup.options.filename = newOilFileName
  }

  /** @param execGroup Orc program execution group to be resumed
    */
  protected def resumeEngine(execGroup: Execution) {
    execGroup.resume()
  }

}
