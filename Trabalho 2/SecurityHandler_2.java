import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
//import javax.xml.bind.DatatypeConverter;


public class SecurityHandler {

	//tenho duvidas se eh suposto fazer isto
	//se calhar seria melhor devolver a chava que foi criada em vez de void
	public void createMAC(String password, String filename) throws Exception {
		
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(filename);
		Mac mac = Mac.getInstance("HmacSHA256");
		SecretKey key = stringToKey(password);
		mac.init(key);
		
		//a ideia de fazer deste split eh para este tipo de situacao
		//exemplo: "teste.txt" = "teste" "txt"
		String [] result = filename.split("\\.");
		
		//nao sei se eh preciso guardar esta chave, mas sim, 
		//provavelmente nao fiz da melhor maneira
		
		FileOutputStream kos = new FileOutputStream(result[0] + "macKey.key");
		ObjectOutputStream oosk = new ObjectOutputStream(kos);
		
		FileOutputStream fos = new FileOutputStream(result[0] + ".mac");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		
		byte[] b = new byte[(int) file.length()];  
	    fis.read(b);
	    mac.update(b);
	    
	    /*String test = "";
	    
	    for(int i = 0; i < b.length; i++) {
	    	String a = DatatypeConverter.printByte(b[i]);
	    	test = test.concat(a);
	    }
	    
		oos.writeObject(test);*/
		oos.write(mac.doFinal());
		
		byte[] keyEncoded = key.getEncoded();
		
		oosk.write(keyEncoded);
		oosk.flush();
		oos.flush();
		
		kos.close();
		//oosk.close();
		
		fos.close();
		fis.close();
		//oos.close();
		

	}
	
	// devolve 1 -> eh valido
	// devolve 0 -> nao eh valido
	// devolve -1 -> nao existe
	//isto provavelmente nao esta correto
	//estah a funcionar, no entanto, acho que dah para fazer melhor
	public int checkMAC(String password, String filename) 
			throws Exception{
		
		String[] result =  filename.split("\\.");
		
		//verificar se existe mac
		File macToComp = new File(result[0]+ ".mac");
		
		if(!macToComp.exists()) {
			System.out.println("O ficheiro nao tem um MAC.");
			return -1;
		}
		
		//para obter a chave secreta
		File macKey = new File(result[0] + "macKey.key");
		ObjectInputStream fisMac = new ObjectInputStream(
				new FileInputStream(result[0] + "macKey.key"));
		byte [] keyEncoded = new byte[(int) macKey.length()];
		fisMac.read(keyEncoded);
		
		SecretKeySpec key = new SecretKeySpec(keyEncoded, "HmacSHA256");
		
		//criar um novo mac para comparar com o original
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(filename);
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(key);
		
		byte[] b = new byte[(int)file.length()];  
	    fis.read(b);
	    mac.update(b);
	    
	   /* String test = "";
	   
	    for(int i = 0; i < b.length; i++) {
	    	test = test.concat(DatatypeConverter.printByte(b[i]));
	    }*/
	    
	    FileOutputStream fos = new FileOutputStream(result[0] + "1.mac");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		
		//oos.writeObject(test);
		oos.write(mac.doFinal());
		
		oos.flush();
		oos.close();
	       
		fis.close();
		fisMac.close();
		
		File toComp = new File(result[0] + "1.mac");
	    
	    //comparacao dos macs
	    byte[] macToTest  = Files.readAllBytes(toComp.toPath());
	    byte[] originalMac = Files.readAllBytes(macToComp.toPath());
	    
	    if(originalMac.length != macToTest.length)
	    	return 0;
				
	   if(!Arrays.equals(originalMac,macToTest))
		   return 0;

	   toComp.delete();
	    
		return 1;
		
	}
	
	//aqui trata-se da ecriptacao
	public void encryptWithPassword (String password, String filename) throws Exception{


		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
		SecretKeyFactory kf = SecretKeyFactory.getInstance("AES");
		SecretKey key = kf.generateSecret(keySpec); 

		// obs: o salt, count e iv não têm de ser definidos explicitamenta na cifra 
		// mas os mesmos valores têm de ser usados para cifrar e decifrar 
		// os seus valores podem ser obtidos pelo método getParameters da classe Cipher

		byte[] ivBytes = {0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,
		0x19,0x1A,0x1B,0x1C,0x1D,0x1E,0x1F,0x20};
		IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
		PBEParameterSpec spec = new PBEParameterSpec(ivBytes, 20, ivSpec);

		Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.ENCRYPT_MODE, key, spec);
		
		FileInputStream fis;
	    FileOutputStream fos;
	    CipherOutputStream cos;
	    
	    fis = new FileInputStream(filename);
	    //eh suposto substituir o ficheiro original?
	    //no enunciado diz que o ficheiro das passwords deve ser mantido cifrado por exemplo
	    String [] result = filename.split("\\.");
	    fos = new FileOutputStream(result[0] + ".cif"); 

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
	
	private static SecretKey stringToKey(String s) {
		return new SecretKeySpec(s.getBytes(), "HmacSHA256");
	}
	
	
}
