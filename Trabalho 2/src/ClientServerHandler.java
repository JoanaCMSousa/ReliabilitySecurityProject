
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Scanner;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
/**
 * A ideia desta classe eh tratar das interaccoes do myGit com o myGitServer
 * @author Utilizador
 *
 */
public class ClientServerHandler {
	
	private static final int PUSH_REP = 20;
	private static final int PUSH_FILE = 30;
	private static final int PULL = 40;
	private static final int SHARE = 50;
	private static final int REMOVE = 60;
	
	private String username;
	private String passwd;
	private Messager msg;
	
	public ClientServerHandler(String username,String passwd){
		this.username = username;
		this.passwd = passwd;
		msg = new Messager();
	}
	
	public void sendInitInfo(ObjectOutputStream outStream){
		try {
			outStream.writeObject(username);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Comando especifico em caso de o utilizador quiser criar um novo
	 * NOTA: DE MANEIRA COMO ISTO ESTA FEITO NAO PODEMOS FAZER NADA DEPOIS
	 * IDEIA: ENVIAMOS TUDO O QUE QUEREMOS ENVIAR E SO DEPOIS FAZEMOS O QUE QUEREMOS FAZER
	 * @param outStream
	 * @param inStream
	 */
	public int addUser(ObjectOutputStream outStream, ObjectInputStream inStream,SecurityHandler sc){
		try {
			short resp;
			outStream.flush();
			if((resp = inStream.readShort()) == 1){ //se nao existir
				System.out.println("--O utilizador " + username + "vai ser criado");
				System.out.println("Confirmar password do utilizador " + username);
				Scanner reader = new Scanner(System.in);
				String comp = reader.nextLine();
				reader.close();
				if( passwd.equals(comp)){
					msg.confirm(outStream);
					//enviamos a password normalmente
					outStream.writeObject(passwd);
					outStream.flush();
					if(inStream.readShort() == 1){ //ler
						System.out.println("--O utilizador " +username+ " foi"
								+ " criado com sucesso");
						
						return 1;
					}
					else{
						System.out.println("--Ouve um erro na criacao"
								+ " do utilizador");
						return -1;
					}
				}
				else{
					msg.reject(outStream);
					System.out.println("Erro:Ocorreu um erro a criar o utilizador");
					return -1;
				}
			}
			else if (resp == -1){ //lemos o reject
				
				//aqui recebemos o nonce
				String nonce = (String) inStream.readObject(); //lemos uma string
				System.out.println("Nonce = " + nonce);
				
				//enviamos a password hashada
				byte[] hashedPasswd = sc.hash(passwd);
				byte[] hashedNonce = sc.hash(nonce);
				outStream.writeInt(hashedPasswd.length); //enviamos um inteiro
				outStream.write(hashedPasswd); //ebnviamos um array de bytes
				outStream.writeInt(hashedNonce.length); //enviamos um inteiro
				outStream.write(hashedNonce); //enviamos um array de bytes
				outStream.flush();
				
				if(inStream.readShort() == -1){
					System.out.println("Erro:Password errada!");
					return -1;
				}
				return 0;
			}else
				System.out.println("Erro de Seguranca no servidor!");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	//push de um ficheiro
	//fazemos push para com o nome do repositorio onde estamos
	public void push_file(File file,String filename,
			ObjectOutputStream outStream, ObjectInputStream inStream,
			SecurityHandler sc,PrivateKey privKey){
		try {
			System.out.println("Vamos enviar um fichiero");
			outStream.writeInt(PUSH_FILE);
			outStream.writeObject(filename);
			outStream.flush();
			
			
			if(inStream.readShort() == -1){
				System.out.println("Erro: Nao tem permissao para entrar nesse ficheiro");
				return;
			}
			if(inStream.readShort() == -1){
				System.out.println("Erro: Repositorio nao existe");
				return;
			}
			//enviar ambos
			if(filename.contains(".sig") || filename.contains(".key.server")){
				System.out.println("Nao pode enviar .sig ou .key.server");
				msg.reject(outStream);
				return;
			}
			msg.confirm(outStream);
				
			
			if(msg.notModified(file,outStream, inStream)){
				//enviar a assinatura
				byte[] sg = sc.signature(file, privKey);
				outStream.writeInt(sg.length);
				outStream.write(sg);
				SecretKey key = sc.getRandomSecretKey();
				outStream.writeObject(key);
				sc.SendEncryptWithPassword(file,key,msg,outStream);
				
				System.out.println("-- O ficheiro " + file.getName() + " foi"
						+ " enviado para o servidor");
			}
			else
				System.out.println("-- O ficheiro " + file.getName() + "ja "
						+ "se encontra actualizado no servidor");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//Provavelmente ha maneira mais simples
	//se o ficheiro existe no servidor mas nao existe no local temos de apagalo do serv
	public void push_rep(File rep,String repName,ObjectOutputStream outStream,
			ObjectInputStream inStream,SecurityHandler sc,PrivateKey privKey){
		try {
			outStream.writeInt(PUSH_REP);
			int total = 0;
			outStream.writeObject(repName); 
			short resp;
			if((resp = inStream.readShort()) == 1){
				total ++;
				
				System.out.println("-- O repositorio " +repName+" foi criado no"
						+ " servidor");
			}
			else if(resp == 0){
				System.out.println("Erro: nao pode criar repositorios "
						+ "nas paginas de outros utilizadores");
				return;
			}
			
			File[] files = rep.listFiles( new FileFilter(){
				@Override
			    public boolean accept(File pathname) {
			        return pathname.isFile();
			    }
			});
			
			outStream.flush();
			if(inStream.readShort() == -1){
				System.out.println("Erro: Nao tem acesso a esse repositorio");
				return;
			}
			int size = files.length;
			outStream.writeInt(size); //numero de ficheiros que vamos enviar
			outStream.flush();
			for(File fl: files){
				outStream.writeObject(rep.getName() + "/" +fl.getName());
				outStream.flush();
				if(msg.notModified(fl,outStream, inStream)){
					byte[] sg = sc.signature(fl, privKey);
					outStream.writeInt(sg.length);
					outStream.write(sg);
					SecretKey key = sc.getRandomSecretKey();
					outStream.writeObject(key);
					sc.SendEncryptWithPassword(fl,key,msg,outStream);
					total ++;
					short caso = inStream.readShort();
					if(caso == 1)
						System.out.println("-- O ficheiro " + fl.getName() +
								" foi adicionado ao servidor mas ja existia historico");
					else if(caso == 0)
						System.out.println("-- O ficheiro " + fl.getName() +
								" vai ser adicionado ao servidor");
					else
						System.out.println("-- O ficheiro " + fl.getName() +
								" foi actualizado no servidor");
				}
			} //end of for
			short delSize = inStream.readShort();
			for(int i = 0; i < delSize; i++){
				try {
					total ++;
					System.out.printf("-- O ficheiro %s foi apagado do servidor\n"
							,(String)inStream.readObject());
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			if (total == 0)
				System.out.println("-- Nao foi mudificado nada no lado do"
						+ " servidor");
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//pull de um ficheiro
	//Melhorar, esta muito feio
	public void pull(String filename,
			ObjectOutputStream outStream, ObjectInputStream inStream,
			SecurityHandler sc){
		try {
			outStream.writeInt(PULL);
			
			outStream.writeObject(filename);
			outStream.flush();
			int answ;
			long date;
			if((answ = inStream.readShort()) == 1) //DIRETORIO
				pull_rep(filename, inStream, outStream,sc);
				
			else if(answ == 0){
				
				System.out.println(filename.substring(0,filename.lastIndexOf("/")));
				if(!new File(filename.substring(0,filename.lastIndexOf("/"))).exists()){
					System.out.println("Erro:Diretorio nao encontrado");
					msg.reject(outStream);
					return;
				}
				else
					msg.confirm(outStream);
				
				if(inStream.readShort() == -1){
					System.out.println("Erro: Nao tem acesso a esse diretorio/ficheiro");
					return;
				}
				date = inStream.readLong();
				String fl = filename.split("/")[filename.split("/").length - 1];
				if(lastModified(filename) < date){
					msg.confirm(outStream);
					File file = msg.receiveFile(filename,inStream);
					byte[] Key = new byte[inStream.readInt()];
					inStream.read(Key);
					sc.decryptFile(file, sc.getKeyFromArray(Key));
					file.setLastModified(date);
					//File sign = msg.receiveFile(filename + ".sig", inStream);
					System.out.println("-- O ficheiro " + fl +
							" foi copiado do servidor");
				}
				else{
					System.out.println("-- O ficheiro " +
							fl + " encountra se actualizado");
					outStream.writeShort(-1);
				}
			}
			else
				System.out.println("Erro: Ficheiro nao encontrado");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void pull_rep(String filename,ObjectInputStream inStream,
			ObjectOutputStream outStream,SecurityHandler sc) throws Exception{
		if(inStream.readShort() == -1){
			System.out.println("Erro: nao tem acesso a esse diretorio");
			return;
		}
		
		boolean ourRep = inStream.readBoolean();
		if(!ourRep && !new File(filename).exists())
			System.out.printf("-- O repositorio %s do utilizador %s foi"
					+ " copiado do servidor\n",filename.split("/")[0],username);
		long date;
		int total = 0,size = inStream.readInt();
		String fl,totalName;
		File file;
		final ArrayList<String> recNames = new ArrayList<String>();
		for(int i = 0; i < size; i++){
			fl = (String) inStream.readObject();
			totalName = getTotalName(filename,fl);
			recNames.add(totalName.split("/")[totalName.split("/").length -1]);
			date = inStream.readLong();
			if(lastModified(totalName) < date){
				msg.confirm(outStream);
				file = msg.receiveFile(totalName,inStream);
				byte[] Key = new byte[inStream.readInt()];
				inStream.read(Key);
				sc.decryptFile(file, sc.getKeyFromArray(Key));
				file.setLastModified(date);
				//File sign = msg.receiveFile(filename + ".sig", inStream);
				if(ourRep)
					System.out.println("-- Copiamos o ficheiro " + fl + " do servidor");
				total ++;
			}
			else{
				msg.reject(outStream);
			}
		}
		if(!ourRep)
			System.out.println("-- Copiamos o repositorio do servidor");
		size = inStream.readInt();
		for(int i = 0; i < size; i ++){
			String name = (String) inStream.readObject();
			totalName = getTotalName(filename,name);
			if(new File(totalName).exists()){
				System.out.println("-- O ficheiro " + name + " existe"
						+ " localmente mas foi eliminado do servidor");
				total++;
			}
		}
		if(total == 0)
			System.out.println("-- Nenhuma alteracao a informar");
	}
	
	private String getTotalName(String filename,String fl){
		if(filename.split("/").length == 1){
			new File(filename).mkdir();
			return filename + "/" + fl;
		}
		if(filename.split("/").length == 2){
			String[] folderNames = filename.split("/");
			if(folderNames[0].equals(username)){
				new File(filename).mkdir();
				return folderNames[1] +"/"+fl;
			}
			
			new File(folderNames[0]).mkdir();
			if(!new File(filename).exists()){
				System.out.printf("-- Vamos copiar o diretorio %s "
						+ "do utilizador %s.\n",folderNames[1],folderNames[0]);
			}
			new File(filename).mkdirs();
			return filename+"/"+fl;
			
		}
		else //tamanho 3
				return filename;
	}
	
	public void share(ObjectOutputStream outStream,String myRep,String userTo,
			ObjectInputStream inStream){
		try {
			outStream.writeInt(SHARE);
			
			outStream.writeObject(myRep);
			outStream.writeObject(userTo);
			if(inStream.readShort() != 1){
				System.out.println("Erro: Nao pode partilhar com o proprio utilizador");
				return;
			}
			outStream.flush();
			if(inStream.readInt() == 1){
				int ans;
				if((ans = inStream.readInt()) == 1)
					System.out.println("--  O repositorio "+myRep+" foi "
							+ "partilhado com o utilizador " + userTo);
				else if(ans == 0)
					System.out.printf("Erro: Utilizador %s ja tinha acesso"
							+ " ao repositorio %s\n",userTo,myRep);
				else
					System.out.println("Erro: Ocorreu um erro");
			}
			else
				System.out.println("Erro: O user "+userTo+" nao existe");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void remove(ObjectOutputStream outStream,String myRep,
			String userTo,ObjectInputStream inStream){
		try {
			outStream.writeInt(REMOVE);
			outStream.writeObject(myRep);
			outStream.writeObject(userTo);
			outStream.flush();
			if(inStream.readInt() == 1){
				int ans;
				if((ans = inStream.readInt()) == 1)
					System.out.println("--  O utilizador "+userTo+" foi "
							+ "removido doo repositorio " + myRep);
				else if(ans == 0)
					System.out.printf("Erro: Utilizador %s nao tinha acesso"
							+ " ao repositorio %s\n",userTo,myRep);
				else
					System.out.println("Erro: Ocorreu um erro ao fazer o remove");
			}
			else
				System.out.println("Erro: O user " +userTo + " nao existe");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private long lastModified(String fileName){
		File file = new File(fileName);
		return file.exists() ? file.lastModified() : -1;
	}
	
	
	
}
