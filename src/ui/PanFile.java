package ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import logic.MyConnection;

/**
 * 构成文件的组件，实质是个jlabel，包含了文件的许多信息，自身有个监听器用于实现移入出现某种颜色的效果
 * 自身还实现了点击某个文件夹时，能蹦到这个文件夹里的功能
 * @author Administrator
 * 版本号1.0，未来会进一步修改
 */
public class PanFile extends JLabel implements MouseListener{

	/**
	 * 1.0版本，注释添加与2017.03.14
	 */
	private static final long serialVersionUID = 1L;
	MyScrollPanel origin;
	private JPanelHasFather fatherPanel;
	JPanelHasFather childPanel;
	private int loc;
	private int deep;
	private String name;
	private byte[] key;
	private byte[] id;
	private int filenameSize;
	
	private int type;
	private static int w;
	
	private static Font font;
	private static MyConnection connection;
	//静态的初始化所有的图片
	private static int iconSize=40;
	static ImageIcon backIcon;
	private static ImageIcon compressIcon;
	private static ImageIcon excelIcon;
	private static ImageIcon exeIcon;
	private static ImageIcon folderIcon;
	private static ImageIcon javaIcon;
	private static ImageIcon movieIcon;
	private static ImageIcon musicIcon;
	private static ImageIcon otherIcon;
	private static ImageIcon pdfIcon;
	private static ImageIcon pictureIcon;
	private static ImageIcon pptIcon;
	private static ImageIcon txtIcon;
	private static ImageIcon wordIcon;
	//jar包中无法实现用静态块加载，只能这样将就，当然只加载一次是为了节省资源
	void iniIcon(){
		if(compressIcon==null){
			
			backIcon=new ImageIcon(this.getClass().getResource("/resourse/back.png"));
			compressIcon=new ImageIcon(this.getClass().getResource("/resourse/compress.png"));
			excelIcon=new ImageIcon(this.getClass().getResource("/resourse/excel.png"));
			exeIcon=new ImageIcon(this.getClass().getResource("/resourse/exe.png"));
			folderIcon=new ImageIcon(this.getClass().getResource("/resourse/folder.png"));
			javaIcon=new ImageIcon(this.getClass().getResource("/resourse/java.png"));
			movieIcon=new ImageIcon(this.getClass().getResource("/resourse/movie.png"));
			musicIcon=new ImageIcon(this.getClass().getResource("/resourse/music.png"));
			otherIcon=new ImageIcon(this.getClass().getResource("/resourse/other.png"));
			pdfIcon=new ImageIcon(this.getClass().getResource("/resourse/pdf.png"));
			pictureIcon=new ImageIcon(this.getClass().getResource("/resourse/picture.png"));
			pptIcon=new ImageIcon(this.getClass().getResource("/resourse/ppt.png"));
			txtIcon=new ImageIcon(this.getClass().getResource("/resourse/txt.png"));
			wordIcon=new ImageIcon(this.getClass().getResource("/resourse/word.png"));

			
			backIcon.setImage(backIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			compressIcon.setImage(compressIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			excelIcon.setImage(excelIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			exeIcon.setImage(exeIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			folderIcon.setImage(folderIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			javaIcon.setImage(javaIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			movieIcon.setImage(movieIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			musicIcon.setImage(musicIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			otherIcon.setImage(otherIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			pdfIcon.setImage(pdfIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			pictureIcon.setImage(pictureIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			pptIcon.setImage(pptIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			txtIcon.setImage(txtIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
			wordIcon.setImage(wordIcon.getImage().getScaledInstance(iconSize, iconSize,Image.SCALE_SMOOTH ));
		}
	}
	
	private long fileSize;
	
	private boolean choosed;
	private long clickTime;
	
	//使能选择，这里的使能是使能进入set
	private boolean canChoose=true;
	
	static private Color onColor;
	static private Color clickColor;
	
//	static MainFrame.MouseRight rightMouseListener;
	
	public static final int DIRECTORY=0;
	public static final int FILE=1;
	public static final int BACK=2;
	/**
	 * 构造方法，构造出这个文件的表示，其上的图片来自系统图标
	 * @param text 文件名
	 * @param id 文件的id，是个32大小的byte数组
	 * @param key 文件的密钥，也是32的大小
	 * @param type 文件的类型，有可能就是文件，也有可能是文件夹
	 * @param location 文件的位置
	 * @param fileSize 文件的大小，就是文件的大小以128向上取整的结果
	 * @param w 这个东西的高度
	 */
	public PanFile(String text,byte[] id,byte[] key,int type,int loc,int deep,long fileSize,int w) {
		// TODO Auto-generated constructor stub
		super(text,JLabel.CENTER);
		if(text==null&&id==null){
			return;
		}
		try {
			filenameSize=text.getBytes("utf-8").length;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String sizeString="";
		if(fileSize<1024){
			sizeString=String.valueOf(fileSize)+"B";
		}else if(fileSize <1024*1024){
			sizeString=String.valueOf(((double)(fileSize*100/1024)/100))+"KB";
		}else if(fileSize <1024*1024*1024){
			sizeString=String.valueOf(((double)(fileSize*100/1024/1024)/100))+"MB";
		}else{
			sizeString=String.valueOf(((double)(fileSize*100/1024/1024/1024))/100)+"GB";
		}
		if(type==FILE){
			setToolTipText("<html>文件名称:"+text+"<br>"+"文件大小:"+sizeString+"</html>");
		}
		else{
			setToolTipText("文件夹名称:"+text);
		}
		name=text;
		this.id=id;
		this.key=key;
		this.type=type;
		this.loc=loc;
		this.deep=deep;
		this.fileSize=fileSize;
		childPanel=null;
		setFont(font);
		setBackground(onColor);
		setOpaque(false);
		setPreferredSize(new Dimension(PanFile.w,PanFile.w));
		setVerticalTextPosition(JLabel.BOTTOM);
        setHorizontalTextPosition(JLabel.CENTER);
		addMouseListener(this);
		//使用图标的形式
		setIcon(getRightImageIcon(text));
		
//		if(rightMouseListener!=null){
//			addMouseListener(rightMouseListener);
//		}
		
		//下面都是尝试获得这个文件的当前系统图标的过程
//		File temp = null;
//		try {
//			if(text.lastIndexOf('.')!=-1){
//				temp=File.createTempFile("temp", text.substring(text.lastIndexOf('.')));
//			}
//			else {
//				if(type==DIRECTORY){
//					temp=new File(text);
//					temp.mkdir();
//				}else {
//					temp=File.createTempFile("temp", "");
//				}
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		ShellFolder shellFolder = null;
//		try {
//			shellFolder = ShellFolder.getShellFolder(temp);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        ImageIcon image = new ImageIcon(shellFolder.getIcon(true));
//        //image.setImage(image.getImage().getScaledInstance(150, 150,Image.SCALE_SMOOTH ));
//        setIcon(image);
//        temp.delete();
	}
	
	public static ImageIcon getRightImageIcon(String text){
		if(text.lastIndexOf('.')!=-1){
			if(text.endsWith(".rar")||text.endsWith(".zip")||text.endsWith(".gz")){
				return compressIcon;
			}else if(text.endsWith(".xls")||text.endsWith(".xlsx")){
				return (excelIcon);
			}else if(text.endsWith(".exe")){
				return (exeIcon);
			}else if(text.endsWith(".jar")||text.endsWith(".java")||text.endsWith(".class")){
				return (javaIcon);
			}else if(text.endsWith(".mp4")||text.endsWith(".avi")||text.endsWith(".flv")||text.endsWith(".mkv")||text.endsWith(".rmvb")){
				return (movieIcon);
			}else if(text.endsWith(".mp3")||text.endsWith(".wma")||text.endsWith(".wav")||text.endsWith(".m4a")){
				return (musicIcon);
			}else if(text.endsWith(".pdf")){
				return (pdfIcon);
			}else if(text.endsWith(".jpg")||text.endsWith(".jpeg")||text.endsWith(".png")||text.endsWith(".ico")||text.endsWith(".bmp")||text.endsWith(".gif")){
				return (pictureIcon);
			}else if(text.endsWith(".ppt")||text.endsWith(".pptx")){
				return (pptIcon);
			}else if(text.endsWith(".doc")||text.endsWith(".docx")){
				return (wordIcon);
			}else if(text.endsWith("txt")){
				return (txtIcon);
			}else {
				return (otherIcon);
			}
		}else{
			return (folderIcon);
		}
	}
	
	/**
	 * 此函数只能运行一次！！！设置字体
	 */
	public static void setLabelFont(Font font){
		PanFile.font=font;
	}
	/**
	 * 此函数只能运行一次！！！设置连接引用
	 */
	public static void setConnection(MyConnection connection){
		PanFile.connection=connection;
	}
	
	/**
	 * 此函数只能运行一次！！！设置w，就是小方块的高度
	 */
	public static void setW(int w){
		PanFile.w=w;
	}
	
//	public static void setMouseRight(MainFrame.MouseRight listener){
//		PanFile.rightMouseListener=listener;
//	}
	
	public static void setColor(Color onColor,Color clickColor){
		PanFile.onColor=onColor;
		PanFile.clickColor=clickColor;
	}
	
	/**
	 * 设置当前文件所在的panel的上一个点进来的panel，以及最开始的放置这个panel的scrollpanel
	 * @param fatherPanel
	 * @param origin
	 */
	void setFather(JPanelHasFather fatherPanel,MyScrollPanel origin){
		this.fatherPanel=fatherPanel;
		this.origin=origin;
	}
	
	void setChild(JPanelHasFather child){
		this.childPanel=child;
	}
	
	/**
	 * 改变当前目录为这个文件夹指向的目录
	 */
	void changeDirectory(){
		//假如不是文件夹的话就不能改变目录
		if(type==FILE){
			return;
		}
		//假如这个文件夹之前已经点击过了，已经获取到了相应的文件夹里的内容，就直接设置到新的panel就行了
		if(childPanel!=null){
			for(JLabel t:origin.allChoosed)
				((PanFile)t).ini();
			origin.allChoosed.removeAll(origin.allChoosed);
			origin.changePanel(childPanel,childPanel.getLoc(),childPanel.getDeep());
			if(this.getType()==BACK){
				origin.setFatherPan(null);
			}else{
				origin.setFatherPan(this);
			}
		}else{
			//否则就通过连接获取到这个位置的文件夹里的东西
			PanFile[] allFiles = null;
			try {
				allFiles=connection.requireFiles(loc,deep+1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//要多加一个返回上级的jpanel，这是在上面的地址栏还没实现的一个举动
			//为了实现页面刷新的功能，只能重载，加个fatherPanel进去，重载成了JPanelHasFather
			
			childPanel=new JPanelHasFather(fatherPanel.getLayout());
			childPanel.setBackground(fatherPanel.getBackground());
			childPanel.setFatherPanel(fatherPanel);
			childPanel.setLocDeep(loc, deep+1);
			childPanel.addMouseListener(origin.getMouseListeners()[0]);
			//将当前的新产生的这个panel展现出来
			origin.changePanel(childPanel,loc,deep+1);
			PanFile backFile=new PanFile("返回上级", null, null, DIRECTORY, loc,deep,0,w);
			backFile.enChoosed(false);
			backFile.setType(PanFile.BACK);
			backFile.setChild(fatherPanel);
			backFile.setFather(childPanel, origin);
			//可能会需要一个返回的图标
			backFile.setIcon(backIcon);
			origin.addMore(backFile);

			for(PanFile file:allFiles){
				file.setFather(childPanel, origin);
				origin.addMore(file);
			}

			//此时要移除所有已经选择了的，因为已经选择的是位于上一个panel里
			for(JLabel t:origin.allChoosed)
				((PanFile)t).ini();
			origin.allChoosed.removeAll(origin.allChoosed);
			origin.setFatherPan(this);
		}
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
		origin.requestFocus();
		if (System.currentTimeMillis()-clickTime<=300)
		{
			setBackground(onColor);
			//一旦双击就要把这个已选择设为false，因为点一下会选择上
			choosed=false;
			clickTime=System.currentTimeMillis();
			if(type==DIRECTORY||type==BACK){
				//假若这个是目录的话才能改变路径，实际上这句有点多余，因为之后会验证
				changeDirectory();
			}
			return;
		}
		if(!choosed){
			setOpaque(true);
		    setBackground(clickColor);
			choosed=true;
			if(this.id!=null&&canChoose)
				origin.allChoosed.add(this);
		}else{
			setBackground(onColor);
			choosed=false;
			if(canChoose)
				origin.allChoosed.remove(this);
		}
		clickTime=System.currentTimeMillis();
		repaint();
	}

	//用于切换了目录后的去已选
	void ini(){
		choosed=false;
		setBackground(onColor);
		setOpaque(false);
	}
	
	//设置能否选择进入set里
	void enChoosed(boolean b){
		canChoose=b;
	}
	
	protected void paintComponent(Graphics g){
		int w=getWidth();
		int h=getHeight();
		RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,w,h,20,20);
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
	
	public int getLoc() {
		return loc;
	}
	
	public int getDeep() {
		return deep;
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type=type;
	}
	
	public byte[] getKey() {
		return key;
	}

	public byte[] getId() {
		return id;
	}
	
	public long getFileSize(){
		return fileSize;
	}

	public byte getFileNameSize(){
		return (byte)filenameSize;
	}
}

class JPanelHasFather extends JPanel{
	/**
	 * 20160405
	 */
	private static final long serialVersionUID = 1L;
	private JPanelHasFather fatherPanel;
	private int loc;
	private int deep;
	
	public JPanelHasFather(LayoutManager layout) {
		// TODO Auto-generated constructor stub
		super(layout);
	}
	
	void setFatherPanel(JPanelHasFather panel){
		fatherPanel=panel;
	}
	
	JPanelHasFather getFatherPanel(){
		return fatherPanel;
	}
	
	void setLocDeep(int loc,int deep){
		this.loc=loc;
		this.deep=deep;
	}
	
	int getLoc(){
		return loc;
	}
	
	int getDeep(){
		return deep;
	}
}
