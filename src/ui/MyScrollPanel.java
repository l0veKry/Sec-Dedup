package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashSet;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicScrollBarUI;


class MyScrollPanel extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//private int width;
	//private int height;
	private JPanelHasFather panel;
	private JScrollPane sPane;
	private int preNum;
	private int nullNum;
	private JLabel[] nullLabel;
	private int loc;
	private int deep=1;
	private PanFile fatherPan;
	
	LinkedHashSet<JLabel> allChoosed;
	
	public MyScrollPanel(int y, int w,int h,Color color) {
		// TODO Auto-generated constructor stub
		super();
		allChoosed=new LinkedHashSet<JLabel>();
		//width=w;
		//height=h;
		GridLayout layout=new GridLayout(0,y);
		layout.setHgap(5);
		layout.setVgap(5);
		panel=new JPanelHasFather(layout);
		panel.setFatherPanel(null);
		panel.setLocDeep(0, 1);
		sPane=new JScrollPane(panel){
		/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

		public void paintComponent(Graphics g){
			RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),30,30);
			g.setClip(rect);
			//Graphics2D g2 = (Graphics2D) g;
			super.paintComponent(g);
		}
		};
		//sPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
//		JViewport v=new JViewport();
//		v.setLayout(new BorderLayout());
//		v.add(new JButton("awdwd"));
//		sPane.setColumnHeader(v);
		
		Border empty = new EmptyBorder(10, 10, 10, 10);
		//CompoundBorder border = new CompoundBorder(null, empty);
		sPane.setViewportBorder(empty);
		sPane.setBorder(null);
		//除以3是因为操作系统默认滚一下走三下
		sPane.getVerticalScrollBar().setUnitIncrement((h-10-10)/y/3);
		sPane.getVerticalScrollBar().setUI(new MyScrollBarUI());
		sPane.getHorizontalScrollBar().setUI(new MyScrollBarUI());
		panel.setBackground(color);
		//panel.setBackground(new Color(255,255,255,0));
		
		//panel.setOpaque(false);
		setBackground(color);
		sPane.setBackground(color);
		//sPane.setOpaque(false);
		setOpaque(false);
		setPreferredSize(new Dimension(w,h));
		setLayout(new BorderLayout());
		super.add(sPane);
	}
	
	
	void setNullNum(int num){
		preNum=num;
		nullNum=num-1;
		nullLabel=new JLabel[num];
		for(int i=0;i<num;i++)
			nullLabel[i]=new JLabel();
	}
	
	@Override
	public Component add(Component comp){
		nullNum-=1; 
		return panel.add(comp);
		
	}
	
	@Override
	public void remove(Component comp){
		nullNum+=1; 
		panel.remove(comp);
		if(nullNum>0){
			panel.add(nullLabel[nullNum-1]);
		}
	}
	
	public Component addMore(Component comp){
		if(nullNum!=0){
			for(int i=0;i<nullNum;i++)
				panel.remove(nullLabel[i]);
		}
		if(comp!=null){
			panel.add(comp);
			nullNum-=1;
		}
		if(nullNum!=0){
			for(int i=0;i<nullNum;i++)
				panel.add(nullLabel[i]);
		}
		return panel;
	}
	
	void changePanel(JPanelHasFather panel,int loc,int deep){
		sPane.setViewportView(panel);
		this.loc=loc;
		this.deep=deep;
		this.panel=panel;
		setNullNum(preNum);
		this.requestFocus();
		//System.out.println(this.deep);
	}
	
	void setFatherPan(PanFile pan){
		this.fatherPan=pan;
	}

	PanFile getFathePan(){
		return fatherPan;
	}
	
	JPanelHasFather getPanel(){
		return panel;
	}
	int getLoc(){
		return loc;
	}
	int getDeep(){
		return deep;
	}
}


class MyScrollBarUI extends BasicScrollBarUI {

    private static final float ARC_NUMBER = 15.0f;

	private Shape increaseShape;
	private Shape decreaseShape;
    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {

        Graphics2D g2 = (Graphics2D) g;
        int w = thumbBounds.width - 1;
        int h = thumbBounds.height - 1;
        Paint oldPaint = g2.getPaint();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2.translate(thumbBounds.x, thumbBounds.y);

        Shape arcRect = new RoundRectangle2D.Float(0.0f, 0.0f, (float) w, (float) h, ARC_NUMBER, ARC_NUMBER);
        //填充滚动条矩形
        Paint arcRectPaint = null;
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            arcRectPaint = new GradientPaint(0, 0, new Color(225, 225, 225),
                    thumbBounds.width, 0, new Color(162, 162, 162));
        } else {
            arcRectPaint = new GradientPaint(0, 0, new Color(225, 225, 225),
                    0, thumbBounds.height, new Color(162, 162, 162));
        }
        g2.setPaint(arcRectPaint);
        g2.fill(arcRect);
        //画滚动条矩形边框
        g2.setColor(new Color(150, 150, 150));
        g2.draw(arcRect);
        //画滚动条矩形内圈边框
        g2.setColor(new Color(230, 230, 230));
        Rectangle bounds = arcRect.getBounds();
        g2.drawRoundRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2, (int) ARC_NUMBER, (int) ARC_NUMBER);

        g2.translate(-thumbBounds.x, -thumbBounds.y);
        g2.setPaint(oldPaint);
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {

        Graphics2D g2 = (Graphics2D) g;
        Paint foregroundRectPaint = null;
        Paint backgroupRectPaint = null;
        Paint oldPaint = g2.getPaint();
        //绘制滚动背景
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            foregroundRectPaint = new GradientPaint(0, 0, new Color(172, 172, 172),
                    trackBounds.width, 0, new Color(225, 225, 225));
            backgroupRectPaint = new GradientPaint(0, 0, new Color(216, 216, 216),
                    trackBounds.width, 0, new Color(172, 172, 172));
        } else {
            foregroundRectPaint = new GradientPaint(0, 0, new Color(172, 172, 172),
                    0, trackBounds.height, new Color(225, 225, 225));
            backgroupRectPaint = new GradientPaint(0, 0, new Color(216, 216, 216),
                    0, trackBounds.height, new Color(172, 172, 172));
        }
        g2.setPaint(backgroupRectPaint);
        g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        g2.setPaint(foregroundRectPaint);
        g2.fillRoundRect(trackBounds.x, trackBounds.y, trackBounds.width - 1, trackBounds.height - 1, (int) ARC_NUMBER, (int) ARC_NUMBER);
        g2.setColor(new Color(162, 162, 162));
        g2.drawRoundRect(trackBounds.x, trackBounds.y, trackBounds.width - 1, trackBounds.height - 1, (int) ARC_NUMBER, (int) ARC_NUMBER);

        g2.setPaint(oldPaint);

        //始终没有进入这两个判断方法，通过单独调用它们中的任意一个即刻明白这两个方法的含义
        if (trackHighlight == DECREASE_HIGHLIGHT) {
            paintDecreaseHighlight(g);
        } else if (trackHighlight == INCREASE_HIGHLIGHT) {
            paintIncreaseHighlight(g);
        }
    }


    @Override
    protected JButton createDecreaseButton(int orientation) {
        return new BasicArrowButton(orientation) {
            /**
			 * 20160405
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public void paint(Graphics g) {
            	
            	if(decreaseShape==null){
            		decreaseShape=g.getClip();
            	}
            	else {
					g.setClip(decreaseShape);
				}
            	
                GradientPaint backgroupRectPaint = null;
                if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                    backgroupRectPaint = new GradientPaint(0, 0, new Color(216, 216, 216),
                            getWidth(), 0, new Color(172, 172, 172));
                } else {
                    backgroupRectPaint = new GradientPaint(0, 0, new Color(216, 216, 216),
                            0, getHeight(), new Color(172, 172, 172));
                }
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(backgroupRectPaint);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Draw the arrow
                MyScrollBarUI.this.paintTriangle(g2, getSize(), direction);
            }
        };
    }

    public void paintTriangle(Graphics2D g2, Dimension buttonSize,
            int direction) {
        int x, y, w, h, size;

        w = buttonSize.width;
        h = buttonSize.height;
        size = Math.min((h - 4) / 3, (w - 4) / 3);
        size = Math.max(size, 2);
        x = (w - size) / 2;
        y = (h - size) / 2;

        Color oldColor = g2.getColor();
        int mid, i, j;

        j = 0;
        size = Math.max(size, 2) + 2;
        mid = (size / 2) - 1;

        g2.translate(x, y);
        g2.setColor(new Color(100,100,100));
        switch (direction) {
            case NORTH:
                for (i = 0; i < size; i++) {
                    g2.drawLine(mid - i, i, mid + i, i);
                }
                break;
            case SOUTH:
                j = 0;
                for (i = size - 1; i >= 0; i--) {
                    g2.drawLine(mid - i, j, mid + i, j);
                    j++;
                }
                break;
            case WEST:
                for (i = 0; i < size; i++) {
                    g2.drawLine(i, mid - i, i, mid + i);
                }
                break;
            case EAST:
                j = 0;
                for (i = size - 1; i >= 0; i--) {
                    g2.drawLine(j, mid - i, j, mid + i);
                    j++;
                }
                break;
        }
        g2.translate(-x, -y);
        g2.setColor(oldColor);
    }

    @Override
	protected JButton createIncreaseButton(int orientation) {

        return new BasicArrowButton(orientation) {
        	
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public void paint(Graphics g) {
            	if(increaseShape==null){
            		increaseShape=g.getClip();
            	}
            	else {
					g.setClip(increaseShape);
				}
                GradientPaint backgroupRectPaint = null;
                if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                    backgroupRectPaint = new GradientPaint(0, 0, new Color(216, 216, 216),
                            getWidth(), 0, new Color(172, 172, 172));
                } else {
                    backgroupRectPaint = new GradientPaint(0, 0, new Color(216, 216, 216),
                            0, getHeight(), new Color(172, 172, 172));
                }
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(backgroupRectPaint);
                g2.fill(increaseShape);//(0, 0, getWidth(), getHeight());
                MyScrollBarUI.this.paintTriangle(g2, getSize(), direction);
                
            }
        };
    }
}