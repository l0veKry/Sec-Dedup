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
 * �����ļ��������ʵ���Ǹ�jlabel���������ļ��������Ϣ�������и�����������ʵ���������ĳ����ɫ��Ч��
 * ����ʵ���˵��ĳ���ļ���ʱ���ܱĵ�����ļ�����Ĺ���
 * @author Administrator
 * �汾��1.0��δ�����һ���޸�
 */
public class PanFile extends JLabel implements MouseListener{

	/**
	 * 1.0�汾��ע�������2017.03.14
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
	//��̬�ĳ�ʼ�����е�ͼƬ
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
	//jar�����޷�ʵ���þ�̬����أ�ֻ���������ͣ���Ȼֻ����һ����Ϊ�˽�ʡ��Դ
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
	
	//ʹ��ѡ�������ʹ����ʹ�ܽ���set
	private boolean canChoose=true;
	
	static private Color onColor;
	static private Color clickColor;
	
//	static MainFrame.MouseRight rightMouseListener;
	
	public static final int DIRECTORY=0;
	public static final int FILE=1;
	public static final int BACK=2;
	/**
	 * ���췽�������������ļ��ı�ʾ�����ϵ�ͼƬ����ϵͳͼ��
	 * @param text �ļ���
	 * @param id �ļ���id���Ǹ�32��С��byte����
	 * @param key �ļ�����Կ��Ҳ��32�Ĵ�С
	 * @param type �ļ������ͣ��п��ܾ����ļ���Ҳ�п������ļ���
	 * @param location �ļ���λ��
	 * @param fileSize �ļ��Ĵ�С�������ļ��Ĵ�С��128����ȡ���Ľ��
	 * @param w ��������ĸ߶�
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
			setToolTipText("<html>�ļ�����:"+text+"<br>"+"�ļ���С:"+sizeString+"</html>");
		}
		else{
			setToolTipText("�ļ�������:"+text);
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
		//ʹ��ͼ�����ʽ
		setIcon(getRightImageIcon(text));
		
//		if(rightMouseListener!=null){
//			addMouseListener(rightMouseListener);
//		}
		
		//���涼�ǳ��Ի������ļ��ĵ�ǰϵͳͼ��Ĺ���
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
	 * �˺���ֻ������һ�Σ�������������
	 */
	public static void setLabelFont(Font font){
		PanFile.font=font;
	}
	/**
	 * �˺���ֻ������һ�Σ�����������������
	 */
	public static void setConnection(MyConnection connection){
		PanFile.connection=connection;
	}
	
	/**
	 * �˺���ֻ������һ�Σ���������w������С����ĸ߶�
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
	 * ���õ�ǰ�ļ����ڵ�panel����һ���������panel���Լ��ʼ�ķ������panel��scrollpanel
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
	 * �ı䵱ǰĿ¼Ϊ����ļ���ָ���Ŀ¼
	 */
	void changeDirectory(){
		//���粻���ļ��еĻ��Ͳ��ܸı�Ŀ¼
		if(type==FILE){
			return;
		}
		//��������ļ���֮ǰ�Ѿ�������ˣ��Ѿ���ȡ������Ӧ���ļ���������ݣ���ֱ�����õ��µ�panel������
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
			//�����ͨ�����ӻ�ȡ�����λ�õ��ļ�����Ķ���
			PanFile[] allFiles = null;
			try {
				allFiles=connection.requireFiles(loc,deep+1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//Ҫ���һ�������ϼ���jpanel������������ĵ�ַ����ûʵ�ֵ�һ���ٶ�
			//Ϊ��ʵ��ҳ��ˢ�µĹ��ܣ�ֻ�����أ��Ӹ�fatherPanel��ȥ�����س���JPanelHasFather
			
			childPanel=new JPanelHasFather(fatherPanel.getLayout());
			childPanel.setBackground(fatherPanel.getBackground());
			childPanel.setFatherPanel(fatherPanel);
			childPanel.setLocDeep(loc, deep+1);
			childPanel.addMouseListener(origin.getMouseListeners()[0]);
			//����ǰ���²��������panelչ�ֳ���
			origin.changePanel(childPanel,loc,deep+1);
			PanFile backFile=new PanFile("�����ϼ�", null, null, DIRECTORY, loc,deep,0,w);
			backFile.enChoosed(false);
			backFile.setType(PanFile.BACK);
			backFile.setChild(fatherPanel);
			backFile.setFather(childPanel, origin);
			//���ܻ���Ҫһ�����ص�ͼ��
			backFile.setIcon(backIcon);
			origin.addMore(backFile);

			for(PanFile file:allFiles){
				file.setFather(childPanel, origin);
				origin.addMore(file);
			}

			//��ʱҪ�Ƴ������Ѿ�ѡ���˵ģ���Ϊ�Ѿ�ѡ�����λ����һ��panel��
			for(JLabel t:origin.allChoosed)
				((PanFile)t).ini();
			origin.allChoosed.removeAll(origin.allChoosed);
			origin.setFatherPan(this);
		}
	}
	
	/**
	 * ��������¼��ķ�Ӧ�����������¼���˫���¼�
	 */
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		//˫���¼����õ�300�������
		if(e.getButton() != MouseEvent.BUTTON1){
			return;
		}
		origin.requestFocus();
		if (System.currentTimeMillis()-clickTime<=300)
		{
			setBackground(onColor);
			//һ��˫����Ҫ�������ѡ����Ϊfalse����Ϊ��һ�»�ѡ����
			choosed=false;
			clickTime=System.currentTimeMillis();
			if(type==DIRECTORY||type==BACK){
				//���������Ŀ¼�Ļ����ܸı�·����ʵ��������е���࣬��Ϊ֮�����֤
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

	//�����л���Ŀ¼���ȥ��ѡ
	void ini(){
		choosed=false;
		setBackground(onColor);
		setOpaque(false);
	}
	
	//�����ܷ�ѡ�����set��
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
	//ʵ����������Ƴ��ĸ���Ч��
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
