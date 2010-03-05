//
// Philosopher.java -- Java class Philosopher
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.simanim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * @author Joseph Cooper
 */
public class Philosopher extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final double rTable = 200;
	private static final double buffer = 0.05 * rTable;
	private static final int winWidth = (int) (4 * rTable);
	public static Image forkImg = Toolkit.getDefaultToolkit().getImage(Philosopher.class.getResource("fork.png"));
	public static Image plateImg = Toolkit.getDefaultToolkit().getImage(Philosopher.class.getResource("plate.png"));
	public static Image headImg = Toolkit.getDefaultToolkit().getImage(Philosopher.class.getResource("head.png"));
	public static Image thoughtImg = Toolkit.getDefaultToolkit().getImage(Philosopher.class.getResource("thought.png"));
	public static Image sauceImg = Toolkit.getDefaultToolkit().getImage(Philosopher.class.getResource("sauce.png"));

	private final int nPhil;
	private final double theta;
	private final double rPlateCenter;
	private final double rPlate;
	private final double rFace;
	private final double rFaceCenter;

	public int armState[][];
	public int thinkState[];
	public int eatState[];

	public BufferedImage bImage;
	public int frameNo;

	public Philosopher() {
		this(5);
	}

	/**
	 * Create the table and invite all the philosophers
	 * to dinner.  Initially, none are eating, thinking,
	 * or reaching for their forks.
	 * @param n How many philosophers are coming.
	 */
	public Philosopher(final int n) {
		nPhil = n;
		theta = 2.0 * Math.PI / nPhil;
		rPlateCenter = rTable / (1 + Math.sin(theta / 2.0));
		rPlate = rPlateCenter * Math.sin(theta / 2.0) - buffer;
		rFace = rPlate + buffer;
		rFaceCenter = rTable + rPlate + 2 * buffer;

		armState = new int[2][nPhil];
		thinkState = new int[nPhil];
		eatState = new int[nPhil];

		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		final GraphicsDevice gd = ge.getDefaultScreenDevice();
		final GraphicsConfiguration gc = gd.getDefaultConfiguration();

		// Create an image that supports transparent pixels

		bImage = gc.createCompatibleImage(winWidth, winWidth);
		frameNo = 0;

		//armState[0][0]=1;
		//armState[1][0]=7;
		//thinkState[3]=5;
		//eatState[2]=9;
	}

	public int setFork(final int phil, final int side, final int s) {
		armState[side][phil] = s;
		return phil;
	}

	public int setThink(final int phil, final int s) {
		thinkState[phil] = s;
		return phil;
	}

	public int setEat(final int phil, final int s) {
		eatState[phil] = s;
		return phil;
	}

	public int open() {
		WindowUtilities.openInJFrame(this, winWidth, winWidth);
		return 0;
	}

	public int redraw() {
		update(this.getGraphics());
		/*paintComponent(bImage.getGraphics());
		try {
			javax.imageio.ImageIO.write(bImage, "jpeg", new File("c:/Temp/pics/frame"+frameNo+".jpeg"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		++frameNo;*/

		return 0;
	}

	@Override
	public void paintComponent(final Graphics g) {
		clear(g);
		final Graphics2D g2d = (Graphics2D) g;
		AffineTransform saveTrans;

		final double fWidth = forkImg.getWidth(this) * buffer / forkImg.getHeight(this);

		g2d.setColor(Color.darkGray);
		g2d.fillOval((int) (winWidth / 2.0 - rTable), (int) (winWidth / 2.0 - rTable), (int) (2 * rTable), (int) (2 * rTable));
		g2d.translate(winWidth / 2.0, winWidth / 2.0);

		for (int ii = 0; ii < nPhil; ++ii) {
			//Draw forks
			g2d.drawImage(forkImg, (int) (rTable - fWidth), (int) (-buffer / 2), (int) fWidth, (int) buffer, this);

			//Draw plates
			g2d.rotate(theta / 2);
			saveTrans = g2d.getTransform();
			g2d.setColor(Color.white);
			g2d.translate((int) rPlateCenter, 0);
			g2d.drawImage(plateImg, (int) -rPlate, (int) -rPlate, (int) (2 * rPlate), (int) (2 * rPlate), this);

			//Eating
			if (eatState[ii] != 0) {
				final float shrink = eatState[ii] / 10.0f;
				g2d.drawImage(sauceImg, (int) (-shrink * rPlate), (int) (-shrink * rPlate), (int) (2 * shrink * rPlate), (int) (2 * shrink * rPlate), this);
			}
			g2d.setTransform(saveTrans);

			//Draw philosophers
			g2d.setColor(Color.black);
			g2d.setStroke(new BasicStroke((int) buffer));

			// Left arm
			if (armState[0][ii] != 0) {
				final float leftEx = 0.3f + 0.7f * armState[0][ii] / 10.0f;
				g2d.translate((int) rFaceCenter, 0);
				g2d.drawLine(0, 0, (int) (leftEx * (-(rFaceCenter + buffer) + Math.cos(theta / 2) * rTable)), (int) (leftEx * Math.sin(theta / 2) * (rTable - buffer)));
				g2d.fillOval((int) (leftEx * (-(rFaceCenter + buffer) + Math.cos(theta / 2) * rTable) - buffer), (int) (leftEx * Math.sin(theta / 2) * (rTable - buffer) - buffer), (int) (2 * buffer), (int) (2 * buffer));
				g2d.setTransform(saveTrans);
			}

			// Right arm
			if (armState[1][ii] != 0) {
				final float rightEx = 0.3f + 0.7f * armState[1][ii] / 10.0f;
				g2d.translate((int) rFaceCenter, 0);
				g2d.drawLine(0, 0, (int) (rightEx * (-(rFaceCenter + buffer) + Math.cos(theta / 2) * rTable)), (int) (-rightEx * Math.sin(theta / 2) * (rTable - buffer)));
				g2d.fillOval((int) (rightEx * (-(rFaceCenter + buffer) + Math.cos(theta / 2) * rTable) - buffer), (int) (-rightEx * Math.sin(theta / 2) * (rTable - buffer) - buffer), (int) (2 * buffer), (int) (2 * buffer));
				g2d.setTransform(saveTrans);
			}

			// Head
			g2d.translate((int) rFaceCenter, 0);
			g2d.drawImage(headImg, (int) -rFace, (int) -rFace, (int) (2 * rFace), (int) (2 * rFace), this);
			//g2d.setTransform(saveTrans);

			//Thinking
			//g2d.translate((int)(rFaceCenter),0);
			if (thinkState[ii] != 0) {
				final double rotEx = 2 * Math.PI * thinkState[ii] / 10.0f;
				g2d.rotate(rotEx, 0, 0);
				g2d.drawImage(thoughtImg, (int) -rFace, (int) -rFace, (int) (2 * rFace), (int) (2 * rFace), this);
			}
			g2d.setTransform(saveTrans);

			//Rotate the rest of the way
			g2d.rotate(theta / 2);
		}

	}

	// super.paintComponent clears offscreen pixmap,
	// since we're using double buffering by default.
	protected void clear(final Graphics g) {
		super.paintComponent(g);
	}

	public static void main(final String[] args) {
		final Philosopher se = new Philosopher();
		se.open();

	}
}
