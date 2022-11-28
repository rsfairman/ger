package vcnc.lex;

/*

To represent a single "token" of some G-code. E.g., G40, G00, O1245, S4800, etc.
These are created by the Lexer and will typically end up in a TokenBuffer.

There are a couple of tokens that are used internally, and are not letters:
* means EOF. Need this to indicate that the lexer ran off the end of the file.
; line-feed. These often appear in the code, but many times they are redundant.
  It is used here when it's needed, like after a move. I haven't been super 
  careful about eliminating them when they are redundant, so the parser may see
  these occasionally when they're just whitespace.
! Used for errors.
@ Used to indicate a "wizard."
s (lower-case) for string constants.
n (lower-case) for numbers.


So, all tokens take either the form letter+value (like G+number or X+number)
OR, if you're in "reading a wizard function call" mode, then tokens will be
the original function call, followed by a sequence of numbers (doubles only)
and strings. The function call is terminated by new-line or semicolon.

Note that, for G/M-codes that are unknown to the system, the lexer assumes
that they follow the usual form. That is, after the initial G/M-whatever,
there will be a series of letter/number pairs.
 
*/


public class Token {
  
  // The special values for this.letter.
  public static final char EOF = '*';
  public static final char EOL = ';';
  public static final char ERROR = '!';
  public static final char WIZARD = '@';
  public static final char STRING = 's';
  public static final char NUMBER = 'n';
  
  // All tokens have some kind of letter associated with them: G, M, X or 
  // whatever. Sometimes this is used more like a type, like for Wizards,
  // but 'letter' seems to fit better than 'type'.
  public char letter = ' ';
  
  // Some tokens have an integer associated with them; others use a double.
  // I could save a tiny amount of space by being more careful and not 
  // allocating both types here, but it's not worth the trouble.
  public int i = 0;
  public double d = 0.0;
  
  // And G/M codes may have a decimal point, like G82.4. This could be treated
  // as a double, but that's not the actual type. These values are being used
  // as labels, not numeric values. So, if there is a decimal point in a 
  // G-code, then any additional whole number after the decimal appears here.
  // A more theoretically correct approach would be to treat these codes as
  // strings, but that's messier to code.
  public int isub = -1;
  
  // And externally defined functions (wizards) have a name.
  // This field is also used for string arguments to wizards.
  // Again, I could use some method of typing with sub-classes of Token so
  // that none of these sub-classes have unused fields, but it's not worth
  // the effort or the resulting messiness. Effectively, the letter field
  // acts as the type.
  public String wizard = null;
  
  // The lexer needs to be able to report errors. This is the error message that
  // should be reported if this.letter == '!'. This will generally be null.
  public String error = null;
  
  // If the parser finds an error, then it needs to know the line number on 
  // which the error occurred. This is the line number from which each token 
  // came.
  public int lineNumber = 0;
  
  // Rarely (like when parsing to find subprograms), we also need to know the
  // character at which a token occurs. This is the character count from the 
  // 0-th character of the source code at which the first character of the 
  // given token occurs.
  public int characterCount = -1;
  
  // This points to the first character AFTER the characters that make up the 
  // current token. This is needed so that we can return from subroutines.
  public int endCount = -1;
  
  public Token(char c,int lineNumber) {
    this.letter = c;
    this.lineNumber = lineNumber;
  }
  
  public String toString() {
    
    // This serves two purposes. It produces output for the user to view
    // when debugging his g-code. It is also used for unit tests.
    
    // BUG: Not really complete. There are cases of tokens this doesn't handle.
    String answer = String.format("%c",letter);
    
    if (letter == WIZARD)
      answer += " " + wizard;
    else if (letter == 'n')
      answer += " " + d;
    else if (letter == 's')
      answer += " " +wizard;
    else if (letter == '!')
      answer += " " + error;
    
    return answer;
  }
}


