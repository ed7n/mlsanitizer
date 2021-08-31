package eden.mlsanitizer;

import java.util.regex.Pattern;

/**
 * Contains the detection heuristics.
 *
 * @author Brendon
 */
public class Heuristics {

  protected static final Pattern REGEX_ANNOTS
      = Pattern.compile("^/Annots .*$");
  protected static final Pattern REGEX_AUTHOR
      = Pattern.compile("^/Author .*$");
  protected static final Pattern REGEX_CATALOG
      = Pattern.compile("^/Type /Catalog$");
  protected static final Pattern REGEX_CONTENTS
      = Pattern.compile("^/Contents "
          + "\\[(\\p{Digit}+ \\p{Digit}+ R )+(\\p{Digit}+ \\p{Digit}+ R)\\]$");
  protected static final Pattern REGEX_CREATION
      = Pattern.compile("^/CreationDate .*$");
  protected static final Pattern REGEX_CREATOR
      = Pattern.compile("^/Creator .*$");
  protected static final Pattern REGEX_ENDOBJ
      = Pattern.compile("^endobj .*$");
  protected static final Pattern REGEX_HOOK
      = Pattern.compile(
          "^\\p{Digit}+ \\p{Digit}+ obj \\[\\p{Digit}+ \\p{Digit}+ R\\]$");
  protected static final Pattern REGEX_KEYWORDS
      = Pattern.compile("^/Keywords .*$");
  protected static final Pattern REGEX_MODDATE
      = Pattern.compile("^/ModDate .*$");
  protected static final Pattern REGEX_OBJ
      = Pattern.compile("^\\p{Digit}+ \\p{Digit}+ obj .*$");
  protected static final Pattern REGEX_PAGES
      = Pattern.compile("^/Type /Pages$");
  protected static final Pattern REGEX_PDF
      = Pattern.compile("^" + toRegex("%PDF-1.4") + "$");
  protected static final Pattern REGEX_PRODUCER
      = Pattern.compile("^/Producer .*$");
  protected static final Pattern REGEX_STARTXREF
      = Pattern.compile("^startxref$");
  protected static final Pattern REGEX_SUBJECT
      = Pattern.compile("^/Subject .*$");
  protected static final Pattern REGEX_TITLE
      = Pattern.compile("^/Title .*$");
  protected static final Pattern REGEX_URI
      = Pattern.compile(
          "^/URI " + toRegex("(http://www.manualslib.com/)") + "$");
  protected static final int DIST_HOOK_URI = 55;
  private static final String HEXLIT = "\\x";

  /** Returns the regular expression literal of the given string. */
  private static String toRegex(String string) {
    StringBuilder out
        = new StringBuilder(string.length() * (HEXLIT.length() + 2));
    string.codePoints().forEach(codePoint -> {
      out.append(HEXLIT).append(Integer.toHexString(codePoint).toUpperCase());
    });
    return out.toString();
  }
}
