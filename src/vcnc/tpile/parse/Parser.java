package vcnc.tpile.parse;

/*

Converts G-code to a series of statement objects. Each statement is a self-
contained G-code "command," including the possibility of wizards commands.

This creates a Lexer and consumes Token objects from it.
 
*/

import java.util.ArrayList;

import vcnc.tpile.StxP;
import vcnc.tpile.lex.Lexer;
import vcnc.tpile.lex.Token;


public class Parser {
  
  // The tokens to be parsed to statements, from the Lexer.
  private ArrayList<Token> theTokens = null;
  
  // Where we are in the parsing process -- the mark relative to theTokens.
  private int tokenIndex = 0;
  
  
  private Parser(ArrayList<Token> toks) {
    
    this.theTokens = toks;
    this.tokenIndex = 0;
  }

  private Token getToken() {
    
    if (tokenIndex >= theTokens.size())
      return new Token(Token.EOF,0);
    
    Token answer = theTokens.get(tokenIndex);
    ++tokenIndex;
    return answer;
  }
  
  private Token peekToken() {
    
    if (tokenIndex >= theTokens.size())
      return new Token(Token.EOF,0);
    
    return theTokens.get(tokenIndex);
  }
  
  private String formError(int lineNumber,String msg) {
    return "Error on line " +lineNumber+ ": " + msg;
  }
  
  private void readWhite() {
    
    // Read any "junk" tokens. The following are ignored: extra EOL's,
    // and line numbers (N).
    Token peek = peekToken();
    while ((peek.letter == Token.EOL) || (peek.letter == 'N'))
      {
        Token trash = getToken();
        peek = peekToken();
      }
  }

  private void readToEOLOrSemi() {
    
    // Read from the current token to the EOL, trashing these tokens.
    // This is used for a few things, like G04 (dwell) that the program ignores.
    // Instead of trying to parse them, we just read everything that might have
    // anything to do with it, and send it to the trash. This is also used when
    // an error is detected, as a way of trying to move beyond the end of it.
    
    // BUG: NO, DON'T TRASH THINGS YOU DON'T UNDERSTAND (YET). THEY SHOULD PASS
    // THROUGH UNCHANGED. ESSENTIALLY, THESE THINGS SHOULD EXTEND TO THE EOL,
    // AND JUST ACCEPT THAT THEY ARE VALID TO WHATEVER MACHINE IS THE FINAL
    // CONSUMER.
    // SO BE CAREFUL WHERE THIS IS CALLED
    Token peek = peekToken();
    while (peek.letter != Token.EOL)
      {
        Token trash = getToken();
        peek = peekToken();
      }
  }
  
  private void parseWizard(StxP answer,Token startToken) {
    
    // Have just read a wizard command. Parse to the end of the line.
    answer.type = StxP.WIZARD;
    
    DataWizard wiz = new DataWizard();
    answer.data = wiz;
    
    wiz.cmd = startToken.wizard;
    
    // The arguments to the wizard consist of everything from the wizard
    // name to the EOL. This may or may not be the correct set of arguments,
    // but that will be determined later.
    
    Token t = getToken();
    while (t.letter != Token.EOL)
      {
        if (t.letter == Token.STRING)
          wiz.args.add(t.wizard);
        else if (t.letter == Token.NUMBER)
          wiz.args.add(t.d);
        else
          {
            answer.type = StxP.ERROR;
            answer.error = formError(answer.lineNumber,
                "Unexpected argument to wizard");
            return;
          }
        
        t = getToken();
      }
    
    return;
  }
  
  private void parseGCode(StxP answer,Token startToken) {
    
    // Have already read the G-something token. Read anything else needed to 
    // form a complete statement.
    
    if (startToken.i == 0)
      answer.type = StxP.G00;
    else if (startToken.i == 1)
      answer.type = StxP.G01;
    else if ((startToken.i == 2) || (startToken.i == 3))
      {
        // Circular interpolation. Both cases are similar.
        if (startToken.i == 2) answer.type = StxP.G02;
        if (startToken.i == 3) answer.type = StxP.G03;
        
        // What follows must be some combination of the following:
        // X## Y## Z## I## J## K## R## F##
        // The entire command may or may not be terminated by F## and
        // the statement may use I/J/K or R to specify the radius, but not both.
        
        // So, the next token must be X, I or R.
        DataCircular data = new DataCircular();
        answer.data = data;
        Token next = peekToken();
        
        // Get the X,Y (if any). If they are not defined, then this is a
        // complete circle (and there must be an I and/or J).
        if (next.letter == 'X')
          {
            // Read X and Y.
            next = getToken();
            data.xDefined = true;
            data.X = next.d;
            next = peekToken();
            
            // There may or may not be a Y and Z.
            if (next.letter == 'Y')
              {
                next = getToken();
                data.yDefined = true;
                data.Y = next.d;
                next = peekToken();
              }
            if (next.letter == 'Z')
              {
                next = getToken();
                data.zDefined = true;
                data.Z = next.d;
                next = peekToken();
              }
          }
        else if (next.letter == 'Y')
          {
            // There was no X, but there is a Y and there might be a Z.
            next = getToken();
            data.yDefined = true;
            data.Y = next.d;
            next = peekToken();

            if (next.letter == 'Z')
              {
                next = getToken();
                data.zDefined = true;
                data.Z = next.d;
                next = peekToken();
              }
          }
        else if (next.letter == 'Z')
          {
            // There was no X or Y, but there is a Z.
            next = getToken();
            data.zDefined = true;
            data.Z = next.d;
            next = peekToken();
          }
        
        // Have read X,Y,Z, if any. Next will be I/J/K or R.
        if (next.letter == 'I')
          {
            // Read I,J,K.
            next = getToken();
            data.iDefined = true;
            data.I = next.d;
            next = peekToken();
            
            // See if there is a J,K.
            if (next.letter == 'J')
              {
                next = getToken();
                data.jDefined = true;
                data.J = next.d;
                next = peekToken();
              }
            if (next.letter == 'K')
              {
                next = getToken();
                data.kDefined = true;
                data.K = next.d;
                next = peekToken();
              }
          }
        else if (next.letter == 'J')
          {
            // No I, but there is a J and maybe a K.
            next = getToken();
            data.jDefined = true;
            data.J = next.d;
            next = peekToken();
            
            if (next.letter == 'K')
              {
                next = getToken();
                data.kDefined = true;
                data.K = next.d;
                next = peekToken();
              }
          }
        else if (next.letter == 'K')
          {
            // No I or J, but we have K.
            next = getToken();
            data.kDefined = true;
            data.K = next.d;
            next = peekToken();
          }
        else if (next.letter == 'R')
          {
            // No I,J,K; R instead.
            next = getToken();
            data.rDefined = true;
            data.R = next.d;
            next = peekToken();
          }
        else
          {
            // Something's not right. G02/G03 is missing both I,J,K and R.
            answer.type = StxP.ERROR;
            answer.error = formError(answer.lineNumber,
                "G02/03 improperly specified");
            readToEOLOrSemi();
            return;
          }
        
        // The entire thing might be followed by a feed rate.
        next = peekToken();
        if (next.letter == 'F')
          {
            next = getToken();
            data.F = next.d;
          }
        
        // Make sure that this makes sense. If the user does not give X or Y,
        // and he tries to used R instead of I/J, that doesn't make sense.
        // Other things that don't make sense are giving R along with I and/or
        // J, though the way this is written, the latter would be caught.
        if ((data.xDefined == false) && (data.yDefined == false) 
            && (data.zDefined == false) && (data.rDefined == true))
            {
              answer.type = StxP.ERROR;
              answer.error = formError(startToken.lineNumber,
                  "radius given, but no end-point");
              readToEOLOrSemi();
            }
      } // end circular interpolation case.
    else if (startToken.i == 15)
      // Polar coordinates off.
      answer.type = StxP.G15;
    else if (startToken.i == 16)
      // Polar coordinates on.
      answer.type = StxP.G16;
    else if (startToken.i == 17)
      // Choose coordinate axes.
      answer.type = StxP.G17;
    else if (startToken.i == 18)
      // Choose coordinate axes.
      answer.type = StxP.G18;
    else if (startToken.i == 19)
      // Choose coordinate axes.
      answer.type = StxP.G19;
    else if (startToken.i == 20)
      answer.type = StxP.G20;
    else if (startToken.i == 21)
      answer.type = StxP.G21;
    else if (startToken.i == 28)
      answer.type = StxP.G28;
    else if (startToken.i == 40)
      // Cancel cutter comp.
      answer.type = StxP.G40;
    else if ((startToken.i == 41) || (startToken.i == 42)) 
      {
        // Apply cutter comp. After the G41 or G42, this takes the form
        // [D## | H##] G## X## Y## Z## F##,
        // where the second G## and the F## are optional.
        // Strictly speaking, I think that we could treat everything after the
        // G41 D## 
        // (or some combination of G41/42 and D/H) as a distinct statement.
        // What G41/42 really does is enter into cutter comp mode.
        // This is what I will do. Whatever follows the G41/42 D/H##
        // must be read as a separate statement.
        DataRegister data = new DataRegister();
        if (startToken.i == 41) answer.type = StxP.G41;
        if (startToken.i == 42) answer.type = StxP.G42;
        
        // Next token should be D or H.
        Token next = getToken();
        if (next.letter == 'D')
          data.D = true;
        else if (next.letter == 'H')
          data.D = false;
        else
          {
            answer.type = StxP.ERROR;
            answer.error = formError(answer.lineNumber,
                "G41/42 not followed by H or D");
            readToEOLOrSemi();
            return;
          }
        data.regValue = next.i;
        answer.data = data;
      } // end cutter comp
    else if ((startToken.i == 43) || (startToken.i == 44))
      {
        // Apply TLO. Similar to cutter comp.
        // Takes the form
        // G43 [Zx.xx] Hn
        if (startToken.i == 43) answer.type = StxP.G43;
        if (startToken.i == 44) answer.type = StxP.G44;
        DataTLO data = new DataTLO();
        
        // See if there's an optional Z value.
        Token next = getToken();
        if (next.letter == 'Z')
          {
            // Read the value.
            data.hasZ = true;
            data.zValue = next.d;
            next = getToken();
          }
          
        // Now there must be an H value
        if (next.letter != 'H')
          {
            answer.type = StxP.ERROR;
            answer.error = formError(answer.lineNumber,
                "G43/G44 not followed by H register");
            readToEOLOrSemi();
            return;
          }
        
        data.hRegister = next.i;
        answer.data = data;
      } // end TLO
    else if (startToken.i == 49)
      // Cancel TLO.  
      answer.type = StxP.G49;
    else if (startToken.i == 52)
      {
        // Temporary change in PRZ (or return to original). This must
        // be followed by some X Y and Z values. This uses a MoveState
        // object for these coordinates, even though the F-value is unused.
        // It would usually (I guess) be an error not to have X and Y, but I
        // assume that if any of these is omitted, it must not be changed.
        answer.type = StxP.G52;
        
        boolean xDefined = false;
        boolean yDefined = false;
        boolean zDefined = false;
        double xValue = 0.0;
        double yValue = 0.0;
        double zValue = 0.0;
        
        Token nextToken = getToken();
        if (startToken.letter == 'X')
          {
            xDefined = true;
            xValue = startToken.d;
          }
        else if (startToken.letter == 'Y')
          {
            yDefined = true;
            yValue = startToken.d;
          }
        else  if (startToken.letter == 'Z')
          {
            zDefined = true;
            zValue = startToken.d;
          }
        else
          {
            answer.error = formError(answer.lineNumber,
                "G52 not followed by coordinates");
            readToEOLOrSemi();
            return;
          }
        
        // Continue for up to two more letters.
        Token peekToken = peekToken();
        while ((peekToken.letter == 'X') || (peekToken.letter == 'Y') || 
            (peekToken.letter == 'Z'))
          {
            // Go ahead and get it instead of just peeking.
            peekToken = getToken();
            if (peekToken.letter == 'X')
              {
                if (xDefined == true)
                  {
                    // Error. Already read this value.
                    answer.type = StxP.ERROR;
                    answer.error = formError(answer.lineNumber,"two x-values");
                    readToEOLOrSemi();
                    return;
                  }
                xDefined = true;
                xValue = peekToken.d;
              }
            else if (peekToken.letter == 'Y')
              {
                if (yDefined == true)
                  {
                    // Error. Already read this value.
                    answer.type = StxP.ERROR;
                    answer.error = formError(answer.lineNumber,"two y-values");
                    readToEOLOrSemi();
                    return;
                  }
                yDefined = true;
                yValue = peekToken.d;
              }
            else
              {
                if (zDefined == true)
                  {
                    // Error. Already read this value.
                    answer.type = StxP.ERROR;
                    answer.error = formError(answer.lineNumber,"two z-values");
                    readToEOLOrSemi();
                    return;
                  }
                zDefined = true;
                zValue = peekToken.d;
              }
            
            // Peek to the next one.
            peekToken = peekToken();
          }
        
        // Statement has been read. Combine the data into a single object.
        answer.data = new DataMove(xDefined,yDefined,zDefined,false,
                                    xValue,yValue,zValue,0.0);
      } // end G52
    else if ((startToken.i >= 54) && (startToken.i <= 59))
      {
        // Work offsets.
        if (startToken.i == 54) answer.type = StxP.G54;
        if (startToken.i == 55) answer.type = StxP.G55;
        if (startToken.i == 56) answer.type = StxP.G56;
        if (startToken.i == 57) answer.type = StxP.G57;
        if (startToken.i == 58) answer.type = StxP.G58;
        if (startToken.i == 59) answer.type = StxP.G59;
      }
    else if (startToken.i == 90)
      // Absolute mode.
      answer.type = StxP.G90;
    else if (startToken.i == 91)
      // Incremental mode.
      answer.type = StxP.G91;
    else
      {
        // Catch any G-code that is not handled above and flag it as an error.
        answer.type = StxP.ERROR;
        answer.error = formError(answer.lineNumber,
            "unknown G-code: " +startToken.i);
        readToEOLOrSemi();
      }
  }

  private void parseMCode(StxP answer,Token startToken) {
    
    // Have already read the M-something token. Read anything else needed to 
    // form a complete statement.
    if (startToken.i == 0)
      // Program Stop. Treated as a hard stop.
      answer.type = StxP.M00;
    else if (startToken.i == 1)
      // Program stop. I think this is an optional stop.
      answer.type = StxP.M01;
    else if (startToken.i == 2)
      // Program end.
      answer.type = StxP.M02;
    else if (startToken.i == 3)
      {
        // Spindle start, clockwise. Requires S-token for RPM.
        Token rpm = getToken();
        if (rpm.letter != 'S')
          {
            answer.type = StxP.ERROR;
            answer.error = formError(answer.lineNumber,"bad spindle speed");
            readToEOLOrSemi();
            return;
          }
        answer.type = StxP.M03;
        answer.data = new DataInt(rpm.i);
      }
    else if (startToken.i == 4)
      {
        // Spindle start, CCW. Almost the same as above.
        Token rpm = getToken();
        if (rpm.letter != 'S')
          {
            answer.type = StxP.ERROR;
            answer.error = formError(answer.lineNumber,"bad spindle speed");
            readToEOLOrSemi();
            return;
          }
        answer.type = StxP.M04;
        answer.data = new DataInt(rpm.i);
      }
    else if (startToken.i == 5)
        // Spindle off
        answer.type = StxP.M05;
    else if (startToken.i == 6)
      {
        // Tool change. This should be followed by a T-token.
        Token tool = getToken();
        if (tool.letter != 'T')
          {
            answer.type = StxP.ERROR;
            answer.error = formError(answer.lineNumber,
                "bad tool change (M06 T?)");
            readToEOLOrSemi();
            return;
          }
        answer.type = StxP.M06;
        answer.data = new DataInt(tool.i);
      }
    else if ((startToken.i == 7) || (startToken.i == 8) || (startToken.i == 9))
      {
        // Coolant on/off. Pass it on even though it will be ignored.
        if (startToken.i == 7) answer.type = StxP.M07;
        if (startToken.i == 8) answer.type = StxP.M08;
        if (startToken.i == 9) answer.type = StxP.M09;
      }
    else if (startToken.i == 30)
      // End of program.
      answer.type = StxP.M30;
    else if ((startToken.i == 40) || (startToken.i == 41))
      {
        // Spindle high/low.
        if (startToken.i == 40) answer.type = StxP.M40;
        if (startToken.i == 41) answer.type = StxP.M41;
      }
    else if (startToken.i == 47)
      // Repeat program
      answer.type = StxP.M47;
    else if (startToken.i == 48)
      // Enable overrides.
      answer.type = StxP.M48;
    else if (startToken.i == 49)
      // Disable overrides.
      answer.type = StxP.M49;
    else if (startToken.i == 98)
      {
        // Call subprogram. The format here should be M98 P[###] L[###], where 
        // P gives the program number to call, and L is the number of times to 
        // call it. The M98 *must* be followed by a P.
        Token P = getToken();
        if (P.letter != 'P')
          {
            answer.type = StxP.ERROR;
            answer.error = formError(answer.lineNumber,
                "bad subroutine call (M98 P?)");
            readToEOLOrSemi();
            return;
          }
        
        DataSubroutineCall call = new DataSubroutineCall();
        answer.data = call;
        
        // Note: I used a double because I think that P can mean other things 
        // too on some machines.
        call.programNumber = (int) P.d;
        call.returnChar = P.endCount;
        
        // This might be followed by an L-value.
        Token L = peekToken();
        if (L.letter == 'L')
          {
            L = getToken();
            call.invocations = L.i;
            call.returnChar = L.endCount;
          }
        else
          {
            // If there is no L-value, then I assume a single invocation.
            // The returnIndex was already set to be just after the P-value.
            call.invocations = 1;
            call.returnChar = P.endCount;
          }
        
        answer.type = StxP.M98;
      } // end M98 (call subroutine)
    else if (startToken.i == 99)
      // Return from subprogram
      answer.type = StxP.M99;
    else
      {
        answer.type = StxP.ERROR;
        answer.error = formError(answer.lineNumber,
            "unknown M-code: M" +startToken.i);
        readToEOLOrSemi();
      }
  }

  private void parseNCode(StxP answer,Token startToken) {
    
    // Line number.
    // BUG: THis shouldn't be possible. They're tossed by readWhite().
    answer.type = StxP.LINE;
    answer.data = new DataInt(startToken.i);
  }
  
  private void parseOCode(StxP answer,Token startToken) {
    
    // Program number. When a sub-program is called, we will need to know
    // the character and line to which the program will be jumping to
    // execute that sub-program. This is the position immediately *after*
    // the O### statement -- the position of the start of the next statement
    // after the O### statement.
    answer.type = StxP.PROG;
    
    DataSubProg data = new DataSubProg();
    data.progNumber = startToken.i;
    answer.data = data;
  }
  
  private void parseXYZ(StxP answer,Token startToken) {
    
    // Have just read an X, Y or Z token (in startToken). Read to the end of the
    // move. The form of a general statement of this type is
    // [X#][Y#][Z#][F#]. The statement is considered complete as soon as either 
    // an F or anything other than X,Y,Z,F is read.
    answer.type = StxP.MOVE;
    boolean xDefined = false;
    boolean yDefined = false;
    boolean zDefined = false;
    boolean fDefined = false;
    double xValue = 0.0;
    double yValue = 0.0;
    double zValue = 0.0;
    double fValue = 0.0;
    
    // Read stuff until the statement runs out, starting with the startToken.
    if (startToken.letter == 'X')
      {
        xDefined = true;
        xValue = startToken.d;
      }
    else if (startToken.letter == 'Y')
      {
        yDefined = true;
        yValue = startToken.d;
      }
    else
      {
        zDefined = true;
        zValue = startToken.d;
      }

    // Continue for up to two more letters.
    Token peekToken = peekToken();
    while ((peekToken.letter == 'X') || (peekToken.letter == 'Y') || 
        (peekToken.letter == 'Z'))
      {
        // Go ahead and get it instead of just peeking.
        peekToken = getToken();
        if (peekToken.letter == 'X')
          {
            if (xDefined == true)
              {
                // Error. Already read this value.
                answer.type = StxP.ERROR;
                answer.error = formError(answer.lineNumber,"two x-values");
                readToEOLOrSemi();
                return;
              }
            xDefined = true;
            xValue = peekToken.d;
          }
        else if (peekToken.letter == 'Y')
          {
            if (yDefined == true)
              {
                // Error. Already read this value.
                answer.type = StxP.ERROR;
                answer.error = formError(answer.lineNumber,"two y-values");
                readToEOLOrSemi();
                return;
              }
            yDefined = true;
            yValue = peekToken.d;
          }
        else
          {
            if (zDefined == true)
              {
                // Error. Already read this value.
                answer.type = StxP.ERROR;
                answer.error = formError(answer.lineNumber,"two z-values");
                readToEOLOrSemi();
                return;
              }
            zDefined = true;
            zValue = peekToken.d;
          }
        
        // Peek to the next one.
        peekToken = peekToken();
      }
    
    // Have read any X,Y,Z values. See if there is an F-value.
    if (peekToken.letter == 'F')
      {
        // Yes, move past the F-token.
        peekToken = getToken();
        fDefined = true;
        fValue = peekToken.d;
      }
    
    // Statement has been read. Combine the data into a single object.
    answer.data = new DataMove(xDefined,yDefined,zDefined,fDefined,
                                xValue,yValue,zValue,fValue);
  }
  
  private StxP readStatement() {
    
    // Read and return one complete Statement.
    StxP answer = new StxP(StxP.UNKNOWN);
    
    // This also reads past any ';' or 'N' tokens.
    readWhite();
    
    // The next token should be something that opens a statement. Parse these
    // incoming Tokens to a statement.
    Token curToken = getToken();
    answer.lineNumber = curToken.lineNumber;
    answer.charNumber = curToken.characterCount;
    
    switch (curToken.letter)
      {
        case Token.ERROR  : answer.type = StxP.ERROR;
                            answer.error = curToken.error;
                            return answer;
        case Token.EOF    : answer.type = StxP.EOF;
                            return answer;
        
        // These are the cases where (we hope) things are going normally.
        case 'G'  : parseGCode(answer,curToken);
                    return answer;
        case 'M'  : parseMCode(answer,curToken);
                    return answer;
                    
        // BUG: This shouldn't be possible. 'N' is tossed by readWhite().
        case 'N'  : parseNCode(answer,curToken);
                    return answer;
        case 'O'  : parseOCode(answer,curToken);
                    return answer;
        case 'X'  :
        case 'Y'  :
        case 'Z'  : // Handle these moves together. When these appear "bare," 
                    // they're assumed to be relative to the previous G0 or G1.
                    parseXYZ(answer,curToken);
                    return answer;
        case '@'  : parseWizard(answer,curToken);
                    return answer;
                            
        // Any other letter that appears here is an error. These tokens
        // should not be "bare."
        default :
          answer.type = StxP.ERROR;
          answer.error = formError(answer.lineNumber,
              "unexpected command -- " +curToken.letter);
          readToEOLOrSemi();
          return answer;
      }
  }
  
 
  
  public static ArrayList<StxP> process(String gCode) {
    
    // Convert the entire G-code program to an array of statements.
    ArrayList<StxP> answer = new ArrayList<>();
    
    Parser p = new Parser(Lexer.process(gCode));
    
    StxP s = p.readStatement();
    
    while (s.type != StxP.EOF)
      {
        answer.add(s);
        s = p.readStatement();
      }
    
    return answer;
  }

  public static String digestAll(String gcode) {
    
    // Take the given g-code and feed it through, producing a single String
    // suitable for output to the user, or for use with unit tests.
    ArrayList<StxP> theStatements = process(gcode);

    StringBuffer answer = new StringBuffer();
    
    for (StxP s : theStatements)
      {
        answer.append(s.toString());
        answer.append("\n");
      }
    
    return answer.toString();
  }
  
}




