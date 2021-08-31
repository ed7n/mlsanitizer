package eden.mlsanitizer.excep;

/**
 * Thrown when a file is either not a PDF or has an unexpected PDF version.
 *
 * @author Brendon
 */
public class NonPDFException extends BadPDFException {

  /** Problem description. */
  protected static final String PROBLEM
      = "The file is either not a PDF or has an unexpected PDF version.";

  /** Makes an instance with the given label. */
  public NonPDFException(String label) {
    super(label, PROBLEM);
  }
}
