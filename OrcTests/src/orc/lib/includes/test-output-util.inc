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
  ( if JavaSys.getProperty("orc.executionlog.dir", "").isEmpty()
    then
      Error("java system property orc.executionlog.dir must be set")
    else signal
  )  >>
  File(JavaSys.getProperty("orc.executionlog.dir"))  >outDir>
  ( if outDir.exists()
    then signal
    else Error("Directory must exist: " + outDir.getAbsolutePath())
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
  outDir + "/" + fileBasenamePrefix + basename + fileBasenameSuffix + "." + extension

{--
@def writeFactorValuesTable(factorValues)
Each process in an experiment that writes experimental-condition-
dependent files should call this method to write a file that associates
the experimental condition (factor) values with its output files.
--}
def writeFactorValuesTable(factorValues) =
  import class FactorValue = "orc.test.util.FactorValue"
  FactorValue.writeFactorValuesTable(factorValues)
