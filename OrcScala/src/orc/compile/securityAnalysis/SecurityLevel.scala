//
// SecurityType.scala -- Scala class SecurityType
// Project OrcScala
//
// $Id: SecurityType.scala 2933 2011-11-27 09:27 laurenyew $
//
// Created by laurenyew on Nov 20, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-SecurityType directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.error.compiletime.typing.ArgumentTypecheckingException

//put into SecurityLevel
class Level
{
  var myName = "LEVEL"
  var parents : List[Level] = List()
  var children : List[Level] = List() 
}

class NamedLevel(name: String, p: List[Level], c: List[Level]) extends Level
{
  this.myName = name;
  this.parents = p
  this.children = c
}
/**
  * A security type.
  * The parser attaches this type to a variable along with other types
  * Parser also helps build Security Type graph
  * Values: name, parents list, children list
  *
  * @author dkitchin
  */
//refactor
case class SecurityType()
{
  var bottomSecurityType = new NamedLevel("BOTTOM",List(),List())
  var topSecurityType = new NamedLevel("TOP",List(),List())
  
  /**
   * Transitive closure for each SecurityType
   * does a search down tree (graph) for all children/parents
   */
  def transClosure(me: Level) 
  {  
    //print out initial children and parents (direct)
 /*   Console.print("SecurityType " + me.myName + " initial children: ")
    for(child <- me.children)
      Console.print(child.myName + ",")
      
    Console.print("\nSecurityType " + me.myName + " initial parents: ")
    for(parent <- me.parents)
      Console.print(parent.myName + ",")
      Console.println()
 */   
    
      //get all of the children possible
      me.children = childTransClosure(me,List())
      me.children = me.children ::: List(bottomSecurityType)//we always have bottomSecurityType as a child
      
     //get all of the parents possible
      me.parents = parentTransClosure(me, List())
      me.parents = me.parents ::: List(topSecurityType)//we always have topSecurityType as a child
     
      //There should be no duplicates in the lists
      //check the lists
      Console.print("Children of SecurityType " + me.myName +": [")
      for(child <- me.children)
        Console.print(child.myName + "," )
    
      Console.print("]\n Parents of SecurityType " + me.myName + ": [")
      for(parent <- me.parents)
        Console.print(parent.myName + ",")
        
      Console.print("]\n")
      
      //check for cycles: if there is a cycle, one of the children will also be a parent
      for(child <- me.children)
      {
        if(me.parents.contains(child))
        {
          Console.println("Problem child " + child.myName + " cycle in SecurityType " + me.myName)
          throw new Exception("Possible cycle for SecurityType " + child.myName)
        }
      }
      
    
  }
  
  //returns a list of all possible children (added on to tempList)
  //depth first search
  def childTransClosure(me: Level, listOfChildren: List[Level]): List[Level] =
  {
    //We first get the transClosure for all of the children
    //Console.println("ME: " + me.myName)
    var addChildren : List[Level] = listOfChildren//added to list
    
      for(child <- me.children)
      {
    /*    Console.print("Children so far: ")
        for(child2 <-addChildren)
          Console.print(child2.myName + " ")
        Console.println()
     */   
        if((!child.equals(bottomSecurityType))//we stop at bottom SecurityType
            &&(!child.equals(topSecurityType)))//and we don't want to redo TOP
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
  def parentTransClosure(me: Level, listOfParents: List[Level]): List[Level] =
  {
    //We first get the transClosure for all of the parents
    //Console.println("ME: " + me.myName)
    var addParents : List[Level] = listOfParents//added to list
    
      for(parent <- me.parents)
      {
    /*    Console.print("Parents so far: ")
        for(parent2 <-addParents)
          Console.print(parent2.myName + " ")
        Console.println()
     */   
        if((!parent.equals(bottomSecurityType))//we stop at bottom SecurityType
            &&(!parent.equals(topSecurityType)))//and we don't want to redo TOP
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
  def createSecurityType(name : String, p: List[Level], c : List[Level]) : Level =
  {
   // Console.println("Creating SecurityType " + name)
    var temp = new NamedLevel(name, p, c)
    
    //if the children list is empty, insert bottomSecurityType and connect (direct connection)
    //if the parent list is empty, insert topSecurityType and connect (direct connection)
    if(temp.parents.isEmpty)
    {
      temp.parents = List(topSecurityType)
      topSecurityType.children = topSecurityType.children ::: List(temp)
    }
    if(temp.children.isEmpty)
    {
      temp.children = List(bottomSecurityType)
      bottomSecurityType.parents = bottomSecurityType.parents ::: List(temp)
    }
    //need to let my parents/children know that I am attaching to them
   // Console.print("My parents: ")
    for(parent <- temp.parents)
    {
     //  Console.print(parent.myName + ",")
      if(!parent.children.contains(temp))
        parent.children = parent.children ::: List(temp)
    } 
    //  Console.print("\nMyChildren: ")
     for(child <- temp.children)
    {
    //   Console.print(child.myName + ",")
       if(!child.parents.contains(temp))
       child.parents = child.parents ::: List(temp)
    } 
   // Console.println()
     
    //Every node has bottom as one of its children and top as one of its parents
    
    if(!temp.children.contains(bottomSecurityType)){
      temp.children = temp.children ::: List(bottomSecurityType)
    }
    if(!temp.parents.contains(topSecurityType)){
        temp.parents = temp.parents ::: List(topSecurityType)
    }
    
    return temp
   
  }
  
  /**
   * SecurityTypeDiff
   * Function that will return the difference of SecurityTypes (-1,0,1)
   * 1 means that the subj has obj as one of its parents
   * 2 means that subj has obj as one of its children
   * 0 means that subj and obj are siblings
   * Where n is some integer
   * based on matrix
   */
  def SecurityTypeDiff(subj : Level, obj : Level) : Integer = 
  {
   /* Console.println("Subj: " + subj.myName + " Obj: " + obj.myName)
    Console.print(subj.myName + "'s Children: ")
    for(child <- subj.children)
      Console.print(child.myName + " " )
    
      Console.print("\n" + subj.myName + "'s Parents: ")
    for(parent <- subj.parents)
      Console.print(parent.myName + " ")
      Console.println()
    */
    if(subj.children.contains(obj))
        return 2
    if(subj.parents.contains(obj))
        return 1   
    return 0;//we are siblings
  }
  
    //need to initialize top and bottom SecurityType of graph
    def initializeGraph()
    {
        topSecurityType.parents = List(topSecurityType)//topSecurityType's parent should be itself
        topSecurityType.children = List(bottomSecurityType)
        bottomSecurityType.children= List(bottomSecurityType)//bottomSecurityType should have itself as the bottomSecurityType
        bottomSecurityType.parents = List(topSecurityType)
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
        for(child <- topSecurityType.children)
        {
          if(child.myName.equalsIgnoreCase(name))
            return child
        }
        
        return bottomSecurityType
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
    def interpretParseST(name: String, parents: List[String], children: List[String]): SecurityLevel =
    {
      var currentLevel : Level= findByName(name)
      var createdParent : Level = null;
        var createdChild : Level = null;
      //if this is true then the securityType has not yet been created
        if((currentLevel == bottomSecurityType) && (!currentLevel.myName.equalsIgnoreCase("BOTTOM"))){
            currentLevel = createSecurityType(name, List(), List())
        }
      
          //go thru parents and add in new parents (creating if necessary)
        for(p <- parents)
        {
          if(findinLevel(currentLevel.parents,p) != 1)//haven't found the level so need to create it
          {
            //we dont know what the parent is yet, so we just make its name, we can add its connections later
            createdParent = createSecurityType(p,List(),List())
            currentLevel.parents ::: List(createdParent)//add that created parent to the list
          }
        }
        
        //go thru children and add children if necessary (create as needed)
        for(c <- children)
        {
          if(findinLevel(currentLevel.children,c) != 1)//haven't found the level so need to create it
          {
            //we dont know what the parent is yet, so we just make its name, we can add its connections later
            createdChild = createSecurityType(c,List(),List())
            currentLevel.children ::: List(createdChild)//add that created parent to the list
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
    def findinLevel (currentLevelList : List[Level], lookForLevel : String) : Integer =
    {
      for(temp <- currentLevelList)
      {
        if(temp.myName.equalsIgnoreCase(lookForLevel))
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
        transClosure(topSecurityType)//do the transitive closure for the topLevel
        //do the transitive closure for all of my children (should be every node in the graph)
        for(child <- topSecurityType.children)
          transClosure(child)
    }
    
  override def toString = "Security Type " + name

/**
 * When I do the typechecker, I will do the override.
 */

}
