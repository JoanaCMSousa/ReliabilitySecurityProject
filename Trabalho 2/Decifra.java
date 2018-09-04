import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;

import java.security.Key;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

public class Decifra {

    public static void main(String[] args) throws Exception {
    	
    	ObjectInputStream fisKey = new ObjectInputStream (new FileInputStream("a.key"));
    	byte[] cipherKey = new byte[256];
    	
    	fisKey.read(cipherKey);
    	
    	FileInputStream kfile = new FileInputStream("keystore.dd");  //keystore
        KeyStore kstore = KeyStore.getInstance("JKS");
        kstore.load(kfile, "123456".toCharArray());           //password
        Key myPrivateKey = kstore.getKey("dd", "123456".toCharArray());
          	
        
        
        Cipher c2 = Cipher.getInstance("RSA");
        c2.init(Cipher.UNWRAP_MODE, myPrivateKey);
        Key keyCifrada = c2.unwrap(cipherKey, "AES", Cipher.SECRET_KEY); 

    	Cipher c = Cipher.getInstance("AES");
    	c.init(Cipher.DECRYPT_MODE, keyCifrada);
        
    	FileInputStream fis;
    	FileOutputStream fos;
    	CipherInputStream cis;
    
    	fis = new FileInputStream("test.cif");
    	fos = new FileOutputStream("result.pdf");

    	cis = new CipherInputStream(fis, c);
    	byte[] b = new byte[16];  
    	int i = cis.read(b);
    	while (i != -1) {
    		fos.write(b, 0, i);
    		i = cis.read(b);
    	}
    	cis.close();
    	fis.close();
    	fos.close();
    	fisKey.close();
    	
    }
    
    
    /* para o decifra
     * 1) ler o a.key
     * new do byte array 2048bits/8 ( = 256) 
     * chaveCifrada
     * 
     * 2)decifra a chave
     * 2.1) obter a chave privada -> keystore
     * 
     * FileInputStream kfile = new FileInputStream("keystore.dd");  //keystore
     * KeyStore kstore = KeyStore.getInstance("JKS");
     * kstore.load(kfile, "123456".toCharArray());           //password
     * Key myPrivateKey = kfile2.getKey("dd", "123456".toCharArray());
     * 
     * 2.2) decifrar
     * Cipher c2 = Cipher.getInstance("RSA");
     * c2.init(Cipher.UNWRAP_MODE, cert);
     * c2.wrap(key);
     * byte[] keyCifrada = c2.unwrap(chaveCifrada, "AES", Cipher.SECRET_KEY);  
     * 
     * 3) decifrar o ficheiro com key (o que esta feito na ultima aula)
     */
}