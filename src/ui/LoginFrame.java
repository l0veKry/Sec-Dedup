package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import logic.MyConnection;
import logic.MyTimeOutException;


/**
 * 登录框，用于登录
 * @author Administrator
 *
 */
public class LoginFrame extends JFrame{
	/**
	 * 20170305
	 */
	private static final long serialVersionUID = 1L;
	private MyConnection connection;
	private JPanel onPanel;
	
	private Point mouse;
	
	private MyTextField userName;
	private MyTextField userPassword;
	private MyButton login;
	private MyButton cancel;
	//用户名或密码错误提示
	private JLabel tip;
	private JLabel loginLogo;
	private JLabel signUp;
	
	
	static  ImageIcon deleteIcon;
	static  ImageIcon nameIcon;
	static  ImageIcon passwordIcon;
	static  ImageIcon loginLogoIcon;	
	{
		//本来是直接用static进行初始化，然而LoginFrame.class.getClassLoader().getResource得到路径时，整个jar还没有被完全加载，因此无法实现，只能这样来将就
		if(deleteIcon==null){
			deleteIcon=new ImageIcon(this.getClass().getResource("/resourse/删除.png"));
			nameIcon=new ImageIcon(this.getClass().getResource("/resourse/用户.png"));
			passwordIcon=new ImageIcon(this.getClass().getResource("/resourse/密码.png"));
			loginLogoIcon=new ImageIcon(this.getClass().getResource("/resourse/loginlogo.png"));
		}
		
		//deleteIcon=new ImageIcon(LoginFrame.class.getClassLoader().getResource("resourse/删除.png"));
		//nameIcon=new ImageIcon(LoginFrame.class.getClassLoader().getResource("resourse/用户.png"));
		//passwordIcon=new ImageIcon(LoginFrame.class.getClassLoader().getResource("resourse/密码.png"));
		//loginLogoIcon=new ImageIcon(LoginFrame.class.getClassLoader().getResource("resourse/loginlogo.png"));
	}
	
	//实现用原来的面板注册或登录
	static final int SIGN_IN_TYPE=0;
	static final int SIGNED_UP_TYPE=1;
	private int type = SIGN_IN_TYPE;
	
	
	public LoginFrame(){
		super();
		//setLayout(null);
		setSize(500, 400);
		setLocationRelativeTo(null);
		
		try {
			connection=new MyConnection(MyConnection.SIGN_IN);
		} catch (NumberFormatException |  IOException e ) {
			// TODO Auto-generated catch block
			//假如连接不成功，则可能是ip地址和端口不对，这时新产生一个设置窗口，销毁原来的窗口
			e.printStackTrace();
			new SetHostFrame();
			dispose();
			return;	
		} catch(MyTimeOutException e){
			//这个异常在这里不会发生
			e.printStackTrace();
		} catch(Exception e){
			e.printStackTrace();
		}
		
		//设置风格为本平台风格
		if(UIManager.getLookAndFeel().isSupportedLookAndFeel()){
			final String platform = UIManager.getSystemLookAndFeelClassName();
			// 如果当前风格与当前平台风格不同，就将风格改为平台的风格
			if (!UIManager.getLookAndFeel().getName().equals(platform)) {
				try {
					UIManager.setLookAndFeel(platform);
				} catch (Exception exception) {
					exception.printStackTrace();
				}
			}
		}
		
		
		onPanel=new JPanel(){
			/**
			 * 20160405
			 */
			private static final long serialVersionUID = 1L;

			protected void paintComponent(Graphics g){
				super.paintComponent(g);
				Paint arcRectPaint =new GradientPaint(0, 0, new Color(19,146,255),
					0, 400, new Color(188,240,255));
				Graphics2D g2=(Graphics2D)g.create(); 
				g2.setPaint(arcRectPaint);
				g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),30,30));
			}
		};
		onPanel.setLayout(null);
		add(onPanel);
		
		loginLogo=new JLabel();
		loginLogo.setIcon(loginLogoIcon);
		loginLogo.setLocation(0, 20);
		loginLogo.setSize(500,120);
		onPanel.add(loginLogo);
		
		
		Font font=new Font("微软雅黑",Font.PLAIN,20);
		userName=new MyTextField("用户名", 260, 50, 25, deleteIcon);
		userName.setLocation(140, 160);
		userName.setSize(260,50);
		userName.setFont(font);
		userName.enableInputMethods(true);
		//这是为了实现文本框的那张删除图片的点击反应
		userName.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				if(userName.clickTheImage(e.getPoint())){
					userName.setText("");
				}
			}
		});
		onPanel.add(userName);
		
		signUp=new JLabel("注册");
		signUp.setLocation(400, 160);
		signUp.setSize(50,50);
		signUp.setFont(new Font("微软雅黑",Font.PLAIN,14));
		signUp.setForeground(Color.DARK_GRAY);
		signUp.setVerticalAlignment(JLabel.CENTER);
		signUp.setHorizontalAlignment(JLabel.CENTER);
		signUp.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked (MouseEvent e){
				new UserItem("用户须知",LoginFrame.this);
			}
			
			@Override  
			 public void mouseEntered(MouseEvent e) {  
				 signUp.setFont(new Font("微软雅黑",Font.PLAIN,17));  
				 signUp.setForeground(Color.red);
			 }  
 
			 @Override  
			 public void mouseExited(MouseEvent e) {  
				 signUp.setFont(new Font("微软雅黑",Font.PLAIN,14));
				 signUp.setForeground(Color.DARK_GRAY);  
			 }
		});
		onPanel.add(signUp);
		
		
		userPassword=new MyTextField("密码", 260, 50, 25, deleteIcon);
		userPassword.setLocation(140, 230);
		userPassword.setSize(260,50);
		userPassword.setFont(font);
		userPassword.enablePassword();
		userPassword.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				if(userPassword.clickTheImage(e.getPoint())){
					userPassword.setText("");
				}
			}
		});
		//用于输完密码后直接回车就登录
		userPassword.addKeyListener(new KeyAdapter() {
			public void keyTyped (KeyEvent e){
				if(e.getKeyChar()==KeyEvent.VK_ENTER){
					if(type==SIGN_IN_TYPE){
						if(LoginFrame.this.loginIn()){
							new MainFrame(connection);
							LoginFrame.this.dispose();
						}else{
							tip.setForeground(Color.red);
							tip.setText("用户名或密码错误");
						}
					}else{
						if(LoginFrame.this.loginUp()){
							new MainFrame(connection);
							LoginFrame.this.dispose();
						}else{
							tip.setForeground(Color.red);
							tip.setText("用户名已存在");
						}
					}
				}
			}
		});
		onPanel.add(userPassword);
		
		tip=new JLabel();
		tip.setFont(new Font("微软雅黑",Font.PLAIN,15));
		tip.setSize(500, 20);
		tip.setForeground(Color.red);
		tip.setLocation(0, 290);
		tip.setHorizontalAlignment(JLabel.CENTER);
		onPanel.add(tip);
		
		font=new Font("微软雅黑",Font.PLAIN,15);
		cancel=new MyButton("取消");
		cancel.setFont(font);
		cancel.setButtonColor(new Color(200,226,251),new Color(128,208,254),new Color(241,248,252),new Color(161,211,253));
		cancel.setSize(80, 30, 20);
		cancel.setLocation(150,325);
		cancel.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				LoginFrame.this.dispose();
				System.exit(0);
			}
		});
		onPanel.add(cancel);
		login=new MyButton("登录");
		login.setFont(font);
		login.setButtonColor(new Color(200,226,251),new Color(128,208,254),new Color(241,248,252),new Color(161,211,253));
		login.setSize(80,30,20);
		login.setLocation(270,325);
		login.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				if(type==SIGN_IN_TYPE){
					if(LoginFrame.this.loginIn()){
						new MainFrame(connection);
						LoginFrame.this.dispose();
					}else{
						tip.setForeground(Color.red);
						tip.setText("用户名或密码错误");
					}
				}else{
					if(LoginFrame.this.loginUp()){
						new MainFrame(connection);
						LoginFrame.this.dispose();
					}else{
						tip.setForeground(Color.red);
						tip.setText("用户名已存在");
					}
				}
			}
		});
		onPanel.add(login);
		
		//添加的这两个监听器只是为了实现鼠标能拖动窗口
		addMouseListener(new MouseAdapter() {
			public void mousePressed (MouseEvent e){
				mouse=e.getPoint();
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged (MouseEvent e){
				Point p=LoginFrame.this.getLocation();
				LoginFrame.this.setLocation(p.x+e.getX()-mouse.x, p.y+e.getY()-mouse.y);
			}
		});
				
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//用于将登录框设成圆角矩形
		setUndecorated(true);
		com.sun.awt.AWTUtilities.setWindowShape(this, new RoundRectangle2D.Double(  
	            0.0D, 0.0D, this.getWidth(), this.getHeight(), 30.0D,  
	            30.0D)); 
		setVisible(true);
	}
	/**
	 * 返回是否登录成功
	 * @return
	 */
	private boolean loginIn() {
		// TODO Auto-generated method stub
		tip.setForeground(Color.black);
		tip.setText("登录中...");
		tip.repaint();
		login.setEnabled(false);
		
		if(connection.signIn(userName.getPassword(),userPassword.getPassword())){
			return true;
		}else {
			login.setEnabled(true);
			return false;
		}
	}
	
	/**
	 * 返回是否注册成功
	 * @return
	 */
	private boolean loginUp() {
		// TODO Auto-generated method stub
		tip.setForeground(Color.black);
		tip.setText("注册中...");
		tip.repaint();
		login.setEnabled(false);
		
		if(connection.signUp(userName.getPassword(),userPassword.getPassword())){
			JOptionPane.showMessageDialog(this,"注册成功！现在自动登录！");
			return true;
		}else {
			login.setEnabled(true);
			return false;
		}
	}
	
	void setType(int type){
		this.type=type;
		if(type==SIGN_IN_TYPE){
			login.setText("登录");
		}else{
			login.setText("注册");
		}
	}
	
	/**
	 * 这个主要是为了画出那两张用户和密码的图片
	 */
	public void paint(Graphics g){
		super.paint(g);
		nameIcon.setImage(nameIcon.getImage().getScaledInstance(50, 50,Image.SCALE_SMOOTH ));
		nameIcon.paintIcon(this,g,90,150);
		passwordIcon.setImage(passwordIcon.getImage().getScaledInstance(50, 50,Image.SCALE_SMOOTH ));
		passwordIcon.paintIcon(this, g, 90, 220);
		
	}
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new LoginFrame();
	}
}

/**
 * 设置服务器信息，只有设置正确才会出现登录框，当连接不上服务器时又会蹦出这个框
 * @author Administrator
 *
 */
class SetHostFrame extends JFrame{
	/**
	 * 20170304
	 * 20170405
	 */
	private static final long serialVersionUID = 1L;
	private MyTextField address;
	private MyTextField port;
	private MyButton sure;
	private MyButton cancel;
	private JPanel onPanel;
	private JLabel tip;
	
	private Point mouse;
	
	
	static final String IPREGEX="\\A((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}"
			+ "(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))\\z";
	SetHostFrame(){
		super();
		//setLayout(null);
		setSize(500, 400);
		setLocationRelativeTo(null);
		
		onPanel=new JPanel(){
			/**
			 * 20160405
			 */
			private static final long serialVersionUID = 1L;

			protected void paintComponent(Graphics g){
				super.paintComponent(g);
				Paint arcRectPaint =new GradientPaint(0, 0, new Color(19,146,255),
					0, 400, new Color(188,240,255));
				Graphics2D g2=(Graphics2D)g.create(); 
				g2.setPaint(arcRectPaint);
				g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),30,30));
			}
		};
		onPanel.setLayout(null);
		add(onPanel);
		
		Font font=new Font("微软雅黑",Font.PLAIN,20);
		
		tip=new JLabel("无法连接到服务器，请手动输入服务器ip地址和端口");
		tip.setLocation(0,50);
		tip.setSize(500, 100);
		tip.setFont(font);
		tip.setVerticalAlignment(JLabel.CENTER);
		tip.setHorizontalAlignment(JLabel.CENTER);
		onPanel.add(tip);
		
		address=new MyTextField("ip地址", 250, 50, 25, LoginFrame.deleteIcon);
		address.setLocation(125, 150);
		//address.setSize(260,50);
		address.setFont(font);
		//用于右边的删除按钮
		address.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				if(address.clickTheImage(e.getPoint())){
					address.setText("");
					SetHostFrame.this.sure.setEnabled(false);
				}
			}
		});
		//用于当输入的格式不对时把框变红而且按钮变灰
		address.addKeyListener(new KeyAdapter() {
			public void keyReleased (KeyEvent e){
				if(SetHostFrame.this.ipRight()){
					((MyTextField)e.getSource()).setBorderColor(Color.gray);
					((MyTextField)e.getSource()).repaint();
					if(SetHostFrame.this.portRight()){
						SetHostFrame.this.sure.setEnabled(true);
					}else {
						SetHostFrame.this.sure.setEnabled(false);
					}
				}else {
					((MyTextField)e.getSource()).setBorderColor(Color.red);
					((MyTextField)e.getSource()).repaint();
					SetHostFrame.this.sure.setEnabled(false);
				}
			}
		});
		onPanel.add(address);
		port=new MyTextField("端口", 250, 50, 25, LoginFrame.deleteIcon);
		port.setLocation(125, 220);
		//port.setSize(260,50);
		port.setFont(font);
		port.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				if(port.clickTheImage(e.getPoint())){
					port.setText("");
					SetHostFrame.this.sure.setEnabled(false);
				}
			}
		});
		//enter键的作用与点击确认效果一样
		port.addKeyListener(new KeyAdapter() {
			public void keyTyped (KeyEvent e){
				if(e.getKeyChar()==KeyEvent.VK_ENTER){
					SetHostFrame.this.ensure();
				}
			}
			@Override
			public void keyReleased(KeyEvent e){
				if(SetHostFrame.this.portRight()){
					((MyTextField)e.getSource()).setBorderColor(Color.gray);
					((MyTextField)e.getSource()).repaint();
					if(SetHostFrame.this.ipRight()){
						SetHostFrame.this.sure.setEnabled(true);
					}else {
						SetHostFrame.this.sure.setEnabled(false);
					}
				}else {
					((MyTextField)e.getSource()).setBorderColor(Color.red);
					((MyTextField)e.getSource()).repaint();
					SetHostFrame.this.sure.setEnabled(false);
				}
			}
		});
		onPanel.add(port);
		
		font=new Font("微软雅黑",Font.PLAIN,15);
		cancel=new MyButton("取消");
		cancel.setFont(font);
		//cancel.setButtonColor(new Color(90,170,232));
		cancel.setSize(80, 30, 20);
		cancel.setLocation(150,315);
		//按下取消会关闭程序
		cancel.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				SetHostFrame.this.dispose();
				System.exit(0);
			}
		});
		cancel.setButtonColor(new Color(200,226,251),new Color(128,208,254),new Color(241,248,252),new Color(161,211,253));
		onPanel.add(cancel);
		
		sure=new MyButton("确认");
		sure.setFont(font);
		sure.setSize(80,30,20);
		sure.setLocation(270,315);
		sure.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				if(SetHostFrame.this.ipRight()&&SetHostFrame.this.portRight())
					SetHostFrame.this.ensure();
			}
		});
		sure.setButtonColor(new Color(200,226,251),new Color(128,208,254),new Color(241,248,252),new Color(161,211,253));
		sure.setEnabled(false);
		onPanel.add(sure);
		onPanel.setFocusable(true);
		
		//添加的这两个监听器只是为了实现鼠标能拖动窗口
		addMouseListener(new MouseAdapter() {
			public void mousePressed (MouseEvent e){
				mouse=e.getPoint();
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged (MouseEvent e){
				Point p=SetHostFrame.this.getLocation();
				SetHostFrame.this.setLocation(p.x+e.getX()-mouse.x, p.y+e.getY()-mouse.y);
			}
		});
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//这段用于设置窗口为圆角矩形而且边框隐藏
		setUndecorated(true);
		com.sun.awt.AWTUtilities.setWindowShape(this, new RoundRectangle2D.Double(  
	            0.0D, 0.0D, this.getWidth(), this.getHeight(), 30.0D,  
	            30.0D)); 
		setVisible(true);
	}

	/**
	 * 验证端口的格式正确
	 * @return
	 */
	protected boolean portRight() {
		// TODO Auto-generated method stub
		int temp;
		try{
			temp=Integer.parseInt(new String(port.getPassword()));
		}catch(NumberFormatException e){
			return false;
		}
		if(temp>=0&&temp<=65535){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * 验证ip格式是否正确
	 * @return
	 */
	protected boolean ipRight() {
		// TODO Auto-generated method stub
		if(Pattern.compile(IPREGEX).matcher(new String(address.getPassword())).find()){
			return true;
		}else{
			return false;
		}
	}
	
	protected void ensure() {
		// TODO Auto-generated method stub
		MyConnection.setHost(new String(address.getPassword()));
		MyConnection.setPort(Integer.parseInt(new String(port.getPassword())));
		new LoginFrame();
		this.dispose();
	}
}

class UserItem extends JFrame implements ActionListener{
	/**
	 * 20160405
	 */
	private static final long serialVersionUID = 1L;
	private JPanel thisPanel;
	private JEditorPane editorPane;
	private JScrollPane scrollPane;
	
	private MyButton sure;
	private MyButton cancel;
	
	private JCheckBox box;
	private Point mouse;
	
	
	@SuppressWarnings("static-access")
	UserItem(String s,JFrame main){
		super(s);
		editorPane=new JEditorPane();
		editorPane.setEditable(false);
		try {
			editorPane.setContentType("UTF-8");
			editorPane.setPage(UserItem.class.getClassLoader().getSystemResource("resourse/useritem.html"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		scrollPane=new JScrollPane(editorPane);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUI(new MyScrollBarUI());
		scrollPane.getHorizontalScrollBar().setUI(new MyScrollBarUI());
		scrollPane.setBackground(Color.white);
		thisPanel=new JPanel();
		thisPanel.setLayout(new BorderLayout(5,5));
		thisPanel.add(scrollPane,BorderLayout.CENTER);
		thisPanel.setBackground(Color.white);
		
		Font font=new Font("微软雅黑",Font.PLAIN,15);
		sure=new MyButton("我要注册");
		sure.setFont(font);
		
		sure.setSize(150,30,20);
		sure.setLocation(300,50);
		sure.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				((LoginFrame)main).setType(LoginFrame.SIGNED_UP_TYPE);
				dispose();
			}
		});
		sure.setButtonColor(new Color(200,226,251),new Color(128,208,254),new Color(241,248,252),new Color(161,211,253));
		sure.setEnabled(false);
		
		cancel=new MyButton("我不同意");
		cancel.setFont(font);
		cancel.setSize(150, 30, 20);
		cancel.setLocation(100,50);
		//按下不同意会关闭程序
		cancel.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				System.exit(0);
			}
		});
		
		box=new JCheckBox("我已仔细阅读并同意上述条款");
		box.setSelected(false);
		box.addActionListener(this);
		box.setFont(font);
		box.setLocation(100, 20);
		box.setSize(400,20);
		box.setOpaque(false);
		box.setBorderPaintedFlat(false);
		
		JPanel buttonJPanel=new JPanel();
		buttonJPanel.setLayout(null);
		buttonJPanel.setPreferredSize(new Dimension(600,100));
		buttonJPanel.add(cancel);
		buttonJPanel.add(sure);
		buttonJPanel.add(box);
		buttonJPanel.setBackground(Color.white);
		
		thisPanel.add(buttonJPanel,BorderLayout.SOUTH);

		add(thisPanel);
		setSize(600,500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setLocationRelativeTo(null);
		
		//这段用于设置窗口为圆角矩形而且边框隐藏
		setUndecorated(true);
//		com.sun.awt.AWTUtilities.setWindowShape(this, new RoundRectangle2D.Double(  
//	            0.0D, 0.0D, this.getWidth(), this.getHeight(), 30.0D,  
//	            30.0D)); 
		setVisible(true);
		setBackground(Color.white);
		
		
		//添加的这两个监听器只是为了实现鼠标能拖动窗口
		addMouseListener(new MouseAdapter() {
			public void mousePressed (MouseEvent e){
				mouse=e.getPoint();
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged (MouseEvent e){
				Point p=UserItem.this.getLocation();
				UserItem.this.setLocation(p.x+e.getX()-mouse.x, p.y+e.getY()-mouse.y);
			}
		});
		
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(box.isSelected()){
			sure.setEnabled(true);
		}else{
			sure.setEnabled(false);
		}
	}
}
