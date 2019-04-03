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
 * ר�������ļ��Ĳ��������Զ�ȡ�ļ������ܺ��䵽��һ��������Ҳ����sock���
 * ���Ƕ�ȡ�������ܺ�д���ļ�����������Ƿֿ��ģ���������ͬ�Ĺ��캯�����й���
 * 1.0�汾��aes256-cbc���м���
 * δ���ĸĽ���2.0�汾��������ctrģʽ����ʵ���ļ����ܵĲ��л������ǻ��ù����ԳƼ����㷨
 * @author Administrator �汾1.0
 *
 */
public class FileOperation {
	private CipherInputStream in;
	private CipherOutputStream out;
	private DataInputStream sin;
	private DataOutputStream sout;
	//Ҫ���ܵĲ����ļ��Ĵ�С
	private long num;
	private Cipher cipher;
	private byte[] IV;
	
	//aes256��aes-cbc-pkcs7��Ҫ���룬ͬʱ�ѵڶ�������ȥ
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
	 * ���ڹ���һ�������ļ�����������ܵ���Ϣ����һ�����еĹ��캯��
	 * @param file ���ڼ��ܵ��ļ�
	 * @param key ���ڼ��ܵ���Կ��Ϊ256�ֽڳ��ȣ�Ҳ����32��byte
	 * @param IV ���ڼ��ܵ�ivֵ��������cbc���м��ܣ�������������ctrģʽ
	 * @param left ��ƫ�ƣ�Ҳ����Ҫ��ʼ���ܵ�λ�ã����ƫ��ָ���Ƿ������ţ��������ļ�ƫ��
	 * @param right ��ƫ�ƣ����ܵ����λ�ã���������ƫ����ͬ
	 * @param sout �����������
	 */
	FileOperation(File file,byte[] key,byte[] IV,int left,int right,DataOutputStream sout) {
		// TODO Auto-generated constructor stub
        //Cipher cipher = null;
        this.sout=sout;
        //�����С����ʵ�ʵ�ƫ������Ҫ����16���������ڼ��ܵ�ʱ���ǰ�����ƫ�ƣ�����������ƫ�Ƶģ���iv������Ҫ��1
        num=right-left+1;
        //System.out.println(num);
        num*=16;
        this.IV=IV;
        try {
        	//����ʹ��256λ��Կ���ȵ�aes���м��ܣ���˿��ܻ������ﱨ����Կ���Ȳ�֧�ֵ��쳣���Ժ�Ҫ������ӵ㶫������֤��Կ����֧�ֵ�ʱ���ܹ��˳�����
			//cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),new IvParameterSpec(IV));
			
			//128λ������Կ�������������ͨ�����һ��Ӧ�����
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
		//Ҫ��һ��ԭ����left��ҲҪ���м��ܲ�����left��С����1
		//System.out.println(left);
        long a=(left-1)*16;
        if(left==0)
        	a=0;
        //�˴�������һ���ֽڣ�Ȼ���ٴ����������������Ϊ��ʵ�ּ���һ���ֵ�Ŀ��
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
	 * ��Ͻ��м��ܲ����ĺ�����Ȼ��Ѽ��ܺ���ļ���ֱ��д�뵽�����������������ǰ��һ����ʹ��������Ĺ��캯�������˳�ʼ��
	 * @param task ����ʵ�ֽ��ȿ��ӻ��������Ҫ��ʵ����getOnePoint(int)���������������ʾtask���������ô���ǧ�ֵ�Ľ��ȣ�����һ��finish����ָʾ���������
	 * @throws IOException
	 */
	void sendFile(Task task) throws IOException{
		//����512�Ļ�����������һ�㣬����һ��
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
			//����task�����˶���
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
	 * ���ڴ����ж�ȡ���ܵ���Ϣ�����ܺ�д�뵽�ļ���ȥ
	 * @param file Ҫд����ļ�
	 * @param key ���ڻ��ܵ���Կ
	 * @param IV ���ڽ��ܵ�ivֵ
	 * @param sin ���������Ϣ����
	 * @param size ��Ҫ���ܵ��ļ���С�������ļ��Ĵ�С��16�ֽ�����ȡ�������Ƿ�����Ŀ������һ����ƫ�Ʋ�ͬ��
	 */
	FileOperation(File file,byte[] key,byte[] IV,DataInputStream sin,long size) {
		// TODO Auto-generated constructor stub
		//�˴�Ҫע���ע���������һ�����캯����ͬ
        //Cipher cipher = null;
        this.sin=sin;
        num=size;
        try {
        	//256λ��Կ���
			//cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),new IvParameterSpec(IV));
			
			//128λ������Կ�������������ͨ�����һ��Ӧ�����
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
	 * �����ļ��ķ������������һ�������ж�ȡ������Ϣ�Ĺ��캯����ƥ�䣬ֻ���ڵ�������һ�����캯������ܵ���
	 * @param task ����ʵ�ֽ��ȿ��ӻ��������Ҫ��ʵ����getOnePoint(int)���������������ʾtask���������ô���ǧ�ֵ�Ľ��ȣ�����һ��finish����ָʾ���������
	 * @throws IOException
	 */
	void receiveFile(Task task) throws IOException{
		//�˴�ע���������Ľ����ļ�����ͬ
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
