/* Grupo sc005
 * Francisco João Guimarães Coimbra de Almeida Araújo nº45701
 * Joana Correia Magalhães Sousa nº47084
 * João Marques de Barros Mendes Leal nº46394
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.IOException;
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

	/**
	 * Adiciona um utilizador ao ficheiro passwords
	 * @param username - nome do utilizador a adicionar
	 * @param password - password do utililizador a adicionar
	 * @throws IOException 
	 */
	public void addUser(String username, String password){
		StringBuilder Stb = new StringBuilder();
		Stb.append(username + ":" + password + System.getProperty("line.separator"));
		if(existsUser(username))
			return;
		try {
			FileWriter fw = new FileWriter(passwords,true);
			fw.write(Stb.toString());
			new File(username).mkdir(); //create user folder
			fw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Funcao que indica se existe um utilizador
	 * @param username - nome do utilizador que vamos avaliar
	 * @return true se o utilizador username existe; false caso contrario
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
	
	/**
	 * Funcao que trata do login do utilizador
	 * @param username - nome do utilizador
	 * @param passwordGiven - password do utilizador
	 * @return true se o login foi feito com sucesso; false caso contrario
	 */
	public boolean login(String username,String passwordGiven){
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader((passwords)));
			String line;
			while((line = reader.readLine()) != null){
				if(line.split(":")[0].equals(username)){
					if(line.split(":")[1].equals(passwordGiven)){
						reader.close();
						return true;
					}
					return false;
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
