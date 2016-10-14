//
// ListDisplay.scala -- Scala class ListDisplay
// Project OrcSites
//
// Created by amp on Oct 11, 2016.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.gui

import javax.swing.JFrame
import javax.swing.DefaultListModel
import javax.swing.JList

import scala.collection.mutable

import ScalaSwingUtils._
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import java.awt.Dimension
import javax.swing.JScrollPane
import javax.swing.JPanel
import javax.swing.BoxLayout
import javax.swing.JLabel

class TableDisplay(title: String) extends JFrame {
  private val model = new DefaultTableModel()
  private lazy val table = onEDTNow { new JTable(model) }
  private lazy val scrollPane = onEDTNow {
    val scrollPane = new JScrollPane(table)
    table.setFillsViewportHeight(true)
    scrollPane
  }
  private lazy val stackPanel = onEDTNow {
    val stackPanel = new JPanel()
    stackPanel.setLayout(new BoxLayout(stackPanel, BoxLayout.PAGE_AXIS))
    stackPanel
  }

  onEDT {
    setTitle(title)
    setContentPane(stackPanel)
    stackPanel.add(scrollPane)
    setPreferredSize(new Dimension(300, 200))
    setVisible(true)
    setSize(new Dimension(600, 720))
    // TODO: This should definitely not be done this way.
    //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  }

  private val columns = mutable.Map[String, Int]()
  private var nextColumn = 0
  private var nextRow = 0

  def columnNames = synchronized {
    columns.toSeq.sortBy(_._2).map(_._1)
  }

  private def getColumn(name: String) = synchronized {
    if (columns contains name)
      columns(name)
    else {
      val n = nextColumn
      nextColumn += 1
      columns += name -> n
      onEDT {
        model.setColumnIdentifiers(columnNames.toArray[AnyRef])
      }
      n
    }
  }

  class Item() {
    private lazy val row = onEDTNow {
      val n = model.getRowCount()
      model.addRow(null: Array[AnyRef])
      n
    }
    def setColumn(name: String, v: AnyRef) = onEDT {
      model.setValueAt(v, row, getColumn(name))
    }
  }

  def addItem() = new Item()

  def addColumns(cols: List[String]) = synchronized {
    for (name <- cols) {
      if (!(columns contains name)) {
        val n = nextColumn
        nextColumn += 1
        columns += name -> n
      }
    }
    onEDT {
      model.setColumnIdentifiers(columnNames.toArray[AnyRef])
    }
  }
  
  def addText(text: String, atBottom: Boolean): Unit = onEDTNow {
    val index = if (atBottom) -1 else 0
    stackPanel.add(new JLabel(text), index)
  }
}

