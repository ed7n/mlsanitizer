package eden.mlsanitizer.excep;

/**
 * Thrown when an object is not closed with `endobj`.
 *
 * @author Brendon
 */
public class PDFObjectOpenException extends BadPDFException {

  /** Problem description. */
  protected static final String PROBLEM
      = "PDF Error: An object is not closed with `endobj`.";

  /** Makes an instance with the given label. */
  public PDFObjectOpenException(String label) {
    super(label, PROBLEM);
  }
}
