package logic;

import java.io.UnsupportedEncodingException;

/**
 * 分解数据的类，传入一个byte数组，通过getByte等方法解析出数据
 * 如解析int，long，string等
 * @author Administrator
 *
 */
class DivideData {
	private byte[] data;
	private int loc;
	DivideData(byte[] data) {
		// TODO Auto-generated constructor stub
		this.data=data;
		loc=0;
	}
	void resert(byte[] data){
		this.data=data;
		loc=0;
	}
	byte[] getBytes(int num){
		byte[] a=new byte[num];
		for(int i=0;i<num;i++){
			a[i]=data[loc+i];
		}
		loc+=num;
		return a;
	}
	int getInt() {
		int num=0;
		for(int i=3;i>=0;i--){
			num*=256;
			num+=0xff&data[loc+i];
		}
		loc+=4;
		return num;
	}
	long getLong(){
		long num=0;
		for(int i=7;i>=0;i--){
			num*=256;
			num+=0xff&data[loc+i];
		}
		loc+=8;
		return num;
	}
	byte getByte(){
		byte d;
		d=data[loc];
		loc+=1;
		return d;
	}
	String getString(byte size){
		byte[] s=new byte[2*size];
		for(int i=0;i<2*size;i++){
			s[i]=data[loc+i];
		}
		loc+=2*size;
		try {
			return new String(s,"utf-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
