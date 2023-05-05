package eden.mlsanitizer.model.application;

import eden.mlsanitizer.MLSanitizer;

/**
 * Consists of application help messages.
 *
 * @author Brendon.
 */
public final class Help {

  /** Program usage syntax. */
  public static final String USAGE = "Usage: <file>...";
  /** Program usage explanation. */
  public static final String EXPLANATION =
    "Each output filename appends `" +
    MLSanitizer.SUFFIX +
    "` to its " +
    "input filename before\nits extension, and will be (over)written to " +
    "only if necessary.";

  /** To prevent instantiations of this class. */
  private Help() {}
}
