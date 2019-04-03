package logic;


/**
 * 用来组合数据的类，可以不断调用addData方法加入新的数据，最后调用getData方法得到一个byte
 * 数组，主要为了组合byte数组的方便
 * @author Administrator
 *
 */
public class CombineData {
	private byte[] data;
	public CombineData() {
		// TODO Auto-generated constructor stub
		data=null;
	}
//	
//	public static void main(String[] args){
//		CombineData data=new CombineData();
////		byte[] a=new byte[3];
////		a[0]=(byte)255;
////		a[1]=(byte)128;
////		a[2]=0;
////		data.addData(a);
//		int a=254;
//		data.addData((byte)a);
//		System.out.println((int)0xff&data.getData()[0]);
//	}
	
	void addData(byte data){
		byte[] a=new byte[1];
		a[0]=data;
		addData(a);
	}
	//因为编码的问题，不能采用new String的方式来先拼接后得到bytes
	/**
	 * 添加byte数组不会添加数组的大小
	 * @param data
	 */
	void addData(byte[] data){
		if(this.data==null){
			this.data=data;
		}else{
			byte[] a=new byte[this.data.length+data.length];
			for(int i=0;i<this.data.length;i++){
				a[i]=this.data[i];
			}
			for(int i=this.data.length;i<this.data.length+data.length;i++){
				a[i]=data[i-this.data.length];
			}
			this.data=a;
		}
	}
	void addData(int data){
		byte[] a=new byte[4];
		for(int i=0;i<4;i++){
			a[i]=(byte) (data%256);
			data=data/256;
		}
		addData(a);
	}
	/**
	 * 添加int或long数组都会先添加大小
	 * @param data
	 */
	void addData(int[] data){
		if(data==null){
			addData(0);
			return;
		}
		addData(data.length);
		for(int i:data){
			addData(i);
		}
	}
	/**
	 * 添加int或long数组都会先添加大小
	 * @param data
	 */
	void addData(long data){
		byte[] a=new byte[8];
		for(int i=0;i<8;i++){
			a[i]=(byte) (data%256);
			data=data/256;
		}
		addData(a);
	}
	
	byte[] getData(){
		return data;
	}
}
