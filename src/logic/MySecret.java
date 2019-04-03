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
 * 用户的秘密类，包含了用户的公私钥信息，能够用公私钥进行加解密操作，以及用传入的公钥进行加密
 * @author Administrator
 *
 */
class MySecret {
	private byte[] publicKey;
	private byte[] privateKey;
	private SM2 sm2;
	@SuppressWarnings("deprecation")
	MySecret(byte[] userSecert) {
		
		
		//userSecert是用于产生公私钥对的，其本质是hash
        sm2=new SM2(userSecert);
		AsymmetricCipherKeyPair key = sm2.ecc_key_pair_generator.generateKeyPair();  
        ECPrivateKeyParameters ecpriv = (ECPrivateKeyParameters) key.getPrivate();  
        ECPublicKeyParameters ecpub = (ECPublicKeyParameters) key.getPublic();  
        //getD得到的是BigInteger
        privateKey = ecpriv.getD().toByteArray();
        
        //getQ得到的是ECPoint---来自新引入的包
        publicKey=ecpub.getQ().getEncoded();
        
	}
	
	byte[] getPublicKey(){
		return publicKey;
	}
	
	byte[] encrypt(byte[] message){
		return encryptWithOthers(message, publicKey);
	}
	
	/**
	 * 加密data数据
	 * @param data
	 * @return
	 */
	byte[] decrypt(byte[] data){
		//加密字节数组转换为十六进制的字符串 长度变为encryptedData.length * 2  
        String dataHexString = Util.byteToHex(data);  
        //System.out.println(dataHexString);
        /***分解加密字串 
         * （C1 = C1标志位2位 + C1实体部分128位 = 130） 
         * （C3 = C3实体部分64位  = 64） 
         * （C2 = encryptedData.length * 2 - C1长度  - C2长度） 
         */  
        byte[] c1Bytes = Util.hexToByte(dataHexString.substring(0,130));  
        int c2Len = data.length - 97;  
        byte[] c2 = Util.hexToByte(dataHexString.substring(130,130 + 2 * c2Len));  
        byte[] c3 = Util.hexToByte(dataHexString.substring(130 + 2 * c2Len,194 + 2 * c2Len));  
          
        BigInteger userD = new BigInteger(1, privateKey);  
          
        //通过C1实体字节来生成ECPoint  
        ECPoint c1 = sm2.ecc_curve.decodePoint(c1Bytes);  
        Cipher cipher = new Cipher();  
        cipher.Init_dec(userD, c1);  
        cipher.Decrypt(c2);  
        cipher.Dofinal(c3);  
          
        //返回解密结果  
        return c2;
	}
	
	/**
	 * 用新传入的otherKey公钥进行加密操作
	 * @param message
	 * @param otherKey 传入的公钥，65个byte的大小
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
        //C1 C2 C3拼装成加密字串  
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
