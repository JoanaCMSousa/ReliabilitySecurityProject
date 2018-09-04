/** Grupo sc005
 * Francisco João Guimarães Coimbra de Almeida Araújo nº45701
 * Joana Correia Magalhães Sousa nº47084
 * João Marques de Barros Mendes Leal nº46394
 */


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
 */
public class repCatalog{
	
	private String fileName;
	
	public repCatalog(String fileName){
		this.fileName = fileName;
	}
	
	/**
	 * Cria um novo repositorio
	 * @param repName - nome do repositorio a ser criado
	 * @param Creator - nome do criador do repositorio a ser criado
	 * @throws IOException
	 */
	public void addRep(String repName,String Creator) throws IOException{
		new File(repName).mkdir();
		new File(Creator + "/" + fileName).createNewFile();
		
	}
	
	/**
	 * Adiciona um utilizador ah lista daqueles que o criador partilha o seu repositorio
	 * @param sharer - Nome do criador quer partilhar o seu repositorio 
	 * @param repName - Nome do repositorio que o criador quer partilhar 
	 * @param userToShare - O nome do utilizador a partilhar com
	 * @throws IOException
	 */
	public boolean addUser(String sharer,String repName,
			String userToShare) throws IOException{
		return addUserToFile(sharer,userToShare,repName);
	}
	
	/**
	 * Funcao auxiliar do addUser
	 * @param sharer - Nome do criador quer partilhar o seu repositorio 
	 * @param userToShare - O nome do utilizador a partilhar com
	 * @param repName - Nome do repositorio que o criador quer partilhar 
	 * @return true se a funcao foi feit com sucesso; false caso contrario
	 */
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
	
	/**
	 * Funcao que indica se um utilizador eh criador de um dado repositorio
	 * @param sharer - nome de utilizador a avaliar
	 * @param repName - nome do repositorio a ser utilizado para fazer a verificacao
	 * @return true se sharer eh criador do repositorio repName; false caso contario
	 */
	public boolean isCreator(String sharer,String repName){
		return new File(sharer + "/" + repName).exists();
	}
	
	/**
	 * Funcao que remove um utilizador da lista daqueles que o criador quer 
	 * partilhar o seu repositorio
	 * @param repName - nome do repositorio ao qual o utilizador vai ser removido
	 * @param remover - nome do criador do repositorio
	 * @param user - nome do utilizador a ser removido
	 */
	 public boolean  removeUser(String sharer,String userToRemove,
	            String repName) throws IOException{
		 File file = new File(sharer + "/"  +fileName);
			File tempFile = new File(sharer + "/myTempFile.txt");
			try {
				boolean wasThere = false;
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
								wasThere = true;
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
			      
			      return wasThere;
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
	    }
	
	 /**
	  * Funcao que indica se um certo utilizador tem acesso a um dado repositorio
	  * @param repName - nome do repositorio
	  * @param userToLook - nome do utilizador que vamos avaliar
	  * @return true se userToLook tem acesso ao repName; false caso contrario
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

	/**
	 * Funcao auxiliar de hasAccess
	 * @param file - nome do ficheiro que se encontra no repositorio
	 * @param repName - nome do repositorio
	 * @param user - nome do utilizador a ser avaliado
	 * @return true se user tem acesso a repName; false caso contrario
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

	/**
	 * Funcao que indica a data da ultima vez que o ficheiro foi alterado
	 * @param user - nome do criador do repositorio
	 * @param myRep - nome do repositorio onde se encontra o ficheiro
	 * @param file - nome do ficheiro que queremos verificar a data
	 * @return a data em que o ficheiro foi modificado, -1 caso nao exista
	 */
	public long lastModified(String user,String myRep,String file){
		File fl = new File(user+"/" +myRep + "/" + file);
		return fl.exists() ? fl.lastModified() : -1;
	}
	
}
