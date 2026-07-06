package mx.com.liverpool.misc;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Cifralo {

    private static final int AES_KEY_SIZE = 256; // bits
    private static final int GCM_IV_LENGTH = 12; // bytes
    private static final int GCM_TAG_LENGTH = 128; // bits

    public static void encryptFile(Path inputFile, Path outputFile, byte[] aesKey) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        try (FileInputStream fis = new FileInputStream(inputFile.toFile());
             FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {

            fos.write(iv); // write IV first

            byte[] buffer = new byte[4096];
            int n;
            while ((n = fis.read(buffer)) != -1) {
                byte[] encrypted = cipher.update(buffer, 0, n);
                if (encrypted != null) {
					fos.write(encrypted);
				}
            }
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) {
				fos.write(finalBytes);
			}
        }
    }

    public static byte[] generateRandomAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE); // 256 bits
        SecretKey secretKey = keyGen.generateKey();
        return secretKey.getEncoded();
    }

    public static void main(String[] args) throws Exception {
        Path inputFile = java.nio.file.Paths.get("D:\\java\\lib\\json.jar");
        Path outputFile = java.nio.file.Paths.get("D:\\tmp\\json.jar.bin");

        byte[] aesKey = null;
        try {
        	aesKey = java.nio.file.Files.readAllBytes(Path.of("aeskey.bin"));
        }catch(Exception e) {
        	aesKey = generateRandomAESKey();
        	Files.write(Path.of("aeskey.bin"), aesKey); // Save the AES key (be careful!)
        }

        encryptFile(inputFile, outputFile, aesKey);

        System.out.println("Encryption complete.");
    }

}
