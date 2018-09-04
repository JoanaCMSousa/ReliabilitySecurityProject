/** Grupo sc005
 * Francisco João Guimarães Coimbra de Almeida Araújo nº45701
 * Joana Correia Magalhães Sousa nº47084
 * João Marques de Barros Mendes Leal nº46394
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class Messager {
	
	private static final int SIZE = 1024;

	/**
	 * Funcao que trata do envio do ficheiro
	 * @param file - ficheiro que vai ser enviado
	 * @param outStream - objeto por onde escreve ao recetor
	 * @throws IOException
	 */
	public void sendFile(File file, ObjectOutputStream outStream) throws IOException{
		FileInputStream fp = new FileInputStream(file.getPath());
		byte[] aEnviar = new byte[SIZE];
		outStream.write(ByteBuffer.allocate(4).putInt((int)file.length()).array(),0,4);
		//passar o tamanho total
		int n;
		while((n=fp.read(aEnviar,0,SIZE))>=0){
			outStream.write(aEnviar,0,n);
		}
		outStream.flush();
		fp.close();
	}
	
	/**
	 * Funcao que trata de rececao de um ficheiro
	 * Recebemos fileSize, e os bytes do ficheiro, 1024 de cada vez
	 * @param: fileName - Nome do ficheiro que vai ser recebido
	 * @throws IOException 
	 */
	public File receiveFile(String fileName,ObjectInputStream inStream) throws IOException{
		byte[] by = new byte[4];
		byte[] fileBytes = new byte[SIZE];
		File file = new File(fileName);
		FileOutputStream fos = new FileOutputStream(fileName);
		inStream.read(by,0,4);
		int fileSize = ByteBuffer.wrap(by).getInt();
		int n;
		while(file.length()< fileSize){
			n = inStream.read(fileBytes, 0, 1024);
			fos.write(fileBytes, 0, n);
		}
		fos.close();
		return file;
	}
	
	/**
	 * Funcao que trata do push do ficheiro
	 * @param file - ficheiro ao qual vamos fazer o push
	 * @param outStream - objeto por onde escreve ao recetor
	 * @param inStream - objeto por onde le ao recetor
	 * @return true se o push foi feito com sucesso; false caso contrario
	 * @throws IOException
	 */
	public boolean basic_push(File file,
			ObjectOutputStream outStream,
			ObjectInputStream inStream) throws IOException{
		outStream.writeLong(file.lastModified());
		outStream.flush();
		if(inStream.readShort() == 1){
			sendFile(file, outStream);
			return true;
		}
		
		else
			return false;
	}
	
	/**
	 * Funcao que confirma a rececao do ficheiro
	 * @param outStream - obejto por onde escreve ao recetor
	 */
	public void confirm(ObjectOutputStream outStream){
		try {
			outStream.writeShort(1);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Funcao que rejeita a rececao do ficheiro
	 * @param outStream - objeto por onde escreve ao recetor
	 */
	public void reject(ObjectOutputStream outStream){
		try {
			outStream.writeShort(-1);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
