import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.security.Key;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

//import org.apache.commons.io.IOUtils;


public class teste {
	
	private static String password;
	
	private static byte[] macFunction (String password,File rec_file) throws Exception{
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
	
	//nao sei se eh preciso guardar esta chave, mas se sim, 
	//provavelmente nao fiz da melhor maneira
	public static void createMAC(File rec_file) throws Exception {
		
		String loc = rec_file.getPath().split("\\.")[0];
		SecretKey key = new SecretKeySpec(password.getBytes(), "HmacSHA256");
		ObjectOutputStream oosk = new ObjectOutputStream
				(new FileOutputStream(loc + "macKey.key"));
		ObjectOutputStream oos = new ObjectOutputStream
				( new FileOutputStream(loc + ".mac") );
	    
		oos.write(macFunction(password,rec_file));
		
		byte[] keyEncoded = key.getEncoded();
		
		oosk.write(keyEncoded);
		oosk.flush();
		oos.flush();
		oosk.close();
		oos.close();
		
	}

	public static void main(String[] args) throws Exception {
		
		password = "teste123";
		File teste1 = new File("scTrab2.pdf");
		File teste2 = new File("a.txt");
		
		byte[] result1 = macFunction(password,teste1);
		//byte[] result2 = macFunction(password,teste2);
		
		//for(int i = 0; i < result1.length; i++) 
			//sb1.append(DatatypeConverter.printByte(result1[i]));
		
		String s1 = result1.toString();
		
		createMAC(teste2);
		
		
		
		
		
		//BufferedReader br = new BufferedReader(new FileReader("a.mac"));
		//String s2 = br.readLine();
		
		//byte[] result2 = IOUtils.toByteArray(new FileInputStream("a.mac"));
		byte[] fileBytes = Files.readAllBytes (new File("a.mac").toPath());
		byte[] result2 = Arrays.copyOfRange(fileBytes, 6, fileBytes.length);
		//for(int i = 0; i < result2.length; i++) 
			//sb2.append(DatatypeConverter.printByte(result2[i]));
		
		System.out.println(Arrays.equals(result1, result2));
		
		//String s2 = result2.toString();
		
		//boolean b = s1.equals(s2); 
		//System.out.println(b);
		
	}

}
