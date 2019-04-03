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
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Stack;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import logic.MyConnection;

class MainFrame extends JFrame{
	/**
	 * 20160405
	 */
	private static final long serialVersionUID = 1L;
	private int windowWidth=java.awt.Toolkit.getDefaultToolkit().getScreenSize().width*2/3;
	private int windowHeight=java.awt.Toolkit.getDefaultToolkit().getScreenSize().height*2/3;
	static final int fontSize=20*java.awt.Toolkit.getDefaultToolkit().getScreenSize().width/1920;
	
	private MyPanel westPanel;
	private MyPanel eastPanel;
	//南部的panel和北部的panel是在中心的panel之上的
	private MyPanel centerPanelOfAll;
	private MyScrollPanel centerPanelOfDownload;
	private MyScrollPanel centerPanelOfUpload;
	private MyScrollPanel centerPanelOfFinish;
	private MyPanel northPanel;
	private MyScrollPanel southPanel;
	
	private ButtonLabel allFiles;
	private ButtonLabel downloadingTask;
	private ButtonLabel uploadingTask;
	private ButtonLabel finishTask;
	private JPanel choosePanel;
	
	private ButtonLabel downloadFiles;
	private ButtonLabel uploadFiles;
	private ButtonLabel shareFiles;
	
	private JPanel myMessagePanel;
	private MyScrollPanel friendPanel;
	private JLabel mine;
	
	private JLabel exit;
	private JLabel shrink;
	private MyTextField addField;
	//private JLabel add;
	
	//鼠标按下时的点的位置
	private Point mouse;
	
	MyConnection connection;
	
	private ImageIcon logoIcon=new ImageIcon(this.getClass().getResource("/resourse/logo.png"));
	private ImageIcon logoutIcon=new ImageIcon(this.getClass().getResource("/resourse/logout.png"));
	private ImageIcon addIcon=new ImageIcon(this.getClass().getResource("/resourse/add.png"));
	private ImageIcon shrinkIcon=new ImageIcon(this.getClass().getResource("/resourse/shrink.png"));
	
	/**
	 * 所有的任务的线程的栈
	 */
	private Stack<Thread> allThread=new Stack<Thread>();
	private Thread refreshThread=null;
	
	
	public MainFrame(MyConnection connection) {
		// TODO Auto-generated constructor stub
		super();
		this.connection=connection;
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
		
		//主要的组件的添加
		setSize(windowWidth,windowHeight);
		getContentPane().setBackground(Color.white);
		setLayout(new BorderLayout(20,20));
		//为了画logo，只能再重载一次
		westPanel=new MyPanel(windowWidth/5,windowHeight, 30, new Color(120,199,255), new Color(26,82,249)){
			/**
			 * 20160405
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void paintComponent(Graphics g){
				super.paintComponent(g);
				logoIcon.paintIcon(this,g,0,0);
			}
		};
		eastPanel=new MyPanel(windowWidth/5,windowHeight, 30, new Color(208,240,255),new Color(44,110,213));
		northPanel=new MyPanel(windowWidth*3/5,windowHeight/8, 30, new Color(119,197,255),new Color(208,240,255));
		northPanel.setChangeColorPoint(windowWidth*3/5, 0);
		southPanel=new MyScrollPanel(6,windowWidth*3/5,windowHeight*7/8-10, new Color(237,245,255));
		
		centerPanelOfAll=new MyPanel(windowWidth*3/5, windowHeight, 30, new Color(255,255,255,0),new Color(255,255,255,0));
		centerPanelOfDownload=new MyScrollPanel(1,windowWidth*3/5, windowHeight, new Color(237,245,255));
		centerPanelOfUpload=new MyScrollPanel(1,windowWidth*3/5, windowHeight, new Color(237,245,255));
		centerPanelOfFinish=new MyScrollPanel(1,windowWidth*3/5, windowHeight, new Color(237,245,255));
		Task.y=windowHeight/10;
		centerPanelOfDownload.setNullNum(10);
		centerPanelOfUpload.setNullNum(10);
		centerPanelOfFinish.setNullNum(10);
		
		add(westPanel,BorderLayout.WEST);
		add(eastPanel,BorderLayout.EAST);
		add(centerPanelOfAll,BorderLayout.CENTER);
		
		centerPanelOfAll.setLayout(new BorderLayout(10,10));
		centerPanelOfAll.add(northPanel,BorderLayout.NORTH);
		centerPanelOfAll.add(southPanel,BorderLayout.SOUTH);
		choosePanel=centerPanelOfAll;
		
		logoIcon.setImage(logoIcon.getImage().getScaledInstance(windowWidth/5, windowHeight/8,Image.SCALE_SMOOTH ));
		
		//左边的三个选择的选项，切换三个面板
		WestListener westListener=new WestListener();
		allFiles=new ButtonLabel(0, windowHeight/8,windowWidth/5,windowHeight/12, fontSize,"所有文件", new Color(255,255,255,128));
		allFiles.setName("allFiles");
		allFiles.addMouseListener(westListener);
		allFiles.iniChoosed();
		downloadingTask=new ButtonLabel(0, windowHeight/8+windowHeight/12, windowWidth/5,windowHeight/12, fontSize, "下载中", new Color(255,255,255,128));
		downloadingTask.setName("downloadingTask");
		downloadingTask.addMouseListener(westListener);
		uploadingTask=new ButtonLabel(0, windowHeight/8+windowHeight*2/12, windowWidth/5,windowHeight/12, fontSize, "上传中", new Color(255,255,255,128));
		uploadingTask.setName("uploadingTask");
		uploadingTask.addMouseListener(westListener);
		finishTask=new ButtonLabel(0, windowHeight/8+windowHeight*3/12, windowWidth/5,windowHeight/12, fontSize, "已完成", new Color(255,255,255,128));
		finishTask.setName("finishTask");
		finishTask.addMouseListener(westListener);
		
		westPanel.setLayout(null);
		westPanel.add(allFiles);
		westPanel.add(downloadingTask);
		westPanel.add(uploadingTask);
		westPanel.add(finishTask);
		westPanel.validate();
		
		//上面的三个按钮
		downloadFiles=new ButtonLabel(30,0,windowHeight/8,windowHeight/8,fontSize,"下载文件",new Color(255,255,255,128));
		uploadFiles=new ButtonLabel(30+windowHeight/8,0,windowHeight/8,windowHeight/8,fontSize,"上传文件",new Color(255,255,255,128));
		shareFiles=new ButtonLabel(30+windowHeight*2/8,0,windowHeight/8,windowHeight/8,fontSize,"分享文件",new Color(255,255,255,128));
		downloadFiles.addMouseListener(new DownloadListener());
		uploadFiles.addMouseListener(new UploadListener());
		shareFiles.addMouseListener(new shareListener());
		northPanel.setLayout(null);
		northPanel.add(downloadFiles);
		northPanel.add(uploadFiles);
		northPanel.add(shareFiles);
		
		
		//右边的好友界面，等待后期基本功能调通了后继续进行完善
		eastPanel.setLayout(new BorderLayout(10, 10));
		eastPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		myMessagePanel=new JPanel();
		myMessagePanel.setOpaque(false);
		myMessagePanel.setPreferredSize(new Dimension(windowWidth/5-20,windowHeight/6));
		myMessagePanel.setLayout(null);
		mine=new JLabel(connection.getName());
		
		mine.setVerticalTextPosition(JLabel.CENTER);
		mine.setHorizontalTextPosition(JLabel.RIGHT);
		mine.setHorizontalAlignment(JLabel.CENTER);
		mine.setFont(new Font("微软雅黑", Font.BOLD, fontSize));
		mine.setSize(windowWidth/5-50,windowHeight/8-30);
		mine.setLocation(0, 30);
		myMessagePanel.add(mine);

		addField=new MyTextField("输入好友名", windowWidth/5-20, windowHeight/6-windowHeight/8, 15, addIcon);
		addField.setLocation(0, windowHeight/8);
		addField.setFont(new Font("微软雅黑",Font.PLAIN,fontSize-5));
		addField.setSize(windowWidth/5-20,windowHeight/6-windowHeight/8);
		addField.enableInputMethods(true);
		addField.setFont(new Font("微软雅黑",Font.PLAIN,fontSize-5));
		addField.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				if(addField.clickTheImage(e.getPoint())){
					addFriend();
				}
			}
		});
		//用于输完密码后直接回车就登录
		addField.addKeyListener(new KeyAdapter() {
			public void keyTyped (KeyEvent e){
				if(e.getKeyChar()==KeyEvent.VK_ENTER){
					addFriend();
				}
			}
		});
		addField.setBackgroundColor(new Color(237,245,255));
		myMessagePanel.add(addField);
		
		//退出的按钮
		logoutIcon.setImage(logoutIcon.getImage().getScaledInstance(30, 30,Image.SCALE_SMOOTH ));
		exit=new JLabel();
		exit.setPreferredSize(new Dimension(30, 30));
		exit.setSize(30, 30);
		exit.setLocation(windowWidth/5-50, 0);
		exit.setOpaque(false);
		exit.setIcon(logoutIcon);
		//exit.setBackground(Color.red);
		exit.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e){
				//没有关闭各个正在下载的进程，强制退出，期待后期优化
				MainFrame.this.connection.signOut();
				System.exit(0);
			}
		});
		myMessagePanel.add(exit);
		//最小化的按钮
		shrinkIcon.setImage(shrinkIcon.getImage().getScaledInstance(30, 30,Image.SCALE_SMOOTH ));
		shrink=new JLabel();
		shrink.setPreferredSize(new Dimension(30, 30));
		shrink.setSize(30, 30);
		shrink.setLocation(windowWidth/5-90, 0);
		shrink.setOpaque(false);
		shrink.setIcon(shrinkIcon);
		shrink.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e){
				setExtendedState(JFrame.ICONIFIED);
			}
		});
		myMessagePanel.add(shrink);
	
		eastPanel.add(myMessagePanel,BorderLayout.NORTH);
		friendPanel=new MyScrollPanel(1, windowWidth/5-20,windowHeight*5/6, new Color(237,245,255));
		eastPanel.add(friendPanel,BorderLayout.CENTER);
		
		//添加的这两个监听器只是为了实现鼠标能拖动窗口
		addMouseListener(new MouseAdapter() {
			public void mousePressed (MouseEvent e){
				mouse=e.getPoint();
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged (MouseEvent e){
				Point p=MainFrame.this.getLocation();
				MainFrame.this.setLocation(p.x+e.getX()-mouse.x, p.y+e.getY()-mouse.y);
			}
		});
		
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//实现了无窗口的模式和圆角矩形的frame
		setUndecorated(true);
		//透明度为1是为了防止鼠标穿透，鼠标点到缝隙时不会点到背后的桌面，如果想点到后面，可以设置为0
		setBackground(new Color(255,255,255,1));
		com.sun.awt.AWTUtilities.setWindowShape(this, new RoundRectangle2D.Double(  
	            0.0D, 0.0D, this.getWidth(), this.getHeight(), 30.0D,  
	            30.0D)); 
		setVisible(true);
		
		//这一大堆东西是为了实现所有文件的panel里的东西的第一次加载
		PanFile.setLabelFont(new Font("微软雅黑",Font.PLAIN,fontSize-4));
		PanFile.setConnection(connection);
		//PanFile.setMouseRight(new MouseRight());
		PanFile.setW((windowHeight*7/8-10-20-5*5)/6);
		PanFile.setColor(new Color(209,237,252), new Color(164,217,253));
		//仅仅为了加载图片
		new PanFile(null, null, null, 0, 0, 0, 0, 0).iniIcon();
		PanFile[] allFiles = null;
		southPanel.setNullNum(36);
		try {
			allFiles = connection.requireFiles(0,1);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(allFiles!=null){
			for(PanFile file:allFiles){
				file.setFather(southPanel.getPanel(), southPanel);
				southPanel.addMore(file);
			}
		}
		//实现右键菜单
		southPanel.addMouseListener(new MouseAdapter(){
			lis listen=new lis();
	        @Override
	        public void mouseReleased(MouseEvent e)
	        {
	        	if (e.getButton() == MouseEvent.BUTTON3){
					JPopupMenu textMenu = new JPopupMenu();
					JPopupMenu.setDefaultLightWeightPopupEnabled(true);
					JMenuItem refresh = new JMenuItem("刷新");
					JMenuItem paste = new JMenuItem("粘贴");
					refresh.addActionListener(listen);
					refresh.setActionCommand("refresh");
					paste.addActionListener(listen);
					paste.setActionCommand("paste");
					textMenu.add(refresh);
					//textMenu.add(paste);
					textMenu.show(e.getComponent(), e.getX(), e.getY());
				 }
	        }
	        //为了实现右键点击能够刷新，使用了内部类的内部类
	        class lis implements ActionListener{
	        	public void actionPerformed (ActionEvent e ){
	        		if(e.getActionCommand()=="refresh"){
	        			MainFrame.this.refreashDir();
	        		}else{
	        			
	        		}
	        		
	        	}
	        }
		});
		//这个监听器必须要在上层的panel上才能实现右键菜单，而右键又要能访问到MainFrame，因此要这样做
		southPanel.getPanel().addMouseListener(southPanel.getMouseListeners()[0]);
		
		//实现按ctrl+d或delete或backspace实现删除
		southPanel.addKeyListener(new KeyAdapter() {

			public void keyReleased (KeyEvent e){
				if((e.getModifiers() == KeyEvent.CTRL_MASK && e.getKeyCode() == 'C')||e.getKeyCode()==KeyEvent.VK_DELETE||e.getKeyCode()==KeyEvent.VK_BACK_SPACE){
					
					if(southPanel.allChoosed.size()==0)
						return;
					if(JOptionPane.showConfirmDialog(null, "确认删除这些文件吗?", "警告", JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION){
						return;
					}
	        		if(refreshThread!=null){
	        			try {
							refreshThread.join();
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
	        		}
					for(JLabel t:southPanel.allChoosed){
						PanFile file=(PanFile)t;
						MainFrame.this.connection.deleteFile(file.getLoc(), file.getDeep());
					}
					MainFrame.this.refreashDir();
					
				}
			}
		});
		
		//下面这几个requestFocus都是为了保证点击到任何地方southPanel都能获取到焦点，从而能对按键事件进行反应
		southPanel.requestFocus();
		this.addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				southPanel.requestFocus();
			}
		});
		southPanel.getPanel().addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				southPanel.requestFocus();
			}
		});
		
		friendPanel.getPanel().addMouseListener(new MouseAdapter() {
			public void mouseClicked (MouseEvent e){
				southPanel.requestFocus();
			}
		});
		
		friendPanel.setNullNum(15);
		ImageIcon temp=new ImageIcon(PanFile.class.getClassLoader().getResource("resourse/B.png"));
		temp.setImage(temp.getImage().getScaledInstance(windowHeight/8-30, windowHeight/8-30,Image.SCALE_SMOOTH ));
		mine.setIcon(temp);
		
		//加载好友列表
		PanFriend.setColor(new Color(209,237,252), new Color(164,217,253));
		PanFriend.setFriendFont(new Font("Times New Roman",Font.BOLD,fontSize-5));
		PanFriend.h=(windowHeight*5/6-20)/15-5;
		//仅仅为了加载图片
		new PanFriend(null,null,0,null,null).iniIcon();
		PanFriend.setOrigin(friendPanel);
		PanFriend[] friends = null;
		try {
			friends = connection.requireFriends();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(friends!=null){
			for(PanFriend friend:friends){
				friendPanel.addMore(friend);
			}
		}
	
		
	}
	
	void refreashDir(){
		PanFile[] allFiles = null;
		try {
			allFiles = connection.requireFiles(southPanel.getLoc(),southPanel.getDeep());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		JPanelHasFather newPanel =new JPanelHasFather(southPanel.getPanel().getLayout());
		newPanel.setBackground(southPanel.getPanel().getBackground());
		newPanel.setLocDeep(southPanel.getPanel().getLoc(), southPanel.getPanel().getDeep());
		newPanel.setFatherPanel(southPanel.getPanel().getFatherPanel());
		newPanel.addMouseListener(southPanel.getMouseListeners()[0]);
		if(southPanel.getFathePan()!=null)
			southPanel.getFathePan().setChild(newPanel);
		PanFile backFile=null;
		if(southPanel.getPanel().getFatherPanel()!=null){
			backFile=new PanFile("返回上级", null, null, PanFile.DIRECTORY, southPanel.getPanel().getLoc(),southPanel.getPanel().getDeep(),0,0);
			backFile.setChild(southPanel.getPanel().getFatherPanel());
			backFile.setFather(newPanel, southPanel);
			//可能会需要一个返回的图标
			backFile.setIcon(PanFile.backIcon);
			southPanel.changePanel(newPanel, southPanel.getLoc(), southPanel.getDeep());
			southPanel.addMore(backFile);
		}else{
			southPanel.changePanel(newPanel, southPanel.getLoc(), southPanel.getDeep());
		}
		
		for(PanFile file:allFiles){
			file.setFather(southPanel.getPanel(), southPanel);
			southPanel.addMore(file);
		}
		
		southPanel.allChoosed.removeAll(southPanel.allChoosed);
		southPanel.validate();
		//southPanel.updateUI();
	}
	
	void download(String dir){
		//这样是为了防止出现点到盘符上最后有分割符而点到文件夹上没有
		String direction;
		if(!dir.endsWith(System.getProperty("file.separator"))){
			direction=dir+System.getProperty("file.separator");
		}else{
			direction=dir;
		}
		System.out.println("下载到"+direction);
		//各种异常以后有时间了都要处理掉
		for(JLabel t:southPanel.allChoosed){
			//假如选择的是个文件夹的话，是要递归下载这个文件夹里的所有东西的
			PanFile file=(PanFile)t;
			if(file.getType()==PanFile.DIRECTORY){
				PanFile[] allFiles = null;
				try {
					allFiles = connection.requireFiles(file.getLoc(),file.getDeep()+1);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for(PanFile file2:allFiles){
					//目录加上这个文件名进行下载
					download(direction+file2.getName());
				}
			}
			else{
				Task task=new Task(file.getName(), file.getFileSize(), Task.DOWNLOAD,windowWidth*3/5-20);
				centerPanelOfDownload.addMore(task);
				//会新开启一条线程进行下载
				Thread thread=new Thread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						connection.downloadFile(file.getId(),file.getKey(),file.getFileSize(),direction+file.getName(),task);
						centerPanelOfDownload.remove(task);
						centerPanelOfFinish.addMore(task);
						centerPanelOfDownload.validate();
						centerPanelOfFinish.validate();
					}
				});
				thread.start();
				allThread.push(thread);
			}
		}
	}
	
	void upload(File[] files,int loc,int deep){
		for(File file:files){
			if(file.isDirectory()){
				//假如选择的是文件夹的会上传这个文件夹里的所有东西，并且还会在云盘里创建一个目录
				try {
					upload(file.listFiles(), connection.createDirectory(file.getName().getBytes("utf-8"), loc,deep),deep+1);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				Task task=new Task(file.getName(), file.length(), Task.UPLOAD,windowWidth*3/5-20);
				centerPanelOfUpload.addMore(task);
				Thread thread=new Thread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if(connection.uploadFile(file, loc,deep,task)){
							centerPanelOfUpload.remove(task);
							centerPanelOfFinish.addMore(task);
							centerPanelOfUpload.validate();
							centerPanelOfFinish.validate();
						};
					}
				});
				thread.start();
				allThread.push(thread);
			}
		}
	}
	
	//获得一个用来分享的文件树

	
	boolean shareFiles(){
		//保证分享文件时，输入输出流没有被定时更新占据住
		if(refreshThread!=null){
			try {
				refreshThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(southPanel.allChoosed.size()==0||friendPanel.allChoosed.size()==0)
			return false;
		PanFile[] files=new PanFile[southPanel.allChoosed.size()];
		PanFriend[] friends=new PanFriend[friendPanel.allChoosed.size()];
		boolean ret=connection.shareFiles(southPanel.allChoosed.toArray(files),friendPanel.allChoosed.toArray(friends));
		//去掉所有已选择的
		for(JLabel t:southPanel.allChoosed)
			((PanFile)t).ini();
		southPanel.allChoosed.removeAll(southPanel.allChoosed);
		for(JLabel t:friendPanel.allChoosed)
			((PanFriend)t).ini();
		friendPanel.allChoosed.removeAll(friendPanel.allChoosed);
		return ret;
	}
	
	void addFriend(){
		//保证添加好友时，输入输出流没有被定时更新占据住
		if(refreshThread!=null){
			try {
				refreshThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		PanFriend friend=connection.addFriend(addField.getPassword());
		if(friend!=null){
			friendPanel.addMore(friend);
			friendPanel.validate();
			JOptionPane.showMessageDialog(this,"添加好友成功！");
		}else{
			JOptionPane.showMessageDialog(this,"好友已添加或不存在！");
		}
	}
	
	
	//怎么都无法去除在文件或文件夹右键后移动鼠标造成的闪烁和图标消失问题，遂废弃该方式，改用键盘执行删除
//	class MouseRight extends MouseAdapter{
//		AL listen=new AL();
//		Object source;
//        @Override
//        public void mouseReleased(MouseEvent e)
//        {
//        	source=e.getSource();
//        	if (e.getButton() == MouseEvent.BUTTON3){
//				JPopupMenu textMenu = new JPopupMenu();
//				JPopupMenu.setDefaultLightWeightPopupEnabled(false);
//				JMenuItem refresh = new JMenuItem("删除");
//				JMenuItem cut = new JMenuItem("剪切");
//				refresh.addActionListener(listen);
//				refresh.setActionCommand("delete");
//				cut.addActionListener(listen);
//				cut.setActionCommand("cut");
//				textMenu.add(refresh);
//				//textMenu.add(paste);
//				//试图show在上一层的panel上解决出现闪烁或组件消失的问题，然而并没有解决
//				textMenu.show(southPanel.getPanel(), e.getX()+((PanFile)source).getX(), ((PanFile)source).getY()+e.getY());
//        	}
//        }
//        class AL implements ActionListener{
//        	public void actionPerformed (ActionEvent e ){
//
//        		PanFile file=(PanFile)source;
//        		if(refreshThread!=null){
//        			try {
//						refreshThread.join();
//					} catch (InterruptedException e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//        		}
//        		if(e.getActionCommand()=="delete"&&JOptionPane.showConfirmDialog(null, "确认删除该文件吗?", "警告", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
//        			connection.deleteFile(file.getLoc(), file.getDeep());
//        			MainFrame.this.refreashDir();
//        		}else{
//        			
//        		}
//        		
//        	}
//		}
//	}
	
	/**
	 * 西边几个选项的监听器，实现了当鼠标点击一下后能维持变亮的状态，而且切换到相应的页面
	 * @author Administrator
	 *
	 */
	class WestListener extends MouseAdapter{
		public void mouseClicked (MouseEvent e){
			ButtonLabel tButtonLabel=((ButtonLabel)e.getSource());
			switch (tButtonLabel.getName()) {
				case "allFiles":
					allFiles.setChoosed(true);
					downloadingTask.setChoosed(false);
					uploadingTask.setChoosed(false);
					finishTask.setChoosed(false);
					if(choosePanel!=centerPanelOfAll){
						remove(choosePanel);
						choosePanel=centerPanelOfAll;
						add(choosePanel,BorderLayout.CENTER);
					}
					break;
				case "downloadingTask":
					allFiles.setChoosed(false);
					downloadingTask.setChoosed(true);
					uploadingTask.setChoosed(false);
					finishTask.setChoosed(false);	
					if(choosePanel!=centerPanelOfDownload){
						remove(choosePanel);
						choosePanel=centerPanelOfDownload;
						add(choosePanel,BorderLayout.CENTER);
					}
					break;
				case "uploadingTask":
					allFiles.setChoosed(false);
					downloadingTask.setChoosed(false);
					uploadingTask.setChoosed(true);
					finishTask.setChoosed(false);	
					if(choosePanel!=centerPanelOfUpload){
						remove(choosePanel);
						choosePanel=centerPanelOfUpload;
						add(choosePanel,BorderLayout.CENTER);
					}
					break;
				case "finishTask":
					allFiles.setChoosed(false);
					downloadingTask.setChoosed(false);
					uploadingTask.setChoosed(false);
					finishTask.setChoosed(true);	
					if(choosePanel!=centerPanelOfFinish){
						remove(choosePanel);
						choosePanel=centerPanelOfFinish;
						add(choosePanel,BorderLayout.CENTER);
					}
					break;
				default:
					break;
			}
			validate();   //提示frame出现了组件的添加操作，没有这句面板切换不出来
			MainFrame.this.repaint();
		}
		
	}
	class DownloadListener extends MouseAdapter{
		
		public void mouseClicked (MouseEvent e){
			//refreashDir();
			if(southPanel.allChoosed.size()==0)
				return;
			JFileChooser chooser=new JFileChooser("C:\\");
			chooser.changeToParentDirectory();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY );
			chooser.showDialog(null,"选择下载到");
			if(chooser.getSelectedFile()!=null)
				download(chooser.getSelectedFile().getAbsolutePath());
		}
	}
	
	class shareListener extends MouseAdapter{
			
			public void mouseClicked (MouseEvent e){
				//refreashDir();
				if(southPanel.allChoosed.size()==0||friendPanel.allChoosed.size()==0)
					return;
				if(shareFiles()){
					JOptionPane.showMessageDialog(MainFrame.this,"分享文件成功！");
				}else{
					JOptionPane.showMessageDialog(MainFrame.this,"分享文件失败！");
				}
			}
		}
	
	class UploadListener extends MouseAdapter{
		
		public void mouseClicked (MouseEvent e){
			JFileChooser chooser=new JFileChooser("C:\\");
			chooser.changeToParentDirectory();
			//chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY );
			chooser.setMultiSelectionEnabled(true);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.addChoosableFileFilter(new FileFilterOfDocument());
			chooser.addChoosableFileFilter(new FileFilterOfMovie());
			chooser.addChoosableFileFilter(new FileFilterOfMusic());
			chooser.addChoosableFileFilter(new FileFilterOfPicture());
			chooser.showDialog(null,"选择文件上传");
			if(chooser.getSelectedFiles()!=null){
				upload(chooser.getSelectedFiles(),southPanel.getLoc(),southPanel.getDeep());
				//用来实现列表的更新
				refreshThread=new Thread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						try {
							//保证所有的线程都建立起来了
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						while(!allThread.isEmpty()){
							try {
								allThread.pop().join();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						try {
							//保证数据写进数据库
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						refreashDir();
						refreshThread=null;
					}
				});
				refreshThread.start();
			}
		}
	}
}

/**
 * 拥有可选择的渐变色和圆角矩形的panel
 * @author Administrator
 *
 */
class MyPanel extends JPanel{
	/**
	 * 20170314
	 */
	private static final long serialVersionUID = 1L;
	private int width;
	private int height;
	private int arc;
	private Color color1;
	private Color color2;
	private int changeColorX;
	private int changeColorY;
	
	/**
	 * 构造方法
	 * @param w 宽度
	 * @param h 高度
	 * @param a 圆角矩形的弧形的长度
	 * @param color1 渐变色的初始颜色，也是点（0，0）的颜色
	 * @param color2 渐变色的终止颜色,可以选择这个终止点的位置，默认是（0，h）的颜色
	 */
	MyPanel(int w,int h,int a,Color color1,Color color2){
		super();
		setPreferredSize(new Dimension(w,h));
		setBackground(new Color(255,255,255,0));
		width=w;
		height=h;
		arc=a;
		this.color1=color1;
		this.color2=color2;
		changeColorX=0;
		changeColorY=h;
		setOpaque(false);
	}
	
	/**
	 * 修改终止点的颜色，x，y是其终止坐标
	 * @param x 
	 * @param y
	 */
	void setChangeColorPoint(int x,int y){
		changeColorX=x;
		changeColorY=y;
		return;
	}
	
	protected void paintComponent(Graphics g){
		RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),arc,arc);
		g.setClip(rect);
		Graphics2D g2 = (Graphics2D) g;
		super.paintComponent(g2);
		//抗锯齿设置
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	                RenderingHints.VALUE_ANTIALIAS_ON);
		rect = new RoundRectangle2D.Double(0,0,width,height,arc,arc);
		
		Paint arcRectPaint =new GradientPaint(0, 0, color1,
                    changeColorX, changeColorY, color2);
		//g2.translate(0,0);
		setOpaque(true);
		g2.setPaint(arcRectPaint);
		g2.fill(rect);
		//g2.fillRect(0, 0, 100, 100);
	}
}


/**
 * 用来当做按钮的JLabel，附带少量效果,可添加图片，默认字体在下居中显示
 * @author Administrator
 *
 */
class ButtonLabel extends JLabel implements MouseListener{
	/**
	 * 20170306
	 */
	private static final long serialVersionUID = 1L;
	private Font labelFont;
	//是否被选择，这个变量引入的目的主要是为了保证当选了这个label时能保持常亮，而选了别人时会立马变暗
	private boolean isChoosed;

	/**
	 * 构造函数，要放在layout为null的panel中
	 * @param x	label的左上角的坐标x
	 * @param y	label的左上角的坐标y
	 * @param w	label的宽度
	 * @param h label的高度
	 * @param fontSize label上的字体大小
	 * @param text label上的文字信息
	 * @param color label的颜色
	 */
	public ButtonLabel(int x,int y,int w,int h,int fontSize,String text,Color color) {
		// TODO Auto-generated constructor stub
		super(text,JLabel.CENTER);
		labelFont=new Font("微软雅黑", Font.BOLD, fontSize);
		setFont(labelFont);
		setOpaque(false);
		setBackground(color);
		setVerticalTextPosition(JLabel.BOTTOM);
        setHorizontalTextPosition(JLabel.CENTER);
		setBounds(x,y,w,h);
		addMouseListener(this);
	}
	//设置这个label是否被选择，假如上次选了它，下次选了别人就要将它变暗
	void setChoosed(boolean isChoosed) {
		if(this.isChoosed&&!isChoosed){
//			color=color.darker();
//			setBackground(color);
			setOpaque(false);
		}
		this.isChoosed = isChoosed;
	}
	
	void iniChoosed(){
//		color=color.brighter().brighter();
//		setBackground(color);
		setOpaque(true);
		this.isChoosed=true;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
//		color=color.darker();
//		setBackground(color);
		setOpaque(false);
		ButtonLabel.this.repaint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
//		color=color.brighter();
//		setBackground(color);
		setOpaque(true);
		ButtonLabel.this.repaint();
	}

	//下面两个函数就实现了移入鼠标颜色变亮，移出变暗的效果
	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		if(!isChoosed){
//			color=color.brighter();
//			ButtonLabel.this.setBackground(color);
			ButtonLabel.this.setOpaque(true);
			ButtonLabel.this.repaint();
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		if(!isChoosed){
//			color=color.darker();
//			setBackground(color);
			ButtonLabel.this.setOpaque(false);
			ButtonLabel.this.repaint();
		}
	}
}