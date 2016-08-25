-- Runnable version of OrcO: A Concurrency-First Approach to Objects Figure 3

include "gui.inc"

import class ArrayList = "java.util.ArrayList" 
import class Collections = "java.util.Collections" 

class MutableList {
  val underlying = Collections.synchronizedList(ArrayList())
  def add(v) = underlying.add(v)
  def each() = repeat(IterableToStream(underlying))
}

class GUI {
  val db
  val queryEntry = TextField()
  val queryButton = Button("Query")
  val resultsList = ListComponent()
  val frame = Frame([queryEntry, queryButton, resultsList]) 
  
  -- GUI event handling
  val _ = repeat({
    {| queryButton.onAction() |
       queryEntry.onAction() |} >>
    queryButton.setEnabled(false) >>
    resultsList.clear() >>
    db.query(queryEntry.getText()) >r>
    resultsList.add(r) >> Println(r) >> stop ;
    queryButton.setEnabled(true)
  })
  -- Initialization event handling
  val _ = queryButton.setEnabled(false) >>
          {| db.ready | Rwait(5000) |} >>
          queryButton.setEnabled(true)
}

class Database {
  val data = new MutableList
  val ready =
    loadData() >s> data.add(s) >> stop ; signal
  def loadData() = "a" | "b" | Rwait(2000) >> "c"
  def query(query) =
    ready >> data.each() >s> (if s.matches(query) then s else stop)
}

{|

val gui = new GUI {
  val db = new Database
} 

Println("The database only includes values 'a', 'b', and 'c'.") >> stop |
gui.frame.onClosing()

|}