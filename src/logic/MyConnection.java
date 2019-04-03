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
	
	//sslģʽ����Ҫ�Ķ���
	private static SSLContext context;
	private static SSLSocketFactory ssf;
	//Ϊ����ǰ���غ�
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
			} catch (Exception e) {         //ʡ�Բ�����쳣��Ϣ  
				e.printStackTrace();  
			}   
			ssf = context.getSocketFactory();
		}
	}
	
	//���͵�����֡�ĵ�һ���ֽڱ�־�����зָ����������
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
	 * ����һ�����ӣ������Ѿ���¼�ˣ��������Ҳ���ǵ�¼��״̬
	 * @param type ������ӵ�����
	 * @throws NumberFormatException ���ָ�ʽ�쳣���������
	 * @throws UnknownHostException	�Ҳ��������쳣
	 * @throws IOException io�쳣��һ�������ӳ�������
	 * @throws TimeOutException �ͻ��˳�ʱ�쳣���ɿͻ��˳�ʱ��û�������߳�ϵͳ����
	 */
	public MyConnection(byte type) throws NumberFormatException, UnknownHostException, IOException, MyTimeOutException{ 
		
		//sslģʽ
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
	 * ��¼
	 * @param name �û���
	 * @param password �û�����
	 * @return �Ƿ��¼�ɹ�
	 */
	public boolean signIn(char[] name,char[] password){
		MessageDigest sha256 = null;
		byte[] nameHash = null;
		byte[] passwordHash;
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			//�����ܷ���
			e.printStackTrace();
		}
		
		userName=new String(name);
		//��hash(hash(name)+hash(password))��Ϊ��˽Կ����������
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
		//���͸����������Ǳ�־λ+hash(name)+hash(hash(hash(password))+hash(name))
		//��ô������Ҫ��֤�����ߵõ��������ݿ⣬Ҳ�ò����û���˽Կ
		data.addData(SIGN_IN);
		data.addData(nameHash);
		data.addData(sha256.digest());
		try {
			socketOut.write(data.getData());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//�ɹ��󷵻�success�ı�־λ��cookies
		//cookiesֻ���ʼ��һ�Σ�֮�󶼲����ٱ仯
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
	 * ���ڲ���ʱע��ĺ������Ƚϼ򵥣����¼���ƣ��ȷ����˵�¼�Ķ�����Ȼ����������ֺ͹�Կ
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
			//�����ܷ���
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
				JOptionPane.showMessageDialog(null,"�û����������޷�ע�ᣡ");
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
	 * �����ļ��б�
	 * @param location �����λ�ã�����һ��int���飬�������о����ļ���·��
	 * @return ����һ�����飬��������jpanel����ʾ��Ӧ���ļ�
	 * @throws IOException
	 */
	public PanFile[] requireFiles(int loc,int deep) throws IOException {
		PanFile[] panFiles;
		CombineData data=new CombineData();
		data.addData(REQUIRE_FILES);
		//������������������Ĵ�Сһ�����
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
				//ע�⣬����size��С�Ƿ�����Ŀ������ʵ�ʴ�С��ʵ�ʴ�СҪ��16
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
			//�����ܷ���
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
	 * �ϴ��ļ�
	 * @param file Ҫ�ϴ����ļ��������Ǹ��ļ������������Ǹ��ļ��У����ϴ�����ļ�����������ļ������һ��ᴴ��Ŀ¼
	 * @param location Ҫ�ϴ�����λ�ã����λ�����ļ��е�λ��
	 * @param task ���񣬴������������Ŀ������ʵ�ֿ��ӻ����ϴ�����
	 * @return �����Ƿ��ϴ��ɹ�
	 */
	public boolean uploadFile(File file,int loc,int deep,Task task) {
		//�ϴ������ض���һ���������̣߳�����һ���µ��������ڴ�������
		MyConnection upConnection=null;
		//�Ȱѳ�����Ҫ������������Ȼ����ȥ��ʱ�䳬ʱ�쳣�Ŀ�
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
		//������ļ���С��ָ�ļ��ܱ��ֳɵķ�����Ŀ��ÿ��������16�ֽڵĴ�С
		fileSize=(int)(file.length()/16+2);
		data.addData((byte)fileName.length);
		if(fileName.length>=256){
			JOptionPane.showMessageDialog(null,"�ļ����������ļ����޷��ϴ���");
			return false;
		}
		if(file.length()/16+2>Integer.MAX_VALUE){
			JOptionPane.showMessageDialog(null,"�ļ������ļ����޷��ϴ���");
			return false;
		}
		data.addData(fileName);
		data.addData(fileSize);
		//�Ȼ���ļ��Ĺ�ϣֵ
		MessageDigest sha256 = null;
		try {
			sha256 = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			//�����ܷ���
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
        //ͨ����һ�����������ļ�       
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
	 * �����ļ��ķ���
	 * @param id �ļ���id�����ļ���Ψһ��ʾ��
	 * @param key �ļ��ļ�����Կ�������ļ���hashֵ
	 * @param size �ļ��Ĵ�С�������ļ��Ĵ�С�����Ƿ������Ŀ
	 * @param fileLoc �ļ���λ�ã�����һ��int����
	 * @param task ���񣬴������������Ŀ�ľ���Ϊ��ʵ�ֿ��ӻ������ؽ���
	 * @return ���������Ƿ�ɹ�
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
        //��������ƫ������Ҫ����������ƫ����������е����ݣ������߽磬ƫ����ָ���Ƿ���ı�ŵ�ƫ�ƣ������������ļ�ƫ����
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
	 * �ڵ�ǰĿ¼����һ���ļ��У������½���������ļ��е�λ��
	 * @param name �ļ��е�����
	 * @param location Ҫ�����ļ��е�λ��
	 * @return ���ؽ������ļ��е�λ��
	 */
	public int createDirectory(byte[] name,int loc,int deep){
		CombineData data=new CombineData();
		data.addData(CREATE_DIRECTORY);
		//addData�������һ��byte����ķ�����û��������byte����Ĵ�С�����Ҫ�������
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
	 * ���������ж�ȡһ��String
	 * @return
	 * @throws IOException
	 */
	String readString() throws IOException{
		int readNum=0;
		//�ȶ�ȡ����ַ����ĳ��ȣ�Ȼ����ݳ��ȶ�ȡ�ַ���
		byte[] nameSize=new byte[1];
		socketIn.read(nameSize);
		byte[] name=new byte[(int)0xff&nameSize[0]];
		readNum=0;
		//ʹ��0xffλ���Ŀ���Ƿ�ֹ��С����127����ʾ�ĸ���
		
		int a=0xff&nameSize[0];
		while(readNum!=a){
			readNum+=socketIn.read(name,readNum,0xff&nameSize[0]-readNum);
		}
		return new String(name,"utf-8");
	}
	
	/**
	 * ��ȡһ��int���飬�����������ļ���λ������
	 * @return ���ض�ȡ������
	 * @throws IOException
	 */
	int[] readInts() throws IOException{
		byte[] a=new byte[4];
		int readNum=0;
		//ǰ4��byte��С�����int��java������Ǵ�������Ҫ�ֶ�ת�������int���������ĳ��ȣ���Ӧ��Ҫ�ٶ�ȡ���ȳ���4��byte
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
