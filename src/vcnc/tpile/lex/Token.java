package vcnc.tpile.lex;

/*

To represent a single "token" of some G-code. E.g., G40, G00, O1245, S4800, etc.

There are a couple of tokens that are used internally, and are not letters:
\u0000 
  means EOF. Need this to indicate that the lexer ran off the end of the file.
  It shouldn't appear in the output of the lexer.
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
  // At one time, I used '*' for EOF, but that presents a problem if a '*'
  // appears in an comment.
  // NOTE: The real solution is an enumerated type, but that's bloatation.
  public static final char EOF = '\u0000';
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
  // 
  // NOTE that this is not used in practice. These commands, that use a 
  // decimal point in their number, vary too much from machine to machine
  // to be handled with a reasonable amount of effort. See the comment in Main
  // for v06.
  //
  // If I firmly choose to abandon handling these kinds of G-codes, then
  // I could get rid of this and there are some minor simplifications this
  // would permit in the lexer too. It seems better to keep this since
  // there are codes like this, even if they are uncommon, and I might
  // want to deal with them.
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
  // came. Prior to v14, we also tracked the character count, but it wasn't
  // really being used.
  public int lineNumber = 0;
  
  
  public Token(char c,int lineNumber) {
    this.letter = c;
    this.lineNumber = lineNumber;
  }
  
  public String toString() {
    
    // This serves two purposes. It produces output for the user to view
    // when debugging his g-code. It is also used for unit tests.
    // Note that each line does *not* have a '\n' at the end.
    String answer = lineNumber + "\t";

    if (letter == Token.EOL)
      answer += ";";
    else if (letter == Token.WIZARD)
      answer += "extern\t" + wizard;
    else if (letter == Token.STRING)
      answer += "string\t" + wizard;
    else if (letter == Token.NUMBER)
      answer += "num\t" + d;
    else if (letter == Token.ERROR)
      answer += "error\t" + error;
    else
      // Normal G/M/whatever-code
      // NOTE: Ignoring this.isub.
      answer += letter+ "\t" +i+ "\t" + d;
    
    return answer;
  }
}


