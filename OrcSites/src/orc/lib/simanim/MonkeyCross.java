//
// MonkeyCross.java -- Java class MonkeyCross
// Project OrcSites
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import javax.swing.JPanel;

/**
 * An example of drawing/filling shapes with Java2D in Java 1.2. From tutorial
 * on learning Java2D at http://www.apl.jhu.edu/~hall/java/Java2D-Tutorial.html
 * 1998 Marty Hall, http://www.apl.jhu.edu/~hall/java/
 */

public class MonkeyCross extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int winWidth = 800;

    private final int links;
    public int linkState[];
    public Stack<Integer> leftCount;
    public Stack<Integer> rightCount;
    public boolean cSide;

    float baseHeight = 0.5f * winWidth;
    float baseRad = 0.05f * winWidth;
    Ellipse2D.Double circle = new Ellipse2D.Double(0, 0, baseRad, baseRad);

    public static Image cliffImg = Toolkit.getDefaultToolkit().getImage(MonkeyCross.class.getResource("cliffs.png"));
    public static Image monkeyImg = Toolkit.getDefaultToolkit().getImage(MonkeyCross.class.getResource("monkey.png"));

    public BufferedImage bImage;
    public int frameNo;

    public MonkeyCross() {
        this(10);

        rightCount.add(5);
        rightCount.add(25);
        rightCount.add(45);
        rightCount.add(55);

    }

    /**
     * @param n How many links in the rope.
     */
    public MonkeyCross(final int n) {
        links = n;
        linkState = new int[links];
        leftCount = new Stack<Integer>();
        rightCount = new Stack<Integer>();

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice gd = ge.getDefaultScreenDevice();
        final GraphicsConfiguration gc = gd.getDefaultConfiguration();

        // Create an image that supports transparent pixels
        bImage = gc.createCompatibleImage(winWidth, winWidth);
        frameNo = 0;

    }

    /**
     * Set link m to be color c (default is 0)
     * 
     * @param m
     * @param c
     * @return
     */
    public int setLink(final int m, final int c) {
        linkState[links - m] = c;
        return c;
    }

    public int leftPush(final int n) {
        leftCount.add(n);
        return n;
    }

    public int leftPop() {
        return leftCount.pop();
    }

    public int rightPush(final int n) {
        rightCount.add(n);
        return n;
    }

    public int rightPop() {
        return rightCount.pop();
    }

    public boolean toggleSide() {
        cSide = !cSide;
        return cSide;
    }

    public int open() {
        WindowUtilities.openInJFrame(this, winWidth, winWidth);
        return 0;
    }

    public int redraw() {
        update(this.getGraphics());
        /*
         * paintComponent(bImage.getGraphics()); try {
         * javax.imageio.ImageIO.write(bImage, "jpeg", new
         * File("c:/Temp/pics/frame"+frameNo+".jpeg")); } catch (IOException e)
         * { e.printStackTrace(); } ++frameNo;
         */

        return 0;
    }

    public Color colSwitch(final int cl) {
        Color rv;

        switch (cl) {
        case -1:
            rv = Color.black;
            break;
        case -2:
            rv = Color.gray;
            break;
        case -3:
            rv = Color.white;
            break;
        default:
            rv = Color.getHSBColor(cl / 64.0f, 1.0f, 1.0f);
        }
        return rv;
    }

    public void paintMonkey(final Graphics2D g2d, final int num) {
        g2d.setPaint(colSwitch(num));
        g2d.fill(circle);
        g2d.setPaint(Color.black);
        g2d.draw(circle);
        g2d.drawImage(monkeyImg, 0, (int) (-0.8 * baseRad), (int) baseRad, (int) baseRad, this);
    }

    @Override
    public void paintComponent(final Graphics g) {
        clear(g);
        final Graphics2D g2d = (Graphics2D) g;
        AffineTransform saveTrans;

        Integer num = null;
        Iterator<Integer> ii = new ReverseListIterator<Integer>(leftCount);
        int lSize = leftCount.size();

        saveTrans = g2d.getTransform();

        g2d.translate((int) (0.10 * winWidth), (int) baseHeight);
        final float xTrans = 0.45f * baseRad;
        final float yTrans = 0.65f * baseRad;
        final float sclDif = 0.9f;

        g2d.setStroke(new BasicStroke(2));
        double ss = Math.pow(sclDif, lSize);
        g2d.scale(ss, ss);
        while (ii.hasNext()) {
            num = ii.next();
            if (num == null) {
                throw new AssertionError();
            }
            g2d.scale(1 / sclDif, 1 / sclDif);
            g2d.translate(-lSize * xTrans, -lSize * yTrans);
            paintMonkey(g2d, num);
            g2d.translate(lSize * xTrans, lSize * yTrans);

            lSize--;
        }
        g2d.setTransform(saveTrans);
        g2d.translate((int) (0.9 * winWidth - baseRad), (int) baseHeight);
        ii = new ReverseListIterator<Integer>(rightCount);
        lSize = rightCount.size();
        ss = Math.pow(sclDif, lSize);
        g2d.scale(ss, ss);
        while (ii.hasNext()) {
            num = ii.next();
            if (num == null) {
                throw new AssertionError();
            }
            g2d.scale(1 / sclDif, 1 / sclDif);
            g2d.translate(lSize * xTrans, -lSize * yTrans);
            paintMonkey(g2d, num);
            g2d.translate(-lSize * xTrans, lSize * yTrans);

            lSize--;
        }
        g2d.setTransform(saveTrans);
        final float gapLen = 0.7f * winWidth - 0.5f * baseRad;

        final Rectangle2D.Double rect = new Rectangle2D.Double(0, (int) -baseRad, (int) (0.5 * baseRad), (int) (2 * baseRad));
        g2d.translate((int) (0.15 * winWidth), (int) baseHeight);
        g2d.drawLine(1, (int) (1 - baseRad), (int) gapLen, (int) (1 - baseRad));
        g2d.fill(rect);
        g2d.translate((int) gapLen, 0);
        g2d.fill(rect);

        g2d.translate((int) -gapLen, 0);
        for (final int element : linkState) {
            g2d.translate(gapLen / (links + 1), 0);
            if (element != 0) {
                paintMonkey(g2d, element);
            }
        }
        g2d.setTransform(saveTrans);
        g2d.drawImage(cliffImg, 0, 0, winWidth, winWidth, this);
    }

    // super.paintComponent clears offscreen pixmap,
    // since we're using double buffering by default.
    protected void clear(final Graphics g) {
        super.paintComponent(g);
    }

    public static void main(final String[] args) throws InterruptedException {
        final MonkeyCross se = new MonkeyCross();
        se.open();
        se.leftCount.add(1);
        Thread.sleep(500);
        se.redraw();

        se.leftCount.add(11);
        Thread.sleep(500);
        se.redraw();

        se.leftCount.add(21);
        Thread.sleep(500);
        se.redraw();

        se.leftCount.add(31);
        Thread.sleep(500);
        se.redraw();

        se.leftCount.add(41);
        Thread.sleep(500);
        se.redraw();

        final int mk = se.leftPop();
        se.setLink(1, mk);
        Thread.sleep(500);
        se.redraw();

        se.setLink(1, 0);
        se.setLink(1, mk);
        Thread.sleep(500);
        se.redraw();

        se.setLink(1, 0);
        se.setLink(2, mk);
        Thread.sleep(500);
        se.redraw();

        se.setLink(2, 0);
        se.setLink(3, mk);
        Thread.sleep(500);
        se.redraw();

        se.setLink(3, 0);
        se.setLink(4, mk);
        Thread.sleep(500);
        se.redraw();

        se.setLink(4, 0);
        se.setLink(5, mk);
        Thread.sleep(500);
        se.redraw();

        se.setLink(5, 0);
        se.setLink(6, mk);
        Thread.sleep(500);
        se.redraw();

        se.setLink(6, 0);
        se.setLink(7, mk);
        Thread.sleep(500);
        se.redraw();

        se.setLink(7, 0);
        se.setLink(8, mk);
        Thread.sleep(500);
        se.redraw();

        se.setLink(8, 0);
        se.setLink(9, mk);
        Thread.sleep(500);
        se.redraw();

        se.setLink(9, 0);
        Thread.sleep(500);
        se.redraw();
    }

    /**
     * In Java 6 we can replace this with the builtin decreasingIterator.
     * 
     * @author quark
     */
    public static class ReverseListIterator<E> implements Iterator<E> {
        private final ListIterator<E> that;

        public ReverseListIterator(final List<E> list) {
            that = list.listIterator(list.size());
        }

        @Override
        public boolean hasNext() {
            return that.hasPrevious();
        }

        @Override
        public E next() {
            return that.previous();
        }

        @Override
        public void remove() {
            that.remove();
        }
    }
}
