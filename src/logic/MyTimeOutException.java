package logic;

/**
 * 表示客户端由于长时间没操作已断线的异常
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
