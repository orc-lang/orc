//
// CanvasImpl.scala -- Scala class CanvasImpl
// Project OrcSites
//
// Created by amp on Feb 15, 2015.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.gui

import javax.swing.JPanel
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.awt.Color
import java.awt.Dimension

/**
  * @author amp
  */
class CanvasImpl(x: Int, y: Int) extends JPanel {
  val image = new BufferedImage(x, y, BufferedImage.TYPE_3BYTE_BGR)

  override def getPreferredSize() = new Dimension(x, y)

  protected override def paintComponent(g: Graphics) = {
    g.drawImage(image, 0, 0, getWidth(), getHeight(), Color.CYAN, null)
  }
}