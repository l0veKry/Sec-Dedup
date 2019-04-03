package ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class PanFriend extends JLabel implements MouseListener{
	/**
	 * 20160405
	 */
	private static final long serialVersionUID = 1L;
	//private String name;
	//private int status;
	private byte[] publicKey;
	private byte[] friendId;
	
	private boolean choosed;
	
	private ImageIcon headIcon;
	private JLabel headLabel;
	private JLabel statusLabel;
	
	private static MyScrollPanel origin;
	private static Color onColor=Color.white;
	private static Color clickColor=Color.white;
	static int h;
	private static Font font;
	
	static ImageIcon temp1;
	static ImageIcon temp2;
	static ImageIcon temp3;	
	static ImageIcon temp4;
	static ImageIcon temp5;
	//jar包中无法实现用静态块加载，只能这样将就，当然只加载一次是为了节省资源
	//不过当然，暂时没有头像，所以用随机头像代替
	void iniIcon(){
		if(temp1!=null)
			return;
		temp1=new ImageIcon(this.getClass().getResource("/resourse/A.png"));
		temp2=new ImageIcon(this.getClass().getResource("/resourse/C.png"));
		temp3=new ImageIcon(this.getClass().getResource("/resourse/D.png"));
		temp4=new ImageIcon(this.getClass().getResource("/resourse/E.png"));
		temp5=new ImageIcon(this.getClass().getResource("/resourse/F.png"));
		temp1.setImage(temp1.getImage().getScaledInstance(h, h,Image.SCALE_SMOOTH ));
		temp2.setImage(temp2.getImage().getScaledInstance(h, h,Image.SCALE_SMOOTH ));
		temp3.setImage(temp3.getImage().getScaledInstance(h, h,Image.SCALE_SMOOTH ));
		temp4.setImage(temp4.getImage().getScaledInstance(h, h,Image.SCALE_SMOOTH ));
		temp5.setImage(temp5.getImage().getScaledInstance(h, h,Image.SCALE_SMOOTH ));
	}
	
	public PanFriend(String name,byte[] id,int status,ImageIcon icon,byte[] key){
		super();
		if(name==null)
			return;
		friendId=id;
		//this.status=status;
		this.publicKey=key;
		//this.name=name;
		setLayout(new GridLayout(1,2));
    	setPreferredSize(new Dimension(0,30));
    	setBackground(onColor);
		setOpaque(false);
    	headLabel=new JLabel(name);
		headLabel.setVerticalTextPosition(JLabel.CENTER);
		headLabel.setHorizontalTextPosition(JLabel.RIGHT);
		headLabel.setHorizontalAlignment(JLabel.LEFT);
		headLabel.setVerticalAlignment(JLabel.CENTER);
		headLabel.setOpaque(false);
		headLabel.setFont(font);
		this.add(headLabel);
		headIcon=icon;
		if(headIcon==null){
			switch (new Random().nextInt()%5) {
				case 0:
					headIcon=temp1;
					break;
				case 1:
					headIcon=temp2;
					break;
				case 2:
					headIcon=temp3;
					break;
				case 3:
					headIcon=temp4;
					break;
				default:
					headIcon=temp5;
					break;
			}
		}
		headLabel.setIcon(headIcon);
//		if(status==1){
//			statusLabel=new JLabel("在线");
//		}else{
//			statusLabel=new JLabel("离线");
//		}
		statusLabel=new JLabel();
		//statusLabel.setVerticalTextPosition(JLabel.CENTER);
		//statusLabel.setHorizontalTextPosition(JLabel.RIGHT);
		statusLabel.setHorizontalAlignment(JLabel.RIGHT);
		statusLabel.setVerticalAlignment(JLabel.CENTER);
		statusLabel.setOpaque(false);
		statusLabel.setFont(font);
		this.add(statusLabel);
		this.setPreferredSize(new Dimension(0,h));
		this.publicKey=key;
		addMouseListener(this);
	}
	
	
	
	public static void setFriendFont(Font font){ 
		PanFriend.font=font;
	}
	
	public static void setOrigin(MyScrollPanel origin){
		PanFriend.origin=origin;
	}
	
	public byte[] getId(){
		return friendId;
	}
	
	public byte[] getKey(){
		return publicKey;
	}
	static void setColor(Color onColor,Color clickColor){
		PanFriend.onColor=onColor;
		PanFriend.clickColor=clickColor;
	}
	
	//用于分享了后的去已选
	void ini(){
		choosed=false;
		setBackground(onColor);
		setOpaque(false);
	}
		
	
	/**
	 * 对鼠标点击事件的反应，包括单击事件和双击事件
	 */
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		//双击事件设置的300的最大间隔
		if(e.getButton() != MouseEvent.BUTTON1){
			return;
		}
		if(!choosed){
			setOpaque(true);
		    setBackground(clickColor);
			choosed=true;
			origin.allChoosed.add(this);
		}else{
			setBackground(onColor);
			choosed=false;
			origin.allChoosed.remove(this);
		}
		repaint();
	}

	protected void paintComponent(Graphics g){
		int w=getWidth();
		int h=getHeight();
		RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,w,h,5,5);
		g.setClip(rect);
        super.paintComponent(g);
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	//实现鼠标移入移出的高亮效果
	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		setOpaque(true);
		repaint();
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		if(!choosed)
			setOpaque(false);
		repaint();
	}
	
	
}
