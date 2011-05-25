
package no.priv.garshol.duke;

import java.io.Reader;
import java.io.IOException;
import java.io.BufferedReader;

/**
 * A basic NTriples parser used by NTriplesDataSource.
 */
public class NTriplesParser {
  private Reader src;
  private StatementHandler handler;
  private int lineno;
  private int pos;
  private String line;
  
  public static void parse(Reader src, StatementHandler handler)
    throws IOException {
    new NTriplesParser(src, handler).parse();
  }

  private NTriplesParser(Reader src, StatementHandler handler)
    throws IOException {
    this.src = src;
    this.handler = handler;
  }

  private void parse() throws IOException {
    BufferedReader in = new BufferedReader(src);
    line = in.readLine();
    while (line != null) {
      lineno++;
      parseLine();
      line = in.readLine();
    }    
  }

  private void parseLine() {
    pos = 0;
    skipws();
    if (line.charAt(pos) == '#')
      return; // think there's nothing to do in this case

    // subject
    String subject;
    if (line.charAt(pos) == '<')
      subject = parseuri();
    else if (line.charAt(pos) == '_')
      subject = parsebnode();
    else
      throw new RuntimeException("Subject in line " + lineno +
                                 " is neither URI nor bnode: " + line);

    skipws();

    // property
    if (line.charAt(pos) != '<')
      throw new RuntimeException("Predicate does not start with '<', " +
                                 "nearby: '" +
                                 line.substring(pos - 5, pos + 5) + "', at " +
                                 "position: " + pos + " in line " + lineno);
    String property = parseuri();

    skipws();

    // object
    boolean literal = false;
    String object;
    if (line.charAt(pos) == '<')
      object = parseuri();
    else if (line.charAt(pos) == '"') {
      object = unescape(parseliteral());
      literal = true;
    } else if (line.charAt(pos) == '_')
      object = parsebnode();
    else
      throw new RuntimeException("Illegal object on line " + lineno + ": " +
                                 line.substring(pos));

    // terminator
    skipws();
    if (line.charAt(pos) != '.')
      throw new RuntimeException("Statement did not end with period; line: '" +
                                 line + "', line number: " + lineno);

    handler.statement(subject, property, object, literal);
  }

  private static String unescape(String literal) {
    char[] buf = new char[literal.length()];
    int pos = 0;

    for (int ix = 0; ix < literal.length(); ix++)
      if (literal.charAt(ix) == '\\') {
        ix++;
        char ch = literal.charAt(ix);
        if (ch == 'n')
          buf[pos++] = '\n';
        else if (ch == 'r')
          buf[pos++] = '\r';
        else if (ch == 't')
          buf[pos++] = '\t';
        else if (ch == '\\')
          buf[pos++] = '\\';
        else if (ch == '"')
          buf[pos++] = '"';
        else if (ch == 'u') {
          ix++; // step over the 'u'
          if (literal.length() < ix + 4 ||
              !(hexchar(literal.charAt(ix)) &&
                hexchar(literal.charAt(ix + 1)) &&
                hexchar(literal.charAt(ix + 2)) &&
                hexchar(literal.charAt(ix + 3))))
            throw new RuntimeException("Bad Unicode escape: '" +
                                       literal.substring(ix - 2, ix + 4) + "'");
          buf[pos++] = unhex(literal, ix);
          ix += 3;
        } else
          throw new RuntimeException("Unknown escaped character: '" + ch + "' in '" + literal + "'");
      } else
        buf[pos++] = literal.charAt(ix);

    return new String(buf, 0, pos);
  }

  private static boolean hexchar(char ch) {
    return (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F');
  }

  private static char unhex(String literal, int pos) {
    int charno = 0;
    for (int ix = pos; ix < pos + 4; ix++) {
      int digit;
      char ch = literal.charAt(ix);
      if (ch >= '0' && ch <= '9')
        digit = ch - '0';
      else
        digit = (ch - 'A') + 10;
      charno = (charno * 16) + digit;
    }
    return (char) charno;
  }
  
  private String parseuri() {
    int start = pos + 1; // skip initial '<'
    while (line.charAt(pos) != '>')
      pos++;
    pos++; // skip final '>'
    return line.substring(start, pos - 1);
  }

  private String parseliteral() {
    pos++; // skip initial quote
    int start = pos; 
    while (line.charAt(pos) != '"') {
      if (line.charAt(pos) == '\\')
        pos++; // skip escaped char (we decode later)
      pos++;
    }
    int end = pos;
    pos++; // skip final quote

    if (line.charAt(pos) == '^')
      parsedatatype();
    else if (line.charAt(pos) == '@')
      parselangtag();
    
    return line.substring(start, end);
  }

  private void parsedatatype() {
    pos++; // skip first ^
    if (line.charAt(pos++) != '^')
      throw new RuntimeException("Incorrect start of datatype");
    if (line.charAt(pos) != '<')
      throw new RuntimeException("Datatype URI does not start with '<'");
    parseuri();
  }

  private void parselangtag() {
    pos++; // skip the '@'
    char ch = line.charAt(pos++);
    while ((ch >= 'a' && ch <= 'z') ||
           (ch >= '0' && ch <= '9') ||
           ch == '-') 
      ch = line.charAt(pos++);
  }

  private String parsebnode() {
    int start = pos;

    pos++; // skip '_'
    if (line.charAt(pos++) != ':')
      throw new RuntimeException("Incorrect start of blank node");

    char ch = line.charAt(pos++);
    while ((ch >= 'A' && ch <= 'Z') ||
           (ch >= 'a' && ch <= 'z') ||
           (ch >= '0' && ch <= '9')) 
      ch = line.charAt(pos++);

    return line.substring(start, pos - 1);
  }

  private void skipws() {
    while (pos < line.length()) {
      char ch = line.charAt(pos);
      if (!(ch == ' ' || ch == '\t'))
        break;
      pos++;
    }
  }
}