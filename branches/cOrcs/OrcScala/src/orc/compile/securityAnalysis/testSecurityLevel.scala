import orc.compile.securityAnalysis._
/**
 * @author laurenyew
 * testSecurityLevel
 * set of test suites to make sure the SecurityLevel.scala performs correctly
 */
object testSecurityLevel {

  def main(args: Array[String]) = {
    Console.println("Testing SecurityLevel.scala");
    createLattice()
    printLattice()
  }
  
  /**
   * createLattice
   * 
   * makes a basic lattice that we will use/add to in the course of testing.
   */
  def createLattice() {
    //TOP and BOTTOM are already created
    SecurityLevel.initializeGraph() // initialize the graph
    SecurityLevel.interpretParseSL("A",List(),List())
    SecurityLevel.interpretParseSL("G",List(),List())
    SecurityLevel.interpretParseSL("G",List("A"),List())//test editing already created
    SecurityLevel.interpretParseSL("B",List("TOP"),List())//test adding already created lvl as link
    SecurityLevel.interpretParseSL("C",List("TOP"),List("BOTTOM"))
    SecurityLevel.interpretParseSL("D",List("A","B"),List("F"))//test creating new link
    SecurityLevel.interpretParseSL("D",List(),List("E"))//test editing created and adding new link
    SecurityLevel.interpretParseSL("G",List("E"),List())
    SecurityLevel.interpretParseSL("C",List("TOP","TOP"),List())//try adding duplicates
   // SecurityLevel.interpretParseSL("X",List("X"),List())//if try to set a pointer to yourself, throws an exception
    
  }
  
  /**
   * Prints the lattice: 
   * if transitive closure worked right, should print all nodes
   *  
   */
  def printLattice() {
    Console.println("PRINTING CURRENT LATTICE --> ")
      printLevel(SecurityLevel.top)
    for(level <- SecurityLevel.top.allChildren)
      printLevel(level)
  }
  
  /**
   * Format:
   * Level Name: <String name>
   * Immediate Parents: <List of Strings>
   * Immediate Children: <List of Strings>
   * All parents: <List of Strings> 
   * All Children: <List of Strings>
   */
  def printLevel(level : SecurityLevel) {
    Console.println("Name: " + level.myName)
    Console.print("Immediate Parents: [" );
    for(l <- level.immediateParents)
      Console.print(l.myName + ", ");
    Console.println("]");
    Console.print("Immediate Children: [" );
    for(l <- level.immediateChildren)
      Console.print(l.myName + ", ");
    Console.println("]");
    Console.print("All Parents: [" );
    for(l <- level.allParents)
      Console.print(l.myName + ", ");
    Console.println("]");
    Console.print("All Children: [" );
    for(l <- level.allChildren)
      Console.print(l.myName + ", ");
    Console.println("]");
    Console.println()
  }
}