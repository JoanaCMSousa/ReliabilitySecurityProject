
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class ServerClientHandler {
	
	private String username;
	//private String passwd;
	private Messager msg;
	
	public ServerClientHandler(String username){
		this.username = username;
		msg = new Messager();
	}
	
	/**
	 * So recebe o pedido quando tem confirmacao que vai fazer
	 * NOTA: DE MANEIRA COMO ISTO ESTA FEITO NAO PODEMOS FAZER NADA DEPOIS
	 * IDEIA: LEMOS TUDO O QUE QUEREMOS LER E SO DEPOIS FAZEMOS O QUE QUEREMOS FAZER
	 * @param users
	 * @param outStream
	 * @param inStream 
	 * @param inStream
	 * @param pwd_in A password passada ao servidor ao inicio
	 */
	public int AddUser(userCatalog users,ObjectOutputStream outStream,
			ObjectInputStream inStream, SecurityHandler sc){
		try {
			//assim verificamos sempre, mesmo quando nao vamos trabalhar no pass
			if(sc.checkMAC(new File("passwords.txt")) == 0)
				throw new SecurityException();
			
			if(!users.existsUser(username)){
				msg.confirm(outStream);
				if(inStream.readShort() == 1){ //LER, confirmado pelo util
					String passwd = (String) inStream.readObject();
					users.addUser(username, passwd,sc);
					msg.confirm(outStream);
					return 1;
				}else{
					return -1;
				}
			//falta me confirmar
			}else{
				msg.reject(outStream); //temos de mudar a parte do login
				
				//enviar o nonce
				String nonce = sc.generateNonce();
				System.out.println("nonce = " +nonce);
				outStream.writeObject(nonce);
				outStream.flush();
				
				//receber a password hashada
				byte[] hashedPasswd = new byte[inStream.readInt()];
				inStream.read(hashedPasswd);
				int size = inStream.readInt();
				byte[] hashedNonce = new byte[size];
				inStream.read(hashedNonce);
				if(!Arrays.equals(hashedNonce, sc.hash(nonce))){
					System.out.println("Nonce diferente!");
				}
				
				//System.out.println("hashedNonce= " + hashedNonce);
				
				//temos de comparar os hash das duas
				if(!users.login(username, hashedPasswd,sc)){//nao consegui fazer login
					msg.reject(outStream);
					return -1;
				}
				msg.confirm(outStream);
				return 1;
			}
		}catch (SecurityException e){
			System.err.println("MAC ERRADO!!!!");
			msg.securityError(outStream);
			return 0;
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
		//ou dizer que deu erro
	}	
	
	/**
	 * Receive pull request from client
	 * @param inStream
	 * @param outStream
	 * @param reps
	 */
	public void push_rep(ObjectInputStream inStream,ObjectOutputStream outStream,
			repCatalog reps,SecurityHandler sc){
		try {
			String repname = fullNameRep((String) inStream.readObject());
			System.out.println("repname: " + repname);
			
			if(!new File(repname).exists() && isCreator(repname)){
				reps.addRep(repname, username,sc);
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
				long id;
				boolean hasHist = new File(totalName + ".1").exists();
				if((id = file.lastModified()) < date){
					msg.confirm(outStream);
					outStream.flush();
					addHist(totalName);
					file = msg.receiveFile(totalName, inStream);
					file.setLastModified(date);
					
					if(id == 0 && new File(totalName + ".1").exists())
						outStream.writeShort(1);
					else if(id == 0)
						outStream.writeShort(0);
					else
						outStream.writeShort(-1);
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
			
			if(!new File(fullName.split("/")[0] + "/" +
					fullName.split("/")[1]).exists()){
				msg.reject(outStream);
				return;
			}
			else
				msg.confirm(outStream);
			
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
	//preciso de ver se tem acesso! -Esquecime de fazer essa parte :P
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
				outStream.flush();
				if(inStream.readShort() == -1)
					return;
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
	
	public void share(ObjectOutputStream outStream,
			ObjectInputStream inStream,repCatalog reps,userCatalog users,
			SecurityHandler sc){
		System.out.println("Vamos fazer o share");
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
					int resp;
					if((resp=reps.addUser(username, myRep, userTo, sc)) == 1)
						outStream.writeInt(0);
					else if(resp == -1)
						outStream.writeInt(1);
					else{
						System.out.println("Mac do cliente errado");
						outStream.writeInt(-10);
					}
				}
				else
					outStream.writeInt(-1);
			}else
				outStream.writeInt(-1);
			
		} catch (SecurityException e) {
			throw new SecurityException();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public void remove(ObjectOutputStream outStream,
			ObjectInputStream inStream,repCatalog reps,userCatalog users,
			SecurityHandler sc){
		System.out.println("Vamos fazer remove");
		try {
			
			String myRep = (String) inStream.readObject();
			String userTo = (String) inStream.readObject();
			if(users.existsUser(userTo)){
				outStream.writeInt(1);
				if(new File(username + "/" + myRep).exists() && 
						reps.isCreator(username, myRep)){
					int resp;
					if((resp = reps.removeUser(username, userTo, myRep,sc)) == 1)
						outStream.writeInt(1);
					else if(resp == -1)
						outStream.writeInt(0);
					else{
						outStream.writeInt(-10);
						System.err.println("Mac errado!");
					}
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
	
	private boolean isCreator(String fullNameFile){
		return username.equals(fullNameFile.split("/")[0]);
	}
}
