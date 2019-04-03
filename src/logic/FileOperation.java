package logic;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import ui.Task;

/**
 * 专门用于文件的操作，可以读取文件，加密后传输到另一个输出流里（也就是sock流里）
 * 或是读取流，解密后写到文件里，两个功能是分开的，用两个不同的构造函数进行构造
 * 1.0版本用aes256-cbc进行加密
 * 未来的改进的2.0版本期望换用ctr模式加密实现文件加密的并行化，或是换用国产对称加密算法
 * @author Administrator 版本1.0
 *
 */
public class FileOperation {
	private CipherInputStream in;
	private CipherOutputStream out;
	private DataInputStream sin;
	private DataOutputStream sout;
	//要加密的部分文件的大小
	private long num;
	private Cipher cipher;
	private byte[] IV;
	
	//aes256用aes-cbc-pkcs7需要加入，同时把第二句移下去
//	static{
//		 Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//		 cipher=Cipher.getInstance("AES/CBC/PKCS7Padding");
//	}
	
	{
		try {
			
			cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 用于构造一个读入文件流，输出加密的信息到另一个流中的构造函数
	 * @param file 用于加密的文件
	 * @param key 用于加密的密钥，为256字节长度，也就是32个byte
	 * @param IV 用于加密的iv值，初期用cbc进行加密，后期期望采用ctr模式
	 * @param left 左偏移，也就是要求开始加密的位置，这个偏移指的是分组的序号，而不是文件偏移
	 * @param right 右偏移，加密到这个位置，基本与左偏移相同
	 * @param sout 用于输出的流
	 */
	FileOperation(File file,byte[] key,byte[] IV,int left,int right,DataOutputStream sout) {
		// TODO Auto-generated constructor stub
        //Cipher cipher = null;
        this.sout=sout;
        //这个大小就是实际的偏移量，要乘以16，而且由于加密的时候是包括左偏移，但不包括右偏移的，有iv，所以要加1
        num=right-left+1;
        //System.out.println(num);
        num*=16;
        this.IV=IV;
        try {
        	//由于使用256位密钥长度的aes进行加密，因此可能会在这里报出密钥长度不支持的异常，以后要在这里加点东西，保证密钥长度支持的时候能够退出程序
			//cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),new IvParameterSpec(IV));
			
			//128位长度密钥情况，适用于普通情况，一般应用这个
			byte[] key16=new byte[16];
			for(int i=0;i<16;i++){
				key16[i]=key[i];
			}
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key16, "AES"),new IvParameterSpec(IV));

			
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        FileInputStream input = null;
		try {
			input = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//要减一的原因是left块也要进行加密操作，left最小就是1
		//System.out.println(left);
        long a=(left-1)*16;
        if(left==0)
        	a=0;
        //此处跳过了一定字节，然后再创建密文输出流就是为了实现加密一部分的目的
    	while(a!=0)
    	{
    		try {
				a-=input.skip(a);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
        in = new CipherInputStream(input, cipher);
	}
	
	/**
	 * 配合进行加密操作的函数，然后把加密后的文件流直接写入到输出流里，调用这个函数前，一定是使用了上面的构造函数进行了初始化
	 * @param task 用于实现进度可视化的组件，要求实现了getOnePoint(int)方法，这个方法提示task又完成了这么多个千分点的进度，还有一个finish方法指示任务已完成
	 * @throws IOException
	 */
	void sendFile(Task task) throws IOException{
		//设立512的缓冲区，加密一点，传输一点
		byte[] buffer=new byte[512];
		int nSend=0;
		int sendnum=0;
		long allNum=num;
		sout.write(IV);
		num-=16;
		while(num>0){
			nSend=in.read(buffer);
			//System.out.println(nSend);
			
			if(nSend<num){
				sout.write(buffer,0,nSend);
			}else{
				sout.write(buffer,0,(int)num);
			}
			num-=nSend;
			//提醒task传输了多少
			sendnum+=nSend;
			if(sendnum*100>allNum){
				task.getOnePoint(sendnum);
				sendnum=0;
			}
		}
		task.finish();
		in.close();
		sout.close();
	}
	
	/**
	 * 用于从流中读取加密的信息，解密后写入到文件中去
	 * @param file 要写入的文件
	 * @param key 用于机密的密钥
	 * @param IV 用于解密的iv值
	 * @param sin 输入加密信息的流
	 * @param size 将要解密的文件大小，就是文件的大小以16字节向上取整，不是分组数目（与另一个的偏移不同）
	 */
	FileOperation(File file,byte[] key,byte[] IV,DataInputStream sin,long size) {
		// TODO Auto-generated constructor stub
		//此处要注意的注释与上面的一个构造函数相同
        //Cipher cipher = null;
        this.sin=sin;
        num=size;
        try {
        	//256位密钥情况
			//cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),new IvParameterSpec(IV));
			
			//128位长度密钥情况，适用于普通情况，一般应用这个
			byte[] key16=new byte[16];
			for(int i=0;i<16;i++){
				key16[i]=key[i];
			}
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key16, "AES"),new IvParameterSpec(IV));

		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        FileOutputStream output = null;
		try {
			output = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
        out = new CipherOutputStream(output, cipher);
	}
      
	/**
	 * 接收文件的方法，与上面的一个从流中读取加密信息的构造函数相匹配，只能在调用上面一个构造函数后才能调用
	 * @param task 用于实现进度可视化的组件，要求实现了getOnePoint(int)方法，这个方法提示task又完成了这么多个千分点的进度，还有一个finish方法指示任务已完成
	 * @throws IOException
	 */
	void receiveFile(Task task) throws IOException{
		//此处注意点与上面的接收文件的相同
		byte[] buffer=new byte[512];
		int nRecv=0;
		int recnum=0;
		long allNum=num;
		//JOptionPane.showMessageDialog(null,"allnum"+allNum);
		while(num>0){
			//JOptionPane.showMessageDialog(null,"num"+num);
			
			nRecv=sin.read(buffer);
			if(nRecv<num){
				out.write(buffer,0,nRecv);
			}else{
				out.write(buffer,0,(int)num);
			}
			num-=nRecv;
			recnum+=nRecv;
			if(recnum*100>allNum){
				task.getOnePoint(recnum);
				recnum=0;
			}
		}
		task.finish();
		out.flush();
		sin.close();
		out.close();
	}

//	public static void main(String[] args){
//		FileOperation a=new FileOperation(new File("C:\\2.txt"), new byte[32], new byte[16]);
//	}
}
