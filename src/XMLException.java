/**
 * This exception is meant to be thrown when something
 * goes wrong while parsing XML.
 */

public class XMLException extends Exception {
	/**
	 * Prints the stack trace of the parameter exception.
	 */
	public XMLException(Exception e) {
		e.printStackTrace();
	}

	public XMLException() {}
}
