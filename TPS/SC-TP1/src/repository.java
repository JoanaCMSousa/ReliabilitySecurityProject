
import java.io.File;
import java.util.ArrayList;

/**
 * Ideia, vamos ver como fica , ACHO QUE NAO NECESSARIO< PELO MENOS NA PARTE DO CLIENTE
 * 
 * DUVIDA: Podemos ter mais do que um repositorio numa maquina?
 *   - SE SIM, talvez ter um arrayList de repositorios?
 *   - SE NAO, BASTA UM
 * 
 * Repositorio local (O que vai ser criado quando chamamos o -init)
 * @author Utilizador
 *
 */
public class repository {
	private String dirName;
	private User creator; //Criador (basicamente admin)
	private ArrayList<User> users; //basicamente os util que tem acceso
	
	public repository(User creator,String dirName){
		this.dirName = dirName;
		this.creator = creator;
		users= new ArrayList<User>();
	}
	
	public boolean init(){
		return new File(dirName).mkdir();
	}
}
