/** Grupo sc005
 * Francisco João Guimarães Coimbra de Almeida Araújo nº45701
 * Joana Correia Magalhães Sousa nº47084
 * João Marques de Barros Mendes Leal nº46394
 */


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

//Servidor myServer

public class myGitServer{

	private repCatalog reps = new repCatalog("users.txt");
	private userCatalog users = new userCatalog();
	
	private final static int ADD_USER = 10;
	private final static int PUSH_REP = 20;
	private final static int PUSH_FILE = 30;
	private static final int PULL = 40;
	private final static int SHARE = 50;
	private final static int REMOVE = 60;
	
	public static void main(String[] args) {
		System.out.println("servidor: main");
		myGitServer server = new myGitServer();
		if(args.length < 1 || args.length > 1){
			System.out.println("Erro: Criacao do servidor so recebe o porto");
			return;
		}
		int serverPort = Integer.parseInt(args[0]);
		server.startServer(serverPort);
	}

	/**
	 * Funcao que inicia o servidor
	 * @param serverSocket - socket pelo qual e feito a ligacao com o servidor
	 */
	public void startServer (int serverSocket){
		ServerSocket sSoc = null;
        
		try {
			System.setProperty("javax.net.ssl.keyStore", "keystore.dd");
			System.setProperty("javax.net.ssl.keyStorePassword", "123456");
			ServerSocketFactory ssf = SSLServerSocketFactory.getDefault( );
			sSoc = ssf.createServerSocket(serverSocket);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
         
		while(true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		}
		//sSoc.close();
	}

	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
		}

		/**
		 * Trata da iteracao entre o servidor e cliente
		 */
		public void run(){
			
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				
				String user = (String)inStream.readObject();
				String passwd = (String)inStream.readObject(); 
				
				ServerClientHandler sch = new ServerClientHandler(user,passwd);
			
				if(sch.AddUser(users, outStream, inStream)){
					System.out.println("Saiu do addUser");
					switch(inStream.readInt()){
					case ADD_USER:
						break;
					case PUSH_REP:
						sch.push_rep(inStream,outStream, reps);
						break;
					case PUSH_FILE:
						sch.push_file(reps, inStream, outStream);
						break;
					case PULL:
						sch.pull(reps, outStream, inStream);
						break;
					case SHARE:
						sch.share(outStream, inStream,reps,users);
						break;
					case REMOVE:
						sch.remove(outStream, inStream, reps, users);
						break;
					default:
						System.out.println("Comando nao reconhecido");
					}
				}
				
				outStream.close();
				inStream.close();
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}