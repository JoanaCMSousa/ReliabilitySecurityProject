import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;



public class SecurityHandler {
	
	private byte[] ivBytes = {0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,
			0x19,0x1A,0x1B,0x1C,0x1D,0x1E,0x1F,0x20};
	private IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
	private PBEParameterSpec spec = new PBEParameterSpec(ivBytes, 20, ivSpec); //esta a dar mal no linux
	private SecretKey key;
	
	private String password;
	
	public SecurityHandler(String password) {
		this.password = password;
	}

	private byte[] macFunction (String password,File rec_file) throws Exception{
		File file = new File(rec_file.getPath());
		Mac mac = Mac.getInstance("HmacSHA256");
		SecretKey key = new SecretKeySpec(password.getBytes(), "HmacSHA256");
		mac.init(key);
		FileInputStream fis = new FileInputStream(rec_file.getPath());
		byte[] b = new byte[(int) file.length()];  
	    fis.read(b);
	    mac.update(b);
	    fis.close();
		return mac.doFinal();
	}
	

	public void createMAC(File rec_file) throws Exception {
		
		String loc = rec_file.getPath().split("\\.")[0];
		SecretKey key = new SecretKeySpec(password.getBytes(), "HmacSHA256");
		ObjectOutputStream oosk = new ObjectOutputStream(new FileOutputStream(loc + "macKey.key"));
		ObjectOutputStream oos = new ObjectOutputStream( new FileOutputStream(loc + ".mac") );
	    
		oos.write(macFunction(password,rec_file));
		
		byte[] keyEncoded = key.getEncoded();
		
		oosk.write(keyEncoded);
		oosk.flush();
		oos.flush();
		oosk.close();
		oos.close();
		
	}
	
	// devolve 1 -> eh valido
	// devolve 0 -> invalido
	//TODO
	public int checkMAC(File rec_file) 
			throws Exception{
		String name = rec_file.getPath().split("\\.")[0];
		String loc = rec_file.getPath();
		File macToComp = new File(name+ ".mac");
		
		//para obter a chave secreta
		File macKey = new File(name + "macKey.key");
		ObjectInputStream fisMac = new ObjectInputStream(
				new FileInputStream(name + "macKey.key"));
		byte [] keyEncoded = new byte[(int) macKey.length()];
		fisMac.read(keyEncoded);
		
		SecretKeySpec key = new SecretKeySpec(keyEncoded, "HmacSHA256");
		
		//criar um novo mac para comparar com o original
		
		byte[] newMac = macFunction(password,rec_file);  
	  
		fisMac.close();
	    
	    //comparacao dos macs
	    byte[] fileBytes = Files.readAllBytes(macToComp.toPath());
	    byte[] originalMac = Arrays.copyOfRange(fileBytes, 6, fileBytes.length);
	    
	    if(originalMac.length != newMac.length)
	    	return 0;
				
	   if(!Arrays.equals(originalMac,newMac))
		   return 0;

	    
		return 1;
		
	}
	
	
	public SecretKey getRandomSecretKey() throws Exception{
		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
		SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
		return kf.generateSecret(keySpec); 
	}
	
	public SecretKey getKeyFromArray(byte[] key){
		return new SecretKeySpec(key,0,key.length,"PBEWithHmacSHA256AndAES_128");
	}
	
	public void SendEncryptWithPassword(File file,SecretKey key,Messager msg,ObjectOutputStream out){
		
		Cipher c;
		try {
			c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
			c.init(Cipher.ENCRYPT_MODE, key, spec);
			
			FileInputStream fis;
		    FileOutputStream fos;
		    CipherOutputStream cos;
		    
		    fis = new FileInputStream(file);
		   
		    fos = new FileOutputStream(file.getName() + ".cif"); 

		    cos = new CipherOutputStream(fos, c);
		    byte[] b = new byte[1024];  
		    int i = fis.read(b);
		    while (i != -1) {
		        cos.write(b, 0, i);
		        i = fis.read(b);
		    }
		    cos.close();
		    fis.close();
		    File fl = new File(file.getName() + ".cif");
		    msg.sendFile(fl, out);
		    
		    fl.delete();
		    
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//aqui trata-se da ecriptacao com a password
	public void encryptWithPassword (File rec_file) throws Exception{
		String name = rec_file.getName().split("\\.")[0];
		
		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
		SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
		key = kf.generateSecret(keySpec); 

		Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
		c.init(Cipher.ENCRYPT_MODE, key, spec);
		
		FileInputStream fis;
	    FileOutputStream fos;
	    CipherOutputStream cos;
	    
	    fis = new FileInputStream(rec_file.getName());
	    fos = new FileOutputStream(name + ".cif"); 

	    cos = new CipherOutputStream(fos, c);
	    byte[] b = new byte[16];  
	    int i = fis.read(b);
	    while (i != -1) {
	        cos.write(b, 0, i);
	        i = fis.read(b);
	    }
	    cos.close();
	    fis.close();  
	}
	//TODO TOTEST
	public void decryptFile(File file,SecretKey key) throws Exception{
		Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
		c.init(Cipher.DECRYPT_MODE,key,spec);
		String path = file.getPath();
		FileInputStream fis = new FileInputStream(file);
		FileOutputStream fos = new FileOutputStream(path + ".temp");;
		CipherInputStream cis = new CipherInputStream(fis, c);
		File newFile = new File(path + ".temp"); 	
		byte[] b = new byte[16];  
	    int i = cis.read(b);
	    while (i != -1) {
	        fos.write(b, 0, i);
	        i = cis.read(b);
	    }
	    
	    fis.close();
		fos.close();
		cis.close();
		
		if(!file.delete())
			System.out.println("Nao apagou");
		newFile.renameTo(file);
	}
	
	public void encrypthFile(File file,SecretKey key) throws Exception{
		Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
		c.init(Cipher.ENCRYPT_MODE,key,spec);
		String path = file.getPath();
		FileInputStream fis = new FileInputStream(file);
		FileOutputStream fos = new FileOutputStream(path + ".temp");;
		CipherInputStream cis = new CipherInputStream(fis, c);
		File newFile = new File(path + ".temp"); 	
		byte[] b = new byte[16];  
	    int i = cis.read(b);
	    while (i != -1) {
	        fos.write(b, 0, i);
	        i = cis.read(b);
	    }
	    
	    fis.close();
		fos.close();
		cis.close();
		
		if(!file.delete())
			System.out.println("Nao apagou");
		newFile.renameTo(file);
	}
	
	public String encryptLine (String line) throws Exception{
		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
		SecretKeyFactory kf;
		if(line == null)
			return null;
		kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
		key = kf.generateSecret(keySpec);
		Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
		c.init(Cipher.ENCRYPT_MODE, key, spec);
		return new String(c.doFinal(line.getBytes()));
		
	}
	
	public void encrypthFile(File file) throws Exception{
		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
		SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
		key = kf.generateSecret(keySpec); 
		Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
		c.init(Cipher.ENCRYPT_MODE,key,spec);
		String path = file.getPath();
		FileInputStream fis = new FileInputStream(file);
		FileOutputStream fos = new FileOutputStream(path + ".temp");;
		CipherInputStream cis = new CipherInputStream(fis, c);
		File newFile = new File(path + ".temp"); 	
		byte[] b = new byte[16];  
	    int i = cis.read(b);
	    while (i != -1) {
	        fos.write(b, 0, i);
	        i = cis.read(b);
	    }
	    
	    fis.close();
		fos.close();
		cis.close();
		
		if(!file.delete())
			System.out.println("Nao apagou");
		newFile.renameTo(file);
	}
	//acho que esta a dar mal
	public void decryptFile(File file) throws Exception{
		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
		SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
		key = kf.generateSecret(keySpec);
		SecretKeySpec secretSpec = new SecretKeySpec
        		(key.getEncoded(), "PBEWithHmacSHA256AndAES_128");
		Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
		c.init(Cipher.DECRYPT_MODE,secretSpec,spec);
		String path = file.getPath();
		FileInputStream fis = new FileInputStream(file);
		FileOutputStream fos = new FileOutputStream(path + ".temp");;
		CipherInputStream cis = new CipherInputStream(fis, c);
		File newFile = new File(path + ".temp"); 	
		byte[] b = new byte[16];  
	    int i = cis.read(b);
	    while (i != -1) {
	        fos.write(b, 0, i);
	        i = cis.read(b);
	    }
	    
	    fis.close();
		fos.close();
		cis.close();
		
		if(!file.delete())
			System.out.println("Nao apagou");
		newFile.renameTo(file);
	}
	
	//aqui trata-se da decriptacao com a password
	public void decryptWithPassword (File rec_file) throws Exception{
		
		String []names = rec_file.getName().split("\\.");
        SecretKeySpec secretSpec = new SecretKeySpec
        		(key.getEncoded(), "PBEWithHmacSHA256AndAES_128");

    	Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
    	c.init(Cipher.DECRYPT_MODE, secretSpec, spec);

    	
    	FileInputStream fis;
    	FileOutputStream fos;
    	CipherInputStream cis;
    
    	fis = new FileInputStream(names[0]+".cif");
    	fos = new FileOutputStream(names[0] + "2." + names[1]);

    	cis = new CipherInputStream(fis, c);
    	byte[] b = new byte[16];  
    	int i = cis.read(b);
    	while (i != -1) {
    		fos.write(b, 0, i);
    		i = cis.read(b);
    	}
    	fos.flush();
    	
    	cis.close();
    	fis.close();
    	fos.close();	
	}
	
	//TODO //TOTEST
	/*
	 * Escolhe 130 bits de um random criptograficamente seguro
	 */
	public String generateNonce(){
		SecureRandom random = new SecureRandom();
		return new BigInteger(130,random).toString();
	}
	
	//síntese da mensagem
	//TODO //TOTEST
	
	public void hash(File rec_file) throws Exception {
		
		File file = new File(rec_file.getName());
		FileInputStream fis = new FileInputStream(rec_file.getName());
		
		FileOutputStream fos = new FileOutputStream(rec_file.getName().split("\\.")[0] + ".hash");
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		
		byte[] read = new byte[(int)file.length()];
		fis.read(read);
		
		oos.writeObject(md.digest(read));
		
		oos.flush();
		fis.close();
		fos.close();	
	}
	
	public byte[] hash (String value) throws NoSuchAlgorithmException{
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		return md.digest(value.getBytes(StandardCharsets.UTF_8));
	}
	
	
	//TODO //TOTEST //nao sei se esta a dar bem
	public byte[] signature(File M,PrivateKey privKey) throws Exception{
		
		Signature s = Signature.getInstance("SHA256withRSA");
		s.initSign(privKey);
		
		FileInputStream fis = new FileInputStream(M);
		BufferedInputStream bufin = new BufferedInputStream(fis);
		byte[] buffer = new byte[1024];
		int len;
		while ((len = bufin.read(buffer)) >= 0) 
		    s.update(buffer, 0, len);
		
		bufin.close();
		fis.close();
		
		return s.sign();
	}
		
}
