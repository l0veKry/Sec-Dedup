package logic;


/**
 * ����������ݵ��࣬���Բ��ϵ���addData���������µ����ݣ�������getData�����õ�һ��byte
 * ���飬��ҪΪ�����byte����ķ���
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
	//��Ϊ��������⣬���ܲ���new String�ķ�ʽ����ƴ�Ӻ�õ�bytes
	/**
	 * ���byte���鲻���������Ĵ�С
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
	 * ���int��long���鶼������Ӵ�С
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
	 * ���int��long���鶼������Ӵ�С
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
