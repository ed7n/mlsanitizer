package eden.mlsanitizer;

import static eden.common.shared.Constants.NUL_INT;
import static eden.mlsanitizer.Heuristics.REGEX_ENDOBJ;
import static eden.mlsanitizer.Heuristics.REGEX_OBJ;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Encapsulates a recyclable execution context.
 *
 * @author Brendon
 */
public class Context {

  /** Initial operation mode. */
  protected static final Mode INIT_MODE = Mode.PDF;

  /** Line buffer capacity. */
  protected static final int CAPACITY = 655360;

  /** End-of-line. */
  protected static final char EOL = '\n';

  /** Mark queue. */
  protected final Deque<Long> marks = new ArrayDeque<>();

  /** Line buffer. */
  protected final List<Integer> buffer = new ArrayList<>(CAPACITY);

  /** Line builder. */
  protected final StringBuilder builder = new StringBuilder(CAPACITY);

  /** Status flags. */
  protected final Set<Flag> flags = EnumSet.noneOf(Flag.class);

  /** Input PDF stream. */
  protected InputStream in;

  /** Output PDF stream. */
  protected OutputStream out;

  /** Operation mode. */
  protected Mode mode;

  /** Line accumulator. */
  protected String line;

  /** Line and size counters, mark, and object mark. */
  protected long lineCount, mark, objMark, size;

  /** Whether its InputStream has reached the end-of-file. */
  protected boolean eof;

  /** Whether its InputStream is at an end-of-line. */
  protected boolean eol = false;

  {
    initialize(true);
  }

  /**
   * Initializes most of its fields.
   *
   * @param clearMarks Whether to clear the mark queue.
   */
  protected void initialize(boolean clearMarks) {
    this.eof = false;
    this.eol = false;
    this.flags.clear();
    clearBuilder();
    getBuffer().clear();
    resetLineCount();
    setLine(null);
    setMark(NUL_INT);
    setMode(INIT_MODE);
    setObjMark(NUL_INT);
    zeroSize();
    if (clearMarks)
      getMarks().clear();
  }

  /** Initializes itself for reading from the given InputStream. */
  protected void initialize(InputStream in) throws IOException {
    close();
    this.in = in;
    this.out = null;
    initialize(true);
  }

  /** Returns whether its line matches the given pattern. */
  protected boolean lineMatches(Pattern pattern) {
    return pattern.matcher(getLine()).matches();
  }

  /** Removes and returns the next mark from its mark queue. */
  protected Long nextMark() {
    return getMarks().pollFirst();
  }

  /** Reads and returns the next line from its InputStream. */
  protected String readLine() throws IOException {
    if (isEof())
      return null;
    int acc;
    if (isInWriteMode())
      getBuffer().clear();
    else
      clearBuilder();
    while (true) {
      if (isEol()) {
        incrementLineCount();
        this.eol = false;
      }
      acc = this.in.read();
      switch (acc) {
        case -1:
          this.eof = true;
        case EOL:
          this.eol = true;
          return isInWriteMode() ? stringifyBuffer() : stringifyBuilder();
      }
      if (isInWriteMode())
        getBuffer().add(acc);
      else
        getBuilder().appendCodePoint(acc);
    }
  }

  /**
   * Sets itself for writing to the given OutputStream. This initializes all but
   * its mark queue. Its InputStream can not be initialized and instead be
   * replaced with the given one.
   */
  protected void setForWriting(InputStream in, OutputStream out)
      throws IOException {
    close();
    this.in = in;
    this.out = out;
    initialize(false);
  }

  /**
   * Stringifies its line buffer into its line accumulator, then returns the
   * latter.
   */
  protected String stringifyBuffer() {
    clearBuilder();
    getBuffer().forEach(acc -> getBuilder().appendCodePoint(acc));
    return stringifyBuilder();
  }

  /**
   * Stringifies its line builder into its line accumulator, then returns the
   * latter.
   */
  protected String stringifyBuilder() {
    setLine(getBuilder().toString());
    if (!isInWriteMode())
      if (lineMatches(REGEX_OBJ))
        setObjMark(getLineCount());
      else if (lineMatches(REGEX_ENDOBJ))
        setObjMark(NUL_INT);
    return getLine();
  }

  /** Writes its line buffer to its OutputStream. */
  protected void writeBuffer() throws IOException {
    for (int index = 0; index < getBuffer().size(); index++)
      this.out.write(getBuffer().get(index));
    this.out.write(EOL);
    incrementSize(getBuffer().size() + 1);
  }

  /** Writes its mark to its OutputStream. */
  protected void writeMark() throws IOException {
    writeString(Long.toString(getMark()));
  }

  /** Writes the given string to its OutputStream. */
  protected void writeString(String string) throws IOException {
    this.out.write(string.getBytes());
    this.out.write(EOL);
    incrementSize(string.length() + 1);
  }

  /** Closes its I/O streams. */
  protected void close() throws IOException {
    if (this.in != null)
      this.in.close();
    if (this.out != null)
      this.out.close();
  }

  /** Returns the difference between its line counter and mark. */
  protected long getDistanceFromMark() {
    return getLineCount() - getMark();
  }

  /** Returns its line buffer. */
  protected List<Integer> getBuffer() {
    return this.buffer;
  }

  /** Returns its line builder. */
  protected StringBuilder getBuilder() {
    return this.builder;
  }

  /** Clears its line builder. */
  protected void clearBuilder() {
    getBuilder().delete(0, getBuilder().length());
  }

  /** Raises the given flag. */
  protected void raiseFlag(Flag flag) {
    this.flags.add(flag);
  }

  /** Returns its line accumulator. */
  protected String getLine() {
    return this.line;
  }

  /** Sets its line accumulator. */
  protected void setLine(String line) {
    this.line = line;
  }

  /** Returns its line counter. */
  protected long getLineCount() {
    return this.lineCount;
  }

  /** Sets its line counter. */
  protected void setLineCount(long lineCount) {
    this.lineCount = lineCount;
  }

  /** Increments its line counter by 1. */
  protected void incrementLineCount() {
    setLineCount(Math.min(Long.MAX_VALUE, getLineCount() + 1));
  }

  /** Resets its line counter. */
  protected void resetLineCount() {
    setLineCount(1);
  }

  /** Returns its mark. */
  protected long getMark() {
    return this.mark;
  }

  /** Sets its mark. */
  protected void setMark(long mark) {
    this.mark = mark;
  }

  /** Returns its mark queue. */
  protected Deque<Long> getMarks() {
    return this.marks;
  }

  /** Adds its line counter to its mark queue. */
  protected void addLine() {
    addRange(getLineCount(), getLineCount() + 1);
  }

  /** Adds its mark and object mark to its mark queue. */
  protected void addMarkToObjMark() {
    addRange(getMark(), getObjMark());
  }

  /**
   * Adds two longs to its mark queue. If {@code from} was added last, then it
   * is replaced with {@code to}.
   */
  protected void addRange(long from, long to) {
    Long last = getMarks().peekLast();
    if (last != null && from == last)
      getMarks().pollLast();
    else
      getMarks().addLast(from);
    getMarks().addLast(to);
  }

  /** Returns its operation mode. */
  protected Mode getMode() {
    return this.mode;
  }

  /** Sets its operation mode. */
  protected void setMode(Mode mode) {
    this.mode = mode;
  }

  /** Returns its object mark. */
  protected long getObjMark() {
    return this.objMark;
  }

  /** Sets its object mark. */
  protected void setObjMark(long mark) {
    this.objMark = mark;
  }

  /** Returns its size counter. */
  protected long getSize() {
    return this.size;
  }

  /** Increments its size counter by the given number. */
  protected void incrementSize(long size) {
    this.size = Math.min(Long.MAX_VALUE, getSize() + size);
  }

  /** Zeros its size counter. */
  protected void zeroSize() {
    incrementSize(-getSize());
  }

  /** Returns whether the given flag is raised. */
  protected boolean hasFlag(Flag flag) {
    return this.flags.contains(flag);
  }

  /** Returns whether its input PDF has open objects. */
  protected boolean hasOpenObjs() {
    return isEof() && !isNotInObj();
  }

  /** Returns whether its InputStream is clean. */
  protected boolean isClean() {
    return isEof() && getMarks().isEmpty();
  }

  /** Returns whether its InputStream has reached the end-of-file. */
  protected boolean isEof() {
    return this.eof;
  }

  /** Returns whether its InputStream is at an end-of-line. */
  protected boolean isEol() {
    return this.eol;
  }

  /** Returns whether its mark is set. */
  protected boolean isMarkSet() {
    return getMark() != NUL_INT;
  }

  /** Returns whether its InputStream cursor is not in an object. */
  protected boolean isNotInObj() {
    return getObjMark() == NUL_INT;
  }

  /** Returns whether it is in write mode. */
  protected boolean isInWriteMode() {
    return this.out != null;
  }

  /** Status flags. */
  protected enum Flag {
    AUTHOR, CREATION, CREATOR, KEYWORDS, MODDATE, PRODUCER, SUBJECT, TITLE;
  }

  /** Operation modes. */
  protected enum Mode {
    PDF, PAGES, PAGE, HOOK, URI, CATALOG, SEEK, INFO, XREF, DONE;
  }
}
