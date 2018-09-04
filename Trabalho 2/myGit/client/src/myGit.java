 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Scanner;
 
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
 
 
 
public class myGit{
     
    private static int ped;
    private static boolean missingPass = false;
    private static String username;
    private static String serverAddress;
    private static String password;
    private static String filename;
    private static String userTo;
     
    private final static int INIT = 0;
    private final static int ADD_USER = 10;
    private final static int PUSH = 20;
    private final static int PULL = 30;
    private final static int SHARE = 40;
    private final static int REMOVE = 50;
     
    private static SecurityHandler sc = new SecurityHandler("Vou ter de tirar daqui a password");
    private static PublicKey pubKey;
    private static PrivateKey privKey;
    
    public static void main(String[] args) throws FileNotFoundException {
        ped = isValid(args);
         
        myGit client = new myGit();
        client.startClient();
        
        FileInputStream kfile;
		try {
			kfile = new FileInputStream("clientkeystore.dd");
			KeyStore kstore = KeyStore.getInstance("JKS");
	        kstore.load(kfile,"qwerty".toCharArray());
	        if(kstore.containsAlias("client"))
	        	System.out.println("Nao contem");
	        Certificate cert = kstore.getCertificate("client");
	        if(cert == null)
	        	System.out.println("Esta null");
	        pubKey = cert.getPublicKey();
	        privKey = (PrivateKey) kstore.getKey("client",
	        		"qwerty".toCharArray());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }
 
    /**
     * Funcao que inicia o cliente
     */
    public void startClient (){
        ClientThread newClientThread = new ClientThread();
        newClientThread.start();
    }
     
    /**
     * Funcao que trata de ligacao do cliente com o servidor
     * @param ServerAddress - o endereco do servidor ao qual vai fazer a ligacao
     * @return a socket da ligacao estabelecida
     */
    public Socket startConnection(String ServerAddress){
        Socket sSoc = null;
        System.setProperty("javax.net.ssl.trustStore", "clientkeystore.dd");
        SocketFactory sf = SSLSocketFactory.getDefault( );
        String srvAdrs[] = ServerAddress.split(":");
        try {
            sSoc = sf.createSocket(srvAdrs[0],Integer.parseInt(srvAdrs[1]));
        } catch (IOException e) {
            System.out.println("Erro: Servidor nao encontrado!");
            System.exit(-1);
        }
        return sSoc;
    }
 
    //Threads utilizadas para comunicacao com o servidor
    class ClientThread extends Thread {
 
        private Socket socket = null;
         
        /**
         * Trata da iteracao entre o cliente e o servidor
         */
        public void run(){
            try {
                 
                if(ped == INIT)
                    init(filename);
                else if(ped == -1)
                    System.out.println("Erro: comando nao reconhecido");
                else{
                     
                    socket = startConnection(serverAddress);
                     
                    ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                    Scanner reader = new Scanner(System.in);
                    if(missingPass){
                        System.out.println("-- Porfavor diga a password");
                        password = reader.nextLine();
                    }
                    ClientServerHandler csh = new ClientServerHandler(username,password);
                         
                    csh.sendInitInfo(outStream); 
                    //preciso de fazer o log in
                    int addUs;
                    if((addUs = csh.addUser(outStream, inStream,sc)) == -1)
                        return;
                    
                    
                    switch(ped){
                    case ADD_USER:
                        if(addUs == 0)
                            System.out.println("-- O utilizador " + username
                                    + " ja existe");
                        System.out.println("Vai para aqui");
                        outStream.writeInt(-1);
                        outStream.flush();
                        break;
                    case PUSH:
                    	System.out.println("Entrou no caso do push");
                        File file = new File(filename);
                        if(file.exists()){
                            if(file.isDirectory())
                                csh.push_rep(file,filename,outStream, inStream, sc, privKey);
                            else{
                                if(filename.split("/").length == 1){
                                    outStream.writeInt(-1);
                                    System.out.println("ERRO: Tem de especificar o diretorio");
                                    break;
                                }
                                
                                csh.push_file(file,filename, outStream, inStream,sc,privKey);
                            }
                        }else{
                            System.out.println("Eroo: Esse ficheiro nao existe");
                            outStream.writeInt(-1);
                        }
                 
                        break;
                    case PULL:
                        csh.pull(filename, outStream, inStream,sc);
                        break;
                    case SHARE:
                        csh.share(outStream, filename, userTo, inStream);
                        break;
                    case REMOVE:
                        csh.remove(outStream, filename, userTo, inStream);
                        break;
                    }
                     
                    reader.close();
                    outStream.close();
                    inStream.close();
                    socket.close();
                }
 
            } catch (Exception e) {
                System.out.println("Erro:nao se conseguiu conectar ao servidor");
            }
        }
    }
     
    /**
     * Funcao que verifica se os argumentos inseridos sao validos
     * @param arg - os argumentos que vao ser avaliados
     * @return
     */
    public static int isValid(String[] arg){
        int min = 0,tam = arg.length;
        if(tam < 2 || tam > 8)
            return -1;
        if(arg[0].equals("-init") && tam == 2){
            filename = arg[1];
            return INIT;
        }
        username = arg[0];
        serverAddress = arg[1];
        if(tam == 2){
            missingPass = true;
            return ADD_USER;
        }
        else if (tam >=4)
            password = arg[3];
        else
            return -1;
        if(tam >= 4){
            if(!arg[2].equals("-p")){
                missingPass = true;
                min = 2;
            }
            if(tam + min == 4)
                return ADD_USER;
            if(tam + min == 6){
                filename = arg[5 - min];
                if(arg[4 - min].equals("-push"))
                    return PUSH;
                else if(arg[4 - min].equals("-pull"))
                    return PULL;
            }
            else if(tam + min == 7){
                filename = arg[5 - min];
                userTo = arg[6 - min];
                if(arg[4 - min].equals("-share"))
                    return SHARE;
                else if(arg[4 - min].equals("-remove"))
                    return REMOVE;
            }
        }   
        return -1;
    }
     
    /**
     * Funcao que trata da criacao de um repositorio local
     * @param folderName - nome do repositorio a ser criado
     */
    public static void init(String folderName){
        if(new File(folderName).mkdirs())
            System.out.println("-- O repositorio " + folderName + " foi criado localmente");
        else
            System.out.println("-- Erro na criacao do " + folderName);
    }
}
