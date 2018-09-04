
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class Messager {
	
	private static final int SIZE = 1024;

	public void sendFile(File file, ObjectOutputStream outStream) throws IOException{
		FileInputStream fp = new FileInputStream(file.getPath());
		byte[] aEnviar = new byte[SIZE];
		outStream.write(ByteBuffer.allocate(4).putInt((int)file.length()).array(),0,4); //passar o tamanho total
		int n;
		while((n=fp.read(aEnviar,0,SIZE))>=0){
			outStream.write(aEnviar,0,n);
		}
		outStream.flush();
		fp.close();
	}
	
	/**
	 * Recebemos fileSize, e os bytes do ficheiro, 1024 de cada vez
	 * @param: fileName -> Nome a dar ao ficheiro
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
	
	public boolean notModified(File file,ObjectOutputStream outStream,
			ObjectInputStream inStream) throws IOException{
		outStream.writeLong(file.lastModified());
		outStream.flush();
		return inStream.readShort() == 1;
	}
	
	public void confirm(ObjectOutputStream outStream){
		try {
			outStream.writeShort(1);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void reject(ObjectOutputStream outStream){
		try {
			outStream.writeShort(-1);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void securityError(ObjectOutputStream outStream){
		try {
			outStream.writeShort(-10);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
