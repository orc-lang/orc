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
// the LICENSE file found in the project's top-SecurityLevel directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.securityAnalysis

import orc.error.compiletime.typing.ArgumentTypecheckingException
     
/**
  * A security Level.
  * The parser attaches this level to a variable
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
      /**
     * need to initialize top and bottom SecurityLevel of graph
     */
      def initializeGraph()
      {
          SecurityLevel.top.allParents = List(SecurityLevel.top)//topSecurityLevel's parent should be itself
          SecurityLevel.top.allChildren = List(SecurityLevel.bottom)
          SecurityLevel.bottom.allChildren= List(SecurityLevel.bottom)//bottomSecurityLevel should have itself as the bottomSecurityLevel
          SecurityLevel.bottom.allParents = List(SecurityLevel.top)
      }
    
    
        /**
       * This will act as our interpreter, for the input parse so 
       * when the typechecker runs, it will create the graph for us
       * using the parser's output tokens
       * 
       * Note: 
       * it is possible for a security type to be given twice,
       * if so, we need to find the security level and add in any changes and redo the 
       * transitive closure.
       * The typechecker should handle the rest of the typechecking for integrity as well.
       */
      def interpretParseSL(name: String, parents: List[String], children: List[String]): SecurityLevel =
      {
        var currentLevel : SecurityLevel= findByName(name)
         
        if(currentLevel == null)
          Console.println("SL " + name + " cannot be found by findByName.")
        else
          Console.println("Editing SL: " + name)
        
          
        var temp : SecurityLevel = null
          
          //create the level if not already made
          if(currentLevel == null){
            currentLevel = createSecurityLevel(name, List(), List())
          }
        
          //go thru parents and add in new parents (creating if necessary)
          for(p <- parents)
          {
            if(p.equals(name)) throw new Exception("Cannot create a security level with a self-pointer")
            temp = findByName(p)
           
            //if level doesn't yet exist in the lattice create it
            if(temp == null){
              temp = createSecurityLevel(p,List(),List(currentLevel)) 
            }
            
            if(findinLevel(currentLevel.immediateParents,temp) != 1)//haven't found the level in my immediate connections
            {
              temp.immediateChildren = temp.immediateChildren ::: List(currentLevel)
              currentLevel.immediateParents = currentLevel.immediateParents ::: List(temp)
            }
          }
          
          //go thru children and add in new children (creating if necessary)
          for(c <- children)
          {
            if(c.equals(name)) throw new Exception("Cannot create a security level with a self-pointer")
             temp = findByName(c)
            
            //if level doesn't yet exist in the lattice create it
            if(temp == null){
              temp = createSecurityLevel(c,List(currentLevel),List()) 
            }
            
            if(findinLevel(currentLevel.immediateChildren,temp) != 1)//haven't found the level in my immediate connections
            {
              
              temp.immediateParents = temp.immediateParents ::: List(currentLevel)
              currentLevel.immediateChildren = currentLevel.immediateChildren ::: List(temp)
            }
          }
          
          makeGraph()//need to redo transitive closure so can get graph
          return currentLevel  
      }
  
  
    /**
     * Creates a SecurityLevel as an instance/object of the trait class
     * not sure if I want to make this a trait but we'll try it as an object
     */
    def createSecurityLevel(name : String, p: List[SecurityLevel], c : List[SecurityLevel]) : SecurityLevel =
    {
      Console.println("Creating new SL: " + name)
      
      //make sure havent already created happens in interpretST
      var temp : SecurityLevel= new SecurityLevel()
        temp.myName = name
        temp.immediateParents = p
        temp.immediateChildren = c
        temp.allParents = p
        temp.allChildren = c

      //add yourself to allparents/allchildren for top and bottom (necessary for trans closure
        if(!p.contains(SecurityLevel.top))
        {
          temp.allParents = List(SecurityLevel.top)
          SecurityLevel.top.allChildren = SecurityLevel.top.allChildren ::: List(temp)
        }
        if(!c.contains(SecurityLevel.bottom)){
          temp.allChildren = List(SecurityLevel.bottom)
          SecurityLevel.bottom.allParents = SecurityLevel.bottom.allParents ::: List(temp)
        }
      //need to let my immediate parents/children know that I am attaching to them
      //since this is a creation, we can just add
      for(parent <- temp.immediateParents)
      {
          parent.immediateChildren = parent.immediateChildren ::: List(temp)
          if(!parent.allChildren.contains(temp))//we may add immediate link to an already transitive link
           parent.allChildren = parent.allChildren ::: List(temp)
      } 
      for(child <- temp.immediateChildren)
      {
           child.immediateParents = child.immediateParents ::: List(temp)
          if(!child.allParents.contains(temp))//we may add immediate link to an already transitive link
           child.allParents = child.allParents ::: List(temp)
      } 
       
      return temp
     
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
     * Transitive closure for each SecurityLevel
     * does a search down tree (graph) for all children/parents
     */
    def transClosure(me: SecurityLevel) 
    {  
        //get all of the children possible
        me.allChildren = childTransClosure(me,me.allChildren)
        if(!me.allChildren.contains(SecurityLevel.bottom))
          me.allChildren = me.allChildren ::: List(SecurityLevel.bottom)
        
       //get all of the parents possible
        me.allParents = parentTransClosure(me, me.allParents)
        if(!me.allParents.contains(SecurityLevel.top))
          me.allParents = me.allParents ::: List(SecurityLevel.top)
        //check for duplicates/cycles: there is a cycle there will be a duplicate child and parent (duplicates are bad anyways)
        val allConnections : List[SecurityLevel] = me.allParents ::: me.allChildren
        val noDuplicates : List[SecurityLevel] = allConnections.distinct
        
        if(allConnections.size != noDuplicates.size)
        {
            Console.println("\n\nProblem in securityLevel " + me.myName + " Possible Cycle.\n")
           // throw new Exception("Possible cycle for SecurityLevel " + me.myName)
        }
        
      
    }
    
    /**
     * returns a list of all possible children (added on to tempList)
     * depth first search
     */
    def childTransClosure(me: SecurityLevel, listOfChildren: List[SecurityLevel]): List[SecurityLevel] =
    {
      //We first get the transClosure for all of the children
      var addChildren : List[SecurityLevel] = listOfChildren//added to list
      
      //go thru all children and recursively add all children immediately underneath it.
        for(child <- me.immediateChildren)
        {
                addChildren = addChildren ::: List(child)//add child to the list
                addChildren = childTransClosure(child,addChildren)//go thru that child's children too
                //get rid of duplicates caused by child and grandchild being the same
                addChildren = addChildren.distinct
        }
      return addChildren
    }
      /**
       * get all possible parents
       * depth first search
       */
      
      def parentTransClosure(me: SecurityLevel, listOfParents: List[SecurityLevel]): List[SecurityLevel] =
      {
        //We first get the transClosure for all of the parents
        var addParents : List[SecurityLevel] = listOfParents//added to list
        
        //we recursively go thru all parents
        for(parent <- me.immediateParents)
          {
            if((!parent.equals(SecurityLevel.bottom))//we stop at bottom SecurityLevel
                &&(!parent.equals(SecurityLevel.top)))//and we don't want to redo TOP
            {
                  addParents = addParents ::: List(parent)//add child to the list
                  addParents =  parentTransClosure(parent,addParents)//go thru that child's children too
                  //prevent duplicates just by going down and having same parent and grandparent
                  addParents = addParents.distinct
            }
          }
        return addParents
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
      def securityLevelDiff(subj : SecurityLevel, obj : SecurityLevel) : Int = 
      {
        if(subj.allChildren.contains(obj))
            return 2
        if(subj.allParents.contains(obj))
            return 1   
        return 0;//we are siblings
      }
    
      
    
      /**
       * findByName
       * given a name of a securitySecurityLevel will look through created graph to try to find it.
       * If found, can return to do action on it
       * 
       * if this returns bottomSecurityLevel, then we know that the name was not found.
       */
      def findByName(name : String) : SecurityLevel = 
      {
        if(SecurityLevel.top.myName.equals(name))
          return SecurityLevel.top
        else{  
          for(child <- SecurityLevel.top.allChildren)
          {
            if(child.myName.equals(name))
              return child
          }
        }
          return null
      }
      
      
      /**
       * findinLevel
       * 
       * find lookForLevel name in currentLevel's List
       * return 1 if found, -1 otherwise
       */
      def findinLevel (currentLevelList : List[SecurityLevel], lookForLevel : SecurityLevel) : Int =
      {
        for (temp : SecurityLevel <- currentLevelList)
        {
          if(temp.myName.equals(lookForLevel.myName))
            return 1
        }
        return -1
      }
      
      
      
      /**
       *  Find the meet (greatest lower bound -- closest common child) in the sublabel lattice
       * of this label and another label.
       */
      def meet( leftSL: SecurityLevel, rightSL: SecurityLevel) : SecurityLevel =
      {
          if(leftSL == null) return rightSL
          if(rightSL == null) return leftSL
        
          //check if they are the same level
          if(leftSL.myName.equals(rightSL.myName)) return leftSL
          //check if either is the child of the other (that would mean that they are the closest)
          val diff = securityLevelDiff(leftSL,rightSL)
          if(diff == 1) return leftSL
          else if(diff == 2) return rightSL
          else//security levels are siblings so we need their meet
          {//to find the meet, we do the breadth first search down leftSL's immediate children links on all children for rightSL
        
            var sharedChildren : List[SecurityLevel] = List()
            
            //create the shared Children list
            for(child <- leftSL.immediateChildren)
              if(rightSL.immediateChildren.contains(child))
                sharedChildren = sharedChildren ::: List(child)
          
            //now go thru immediate children until find the closest match (BFS)
            return levelBFS(leftSL,sharedChildren)
          
         }
      }
    
    /**
     * Breadth First Search thru levels
     * Takes subj (treats as root of tree)
     * goes through the "tree" thru immediateChildren and checks each node with checklist to see
     * if they are there. If found, returns that security level. Otherwise returns null  
     */
    def levelBFS(subj: SecurityLevel, checkList: List[SecurityLevel]) : SecurityLevel = 
    {
      if(subj.immediateChildren.isEmpty) return SecurityLevel.bottom
      
        for(checkLevel <- checkList)
        {
          if(subj.immediateChildren.contains(checkLevel))
            return checkLevel
        }
        
        for(child <- subj.allChildren)
        {
          return levelBFS(child,checkList)
        }
        
        return SecurityLevel.bottom
    }
      
    /**
     * checks on whether or not siteSl can write to argSl without causing an integrity error
     * the site must have lower sL than the arguments (shouldn't be able to write high level info to lower level things)
     * if the site is not a child (transitive closure) of the argument, return false (can't be a sibling since 
     * we shouldn't be able to write to other categories)  
     */
    def canWrite(siteSl : SecurityLevel, argSl : SecurityLevel) : Boolean = {
      
      if(argSl eq siteSl) //can write if we are the same security level
      {
        true
      }
      else{
        //we aren't the same security level      
        if(siteSl == null || argSl == null)
        {
            if(siteSl == null) true //if the site is null it can write to any info w/ security (integrity)
            //this means that argSl is null and siteSl isn't so we cant write info with security to something with none
            else false 
        }
        else
        {
          //can write if the object is of higher security
          if(argSl.allChildren.contains(siteSl))
              true
          else
              false
      
        }
      }
    }
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
  
        override def toString = "Security Level " + this.myName
  
  }
