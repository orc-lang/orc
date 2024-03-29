//
// MainExit.scala -- Scala trait MainExit and object ExitStatus
// Project OrcScala
//
// Created by jthywiss on Nov 7, 2017.
//
// Copyright (c) 2021 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.{ Level, LogRecord }

import scala.util.control.ControlThrowable

/** Handle exit from main method.  Provides failure diagnostic messages, sets
  * exit status values, and handles uncaught exceptions.
  *
  * @author jthywiss
  */
trait MainExit extends Thread.UncaughtExceptionHandler {
  /* POSIX exit status and message guidelines: If a program completed
   * successfully, its exit status must be 0, and no messages should be sent to
   * standard error.  If an error occurred, both the exit status must be
   * non-zero, and a diagnostic message must be sent to standard error. If a
   * program processes multiple files, or walks a directory tree, if the program
   * encounters an error on a file, it should send a diagnostic message to
   * standard error, and keep on trying the other files. When the program
   * finishes, it must exit with a non-zero exit status.
   */

  val shutdownInProgress = new AtomicBoolean(false)

  /** Perform program failure exit processing; namely, write diagnostic message
    * and exit JVM with given status. Only one call to failureExit will
    * succeed; others will hang pending JVM termination.
    */
  def failureExit(message: String, exitStatus: Int): Nothing = failureExit(null, message, exitStatus)

  /** Perform program failure exit processing; namely, write diagnostic message
    * and exit JVM with given status. Only one call to failureExit will
    * succeed; others will hang pending JVM termination.
    */
  def failureExit(pathName: String, message: String, exitStatus: Int): Nothing = {
    /* Only permit one failureExit call to succeed, and cause others to hang. */
    getClass synchronized {
      /* 
       * Unfortunately, the JRE does not expose its "shutdown in progress"
       * status, so we keep our own.  This is approximate -- if a shutdown is
       * initiated somewhere else, and failureExit is invoked afterwards, e.g.
       * from another thread, a shutdown hook, or a finalizer, we'll
       * writeHaltMessage for the second shutdown reason (possibly misleading)
       * and call System.exit a second time, which will hang (as intended).
       */
      shutdownInProgress.getAndSet(true)
      writeHaltMessage(pathName, message)
      /* Cast to let type checker know System.exit never returns. */
      System.exit(exitStatus).asInstanceOf[Nothing]
    }
  }

  /** Write a HALT diagnostic message to stderr.
    *
    * @param pathName path to the input file with the problem
    * @param message description of the failure
    */
  def writeHaltMessage(pathName: String, message: String): Unit =
    writeDiagnosticMessage(pathName, "HALT", message)

  /** Write an ERROR diagnostic message to stderr.
    *
    * @param pathName path to the input file with the problem
    * @param message description of the failure
    */
  def writeErrorMessage(pathName: String, message: String): Unit =
    writeDiagnosticMessage(pathName, "ERROR", message)

  /** Write a WARNING diagnostic message to stderr.
    *
    * @param pathName path to the input file with the problem
    * @param message description of the failure
    */
  def writeWarningMessage(pathName: String, message: String): Unit =
    writeDiagnosticMessage(pathName, "WARNING", message)

  /** Write a INFO diagnostic message to stderr.
    *
    * @param pathName path to the input file with the problem
    * @param message description of the failure
    */
  def writeInfoMessage(pathName: String, message: String): Unit =
    writeDiagnosticMessage(pathName, "INFO", message)

  protected def writeDiagnosticMessage(pathName: String, severity: String, message: String): Unit =
    MainExit.writeDiagnosticMessage(MainExit.progName, pathName, severity, message)

  /** Call to set this JVM's defaultUncaughtExceptionHandler to halt the JVM
    * when an exception is thrown, but not caught.
    */
  def haltOnUncaughtException() {
    Thread.setDefaultUncaughtExceptionHandler(this)
  }

  /** Invoked when the given thread terminates due to the given uncaught
    * exception.
    *
    * Any exception thrown by this method will be ignored by the JVM.
    */
  override def uncaughtException(t: Thread, e: Throwable): Unit = {
    if (!propagateThrowableQuietly(e)) {
      MainExit.printAndLogException(t, e, Level.SEVERE)
      if (mainUncaughtExceptionHandler.isDefinedAt(e)) {
        mainUncaughtExceptionHandler(e)
      } else {
        failureExit(e.toString, ExitStatus.Software)
      }
    }
  }

  /** Compose your handler with basicUncaughtExceptionHandler, and store
    * in mainUncaughtExceptionHandler.  e.g.:
    *
    *   private val ourUEH: PartialFunction[Throwable, Unit] = {
    *     case e: Blah => failureExit(e.getMessage, ExitStatus.Blah)
    *   }
    *   val mainUncaughtExceptionHandler = ourUEH orElse basicUncaughtExceptionHandler
    */
  val mainUncaughtExceptionHandler: PartialFunction[Throwable, Unit]

  /** Should the given Throwable be allowed to continue up the handler stack,
    * without any logging/diagnostic messages?
    */
  def propagateThrowableQuietly(t: Throwable): Boolean = t match {
    case _: ThreadDeath | _: InterruptedException | _: ControlThrowable => true
    case _ => false
  }

  /** Handles some well-known Exceptions, and falls back to "internal software
    * error" (exit status 70).
    */
  val basicUncaughtExceptionHandler: PartialFunction[Throwable, Unit] = {
    case e: CmdLineUsageException => failureExit(e.getMessage, ExitStatus.Usage)
    case e: java.net.UnknownHostException => failureExit(e.toString, ExitStatus.NoHost)
    case e: java.net.ConnectException => failureExit(e.toString, ExitStatus.Unavailable)
    case e: VirtualMachineError => failureExit(e.toString, ExitStatus.OsErr)
    case e: java.net.ProtocolException => failureExit(e.toString, ExitStatus.Protocol)
    case e: SecurityException => failureExit(e.toString, ExitStatus.NoPerm)
    case e: java.io.IOException => failureExit(e.toString, ExitStatus.IoErr)
    case e: Exception if !propagateThrowableQuietly(e) => failureExit(e.toString, ExitStatus.Software)
    case e: Error if !propagateThrowableQuietly(e) => failureExit(e.toString, ExitStatus.Software)
  }

}

object MainExit {
  /** A guess at this program's main class name */
  val mainClassName: String = {
    /* Based on Sun JVM monitoring tools' heuristic */
    val commandProperty = System.getProperty("sun.java.command")
    if (commandProperty != null && !commandProperty.isEmpty()) {
      val firstSpace = if (commandProperty.contains(" ")) commandProperty.indexOf(" ") else commandProperty.length
      val lastFileSep = commandProperty.lastIndexOf(java.io.File.separator, firstSpace - 1)
      commandProperty.substring(lastFileSep + 1, firstSpace)
    } else {
      null
    }
  }

  /** This program's name, for stderr messages */
  val progName: String = {
    if (mainClassName != null && !mainClassName.isEmpty()) {
      mainClassName.substring(mainClassName.lastIndexOf(".") + 1)
    } else {
      null
    }
  }

  /** Write a diagnostic message to stderr.
    *
    * Diagnostic message recommended format from POSIX:
    * progname: pathname: SEVERITY: Message
    * Where progname is the basename of the program; pathname (if applicable)
    * is the path to the input file with the problem; SEVERITY is HALT, ERROR,
    * WARNING, or INFO; and Message is a description of the failure.
    */
  def writeDiagnosticMessage(progName: String, pathName: String, severity: String, message: String): Unit = {
    /* Trying to avoid allocation here, since the JVM may be in a broken state */
    try {
      System.out.flush()

      if (progName != null && !progName.isEmpty()) {
        System.err.print(progName)
        System.err.print(": ")
      }
      if (pathName != null && !pathName.isEmpty()) {
        System.err.print(pathName)
        System.err.print(": ")
      }
      System.err.print(severity)
      System.err.print(": ")
      System.err.println(message)

      System.err.flush()
    } catch {
      case _: Throwable => /* Ignore and continue halting */
    }
  }

  /** Print an exception to stderr, and log to the main class package's logger
    * at the given severity level.
    */
  def printAndLogException(t: Thread, e: Throwable, level: Level): Unit = {
    System.err.println("Exception " + e.toString + " in thread \"" + t.getName() + "\"")
    val mainExitLogger =
      if (mainClassName != null && !mainClassName.isEmpty()) {
        java.util.logging.Logger.getLogger(mainClassName.substring(0, mainClassName.lastIndexOf(".")))
      } else {
        java.util.logging.Logger.getGlobal
      }
    if (mainExitLogger.isLoggable(level)) {
      /* Log using the top stack trace frame as the "source" of the log record */
      val logRecord = new LogRecord(level, "Exception in thread \"" + t.getName + "\" #" + t.getId)
      if (e != null && e.getStackTrace != null && e.getStackTrace().length > 0) {
        logRecord.setSourceClassName(e.getStackTrace()(0).getClassName)
        logRecord.setSourceMethodName(e.getStackTrace()(0).getMethodName)
      } else {
        logRecord.setSourceClassName(null)
        logRecord.setSourceMethodName(null)
      }
      logRecord.setThreadID(t.getId.asInstanceOf[Int])
      logRecord.setThrown(e)
      logRecord.setLoggerName(mainExitLogger.getName)
      mainExitLogger.log(logRecord)
    }
  }

}

/** Program exit statuses.  0 indicates success. 1 to 63 are application-
  * specific failure reasons, although 1 is also used by the shell.  63 to 127
  * are standard, given below.
  *
  * NOTE: Only the lower 8 bits of the exit status value are used, even though
  * exit statuses are often declared as int.  Furthermore, exit statuses 128 to
  * 255 are reserved for termination by signal.
  *
  * From the BSD sysexits.h header file and sysexits(3) man page.
  */
object ExitStatus {
  /** Exit status 0: Program terminated successfully */
  val Success = 0
  val Ok = 0

  /* Exit status 1: Generic program terminated unsuccessfully code. Also used
   * by many shells when a command fails during word expansion or redirection. */

  /* Exit status 2 to 63: Other failure, implementation/program defined */

  /** Exit status 64: Command line usage error */
  val Usage = 64

  /** Exit status 65: Data format error: "The input data was incorrect in some
    * way. This should only be used for user's data and not system files."
    */
  val DataErr = 65

  /** Exit status 66: Cannot open input: "An input file (not a system file) did
    * not exist or was not readable."
    */
  val NoInput = 66

  /** Exit status 67: "The user specified did not exist. This might be used for
    * mail addresses or remote logins."
    */
  val NoUser = 67

  /** Exit status 68: Host name unknown */
  val NoHost = 68

  /** Exit status 69: Service unavailable: "This can occur if a support program
    * or file does not exist."
    */
  val Unavailable = 69

  /** Exit status 70: "An internal software error has been detected. This
    * should be limited to non-operating system related errors as possible."
    */
  val Software = 70

  /** Exit status 71: System error (e.g., can't fork) */
  val OsErr = 71

  /** Exit status 72: "Some system file does not exist, cannot be opened, or
    * has some sort of error."
    */
  val OsFile = 72

  /** Exit status 73: "A (user specified) output file cannot be created." */
  val CantCreate = 73

  /** Exit status 74: Input/output error */
  val IoErr = 74

  /** Exit status 75: Temporary failure; user is invited to retry */
  val TempFail = 75

  /** Exit status 76: "The remote system returned something that was 'not
    * possible' during a protocol exchange."
    */
  val Protocol = 76

  /** Exit status 77: Permission denied: "This is not intended for file
    * system problems, which should use ExitStatus.NoInput or
    * ExitStatus.CantCreate, but rather for higher level permissions."
    */
  val NoPerm = 77

  /** Exit status 78: Configuration error: "Something was found in an
    * un-configured or mis-configured state."
    */
  val Config = 78

  /* Exit status 126: Command not executable */

  /* Exit status 127: Command not found */

  /* Exit status 128 to 255: Terminated by signal; Exit status value is
   * signo+128 */

}
