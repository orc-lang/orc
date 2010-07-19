//
// CmdLineParser.scala -- Scala trait CmdLineParser
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 19, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc

import java.io.File
import java.util.NoSuchElementException

/**
 * Parses command line arguments per POSIX and GNU command line syntax guidelines.
 * Mix this trait in and add XxxOprd and XxxOpt statements to your class holding
 * the resulting parsed values.  Call parseCmdLine after all the XxxOprd and XxxOpt
 * statements.
 * 
 * This trait understands POSIX single-letter option names (-a -b -c) and GNU long
 * option names (--option-name=value).  It also handles the various option argument
 * syntax: -avalue -a value --long-name=value and -long-name value, it tries to
 * "do the right thing" in the ambiguous situation -abc (could mean -a -b -c or
 * -a bc or --abc), and it understands the -- option list terminator.
 * See POSIX XBD §12.1 and §12.2 and GNU libc §25.1.1.
 *
 * @author jthywiss
 */
trait CmdLineParser {
  abstract class CmdLineOprdOpt(val argName: String, val usage: String, val required: Boolean, val hidden: Boolean) {
    def setValue(s: String): Unit
  }
  abstract class CmdLineOprd(val position: Int, override val argName: String, override val usage: String, override val required: Boolean, override val hidden: Boolean) extends CmdLineOprdOpt(argName, usage, required, hidden) {
    if (recognizedOprds.contains(position)) throw new MultiplyDefinedCmdLineOprndException(position)
    recognizedOprds += ((position, this))
  }
  abstract class CmdLineOpt(val shortName: Char, val longName: String, override val argName: String, override val usage: String, override val required: Boolean, override val hidden: Boolean) extends CmdLineOprdOpt(argName, usage, required, hidden) {
    if (shortName != ' ') {
      if (recognizedShortOpts.contains(shortName)) throw new MultiplyDefinedCmdLineOptException(shortName.toString)
      recognizedShortOpts += ((shortName, this))
    }
    if (longName != null) {
      if (recognizedLongOpts.contains(longName)) throw new MultiplyDefinedCmdLineOptException(longName)
      recognizedLongOpts += ((longName, this))
    }
    recognizedOpts += this
  }
  var recognizedOprds = scala.collection.mutable.Map.empty[Int, CmdLineOprd]
  var recognizedOpts = scala.collection.mutable.Set.empty[CmdLineOpt]
  var recognizedShortOpts = scala.collection.mutable.Map.empty[Char, CmdLineOpt]
  var recognizedLongOpts = scala.collection.mutable.Map.empty[String, CmdLineOpt]
//TODO:Add the following
// 102   {"help",        '?',          0, 0,  N_("Give this help list"), -1},
// 103   {"usage",       OPT_USAGE,    0, 0,  N_("Give a short usage message")},
// 167   {"version",     'V',          0, 0,  N_("Print program version"), -1},

  case class BooleanOprd(val setter: (Boolean => Unit), override val position: Int, override val argName: String = "BOOL", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value.toBoolean) }
  }
  case class DoubleOprd(val setter: (Double => Unit), override val position: Int, override val argName: String = "DOUBLE", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value.toDouble) }
  }
  case class IntOprd(val setter: (Int => Unit), override val position: Int, override val argName: String = "INT", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value.toInt) }
  }
  case class CharOprd(val setter: (Char => Unit), override val position: Int, override val argName: String = "CHAR", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value(0)) }
  }
  case class StringOprd(val setter: (String => Unit), override val position: Int, override val argName: String = "STRING", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value) }
  }
  case class StringListOprd(val setter: (Seq[String] => Unit), override val position: Int, override val argName: String = "STRING", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value.split(System.getProperty("path.separator"))) }
  }
  case class FileOprd(val setter: (File => Unit), override val position: Int, override val argName: String = "FILE", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def setValue(value: String) { setter(new File(value)) }
  }
  case class PathListOprd(val setter: (Seq[File] => Unit), override val position: Int, override val argName: String = "PATH", override val usage: String = "", override val required: Boolean = true, override val hidden: Boolean = false)
    extends CmdLineOprd(position, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value.split(System.getProperty("path.separator")).map(new File(_))) }
  }
  case class UnitOpt(val setter: (() => Unit), override val shortName: Char, override val longName: String, override val argName: String = "", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def setValue(value: String) { setter() }
  }
  case class BooleanOpt(val setter: (Boolean => Unit), override val shortName: Char, override val longName: String, override val argName: String = "BOOL", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value.toBoolean) }
  }
  case class DoubleOpt(val setter: (Double => Unit), override val shortName: Char, override val longName: String, override val argName: String = "DOUBLE", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value.toDouble) }
  }
  case class IntOpt(val setter: (Int => Unit), override val shortName: Char, override val longName: String, override val argName: String = "INT", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value.toInt) }
  }
  case class CharOpt(val setter: (Char => Unit), override val shortName: Char, override val longName: String, override val argName: String = "CHAR", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value(0)) }
  }
  case class StringOpt(val setter: (String => Unit), override val shortName: Char, override val longName: String, override val argName: String = "STRING", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value) }
  }
  case class StringListOpt(val setter: (Seq[String] => Unit), override val shortName: Char, override val longName: String, override val argName: String = "STRING", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value.split(System.getProperty("path.separator"))) }
  }
  case class FileOpt(val setter: (File => Unit), override val shortName: Char, override val longName: String, override val argName: String = "FILE", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def setValue(value: String) { setter(new File(value)) }
  }
  case class PathListOpt(val setter: (Seq[File] => Unit), override val shortName: Char, override val longName: String, override val argName: String = "PATH", override val usage: String = "", override val required: Boolean = false, override val hidden: Boolean = false)
    extends CmdLineOpt(shortName, longName, argName, usage, required, hidden) {
    def setValue(value: String) { setter(value.split(System.getProperty("path.separator")).map(new File(_))) }
  }

  
  class MissingCmdLineOprdException(operandIndex: Int) extends Exception("Command line operand number "+operandIndex+" not defined, but operand "+(operandIndex+1)+" is")
  class InvalidRequiredCmdLineOprdException(operandIndex: Int) extends Exception("Command line operand number "+operandIndex+" marked required, but operand "+(operandIndex-1)+" is not")
  class MultiplyDefinedCmdLineOprndException(operandIndex: Int) extends Exception("Command line operand number "+operandIndex+" multiply defined")
  class MultiplyDefinedCmdLineOptException(optName: String) extends Exception("Command line option \""+optName+"\" multiply defined")
  
  abstract class CmdLineUsageException(msg: String) extends IllegalArgumentException(msg)
  class ExtraneousCmdLineOprdsException(val operand: String) extends CmdLineUsageException("extra operand -- '"+operand+"'")
  class MissingCmdLineOprdsException(val argName: String) extends CmdLineUsageException("missing "+argName+" operand")
  class UnrecognizedCmdLineOptException(val optName: String) extends CmdLineUsageException("invalid option -- "+optName)
  class MissingCmdLineOptException(val optName: String) extends CmdLineUsageException("missing option -- "+optName)
  class ExtraneousCmdLineOptArgException(val optName: String, optArg: String) extends CmdLineUsageException("option "+optName+" doesn't allow an argument")
  class MissingCmdLineOptArgException(val optName: String) extends CmdLineUsageException("option requires an argument -- "+optName)
  
  def parseCmdLine(args: Seq[String]) {
    // Parse command line arguments per POSIX XBD §12.1 and §12.2 and GNU libc §25.1.1
    // Note: We don't attempt the GNU libc "abbreviate options to unique prefix" nonsense
    var maxReqOprdIndex = -1
    for ((_, oprd) <- recognizedOprds) {
      val position = oprd.position      
      if (position > 0 && !recognizedOprds.contains(position-1)) throw new MissingCmdLineOprdException(position)
      if (position > 0 && oprd.required && !recognizedOprds(position-1).required) throw new InvalidRequiredCmdLineOprdException(position)
      if (oprd.required) maxReqOprdIndex = maxReqOprdIndex max position
    }

    def before(str: String, sep: String) = { val i = str.indexOf(sep) ; if (i >= 0) str.take(i) else str }
    def after(str: String, sep: String) = { val i = str.indexOf(sep) ; if (i >= 0) str.drop(i+1) else null }
    var currOprdIndex = 0
    var endOptions = false
    var gobbleNext: CmdLineOpt = null
    for (arg <- args) {
           if (gobbleNext != null)                      { gobbleNext.setValue(arg); gobbleNext = null }
      else if (endOptions)                              setCurrOprd(arg)
      else if (arg.equals("--"))                        endOptions = true
      else if (arg.startsWith("--"))                    setLongOpt(arg.drop(2)) //GNU-style long name (lower case, hyphenated) option
      else if (arg.startsWith("-") && arg.length == 2)  setShortOpt(arg(1), null) //POSIX-style short name (one alphanum char) option
      else if (!arg.startsWith("-"))                    setCurrOprd(arg)
      else { // Ambiguous: "-abc" could be POSIX "-a -b -c" or "-a bc" or GNU "--abc"
        if (recognizedShortOpts.contains(arg(1))) {
          if (recognizedShortOpts(arg(1)).isInstanceOf[UnitOpt]) {
            // "-abc" treated as "-a -b -c"
            arg.drop(1).map(setShortOpt(_, null))
          } else {
            // "-abc" treated as "-a bc"
            setShortOpt(arg(1), arg.drop(2))
          }
        }
        else if (recognizedLongOpts.contains(before(arg, "=").drop(1))) {
          // "-abc" treated as "--abc"
          setLongOpt(arg.drop(1))
        }
        else throw new UnrecognizedCmdLineOptException(arg.drop(1))
      }
    }
    if (currOprdIndex <= maxReqOprdIndex) throw new MissingCmdLineOprdsException(recognizedOprds(currOprdIndex).argName)
    //TODO: Check all req'd opts are present
    
    def setCurrOprd(arg: String) = {
      try {
        recognizedOprds(currOprdIndex).setValue(arg)
        currOprdIndex += 1
      } catch {
        case _: NoSuchElementException => throw new ExtraneousCmdLineOprdsException(arg)
      }
    }
    def setLongOpt(arg: String) = {
      try {
        val currOpt = recognizedLongOpts(before(arg, "="))
        if (!currOpt.isInstanceOf[UnitOpt] && !arg.contains("=")) gobbleNext = currOpt
        else if (currOpt.isInstanceOf[UnitOpt] && arg.contains("=")) throw new ExtraneousCmdLineOptArgException(currOpt.longName, after(arg, "="))
        else currOpt.setValue(after(arg, "="))
      } catch {
        case _: NoSuchElementException => throw new UnrecognizedCmdLineOptException(before(arg, "="))
      }
    }
    def setShortOpt(optName: Char, value: String) = {
      try {
        val currOpt = recognizedShortOpts(optName)
        if (!currOpt.isInstanceOf[UnitOpt] && value == null) gobbleNext = currOpt
        else if (currOpt.isInstanceOf[UnitOpt] && value != null) throw new ExtraneousCmdLineOptArgException(optName.toString, value)
        else currOpt.setValue(value)
      } catch {
        case _: NoSuchElementException => throw new UnrecognizedCmdLineOptException(optName.toString)
      }
    }
  }
}
