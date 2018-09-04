

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.swing.plaf.synth.SynthSeparatorUI;

//Servidor myServer

public class myServer{

	private final static int SIZE = 1024;
	
	public static void main(String[] args) {
		System.out.println("servidor: main");
		myServer server = new myServer();
		server.startServer();
	}

	public void startServer (){
		ServerSocket sSoc = null;
        
		try {
			sSoc = new ServerSocket(23456);
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


	//Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}
 
		public void run(){
			
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				
				String user = null;
				String passwd = null;
				
				try {
					user = (String)inStream.readObject(); //Nao estou a enviar nada dah
					passwd = (String)inStream.readObject();
					System.out.println("thread: depois de receber a password e o user");
				}catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}
				
				if (passwd.equals("quemTeDeraSaber2") && user.equals("fc45701")){
					outStream.writeObject(new Boolean(true));
					receiveFile("NewFile.pdf",inStream);
				}
				else {
					outStream.writeObject(new Boolean(false));
				}
				
				
				outStream.close();
				inStream.close();
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * Recebemos fileSize, e os bytes do ficheiro, 1024 de cada vez
	 * @param: fileName -> Nome a dar ao ficheiro
	 * @throws IOException 
	 */
	public void receiveFile(String fileName,ObjectInputStream inStream) throws IOException{
		byte[] by = new byte[4]; //se usarmos o getInt poupamos linhas
		byte[] fileBytes = new byte[1024];
		File file = new File("NewFile.pdf");
		FileOutputStream fos = new FileOutputStream(fileName);
		inStream.read(by,0,4);//receber o tamanho total
		int fileSize = ByteBuffer.wrap(by).getInt(); //converter o tamanho total de bytes a int
		System.out.println(fileSize);
		int n;
		while(file.length()< fileSize){
			n = inStream.read(fileBytes, 0, 1024);
			fos.write(fileBytes, 0, n);
		}
		fos.close();
	}
}