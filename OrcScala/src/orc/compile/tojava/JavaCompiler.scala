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

/** @author amp
  */
class JavaCompiler {
  import scala.collection.JavaConversions._
  
  val pkgName = "orctojavaoutput"
  val className = "Prog"

  def apply(rawcode: String): Class[_ <: OrcProgram] = {
    val code = s"package $pkgName;\n\n$rawcode"
    val compiler = ToolProvider.getSystemJavaCompiler()
    val tempDir = Files.createTempDirectory("orc")
    // FIXME: This leaves temporary files laying around for some reason.
    tempDir.toFile.deleteOnExit()
    val options = Seq("-d", tempDir.toAbsolutePath.toString())
    val compilationUnits = Seq(InMemoryJavaFileObject(className, code))
    val task = compiler.getTask(null, null, null, options, null, compilationUnits)
    task.call()
    
    // Write out code file next to bytecode
    Files.write(tempDir.resolve(s"$pkgName/$className.java"), code.getBytes(StandardCharsets.UTF_8))

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