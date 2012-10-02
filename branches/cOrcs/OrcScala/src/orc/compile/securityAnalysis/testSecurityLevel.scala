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
    testMeet()
    testWrite()
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
   * tests writing.
   * parent cannot write to child
   * child can write to parent
   * siblings cannot write to eachother
   * you can write to yourself.
   * 
   * Works correctly
   */
  def testWrite()
  {
    Console.println("Testing the Write --> ")
    val A = SecurityLevel.findByName("A")
    val B = SecurityLevel.findByName("B")
    val C = SecurityLevel.findByName("C")
    val D = SecurityLevel.findByName("D")
    val E = SecurityLevel.findByName("E")
    val F = SecurityLevel.findByName("F")
    val G = SecurityLevel.findByName("G")
  
    printWrite(B,B) // TRUE --> Can write to self
    printWrite(A,F) // FALSE --> Can't write from parent to child
    printWrite(E,B) // True --> Can write to a grandparent
    printWrite(A,C) // FALSE --> siblings can't write to eachother
    
    
  }
  
  def printWrite(level1 : SecurityLevel, level2 : SecurityLevel)
  {
    val answer = SecurityLevel.canWrite(level1,level2)
    
    Console.println("Can you write info from " + level1.myName + " to " 
        + level2.myName + "? " + answer)
  }
  /**
   * tests the meet. Makes sure that it is working correctly.
   * So far it is working correctly
   */
  def testMeet()
  {
    Console.println("Testing the Meet --> ")
    val A = SecurityLevel.findByName("A")
    val B = SecurityLevel.findByName("B")
    val C = SecurityLevel.findByName("C")
    val D = SecurityLevel.findByName("D")
    val E = SecurityLevel.findByName("E")
    val F = SecurityLevel.findByName("F")
    val G = SecurityLevel.findByName("G")
    
    printMeet(A,B)//D --> tests siblings with a shared immediate 
    printMeet(A,C)//BOTTOM --> tests siblings with no shared immediate
    printMeet(D,D)//D --> tests meet on same
    printMeet(D,E)//E --> tests child, parent
    printMeet(F,B)//F --tests grandchild, grandparent 
  }
  
  def printMeet(level1 : SecurityLevel, level2 : SecurityLevel)
  {
    val meetLvl = SecurityLevel.meet(level1,level2)
    Console.println("Meet of " + level1.myName + " and " + level2.myName + " = " + meetLvl.myName)
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