
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Cada utilizador vai ter no seu dir de util um ficheiro, que nao pode ser acedido, que
 * dita quem pode ver que repositorios no seu dir do genero
 * 
 * <repositorio>:<util1;util2;util3>
 * 
 * @author Utilizador
 *
 */
public class repCatalog{
	
	private String fileName;
	
	public repCatalog(String fileName){
		this.fileName = fileName;
	}
	
	/**
	 * CRIAR UM NOVO REPOSITORIO
	 * @param repName
	 * @param Creator
	 * @throws IOException
	 */
	public void addRep(String repName,String Creator,SecurityHandler sc) throws IOException{
		new File(repName).mkdir();
		File usersFile = new File(Creator + "/" + fileName);
		usersFile.createNewFile();
		try {
			sc.createMAC(usersFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param sharer Eh o tipo que quer partilhar
	 * @param repName O nome do repositorio que queremos partilhar (fullName)
	 * @param userToShare O utilizador a partilhar com
	 * @throws IOException
	 */
	public short addUser(String sharer,String repName,
			String userToShare,SecurityHandler sc) throws IOException{
		try {
			System.out.println("Estamos no add user");
			if(sc.checkMAC(new File(sharer+"/"+fileName)) == 0){
				System.out.println("Erro no mac!!!!!!!!");
				return 0;
			}
			if(addUserToFile(sharer,userToShare,repName)){
				sc.createMAC(new File (sharer+ "/" +fileName)); //nao esta a fazer o update
				return 1;
			}
			sc.createMAC(new File (sharer+ "/" +fileName)); //nao esta a fazer o update
			
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
			
		return -1;
	}
	
	private boolean addUserToFile(String sharer,String userToShare,String repName){
		File file = new File(sharer + "/"  +fileName);
		File tempFile = new File(sharer + "/myTempFile.txt");
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
			boolean isArleadyThere = false,foundRep = false;
			String currentLine;
			String[] line,users;
			StringBuilder userLine = new StringBuilder();
			while((currentLine = reader.readLine()) != null) {
				line = currentLine.split(":");
				if(line[0].equals(repName)){
					foundRep = true;
					users = line[1].split(";");
					for(String user:users){
						if(user.equals(userToShare))
							isArleadyThere = true;
						userLine.append(user + ";");
					}
					if(!isArleadyThere)
						userLine.append(userToShare+";");
					userLine.deleteCharAt(userLine.length() - 1);
					writer.write(repName +":"+userLine.toString()+
							System.getProperty("line.separator"));
				}
				else
					writer.write(currentLine + System.getProperty("line.separator"));
			}
			if(!foundRep)
				writer.write(repName +":"+userToShare + System.getProperty("line.separator"));
			writer.close(); 
			reader.close(); 
			if(!file.delete()) 
		        System.out.println("Could not delete file");
		      //Rename the new file to the filename the original file had.
		     if (!tempFile.renameTo(file))
		    	 System.out.println("Could not rename file");
		      
		      return isArleadyThere;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean isCreator(String sharer,String repName){
		return new File(sharer + "/" + repName).exists();
	}
	
	/**
	 * Este vai ser o mais heavy
	 * @param repName
	 * @param remover
	 * @param user
	 * @throws SecurityException 
	 */
	 public int  removeUser(String sharer,String userToRemove,
	            String repName,SecurityHandler sc){
		 	File file = new File(sharer + "/"  +fileName);
			File tempFile = new File(sharer + "/myTempFile.txt");
			try {
				if(sc.checkMAC(file) == 0)
					return 0;
				
				int wasThere = -1;
				BufferedReader reader = new BufferedReader(new FileReader(file));
				BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
				String currentLine;
				String[] line,users;
				StringBuilder userLine = new StringBuilder();
				while((currentLine = reader.readLine()) != null) {
					line = currentLine.split(":");
					if(line[0].equals(repName)){
						users = line[1].split(";");
						for(String user:users){
							if(user.equals(userToRemove)) {
								wasThere = 1;
							} else
								userLine.append(user + ";");
						}
						if(userLine.length() > 0){
							userLine.deleteCharAt(userLine.length() - 1);
							writer.write(repName +":"+userLine.toString()+
									System.getProperty("line.separator"));
						}
					}
					else
						writer.write(currentLine + System.getProperty("line.separator"));
				}
				writer.close(); 
				reader.close(); 
				if(!file.delete()) 
			        System.out.println("Could not delete file");
			      
			      //Rename the new file to the filename the original file had.
			      if (!tempFile.renameTo(file))
			        System.out.println("Could not rename file");
			      
			      sc.createMAC(file);
			      
			      return wasThere;
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return -1;
	    }
	
	//TODO: MAIS IMPORTANTE A FAZER
	 /**
	  * 
	  * @param repName
	  * @param userToLook
	  * @return
	  * @throws IOException
	  * @requires File repName exists
	  */
	public boolean hasAccess(String repName, String userToLook) throws IOException{
		System.out.println("Entrou no hasAcess");
		String username = repName.split("/")[0];
		System.out.println("repName: " + repName);
		File file = new File(username+"/"+fileName);
		return hasAccess(file,repName,userToLook);
	}
	//TODO
	/**
	 * falta passar o criador, senao dois dir com o mesmo nome vamos ter acesso a eles
	 * Passamos por todos as linhas no ficheiro e vemos se tem
	 * @param file
	 * @param repName
	 * @param user
	 * @return
	 */
	private boolean hasAccess(File file,String repName,String user){
		try {
			System.out.println("repName: " + repName);
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line;
			while((line = br.readLine()) != null){
				System.out.println(line);
				String[] folderNames = repName.split("/");
				if(line.split(":")[0].equals(folderNames[1]) ){
					String[] users = line.split(":")[1].split(";");
					for(String fileUser:users){
						if(fileUser.equals(user)){
							fr.close();
							br.close();
							return true;
						}
					}
				}
			}
			fr.close();
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	//RETORNA A DATA EM QUE FOI MUDIFICADO OU -1 caso nao exista
	public long lastModified(String user,String myRep,String file){
		File fl = new File(user+"/" +myRep + "/" + file);
		return fl.exists() ? fl.lastModified() : -1;
	}
	
}
