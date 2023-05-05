package eden.mlsanitizer;

import static eden.common.shared.Constants.EOL;
import static eden.common.shared.Constants.EXIT_FAILURE;
import static eden.common.shared.Constants.EXIT_SUCCESS;
import static eden.common.shared.Constants.SPACE;
import static eden.common.shared.Constants.STDOUT;
import static eden.mlsanitizer.Heuristics.DIST_HOOK_URI;
import static eden.mlsanitizer.Heuristics.REGEX_ANNOTS;
import static eden.mlsanitizer.Heuristics.REGEX_AUTHOR;
import static eden.mlsanitizer.Heuristics.REGEX_CATALOG;
import static eden.mlsanitizer.Heuristics.REGEX_CONTENTS;
import static eden.mlsanitizer.Heuristics.REGEX_CREATION;
import static eden.mlsanitizer.Heuristics.REGEX_CREATOR;
import static eden.mlsanitizer.Heuristics.REGEX_ENDOBJ;
import static eden.mlsanitizer.Heuristics.REGEX_HOOK;
import static eden.mlsanitizer.Heuristics.REGEX_KEYWORDS;
import static eden.mlsanitizer.Heuristics.REGEX_MODDATE;
import static eden.mlsanitizer.Heuristics.REGEX_PAGES;
import static eden.mlsanitizer.Heuristics.REGEX_PDF;
import static eden.mlsanitizer.Heuristics.REGEX_PRODUCER;
import static eden.mlsanitizer.Heuristics.REGEX_STARTXREF;
import static eden.mlsanitizer.Heuristics.REGEX_SUBJECT;
import static eden.mlsanitizer.Heuristics.REGEX_TITLE;
import static eden.mlsanitizer.Heuristics.REGEX_URI;

import eden.common.excep.EDENException;
import eden.common.excep.EDENExceptions;
import eden.common.excep.EDENRuntimeException;
import eden.common.io.Modal;
import eden.common.util.Strings;
import eden.mlsanitizer.Context.Flag;
import eden.mlsanitizer.Context.Mode;
import eden.mlsanitizer.excep.BadPDFException;
import eden.mlsanitizer.excep.NonPDFException;
import eden.mlsanitizer.excep.PDFObjectOpenException;
import eden.mlsanitizer.model.application.Help;
import eden.mlsanitizer.model.application.Information;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * This class serves as the entry point to this application. It consists of the
 * main method from which the application initializes into an instance.
 *
 * @author Brendon
 */
public class MLSanitizer {

  /** Output filename suffix. */
  public static final String SUFFIX = "-mlsanitized";
  /** Whether to print stack traces of caught exceptions. */
  public static final boolean DEBUG = false;
  /** xref command. */
  private static final String XREF = "xref";

  /**
   * The main method is the entry point to this application.
   *
   * @param args Command-line arguments to be passed on execution.
   */
  public static void main(String[] arguments) {
    System.exit(new MLSanitizer(arguments).run());
  }

  /** Program modal. */
  private final Modal modal = new Modal(Information.NAME);
  /** Program arguments. */
  private final String[] arguments;
  /** Recyclable execution context. */
  private final Context context = new Context();
  /** Current working item. */
  private String item;
  /** Whether an error has occurred. */
  private boolean error = false;

  /** Makes an instance with the given arguments. */
  private MLSanitizer(String[] args) {
    this.arguments = args;
  }

  /** Runs itself. */
  private int run() {
    if (this.arguments.length == 0) return help();
    File file;
    InputStream in;
    int index;
    for (String argument : this.arguments) {
      this.item = argument;
      file = new File(argument);
      try {
        in = new BufferedInputStream(Files.newInputStream(file.toPath()));
        getContext().initialize(in);
        read();
        if (getContext().isClean()) continue;
        in = new BufferedInputStream(Files.newInputStream(file.toPath()));
        index = argument.lastIndexOf(".");
        getContext()
          .setForWriting(
            in,
            new BufferedOutputStream(
              Files.newOutputStream(
                Paths.get(
                  argument.substring(0, index) +
                  SUFFIX +
                  argument.substring(index)
                )
              )
            )
          );
        write();
        getContext().close();
      } catch (AccessDeniedException exception) {
        this.modal.println(argument + ": Access denied.", Modal.ERROR);
      } catch (NoSuchFileException exception) {
        this.modal.println(argument + ": Not found.", Modal.ERROR);
      } catch (BadPDFException | IOException exception) {
        printException(argument, exception);
        this.error = true;
      }
    }
    return this.error ? EXIT_FAILURE : EXIT_SUCCESS;
  }

  /** Prints its help message. */
  private int help() {
    STDOUT.println(
      Information.getHeader() + EOL + Help.USAGE + EOL + Help.EXPLANATION
    );
    return EXIT_SUCCESS;
  }

  /** Prints the stack trace of the given exception. */
  private void printException(Exception exception) {
    printException(null, exception);
  }

  /**
   * Prints the stack trace of the given exception headered by the given header.
   */
  private void printException(String header, Exception exception) {
    if (exception == null) {
      return;
    }
    if (!Strings.isNullOrEmpty(header)) this.modal.print(
        header + ":" + EOL + "  ",
        Modal.ERROR
      );
    if (exception instanceof EDENRuntimeException) {
      this.modal.println(exception.getMessage(), Modal.ERROR);
      this.modal.println(((EDENRuntimeException) exception).getRemedy());
    } else if (exception instanceof EDENException) {
      this.modal.println(exception.getMessage(), Modal.ERROR);
      this.modal.println(((EDENException) exception).getRemedy());
    } else this.modal.println(exception.toString(), Modal.ERROR);
    if (DEBUG) exception.printStackTrace(this.modal.getPrintStream());
  }

  private void read() throws BadPDFException, IOException {
    main:while (getContext().readLine() != null) switch (
      getContext().getMode()
    ) {
      case PDF:
        readPdf();
        break;
      case PAGES:
        readPages();
        break;
      case PAGE:
        readPage();
        break;
      case HOOK:
        readHook();
        break;
      case URI:
        readUri();
        break;
      case CATALOG:
        readCatalog();
        break;
      case SEEK:
        seek(REGEX_ENDOBJ, Mode.INFO);
        break;
      case INFO:
        readInfo();
        break;
      case DONE:
        break main;
    }
    if (getContext().hasOpenObjs()) throw new PDFObjectOpenException(
      EDENExceptions.makeSubject(
        this.item,
        Long.toString(getContext().getObjMark())
      )
    );
  }

  private void readCatalog() {
    if (getContext().lineMatches(REGEX_CATALOG)) {
      getContext().addMarkToObjMark();
      getContext().setMode(Mode.SEEK);
    }
  }

  private void readHook() {
    if (getContext().lineMatches(REGEX_HOOK)) {
      getContext().setMark(getContext().getLineCount());
      getContext().setMode(Mode.URI);
    }
  }

  private void readInfo() {
    if (
      readInfo(Flag.AUTHOR, REGEX_AUTHOR) &&
      readInfo(Flag.CREATION, REGEX_CREATION) &&
      readInfo(Flag.CREATOR, REGEX_CREATOR) &&
      readInfo(Flag.KEYWORDS, REGEX_KEYWORDS) &&
      readInfo(Flag.MODDATE, REGEX_MODDATE) &&
      readInfo(Flag.PRODUCER, REGEX_PRODUCER) &&
      readInfo(Flag.SUBJECT, REGEX_SUBJECT) &&
      readInfo(Flag.TITLE, REGEX_TITLE) &&
      getContext().lineMatches(REGEX_ENDOBJ)
    ) getContext().setMode(Mode.DONE);
  }

  private boolean readInfo(Flag flag, Pattern pattern) {
    if (!getContext().hasFlag(flag) && getContext().lineMatches(pattern)) {
      getContext().addLine();
      getContext().raiseFlag(flag);
      return false;
    }
    return true;
  }

  private void readPage() {
    if (getContext().lineMatches(REGEX_ANNOTS)) getContext()
      .addLine(); else readHook();
  }

  private void readPages() {
    seek(REGEX_PAGES, Mode.PAGE);
  }

  private void readPdf() throws NonPDFException {
    if (getContext().getLineCount() > 1) throw new NonPDFException(this.item);
    seek(REGEX_PDF, Mode.PAGES);
  }

  private void readUri() {
    if (getContext().lineMatches(REGEX_URI)) getContext()
      .setMode(
        getContext().getDistanceFromMark() == DIST_HOOK_URI
          ? Mode.CATALOG
          : Mode.HOOK
      );
  }

  private void seek(Pattern pattern, Mode mode) {
    if (getContext().lineMatches(pattern)) getContext().setMode(mode);
  }

  private void write() throws IOException {
    Long mark = getContext().nextMark();
    boolean penDown = true;
    while (true) {
      getContext().readLine();
      if (getContext().isEof()) break;
      if (mark != null && getContext().getLineCount() == mark) {
        penDown = !penDown;
        mark = getContext().nextMark();
      }
      if (penDown) switch (getContext().getMode()) {
        case PDF:
          getContext().setMode(Mode.PAGES);
        case PAGES:
          writePages();
          break;
        case PAGE:
          writePage();
          break;
        case XREF:
          writeXref();
          break;
        case DONE:
          getContext().writeBuffer();
          break;
      }
    }
    getContext().close();
  }

  private void writePage() throws IOException {
    if (getContext().lineMatches(REGEX_CONTENTS)) {
      String line = getContext().getLine();
      String[] contents = line.split(SPACE);
      StringBuilder builder = new StringBuilder(line.length());
      for (int index = 0; index < contents.length - 4; index++) builder
        .append(contents[index])
        .append(SPACE);
      getContext()
        .writeString(
          builder.append(contents[contents.length - 4]).append("]").toString()
        );
    } else {
      seek(REGEX_CATALOG, Mode.XREF);
      getContext().writeBuffer();
    }
  }

  private void writePages() throws IOException {
    readPages();
    getContext().writeBuffer();
  }

  private void writeXref() throws IOException {
    if (getContext().isNotInObj()) if (getContext().isMarkSet()) {
      if (getContext().lineMatches(REGEX_STARTXREF)) {
        getContext().writeBuffer();
        getContext().readLine();
        getContext().writeMark();
        getContext().setMode(Mode.DONE);
        return;
      }
    } else {
      int index = getContext().getLine().indexOf(XREF);
      if (index > -1) getContext().setMark(getContext().getSize() + index);
    }
    getContext().writeBuffer();
  }

  /** Returns its recyclable execution context. */
  private Context getContext() {
    return this.context;
  }
}
