import javax.crypto.KeyGenerator;
import javax.crypto.Cipher;
//import javax.crypto.NoSuchPaddingException;
//import javax.crypto.IllegalBlockSizeException;
//import javax.crypto.BadPaddingException;
import javax.crypto.spec.SecretKeySpec;

//import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

/**
 * This class contains methods for encrypting and decrypting byte arrays with AES encryption.
 */

public class AES {
	private static final byte[] encryptKey;
	private static final Cipher encryptCipher;
	private static final Cipher decryptCipher;

	static {
		KeyGenerator keyGen = null;
		try {
			keyGen = KeyGenerator.getInstance("AES");
			encryptCipher = Cipher.getInstance("AES");
			decryptCipher = Cipher.getInstance("AES");
		//} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
			throw null;
		}
		keyGen.init(128);

		SecretKeySpec encryptSpec = (SecretKeySpec)keyGen.generateKey();
		encryptKey = encryptSpec.getEncoded();

		try {
			encryptCipher.init(Cipher.ENCRYPT_MODE, encryptSpec);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Encrypts and returns the input data.
	 * @throws EncryptionException if something goes wrong while decrypting
	 */
	public static synchronized byte[] encrypt(byte[] data) throws EncryptionException {
		try {
			return encryptCipher.doFinal(data);
		//} catch (IllegalBlockSizeException | BadPaddingException e) {
		} catch (Exception e) {
			throw new EncryptionException(e);
		}
	}

	/**
	 * Decrypts and returns the input data with the given key.
	 * @throws EncryptionException if something goes wrong while decrypting
	 */
	public static synchronized byte[] decrypt(byte[] data, byte[] decryptKey) throws EncryptionException {
		try {
			decryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptKey, "AES"));
		} catch (InvalidKeyException e) {
			throw new EncryptionException(e);
		}
		try {
			return decryptCipher.doFinal(data);
		//} catch (IllegalBlockSizeException | BadPaddingException e) {
		} catch (Exception e) {
			throw new EncryptionException(e);
		}
	}

	/**
	 * Returns the encryption key.
	 */
	public static byte[] getKey() {
		return encryptKey;
	}
}