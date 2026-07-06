package mx.com.liverpool.misc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESDecryptor {

    private static final int GCM_IV_LENGTH = 12; // bytes
    private static final int GCM_TAG_LENGTH = 128; // bits

    public static void decryptFile(Path encryptedFile, Path outputFile, byte[] aesKey) throws Exception {
        try (FileInputStream fis = new FileInputStream(encryptedFile.toFile());
             FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {

            byte[] iv = new byte[GCM_IV_LENGTH];
            if (fis.read(iv) != GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted file format (missing IV)");
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] buffer = new byte[4096];
            int n;
            while ((n = fis.read(buffer)) != -1) {
                byte[] decrypted = cipher.update(buffer, 0, n);
                if (decrypted != null) {
					fos.write(decrypted);
				}
            }
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) {
				fos.write(finalBytes);
			}
        }
    }

    public static void main(String[] args) throws Exception {
        Path encryptedFile = java.nio.file.Paths.get("D:\\tmp\\paella.bin");
        Path outputFile = java.nio.file.Paths.get("D:\\tmp\\decryptedfile.txt");

        byte[] aesKey = java.nio.file.Files.readAllBytes(Path.of("aeskey.bin")); // Load saved key

        decryptFile(encryptedFile, outputFile, aesKey);

        System.out.println("Decryption complete.");
    }
}