package orc.input.examples

import java.net.URL
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.TagNode
import android.app.ProgressDialog
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView
import orc.android.R
import orc.android.RunActivity
import orc.input.explorer.Item
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView
import android.content.Intent
import android.widget.Toast

/**
 * @author Joao Barbosa, Ricardo Bernardino
 */

class ExamplesSectionFragment extends Fragment {

  // Stores names of traversed directories
  var str = new java.util.ArrayList[String]()

  // Check if the first level of the directory structure is the one showing
  var firstLvl: Boolean = true
  var fileList: Array[Item] = Array[Item]()
  var path: String = ""
  var chosenFile: String = null
  var adapter: ListAdapter = null
  val base_url = "http://orc.csres.utexas.edu/tryorc"
  var url = "http://orc.csres.utexas.edu/tryorc"

  val TAG = "Explorer"

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreate(savedInstanceState)

    inflater.inflate(R.layout.examples, container, false)

  }

  override def onActivityCreated(savedState: Bundle) = {
    super.onActivityCreated(savedState)

    loadFileList()
    getActivity().findViewById(R.id.listView2).asInstanceOf[ListView].setOnItemClickListener(new OnItemClickListener() {
      override def onItemClick(a: AdapterView[_], v: View, which: Int, l: Long) = {
        chosenFile = fileList(which).file

        if (chosenFile.endsWith("/")) { // is a Directory
          firstLvl = false
          url = base_url + "/" + chosenFile

          loadFileList()
        } // Checks if 'up' was clicked
        else if (chosenFile.equalsIgnoreCase("up")) {

          fileList = null
          val lastSlashIndex = url.length - url.split("/").last.length - 2
          url = url.substring(0, lastSlashIndex)

          // if there are no more directories in the list, then
          // its the first level
          if (url.equals(base_url))
            firstLvl = true
          else
            firstLvl = false

          loadFileList()
        } // File is picked and will be run on the RunActivity
        else {
          val runActivityIntent = new Intent(getActivity(), classOf[RunActivity])
          runActivityIntent.putExtra("path", url + chosenFile)
          startActivity(runActivityIntent)
        }
      }
    })
  }

  def loadFileList(): Unit = {
    if (url.endsWith(".orc") || url.endsWith(".oil")) {
      val runActivityIntent = new Intent(getActivity(), classOf[RunActivity])
      runActivityIntent.putExtra("path", url)
      startActivity(runActivityIntent)
    } else {
      new GetOrcFilesTask().execute()
    }
  }

  class GetOrcFilesTask() extends AsyncTask[AnyRef, Array[String], Array[String]] {

    var response: String = ""
    var client: HttpClient = null

    var dialog: ProgressDialog = null

    // can use UI thread here
    override def onPreExecute() {
      dialog = ProgressDialog.show(getActivity(), "", "Getting Orc files from Server...", true);

      client = new DefaultHttpClient()
    }

    // automatically done on worker thread (separate from UI thread)
    override def doInBackground(params: AnyRef*): Array[String] = {
      val url_orc: URL = new URL(url)

      if (url.equals("http://orc.csres.utexas.edu/tryorc"))
        firstLvl = true
      else
        firstLvl = false

      val cleaner: HtmlCleaner = new HtmlCleaner()
      var node: TagNode = null
      var paths: Array[String] = null
      try {
        /* fetch the html page */
        node = cleaner.clean(url_orc)

        /* parse all the link tags */
        val nodes = node.evaluateXPath("//td/a")
        paths = new Array[String](nodes.length - 2)

        /* ignore the first two links (parent directory and .svn) */
        for (i <- 2 until nodes.length) {
          paths(i - 2) = nodes(i).asInstanceOf[TagNode].getText().toString()
          paths(i - 2) = paths(i - 2).substring(0, paths(i - 2).length() - 6) // remove &nbsp;
        }
      } catch {
        case e: Exception => e.printStackTrace()
      }

      paths
    }

    // can use UI thread here
    override def onPostExecute(result: Array[String]) = {

      if (result != null) {
        fileList = new Array[Item](result.length)

        var i = 0
        for (filename <- result) {
          fileList(i) = new Item(filename, R.drawable.file_icon)

          // Set drawables
          if (filename.endsWith("/")) //Directory
            fileList(i).icon = R.drawable.directory_icon

          fileList(i).path = base_url + filename
          i += 1
        }

        if (!firstLvl) {
          var temp: Array[Item] = new Array[Item](fileList.length + 1)
          var t = 0
          for (t <- 0 until fileList.length) {
            temp(t + 1) = fileList(t)
          }
          temp(0) = new Item("Up", R.drawable.directory_up)
          fileList = temp
        }

        adapter = new ArrayAdapter[Item](getActivity().getApplicationContext(), android.R.layout.select_dialog_item, android.R.id.text1, fileList) {
          override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
            val view: View = super.getView(position, convertView, parent)
            val textView: TextView = view.findViewById(android.R.id.text1).asInstanceOf[TextView]

            // put the image on the text view
            textView.setCompoundDrawablesWithIntrinsicBounds(
              fileList(position).icon, 0, 0, 0)

            // add margin between image and text (support various screen
            // densities)
            val dp5: Int = (5 * getResources().getDisplayMetrics().density + 0.5f).asInstanceOf[Int]
            textView.setCompoundDrawablePadding(dp5)
            textView.setTextColor(Color.BLACK)

            view
          }
        }

        //refresh the adapter to show the new directory
        getActivity().findViewById(R.id.listView2).asInstanceOf[ListView].setAdapter(adapter)
        //getListView().setAdapter(adapter)

      } else {
        Toast.makeText(getActivity().getApplicationContext(), "An error occurred while getting the directory.", Toast.LENGTH_LONG).show();
      }

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
