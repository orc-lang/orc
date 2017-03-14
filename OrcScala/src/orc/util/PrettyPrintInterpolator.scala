package orc.util

import scala.language.implicitConversions
import java.util.regex.Pattern

class StringBuilderWrapper(sb: StringBuilder)(implicit interp: PrettyPrintInterpolator) {
  import interp._

  private var indentLevel = indentLevelOffset
  var lineNo = lineNoOffset;
  private var deleteNewLine = false

  def append(s: String) = {
    // TODO: This could probably be optimized by processing the string manually.
    // StringBuilder supports append(CharSequence s, int start, int end)
    val a = NEW_LINE_MARGIN_PATTERN.matcher(s).replaceAll("\n" + " " * (interp.indentStep * indentLevel))
    //val b = interp.BLANK_LINE_PATTERN.matcher(a).replaceAll("\n")
    val b = if (deleteNewLine) {
      deleteNewLine = false
      NEW_LINE_PATTERN.matcher(a).replaceAll("")
    } else a
    lineNo += b.count(_ == '\n')
    sb.append(b)
  }

  def indent(n: Int) = indentLevel += n

  def dropNextNewLine() = deleteNewLine = true

  override def toString() = sb.toString()
}

class FragmentAppender(val appendTo: StringBuilderWrapper => Unit)(implicit interp: PrettyPrintInterpolator) {
  def build() = {
    val sb = new StringBuilderWrapper(new StringBuilder)
    appendTo(sb)
    sb.toString
  }

  override def toString() = build()
}

object FragmentAppender {
  def apply(s: String)(implicit interp: PrettyPrintInterpolator) = new FragmentAppender(_.append(s))

  def mkString(fs: Iterable[FragmentAppender], sep: String = "")(implicit interp: PrettyPrintInterpolator) = {
     new FragmentAppender(sb => {
       var first = true
       for(f <- fs) {
         if(!first) {
           sb append sep
         } else {
           first = false
         }
         f.appendTo(sb)
       }
     })
  }
}

/** A pretty printing utility that builds a string interpolator.
  *
  * This supports indentation and a special processor which can be used to
  * call back into the pretty printer for specific types.
  *
  * The interpolator supports margin space ending with either | or ".
  *
  */
class PrettyPrintInterpolator {
  implicit val implicitConversions = scala.language.implicitConversions
  implicit val interp = this

  val NEW_LINE_MARGIN_PATTERN = Pattern.compile(raw"""\n([\t ]*[|"])?""")
  val NEW_LINE_PATTERN = Pattern.compile(raw"""\n *""")
  val BLANK_LINE_PATTERN = Pattern.compile(raw"""\n[\t ]*\n""")

  val indentStep = 2
  val lineNoOffset = 0
  val indentLevelOffset = 0

  val DNL = new FragmentAppender(_.dropNextNewLine())
  val StartIndent = new FragmentAppender(_.indent(1))
  val EndIndent = new FragmentAppender(_.indent(-1))
  val LineNumber = new FragmentAppender(sb => sb append sb.lineNo.toString)

  abstract class Interpolator(val sc: StringContext) {
    import StringContext._
    import sc._

    def pp(args: (() => Any)*): FragmentAppender = new FragmentAppender((sb: StringBuilderWrapper) => {
      checkLengths(args)
      val pi = parts.iterator
      val ai = args.iterator
      val first = pi.next()
      if(!NEW_LINE_MARGIN_PATTERN.matcher(first).matches())
        sb append treatEscapes(first)
      while (ai.hasNext) {
        val a = ai.next
        a() match {
          case f: FragmentAppender => f.appendTo(sb)
          case a if processValue.isDefinedAt(a) => processValue(a).appendTo(sb)
          case null => sb append "null"
          case a => sb append a.toString
        }
        sb append treatEscapes(pi.next())
      }
    })

    val processValue: PartialFunction[Any, FragmentAppender]
  }
  //def implicitInterpolator(sc: StringContext) = new Interpolator(sc)

  object ImplicitStringToFragmentAppender {
    implicit def string2FragmentAppender(s: String) = new FragmentAppender((sb) => sb append s)
  }

  implicit def byName2NoArg[T](f: => T): () => T = () => f
}

/** A little test for the class above.
  */
object PrettyPrintInterpolator {
  case class Test(a: Int, b: Any)
  case class Context(i: Int)

  def main(args: Array[String]): Unit = {
    class MyPrettyPrintInterpolator extends PrettyPrintInterpolator {
      implicit def implicitInterpolator(sc: StringContext)(implicit ctx: Context) = new MyInterpolator(sc)
      class MyInterpolator(sc: StringContext)(implicit ctx: Context) extends Interpolator(sc) {
        override val processValue: PartialFunction[Any, FragmentAppender] = {
          case Test(a, b) => pp"""$a[${ctx.i}] {$StartIndent
                                 "$b$EndIndent
                                 "}"""
        }
      }
    }
    val INTERP = new MyPrettyPrintInterpolator

    import INTERP._

    implicit val ctx = Context(42)

    println(pp"""$DNL
                "${Test(2, Test(4, null))} Test
                "$LineNumber
                "${Test(5, Test(2, Test(4, null)))}
                """)
  }
}
