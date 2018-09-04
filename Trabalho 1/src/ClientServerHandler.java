/**Grupo sc005
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
import java.util.Scanner;
/**
 * A ideia desta classe eh tratar das interaccoes do myGit com o myGitServer
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
	
	
	/**
	 * Construtor de ClientServerHandler
	 * @param username - nome do cliente
	 * @param passwd - password do cliente
	 */
	public ClientServerHandler(String username,String passwd){
		this.username = username;
		this.passwd = passwd;
		msg = new Messager();
	}
	
	/**
	 * Envia para o servidor os dados relativamente sobre o cliente em questao
	 * @param outStream - objeto por onde escreve ao servidor
	 */
	public void sendInitInfo(ObjectOutputStream outStream){
		try {
			outStream.writeObject(username);
			outStream.writeObject(passwd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Metodo que cria um novo utilizador caso este nao exista
	 * @param outStream - objeto por onde escreve ao servidor
	 * @param inStream - objeto por onde le ao servidor
	 */
	public int addUser(ObjectOutputStream outStream, ObjectInputStream inStream){
		try {
			outStream.flush();
			if(inStream.readShort() == 1){ //se nao existir
				System.out.println("--O utilizador " + username + "vai ser criado");
				System.out.println("Confirmar password do utilizador " + username);
				Scanner reader = new Scanner(System.in);
				String comp = reader.nextLine();
				reader.close();
				if( passwd.equals(comp)){
					msg.confirm(outStream);
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
			else{
				if(inStream.readShort() == -1){
					System.out.println("Erro:Password errada!");
					return -1;
				}
				return 0;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	
	/**
	 * Funcao que faz push de um ficheiro para o repositorio que se encontra no servidor
	 * que tem o mesmo nome que o repositorio que se encontra localmente
	 * @param file - ficheiro ao qual fazemos push 
	 * @param filename - nome do ficheiro ao qual vamos fazer push
	 * @param outStream - objeto por onde escreve ao servidor 
	 * @param inStream - objeto por onde le ao servidor
	 */
	public void push_file(File file,String filename,
			ObjectOutputStream outStream, ObjectInputStream inStream){
		try {
			outStream.writeInt(PUSH_FILE);
			outStream.writeObject(filename);
			outStream.flush();
			if(inStream.readShort() == -1){
				System.out.println("Erro:Nao tem permissao para entrar nesse ficheiro");
				return;
			}	
			if(msg.basic_push(file,outStream, inStream))
				System.out.println("-- O ficheiro " + file.getName() + " foi"
						+ " enviado para o servidor");
			else
				System.out.println("-- O ficheiro " + file.getName() + "ja "
						+ "se encontra actualizado no servidor");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Funcao que faz push de todos os ficheiros que se encontram no repositorio para o servidor
	 * com o repositorio com o mesmo nome
	 * Caso exista um ficheiro no servidor mas nao localmente, apaga-se o ficheiro do servidor
	 * @param rep - repositorio ao qual vamos fazer push
	 * @param repName - nome do repositorio ao qual vamos fazer push
	 * @param outStream - objeto por onde escreve ao servidor
	 * @param inStream - objeto por onde le ao servidor
	 */
	public void push_rep(File rep,String repName,ObjectOutputStream outStream,
			ObjectInputStream inStream){
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
				if(msg.basic_push(fl,outStream, inStream)){
					total ++;
					System.out.println("-- O ficheiro " + fl.getName() +
							" vai ser adicionado ao servidor");
				}
			}
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * Funcao que faz pull de um ficheiro de um servidor para o repositorio local
	 * @param filename - nome do ficheiro ao qual vamos fazer pull
	 * @param outStream - objeto por onde escreve ao servidor
	 * @param inStream - objeto por onde le ao servidor
	 */
	public void pull(String filename,
			ObjectOutputStream outStream, ObjectInputStream inStream){
		try {
			outStream.writeInt(PULL);
			
			outStream.writeObject(filename);
			outStream.flush();
			int answ;
			long date;
			if((answ = inStream.readShort()) == 1){ //DIRETORIO
				pull_rep(filename, inStream, outStream);
				
			}
			else if(answ == 0){
				if(inStream.readShort() == -1){
					System.out.println("Erro: Nao tem acesso a esse diretorio/ficheiro");
					return;
				}
				date = inStream.readLong();
				if(lastModified(filename) < date){
					msg.confirm(outStream);
					File file = msg.receiveFile(filename,inStream);
					file.setLastModified(date);
					System.out.println("-- O ficheiro " + filename.split("/")[1] +
							" foi copiado do servidor");
				}
				else{
					System.out.println("-- O ficheiro " +
							filename.split("/")[1] + " encountra se actualizado");
					outStream.writeShort(-1);
				}
			}
			else
				System.out.println("Erro: Ficheiro nao encontrado");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * Funcao que faz pull de todos os ficheiros que se encontram num repositorio do servidor
	 * para o repositorio local com o mesmo nome
	 * @param filename - nome no repositorio ao qual vamos fazer pull
	 * @param inStream - objeto por onde le ao servidor
	 * @param outStream - objeto por onde escreve ao servidor
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void pull_rep(String filename,ObjectInputStream inStream,
			ObjectOutputStream outStream) throws IOException, ClassNotFoundException{
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
				file = msg.receiveFile(totalName, inStream);
				file.setLastModified(date);
				if(ourRep)
					System.out.println("-- Copiamos o ficheiro " + fl + " do servidor");
				total ++;
			}
			else{
				msg.reject(outStream);
			}
		}
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
	
	/**
	 * Funcao que devolve o nome completo, ou seja, o diretorio de um ficheiro
	 * @param filename - nome do ficheiro ao qual vamos depois devolver o diretorio completo
	 * @param fl
	 * @return o diretorio do ficheiro com nome filename
	 */
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
	
	/**
	 * Funcao que vai permitir um utilizador partilhar o seu repositorio com um outro 
	 * utilizador
	 * @param outStream - objeto por onde escreve ao servidor
	 * @param myRep - nome do repositorio que vai ser partilhado
	 * @param userTo - nome do utilizador nao criador do repositorio que vai ganhar acesso ao
	 * dito repositorio
	 * @param inStream - objeto por onde le ao servidor
	 */
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
	
	/**
	 * Funcao que permite retirar as permissoes de acesso de um utilizador sobre um 
	 * repositorio
	 * @param outStream - obejto por onde escreve ao servidor
	 * @param myRep - nome do repositorio ao qual vai retirar as permissoes de acesso
	 * @param userTo - nome do utilizador nao criador que vai perder as permissoes de acesso
	 * @param inStream - objeto por onde le ao servidor
	 */
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
	
	/**
	 * Funcao que indica a data da ultima vez que um ficheiro foi alterado
	 * @param fileName - nome do ficheiro ao qual vamos avaliar
	 * @return qual foi a ultima vez que um ficheiro foi alterado, -1 caso nao exista
	 */
	private long lastModified(String fileName){
		File file = new File(fileName);
		return file.exists() ? file.lastModified() : -1;
	}
	
	
	
}
