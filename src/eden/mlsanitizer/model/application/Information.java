package eden.mlsanitizer.model.application;

import static eden.common.shared.Constants.EOL;
import static eden.common.shared.Constants.SPACE;

/**
 * Consists of application descriptions.
 *
 * @author Brendon
 */
public final class Information {

  /** Application name. */
  public static final String NAME = "MLSanitizer";

  /** Application version. */
  public static final String VER = "u0r2";

  /** Application version, long version. */
  public static final String VERSION = "Update 0 Revision 2";

  /** Application release date. */
  public static final String DATE = "11/06/2021";

  /** Application description. */
  public static final String DESCRIPTION
      = "ManualsLib-watermarked PDF sanitizer.";

  /** Application landing URL. */
  public static final String URL
      = "https://ed7n.github.io/mlsanitizer";

  /** Returns the header for this application. */
  public static String getHeader() {
    return NAME + SPACE + VER + SPACE + "by Brendon,"
        + SPACE + DATE + "." + EOL + "——" + DESCRIPTION + SPACE + URL + EOL;
  }

  /** To prevent instantiations of this class. */
  private Information() {
  }
}
