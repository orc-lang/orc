{- test-output-util.inc -- Orc include for test procedure output
 -
 - Created by jthywiss on Oct 4, 2017 10:43:33 PM
 -}

{--
@def setupOutput()
Set up the test output by setting orc.executionlog.dir, if it isn't set;
creating the output directory, if needed; and adding the
TestEnvironmentDescription shutdown hook.
--}
def setupOutput() =
  import class JavaSys = "java.lang.System"
  import class File = "java.io.File"
  import class TestEnvironmentDescription = "orc.test.util.TestEnvironmentDescription"
  import class TestRunNumber = "orc.test.util.TestRunNumber"
  JavaSys.getProperty("orc.executionlog.dir", "")  >outDir>
  ( if outDir.isEmpty()
    then
      TestRunNumber.getNext()  >testRunNumber>
      File("runs/" + testRunNumber + "/raw-output")  >outDirName>
      JavaSys.setProperty("orc.executionlog.dir", outDirName)
    else signal
  )  >>
  ( if outDir.mkdirs()
    then Println("Created output directory: " + outDir.getCanonicalPath())
    else signal
  )  >>  
  TestEnvironmentDescription.dumpAtShutdown()

{--
@def buildOutputPathname(basename :: String, extension :: String) :: String
Build a full pathname from the output file base name and extension.
--}
def buildOutputPathname(basename :: String, extension :: String) :: String =
  import class JavaSys = "java.lang.System"
  JavaSys.getProperty("orc.executionlog.dir", ".")  >outDir>
  JavaSys.getProperty("orc.executionlog.fileprefix", "")  >fileBasenamePrefix>
  JavaSys.getProperty("orc.executionlog.filesuffix", "")  >fileBasenameSuffix>
  outDir + "/" + fileBasenamePrefix + basename + "." + extension
