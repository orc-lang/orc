//
// JavaCompiler.scala -- Scala class JavaCompiler
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.tojava

import java.io.IOException
import java.net.{ URI, URLClassLoader }
import java.nio.charset.StandardCharsets
import java.nio.file.{ FileVisitResult, Files, Path, SimpleFileVisitor }
import java.nio.file.attribute.BasicFileAttributes
import java.util.logging.Level

import javax.tools.{ SimpleJavaFileObject, ToolProvider }
import javax.tools.JavaFileObject.Kind

import scala.collection.JavaConverters._

import orc.compile.Logger
import orc.run.tojava.OrcProgram

/** @author amp
  */
class JavaCompiler {
  val pkgName = "orctojavaoutput"
  val className = "Prog"

  def deleteDirectoryRecursive(p: Path) = {
    Files.walkFileTree(p, new SimpleFileVisitor[Path]() {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    })
  }

  def apply(rawcode: String): Class[_ <: OrcProgram] = {
    val code = s"package $pkgName;\n\n$rawcode"
    val compiler = ToolProvider.getSystemJavaCompiler()
    val tempDir = Files.createTempDirectory("orc")
    val options = Seq("-d", tempDir.toAbsolutePath.toString())
    val compilationUnits = Seq(InMemoryJavaFileObject(className, code))
    val task = compiler.getTask(null, null, null, options.asJava, null, compilationUnits.asJava)
    task.call()

    if (Logger.julLogger.isLoggable(Level.FINE)) {
      // Write out code file next to bytecode
      val javaFile = tempDir.resolve(s"$pkgName/$className.java")
      Files.write(javaFile, code.getBytes(StandardCharsets.UTF_8))
      Logger.fine(s"Storing java source and class files to $tempDir")
    }

    // If FINE is not logged then delete the files when we are done.
    if (!Logger.julLogger.isLoggable(Level.FINE)) {
      // Setup a hook to delete the class and source (if written)
      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run(): Unit = {
          deleteDirectoryRecursive(tempDir)
        }
      })
    }

    val cl = new URLClassLoader(Array(tempDir.toUri().toURL()))
    cl.loadClass(s"$pkgName.$className").asSubclass(classOf[OrcProgram])
  }
}

/** java File Object represents an in-memory java source file <br>
  * so there is no need to put the source file on hard disk  *
  */
case class InMemoryJavaFileObject(className: String, code: String)
  extends SimpleJavaFileObject(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE) {
  override def getCharContent(ignoreEncodingErrors: Boolean) = code
}
