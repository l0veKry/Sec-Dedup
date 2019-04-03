package ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;

/**
 * 拥有圆角效果和按下，移入效果的button
 * @author Administrator
 *
 */
class MyButton extends JButton{

	/**
	 * 20170302
	 */
	private static final long serialVersionUID = 1L;
	private static Color buttonColor1= new Color(91, 118, 173);
	private static Color buttonColor2= new Color(125, 161, 237);
	private static Color buttonOnColor1=new Color(180, 230, 250, 255);
	private static Color buttonOnColor2= new Color(108, 135, 210, 255);
	
	private int arc;
	private boolean mouseIn;
	private Shape buttonShape;
	
	MyButton(String text){
		super(text);
		setBorderPainted(false);
		setFocusPainted(false);
		setContentAreaFilled(false);
		
		addMouseListener(new MouseAdapter() {
			 @Override  
			 public void mouseEntered(MouseEvent e) {  
				 mouseIn = true;  
				 repaint();  
			 }  
  
			 @Override  
			 public void mouseExited(MouseEvent e) {  
				 mouseIn = false;  
				 repaint();  
			 }
		});
	}
	
	/**
	 * 设置按钮的形状
	 * @param width 按钮的宽度
	 * @param height 按钮的高度
	 * @param arc 按钮的圆角的弧的宽度和高度
	 */
	protected void setSize(int width,int height,int arc){
		setSize(width, height);
		this.arc=arc;
		buttonShape=new RoundRectangle2D.Float(0, 0, width, height , arc,arc) ;
	}
	
	@Override
	public boolean contains(int x, int y) {
        return buttonShape.contains(x, y);
    }
	
	@Override  
    protected void paintComponent(Graphics g){
		Graphics2D g2=(Graphics2D)g.create();
		int h=getHeight();
		int w=getWidth();
		//透明度
		float tran=0.7f;
		if(mouseIn){
			tran=1.0f;
		}
		//设置抗锯齿打开
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		//设置混色算法
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,  
                tran));  
        //获得原始按钮的区域，然后设置一个新的区域
        Shape clip = g2.getClip();  
        g2.clip(new RoundRectangle2D.Float(0, 0, w - 1,  
                h - 1, arc, arc));  
        GradientPaint gp = new GradientPaint(0.0F, 0.0F, buttonColor1, 0.0F,  
                h, buttonColor2, true);  
        g2.setPaint(gp);  
        g2.fillRect(0, 0, w, h);  
        //鼠标移入就绘制立体效果  
        if (mouseIn) {  
            g2.clip(new RoundRectangle2D.Float(5, 2,  
                    w - 10, h / 2 - 1, arc / 2, arc / 2));  
            GradientPaint gp2 = new GradientPaint(0.0F, 0.0F, buttonOnColor1, 0.0F,  
                    h / 2, buttonOnColor2, true);  
            g2.setPaint(gp2);  
            g2.fillRect(5, 2, w - 10, h / 2);  
        }  
        //两个渐变色用于描绘按钮按下时边缘的变化
		GradientPaint p1;  
		GradientPaint p2;  
		if (getModel().isPressed()) {  
		    p1 = new GradientPaint(0, 0, new Color(0, 0, 0), 0, h - 1,  
		              new Color(100, 100, 100));  
		    p2 = new GradientPaint(0, 1, new Color(0, 0, 0, 50), 0, h - 3,  
		              new Color(255, 255, 255, 100));  
		} else {  
		    p1 = new GradientPaint(0, 0, new Color(100, 100, 100), 0, h - 1,  
		              new Color(0, 0, 0));  
		    p2 = new GradientPaint(0, 1, new Color(255, 255, 255, 100), 0,  
		              h - 3, new Color(0, 0, 0, 50));  
		}  
        g2.setClip(clip);   
        g2.setPaint(p1);  
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);  
        g2.setPaint(p2);  
        g2.drawRoundRect(1, 1, w - 3, h - 3, arc - 2, arc - 2);  
        g2.dispose();
        super.paintComponent(g);
	}
	
	
	protected void setButtonColor(Color color1,Color color2,Color onColor1,Color onColor2){
		buttonColor1=color1;
		buttonColor2=color2;
		buttonOnColor1=onColor1;
		buttonOnColor2=onColor2;
	}
}
