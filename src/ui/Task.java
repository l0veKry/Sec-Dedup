package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

public class Task extends JPanel{
	/**
	 * 20160405
	 */
	private static final long serialVersionUID = 1L;
	private long startMoment;
	private long lastMoment;
	private long lastRefresh;
	private long schedue;
	private long size;
	private String name;
	//private int type;
	private String sizeString="";
	
	private JLabel nameAndSpeed;
	private ProgressBar sBar;
	
	static int arc=20;
	
	static ImageIcon upload;
	static final int UPLOAD=1;
	static ImageIcon download;
	static final int DOWNLOAD=2;
	static int y;
	Task(String name,long size,int type,int w){
		super();
		if(size<1024){
			sizeString=String.valueOf(size)+"B";
		}else if(size <1024*1024){
			sizeString=String.valueOf(((double)(size*100/1024)/100))+"KB";
		}else if(size <1024*1024*1024){
			sizeString=String.valueOf(((double)(size*100/1024/1024)/100))+"MB";
		}else{
			sizeString=String.valueOf(((double)(size*100/1024/1024/1024))/100)+"GB";
		}
		setToolTipText("<html>文件名称:"+
				name+"<br>"+"文件大小:"+sizeString+"</html>");
		this.name=name;
		if(name.length()>33){
			this.name=name.substring(0, 33)+"...";
		}
		
		setPreferredSize(new Dimension(0, y));
		GridLayout layout=new GridLayout(1,2);
		layout.setVgap(5);
		layout.setHgap(5);
		setLayout(layout);
		
		Font font=new Font("微软雅黑",Font.PLAIN,MainFrame.fontSize-5);
		
		Border empty = new EmptyBorder(10, 10, 10, 10);
		setBorder(empty);
		nameAndSpeed=new JLabel();
		nameAndSpeed.setOpaque(false);
		nameAndSpeed.setIcon(PanFile.getRightImageIcon(name));
		nameAndSpeed.setText(getSpeedText(-1));
		nameAndSpeed.setFont(font);
		//nameAndSpeed.setPreferredSize(new Dimension((w-15)/2, y-10));
		
		nameAndSpeed.setHorizontalTextPosition(JLabel.RIGHT);
		nameAndSpeed.setVerticalAlignment(JLabel.CENTER);
		nameAndSpeed.setHorizontalAlignment(JLabel.LEFT);
		add(nameAndSpeed,BorderLayout.WEST);
		
		sBar=new ProgressBar();
		sBar.setSchedule(0);
		sBar.setFont(font);
		//sBar.setPreferredSize(new Dimension((w-15)/2, y-10));
		add(sBar,BorderLayout.EAST);
		
        setOpaque(false);
        this.size=size;
        //this.type=type;
        lastMoment=System.currentTimeMillis();
        startMoment=lastMoment;
        lastRefresh=lastMoment;
	}
	
	String getSpeedText(long speed){
		String speedString;
		if(speed>1024){
			speedString=String.valueOf((double)((int)((double)speed/1024*100))/100)+"M/s";
		}else {
			speedString=String.valueOf(speed)+"k/s";
		}
		if(speed==-1)
			speedString="去重检测中...";
		String s="<html><div align=\"center\">"+name+"</div><div align=\"center\">"+speedString+"</div></html>";
		return s;
	}
	
	//重载用于实现最终的平均速度和文件大小
	String getSpeedText(long speed,long size){
		String speedString;
		if(speed>1024){
			speedString=String.valueOf((double)((int)((double)speed/1024*100))/100)+"M/s";
		}else {
			speedString=String.valueOf(speed)+"k/s";
		}
		String s="<html><div align=\"center\">"+name+"</div><div align=\"center\">"+"平均速度:"+speedString+"  文件大小"+sizeString+"</div></html>";
		return s;
	}
	
	protected void paintComponent(Graphics g){
		RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),arc,arc);
		g.setClip(rect);
		super.paintComponent(g);
	}
	
	public void getOnePoint(int getsize){
		schedue+=getsize;
		//防止出现除零异常
		if(System.currentTimeMillis()==lastMoment)
			lastMoment--;
		long speed=(long)getsize/1024*1000/(System.currentTimeMillis()-lastMoment);
		if(System.currentTimeMillis()-lastRefresh>=1000){
			nameAndSpeed.setText(getSpeedText(speed));
			//nameAndSpeed.validate();
			nameAndSpeed.repaint();
			lastRefresh=System.currentTimeMillis();
		}
		sBar.setSchedule((int)(schedue*100/size));
		sBar.repaint();
		lastMoment=System.currentTimeMillis();
	}
	
	public void finish(){
		long speed=size*1000/(System.currentTimeMillis()-startMoment)/1024;
		schedue=size;
		nameAndSpeed.setText(getSpeedText(speed,size));
		nameAndSpeed.validate();
		sBar.setSchedule(100);
		sBar.repaint();
	}
}



class ProgressBar extends JLabel{
	
	/**
	 * 20160405
	 */
	private static final long serialVersionUID = 1L;
	//schedule为百分制
	private int schedule;
	//private JLabel scheduleText;
	
	
	ProgressBar(){
		super("0%");
		setOpaque(false);
		setFont(new Font("微软雅黑",Font.PLAIN,16));
		setOpaque(false);
		setVerticalAlignment(JLabel.CENTER);
		setHorizontalAlignment(JLabel.CENTER);
	}
	
	
	void setSchedule(int l) {
		this.schedule = l;
		setText(String.valueOf(l)+"%");
		repaint();
	}
	
	@Override
	protected void paintComponent(Graphics g){
		int w=getWidth();
		int h=getHeight();
		RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,w,h,h,h);
		g.setClip(rect);
		//抗锯齿设置
		Graphics2D g2 = (Graphics2D)g.create();
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	                RenderingHints.VALUE_ANTIALIAS_ON);
	    g2.setRenderingHint(RenderingHints. KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		//就先不写成可以自己控制颜色的了
		Paint emptyPaint =new GradientPaint(0, 0, new Color(196,217,230),
                    0, getHeight(), new Color(250,251,253));
		Paint fullPaint =new GradientPaint(0, 0, new Color(224,239,254),
                0, getHeight(), new Color(128,208,254));
		setOpaque(false);
		g2.setPaint(emptyPaint);
		g2.fill(rect);
		
		RoundRectangle2D fullRect = new RoundRectangle2D.Double(0,0,w*schedule/100,h,h,h);

		setOpaque(false);
		g2.setPaint(fullPaint);
		g2.fill(fullRect);
		rect=new RoundRectangle2D.Double(1,1,w-2,h-2,h-2,h-2);
		
		//画出边框的效果
		g2.setColor(new Color(200, 200, 200));
        g2.draw(rect);
        g2.setColor(new Color(91,130,151));
        g2.drawRoundRect(1, 1, w*schedule/100 - 2, h - 2, h-2,h-2);
        
        //只有最后再调用super的方法才能画出百分比
        super.paintComponent(g);
	}
}
