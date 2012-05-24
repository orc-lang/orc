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
    SecurityLevel.interpretParseSL("A2",List(),List())
    SecurityLevel.interpretParseSL("A2",List("A"),List())
    SecurityLevel.interpretParseSL("B",List("TOP"),List())
    
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