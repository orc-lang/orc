{- write-csv-file.inc -- Orc include for writing tabular data (comma-separated value) files
 -
 - Created by jthywiss on Oct 4, 2017 6:39:20 PM
 -}

{--
@def writeCsvFile(pathname :: String, description :: String, tableColumnTitles :: List[String], rows :: List[List[Top]]) :: Signal
Write a tabular data (comma-separated value) file,
with the columns titled per the <code>tableColumnTitles</code> list, and
the rows of data given in the <code>rows</code> list of lists.

Recommendation: Column titles should be human-readable, with sentence-style
capitalization, and include units of measure in parentheses.  For example,
["Power setting (%)", "Airspeed (m/s)", "Fuel flow (kg/s)"].

The file is created using the given <code>pathname</code> string.
The call to <code>writeCsvFile</code> will halt with an error message if the
file already exists.

The <code>description</code> string is used to describe the table in user-
facing messages (e.g., "Engine power settings" or "Method cumulative elapsed
time").
--}
def writeCsvFile(pathname :: String, description :: String, tableColumnTitles :: List[String], rows :: List[List[Top]]) :: Signal =
  import class File = "java.io.File"
  import class FileOutputStream = "java.io.FileOutputStream"
  import class OutputStreamWriter = "java.io.OutputStreamWriter"
  import class CsvWriter = "orc.util.CsvWriter"
  File(pathname)  >outFile>
  ( if outFile.createNewFile()
    then signal
    else Error(description + ": File already exists: " + outFile.getCanonicalPath())
  )  >>
  OutputStreamWriter(FileOutputStream(outFile), "UTF-8")  >csvOut>
  CsvWriter(csvOut)  >csvWriter>
  csvWriter.writeHeader(tableColumnTitles)  >>
  csvWriter.writeRowsOfTraversables(rows)  >>
  csvOut.close()  >>
  Println(description + " written to " + outFile.getCanonicalPath())
