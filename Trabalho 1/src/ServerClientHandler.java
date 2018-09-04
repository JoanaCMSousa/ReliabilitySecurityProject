/** Grupo sc005
 * Francisco João Guimarães Coimbra de Almeida Araújo nº45701
 * Joana Correia Magalhães Sousa nº47084
 * João Marques de Barros Mendes Leal nº46394
 */


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class ServerClientHandler {
	
	private String username;
	private String passwd;
	private Messager msg;
	
	public ServerClientHandler(String username,String passwd){
		this.username = username;
		this.passwd = passwd;
		msg = new Messager();
	}
	
	/**
	 * Adiciona um utilizador a uma lista de utilizadores que se encontra no servidor
	 * @param users - lista de utilizadores onde se vai adicionar o novo
	 * @param outStream - objeto por onde escreve ao servidor
	 * @param inStream  - objeto por onde le ao servidor
	 */
	public boolean AddUser(userCatalog users,ObjectOutputStream outStream,
			ObjectInputStream inStream){
		try {
			if(!users.existsUser(username)){
				msg.confirm(outStream);
				if(inStream.readShort() == 1){ //LER, confirmado pelo util
					users.addUser(username, passwd);
					msg.confirm(outStream);
					return true;
				}else{
					return false;
				}
			//falta me confirmar
			}else{
				msg.reject(outStream);
				if(!users.login(username, passwd)){//nao consegui fazer login
					msg.reject(outStream);
					return false;
				}
				msg.confirm(outStream);
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
		//ou dizer que deu erro
	}	
	
	/**
	 * Funcao que apos receber o pedido de push de um repositorio, faz o push de todos os 
	 * ficheiros que se encontram do repositorio local para o repositorio do servidor com 
	 * o mesmo nome
	 * @param inStream - objeto por onde le ao servidor
	 * @param outStream - objeto por onde escreve ao servidor
	 * @param reps - lista dos repositorios que se encontram no servidor
	 */
	public void push_rep(ObjectInputStream inStream,ObjectOutputStream outStream,
			repCatalog reps){
		try {
			String repname = fullNameRep((String) inStream.readObject());
			System.out.println("repname: " + repname);
			
			if(!new File(repname).exists() && isCreator(repname)){
				reps.addRep(repname, username);
				msg.confirm(outStream);
			}
			else if(!new File(repname).exists() && !isCreator(repname)){
				outStream.writeShort(0);
				return;
			}
			else
				msg.reject(outStream); //ja exisita
			
			if(!hasAccess(reps, repname, username, outStream))
					return;
			int size = inStream.readInt(); //receber o num dos ficheiros
			
			final ArrayList<File> allAddedFiles = new ArrayList<File>();
			for(int i = 0; i < size; i++){
				outStream.flush();
				String filename = (String) inStream.readObject();
				long date = inStream.readLong();
				String totalName = repname + "/" +filename.split("/")[1];
				File file = new File(totalName);
				allAddedFiles.add(file);
				if(file.lastModified() < date){
					msg.confirm(outStream);
					addHist(totalName);
					file = msg.receiveFile(totalName, inStream);
					file.setLastModified(date);
				}
				else
					outStream.writeShort(-1);
			}
			//so falta avizar
			File[] files = new File(repname).listFiles( new FileFilter(){
				@Override
			    public boolean accept(File pathname) {
					char lastChar = pathname.getName().charAt(
							(int) (pathname.getName().length() - 1));
			        return !allAddedFiles.contains(pathname) &&
			        		!Character.isDigit(lastChar);
			    }
			});
			outStream.writeShort(files.length);
			String totalName;
			for(File fl:files){
				outStream.writeObject(fl.getName());
				totalName = repname +"/"+fl.getName();
				addHist(totalName); //tenho que testar
				fl.delete();
			}
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
	/**
	 * Funcao que trata de fazer o push de um ficheiro para um repositorio especifico 
	 * no servidor
	 * @param reps - lista de repositorios
	 * @param inStream - objeto por onde escreve ao servidor
	 * @param outStream - objeto por onde le ao servidor
	 */
	public void push_file(repCatalog reps,ObjectInputStream inStream,
			ObjectOutputStream outStream){
		try {
			String filename = (String) inStream.readObject();
			System.out.println("FileName: " + filename);
			String fullName = fullNameFile(filename);
			System.out.println("FullName: " +fullName);
			File file = new File(fullName);
			
			
			if(!hasAccess(reps, fullName, username, outStream))
				return;
	
			long date = inStream.readLong();
			if(file.lastModified() < date){
				msg.confirm(outStream);
				addHist(fullName);
				file = msg.receiveFile(fullName,inStream);
				file.setLastModified(date);
			}
			else
				msg.reject(outStream);
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		} 
	}

	/**
	 * Funcao de trata de fazer pull de um ficheiro que se encontra num repositorio no servidor
	 * para o repositorio local com o mesmo nome
	 * @param rep - nome do repositorio de onde se vai fazer o pull dos seu ficheiros
	 * @param outStream - objeto por onde escreve ao servidor
	 * @param inStream - objeto por onde le ao servidor
	 */
	public void pull(repCatalog rep,ObjectOutputStream outStream,
			ObjectInputStream inStream){
		String filename;
		File file;
		try {
			filename = (String) inStream.readObject();//nome
			
			String totalName;
			if(filename.split("/").length == 1)
				totalName = username + "/" + filename;
			else if(filename.split("/").length == 2){
				File test= new File(username +"/"+filename);
				if(test.exists() && test.isFile())
					totalName = username + "/" + filename;
				else
					totalName = filename;
			}
			else
				totalName = filename;
			
			file = new File(totalName);
			//ver se esta em formato folder
			if(file.isDirectory()){ //ver se eh diretoria
				System.out.println("EH DIRETORIA");
				outStream.writeShort(1);//dir
				pull_rep(file, totalName, rep, outStream, inStream);
			}
			//ver se esta em formato file
			else if(file.isFile()){
				outStream.writeShort(0); //confirmar
				if(!hasAccess(rep, totalName, username, outStream)){
					return;
				}
				msg.basic_push(file,outStream, inStream);
			}
			else
				outStream.writeShort(-1);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Funcao de trata de fazer pull de todos os ficheiros que se encontram num repositorio
	 * especifico no servidor para o repositorio local com o mesmo nome
	 * @param file - o repositorio onde vamos buscar os ficheiros
	 * @param totalName - nome do diretorio do repositorio
	 * @param rep - lista de repositorios que se encontram no servidor
	 * @param outStream - objeto por onde escreve ao servidor
	 * @param inStream - objeto por onde le ao servidor
	 */
	public void pull_rep(File file,String totalName,repCatalog rep,
			ObjectOutputStream outStream,ObjectInputStream inStream){
		if(!hasAccess(rep, totalName, username, outStream)){
			return;
		}
		File[] files = file.listFiles( new FileFilter(){
			@Override
		    public boolean accept(File pathname) {
				char lastChar = pathname.getName().charAt(
						pathname.getName().length() - 1);
		        return !Character.isDigit(lastChar);
		    }
		}); //nao ha subdir
		try {
			if(totalName.split("/")[0].equals(username))
				outStream.writeBoolean(true); //eh o nosso util a fazer o push?
			else
				outStream.writeBoolean(false);
			outStream.writeInt(files.length);
			final ArrayList<String> sendFiles = new ArrayList<>();
			for(File fl:files){
				outStream.writeObject(fl.getName()); //enviar o nome
				msg.basic_push(fl,outStream, inStream);
				sendFiles.add(fl.getName());
			}
			//Envio o nome do primeiro historico dos que nao foram enviados
			
			File[] histFiles = file.listFiles( new FileFilter(){
				@Override
			    public boolean accept(File pathname) {
					char lastChar = pathname.getName().charAt(
							(int) (pathname.getName().length() - 1));
					String[] allDots = pathname.getName().split("\\.");
					System.out.println(pathname.getName());
					String fileActualName = allDots[0] + "." + allDots[1];
			        return lastChar == '1' && !sendFiles.contains(fileActualName);
			    }
			});
			
			outStream.writeInt(histFiles.length);
			for(File fl : histFiles){
				String[] allDots = fl.getName().split("\\.");
				String fileActualName = allDots[0] + "." + allDots[1];
				outStream.writeObject(fileActualName);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Funcao que trata da organizacao do acesso de um utilizador a um repositorio
	 * @param outStream - objeto por onde escreve ao servidor
	 * @param inStream - objeto por onde le ao servidor
	 * @param reps - lista de repositorios que se encontram no servidor
	 * @param users - lista de utilizadores que se econtram no servidor
	 */
	public void share(ObjectOutputStream outStream,
			ObjectInputStream inStream,repCatalog reps,userCatalog users){
		try {
			String myRep = (String) inStream.readObject();
			String userTo = (String) inStream.readObject();
			if(userTo.equals(username)){
				msg.reject(outStream);
				return;
			}
			else
				msg.confirm(outStream);
				
			if(users.existsUser(userTo)){
				outStream.writeInt(1); //first confirm
				if(new File(username + "/" + myRep).exists() &&
						reps.isCreator(username, myRep)){
					if(reps.addUser(username, myRep, userTo))
						outStream.writeInt(0);
					else
						outStream.writeInt(1);
				}
				else
					outStream.writeInt(-1);
			}else
				outStream.writeInt(-1);
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Funcao que trata de remover a permissao de acesso de um utilizador a um repositorio 
	 * especifico
	 * @param outStream - objeto por onde escreve ao servidor
	 * @param inStream - objeto por onde le ao servidor
	 * @param reps - lista de repositorios que se encontram no servidor
	 * @param users - lista de utilizadores que se encontram no servidor
	 */
	public void remove(ObjectOutputStream outStream,
			ObjectInputStream inStream,repCatalog reps,userCatalog users){
		System.out.println("Vamos fazer remove");
		try {
			String myRep = (String) inStream.readObject();
			String userTo = (String) inStream.readObject();
			if(users.existsUser(userTo)){
				outStream.writeInt(1);
				if(new File(username + "/" + myRep).exists() && 
						reps.isCreator(username, myRep)){
					if(reps.removeUser(username, userTo, myRep))
						outStream.writeInt(1);
					else
						outStream.writeInt(0);
				}
				else
					outStream.writeInt(-1);
			}else
				outStream.writeInt(-1);
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Funcao que trata do historico de um ficheiro
	 * @param filename - nome do ficheiro onde vamos tratar do historico
	 */
	public void addHist(String filename){
		if(new File(filename).exists()){
			boolean found = false;
			for(int i = 1 ;!found;i++ ){
				File file = new File(filename + "." + i);
				if(!file.exists()){
					new File(filename).renameTo(file);
					found = true;
				}
			}
		}
		
	}
	
	/**
	 * Funcao que devolve a diretoria de um ficheiro
	 * @param filename - nome do ficheiro onde vamos descobrir o historico
	 * @return o diretorio do ficheiro com nome filename
	 */
	private String fullNameFile(String filename){
		String[] allFiles = filename.split("/");
		if(allFiles.length == 1)
			return filename;
		else if(allFiles.length == 2){
			
			return username + "/" +filename;
		}
		else
			return filename;
	}
	
	//Se o ficheiro for um folder
	/**
	 * Funcao que devolve o diretorio de um repositorio
	 * @param filename - nome do repositorio
	 * @return o diretorio do repositorio
	 */
	private String fullNameRep(String filename){
		String[] allFiles = filename.split("/");
		
		System.out.println("InFullNameRepFunc arg0 = " + filename);
		
		if( allFiles.length == 1 ){
			return username + "/" + filename;
		}
		else if(allFiles.length == 2){
			return filename;
		}
		else
			return filename;
	}
	
	/**
	 * Funcao que indica se um utilizador tem acesso a um repositorio especifico
	 * @param reps - lista de repositorios que se encontram no servidor
	 * @param fullName - diretorio do repositorio
	 * @param username - nome do utilizador que vamos avaliar
	 * @param outStream - objeto por onde escreve ao servidor
	 * @return true se o utilizador username tem acesso ao repositorio com o diretorio fullName;
	 * false caso contrario
	 */
	private boolean hasAccess(repCatalog reps,String fullName,
			String username,ObjectOutputStream outStream){
		String[] folderNames = fullName.split("/");
		try {
			
			if(fullName.split("/")[1].equals("users.txt") ||
					fullName.split("/")[1].equals("..")){
				msg.reject(outStream);
				return false;
			}
			if(isCreator(fullName)){
				msg.confirm(outStream);
				return true;
			}
			if(!reps.hasAccess(folderNames[0] +"/"+ folderNames[1], username)){
				msg.reject(outStream);
				return false;
			}
			msg.confirm(outStream);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Funcao que indica se um utilizador eh criador de um repositorio
	 * @param fullNameFile - diretorio do repositorio 
	 * @return true se o utilizador username eh criador do repositorio com o diretorio 
	 * fullNameFile; false caso contrario
	 */
	private boolean isCreator(String fullNameFile){
		return username.equals(fullNameFile.split("/")[0]);
	}
}
