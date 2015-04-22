import java.util.Random;

/**
 * This class contains methods for encrypting and decrypting byte arrays with Caesar encryption.
 */

public class Caesar {
	private static final int encryptKey;

	static {
		byte[] temp = new byte[1];
		while (temp[0] == 0) new Random().nextBytes(temp);
		encryptKey = temp[0];
	}

	/**
	 * Clones the input data and shifts each byte by the given key value.
	 */
	private static byte[] shift(byte[] data, int key) {
		byte[] ret = data.clone();
		for (int i = 0; i < ret.length; i++) ret[i] += key;
		return ret;
	}

	/**
	 * Encrypts and returns the input data.
	 */
	public static byte[] encrypt(byte[] data) {
		return shift(data, encryptKey);
	}

	/**
	 * Decrypts and returns the input data with the given key.
	 */
	public static byte[] decrypt(byte[] data, int decryptKey) {
		return shift(data, -decryptKey);
	}

	/**
	 * Returns the encryption key.
	 */
	public static int getKey() {
		return encryptKey;
	}
}