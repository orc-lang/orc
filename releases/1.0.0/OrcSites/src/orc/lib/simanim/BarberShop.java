package orc.lib.simanim;

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
import java.util.Stack;

import javax.swing.JPanel;


/** An example of drawing/filling shapes with Java2D in Java 1.2.
 *
 *  From tutorial on learning Java2D at
 *  http://www.apl.jhu.edu/~hall/java/Java2D-Tutorial.html
 *
 *  1998 Marty Hall, http://www.apl.jhu.edu/~hall/java/
 */

public class BarberShop extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int winWidth = (int)(800);
	public static Image sauceImg = Toolkit.getDefaultToolkit().getImage(
			BarberShop.class.getResource("sauce.png"));
	
	public BufferedImage bImage;
	public int frameNo;
	
	public int numBarb;
	public int numWait;
	
	public Stack<Integer> floorList=new Stack<Integer>();
	public Stack<Integer> regList=new Stack<Integer>();
	
	public boolean cashReg;
	public int barbState[];
	public int chairState[];
	public int bChairState[];
	public Image clipImg[];
	public double rot=0;

	double maxFrac=20;
	class Transition {
		double startX,startY;
		double endX,endY;
		int frac;
		int c;
		
		double getX() {
			return startX*(maxFrac-frac)/maxFrac+endX*frac/maxFrac;
		}
		double getY() {
			return startY*(maxFrac-frac)/maxFrac+endY*frac/maxFrac;
		}
	}
	
	Transition changes[];
	
	
	float baseRad = 0.05f*winWidth;
	Ellipse2D.Double circle = new Ellipse2D.Double(0, 0, baseRad, baseRad);
	Rectangle2D.Double shopBound = 
		new Rectangle2D.Double(0,0,
			0.8*winWidth,0.8*winWidth);
	public static Image bChairImg = Toolkit.getDefaultToolkit().getImage(
			BarberShop.class.getResource("barbChair.png"));
	public static Image wChairImg = Toolkit.getDefaultToolkit().getImage(
			BarberShop.class.getResource("chair.png"));
	public static Image cReg1Img = Toolkit.getDefaultToolkit().getImage(
			BarberShop.class.getResource("register1.png"));
	public static Image cReg2Img = Toolkit.getDefaultToolkit().getImage(
			BarberShop.class.getResource("register2.png"));
	public static Image scissorImg = Toolkit.getDefaultToolkit().getImage(
			BarberShop.class.getResource("scissors.png"));
	public static Image razorImg = Toolkit.getDefaultToolkit().getImage(
			BarberShop.class.getResource("razor.png"));
	public static Image clipperImg = Toolkit.getDefaultToolkit().getImage(
			BarberShop.class.getResource("clippers.png"));
	public static Image monkeyImg = Toolkit.getDefaultToolkit().getImage(
			BarberShop.class.getResource("monkey.png"));
	/**
	 * Barber can be 
	 * 	sleeping=0, 
	 * 	cutting hair=1, 
	 * 	at register=2
	 * @param bID
	 * @param state
	 * @return
	 */
	public boolean setBarberState(int bID,int state)
	{
		barbState[bID-1]=state;
		return true;
	}
	
	public int newTransition(int id) 
	{
		int ii;
		for (ii=0;ii<changes.length;++ii)
		{
			if (changes[ii]==null) break;
		}
		changes[ii]=new Transition();
		changes[ii].c = id;
		return ii;
	}
	
	public boolean endTransition(int tt)
	{
		changes[tt]=null;
		return true;
	}
	
	public boolean setTransition(int tt,int st)
	{
		changes[tt].frac=st;
		return true;
	}
	
	
	public int lineToChair(int chair,int id)
	{
		chair-=1;
		int tt=newTransition(id);
		changes[tt].startX=0.5*baseRad;
		changes[tt].startY= 2*baseRad;
		changes[tt].endX=chair*0.6*winWidth/chairState.length+0.1*winWidth+0.5*baseRad;
		changes[tt].endY=0;
		return tt;
	}
	
	public int lineToBChair(int chair,int id)
	{
		chair-=1;
		int tt=newTransition(id);
		changes[tt].startX=0.5*baseRad;
		changes[tt].startY= 2*baseRad;
		changes[tt].endX=0.2*winWidth+chair*0.4*winWidth/bChairState.length+baseRad;
		changes[tt].endY= 0.65*winWidth+0.5*baseRad;
		
		return tt;
	}
	
	public int chairToBChair(int chair,int bChair,int id)
	{
		chair-=1;
		bChair-=1;
		int tt=newTransition(id);
		changes[tt].startX= chair*0.6*winWidth/chairState.length+0.1*winWidth+0.5*baseRad;
		changes[tt].startY= 0;
		changes[tt].endX= 0.2*winWidth+bChair*0.4*winWidth/bChairState.length+baseRad;
		changes[tt].endY= 0.65*winWidth+0.5*baseRad;
		
		return tt;
	}
	
	public int bChairToLine(int chair,int id)
	{
		chair-=1;
		int tt=newTransition(id);
		changes[tt].startX=0.2*winWidth+chair*0.4*winWidth/bChairState.length+baseRad;
		changes[tt].startY= 0.65*winWidth+0.5*baseRad;
		changes[tt].endX = 0.6*winWidth;
		changes[tt].endY = 5*baseRad+baseRad*regList.size();
		return tt;
	}
	
	public int barberToReg(int id)
	{
		id-=1;
		int tt=newTransition(-(1+id));
		changes[tt].startX= 0.2*winWidth + id*0.4*winWidth/bChairState.length+baseRad;
		changes[tt].startY= 0.65*winWidth + -2.5*baseRad;
		changes[tt].endX = 0.2*winWidth + 0.45*winWidth;
		changes[tt].endY = 0.65*winWidth - 0.5*winWidth;
		return tt;
	}

	public int barberFromReg(int id)
	{
		id-=1;
		int tt=newTransition(-(1+id));
		changes[tt].startX = 0.2*winWidth + 0.45*winWidth;
		changes[tt].startY = 0.65*winWidth - 0.5*winWidth;
		changes[tt].endX= 0.2*winWidth + id*0.4*winWidth/bChairState.length+baseRad;
		changes[tt].endY= 0.65*winWidth + -2.5*baseRad;
		return tt;
	}
	
	/**
	 * If state is not 0, a customer is waiting
	 * @param chID
	 * @param state
	 * @return
	 */
	public boolean setChairState(int chID,int state)
	{
		chairState[chID-1]=state;
		return true;
	}
	
	/**
	 * If state is not 0, a customer is waiting
	 * @param chID
	 * @param state
	 * @return
	 */
	public boolean setBChairState(int chID,int state)
	{
		bChairState[chID-1]=state;
		return true;
	}
	
	/**
	 * A customer has entered and is waiting.
	 * @param state
	 * @return
	 */
	public boolean pushFloor(int state)
	{
		floorList.add(state);
		return true;
	}
	
	/**
	 * Customer's not on the floor anymore
	 * @return
	 */
	public int popFloor()
	{
		return floorList.pop();
	}
	
	/**
	 * A new customer at the cash register.
	 * @param state
	 * @return
	 */
	public boolean pushRegister(int state)
	{
		regList.add(state);
		return true;
	}
	
	/**
	 * Leave the register, leave the store.
	 * @return
	 */
	public int popRegister()
	{
		return regList.pop();
	}
	
	public boolean setReg(boolean open)
	{
		cashReg=open;
		return open;
	}
	
	public Color colSwitch(int cl)
	{
		Color rv;
	
		switch(cl) {
		case -1:
			rv=Color.black;
			break;
		case -2:
			rv=Color.gray;
			break;
		case -3:
			rv=Color.white;
			break;
		default:
			rv = Color.getHSBColor(cl/64.0f, 1.0f, 1.0f);
		}
		return rv;
	}
	
	public void paintMonkey(Graphics2D g2d,int num)
	{
		g2d.setPaint(colSwitch(num));
    	g2d.fill(circle);
    	g2d.setPaint(Color.black);
    	g2d.draw(circle);
    	g2d.drawImage(monkeyImg, 0,(int)(-0.8*baseRad),
    			(int)(baseRad), (int)(baseRad), this);
	}
	
	public BarberShop()
	{
		this(8,3);
	}
	
	/**
	 * Just your typical barber shop.
	 */
	public BarberShop(int wCount,int bCount)
	{
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		
		numBarb=bCount;
		numWait=wCount;
		
		barbState=new int[numBarb];
		chairState=new int[numWait];
		bChairState=new int[numBarb];
		clipImg=new Image[numBarb];
		changes=new Transition[40];
		
		for (int ii=0;ii<numBarb;++ii) {
			switch (ii%3) {
			case 0:
				clipImg[ii]=scissorImg;
				break;
			case 1:
				clipImg[ii]=razorImg;
				break;
			case 2:
			default:
				clipImg[ii]=clipperImg;
				break;
			}
		}
		
		// This is for recording frames
		bImage = gc.createCompatibleImage(winWidth, winWidth);
		frameNo=0;
		
		
	}
	
	public int open()
	{
		WindowUtilities.openInJFrame(this, winWidth, winWidth);
		return 0;
	}
	public int redraw()
	{
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
  
	public void paintComponent(Graphics g) 
	{
	    clear(g);
	    Graphics2D g2d = (Graphics2D)g;
	    AffineTransform saveTrans;
	    AffineTransform saveTrans2;
	    
	    rot+=Math.PI/20.0;
	    while (rot>Math.PI) rot-=2*Math.PI;
	    
	    g2d.translate(0.1*winWidth,0.1*winWidth);
	    saveTrans=g2d.getTransform();
	    g2d.draw(shopBound);
	    
	    //Cash register
	    g2d.translate(0.65*winWidth, 0.15*winWidth);
	    g2d.drawImage(cashReg?cReg2Img:cReg1Img, 
    			0,0,(int)(3*baseRad), (int)(3*baseRad), this);
	    g2d.setTransform(saveTrans);
	    
	    //Draw barbers and chairs.
	    g2d.translate(0.2*winWidth, 0.65*winWidth);
	    for (int ii = 0; ii<bChairState.length;++ii) {
	    	saveTrans2=g2d.getTransform();
	    	
	    	
	    	switch (barbState[ii]) {
	    	case 0:
	    		g2d.translate(ii*0.4*winWidth/bChairState.length+baseRad,-2.5*baseRad);
	    		paintMonkey(g2d,-(1+ii));
	    		break;
	    	case 1:
	    		g2d.translate(ii*0.4*winWidth/bChairState.length,-0.5*baseRad);
	    		paintMonkey(g2d,-(1+ii));
	    		g2d.translate(1.5*baseRad,0);
	    		g2d.rotate(rot);
	    		g2d.drawImage(clipImg[ii], 
		    			(int)(-0.5*baseRad),(int)(-0.5*baseRad),
		    			(int)(baseRad), (int)(baseRad), this);
	    		break;
	    	case 2:
	    		g2d.translate(0.45*winWidth,-0.5*winWidth);
	    		paintMonkey(g2d,-(1+ii));
	    		break;
	    	}
	    	g2d.setTransform(saveTrans2);
	    	g2d.drawImage(bChairImg, 
	    			(int)(ii*0.4*winWidth/bChairState.length),0,
	    			(int)(3*baseRad), (int)(3*baseRad), this);
	    	
	    	
	    	if (bChairState[ii]!=0) {
	    		g2d.translate(ii*0.4*winWidth/bChairState.length+baseRad,0.5*baseRad);
	    		paintMonkey(g2d,bChairState[ii]);
	    	}
	    	g2d.setTransform(saveTrans2);
	    }
	    g2d.setTransform(saveTrans);
	    
	  //Draw waiting chairs.
	    g2d.translate(0.1*winWidth, 0.0*winWidth);
	    for (int ii = 0; ii<chairState.length;++ii) {
	    	saveTrans2=g2d.getTransform();
	    	g2d.drawImage(wChairImg, 
	    			(int)(ii*0.6*winWidth/chairState.length),0,
	    			(int)(2*baseRad), (int)(2*baseRad), this);
	    	
	    	if (chairState[ii]!=0) {
	    		g2d.translate(0.5*baseRad+ii*0.6*winWidth/chairState.length,0);
	    		paintMonkey(g2d,chairState[ii]);
	    	}
	    	g2d.setTransform(saveTrans2);
	    }
	    g2d.setTransform(saveTrans);
	
	    //Waiting monkeys
	    g2d.translate(0.5*baseRad, 2*baseRad);
	    for (Integer ii : floorList) {
	    	paintMonkey(g2d,ii);
	    	g2d.translate(0,baseRad);
	    }
     	g2d.setTransform(saveTrans);

     	//Waiting monkeys
	    g2d.translate(0.6*winWidth, 5*baseRad);
	    for (Integer ii : regList) {
	    	paintMonkey(g2d,ii);
	    	g2d.translate(0,baseRad);
	    }
     	g2d.setTransform(saveTrans);
     	
     	for (int ii=0;ii<changes.length;++ii) {
     		if (changes[ii]!=null) {
     			g2d.translate(changes[ii].getX(),changes[ii].getY());
     			paintMonkey(g2d,changes[ii].c);
     			g2d.setTransform(saveTrans);
     		}
     	}

	
	}
  

	// super.paintComponent clears offscreen pixmap,
	// since we're using double buffering by default.
	protected void clear(Graphics g) {
	    super.paintComponent(g);
	}

	public static void main(String[] args) {
		BarberShop se = new BarberShop();
		se.open();
		
		se.pushFloor(10);
		se.pushFloor(20);
		se.pushFloor(50);
		se.pushRegister(5);
		se.pushRegister(15);
		
		se.setBChairState(3,10);
		se.setBarberState(3, 1);
		se.setBarberState(2, 1);
		se.setBarberState(1, 1);
		se.setReg(true);
		se.setChairState(5, 60);
		
		int tt=se.chairToBChair(4, 3, 5);
		for (int ii=0;ii<=20;++ii) {
			se.setTransition(tt,ii);
			se.redraw();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
	}
}

