package logic;

import java.math.BigInteger;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;

import Cipher.Cipher;
import Cipher.SM2;
import Cipher.Util;

/**
 * �û��������࣬�������û��Ĺ�˽Կ��Ϣ���ܹ��ù�˽Կ���мӽ��ܲ������Լ��ô���Ĺ�Կ���м���
 * @author Administrator
 *
 */
class MySecret {
	private byte[] publicKey;
	private byte[] privateKey;
	private SM2 sm2;
	@SuppressWarnings("deprecation")
	MySecret(byte[] userSecert) {
		
		
		//userSecert�����ڲ�����˽Կ�Եģ��䱾����hash
        sm2=new SM2(userSecert);
		AsymmetricCipherKeyPair key = sm2.ecc_key_pair_generator.generateKeyPair();  
        ECPrivateKeyParameters ecpriv = (ECPrivateKeyParameters) key.getPrivate();  
        ECPublicKeyParameters ecpub = (ECPublicKeyParameters) key.getPublic();  
        //getD�õ�����BigInteger
        privateKey = ecpriv.getD().toByteArray();
        
        //getQ�õ�����ECPoint---����������İ�
        publicKey=ecpub.getQ().getEncoded();
        
	}
	
	byte[] getPublicKey(){
		return publicKey;
	}
	
	byte[] encrypt(byte[] message){
		return encryptWithOthers(message, publicKey);
	}
	
	/**
	 * ����data����
	 * @param data
	 * @return
	 */
	byte[] decrypt(byte[] data){
		//�����ֽ�����ת��Ϊʮ�����Ƶ��ַ��� ���ȱ�ΪencryptedData.length * 2  
        String dataHexString = Util.byteToHex(data);  
        //System.out.println(dataHexString);
        /***�ֽ�����ִ� 
         * ��C1 = C1��־λ2λ + C1ʵ�岿��128λ = 130�� 
         * ��C3 = C3ʵ�岿��64λ  = 64�� 
         * ��C2 = encryptedData.length * 2 - C1����  - C2���ȣ� 
         */  
        byte[] c1Bytes = Util.hexToByte(dataHexString.substring(0,130));  
        int c2Len = data.length - 97;  
        byte[] c2 = Util.hexToByte(dataHexString.substring(130,130 + 2 * c2Len));  
        byte[] c3 = Util.hexToByte(dataHexString.substring(130 + 2 * c2Len,194 + 2 * c2Len));  
          
        BigInteger userD = new BigInteger(1, privateKey);  
          
        //ͨ��C1ʵ���ֽ�������ECPoint  
        ECPoint c1 = sm2.ecc_curve.decodePoint(c1Bytes);  
        Cipher cipher = new Cipher();  
        cipher.Init_dec(userD, c1);  
        cipher.Decrypt(c2);  
        cipher.Dofinal(c3);  
          
        //���ؽ��ܽ��  
        return c2;
	}
	
	/**
	 * ���´����otherKey��Կ���м��ܲ���
	 * @param message
	 * @param otherKey ����Ĺ�Կ��65��byte�Ĵ�С
	 * @return
	 */
	byte[] encryptWithOthers(byte[] message,byte[] otherKey){
		byte[] source = new byte[message.length];  
        System.arraycopy(message, 0, source, 0, message.length);  
          
        Cipher cipher = new Cipher();  
        
        ECPoint userKey = sm2.ecc_curve.decodePoint(otherKey);  
        ECPoint c1 = cipher.Init_enc(sm2, userKey);  
        cipher.Encrypt(source);  
        byte[] c3 = new byte[32];  
        cipher.Dofinal(c3);  
          
//      System.out.println("C1 " + Util.byteToHex(c1.getEncoded()));  
//      System.out.println("C2 " + Util.byteToHex(source));  
//      System.out.println("C3 " + Util.byteToHex(c3));  
        //C1 C2 C3ƴװ�ɼ����ִ�  
        //System.out.println(Util.byteToHex(c1.getEncoded())+Util.byteToHex(source)+Util.byteToHex(c3));
        byte[] temp=new byte[129];
        int i=0;
        @SuppressWarnings("deprecation")
		byte[] tc1=c1.getEncoded();
        for(;i<tc1.length;i++)
        	temp[i]=tc1[i];
        for(;i<tc1.length+source.length;i++)
        	temp[i]=source[i-tc1.length];
        for(;i<tc1.length+source.length+c3.length;i++)
        	temp[i]=c3[i-tc1.length-source.length];
        
		return temp;
	}
//	public static void main(String[] args){
//		 System.out.println(Math.ceil((double)((double)16/16))+1);
//		 System.out.println((int)(15/16+2));
////		MySecret a=new MySecret(new byte[32]);
////		System.out.println(Util.byteToHex(a.encrypt(new byte[32])));
////		//System.out.println(a.decrypt(a.encrypt(new byte[32])));//129
////		System.out.println(a.getPublicKey().length);65
//	}
}
