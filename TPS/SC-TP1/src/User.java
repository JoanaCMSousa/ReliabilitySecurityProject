
/**
 * Um draft do utilizador para fazer? Nao sei se necessario 
 * Talvez suficiente para fazer no ficheiro?
 * @author Utilizador
 *
 */
public class User {
	private String username;
	private String password;
	
	public User(String username,String password){
		this.username = username;
		this.password = password;
	}
	
	public void setUserName(String username){this.username = username;}
	public void setPassword(String password){this.password = password;}
	public String getUserName(){return username;}
	public String getPassword(){return password;}
	
	/**
	 * Ideia meter o utilizador actual no ficheiro especifico
	 * @return
	 */
}
