
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
/**
 * Ideia, usar o ficheiro como o catalogo de utilizadores
 * Podemos usar User U ou 
 * String username && String password
 * @author Utilizador
 */
public class userCatalog {
	private static File passwords;
	public userCatalog(){
		passwords = new File("passwords.txt");
		try {
			passwords.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//ideia passamos tambem a password dada ao inicio
	
	/**
	 * Adiciona um utilizador ao ficheiro passwords
	 * @param username nome do utilizador a adicionar
	 * @param password password do utililizador a adicionar
	 * @throws IOException 
	 */
	public void addUser(String username, String password,SecurityHandler sc){
		StringBuilder Stb = new StringBuilder();
		try {
			Stb.append(username + ":" + password + System.getProperty("line.separator"));
			if(existsUser(username,sc))
				return;
			sc.decryptFile(passwords);
			FileWriter fw = new FileWriter(passwords,true);
			fw.write(Stb.toString());
			new File(username).mkdir(); //create user folder
			fw.close();
			sc.encrypthFile(passwords);
			sc.createMAC(passwords);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Acho que da para melhorar
	 * @param u
	 * @return
	 * @throws IOException 
	 */
	public boolean existsUser(String username,SecurityHandler sc){
		BufferedReader reader;
		try {
			sc.decryptFile(passwords);
			reader = new BufferedReader(new FileReader(passwords));
			//temos de decriptar, talvez linha a linha
			
			String line;
			while((line = reader.readLine()) != null){
				if(line.split(":")[0].equals(username)){
					reader.close();
					sc.encrypthFile(passwords);
					return true;
				}
			}
			reader.close();
			sc.encrypthFile(passwords);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean login(String username,byte[] passwordGiven,SecurityHandler sc){
		BufferedReader reader;
		try {
			sc.decryptFile(passwords);
			reader = new BufferedReader(new FileReader((passwords)));
			String line;
			while((line = reader.readLine()) != null){
				if(line.split(":")[0].equals(username)){
					byte[] gotPass = sc.hash(line.split(":")[1]);
					if(Arrays.equals(gotPass, passwordGiven)){
						reader.close();
						sc.encrypthFile(passwords);
						return true;
					}
					reader.close();
					sc.encrypthFile(passwords);
					return false;
				}
			}
			reader.close();
			sc.encrypthFile(passwords);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return false;
	}
}
