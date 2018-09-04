import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class Cifra {

    public static void main(String[] args) throws Exception {

    //gerar uma chave aleatória para utilizar com o AES
    KeyGenerator kg = KeyGenerator.getInstance("AES");
    kg.init(128);
    SecretKey key = kg.generateKey();

    Cipher c = Cipher.getInstance("AES");
    c.init(Cipher.ENCRYPT_MODE, key);

    FileInputStream fis;
    FileOutputStream fos;
    CipherOutputStream cos;
    
    fis = new FileInputStream("test.pdf");
    fos = new FileOutputStream("test.cif");

    cos = new CipherOutputStream(fos, c);
    byte[] b = new byte[1024];
    int i = fis.read(b);
    
    while (i != -1) {
        cos.write(b, 0, i);
        i = fis.read(b);
    }
    cos.close();
    fis.close();

    //cifrar a chave secreta com a chave publica
    //1) obter chave publica -> da keystore
    FileInputStream kfile = new FileInputStream("keystore.dd");  //keystore
    KeyStore kstore = KeyStore.getInstance("JKS");
    kstore.load(kfile, "123456".toCharArray());           //password
    Certificate cert = kstore.getCertificate("dd");  //alias do utilizador
    
    //2) cifrar
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.WRAP_MODE, cert);
    byte[] keyCifrada = cipher.wrap(key);
    
    //3) escrever para o ficheiro
    FileOutputStream kos = new FileOutputStream("a.key");
    ObjectOutputStream oos = new ObjectOutputStream(kos);
    oos.write(keyCifrada);
    
    oos.close();
    kos.close();
    }
}