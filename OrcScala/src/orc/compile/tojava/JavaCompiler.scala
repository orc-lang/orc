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

import javax.tools.ToolProvider
import orc.run.tojava.OrcProgram
import java.nio.file.Files
import java.io.File
import javax.tools.SimpleJavaFileObject
import java.net.URI
import javax.tools.JavaFileObject.Kind
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.SimpleFileVisitor
import java.nio.file.Path
import java.nio.file.FileVisitResult
import java.nio.file.attribute.BasicFileAttributes
import java.io.IOException
import java.util.logging.Level
import orc.compile.Logger

/** @author amp
  */
class JavaCompiler {
  import scala.collection.JavaConversions._

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
    val task = compiler.getTask(null, null, null, options, null, compilationUnits)
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
