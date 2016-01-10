package orc.compile.tojava

import javax.tools.ToolProvider
import orc.run.tojava.OrcProgram
import java.nio.file.Files
import java.io.File
import scala.collection.JavaConversions._
import javax.tools.SimpleJavaFileObject
import java.net.URI
import javax.tools.JavaFileObject.Kind
import java.net.URLClassLoader

/** @author amp
  */
class JavaCompiler {
  val className = "Prog"

  def apply(code: String): Class[_ <: OrcProgram] = {
    val compiler = ToolProvider.getSystemJavaCompiler()
    val tempDir = Files.createTempDirectory("orc")
    tempDir.toFile.deleteOnExit()
    val options = Seq("-d", tempDir.toAbsolutePath.toString())
    val compilationUnits = Seq(InMemoryJavaFileObject(className, code))
    val task = compiler.getTask(null, null, null, options, null, compilationUnits)
    task.call()

    val cl = new URLClassLoader(Array(tempDir.toUri().toURL()))
    cl.loadClass(className).asSubclass(classOf[OrcProgram])
  }
}

/** java File Object represents an in-memory java source file <br>
  * so there is no need to put the source file on hard disk  *
  */
case class InMemoryJavaFileObject(className: String, code: String)
  extends SimpleJavaFileObject(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE) {
  override def getCharContent(ignoreEncodingErrors: Boolean) = code
}