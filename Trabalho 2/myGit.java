/** Grupo sc005
 * Francisco João Guimarães Coimbra de Almeida Araújo nº45701
 * Joana Correia Magalhães Sousa nº47084
 * João Marques de Barros Mendes Leal nº46394
 */
 
 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
    
    public static void main(String[] args) throws FileNotFoundException {
        ped = isValid(args);
         
        myGit client = new myGit();
        client.startClient();
         
        /*
         * SocketFactory sf = SSLSocketFactory.getDefault( );
         * Socket s = sf.createSocket(args[0], Integer.parseInt(args[1])
         */
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
        System.setProperty("javax.net.ssl.trustStore", "client.dd");
        SocketFactory sf = SSLSocketFactory.getDefault( );
        String srvAdrs[] = ServerAddress.split(":");
        try {
            sSoc = sf.createSocket(srvAdrs[0],Integer.parseInt(srvAdrs[1]));
                    //new Socket(srvAdrs[0],Integer.parseInt(srvAdrs[1]));
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
                         
                    //enviar logo aquilo que vai enviar obrigatoriamente
                    csh.sendInitInfo(outStream); //acho que vou ter de tirar daqui coisas, por exemplo nao precisamos de enviar a password logo
                    //preciso de fazer o log in
                    int addUs;
                    if((addUs = csh.addUser(outStream, inStream,sc)) == -1)
                        return;
                    //String nonce = (String) inStream.readObject();
                	//System.out.println(nonce);
                
                    switch(ped){
                    case ADD_USER:
                        if(addUs == 0)
                            System.out.println("-- O utilizador " + username
                                    + " ja existe");
                        outStream.writeInt(-1);
                        outStream.flush();
                        break;
                    case PUSH:
                        File file = new File(filename);
                        if(file.exists()){
                            if(file.isDirectory())
                                csh.push_rep(file,filename,outStream, inStream);
                            else{
                                if(filename.split("/").length == 1){
                                    outStream.writeInt(-1);
                                    System.out.println("ERRO: Tem de especificar o diretorio");
                                    break;
                                }
                                csh.push_file(file,filename, outStream, inStream);
                            }
                        }else{
                            System.out.println("Eroo: Esse ficheiro nao existe");
                            outStream.writeInt(-1);
                        }
                 
                        break;
                    case PULL:
                        csh.pull(filename, outStream, inStream);
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