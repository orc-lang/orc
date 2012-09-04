package orc.android

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.view.View
import android.widget.Toast
import android.util.Log
import android.widget.TextView
import android.os.AsyncTask
import android.os.Handler
import android.app.AlertDialog
import android.widget.EditText
import android.content.DialogInterface
import android.app.Dialog
import android.app.ProgressDialog
import java.util.LinkedList
import java.lang.StringBuffer
import java.io._
import java.util.Hashtable
import OrchardCompileLogger.CompileMessage
import orc.compile.StandardOrcCompiler
import orc.compile.parse.OrcStringInputContext
import orc.progress.NullProgressMonitor$
import orc.error.compiletime.CompileLogger
import orc.run.StandardOrcRuntime
import orc.OrcEventAction
import orc.OrcEvent
import orc.lib.str.PrintEvent
import orc.lib.util.PromptEvent
import orc.lib.util.PromptCallback
import orc.lib.util.PromptCallback
import orc.compile.parse.OrcStringInputContext
import orc.run.StandardOrcRuntime
import orc.OrcEventAction
import orc.lib.str.PrintEvent
import orc.lib.util.PromptEvent
import orc.compile.StandardOrcCompiler
import orc.OrcEvent
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.HttpClient
import org.apache.http.HttpResponse
import org.apache.http.util.EntityUtils
import org.apache.http.HttpEntity
import org.apache.http.client.methods.HttpGet
import android.content.ClipData.Item
import orc.ast.oil.xml.OrcXML

/*
 * http://stackoverflow.com/questions/9924015/eclipse-android-scala-made-easy-but-still-does-not-work/11084146#11084146
 *
 */

/**
 * @author Joao Barbosa, Ricardo Bernardino
 */

class RunActivity extends Activity {
  lazy val my_button = findViewById(R.id.button).asInstanceOf[Button]
  lazy val my_tv: TextView = findViewById(R.id.tvtext).asInstanceOf[TextView]
  lazy val path: String = getIntent().getExtras().get("path").asInstanceOf[String]
  lazy val TAG = "RunActivity"
  /* Alternative to using runOnUiThread() method, by using handler.post(runnable) */
  var handler: Handler = null
  var input: EditText = null
  /* hashtable containing the callback function of the given prompt. The key is the toString() of the AlertDialog */
  lazy val callbackPrompts: Hashtable[String, PromptCallback] = new Hashtable[String, PromptCallback]()
  /* hashtable containing the input of the given prompt. The key is the toString() of the AlertDialog */
  lazy val inputPrompts: Hashtable[String, EditText] = new Hashtable[String, EditText]()
  /* output string */
  lazy val resultString: StringBuffer = new StringBuffer()
  lazy val fileContent: StringBuffer = new StringBuffer()

  implicit def func2OnClickListener(func: (View) => Unit) = {
    new View.OnClickListener() {
      override def onClick(v: View) = func(v)
    }
  }

  implicit def alertdialogOnClickListener(func: (DialogInterface, Int) => Unit) = {
    new DialogInterface.OnClickListener() {
      override def onClick(dialog: DialogInterface, whichButton: Int) = func(dialog, whichButton)
    }
  }

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.run_activity)
    handler = new Handler()

    /*
     * If the path string starts with http:// we know that we were previously
     * on the ExamplesSectionFragment class which explores the try_orc website.
     * The path is sent via the activity intent.
     */
    if (path.startsWith("http://")) {
      new GetOrcFilesTask().execute()
    }
  }

  /* "Run" button callback function, specified in layouts/run_activity.xml */
  def startProgress(view: View) = {
    // Do something long
    val runnable: Runnable = new Runnable() {
      override def run() = {
        var str: String = ""

        if (!path.startsWith("http://")) { // Read from file

          /* 
           * Read simple.orc file -> res/raw/simple.orc 
           * val is: InputStream = getResources().openRawResource(R.raw.simple)
           */

          fileContent.setLength(0) // Clean StringBuffer

          /* Read file from sdcard */
          val is: InputStream = new FileInputStream(path)
          val reader: BufferedReader = new BufferedReader(new InputStreamReader(is))
          if (is != null) {
            while ({ str = reader.readLine(); str != null }) {
              fileContent.append(str + "\n")
            }
          }
          is.close()
        }

        var result: orc.ast.oil.nameless.Expression = null
        lazy val options = new OrcBindings()

        if (path.endsWith(".orc")) { /* Compile .orc file */
          val orc_string: OrcStringInputContext = new OrcStringInputContext(fileContent.toString())
          val compiler: StandardOrcCompiler = new StandardOrcCompiler()
          val compileMsgs: LinkedList[CompileMessage] = new LinkedList[CompileMessage]
          val cl: CompileLogger = new OrchardCompileLogger(compileMsgs)
          options.usePrelude = false
          result = compiler.apply(orc_string, options, cl, NullProgressMonitor$.MODULE$)
        } else { /* .oil file */
          val oil_is: InputStream = new ByteArrayInputStream(fileContent.toString().getBytes());
          result = OrcXML.readOilFromStream(oil_is)
        }

        /* 
         * If you want the output of a single program on the console
         * just reset resultString: resultString.setLength(0)
         */
        resultString.append("--------\n")

        val exec: StandardOrcRuntime = new StandardOrcRuntime("Orc")
        /*
         * In order to display the outputs of the program,
         * we will need to override the callback functions:
         *   - PrintEvent: to display Print/Println calls
         *   - PromptEvent: create a Dialog Pop-Up in Android
         *   - pusblished: to display the values to be published on the console
         *   - caught: handle the error messages from the execution
         */
        val event: OrcEventAction = new OrcEventAction() {
          override def other(event: OrcEvent) {
            Log.i(TAG, "entered other")
            event match {
              case PrintEvent(text) => {
                resultString.append(text + " ")
              }
              case PromptEvent(prompt, callback) => {
                val alert: AlertDialog.Builder = new AlertDialog.Builder(RunActivity.this)

                alert.setTitle(prompt)

                Log.i(TAG, prompt)

                alert.setPositiveButton("Ok", positiveButtonOnClick _)
                alert.setNegativeButton("Cancel", negativeButtonOnClick _)

                lazy val alert2: AlertDialog = alert.create()

                /* In order to alter the UI, we have got to run it on the main thread */
                runOnUiThread(new Runnable() {
                  override def run() = {
                    // Set an EditText view to get user input
                    input = new EditText(getApplication())

                    alert.setView(input)
                    println(alert2)
                    callbackPrompts.put(alert2.toString(), callback)
                    inputPrompts.put(alert2.toString(), input)
                    alert2.show()
                  }
                })

              }
              case e => {
                handler.post(new Runnable() {
                  override def run() = {
                    Toast.makeText(getApplicationContext(), "Unhandled event.", Toast.LENGTH_LONG).show();
                  }
                });
              }
            }
          }
          override def published(value: AnyRef) {
            resultString.append(value + "\n")

            /* In order to alter the UI, we have got to run it on the main thread */
            handler.post(new Runnable() {
              override def run() = {
                my_tv.setText(resultString.toString())
              }
            });
          }
          override def caught(e: Throwable) {
            Log.i(TAG, "Error " + e.printStackTrace())
            handler.post(new Runnable() {
              override def run() = {
                Toast.makeText(getApplicationContext(), "An error occurred while running the program.", Toast.LENGTH_LONG).show();
              }
            });
          }
        }

        /* Execute the compiled Orc program */
        exec.runSynchronous(result, event.asFunction(_), options)
        exec.stop()
      }
    }
    new Thread(new ThreadGroup("orc"), runnable, "orc", 40000).start() // 40000 bytes stack size
    /*
     * Due to the high memory requirements of Orc and the Android system
     * stack size restrictions on the Main Thread (8KB), we always have to create
     * our own Thread so that we can specify how many bytes we would like the stack to have.
     * NOTE: It is said on the Android Documentation, that the stack size value may be
     * ignored in some systems.
     */
  }

  /* Prompt positive button callback */
  def positiveButtonOnClick(dialog: DialogInterface, whichButton: Int): Unit = {
    val dialogNew = dialog.asInstanceOf[AlertDialog]
    println(dialogNew);
    val callback = callbackPrompts.get(dialogNew.toString())
    val text = inputPrompts.get(dialogNew.toString()).getText().toString()
    callback.respondToPrompt(text)
    callbackPrompts.remove(dialogNew.toString())
  }

  /* Prompt negative button callback */
  def negativeButtonOnClick(dialog: DialogInterface, whichButton: Int): Unit = {
    val dialogNew = dialog.asInstanceOf[AlertDialog]
    val callback = callbackPrompts.get(dialogNew.toString())
    callback.cancelPrompt()
    callbackPrompts.remove(dialogNew.toString())
  }

  /*
   * On Android, we cannot access the network through the Main Thread,
   * so either we use the AsyncTask class (as shown below), or we create
   * a new Thread. We chose the former due to its simplicity and the ability
   * to communicate with the Main Thread for the UI changes.
   */
  class GetOrcFilesTask() extends AsyncTask[AnyRef, Unit, Unit] {

    var response: String = ""
    var client: HttpClient = null

    var dialog: Dialog = null

    /* can use UI thread here */
    override def onPreExecute() {
      dialog = ProgressDialog.show(RunActivity.this, "", "Getting Orc file...", true);

      client = new DefaultHttpClient()
    }

    /* automatically done on worker thread (separate from UI thread) */
    override def doInBackground(params: AnyRef*): Unit = {
      val httpget: HttpGet = new HttpGet(path)
      var response: HttpResponse = null
      var response_file: String = null

      try {
        response = client.execute(httpget)

        response_file = getBodyFromResponse(response)
      } catch {
        case e: Exception =>
          Log.e("doInBackGround", e.toString())
          null
      }

      fileContent.append(response_file)
    }

    /* can use UI thread here */
    override def onPostExecute(params: Unit) = {
      dialog.dismiss()
    }

    def getBodyFromResponse(response: HttpResponse): String = {
      var result: String = ""
      val entity: HttpEntity = response.getEntity()

      try {
        if (entity != null) {
          var len: Long = entity.getContentLength();
          if ((len != -1L) && (len < 2048L)) {
            result = EntityUtils.toString(entity);
          } else
            result = EntityUtils.toString(entity);
        }
      } catch {
        case e: Exception => Log.e("getBodyFromResponse", e.toString())
      }

      result
    }
  }
}