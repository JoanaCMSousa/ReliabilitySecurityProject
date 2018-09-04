import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.Mac;
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
	private PBEParameterSpec spec = new PBEParameterSpec(ivBytes, 20, ivSpec);
	
	private SecretKey key;
	
	String filename;
	String name;
	String extension;
	
	public SecurityHandler(String filename) {
		this.filename = filename;
		
		//a ideia de fazer deste split eh para este tipo de situacao
				//exemplo: "teste.txt" = "teste" "txt"
		String[] result = filename.split("\\.");
		name = result[0];
		extension = result[1];
	}

	
	public void createMAC(String password) throws Exception {
		
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(filename);
		Mac mac = Mac.getInstance("HmacSHA256");
		SecretKey key = new SecretKeySpec(password.getBytes(), "HmacSHA256");
		mac.init(key);
		
		//nao sei se eh preciso guardar esta chave, mas se sim, 
		//provavelmente nao fiz da melhor maneira
		
		FileOutputStream kos = new FileOutputStream(name + "macKey.key");
		ObjectOutputStream oosk = new ObjectOutputStream(kos);
		
		FileOutputStream fos = new FileOutputStream(name + ".mac");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		
		byte[] b = new byte[(int) file.length()];  
	    fis.read(b);
	    mac.update(b);
	    
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
	public int checkMAC(String password) 
			throws Exception{
		
		//verificar se existe mac
		File macToComp = new File(name+ ".mac");
		
		if(!macToComp.exists()) {
			System.out.println("O ficheiro nao tem um MAC.");
			return -1;
		}
		
		//para obter a chave secreta
		File macKey = new File(name + "macKey.key");
		ObjectInputStream fisMac = new ObjectInputStream(
				new FileInputStream(name + "macKey.key"));
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
	    
	    FileOutputStream fos = new FileOutputStream(name+ "1.mac");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		
		oos.write(mac.doFinal());
		
		oos.flush();
		oos.close();
	       
		fis.close();
		fisMac.close();
		
		File toComp = new File(name + "1.mac");
	    
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
	/*-------------------------------NOTA----------------------------------------
	 * Nesta funcao meto-o a criar o ficheiro cifrado à parte mas foi para testar
	 * se o decryptWithPassword estava a decifrar bem, se for para substituir o
	 * ficheiro original com o ficheiro cifrado, eh preciso fazer alteracoes tanto
	 * nesta funcao como a decryptWithPassword
	 *---------------------------------------------------------------------------
	 */
	
	
	//aqui trata-se da ecriptacao com a password
	public void encryptWithPassword (String password) throws Exception{


		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
		SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
		key = kf.generateSecret(keySpec); 

		// obs: o salt, count e iv não têm de ser definidos explicitamenta na cifra 
		// mas os mesmos valores têm de ser usados para cifrar e decifrar 
		// os seus valores podem ser obtidos pelo método getParameters da classe Cipher

		Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
		c.init(Cipher.ENCRYPT_MODE, key, spec);
		
		FileInputStream fis;
	    FileOutputStream fos;
	    CipherOutputStream cos;
	    
	    fis = new FileInputStream(filename);
	    //eh suposto substituir o ficheiro original?
	    //no enunciado diz que o ficheiro das passwords deve ser mantido cifrado por exemplo
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
	
	//aqui trata-se da decriptacao com a password
	public void decryptWithPassword () throws Exception{

        SecretKeySpec secretSpec = new SecretKeySpec
        		(key.getEncoded(), "PBEWithHmacSHA256AndAES_128S");

    	Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
    	c.init(Cipher.DECRYPT_MODE, secretSpec, spec);

    	
    	FileInputStream fis;
    	FileOutputStream fos;
    	CipherInputStream cis;
    
    	fis = new FileInputStream(name+".cif");
    	fos = new FileOutputStream(name + "2." + extension);

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
	
	//síntese da mensagem
	
	public void hash() throws Exception {
		
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(filename);
		
		FileOutputStream fos = new FileOutputStream(name + ".hash");
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		
		byte[] read = new byte[(int)file.length()];
		fis.read(read);
		
		oos.writeObject(md.digest(read));
		
		oos.flush();
		fis.close();
		fos.close();
		
	}
	//Nao gosto da maneira como estah feito, mas estah a funcionar, tentem melhorar
	//devolve 1 -> eh valido
	//devolve 0 -> nah eh valido
	public int checkHash() throws Exception{
		
		//FileInputStream fis = new FileInputStream(result[0] + ".hash");
		File fileHash = new File(name + ".hash");
		//ObjectInputStream ois = new ObjectInputStream(fis);
		
		byte[] b = Files.readAllBytes(fileHash.toPath());
		
		
		
		FileInputStream fisOrig = new FileInputStream(filename);
		File fileOrig = new File(filename);
		
		FileOutputStream fos = new FileOutputStream(name + "1.hash");
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		
		byte[] read = new byte[(int)fileOrig.length()];
		fisOrig.read(read);
		
		oos.writeObject(md.digest(read));
		
		oos.flush();
		oos.close();
		
		//FileInputStream fis1 = new FileInputStream(result[0] + "1.hash");
		File fileHash1 = new File(name + "1.hash");
		//ObjectInputStream ois = new ObjectInputStream(fis);
		
		byte[] b1 = Files.readAllBytes(fileHash1.toPath());
		fileHash1.delete();
		
		//byte[] orig = new byte[(int) fileOrig.length()];
		//fisOrig.read(orig);
		
	//	MessageDigest md = MessageDigest.getInstance("SHA-256");
		//byte[] test = md.digest(orig);
		
		//fis.close();
		fisOrig.close();
		
		if(MessageDigest.isEqual(b, b1))
			return 1;
		
		else
			return 0;
		
	}
	
	public void signature() throws Exception{

		File file = new File(filename);
		FileInputStream fis = new FileInputStream(filename);
		
		FileOutputStream fos = new FileOutputStream(name + ".sig");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		
		PrivateKey pkey = null; //como obter?
		
		Signature s = Signature.getInstance("SHA-256");
		s.initSign(pkey);
		
		byte[] buf = new byte[(int) file.length()];
		fis.read(buf);
		
		s.update(buf);
		
		oos.writeObject(s.sign());
		
		oos.flush();
		fis.close();
		fos.close();
		oos.close();
	}
	
	
}
