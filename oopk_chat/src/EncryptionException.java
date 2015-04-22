/**
 * This exception is meant to be thrown when something
 * goes wrong while encrypting or decrypting.
 */

public class EncryptionException extends Exception {
	/**
	 * Prints the stack trace of the parameter exception.
	 */
	public EncryptionException(Exception e) {
		e.printStackTrace();
	}
}
