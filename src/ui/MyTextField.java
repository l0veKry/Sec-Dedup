package ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;

import javax.swing.ImageIcon;
import javax.swing.JPasswordField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 * Բ�Ǿ����ı��򣬰�����ʾ���һ����ť
 * @author Administrator
 *
 */
class MyTextField extends JPasswordField{
	/**
	 * 20170302
	 */
	private static final long serialVersionUID = 1L;
	private int tran;
	private int arc;
	private int w;
	private int h;
	private Shape shape;
	private ImageIcon image;
	private char echoChar;
	private boolean enPassword;
	private Color borderColor=Color.gray;
	private Color backGroundColor=Color.white;
	
	//private boolean enablePassword;
	/**
	 * ��ʼ��һ���ı�������ı����������������
	 * @param point ��ʾ��
	 * @param w	�ı���Ŀ�
	 * @param h	�ı���ĸ�
	 * @param a	�ı���Բ�ǵİ뾶
	 * @param image	��ʾ�Ŀɵ���İ�ť�ϵ�ͼƬ
	 */
	public MyTextField(String point,int w,int h,int a,ImageIcon image) {
		// TODO Auto-generated constructor stub
		super();
		tran=150;
		arc=a;
		this.w=w;
		this.h=h;
		shape=new RoundRectangle2D.Double(0, 0, w, h , a , a);
		if(image!=null){
			image.setImage(image.getImage().getScaledInstance(h, h,Image.SCALE_SMOOTH ));
		}
        this.image=image;

		setOpaque(false);
		//���ñ߽������
        echoChar=getEchoChar();
        setEchoChar((char)0);
		setBorder(null);
		setPreferredSize(new Dimension(w, h));
		setSize(w,h);
		Border empty = new EmptyBorder(0, 10, 0, 10+h);
		CompoundBorder border = new CompoundBorder(getBorder(), empty);
		setBorder(border);
		//ʵ��ָ���ƶ����Ҳ�ʱ��ΪĬ��ָ��
		addMouseMotionListener(new MouseMotionAdapter(){
			public void mouseMoved (MouseEvent e){
				if(e.getPoint().getX()>w-h-10){
					setCursor(Cursor.getDefaultCursor());
				}else {
					setCursor(new Cursor(Cursor.TEXT_CURSOR));
				}
			}
		});
		
		//ʵ���ı������ʾ��
		setText(point);
		addFocusListener(new FocusListener() {
		     
		    @Override
		    public void focusLost(FocusEvent e) {
		        //�������Ϊ��,�����ı�
		    	//System.out.println("y");
		        if(getPassword().length==0){
		        	tran=150;
		        	setEchoChar((char)0);
		            setText(point);
		        }
		    }
		     
		    @Override
		    public void focusGained(FocusEvent e) {
		        if(new String(getPassword()).equals(point)){
		        	tran=20;
		            setText("");
		            if(enPassword)
		            	setEchoChar(echoChar);
		        }
		    }
		});
	}
	
	public boolean clickTheImage(Point point){
		if(point.x>=w-10-h&&point.x<=w-10&&point.y>=h/6&&point.y<=h*5/6){
			return true;
		}
		else {
			return false;
		}
	}
	public void setBorderColor(Color color){
		borderColor=color;
	}
	
	public void enablePassword() {
		enPassword=true;
	}
	
	public boolean contains(int x, int y) {
        return shape.contains(x, y);
    }
	public void setBackgroundColor(Color bg) {
		backGroundColor=bg;
	}
	@Override
    protected void paintComponent(Graphics g) {
		
		g.setClip(new RoundRectangle2D.Double(0, 0, w, h , arc , arc));
		Graphics2D g2 = (Graphics2D) g ;
		g2.setColor(backGroundColor);
		g2.fill(new RoundRectangle2D.Double(0, 0, w, h , arc , arc));
		super.paintComponent(g);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(borderColor);
        g2.drawRoundRect(0, 0, w-1, h-1, arc, arc);
        g2.setColor(new Color(255,255,255, tran));
        g2.fillRoundRect(0, 0, w, h, arc, arc);
        //�����ͼ��
        if(image!=null){
        	image.paintIcon(this, g2, w-10-h, 0);
        }
	}
}
