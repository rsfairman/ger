package vcnc.tpile.lex;

/*

Produce a series of Token objects that are fed to the Parser.

*/

import java.util.ArrayList;

import vcnc.tpile.CodeBuffer;


public class Lexer {
  
  // This is where the characters of code come from.
  private CodeBuffer theCode = null;
  
  // The line currently being read. This is needed to report errors.
  private int lineCount = 1;
  
  // Go into wizard mode when a token starts with two letters. Leave wizard
  // mode when you reach EOL. We need to distinguish this mode because tokens 
  // are generated one-at-a-time. 
  private boolean wizardMode = false;
  
  // Used when parsing (and discarding) comments.
  private final int CommentClosedNormally = 0;
  private final int CommentNotClosed = 1;
  private final int CommentNested = 2;
  
    
  private Lexer(CodeBuffer gCode) {

    this.theCode = gCode;
    this.lineCount = 1;
  }
  
  private String formError(int badLine,String msg) {
    return "Lexical error on line " +badLine+ ": " + msg;
  }
  
  private char getc() {
    
    // This is the only place that gets characters of code. All code flows 
    // through this method.
    char answer;
    try {
      answer = theCode.getc();
    } catch (Exception e) {
      // Assume that the only problem could be EOF.
      answer = Token.EOF;
    }
    
    // Use ';' (Token.EOL) for new lines. 
    if (answer == '\n')
      {
        answer = Token.EOL;
        ++lineCount;
      }
    
    return answer;
  }
  
  private char peekc() {
    
    char answer;
    try {
      answer = theCode.peekc();
    } catch (Exception e) {
      answer = Token.EOF;
    }
    return answer;
  }
  
  private void advanceToEOL() {
  
    // Occasionally, when there's an error in the input code, the best way to
    // get out of it is to skip to the EOL and ignore everything up to that 
    // point.
    char c = getc();
    while ((c != Token.EOL) && (c!= Token.EOF))
      c = getc();
  }
  
  private int readComment() {
    
    // Read and ignore a comment. It is assumed an open-parenthesis has just 
    // been read. Recall that comments in G-code are simply (...). Anything
    // in parenthesis is a comment.
    // 
    // NOTE: I have written this to accept multi-line comments. Not all 
    // controllers allow them. This does NOT allow nested comments.
    // 
    // This returns one of:
    // 0 means everything was normal -- the comment was closed:
    //   CommentClosedNormally
    // 1 means that the comment was never closed; it ran off the end of the 
    //   file : CommentNotClosed.
    // 2 means that a '(' appeared inside the comment. Most likely, the user
    //   used a ')' shortly thereafter because he had a short remark (like 
    //   this). By flagging these separately, we're being nice to the user; 
    //   otherwise, he would get a long string of mysterious lexical errors. I was
    //   bitten by this myself, which is why I added this flag: CommentNested.
    // These constants are defined above.
    
    char c = getc();
    
    // Loop until comment is closed or we reach EOF.
    while ((c != ')') && (c != Token.EOF))
      {
        if (c == '(')
          // Not allowed to use '(' inside a comment.
          return CommentNested;
        
        c = getc();
      }
     
    if (c == Token.EOF)
      return CommentNotClosed;
    else
      return CommentClosedNormally;
  }

  private void readSpaces() {
    
    // Read and discard any spaces. This does not read (or discard) comments, 
    // tabs or carriage returns. This is meant to be used to allow things like 
    // "X   5.0" with spaces between the letter token and the value.
    while (true)
      {
        char c = peekc();
        if (c == Token.EOF)
          return;
        if (c == ' ')
          c = getc();
        else
          return;
      }
  }

  private int readWhite() {
    
    // Read and discard any whitespace: spaces, tabs, comments and carriage 
    // returns. NOTE: \n is not considered to be white space, but \r is.
    // This returns values with the same meaning as in readComment(), but where 
    // CommentClosedNormally is extended to mean "everything was normal."
    while (true)
      {
        char c = peekc();
        if (c == Token.EOF)
          return CommentClosedNormally;
        
        // NOTE: I am a bit worried about \n and \r. I have assumed that all 
        // machines will use \n for a newline, but some machines also have a 
        // \r, which can always be ignored.
        if ((c == ' ') || (c == '\t') || (c == '\r'))
          {
            // Keep reading
            char ignore = getc();
          }
        else if (c == '(')
          {
            // Opening comment. Advance to make the previous peekc() a getc().
            char ignore = getc();
            int comClose = readComment();
            if (comClose != CommentClosedNormally)
              return comClose;
          }
        else
          // Next character is not white space. Exit.
          return CommentClosedNormally;
      }
  }
  
  private String readString() throws MultilineStringException {
    
    // The caller has just read a single ". Everything up to the closing "
    // is treated as a single string. The closing " is read and discarded.
    // All strings must be on a single line -- no newline (\n) allowed inside.
    // 
    // BUG: This doesn't allow escape characters, like \" or \n or whatever.
    String answer = new String();
    
    char c = getc();
    while (c != '"')
      {
        if ((c == '\n') || (c == Token.EOF))
          throw new MultilineStringException("");
        
        answer = answer + c;
        c = getc();
      }
    
    return answer;
  }
  
  private int readWholeNumber() throws ExpectedWholeNumberException {
    
    // Read and return a whole number whose first digit is ready to be read. 
    // These may be line numbers, G-code numbers, M-code numbers, etc.
    
    // Read digits, putting them into a String to be parsed.
    String buf = new String();
    char c = peekc();
    if (Character.isDigit(c) == false)
      throw new ExpectedWholeNumberException(
          formError(lineCount,"expected whole number"));
    
    while (Character.isDigit(c))
      {
        c = getc();
        buf = buf + Character.toString(c);
        c = peekc();
      }
    
    // Number is empty.
    if (buf.length()<= 0)
      throw new ExpectedWholeNumberException(
          formError(lineCount,"expected whole number"));
    
    try {
      int answer = Integer.parseInt(buf);
      return answer;
    } catch (Exception e) {
      throw new ExpectedWholeNumberException(
          formError(lineCount,"expected whole number"));
    }
  }
  
  private double readDouble() throws ExpectedFloatNumberException {
    
    // Similar to readWholeNumber(), but the value is a floating point number.

    // Read digits, putting them into a String to be parsed.
    String buf = new String();
    char c = peekc();
    if ((Character.isDigit(c) || (c == '+') || (c == '-') || (c == '.')) == false)
      throw new ExpectedFloatNumberException(
          formError(lineCount,"expected decimal number"));
    
    while (Character.isDigit(c) || (c == '+') || (c == '-') || (c == '.'))
      {
        c = getc();
        buf = buf + Character.toString(c);
        c = peekc();
      }
    
    // Number is empty.
    if (buf.length() <= 0)
      throw new ExpectedFloatNumberException(
          formError(lineCount,"expected decimal number"));
    
    try {
      double answer = Double.parseDouble(buf);
      return answer;
    } catch (Exception e) {
      throw new ExpectedFloatNumberException(
          formError(lineCount,"expected decimal number"));
    }
  }
  
  private Token readDCode() {
    
    // Refers to a register number, a whole number.
    Token answer = new Token('D',lineCount);
    try {
      answer.i = readWholeNumber();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed reference to D register");
    }
    return answer;
  }
  
  private Token readFCode() {
    
    // As with G-codes, and the value is floating point.
    Token answer = new Token('F',lineCount);
    try {
      answer.d = readDouble();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed feed rate");
    }
    return answer;
  }

  private Token readGCode() {
    
    // Read a G-code. It is assumed that the "G" has already been read. So we 
    // are reading the digits of a number, then stopping.
    Token answer = new Token('G',lineCount);
    try {
      
      // BUG: changing... Not sure why I was using d (double) here.
      //answer.d = readWholeNumber();
      
      
      answer.i = readWholeNumber();
      
      // Maybe this is a GXX.XX type code, with a "subpart" to the number.
      char c = peekc();
      if (c == '.')
        {
          c = getc();
          answer.isub = readWholeNumber();
        }
      
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed G-code");
    }
    return answer;
  }
  
  private Token readHCode() {
    
    // Refers to a register number, a whole number.
    Token answer = new Token('H',lineCount);
    try {
      answer.i = readWholeNumber();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed reference to H register");
    }
    return answer;
  }
  
  private Token readICode() {
    
    // Used with circular interpolation. A double.
    Token answer = new Token('I',lineCount);
    try {
      answer.d = readDouble();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed I-value");
    }
    return answer;
  }
  
  private Token readJCode() {
    
    // Used with circular interpolation. A double.
    Token answer = new Token('J',lineCount);
    try {
      answer.d = readDouble();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed J-value");
    }
    return answer;
  }
  
  private Token readKCode() {
    
    // Used with circular interpolation. A double.
    Token answer = new Token('K',lineCount);
    try {
      answer.d = readDouble();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed K-value");
    }
    return answer;
  }
  
  private Token readLCode() {
    
    // This is typically the number of calls to a subroutine. A whole number.
    Token answer = new Token('L',lineCount);
    try {
      answer.i = readWholeNumber();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed L-value");
    }
    return answer;
  }
  
  private Token readMCode() {
    
    // As with G-codes.
    Token answer = new Token('M',lineCount);
    try {
      answer.i = readWholeNumber();
      
      // Maybe this is a MXX.XX type code, with a "subpart" to the number.
      // Any such thing would be *highly* non-standard, but there's no harm
      // in allowing for it.
      char c = peekc();
      if (c == '.')
        {
          c = getc();
          answer.isub = readWholeNumber();
        }
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed M-code");
    }
    return answer;
  }
  
  private Token readNCode() {
    
    // Line number, similar to G-code.
    Token answer = new Token('N',lineCount);
    try {
      answer.i = readWholeNumber();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed line number");
    }
    return answer;
  }
  
  private Token readOCode() {
    
    // Similar to G-code.
    Token answer = new Token('O',lineCount);
    
    // This is one of the rare situations where I care about the character at
    // which this occurred since we need it for subroutines.
    // BUG: DO I care??? Things have changed and character counts may not
    // be needed anymore.
    // This is the character at which the "O" occurs, with 0 as the first 
    // character.
    answer.characterCount = theCode.getLastCharIndex()-1;
    try {
      answer.i = readWholeNumber();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed O-value");
    }
    return answer;
  }
  
  private Token readPCode() {
    
    // Typically used for calling subroutines. I think that
    // this may also be used for dwell time. For now, consider this to
    // be a double.
    Token answer = new Token('P',lineCount);
    try {
      answer.d = readDouble();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed P-value");
    }
    return answer;
  }
  
  private Token readQCode() {
    
    // This is another one that might have multiple uses, so treat it as a 
    // double for now. I think it could be used to pass parameters to canned 
    // cycles or be the number of repetitions for a subroutine call.
    Token answer = new Token('Q',lineCount);
    try {
      answer.d = readDouble();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed Q-value");
    }
    return answer;
  }
  
  private Token readRCode() {
    
    // Radius for circular interpolation. Double.
    Token answer = new Token('R',lineCount);
    try {
      answer.d = readDouble();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed R-value");
    }
    return answer;
  }
  
  private Token readSCode() {
    
    // Similar to G-code. This assumes that spindle speed is a whole number.
    Token answer = new Token('S',lineCount);
    try {
      answer.i = readWholeNumber();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed spindle speed");
    }
    return answer;
  }
  
  private Token readTCode() {
    
    // Tool number. An integer.
    Token answer = new Token('T',lineCount);
    try {
      answer.i = readWholeNumber();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed T-value");
    }
    return answer;
  }
  
  private Token readUCode() {
    
    // I think (?) that this is used for dwell time occasionally. Treat it as
    // a double.
    Token answer = new Token('U',lineCount);
    try {
      answer.d = readDouble();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed U-value");
    }
    return answer;
  }
  
  private Token readXCode() {
    
    // As with G-codes, but the value is floating point.
    Token answer = new Token('X',lineCount);
    try {
      answer.d = readDouble();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed X value");
    }
    return answer;
  }
  
  private Token readYCode() {
    
    // As with G-codes, but the value is floating point.
    Token answer = new Token('Y',lineCount);
    try {
      answer.d = readDouble();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed Y value");
    }
    return answer;
  }
  
  private Token readZCode() {
    
    // As with G-codes, but the value is floating point.
    Token answer = new Token('Z',lineCount);
    try {
      answer.d = readDouble();
    } catch (Exception e) {
      answer.letter = Token.ERROR;
      answer.error = formError(lineCount,"malformed Z value");
    }
    return answer;
  }
  
  private Token readWizardToken() {
    
    // Read a number, string, EOL or EOF. Any whitespace has already been read.
    Token answer = null;
    
    // There are only two possible types of token here: numbers and strings.
    char c = peekc();
    
    if (c == Token.EOF)
      {
        c = getc();
        answer = new Token(Token.EOF,lineCount);
      }
    else if (c == '\n')
      {
        c = getc();
        answer = new Token(Token.EOL,lineCount);
      }
    else if (c == '"')
      {
        // Must be a string.
        c = getc();
        answer = new Token(Token.STRING,lineCount);
        try {
          answer.wizard = readString();
        } catch (Exception e) {
          answer.letter = Token.ERROR;
          answer.error = formError(lineCount,
              "multi-line strings not allowed");
        }
      }
    else
      {
        // Has to be a number.
        answer = new Token(Token.NUMBER,lineCount);
        try {
          answer.d = readDouble();
        } catch (Exception e) {
          answer.letter = Token.ERROR;
          answer.error = formError(lineCount,
              "malformed number in argument to wizard");
        }
      }
    
    return answer;
  }
  
  private Token readWord(char c) {
    
    // We've read c from the stream, and have already peeked ahead
    // so that we know that the token is a "word" consisting of alphanumeric
    // characters (not a G-code). Read up through all alphanumeric characters.
    Token answer = new Token(Token.WIZARD,lineCount);
    
    String buf = new String();
    buf = buf + c;
    
    c = peekc();
    while (Character.isLetterOrDigit(c))
      {
        c = getc();
        buf = buf + c;
        c = peekc();
      }
    
    answer.wizard = buf;
    return answer;
  }
  
  private Token readToken() {
    
    // Read and return an entire Token.
    Token answer = null;
    
    int closed = readWhite();
    if (closed == CommentNotClosed)
      {
        // NOTE: Nested comments lead to all kinds of "extra" errors in the
        // output. It might be less confusing to the user if the entire
        // process just halted here.
        answer = new Token(Token.ERROR,lineCount);
        answer.error = formError(lineCount,"comment not closed");
        return answer;
      }
    if (closed == CommentNested)
      {
        answer = new Token(Token.ERROR,lineCount);
        answer.error = formError(lineCount,
            "use of '(' inside comments not allowed.");
        return answer;
      }
    
    if (wizardMode == true)
      {
        // If in wizard mode, then we have already read the single-word
        // name of the wizard function. We are now reading a series of
        // numbers and strings up till we reach EOL.
        answer = readWizardToken();
        
        if ((answer.letter == Token.EOL) || (answer.letter == Token.EOF) ||
            (answer.letter == Token.ERROR))
          this.wizardMode = false;
        
        if (answer.letter == Token.ERROR)
          advanceToEOL();
        
        answer.endCount = theCode.getLastCharIndex();
        return answer;
      }
    
    // Not yet in wizard mode, but we might be entering it.
    char c = getc();
    char pc = peekc();
    
    if ((Character.isLetter(c) == true) && (Character.isLetter(pc) == true))
      {
        // The next token starts with a *pair* of letters, so it must be
        // a call to an external wizard function.
        this.wizardMode = true;
        answer = readWord(c);
        
        answer.endCount = theCode.getLastCharIndex();
        return answer;
      }
    
    // Got here, so it's a normal G-code. Nothing to do with wizard functions.
    readSpaces();
    
    switch (c)
      {
        case Token.EOF  :  
        case Token.EOL  : answer = new Token(c,lineCount); break;
        
        case 'D'        : answer = readDCode(); break;
        case 'F'        : answer = readFCode(); break;
        case 'G'        : answer = readGCode(); break;
        case 'H'        : answer = readHCode(); break;
        case 'I'        : answer = readICode(); break;
        case 'J'        : answer = readJCode(); break;
        case 'K'        : answer = readKCode(); break;
        case 'L'        : answer = readLCode(); break;
        case 'M'        : answer = readMCode(); break;
        case 'N'        : answer = readNCode(); break;
        case 'O'        : answer = readOCode(); break;
        case 'P'        : answer = readPCode(); break;
        case 'Q'        : answer = readQCode(); break;
        case 'R'        : answer = readRCode(); break;
        case 'S'        : answer = readSCode(); break;
        case 'T'        : answer = readTCode(); break;
        case 'U'        : answer = readUCode(); break;
        case 'X'        : answer = readXCode(); break;
        case 'Y'        : answer = readYCode(); break;
        case 'Z'        : answer = readZCode(); break;
        
        default   : answer = new Token(Token.ERROR,lineCount);
                    answer.error = formError(lineCount,"unknown code: " +c);
                    return answer;            
      }
    
    // Tack on the index of the character following the one most
    // recently read.
    answer.endCount = theCode.getLastCharIndex();
    return answer;
  }
  
  public static ArrayList<Token> process(String gCode) {
    
    // Convert the entire G-code program to an array of Token objects.
    ArrayList<Token> answer = new ArrayList<>();
    
    Lexer lex = new Lexer(new CodeBuffer(gCode));
    
    Token t = lex.readToken();
    while (t.letter != Token.EOF)
      {
//        System.out.println("Token: " +t.toString());
        
        answer.add(t);
        t = lex.readToken();
      }
    
    return answer;
  }

  public static String digestAll(String gcode) {
    
    // Take the given g-code and feed it through, producing a single String
    // suitable for output to the user, or for use with unit tests.
    ArrayList<Token> theTokens = process(gcode);

    StringBuffer answer = new StringBuffer();
    
    for (Token t : theTokens)
      {
        answer.append(t.toString());
        answer.append("\n");
      }
    
    return answer.toString();
  }
  
}



