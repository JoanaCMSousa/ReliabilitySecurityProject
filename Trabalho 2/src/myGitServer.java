
 
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Scanner;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.security.cert.Certificate;

//Servidor myServer
 
public class myGitServer{
 
    private repCatalog reps = new repCatalog("users.txt");
    private userCatalog users = new userCatalog();
    private static SecurityHandler sc_hd;
    
    private final static int ADD_USER = 10;
    private final static int PUSH_REP = 20;
    private final static int PUSH_FILE = 30;
    private static final int PULL = 40;
    private final static int SHARE = 50;
    private final static int REMOVE = 60;
     
    private static String pwd_in;
    private static PublicKey pubK;
    private static PrivateKey privKey;
    
    public static void main(String[] args) {
        System.out.println("servidor: main");
       
        myGitServer server = new myGitServer();
        if(args.length < 1 || args.length > 1){
            System.out.println("Erro: Criacao do servidor so recebe o porto");
            return;
        }
       
        
        Scanner sc = new Scanner(System.in);
        //AO INICIAR O SERVIDOR, EH PRECISO PEDIR UMA PASSWORD AO UTILIZADOR
        System.out.println("Porfavor de me a password");
        pwd_in = sc.nextLine();
        sc.close();
        sc_hd = new SecurityHandler(pwd_in);
        
        File file = new File("passwords.txt");
        try {
			file.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        if(!new File("passwords.mac").exists()){
        	try {
        		System.out.println("Vai ser criado um MAC para as passwords");
				sc_hd.createMAC(file);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }else{
        	try {
				if(sc_hd.checkMAC(file) == 0){
					System.out.println("MAC ERRADO!!!! ABORT! ABORT!");
					System.exit(-1);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        FileInputStream kfile;
		try {
			kfile = new FileInputStream("serverkeystore.dd");
			KeyStore kstore = KeyStore.getInstance("JKS");
			//ate aqui tudo bem
	        kstore.load(kfile,"qwerty".toCharArray()); //pode ser que nao esteja a fazer p load
	        if(kstore.containsAlias("server"))
	        	System.out.println("Nao contem");
	        Certificate cert = kstore.getCertificate("server"); //o certificado esta a devolver null
	        pubK = cert.getPublicKey();
	        privKey = (PrivateKey) kstore.getKey("server",
	        		"qwerty".toCharArray());//esta a dar mal
	        if(privKey == null)
	        	System.out.println("Esta null");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
        	System.setProperty("javax.net.ssl.keyStore", "serverkeystore.dd");
            System.setProperty("javax.net.ssl.keyStorePassword", "qwerty");
            ServerSocketFactory ssf = SSLServerSocketFactory.getDefault( );
            sSoc = ssf.createServerSocket(serverSocket);
            //sSoc = new ServerSocket(serverSocket); 
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
        
        public void run(){
            try {
            	
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                 
                String user = (String)inStream.readObject();
                
                ServerClientHandler sch = new ServerClientHandler(user);
                int resp;
                if((resp = sch.AddUser(users, outStream, inStream, sc_hd)) == 1 ){
                    
                    
                    switch(inStream.readInt()){
                    case ADD_USER:
                        break;
                    case PUSH_REP:
                        sch.push_rep(inStream,outStream, reps,sc_hd,pubK);
                        break;
                    case PUSH_FILE:
                    	System.out.println("Entrou no push file");
                        sch.push_file(reps, inStream, outStream,sc_hd,pubK);
                        break;
                    case PULL:
                        sch.pull(reps, outStream, inStream,sc_hd,privKey);
                        break;
                    case SHARE:
                        sch.share(outStream, inStream,reps,users, sc_hd);
                        break;
                    case REMOVE:
                        sch.remove(outStream, inStream, reps, users,sc_hd);
                        break;
                    default:
                        System.out.println("Comando nao reconhecido");
                    }
                }
                else if(resp == 0){
                	outStream.close();
                    inStream.close();
                    socket.close();
                    System.exit(-1);
                }
                 
                outStream.close();
                inStream.close();
                socket.close();
 
            }catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}