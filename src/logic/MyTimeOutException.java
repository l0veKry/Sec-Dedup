package logic;

/**
 * ��ʾ�ͻ������ڳ�ʱ��û�����Ѷ��ߵ��쳣
 * @author Administrator
 *
 */
public class MyTimeOutException extends Exception{
	/**
	 * 20170302
	 */
	private static final long serialVersionUID = 1L;
	String reason;
	MyTimeOutException(){
		super();
		reason="time out";
	}
}
