
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

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
			
			if(!users.existsUser(username,sc)){
				System.out.println("O user nao existe");
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
				msg.reject(outStream);
				
				//enviar o nonce
				String nonce = sc.generateNonce();
				System.out.println("nonce = " +nonce);
				outStream.writeObject(nonce); //escrevos uma string
				outStream.flush();
				
				//receber a password hashada
				int size = inStream.readInt();
				byte[] hashedPasswd = new byte[size]; //lemos um inteiro
				inStream.read(hashedPasswd); //lemos um array de bytes
				size = inStream.readInt(); //receboms um inteiro
				byte[] hashedNonce = new byte[size];
				inStream.read(hashedNonce); //recemos um array de bytes
				if(!Arrays.equals(hashedNonce, sc.hash(nonce))){
					System.out.println("Nonce diferente!");
				}
				
				//temos de comparar os hash das duas
				if(!users.login(username, hashedPasswd,sc)){//nao consegui fazer login
					msg.reject(outStream);
					return -1;
				}
				System.out.println("Fez o login bem");
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
			repCatalog reps,SecurityHandler sc,PublicKey pubKey){
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
			
			if(!hasAccess(reps, repname, username, outStream,sc))
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
				if((id = file.lastModified()) < date){
					msg.confirm(outStream);
					byte[] sign = new byte[inStream.readInt()];
					inStream.read(sign);
					setSignature(sign,totalName + ".sig"); //nao sei se esta a dar bem
					SecretKey key = (SecretKey) inStream.readObject();
					addHist(totalName);
					file = msg.receiveFile(totalName,inStream);
					file.setLastModified(date);
					setKey(key,pubKey,totalName);
					
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
			
			File[] files = new File(repname).listFiles( new FileFilter(){
				@Override
			    public boolean accept(File pathname) {
					char lastChar = pathname.getName().charAt(
							(int) (pathname.getName().length() - 1));
			        return !allAddedFiles.contains(pathname) &&
			        		!Character.isDigit(lastChar)&& !pathname.getName().contains(".sig")
			        		&& !pathname.getName().contains(".key.server");
			    }
			});
			outStream.writeShort(files.length);
			String totalName;
			for(File fl:files){
				outStream.writeObject(fl.getName());
				totalName = repname +"/"+fl.getName();
				addHist(totalName); //tenho que testar
				fl.delete();
				File sig = new File(fl.getPath() + ".sig");
				File keyServer = new File(fl.getPath() + ".key.server");
				if(sig.exists()) 
					sig.delete();
				if(keyServer.exists()) 
					keyServer.delete();
				
			}
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public void push_file(repCatalog reps,ObjectInputStream inStream,
			ObjectOutputStream outStream,SecurityHandler sc,PublicKey pubKey){
		String endOfKeyServ = ".key.server";
		try {
			String filename = (String) inStream.readObject();
			String fullName = fullNameFile(filename);
			
			File file = new File(fullName);
			
			if(!hasAccess(reps, fullName, username, outStream,sc))
				return;
			
			if(!new File(fullName.split("/")[0] + "/" +
					fullName.split("/")[1]).exists()){
				msg.reject(outStream);
				return;
			}
			else
				msg.confirm(outStream);
			
			if(inStream.readShort() == -1)
				return;
			
			long date = inStream.readLong();
			if(file.lastModified() < date){
				msg.confirm(outStream);
				//acho que tenho de enviar passos a passos
				byte[] sign = new byte[inStream.readInt()];
				inStream.read(sign);
				setSignature(sign,fullName + ".sig"); //nao sei se esta a dar bem
				SecretKey key = (SecretKey) inStream.readObject();
				addHist(fullName);
				file = msg.receiveFile(fullName,inStream);
				file.setLastModified(date);
				setKey(key,pubKey,fullName);
			}
			else
				msg.reject(outStream);
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	private void setSignature(byte[] sign,String filename) throws IOException{
		byte[] fileBytes = new byte[1024];
		File file = new File(filename);
		if(file.exists())
			file.delete();
		FileOutputStream fos = new FileOutputStream(filename);
		int fileSize = sign.length;
		fos.write(fileBytes, 0, fileSize);
		fos.close();
	}
	
	//ciframos com a chave publica do servidor
	private void setKey(SecretKey key,PublicKey pubKey,String filename) throws Exception{
		String end = ".key.server";
		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.ENCRYPT_MODE, pubKey);
		byte[] cipherData = cipher.doFinal(key.getEncoded());
		File fl = new File(filename + end);
		if(fl.exists())
			fl.delete();
		
		FileOutputStream fos = new FileOutputStream(filename + end);
		fos.write(cipherData, 0, cipherData.length);
		fos.close();
	}
	
	//preciso de ver se tem acesso! -Esquecime de fazer essa parte :P
	public void pull(repCatalog rep,ObjectOutputStream outStream,
			ObjectInputStream inStream,SecurityHandler sc,PrivateKey privKey){
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
				pull_rep(file, totalName, rep, outStream, inStream,sc,privKey);
			}
			//ver se esta em formato file
			else if(file.isFile()){
				outStream.writeShort(0); //confirmar
				outStream.flush();
				if(inStream.readShort() == -1)
					return;
				if(!hasAccess(rep, totalName, username, outStream,sc)){
					return;
				}
				if(msg.notModified(file,outStream, inStream)){
					File keyServer = new File(file.getPath() + ".key.server");
					byte[] decKey = getKey(privKey, keyServer);
					msg.sendFile(file, outStream);
					outStream.writeInt(decKey.length);
					outStream.write(decKey);
					//msg.sendFile(new File(file.getPath() + ".sig"), outStream);
				}
			}
			else
				outStream.writeShort(-1);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	private byte[] getKey(PrivateKey priKey,File file) throws Exception{
		Cipher decrypt = Cipher.getInstance("RSA");
		decrypt.init(Cipher.DECRYPT_MODE, priKey);
		byte[] encryptedArray = new byte[(int) file.length()];
		FileInputStream fis = new FileInputStream(file);
		fis.read(encryptedArray); //read file into bytes[]
		fis.close();
		return decrypt.doFinal(encryptedArray);
	}
	
	public void pull_rep(File file,String totalName,repCatalog rep,
			ObjectOutputStream outStream,ObjectInputStream inStream,SecurityHandler sc,
			PrivateKey privKey){
		if(!hasAccess(rep, totalName, username, outStream,sc)){
			return;
		}
		File[] files = file.listFiles( new FileFilter(){
			@Override
		    public boolean accept(File pathname) {
				char lastChar = pathname.getName().charAt(
						pathname.getName().length() - 1);
		        return !Character.isDigit(lastChar) && !pathname.getName().contains(".sig") 
		        		&& !pathname.getName().contains(".key.server");
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
				if(msg.notModified(fl,outStream, inStream)){
					File keyServer = new File(fl.getPath() + ".key.server");
					byte[] decKey = getKey(privKey, keyServer);
					msg.sendFile(fl, outStream);
					outStream.writeInt(decKey.length);
					outStream.write(decKey);
					//msg.sendFile(new File(file.getPath() + ".sig"), outStream);
				}
				sendFiles.add(fl.getName()); //nao sei se aqui ou dentro do notmod
			}
			//Envio o nome do primeiro historico dos que nao foram enviados
			
			File[] histFiles = file.listFiles( new FileFilter(){
				@Override
			    public boolean accept(File pathname) {
					System.out.println(pathname);
					if(pathname.getName().contains(".sig") || pathname.getName().contains(".key.server"))
						return false;
					char lastChar = pathname.getName().charAt(
							(int) (pathname.getName().length() - 1));
					String[] allDots = pathname.getName().split("\\.");
					System.out.println(pathname.getName());
					String fileActualName;
					if(allDots.length < 2)
						fileActualName = allDots[0];
					else
						fileActualName = allDots[0] + "." + allDots[1];
			        return lastChar == '1' && !sendFiles.contains(fileActualName);
			    }
			});
			
			outStream.writeInt(histFiles.length);
			for(File fl : histFiles){
				String[] allDots = fl.getName().split("\\.");
				String fileActualName = allDots[0] + "." + allDots[1];
				outStream.writeObject(fileActualName);
			}
			
		}catch (Exception e) {
			// TODO Auto-generated catch block
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
				
			if(users.existsUser(userTo,sc)){
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
			if(users.existsUser(userTo,sc)){
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
			String username,ObjectOutputStream outStream,SecurityHandler sc){
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
			if(!reps.hasAccess(folderNames[0] +"/"+ folderNames[1], username,sc)){
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
