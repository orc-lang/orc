package orc.input.explorer

import java.io.File
import java.io.FilenameFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView
import orc.android.R
import orc.android.RunActivity
import android.content.Intent
import android.widget.Toast

/*Explorer based on https://github.com/mburman/Android-File-Explore.git */
/**
 * A fragment representing a section of the app, which displays the file explorer.
 */
class InputSectionFragment extends Fragment {

  var currentDirectory: File = new File("/")
  var directoryEnteries: List[String] = List()
  // Stores names of traversed directories
  var str = new java.util.ArrayList[String]()
  // Check if the first level of the directory structure is the one showing
  var firstLvl: Boolean = true
  var fileList: Array[Item] = null
  var path: File = new File(Environment.getRootDirectory() /*.getExternalStorageDirectory()*/ + "")
  var chosenFile: String = null
  var adapter: ListAdapter = null

  val TAG = "InputSection"

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)

    Log.d(TAG, path.getAbsolutePath())
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreate(savedInstanceState)

    Log.d(TAG, path.getAbsolutePath())

    inflater.inflate(R.layout.explorer_list, container, false)

  }

  override def onActivityCreated(savedState: Bundle) = {
    super.onActivityCreated(savedState)

    loadFileList()

    var lv: ListView = getActivity().findViewById(R.id.listView1).asInstanceOf[ListView]

    lv.setAdapter(adapter)
    lv.setOnItemClickListener(new OnItemClickListener() {
      override def onItemClick(a: AdapterView[_], v: View, which: Int, l: Long) = {
        chosenFile = fileList(which).file
        var sel: File = new File(path + "/" + chosenFile)

        if (sel.isDirectory()) {
          firstLvl = false

          // Adds chosen directory to list
          str.add(chosenFile)

          fileList = null
          path = new File(sel + "")

          loadFileList()

          Log.d(TAG, path.getAbsolutePath())
        } // Checks if 'up' was clicked
        else if (chosenFile.equalsIgnoreCase("up") && !sel.exists()) {

          // present directory removed from list
          var s = str.remove(str.size() - 1)

          // path modified to exclude present directory
          path = new File(path.toString().substring(0,
            path.toString().lastIndexOf(s)))
          fileList = null

          // if there are no more directories in the list, then
          // its the first level
          if (str.isEmpty()) {
            firstLvl = true
          }
          loadFileList()

          Log.d(TAG, path.getAbsolutePath())

        } // File picked
        else {
          val runActivityIntent = new Intent(getActivity(), classOf[RunActivity])
          runActivityIntent.putExtra("path", path + "/" + chosenFile)
          startActivity(runActivityIntent)
        }
      }
    })

    Log.d(TAG, path.getAbsolutePath())
  }

  def loadFileList(): Unit = {
    try {
      path.mkdirs()
    } catch {
      case e => Log.e(TAG, "Unable to write on the sd card.")
    }

    // Checks whether path exists
    if (path.exists()) {

      var filter: FilenameFilter = new FilenameFilter() {
        override def accept(dir: File, filename: String): Boolean = {
          var sel: File = new File(dir, filename)
          // Filters based on whether the file is hidden or not, and ends with either .orc or .oil
          ((sel.isDirectory() || (sel.isFile() && (filename.endsWith(".orc") || filename.endsWith(".oil")))) && !sel.isHidden())
        }
      }

      var fList: Array[String] = path.list(filter)
      fileList = new Array[Item](fList.length)

      var i = 0
      for (i <- 0 until fList.length) {
        fileList(i) = new Item(fList(i), R.drawable.file_icon)

        // Convert into file path
        var sel: File = new File(path, fList(i))

        // Set drawables
        if (sel.isDirectory())
          fileList(i).icon = R.drawable.directory_icon
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
    } else {
      Log.e(TAG, "path does not exist")
      Toast.makeText(getActivity().getApplicationContext(), "This path does not exist.", Toast.LENGTH_LONG).show();
    }

    adapter = new ArrayAdapter[Item](getActivity().getApplicationContext(), android.R.layout.select_dialog_item, android.R.id.text1, fileList) {
      override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
        var view: View = super.getView(position, convertView, parent)
        var textView: TextView = view.findViewById(android.R.id.text1).asInstanceOf[TextView]

        // put the image on the text view
        textView.setCompoundDrawablesWithIntrinsicBounds(
          fileList(position).icon, 0, 0, 0)

        // add margin between image and text (support various screen
        // densities)
        var dp5: Int = (5 * getResources().getDisplayMetrics().density + 0.5f).asInstanceOf[Int]
        textView.setCompoundDrawablePadding(dp5)
        textView.setTextColor(Color.BLACK)

        view
      }
    }

    /* refresh the adapter to show the new directory */
    getActivity().findViewById(R.id.listView1).asInstanceOf[ListView].setAdapter(adapter)
  }
}