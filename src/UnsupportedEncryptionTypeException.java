/**
 * This exception is meant to be thrown when an encryption or decryption is
 * attempted with an unsupported type.
 */

public class UnsupportedEncryptionTypeException extends Exception {
	/**
	 * true iff the exception was thrown because of an attempt to send a file
	 * request with unsupported type.
	 */
	public final boolean fromOutboundFileRequest;

	public UnsupportedEncryptionTypeException() {
		fromOutboundFileRequest = false;
	}

	public UnsupportedEncryptionTypeException(boolean _fromOutboundFileRequest) {
		fromOutboundFileRequest = _fromOutboundFileRequest;
	}
}