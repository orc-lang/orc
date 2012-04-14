//
// SecurityLevel.scala -- Scala class SecurityLevel
// Project OrcScala
//
// $Id: SecurityLevel.scala 2933 2011-11-27 09:27 laurenyew $
//
// Created by laurenyew on Nov 20, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-SecurityType directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.securityAnalysis

import orc.error.compiletime.typing.ArgumentTypecheckingException
      
/**
  * A security Level.
  * The parser attaches this type to a variable along with other types
  * Parser also helps build Security Type graph
  * Values: name, parents list, children list
  *
  * @author laurenyew
  */
object SecurityLevel
{
    val bottom = new SecurityLevel()
      bottom.myName = "BOTTOM"
    val top = new SecurityLevel()
      top.myName = "TOP"
}
class SecurityLevel
{
  var myName = ""
  //"all" is used for the transitive closures
  //"immediate" is for traversing the tree, for instance to find closest shared parent/child
  var allParents : List[SecurityLevel] = List()
  var immediateParents : List[SecurityLevel] = List()
  var allChildren : List[SecurityLevel] = List() 
  var immediateChildren : List[SecurityLevel] = List()

      
  /**
   * Transitive closure for each SecurityType
   * does a search down tree (graph) for all children/parents
   */
  def transClosure(me: SecurityLevel) 
  {  
 
      //get all of the children possible
      me.allChildren = childTransClosure(me,me.allChildren)
      me.allChildren = me.allChildren ::: List(SecurityLevel.bottom)//we always have bottomSecurityType as a child
      
     //get all of the parents possible
      me.allParents = parentTransClosure(me, me.allParents)
      me.allParents = me.allParents ::: List(SecurityLevel.top)//we always have topSecurityType as a child
     
      //There should be no duplicates in the lists
      //check the lists
      Console.print("Children of SecurityType " + me.myName +": [")
      for(child <- me.allChildren)
        Console.print(child.myName + "," )
    
      Console.print("]\n Parents of SecurityType " + me.myName + ": [")
      for(parent <- me.allParents)
        Console.print(parent.myName + ",")
        
      Console.print("]\n")
      
      //check for cycles: if there is a cycle, one of the children will also be a parent
      for(child <- me.allChildren)
      {
        if(me.allParents.contains(child))
        {
          Console.println("Problem child " + child.myName + " cycle in SecurityType " + me.myName)
          throw new Exception("Possible cycle for SecurityType " + child.myName)
        }
      }
      
    
  }
  
  //returns a list of all possible children (added on to tempList)
  //depth first search
  def childTransClosure(me: SecurityLevel, listOfChildren: List[SecurityLevel]): List[SecurityLevel] =
  {
    //We first get the transClosure for all of the children
    var addChildren : List[SecurityLevel] = listOfChildren//added to list
    
      for(child <- me.allChildren)
      {
        if((!child.equals(SecurityLevel.bottom))//we stop at bottom SecurityType
            &&(!child.equals(SecurityLevel.top)))//and we don't want to redo TOP
        {
          if(!addChildren.contains(child))//prevents cycles because doesn't add children who it already saw
          {
              addChildren = addChildren ::: List(child)//add child to the list
              addChildren = addChildren ::: childTransClosure(child,addChildren)//go thru that child's children too
              addChildren = addChildren.distinct//get rid of duplicates
          }
        }
      }
    return addChildren
  }
  //get all possible parents
    //depth first search
  def parentTransClosure(me: SecurityLevel, listOfParents: List[SecurityLevel]): List[SecurityLevel] =
  {
    //We first get the transClosure for all of the parents
    //Console.println("ME: " + me.myName)
    var addParents : List[SecurityLevel] = listOfParents//added to list
    
      for(parent <- me.allParents)
      {
        if((!parent.equals(SecurityLevel.bottom))//we stop at bottom SecurityType
            &&(!parent.equals(SecurityLevel.top)))//and we don't want to redo TOP
        {
          if(!addParents.contains(parent))//prevents cycles because doesn't add children who it already saw
          {
              addParents = addParents ::: List(parent)//add child to the list
              addParents = addParents ::: parentTransClosure(parent,addParents)//go thru that child's children too
              addParents = addParents.distinct//get rid of duplicates
          }
        }
      }
    return addParents
  }
  
  /**
   * Creates a SecurityType as an instance/object of the trait class
   * not sure if I want to make this a trait but we'll try it as an object
   */
  def createSecurityLevel(name : String, p: List[SecurityLevel], c : List[SecurityLevel]) : SecurityLevel =
  {
   // Console.println("Creating SecurityType " + name)
    //make sure havent already created happens in interpretST
    var temp : SecurityLevel= new SecurityLevel()
      temp.myName = name
      temp.allParents = p
      temp.allChildren = c
      temp.immediateParents = p
      temp.immediateChildren = c
    
    //if the children list is empty, insert bottomSecurityType and connect (direct connection)
    //if the parent list is empty, insert topSecurityType and connect (direct connection)
    if(temp.immediateParents.isEmpty)
    {
      temp.immediateParents = List(SecurityLevel.top)
      SecurityLevel.top.immediateChildren = SecurityLevel.top.immediateChildren ::: List(temp)
    }
    if(temp.immediateChildren.isEmpty)
    {
      temp.allChildren = List(SecurityLevel.bottom)
      SecurityLevel.bottom.immediateParents = SecurityLevel.bottom.immediateParents ::: List(temp)
    }
    //need to let my parents/children know that I am attaching to them
    //since this is a creation, we can just add
    for(parent <- temp.immediateParents)
    {
      if(!parent.immediateChildren.contains(temp)){
        parent.immediateChildren = parent.immediateChildren ::: List(temp)
      }
      if(!parent.allChildren.contains(temp))
        parent.allChildren = parent.allChildren ::: List(temp)
    } 
    for(child <- temp.immediateChildren)
    {
       if(!child.immediateParents.contains(temp))
         child.immediateParents = child.immediateParents ::: List(temp)
       if(!child.allParents.contains(temp))
         child.allParents = child.allParents ::: List(temp)
    } 
     
    //Every node has bottom as one of its children and top as one of its parents
    //in allChildren/allParents
    if(!temp.allChildren.contains(SecurityLevel.bottom)){
      temp.allChildren = temp.allChildren ::: List(SecurityLevel.bottom)
      SecurityLevel.bottom.allChildren = SecurityLevel.bottom.allChildren ::: List(temp)
    }
    if(!temp.allParents.contains(SecurityLevel.top)){
        temp.allParents = temp.allParents ::: List(SecurityLevel.top)
        SecurityLevel.top.allParents = SecurityLevel.top.allParents ::: List(temp)
    }
    
    return temp
   
  }
  
  /**
   * SecurityLevelDiff
   * Function that will return the difference of SecurityLevels (-1,0,1)
   * 1 means that the subj has obj as one of its parents
   * 2 means that subj has obj as one of its children
   * 0 means that subj and obj are siblings
   * Where n is some integer
   * based on matrix
   */
  def SecurityLevelDiff(subj : SecurityLevel, obj : SecurityLevel) : Int = 
  {
    if(subj.allChildren.contains(obj))
        return 2
    if(subj.allParents.contains(obj))
        return 1   
    return 0;//we are siblings
  }
  
    //need to initialize top and bottom SecurityType of graph
    def initializeGraph()
    {
        SecurityLevel.top.allParents = List(SecurityLevel.top)//topSecurityType's parent should be itself
        SecurityLevel.top.allChildren = List(SecurityLevel.bottom)
        SecurityLevel.bottom.allChildren= List(SecurityLevel.bottom)//bottomSecurityType should have itself as the bottomSecurityType
        SecurityLevel.bottom.allParents = List(SecurityLevel.top)
    }
  
    /**
     * findByName
     * given a name of a securitySecurityType will look through created graph to try to find it.
     * If found, can return to do action on it
     * 
     * if this returns bottomSecurityType, then we know that the name was not found.
     */
    def findByName(name : String) : SecurityLevel = 
    {
        for(child <- SecurityLevel.top.allChildren)
        {
          if(child.myName.equalsIgnoreCase(name))
            return child
        }
        
        return SecurityLevel.bottom
    }
    
    /**
     * This will act as our interpreter, for the input parse so 
     * when the typechecker runs, it will create the graph for us
     * using the parser's output tokens
     * 
     * Note: 
     * it is possible for a security type to be given twice,
     * if so, we need to find the security type and add in any changes and redo the 
     * transitive closure.
     * The typechecker should handle the rest of the typechecking for integrity as well.
     */
    def interpretParseSL(name: String, parents: List[String], children: List[String]): SecurityLevel =
    {
      var currentLevel : SecurityLevel= findByName(name)
      var foundParent : SecurityLevel = null;
        var foundChild : SecurityLevel = null;
      //if this is true then the securityType has not yet been created
        if((currentLevel == SecurityLevel.bottom) && (!currentLevel.myName.equalsIgnoreCase("BOTTOM"))){
            currentLevel = createSecurityLevel(name, List(), List())
        }
      
        //go thru parents and add in new parents (creating if necessary)
        for(p <- parents)
        {
          foundParent = findByName(p)
          
          //level doesn't yet exist in the lattice, so create it
          if(foundParent == SecurityLevel.bottom && !p.equals("BOTTOM")){
            foundParent = createSecurityLevel(p,List(),List()) 
          }
          
          if(findinLevel(SecurityLevel.bottom.allParents,p) != 1)//haven't found the level in my immediate connections
          {
            foundParent.immediateChildren = foundParent.immediateChildren ::: List(currentLevel)
            foundParent.allChildren = foundParent.allChildren ::: List(currentLevel)
            currentLevel.immediateParents = currentLevel.immediateParents ::: List(foundParent)
            currentLevel.allParents = currentLevel.allParents ::: List(foundParent)//add that created parent to the list
          }
        }
        
        //go thru children and add in new children (creating if necessary)
        for(c <- children)
        {
          foundChild = findByName(c)
          
          //level doesn't yet exist in the lattice, so create it
          if(foundChild == SecurityLevel.bottom && !c.equals("BOTTOM")){
            foundChild = createSecurityLevel(c,List(),List()) 
          }
          
          if(findinLevel(SecurityLevel.bottom.allParents,c) != 1)//haven't found the level in my immediate connections
          {
            foundChild.immediateParents = foundChild.immediateParents ::: List(currentLevel)
            foundChild.allParents = foundChild.allParents ::: List(currentLevel)
            currentLevel.immediateChildren = currentLevel.immediateChildren ::: List(foundChild)
            currentLevel.allChildren = currentLevel.allChildren ::: List(foundChild)
          }
        }
        
         makeGraph()//need to redo transitive closure so can get graph
        return currentLevel
      
        
      
    }
    
    /**
     * findinLevel
     * 
     * find lookForLevel name in currentLevel's List
     * return 1 if found, -1 otherwise
     */
    def findinLevel (currentLevelList : List[SecurityLevel], lookForLevel : String) : Int =
    {
      for(temp <- currentLevelList)
      {
        if(temp.myName.equals(lookForLevel))
        {
          return 1
        }
      }
      //haven't found level
      return -1
    }
    
    /**
     * Call this to make the grpah i.e., fix all of the transitive closures for all of the levels
     */
    def makeGraph()
    {
      //do transitive closure for full graph
        transClosure(SecurityLevel.top)//do the transitive closure for the topLevel
        //do the transitive closure for all of my children (should be every node in the graph)
        for(child <- SecurityLevel.top.allChildren)
          transClosure(child)
    }
    
    /**
     * Find the closest shared child (can be one of the inputs or bottomLevel
     */
    def findClosestChild( leftSL: SecurityLevel, rightSL: SecurityLevel) : SecurityLevel =
    {
      //first check if either is the child of the other (that would mean that they are the closest)
      
        if(leftSL.allChildren.contains(rightSL)) rightSL
        if(rightSL.allChildren.contains(leftSL)) leftSL
        
      /*
       * no we know they are not ancestors, so they must be siblings, so I need to do a join of their
       * allChildren to get which children they share. To find their closest from there, I go down the 
       * immediateChildren to find which one is closest (Breadth first search?)
       */
        var sharedChildren : List[SecurityLevel] = List()
        
        //create the shared Children list
        for(child <- leftSL.allChildren)
          if(rightSL.allChildren.contains(child))
            sharedChildren = sharedChildren ::: List(child)
      
        //now go thru immediate children until find the closest match
        //TODO: figure out a way to go thru and find the closest shared child
         
        SecurityLevel.bottom
        
    }
    
  override def toString = "Security Type " + this.myName

/**
 * When I do the typechecker, I will do the overrides.
 */

}
