import java.nio.charset.Charset;
import javax.xml.bind.DatatypeConverter;

/**
 * This class contains methods for encrypting plain strings and returning them as hexadecimal strings,
 * and vice versa. AES and Caesar encryption are supported.
 */

public class Crypto {

	private static final Charset utf8 = Charset.forName("utf-8");

	private static final String encryptKeyHexAES = bytesToHex(AES.getKey());
	private static final String encryptKeyHexCaesar = bytesToHex(stringToBytes("" + Caesar.getKey()));

	/**
	 * Returns true iff the given encryption type is AES.
	 */
	private static boolean isAES(String encryptionType) {
		return "AES".equalsIgnoreCase(encryptionType);
	}

	/**
	 * Returns true iff the given encryption type is Caesar.
	 */
	private static boolean isCaesar(String encryptionType) {
		return "Caesar".equalsIgnoreCase(encryptionType);
	}

	/**
	 * Converts a string into a byte array.
	 */
	private static byte[] stringToBytes(String str) {
		return str.getBytes(utf8);
	}

	/**
	 * Converts a byte array into a hexadecimal string.
	 */
	private static String bytesToHex(byte[] data) {
		return DatatypeConverter.printHexBinary(data);
	}

	/**
	 * Converts a hexadecimal string into a byte array.
	 * @throws EncryptionException if the input string isn't in hexadecimal format.
	 */
	private static byte[] hexToBytes(String hex) throws EncryptionException {
		try {
			return DatatypeConverter.parseHexBinary(hex);
		} catch (IllegalArgumentException e) {
			throw new EncryptionException(e);
		}
	}

	/**
	 * Converts a byte array into a string.
	 */
	private static String bytesToString(byte[] data) {
		return new String(data, utf8);
	}

	/**
	 * Converts a hexadecimal string into a raw integer.
	 * @throws EncryptionException if the input string isn't the hexadecimal representation of
	 *		   an integer.
	 */
	private static int hexToInt(String hex) throws EncryptionException {
		try {
			return Integer.parseInt(bytesToString(hexToBytes(hex)));
		} catch (NumberFormatException e) {
			throw new EncryptionException(e);
		}
	}

	/**
	 * Encrypts a byte array with the given encryption type.
	 * @throws EncryptionException if something goes wrong while encrypting
	 * @throws UnsupportedEncryptionTypeException if the given encryption type isn't supported.
	 */
	public static byte[] encrypt(String encryptionType, byte[] data)
			throws EncryptionException, UnsupportedEncryptionTypeException {
		if (isAES(encryptionType)) return AES.encrypt(data);
		if (isCaesar(encryptionType)) return Caesar.encrypt(data);
		throw new UnsupportedEncryptionTypeException();
	}

	/**
	 * Decrypts a byte array with the given encryption type.
	 * @throws EncryptionException if something goes wrong while decrypting
	 * @throws UnsupportedEncryptionTypeException if the given encryption type isn't supported.
	 */
	public static byte[] decrypt(String encryptionType, byte[] data, String decryptKeyHex)
			throws EncryptionException, UnsupportedEncryptionTypeException {
		if (isAES(encryptionType)) return AES.decrypt(data, hexToBytes(decryptKeyHex));
		if (isCaesar(encryptionType)) return Caesar.decrypt(data, hexToInt(decryptKeyHex));
		throw new UnsupportedEncryptionTypeException();
	}

	/**
	 * Encrypts a string as a hexadecimal string with the given encryption type.
	 * @throws EncryptionException if something goes wrong while encrypting
	 * @throws UnsupportedEncryptionTypeException if the given encryption type isn't supported.
	 */
	public static String encrypt(String encryptionType, String text)
			throws EncryptionException, UnsupportedEncryptionTypeException {
		return bytesToHex(encrypt(encryptionType, stringToBytes(text)));
	}

	/**
	 * Decrypts a hexadecimal string into a plain string with the given encryption type.
	 * @throws EncryptionException if something goes wrong while decrypting
	 * @throws UnsupportedEncryptionTypeException if the given encryption type isn't supported.
	 */
	public static String decrypt(String encryptionType, String textHex, String decryptKeyHex)
			throws EncryptionException, UnsupportedEncryptionTypeException {
		return bytesToString(decrypt(encryptionType, hexToBytes(textHex), decryptKeyHex));
	}

	/**
	 * Returns a hexadecimal string representation of the encryption key of the given type.
	 * @throws UnsupportedEncryptionTypeException if the given encryption type isn't supported.
	 */
	public static String getKey(String encryptionType) throws UnsupportedEncryptionTypeException {
		if (isAES(encryptionType)) return encryptKeyHexAES;
		if (isCaesar(encryptionType)) return encryptKeyHexCaesar;
		throw new UnsupportedEncryptionTypeException();
	}
}