package logic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.JOptionPane;

import ui.PanFile;
import ui.PanFriend;
import ui.Task;

public class MyConnection {
	private DataInputStream socketIn;
	private DataOutputStream socketOut;
	private static String host="192.168.79.1";
	private static int port=3456;
	private static byte[] cookies=null;
	//private int type;
	private MySecret mySecret;
	private Socket socket;
	private String userName;
	
	//ssl模式所需要的东西
	private static SSLContext context;
	private static SSLSocketFactory ssf;
	//为了提前加载好
	{
		if(context == null){  
			try {  
				KeyStore ts = KeyStore.getInstance("JKS");  
				ts.load(this.getClass().getResourceAsStream("/resourse/clientTrust.jks"), "123456".toCharArray());  
				
				System.out.println(this.getClass().getResourceAsStream("/resourse/clientTrust.jks").available());
				
				TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");  
				tmf.init(ts);  
				TrustManager [] tm = tmf.getTrustManagers();  
				context = SSLContext.getInstance("SSL");  
				context.init(null, tm, null);  
			} catch (Exception e) {         //省略捕获的异常信息  
				e.printStackTrace();  
			}   
			ssf = context.getSocketFactory();
		}
	}
	
	//传送的数据帧的第一个字节标志，空行分割两个方向的
	public static final byte  SIGN_IN=1;
	public static final byte  SIGN_OUT=2;
	public static final byte  SIGN_UP=3;
	public static final byte  REQUIRE_FILES=4;
	public static final byte  REQUIRE_FRIENDS=5;
	public static final byte  UPLOAD_FILE=7;
	public static final byte  DOWNLOAD_FILE=8;
	public static final byte  ADD_FRIEND=9;
	public static final byte  SHARE_FILE=10;
	public static final byte CREATE_DIRECTORY=11;
	public static final byte DELETE_FILE=12;

	public static final byte  SUCCESS=101;
	public static final byte  FAILED_ACTION=100;
	//public static final byte  SUCCESS_LOGN=101;
	public static final byte  GIVE_FILES=102;
	public static final byte  GIVE_FRIENDS=103;
	public static final byte  DEMMAND_FILE=104;
	public static final byte  GIVE_FILE=105;
	public static final byte  GIVE_FRIEND=106;
	public static final byte  UPLOAD_FINISH=107;
	public static final byte SUCCESS_SHARE=108;
	public static final byte SUCCESS_CREATE=109;
	public static final byte SUCCESS_DELETE=110;
	//public static final byte  SUCCESS_SHARE=108;
	
	/**
	 * 建立一个连接，假如已经登录了，这个连接也会是登录的状态
	 * @param type 这个连接的类型
	 * @throws NumberFormatException 数字格式异常，不会出现
	 * @throws UnknownHostException	找不到主机异常
	 * @throws IOException io异常，一般是连接出现问题
	 * @throws TimeOutException 客户端超时异常，由客户端长时间没操作被踢出系统产生
	 */
	public MyConnection(byte type) throws NumberFormatException, UnknownHostException, IOException, MyTimeOutException{ 
		
		//ssl模式
		socket = (SSLSocket) ssf.createSocket(host,port);
		//socket = new Socket(host,port);
		socketIn=new DataInputStream(socket.getInputStream());
		socketOut=new DataOutputStream(socket.getOutputStream());
		//this.type=type;
		if(type==UPLOAD_FILE||type==DOWNLOAD_FILE){
			socketOut.write(type);
			socketOut.write(cookies);
//			byte[] successFlag=new byte[1];
//			socketIn.read(successFlag);
//			if(successFlag[0]!=(byte)SUCCESS){
//				throw new MyTimeOutException();
//			}
		}
	}
	
	/**
	 * 登录
	 * @param name 用户名
	 * @param password 用户密码
	 * @return 是否登录成功
	 */
	public boolean signIn(char[] name,char[] password){
		MessageDigest sha256 = null;
		byte[] nameHash = null;
		byte[] passwordHash;
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			//不可能发生
			e.printStackTrace();
		}
		
		userName=new String(name);
		//以hash(hash(name)+hash(password))作为公私钥产生的种子
		try {
			sha256.update(new String(name).getBytes("utf-8"));
			nameHash=sha256.digest();
			sha256.update(new String(password).getBytes("utf-8"));
		} catch (UnsupportedEncodingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		passwordHash=sha256.digest();
		sha256.update(nameHash);
		sha256.update(passwordHash);
		mySecret=new MySecret(sha256.digest());
		sha256.update(passwordHash);
		sha256.update(sha256.digest());
		sha256.update(nameHash);
		CombineData data=new CombineData();
		//传送给服务器的是标志位+hash(name)+hash(hash(hash(password))+hash(name))
		//这么复杂是要保证攻击者得到整个数据库，也拿不到用户的私钥
		data.addData(SIGN_IN);
		data.addData(nameHash);
		data.addData(sha256.digest());
		try {
			socketOut.write(data.getData());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//成功后返回success的标志位和cookies
		//cookies只会初始化一次，之后都不会再变化
		byte[] f=new byte[1];
		try {
			socketIn.read(f);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(f[0]==SUCCESS){			
			cookies=new byte[32];
			int readNum=0;
			while(readNum!=32){
				try {
					readNum+=socketIn.read(cookies,readNum,32-readNum);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	public int signOut() {
		byte[] b=new byte[1];
		b[0]=SIGN_OUT;
		try {
			socketOut.write(b);
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	/**
	 * 用于测试时注册的函数，比较简单，与登录类似，先发送了登录的东西，然后加上了名字和公钥
	 * @param name
	 * @param password
	 * @return
	 */
	public boolean signUp(char[] name,char[] password) {
		MessageDigest sha256 = null;
		byte[] nameHash = null;
		byte[] passwordHash;
		userName=new String(name);
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			//不可能发生
			e.printStackTrace();
		}
		try {
			sha256.update(new String(name).getBytes("utf-8"));
			nameHash=sha256.digest();
			sha256.update(new String(password).getBytes("utf-8"));
		} catch (UnsupportedEncodingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		passwordHash=sha256.digest();
		sha256.update(nameHash);
		sha256.update(passwordHash);
		mySecret=new MySecret(sha256.digest());
		sha256.update(passwordHash);
		sha256.update(sha256.digest());
		sha256.update(nameHash);
		CombineData data=new CombineData();
		data.addData(SIGN_UP);
		data.addData(nameHash);
		data.addData(sha256.digest());
		try {
			if(new String(name).getBytes("utf-8").length>255){
				JOptionPane.showMessageDialog(null,"用户名过长，无法注册！");
				return false;
			}
			data.addData((byte)new String(name).getBytes("utf-8").length);
			data.addData(new String(name).getBytes("utf-8"));
		} catch (UnsupportedEncodingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		data.addData(mySecret.getPublicKey());
		
		try {
			socketOut.write(data.getData());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] f=new byte[1];
		try {
			socketIn.read(f);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if(f[0]==SUCCESS){			
			cookies=new byte[32];
			int readNum=0;
			while(readNum!=32){
				try {
					readNum+=socketIn.read(cookies,readNum,32-readNum);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 请求文件列表
	 * @param location 请求的位置，这是一个int数组，数组序列就是文件的路径
	 * @return 返回一个数组，数组里存放jpanel，表示相应的文件
	 * @throws IOException
	 */
	public PanFile[] requireFiles(int loc,int deep) throws IOException {
		PanFile[] panFiles;
		CombineData data=new CombineData();
		data.addData(REQUIRE_FILES);
		//这个方法里会连着数组的大小一起添加
		data.addData(loc);
		data.addData(deep);
		socketOut.write(data.getData());

		byte[] f=new byte[5];
		int readNum=0;
		while(readNum!=5){
			readNum+=socketIn.read(f,readNum,5-readNum);
		}
		DivideData dataDiv=new DivideData(f);
		if(dataDiv.getByte()!=GIVE_FILES){
			return null;
		}else{
			int t=dataDiv.getInt();
//			if(t==0){
//				return null;
//			}
			panFiles=new PanFile[t];
			for (int i = 0; i < t; i++) {
				int id;
				byte[] fileID=new byte[32];
				byte[] tempKey=new byte[129];
				byte[] fileKey=new byte[32];
				byte[] recdata=new byte[166];
				byte type;
				int size;
				readNum=0;
				while(readNum!=36){
					readNum+=socketIn.read(recdata,readNum,36-readNum);
				}
				dataDiv.resert(recdata);
				id=dataDiv.getInt();
				fileID=dataDiv.getBytes(32);
				
				
				String name=readString();
				//System.out.println(name);
				
				readNum=0;
				while(readNum!=134){
					readNum+=socketIn.read(recdata,readNum,134-readNum);
				}
				dataDiv.resert(recdata);
				size=dataDiv.getInt();
				tempKey=dataDiv.getBytes(129);
				type=dataDiv.getByte();
				if(type==1)
					fileKey=mySecret.decrypt(tempKey);
				else {
					fileKey=null;
				}
				//System.out.println(size);
				//注意，给的size大小是分组数目，不是实际大小，实际大小要乘16
				panFiles[i]=new PanFile(name, fileID, fileKey, type, id,deep, (size)*16, 0);
			}
			return panFiles;
		}
	}
	
	public boolean shareFiles(PanFile[] files,PanFriend[] friends){
		boolean ret=true;
		for(PanFriend friend:friends){
			CombineData data=new CombineData();
			data.addData(SHARE_FILE);
			data.addData(friend.getId());
			data.addData((int)files.length);
			for(PanFile file:files){
				if(file.getType()==PanFile.DIRECTORY){
					PanFile[] nextFiles = null;
					try {
						nextFiles=requireFiles(file.getLoc(), file.getDeep()+1);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					data.addData(file.getFileNameSize());
					try {
						data.addData(file.getName().getBytes("utf-8"));
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					data.addData((int)0);
					data.addData((byte)PanFile.DIRECTORY);
					if(nextFiles==null)
						data.addData((int)0);
					else{
						data.addData(nextFiles.length);
						if(!getDeepFiles(nextFiles, data,friend.getKey())){
							return false;
						}
					}
				}else{
					data.addData(file.getFileNameSize());
					try {
						data.addData(file.getName().getBytes("utf-8"));
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					data.addData((int)(file.getFileSize()/16));
					data.addData((byte)PanFile.FILE);
					data.addData(file.getId());
					data.addData(mySecret.encryptWithOthers(file.getKey(), friend.getKey()));
				}
			}
			try {
				socketOut.write(data.getData());
				byte[] a=new byte[1];
				socketIn.read(a);
				if(a[0]!=SUCCESS_SHARE){
					ret&=false;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
		return ret;
	}
	
	boolean getDeepFiles(PanFile[] files,CombineData data,byte[] friendKey){
		boolean ret=true;
		for(PanFile file:files){
			if(file.getType()==PanFile.DIRECTORY){
				PanFile[] nextFiles = null;
				try {
					nextFiles=requireFiles(file.getLoc(), file.getDeep()+1);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
				data.addData(file.getFileNameSize());
				try {
					data.addData(file.getName().getBytes("utf-8"));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				data.addData((int)0);
				data.addData((byte)PanFile.DIRECTORY);
				if(nextFiles==null)
					data.addData((int)0);
				else{
					data.addData(nextFiles.length);
					ret&=getDeepFiles(nextFiles, data,friendKey);
				}
			}else{
				data.addData(file.getFileNameSize());
				try {
					data.addData(file.getName().getBytes("utf-8"));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				data.addData((int)(file.getFileSize()/16));
				data.addData((byte)PanFile.FILE);
				data.addData(file.getId());
				data.addData(mySecret.encryptWithOthers(file.getKey(), friendKey));
			}
		}
		return ret;
	}
	
	public PanFriend[] requireFriends() throws IOException {
		byte[] a=new byte[1];
		a[0]=REQUIRE_FRIENDS;
		try {
			socketOut.write(a);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		byte[] f=new byte[5];
		int readNum=0;
		while(readNum!=5){
			readNum+=socketIn.read(f,readNum,5-readNum);
		}
		DivideData dataDiv=new DivideData(f);
		PanFriend[] friends=null;
		if(dataDiv.getByte()!=GIVE_FRIENDS){
			return null;
		}else{
			int t=dataDiv.getInt();
//			if(t==0){
//				return null;
//			}
			friends=new PanFriend[t];
			for (int i = 0; i < t; i++) {
				byte[] friendID=new byte[32];
				
				byte[] friendKey=new byte[65];
				readNum=0;
				while(readNum!=32){
					readNum+=socketIn.read(friendID,readNum,32-readNum);
				}
				String name=readString();
				readNum=0;
				while(readNum!=65){
					readNum+=socketIn.read(friendKey,readNum,65-readNum);
				}
				friends[i]=new PanFriend(name, friendID, 0, null, friendKey);
			}
		}
		return friends;
	}
	
	public PanFriend addFriend(char[] friendName){
		MessageDigest sha256 = null;
		byte[] nameHash = null;
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			//不可能发生
			e.printStackTrace();
		}
		try {
			sha256.update(new String(friendName).getBytes("utf-8"));
			nameHash=sha256.digest();
		} catch (UnsupportedEncodingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		CombineData data=new CombineData();
		data.addData(ADD_FRIEND);
		data.addData(nameHash);
		
		try {
			socketOut.write(data.getData());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] f=new byte[1];
		try {
			socketIn.read(f);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		byte[] friendKey=new byte[65];
		if(f[0]==GIVE_FRIEND){			
			int readNum=0;
			while(readNum!=65){
				try {
					readNum+=socketIn.read(friendKey,readNum,65-readNum);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return new PanFriend(new String(friendName), nameHash, 0, null, friendKey);
			
		}else{
			return null;
		}
	}
	
	/**
	 * 上传文件
	 * @param file 要上传的文件，可以是个文件夹名。假如是个文件夹，将上传这个文件夹里的所有文件，并且还会创建目录
	 * @param location 要上传到的位置，这个位置是文件夹的位置
	 * @param task 任务，传入这个参数的目的是能实现可视化的上传进度
	 * @return 返回是否上传成功
	 */
	public boolean uploadFile(File file,int loc,int deep,Task task) {
		//上传和下载都是一个独立的线程，建立一个新的连接用于传输数据
		MyConnection upConnection=null;
		//先把程序主要部分填起来，然后再去补时间超时异常的坑
		try {
			upConnection=new MyConnection(MyConnection.UPLOAD_FILE);
		} catch (NumberFormatException | IOException | MyTimeOutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] fileName = null;
		byte[] hashOfFile;
		byte[] fileID;
		byte[] fileKey;
		int fileSize;
		CombineData data=new CombineData();
		try {
			fileName=file.getName().getBytes("utf-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//这里的文件大小是指文件能被分成的分组数目，每个分组有16字节的大小
		fileSize=(int)(file.length()/16+2);
		data.addData((byte)fileName.length);
		if(fileName.length>=256){
			JOptionPane.showMessageDialog(null,"文件名过长，文件将无法上传！");
			return false;
		}
		if(file.length()/16+2>Integer.MAX_VALUE){
			JOptionPane.showMessageDialog(null,"文件过大，文件将无法上传！");
			return false;
		}
		data.addData(fileName);
		data.addData(fileSize);
		//先获得文件的哈希值
		MessageDigest sha256 = null;
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			//不可能发生
			e.printStackTrace();
		}
		FileInputStream fileStream=null;
		try {
			fileStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        byte[] fileBytes = new byte[1024*4];
        int nread = 0; 
        try {
			while ((nread = fileStream.read(fileBytes)) != -1) {
				sha256.update(fileBytes, 0, nread);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		try {
			fileStream.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        hashOfFile = sha256.digest();
        sha256.update(hashOfFile, 0, hashOfFile.length);
        fileID=sha256.digest();
        fileKey=mySecret.encrypt(hashOfFile);
        data.addData(fileID);
        data.addData(fileKey);
        data.addData(deep);
        data.addData(loc);
        try {
			upConnection.socketOut.write(data.getData());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        byte[] receiveData=new byte[25];
        int receiveNum=0;
        while(receiveNum!=25){
        	try {
				receiveNum+=upConnection.socketIn.read(receiveData,receiveNum,25-receiveNum);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				try {
					upConnection.socket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				return false;
			}
        }
        DivideData recData=new DivideData(receiveData);
        if(recData.getByte()!=GIVE_FILE){
        	try {
				upConnection.socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	return false;
        }
        //通过另一个类来操作文件       
        FileOperation fileOperation=new FileOperation(file, hashOfFile, recData.getBytes(16), recData.getInt(), recData.getInt(),upConnection.socketOut);
        byte[] f=new byte[1];
        f[0]=UPLOAD_FILE;
        try {
			upConnection.socketOut.write(f);
			fileOperation.sendFile(task);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				upConnection.socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return false;
		}
    	try {
			upConnection.socket.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	return true;
	}
	
	/**
	 * 下载文件的方法
	 * @param id 文件的id，是文件的唯一标示符
	 * @param key 文件的加密密钥，就是文件的hash值
	 * @param size 文件的大小，就是文件的大小，不是分组的数目
	 * @param fileLoc 文件的位置，就是一个int数组
	 * @param task 任务，传入这个参数的目的就是为了实现可视化的下载进度
	 * @return 返回下载是否成功
	 */
	public boolean downloadFile(byte[] id,byte[] key,Long size,String fileLoc,Task task) {
		File file=new File(fileLoc);
		MyConnection doConnection=null;
		try {
			doConnection=new MyConnection(MyConnection.DOWNLOAD_FILE);
		} catch (NumberFormatException | IOException | MyTimeOutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CombineData data=new CombineData();
		//data.addData(DOWNLOAD_FILE);
		data.addData(id);
        try {
			doConnection.socketOut.write(data.getData());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        byte[] f=new byte[1];
        try {
			doConnection.socketIn.read(f);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        if(f[0]!=GIVE_FILE){
        	try {
				doConnection.socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        	return false;
        }
        byte[] iv=new byte[16];
        int receiveNum=0;
        //会获得两个偏移量，要求传输这两个偏移量间的所有的数据，包括边界，偏移量指的是分组的编号的偏移，不是真正的文件偏移量
        while(receiveNum!=16){
        	try {
				receiveNum+=doConnection.socketIn.read(iv,receiveNum,16-receiveNum);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        FileOperation fileOperation=new FileOperation(file, key, iv,doConnection.socketIn,size-16);
        try {
			fileOperation.receiveFile(task);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				doConnection.socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return false;
		}
        try {
			doConnection.socket.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        return true;
	}
	
	public String getName(){
		return userName;
	}
	
	public boolean deleteFile(int loc,int deep){
		CombineData data=new CombineData();
		data.addData(DELETE_FILE);
		data.addData(loc);
		data.addData(deep);
		byte[] r=new byte[1];
		try {
			socketOut.write(data.getData());
			socketIn.read(r);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(r[0]==SUCCESS_DELETE){
			return true;
		}
		return false;
	}
	
	
	
	/**
	 * 在当前目录建立一个文件夹，返回新建立的这个文件夹的位置
	 * @param name 文件夹的名字
	 * @param location 要建立文件夹的位置
	 * @return 返回建立的文件夹的位置
	 */
	public int createDirectory(byte[] name,int loc,int deep){
		CombineData data=new CombineData();
		data.addData(CREATE_DIRECTORY);
		//addData里面添加一个byte数组的方法里没有添加这个byte数组的大小，因此要独立添加
		data.addData((byte)name.length);
		data.addData(name);
		data.addData(loc);
		data.addData(deep);
		try {
			socketOut.write(data.getData());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] d=new byte[5];
		int readNum=0;
		while(readNum!=5){
			try {
				readNum+=socketIn.read(d,readNum,5-readNum);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		DivideData getData=new DivideData(d);
		if(getData.getByte()==SUCCESS_CREATE){
			int w=getData.getInt();
			return w;
		}else{
			return 0;
		}
	}
	
	public static void setHost(String host) {
		MyConnection.host = host;
	}

	public static void setPort(int port) {
		MyConnection.port = port;
	}
	
	/**
	 * 从输入流中读取一个String
	 * @return
	 * @throws IOException
	 */
	String readString() throws IOException{
		int readNum=0;
		//先读取这个字符串的长度，然后根据长度读取字符串
		byte[] nameSize=new byte[1];
		socketIn.read(nameSize);
		byte[] name=new byte[(int)0xff&nameSize[0]];
		readNum=0;
		//使用0xff位与的目的是防止大小超过127后显示的负数
		
		int a=0xff&nameSize[0];
		while(readNum!=a){
			readNum+=socketIn.read(name,readNum,0xff&nameSize[0]-readNum);
		}
		return new String(name,"utf-8");
	}
	
	/**
	 * 读取一个int数组，这个数组就是文件的位置数组
	 * @return 返回读取的数组
	 * @throws IOException
	 */
	int[] readInts() throws IOException{
		byte[] a=new byte[4];
		int readNum=0;
		//前4个byte是小端序的int，java虚拟机是大端序，因此要手动转换，这个int是这个数组的长度，相应的要再读取长度乘以4个byte
		while(readNum!=4){
			readNum+=socketIn.read(a,readNum,4-readNum);
		}
		int length=0;
		for(int i=3;i>=0;i--){
			length*=256;
			length+=0xff&a[i];
		}
		readNum=0;
		byte[] data=new byte[length*4];
		while(readNum!=length*4){
			readNum+=socketIn.read(data,readNum,length*4-readNum);
		}
		DivideData intData=new DivideData(data);
		int[] ints=new int[length];
		for(int i=0;i<length;i++){
			ints[i]=intData.getInt();
		}
		return ints;
	}
	
}
