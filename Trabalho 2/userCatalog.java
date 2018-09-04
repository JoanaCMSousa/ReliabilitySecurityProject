
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
		Stb.append(username + ":" + password + System.getProperty("line.separator"));
		
		try {
			if(existsUser(username))
				return;
			
			FileWriter fw = new FileWriter(passwords,true);
			fw.write(Stb.toString());
			new File(username).mkdir(); //create user folder
			fw.close();
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
	public boolean existsUser(String username){
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(passwords));
			String line;
			while((line = reader.readLine()) != null){
				if(line.split(":")[0].equals(username)){
					reader.close();
					return true;
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean login(String username,byte[] passwordGiven,SecurityHandler sc){
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader((passwords)));
			String line;
			while((line = reader.readLine()) != null){
				if(line.split(":")[0].equals(username)){
					byte[] gotPass = sc.hash(line.split(":")[1]);
					if(Arrays.equals(gotPass, passwordGiven)){
						reader.close();
						return true;
					}
					reader.close();
					return false;
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return false;
	}
}
