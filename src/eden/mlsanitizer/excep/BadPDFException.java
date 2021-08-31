package eden.mlsanitizer.excep;

import eden.common.excep.EDENException;

/**
 * Thrown when something in a PDF is malformed.
 *
 * @author Brendon
 */
public abstract class BadPDFException extends EDENException {

  /** Makes an instance with the given subject and problem. */
  protected BadPDFException(String item, String problem) {
    super(item, problem);
  }

  /** To prevent null instantiations of this class. */
  protected BadPDFException() {
  }
}
