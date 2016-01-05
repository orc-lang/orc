//
// CmdLineParser.scala -- Scala trait CmdLineParser
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 19, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.util

import java.io.File
import java.util.NoSuchElementException

/** Parses command line arguments per POSIX and GNU command line syntax guidelines.
  * Mix this trait in and add XxxOprd and XxxOpt statements to your class holding
  * the resulting parsed values.  Call parseCmdLine after all the XxxOprd and XxxOpt
  * statements.
  *
  * This CmdLineParser understands POSIX single-letter option names (-a -b -c) and GNU
  * long option names (--option-name=value).  It also handles the various option argument
  * syntax: -avalue -a value --long-name=value and -long-name value, it tries to
  * "do the right thing" in the ambiguous situation -abc (could mean -a -b -c or
  * -a bc or --abc), and it understands the -- option list terminator.
  * See POSIX XBD §12.1 and §12.2 and GNU libc §25.1.1.
  *
  * @author jthywiss
  */
trait CmdLineParser {

  var recognizedOprds = scala.collection.mutable.Map.empty[Int, CmdLineOprd]
  var recognizedOpts = scala.collection.mutable.Set.empty[CmdLineOpt]
  var recognizedShortOpts = scala.collection.mutable.Map.empty[Char, CmdLineOpt]
  var recognizedLongOpts = scala.collection.mutable.Map.empty[String, CmdLineOpt]

  ////////
  // Constructor
  ////////

  UnitOpt(() => false, () => printHelp, '?', "help", usage = "Give this help list")
  protected def printHelp {
    def shortOptHelp(opt: CmdLineOpt) =
      if (opt.shortName != 0 && opt.shortName != ' ')
        "-" + opt.shortName + (if (opt.argName != null && opt.argName.length > 0) (" " + opt.argName) else "")
      else
        ""
    def longOptHelp(opt: CmdLineOpt) =
      if (opt.longName != null && opt.longName.length > 0)
        "--" + opt.longName + (if (opt.argName != null && opt.argName.length > 0) ("=" + opt.argName) else "")
      else
        ""
    val helpString = usageString +
      (if (recognizedOpts.size == 0) ""
      else "\nOptions:\n" +
        (recognizedOpts.toList.sortBy(o => (o.longName, o.shortName)).map({ opt: CmdLineOpt =>
          //TODO: Aligned columns?
          "  " + shortOptHelp(opt) +
            "  " + longOptHelp(opt) +
            "  " + opt.usage
        })).mkString("\n"))
    throw new PrintVersionAndMessageException(helpString)
  }

  UnitOpt(() => false, () => printUsage, ' ', "usage", usage = "Give a short usage message")
  protected def printUsage { throw new PrintVersionAndMessageException(usageString) }

  UnitOpt(() => false, () => printVersion, 'V', "version", usage = "Print program version")
  protected def printVersion { throw new PrintVersionAndMessageException("") }

  def usageString =
    "usage: " +
      (if (recognizedOpts.size > 0) "[options...]" else "") +
      (for { i <- 0 until recognizedOprds.size } yield " " + recognizedOprds(i).argName).mkString(" ")
  //"Try the --help option for more information." //TODO: In simple cases, show the usage here

  ////////
  // Command line operand and option definitions
  ////////

  abstract class CmdLineOprdOpt(val argName: String, val usage: String, val required: Boolean, val hidden: Boolean) extends Serializable {
    def getValue: String
    def setValue(s: String): Unit
  }

  abstract class CmdLineOprd(val position: Int, override val argName: String, override val usage: String, override val required: Boolean, override val hidden: Boolean) extends CmdLineOprdOpt(argName, usage, required, hidden) {
    if (recognizedOprds.contains(position)) throw new MultiplyDefinedCmdLineOprndError(position)
    recognizedOprds += ((position, this))
  }

  abstract class CmdLineOpt(val shortName: Char, val longName: String, override val argName: String, override val usage: String, override val required: Boolean, override val hidden: Boolean) extends CmdLineOprdOpt(argName, usage, required, hidden) {
    if (shortName != ' ') {
      if (recognizedShortOpts.contains(shortName)) throw new MultiplyDefinedCmdLineOptError(shortName.toString)
      recognizedShortOpts += ((shortName, this))
    }
    if (longName != null) {
      if (recognizedLongOpts.contains(longName)) throw new MultiplyDefinedCmdLineOptError(longName)
      recognizedLongOpts += ((longName, this))
    }
    recognizedOpts += this
  }

  case class BooleanOprd(val getter: Function0[Boolean], val setter: (Boolean => Unit), override val position: Int, override val argName: String = "BOOL", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def getValue: String = { getter().toString }
    def setValue(value: String) { setter(value.toBoolean) }
  }

  case class DoubleOprd(val getter: Function0[Double], val setter: (Double => Unit), override val position: Int, override val argName: String = "DOUBLE", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def getValue: String = { getter().toString }
    def setValue(value: String) { setter(value.toDouble) }
  }

  case class IntOprd(val getter: Function0[Int], val setter: (Int => Unit), override val position: Int, override val argName: String = "INT", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def getValue: String = { getter().toString }
    def setValue(value: String) { setter(value.toInt) }
  }

  case class CharOprd(val getter: Function0[Char], val setter: (Char => Unit), override val position: Int, override val argName: String = "CHAR", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def getValue: String = { getter().toString }
    def setValue(value: String) { setter(value(0)) }
  }

  case class StringOprd(val getter: Function0[String], val setter: (String => Unit), override val position: Int, override val argName: String = "STRING", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def getValue: String = { getter() }
    def setValue(value: String) { setter(value) }
  }

  case class StringListOprd(val getter: Function0[Seq[String]], val setter: (Seq[String] => Unit), override val position: Int, override val argName: String = "STRING", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def getValue: String = { getter().mkString(File.pathSeparator) }
    def setValue(value: String) { setter(value.split(File.pathSeparator)) }
  }

  case class FileOprd(val getter: Function0[File], val setter: (File => Unit), override val position: Int, override val argName: String = "FILE", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def getValue: String = {
      getter() match {
        case null => ""
        case f => f.toString
      }
    }
    def setValue(value: String) { if (value != null && !value.isEmpty) setter(new File(value)) }
  }

  case class PathListOprd(val getter: Function0[Seq[File]], val setter: (Seq[File] => Unit), override val position: Int, override val argName: String = "PATH", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def getValue: String = { getter().map(_.toString).mkString(File.pathSeparator) }
    def setValue(value: String) { setter(value.split(File.pathSeparator).map(new File(_))) }
  }

  case class UnitOpt(val getter: Function0[Boolean], val setter: (() => Unit), override val shortName: Char, override val longName: String, override val argName: String = "", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def getValue: String = ""
    def setValue(value: String) { setter() }
  }

  case class BooleanOpt(val getter: Function0[Boolean], val setter: (Boolean => Unit), override val shortName: Char, override val longName: String, override val argName: String = "BOOL", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def getValue: String = { getter().toString }
    def setValue(value: String) { setter(value.toBoolean) }
  }

  case class DoubleOpt(val getter: Function0[Double], val setter: (Double => Unit), override val shortName: Char, override val longName: String, override val argName: String = "DOUBLE", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def getValue: String = { getter().toString }
    def setValue(value: String) { setter(value.toDouble) }
  }

  case class IntOpt(val getter: Function0[Int], val setter: (Int => Unit), override val shortName: Char, override val longName: String, override val argName: String = "INT", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def getValue: String = { getter().toString }
    def setValue(value: String) { setter(value.toInt) }
  }

  case class CharOpt(val getter: Function0[Char], val setter: (Char => Unit), override val shortName: Char, override val longName: String, override val argName: String = "CHAR", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def getValue: String = { getter().toString }
    def setValue(value: String) { setter(value(0)) }
  }

  case class StringOpt(val getter: Function0[String], val setter: (String => Unit), override val shortName: Char, override val longName: String, override val argName: String = "STRING", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def getValue: String = { getter() }
    def setValue(value: String) { setter(value) }
  }

  case class StringListOpt(val getter: Function0[Seq[String]], val setter: (Seq[String] => Unit), override val shortName: Char, override val longName: String, override val argName: String = "STRING", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false, val separator: String = File.pathSeparator)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def getValue: String = { getter().mkString(separator) }
    def setValue(value: String) { setter(value.split(separator)) }
  }

  case class FileOpt(val getter: Function0[File], val setter: (File => Unit), override val shortName: Char, override val longName: String, override val argName: String = "FILE", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def getValue: String = {
      getter() match {
        case null => ""
        case f => f.toString
      }
    }
    def setValue(value: String) { if (value != null && !value.isEmpty) setter(new File(value)) }
  }

  case class PathListOpt(val getter: Function0[List[File]], val setter: (Seq[File] => Unit), override val shortName: Char, override val longName: String, override val argName: String = "PATH", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def getValue: String = { getter().map(_.toString).mkString(File.pathSeparator) }
    def setValue(value: String) { setter(value.split(File.pathSeparator).map(new File(_))) }
  }

  ////////
  // Parse method
  ////////

  @throws(classOf[PrintVersionAndMessageException])
  @throws(classOf[CmdLineUsageException])
  def parseCmdLine(args: Seq[String]) {
    // Parse command line arguments per POSIX XBD §12.1 and §12.2 and GNU libc §25.1.1
    // Note: We don't attempt the GNU libc "abbreviate options to unique prefix" nonsense
    var maxReqOprdIndex = -1
    for ((_, oprd) <- recognizedOprds) {
      val position = oprd.position
      if (position > 0 && !recognizedOprds.contains(position - 1)) throw new MissingCmdLineOprdError(position)
      if (position > 0 && oprd.required && !recognizedOprds(position - 1).required) throw new InvalidRequiredCmdLineOprdError(position)
      if (oprd.required) maxReqOprdIndex = maxReqOprdIndex max position
    }

    def before(str: String, sep: String) = { val i = str.indexOf(sep); if (i >= 0) str.take(i) else str }
    def after(str: String, sep: String) = { val i = str.indexOf(sep); if (i >= 0) str.drop(i + 1) else null }
    var currOprdIndex = 0
    var endOptions = false
    var gobbleNext: CmdLineOpt = null
    for (arg <- args) {
      if (gobbleNext != null) { gobbleNext.setValue(arg); gobbleNext = null }
      else if (endOptions) setCurrOprd(arg)
      else if (arg.equals("--")) endOptions = true
      else if (arg.startsWith("--")) setLongOpt(arg.drop(2)) //GNU-style long name (lower case, hyphenated) option
      else if (arg.startsWith("-") && arg.length == 2) setShortOpt(arg(1), null) //POSIX-style short name (one alphanum char) option
      else if (!arg.startsWith("-")) setCurrOprd(arg)
      else { // Ambiguous: "-abc" could be POSIX "-a -b -c" or "-a bc" or GNU "--abc"
        if (recognizedShortOpts.contains(arg(1))) {
          if (recognizedShortOpts(arg(1)).isInstanceOf[UnitOpt]) {
            // "-abc" treated as "-a -b -c"
            arg.drop(1).map(setShortOpt(_, null))
          } else {
            // "-abc" treated as "-a bc"
            setShortOpt(arg(1), arg.drop(2))
          }
        } else if (recognizedLongOpts.contains(before(arg, "=").drop(1))) {
          // "-abc" treated as "--abc"
          setLongOpt(arg.drop(1))
        } else throw new UnrecognizedCmdLineOptException(arg.drop(1), this)
      }
    }
    if (currOprdIndex <= maxReqOprdIndex) throw new MissingCmdLineOprdsException(recognizedOprds(currOprdIndex).argName, this)
    //TODO: Check all req'd opts are present

    def setCurrOprd(arg: String) = {
      try {
        recognizedOprds(currOprdIndex).setValue(arg)
        currOprdIndex += 1
      } catch {
        case _: NoSuchElementException => throw new ExtraneousCmdLineOprdsException(arg, this)
      }
    }
    def setLongOpt(arg: String) = {
      try {
        val currOpt = recognizedLongOpts(before(arg, "="))
        if (!currOpt.isInstanceOf[UnitOpt] && !arg.contains("=")) gobbleNext = currOpt
        else if (currOpt.isInstanceOf[UnitOpt] && arg.contains("=")) throw new ExtraneousCmdLineOptArgException(currOpt.longName, after(arg, "="), this)
        else currOpt.setValue(after(arg, "="))
      } catch {
        case _: NoSuchElementException => throw new UnrecognizedCmdLineOptException(before(arg, "="), this)
      }
    }
    def setShortOpt(optName: Char, value: String) = {
      try {
        val currOpt = recognizedShortOpts(optName)
        if (!currOpt.isInstanceOf[UnitOpt] && value == null) gobbleNext = currOpt
        else if (currOpt.isInstanceOf[UnitOpt] && value != null) throw new ExtraneousCmdLineOptArgException(optName.toString, value, this)
        else currOpt.setValue(value)
      } catch {
        case _: NoSuchElementException => throw new UnrecognizedCmdLineOptException(optName.toString, this)
      }
    }
  }

  ////////
  // Parse method
  ////////

  def composeCmdLine(): Array[String] = {
    ((recognizedOpts.toList.sortBy(o => (o.longName, o.shortName)).flatMap({ opt: CmdLineOpt =>
      if (!opt.isInstanceOf[UnitOpt]) {
        if (opt.shortName != 0 && opt.shortName != ' ') List("-" + opt.shortName, opt.getValue) else List("--" + opt.longName + "=" + opt.getValue)
      } else {
        if (opt.asInstanceOf[UnitOpt].getter()) {
          if (opt.shortName != 0 && opt.shortName != ' ') List("-" + opt.shortName) else List("--" + opt.longName)
        } else {
          Nil
        }
      }
    })) ++
      (for { i <- 0 until recognizedOprds.size } yield recognizedOprds(i).getValue).toList).toArray
  }

}

////////
// Error for mal-formed operand and option declarations
////////

class MissingCmdLineOprdError(operandIndex: Int) extends Error("Command line operand number " + operandIndex + " not defined, but operand " + (operandIndex + 1) + " is")
class InvalidRequiredCmdLineOprdError(operandIndex: Int) extends Error("Command line operand number " + operandIndex + " marked required, but operand " + (operandIndex - 1) + " is not")
class MultiplyDefinedCmdLineOprndError(operandIndex: Int) extends Error("Command line operand number " + operandIndex + " multiply defined")
class MultiplyDefinedCmdLineOptError(optName: String) extends Error("Command line option \"" + optName + "\" multiply defined")

////////
// Exceptions for command args that didn't parse
////////

abstract class CmdLineUsageException(msg: String, p: CmdLineParser) extends IllegalArgumentException(msg + "\n" + p.usageString + "\nTry the --help option for more information.")
class ExtraneousCmdLineOprdsException(val operand: String, p: CmdLineParser) extends CmdLineUsageException("extra operand -- '" + operand + "'", p)
class MissingCmdLineOprdsException(val argName: String, p: CmdLineParser) extends CmdLineUsageException("missing " + argName + " operand", p)
class UnrecognizedCmdLineOptException(val optName: String, p: CmdLineParser) extends CmdLineUsageException("invalid option -- " + optName, p)
class MissingCmdLineOptException(val optName: String, p: CmdLineParser) extends CmdLineUsageException("missing option -- " + optName, p)
class ExtraneousCmdLineOptArgException(val optName: String, optArg: String, p: CmdLineParser) extends CmdLineUsageException("option " + optName + " doesn't allow an argument", p)
class MissingCmdLineOptArgException(val optName: String, p: CmdLineParser) extends CmdLineUsageException("option requires an argument -- " + optName, p)

////////
// Exception for command args that requests immediate termination of program with usage/help/version info.
////////

class PrintVersionAndMessageException(msg: String) extends Exception(msg)
