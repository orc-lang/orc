//
// Impl.scala -- Java land classes for the Orc GUI layer.
// Project OrcSites
//
// $Id$
//
// Created by amp on Feb 11, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.gui

import javax.swing.JFrame
import java.awt.Component
import javax.swing.SwingUtilities
import javax.swing.JButton
import orc.values.sites.Site0
import orc.Handle
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import javax.swing.BoxLayout
import java.awt.event.WindowListener
import java.awt.event.WindowAdapter
import javax.swing.WindowConstants
import java.awt.event.WindowEvent
import scala.collection.mutable
import scala.ref.WeakReference
import java.awt.Dimension

object Impl {
  def onEDT(f: => Unit): Unit = {
    SwingUtilities.invokeLater(new Runnable {
      def run() {
        f
      }
    })
  }
}

abstract class ImplComponent {
  val underlying: Component
  
  val blockedHandles = mutable.Buffer[Handle]()
  def haltBlockedHandles() = {
    blockedHandles map { _.halt }
    blockedHandles.clear()
  }
}

/**
  *
  * @author amp
  */
class ImplFrame extends ImplComponent {
  import Impl._
  val underlying = new JFrame()
  onEDT {
    underlying.getContentPane().setLayout(new BoxLayout(underlying.getContentPane(), BoxLayout.PAGE_AXIS))
    underlying.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
  }
  
  def setVisible(b: Boolean) = {
    //underlying.setPreferredSize(new Dimension(300, 300));
    underlying.pack()
    underlying.setVisible(b)
  }
  
  def dispose() = onEDT {
    underlying.setVisible(false)
    underlying.dispose()
    haltBlockedHandles()
  }
  
  def add(c: ImplComponent) = onEDT {
    underlying.add(c.underlying)
  }
  
  val onClosed = new Site0 {
    def call(h: Handle): Unit = {
      blockedHandles += h
      underlying.addWindowListener(new WindowAdapter {
        val handleRef = WeakReference(h)
        override def windowClosing(e: WindowEvent) = {
          if(handleRef.get map { _.isLive } getOrElse false)
            handleRef.get map { _.publishNonterminal(e) }
          else
            underlying.removeWindowListener(this)
        }
      })
    }
  }
}

class ImplButton extends ImplComponent {
  import Impl._
  val underlying = new JButton()
  
  def dispose() = onEDT {
    underlying.setVisible(false)
    haltBlockedHandles()
  }
  
  def setText(s: String) = onEDT {
    underlying.setText(s)
  }
  
  val onClicked = new Site0 {
    def call(h: Handle): Unit = {
      blockedHandles += h
      underlying.addActionListener(new ActionListener {
        val handleRef = WeakReference(h)
        def actionPerformed(e: ActionEvent) = {
          if(handleRef.get map { _.isLive } getOrElse false)
            handleRef.get map { _.publishNonterminal(e) }
          else
            underlying.removeActionListener(this)
        }
      })
    }
  }
}