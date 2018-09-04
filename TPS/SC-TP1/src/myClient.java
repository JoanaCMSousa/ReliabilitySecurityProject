
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
//import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.SecureRandom;



public class myClient {
	
	private final static int SIZE = 1024; //tamanho maximo de leitura de cada vez
	private final int INT_SIZE = 4;
	private static String serverAddress;
	
	public static void main(String[] args) throws FileNotFoundException {
		//System.out.println("working directory = " + System.getProperty("user.dir")); //<- how to get local directory
		//File[] files = new File(System.getProperty("user.dir")).listFiles(); //<-get all files in dir
		doWhat(args);
		System.out.println("client: main");
		myClient client = new myClient();
		client.startClient();
	}

	public void startClient (){
		Socket sSoc = null;
        
		try {
			sSoc = new Socket("127.0.0.1",23456);
			ClientThread newClientThread = new ClientThread(sSoc);
			newClientThread.start();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}

	//Threads utilizadas para comunicacao com o servidor
	class ClientThread extends Thread {

		private Socket socket = null;

		ClientThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}
 
		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				
				outStream.writeObject("fc45701");
				outStream.writeObject("quemTeDeraSaber2");
				
				try {
					Object res = inStream.readObject();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				//String path = "test.pdf";
				String path = "20-coordenacao.pdf";
				File file = new File(path);
				sendFile(file,outStream);
				outStream.close();
				inStream.close();
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	//Estou a pensar meter isto numa class que tenha funcoes com a interacao
	/**
	 * Primeiro envia o tamanho do ficheiro
	 * Segundo comeca a enviar o ficheiro 1024 bytes de cada vez
	 * @param file -> ficheiro a entregar
	 * @param outStream -> Stream de dados
	 * @throws IOException 
	 */
	public void sendFile(File file, ObjectOutputStream outStream) throws IOException{
		FileInputStream fp = new FileInputStream(file.getPath());
		byte[] aEnviar = new byte[SIZE];
		outStream.write(ByteBuffer.allocate(INT_SIZE).putInt((int)file.length()).array(),0,INT_SIZE); //passar o tamanho total
		int n;
		while((n=fp.read(aEnviar,0,SIZE))>=0){
			outStream.write(aEnviar,0,n);
		}
		fp.close();
	}
	
	public static int doWhat(String[] args){
		int tam = args.length;
		if(!args[0].equals("myGit") || tam < 2 || tam > 8){
			System.out.println("Something went wrong");
			return -1;
		}
		if(args[1].equals("-init") && tam == 3){
			init(args[2]);
		}
		else if(tam >= 5 && args[3].equals("-p")){
			String localUser = args[1];
			serverAddress = args[2];
			String password = args[4];
			if(tam == 5)
				System.out.println("Criar um novo utilizador caso, nao exista");
			else if(tam == 7){
				if(args[5].equals("-push"))
					push(localUser,password,args[6]);
				else if(args[5].equals("-pull"))
					pull(localUser,password,args[6]);
			}
			else if(tam == 8){
				if(args[5].equals("-share")){
					//share
				}
				else if(args[5].equals("-remove")){
					//remove
				}
			}
		}
		return 0;
	}

	
	//Ideia, meter isto tudo numa interface partilhada entre o client e o servidor
	//UMA VERSAO MUITO INICIAL
	//TEMOS DE CRIAR UMA CLASE ESPECIAL NAO?
	/**
	 * Ideia mandar com um codigo, ex 10 eh o INIT
	 * 20 eh o -push... assim, mandamos com um int e pronto
	 * @param folderName
	 */
	public static void init(String folderName){
		if(new File(folderName).mkdirs())
			System.out.println("Managed to create the file");
		else
			System.out.println("Folder not created");
	}
	
	/**
	 * Preciso de verificar que fileName existe e de descobrir se eh um folder ou
	 * um ficheiro
	 * @param localUser
	 * @param password
	 * @param fileName
	 */
	public static void push(String localUser, String password, String fileName) {
		// TODO Auto-generated method stub
		}
	public static void pull(String localUser, String password, String fileName) {
		// TODO Auto-generated method stub
		}
}

