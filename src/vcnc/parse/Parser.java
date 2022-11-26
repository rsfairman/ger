    package vcnc.parse;

/*
12345678901234567890123456789012345678901234567890123456789012345678901234567890
This is the layer on top of the Lexer. It converts a series of tokens to
syntactically valid statements.
 
*/

import vcnc.lex.Lexer;
import vcnc.lex.Token;
import vcnc.transpile.TextBuffer;


public class Parser {

  // The underlying Lexer object.
  private Lexer theLexer = null;

  // These statements are to be fed upward to the next layer.
  private StatementBuffer statements = null;
  
  
  public Parser(TextBuffer theCode) {
    
    // Get the lexer ready.
    this.theLexer = new Lexer(theCode);
    
    // Fill the statement buffer to start off.
    this.statements = new StatementBuffer();
    statements.first = fillOneBuffer();
    statements.second = fillOneBuffer();
  }


  private String formError(int lineNumber,String msg) {
    return "Error on line " +lineNumber+ ": " + msg;
  }
  
  private void readWhite() {
    
    // Read any "junk" tokens. The following are ignored: extra EOL's,
    // and line numbers (N).
    Token peek = theLexer.peekToken();
    while ((peek.letter == Token.EOL) || (peek.letter == 'N'))
      {
        Token trash = theLexer.getToken();
        peek = theLexer.peekToken();
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
    Token peek = theLexer.peekToken();
    while (peek.letter != Token.EOL)
      {
        Token trash = theLexer.getToken();
        peek = theLexer.peekToken();
      }
  }
  
  private void parseWizard(Statement answer,Token startToken) {
    
    // Have just read a wizard command. Parse to the end of the line.
    answer.type = Statement.WIZARD;
    
    Wizard wiz = new Wizard();
    answer.data = wiz;
    
    wiz.cmd = startToken.wizard;
    
    // The arguments to the wizard consist of everything from the wizard
    // name to the EOL. This may or may not be the correct set of arguments,
    // but that will be determined later.
    
    Token t = theLexer.getToken();
    while (t.letter != Token.EOL)
      {
        if (t.letter == Token.STRING)
          wiz.args.add(t.wizard);
        else if (t.letter == Token.NUMBER)
          wiz.args.add(t.d);
        else
          {
            answer.type = Statement.ERROR;
            answer.error = formError(answer.lineNumber,
                "Unexpected argument to wizard");
            return;
          }
        
        t = theLexer.getToken();
      }
    
    return;
  }
  
  private void parseGCode(Statement answer,Token startToken) {
    
    // Have already read the G-something token. Read anything else needed to 
    // form a complete statement.
    
    if (startToken.i == 0)
      answer.type = Statement.G00;
    else if (startToken.i == 1)
      answer.type = Statement.G01;
    else if ((startToken.i == 2) || (startToken.i == 3))
      { 
        // Circular interpolation. Both cases are similar.
        if (startToken.i == 2) answer.type = Statement.G02;
        if (startToken.i == 3) answer.type = Statement.G03;
        
        // What follows must be some combination of the following:
        // X## Y## Z## I## J## K## R## F##
        // The entire command may or may not be terminated by F## and
        // the statement may use I/J/K or R to specify the radius, but not both.
        
        // So, the next token must be X, I or R.
        Circular data = new Circular();
        answer.data = data;
        Token next = theLexer.peekToken();
        
        // Get the X,Y (if any). If they are not defined, then this is a
        // complete circle (and there must be an I and/or J).
        if (next.letter == 'X')
          {
            // Read X and Y.
            next = theLexer.getToken();
            data.xDefined = true;
            data.X = next.d;
            next = theLexer.peekToken();
            
            // There may or may not be a Y and Z.
            if (next.letter == 'Y')
              {
                next = theLexer.getToken();
                data.yDefined = true;
                data.Y = next.d;
                next = theLexer.peekToken();
              }
            if (next.letter == 'Z')
              {
                next = theLexer.getToken();
                data.zDefined = true;
                data.Z = next.d;
                next = theLexer.peekToken();
              }
          }
        else if (next.letter == 'Y')
          {
            // There was no X, but there is a Y and there might be a Z.
            next = theLexer.getToken();
            data.yDefined = true;
            data.Y = next.d;
            next = theLexer.peekToken();

            if (next.letter == 'Z')
              {
                next = theLexer.getToken();
                data.zDefined = true;
                data.Z = next.d;
                next = theLexer.peekToken();
              }
          }
        else if (next.letter == 'Z')
          {
            // There was no X or Y, but there is a Z.
            next = theLexer.getToken();
            data.zDefined = true;
            data.Z = next.d;
            next = theLexer.peekToken();
          }
        
        // Have read X,Y,Z, if any. Next will be I/J/K or R.
        if (next.letter == 'I')
          {
            // Read I,J,K.
            next = theLexer.getToken();
            data.iDefined = true;
            data.I = next.d;
            next = theLexer.peekToken();
            
            // See if there is a J,K.
            if (next.letter == 'J')
              {
                next = theLexer.getToken();
                data.jDefined = true;
                data.J = next.d;
                next = theLexer.peekToken();
              }
            if (next.letter == 'K')
              {
                next = theLexer.getToken();
                data.kDefined = true;
                data.K = next.d;
                next = theLexer.peekToken();
              }
          }
        else if (next.letter == 'J')
          {
            // No I, but there is a J and maybe a K.
            next = theLexer.getToken();
            data.jDefined = true;
            data.J = next.d;
            next = theLexer.peekToken();
            
            if (next.letter == 'K')
              {
                next = theLexer.getToken();
                data.kDefined = true;
                data.K = next.d;
                next = theLexer.peekToken();
              }
          }
        else if (next.letter == 'K')
          {
            // No I or J, but we have K.
            next = theLexer.getToken();
            data.kDefined = true;
            data.K = next.d;
            next = theLexer.peekToken();
          }
        else if (next.letter == 'R')
          {
            // No I,J,K; R instead.
            next = theLexer.getToken();
            data.rDefined = true;
            data.R = next.d;
            next = theLexer.peekToken();
          }
        else
          {
            // Something's not right. G02/G03 is missing both I,J,K and R.
            answer.type = Statement.ERROR;
            answer.error = formError(answer.lineNumber,
                "G02/03 improperly specified");
            readToEOLOrSemi();
            return;
          }
        
        // The entire thing might be followed by a feed rate.
        next = theLexer.peekToken();
        if (next.letter == 'F')
          {
            next = theLexer.getToken();
            data.F = next.d;
          }
        
        // Make sure that this makes sense. If the user does not give X or Y,
        // and he tries to used R instead of I/J, that doesn't make sense.
        // Other things that don't make sense are giving R along with I and/or J,
        // though the way this is written, the latter would be caught.
        if ((data.xDefined == false) && (data.yDefined == false) 
            && (data.zDefined == false) && (data.rDefined == true))
            {
              answer.type = Statement.ERROR;
              answer.error = formError(startToken.lineNumber,
                  "radius given, but no end-point");
              readToEOLOrSemi();
            }
      }
    else if (startToken.i == 15)
      // Polar coordinates off.
      answer.type = Statement.G15;
    else if (startToken.i == 16)
      // Polar coordinates on.
      answer.type = Statement.G16;
    else if (startToken.i == 17)
      // Choose coordinate axes.
      answer.type = Statement.G17;
    else if (startToken.i == 18)
      // Choose coordinate axes.
      answer.type = Statement.G18;
    else if (startToken.i == 19)
      // Choose coordinate axes.
      answer.type = Statement.G19;
    else if (startToken.i == 20)
      answer.type = Statement.G20;
    else if (startToken.i == 21)
      answer.type = Statement.G21;
    else if (startToken.i == 28)
      // Return to home. Ignore.
      // BUG: Do NOT IGNORE THIS.
      answer.type = Statement.PASS;
    else if (startToken.i == 40)
      // Cancel cutter comp.
      answer.type = Statement.G40;
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
        RegisterState data = new RegisterState();
        if (startToken.i == 41) answer.type = Statement.G41;
        if (startToken.i == 42) answer.type = Statement.G42;
        
        // Next token should be D or H.
        Token next = theLexer.getToken();
        if (next.letter == 'D')
          data.D = true;
        else if (next.letter == 'H')
          data.D = false;
        else
          {
            answer.type = Statement.ERROR;
            answer.error = formError(answer.lineNumber,
                "G41/42 not followed by H or D");
            readToEOLOrSemi();
            return;
          }
        data.regValue = next.i;
        answer.data = data;
      }
    else if ((startToken.i == 43) || (startToken.i == 44))
      {
        // Apply TLO. Similar to cutter comp.
        // Takes the form
        // G43 [Zx.xx] Hn
        if (startToken.i == 43) answer.type = Statement.G43;
        if (startToken.i == 44) answer.type = Statement.G44;
        TLOState data = new TLOState();
        
        // See if there's an optional Z value.
        Token next = theLexer.getToken();
        if (next.letter == 'Z')
          {
            // Read the value.
            data.hasZ = true;
            data.zValue = next.d;
            next = theLexer.getToken();
          }
          
        // Now there must be an H value
        if (next.letter != 'H')
          {
            answer.type = Statement.ERROR;
            answer.error = formError(answer.lineNumber,
                "G43/G44 not followed by H register");
            readToEOLOrSemi();
            return;
          }
        
        data.hRegister = next.i;
        answer.data = data;
      }
    else if (startToken.i == 49)
      // Cancel TLO.  
      answer.type = Statement.G49;
    else if (startToken.i == 52)
      {
        // Temporary change in PRZ (or return to original). This must
        // be followed by some X Y and Z values. This uses a MoveState
        // object for these coordinates, even though the F-value is unused.
        // It would usually (I guess) be an error not to have X and Y, but I
        // assume that if any of these is omitted, it must not be changed.
        answer.type = Statement.G52;
        
        boolean xDefined = false;
        boolean yDefined = false;
        boolean zDefined = false;
        double xValue = 0.0;
        double yValue = 0.0;
        double zValue = 0.0;
        
        Token nextToken = theLexer.getToken();
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
        Token peekToken = theLexer.peekToken();
        while ((peekToken.letter == 'X') || (peekToken.letter == 'Y') || 
            (peekToken.letter == 'Z'))
          {
            // Go ahead and get it instead of just peeking.
            peekToken = theLexer.getToken();
            if (peekToken.letter == 'X')
              {
                if (xDefined == true)
                  {
                    // Error. Already read this value.
                    answer.type = Statement.ERROR;
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
                    answer.type = Statement.ERROR;
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
                    answer.type = Statement.ERROR;
                    answer.error = formError(answer.lineNumber,"two z-values");
                    readToEOLOrSemi();
                    return;
                  }
                zDefined = true;
                zValue = peekToken.d;
              }
            
            // Peek to the next one.
            peekToken = theLexer.peekToken();
          }
        
        // Statement has been read. Combine the data into a single object.
        answer.data = new MoveState(xDefined,yDefined,zDefined,false,
                                    xValue,yValue,zValue,0.0);
      }
    else if ((startToken.i >= 54) && (startToken.i <= 59))
      {
        // Work offsets.
        if (startToken.i == 54) answer.type = Statement.G54;
        if (startToken.i == 55) answer.type = Statement.G55;
        if (startToken.i == 56) answer.type = Statement.G56;
        if (startToken.i == 57) answer.type = Statement.G57;
        if (startToken.i == 58) answer.type = Statement.G58;
        if (startToken.i == 59) answer.type = Statement.G59;
      }
    else if ((startToken.i >= 80) && (startToken.i <= 89))
      // Various canned cycles, which I ignore.
      // BUG: Don't ignore. Pass through.
      answer.type = Statement.PASS;
    else if (startToken.i == 90)
      // Absolute mode.
      answer.type = Statement.G90;
    else if (startToken.i == 91)
      // Incremental mode.
      answer.type = Statement.G91;
    else
      {
        // Catch any G-code that is not handled above and flag it as an error.
        answer.type = Statement.ERROR;
        answer.error = formError(answer.lineNumber,
            "unknown G-code: " +startToken.i);
        readToEOLOrSemi();
      }
  }

  private void parseMCode(Statement answer,Token startToken) {
    
    // Have already read the M-something token. Read anything else needed to 
    // form a complete statement.
    if (startToken.i == 0)
      // Program Stop. I think this is a hard stop.
      answer.type = Statement.M00;
    else if (startToken.i == 1)
      // Program stop. I think this is an optional stop.
      answer.type = Statement.M01;
    else if (startToken.i == 2)
      // Program end.
      answer.type = Statement.M02;
    else if (startToken.i == 3)
      {
        // Spindle start, clockwise. Requires S-token for RPM.
        Token rpm = theLexer.getToken();
        if (rpm.letter != 'S')
          {
            answer.type = Statement.ERROR;
            answer.error = formError(answer.lineNumber,"bad spindle speed");
            readToEOLOrSemi();
            return;
          }
        answer.type = Statement.M03;
        answer.data = new IntState(rpm.i);
      }
    else if (startToken.i == 4)
      {
        // Spindle start, CCW. Almost the same as above.
        Token rpm = theLexer.getToken();
        if (rpm.letter != 'S')
          {
            answer.type = Statement.ERROR;
            answer.error = formError(answer.lineNumber,"bad spindle speed");
            readToEOLOrSemi();
            return;
          }
        answer.type = Statement.M04;
        answer.data = new IntState(rpm.i);
      }
    else if (startToken.i == 5)
        // Spindle off
        answer.type = Statement.M05;
    else if (startToken.i == 6)
      {
        // Tool change. This should be followed by a T-token.
        Token tool = theLexer.getToken();
        if (tool.letter != 'T')
          {
            answer.type = Statement.ERROR;
            answer.error = formError(answer.lineNumber,
                "bad tool change (M06 T?)");
            readToEOLOrSemi();
            return;
          }
        answer.type = Statement.M06;
        answer.data = new IntState(tool.i);
      }
    else if ((startToken.i == 7) || (startToken.i == 8) || (startToken.i == 9))
      {
        // Coolant on/off. Pass it on even though it will be ignored.
        if (startToken.i == 7) answer.type = Statement.M07;
        if (startToken.i == 8) answer.type = Statement.M08;
        if (startToken.i == 9) answer.type = Statement.M09;
      }
    else if (startToken.i == 30)
      // End of program.
      answer.type = Statement.M30;
    else if ((startToken.i == 40) || (startToken.i == 41))
      {
        // Spindle high/low. Pass it on even though it will be ignored.
        if (startToken.i == 40) answer.type = Statement.M40;
        if (startToken.i == 41) answer.type = Statement.M41;
      }
    else if (startToken.i == 47)
      // Repeat program
      answer.type = Statement.M47;
    else if (startToken.i == 48)
      // Enable overrides.
      answer.type = Statement.M48;
    else if (startToken.i == 49)
      // Disable overrides.
      answer.type = Statement.M49;
    else if (startToken.i == 98)
      {
        // Call subprogram. The format here should be M98 P[###] L[###], where 
        // P gives the program number to call, and L is the number of times to 
        // call it. The M98 *must* be followed by a P.
        Token P = theLexer.getToken();
        if (P.letter != 'P')
          {
            answer.type = Statement.ERROR;
            answer.error = formError(answer.lineNumber,
                "bad subroutine call (M98 P?)");
            readToEOLOrSemi();
            return;
          }
        
        SubroutineCall call = new SubroutineCall();
        answer.data = call;
        
        // Note: I used a double because I think that P can mean other things 
        // too.
        call.programNumber = (int) P.d;
        call.returnIndex = P.endCount;
        
        // This might be followed by an L-value.
        Token L = theLexer.peekToken();
        if (L.letter == 'L')
          {
            L = theLexer.getToken();
            call.invocations = L.i;
            call.returnIndex = L.endCount;
          }
        else
          {
            // If there is no L-value, then I assume a single invocation.
            // The returnIndex was already set to be just after the P-value.
            call.invocations = 1;
            call.returnIndex = P.endCount;
          }
        
        answer.type = Statement.M98;
      }
    else if (startToken.i == 99)
      // Return from subprogram
      answer.type = Statement.M99;
    else
      {
        answer.type = Statement.ERROR;
        answer.error = formError(answer.lineNumber,
            "unknown M-code: M" +startToken.i);
        readToEOLOrSemi();
      }
  }

  private void parseNCode(Statement answer,Token startToken) {
    
    // Line number.
    // BUG: THis shouldn't be possible. They're tossed by readWhite().
    answer.type = Statement.LINE;
    answer.data = new IntState(startToken.i);
  }
  
  private void parseOCode(Statement answer,Token startToken) {
    
    // Program number. When a sub-program is called, we will need to know
    // the character and line to which the program will be jumping to
    // execute that sub-program. This is the position immediately *after*
    // the O### statement -- the position of the start of the next statement
    // after the O### statement.
    answer.type = Statement.PROG;
    
    SubProgState data = new SubProgState();
    data.progNumber = startToken.i;
    answer.data = data;
    
    // Peek ahead to the token that follows this one. That's where the
    // body of the sub-program starts.
    Token nextToken = theLexer.peekToken();
    data.characterNumber = nextToken.characterCount;
    data.lineNumber = nextToken.lineNumber;
  }
  
  private void parseXYZ(Statement answer,Token startToken) {
    
    // Have just read an X, Y or Z token (in startToken). Read to the end of the
    // move. The form of a general statement of this type is
    // [X#][Y#][Z#][F#]. The statement is considered complete as soon as either 
    // an F or anything other than X,Y,Z,F is read.
    answer.type = Statement.MOVE;
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
    Token peekToken = theLexer.peekToken();
    while ((peekToken.letter == 'X') || (peekToken.letter == 'Y') || 
        (peekToken.letter == 'Z'))
      {
        // Go ahead and get it instead of just peeking.
        peekToken = theLexer.getToken();
        if (peekToken.letter == 'X')
          {
            if (xDefined == true)
              {
                // Error. Already read this value.
                answer.type = Statement.ERROR;
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
                answer.type = Statement.ERROR;
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
                answer.type = Statement.ERROR;
                answer.error = formError(answer.lineNumber,"two z-values");
                readToEOLOrSemi();
                return;
              }
            zDefined = true;
            zValue = peekToken.d;
          }
        
        // Peek to the next one.
        peekToken = theLexer.peekToken();
      }
    
    // Have read any X,Y,Z values. See if there is an F-value.
    if (peekToken.letter == 'F')
      {
        // Yes, move past the F-token.
        peekToken = theLexer.getToken();
        fDefined = true;
        fValue = peekToken.d;
      }
    
    // Statement has been read. Combine the data into a single object.
    answer.data = new MoveState(xDefined,yDefined,zDefined,fDefined,
                                xValue,yValue,zValue,fValue);
  }
  
  private Statement readStatement() {
    
    // Read and return one complete Statement.
    Statement answer = new Statement();
    
    // This also reads past any ';' or 'N' tokens.
    readWhite();
    
    // The next token should be something that opens a statement.
    // Even though the lexer would accept certain "bare" items, like
    // "I13.0", they are not valid openings for a statement.
    
    Token curToken = theLexer.getToken();
    answer.lineNumber = curToken.lineNumber;
    answer.charNumber = curToken.characterCount;
    
//    System.out.println("parser reading line " +answer.lineNumber);
    
    // BUG: Really: I could collapse a lot of these cases. There are only 
    // a few that are *not* unexpected bare tokens.
    
    switch (curToken.letter)
      {
        case Token.ERROR  : answer.type = Statement.ERROR;
                            answer.error = curToken.error;
                            return answer;
        case Token.EOF    : answer.type = Statement.EOF;
                            return answer;
        case 'D'          : // Should not appear as a bare token.
                            answer.type = Statement.ERROR;
                            answer.error = formError(answer.lineNumber,
                                "unexpected D-value");
                            readToEOLOrSemi();
                            return answer;
        case 'F'          : // Should not appear as a bare token.
                            answer.type = Statement.ERROR;
                            answer.error = formError(answer.lineNumber,
                                "unexpected F-value");
                            readToEOLOrSemi();
                            return answer;
        case 'G'          : parseGCode(answer,curToken);
                            return answer;
        case 'H'          : // Should not appear as a bare token.
                            answer.type = Statement.ERROR;
                            answer.error = formError(answer.lineNumber,
                                "unexpected H-value");
                            readToEOLOrSemi();
                            return answer;
        case 'I'          :
        case 'J'          :
        case 'K'          : // Should not appear as bare tokens.
                            answer.type = Statement.ERROR;
                            answer.error = formError(answer.lineNumber,
                                "unexpected I/J/K-value");
                            readToEOLOrSemi();
                            return answer;
        case 'L'          : // Should not appear as a bare token.
                            answer.type = Statement.ERROR;
                            answer.error = formError(answer.lineNumber,
                                "unexpected L-value");
                            readToEOLOrSemi();
                            return answer;
        case 'M'          : parseMCode(answer,curToken);
                            return answer;

        // BUG: THis shouldn't be possible. 'N' is tossed by readWhite().
        case 'N'          : parseNCode(answer,curToken);
                            return answer;
                            
        case 'O'          : parseOCode(answer,curToken);
                            return answer;
        case 'P'          : // Should not appear as a bare token.
                            answer.type = Statement.ERROR;
                            answer.error = formError(answer.lineNumber,
                                "unexpected P-value");
                            readToEOLOrSemi();
                            return answer;
        case 'Q'          : // I'm not sure what this could be. Die on it.
                            answer.type = Statement.ERROR;
                            answer.error = formError(answer.lineNumber,
                                "unexpected Q-value");
                            readToEOLOrSemi();
                            return answer;
        case 'R'          : // Should not appear as a bare token.
                            answer.type = Statement.ERROR;
                            answer.error = formError(answer.lineNumber,
                                "unexpected R-value");
                            readToEOLOrSemi();
                            return answer;                            
        case 'S'          : // Neither of these should appear here as a bare token.
                            answer.type = Statement.ERROR;
                            answer.error = formError(answer.lineNumber,
                                    "unexpected S-value");
                            readToEOLOrSemi();
                            return answer;
        case 'T'          : // Should not appear as a bare token.
                            answer.type = Statement.ERROR;
                            answer.error = formError(answer.lineNumber,
                                "unexpected T-value");
                            readToEOLOrSemi();
                            return answer;
        case 'U'          : // Not sure what this is. Die on it.
                            answer.type = Statement.ERROR;
                            answer.error = formError(answer.lineNumber,
                                "unexpected U-value");
                            readToEOLOrSemi();
                            return answer;
        case 'X'          :
        case 'Y'          :
        case 'Z'          : // Handle these moves together.
                            // When these appear "bare," they're assumed to be
                            // relative to the previous G0 or G1.
                            parseXYZ(answer,curToken);
                            return answer;
        case '@'          : parseWizard(answer,curToken);
                            return answer;
        default :
          answer.type = Statement.ERROR;
          answer.error = formError(answer.lineNumber,
              "unexpected command -- " +curToken.letter);
          readToEOLOrSemi();
          return answer;
      }
  }
  
  private Statement[] fillOneBuffer() {
    
    // Fill an array with StatementBuffer.BufferSize tokens, by eating tokens
    // from the lexer.
    Statement[] answer = new Statement[StatementBuffer.BufferSize];
    
    for (int i = 0; i < answer.length; i++)
      answer[i] = readStatement();
    
    return answer;
  }

  public Statement getStatement() {
    
    // Return a Statement from the buffer. If this is the last Statement in 
    // statements.first, then refill the buffer.
    
    // BUG: (Maybe) In the C++ version, I was using three buffers and
    // I'm not sure why. I was also a bit more clever about peeking, taking
    // into account the possibility that I need to refresh a buffer there too.
    
    Statement answer = statements.first[statements.index];
    ++statements.index;
    
    if (statements.index >= StatementBuffer.BufferSize)
      {
        // Refresh.
        statements.first = statements.second;
        statements.second = fillOneBuffer();
        statements.index = 0;
      }
    
    return answer;
  }
  
  public Statement peekStatement() {
    
    return statements.first[statements.index];
  }
  

  public void reset() {
    
    // Restart from the very beginning of the code.
    theLexer.reset();
    statements.first = fillOneBuffer();
    statements.second = fillOneBuffer();
    statements.index = 0;
  }

  public void moveTo(int n,int lineNumber) {
    
    // Restart the parsing process at the n-th character, which is assumed
    // to be on the given line number.
    theLexer.moveTo(n,lineNumber);
    
    // Restart the buffers.
    statements.first = fillOneBuffer();
    statements.second = fillOneBuffer();
    statements.index = 0;
  }
  
}


/*

const Token* Parser::getToken() throw(const Token*)
{
  // Glue. It goes to theLexer and throws if there's an error in the
  // returned token. Whether the answer is thrown or simply returned,
  // it's the same answer.
  const Token* answer = theLexer.getToken();
  if (answer->letter == TOKEN::LexError)
    throw (answer);
  
  return answer;
}

//void Parser::parseIJK(Statement *answer,const Token *startToken)
//{
//  // Very similar to parseXYZ(). Have just read an I, J or K token (in startToken).
//  // Read to the end of the move. The form of a general statment of this type is
//  // [I#][J#][K#][F#]. The statement is considered complete as soon as either an
//  // F or anything other than I,J,K,F is read.
//  
//  // BUG: I don't think that this should ever be called.
//  //qDebug() << "WHAT THE HECK?? Parser.parseIJK() called";
//  
//  
//   
//
//Original Java code that I never bothered porting.
//   
//   
//  answer->type = Statement.IJK;
//  
//  // NOTE: I just left these variable names is x,y,z instead of changing them
//  // to i,j,k.
//  boolean xDefined = false;
//  boolean yDefined = false;
//  boolean zDefined = false;
//  boolean fDefined = false;
//  double xValue = 0.0;
//  double yValue = 0.0;
//  double zValue = 0.0;
//  double fValue = 0.0;
//  
//  // Read stuff until the statment runs out, starting with the startToken.
//  if (startToken.letter == 'I')
//    {
//      xDefined = true;
//      xValue = startToken.d;
//    }
//  else if (startToken.letter == 'J')
//    {
//      yDefined = true;
//      yValue = startToken.d;
//    }
//  else
//    {
//      zDefined = true;
//      zValue = startToken.d;
//    }
//
//  // Continue for up to two more letters.
//  Token peekToken = theLexer.peekToken();
//  while ((peekToken->letter == 'I') || (peekToken->letter == 'J') || 
//      (peekToken->letter == 'K'))
//    {
//      // Go ahead and get it instead of just peeking.
//      peekToken = theLexer.getToken();
//      if (peekToken->letter == 'I')
//        {
//          if (xDefined == true)
//            {
//              // Error. Already read this value.
//              answer->type = Statement::SError;
//              answer->errMsg = formError(answer->lineNumber,"two I-values");
//              return;
//            }
//          xDefined = true;
//          xValue = peekToken->d;
//        }
//      else if (peekToken->letter == 'J')
//        {
//          if (yDefined == true)
//            {
//              // Error. Already read this value.
//              answer->type = Statement::SError;
//              answer->errMsg = formError(answer->lineNumber,"two J-values");
//              return;
//            }
//          yDefined = true;
//          yValue = peekToken->d;
//        }
//      else
//        {
//          if (zDefined == true)
//            {
//              // Error. Already read this value.
//              answer->type = Statement::SError;
//              answer->errMsg = formError(answer->lineNumber,"two K-values");
//              return;
//            }
//          zDefined = true;
//          zValue = peekToken->d;
//        }
//      
//      // Peek to the next one.
//      peekToken = theLexer.peekToken();
//    }
//  
//  // Have read any I,J,K values. See if there is an F-value.
//  if (peekToken->letter == 'F')
//    {
//      // Yes, move past the F-token.
//      peekToken = theLexer.getToken();
//      fDefined = true;
//      fValue = peekToken->d;
//    }
//  
//  // Statement has been read. Combine the data into a single object.
//  answer->data = new MoveState(xDefined,yDefined,zDefined,fDefined,
//                              xValue,yValue,zValue,fValue);
//                              
//}


void Parser::parseSCode(Statement *answer,const Token *startToken)
{
  // Only two things are possible: S### M03 and S### M04.
  // This is similar to what was done in parseMCode(), but the terms
  // are reversed.
  // So, startToken is the S### token. Check the next token.
  // I need this in addition to the parseMCode() because the S and M
  // terms can be in either order. Smid even says that the two terms
  // don't even need to be on the same line, and can be seperated by
  // other statements. I'm not going to go that far.
  const Token *nextToken = NULL;
  try {
    nextToken = getToken();
    if ((nextToken->letter != 'M') || 
        ((nextToken->i != 3) && (nextToken->i != 4)))
      {
        // S### is not followed by either M03 or M04.
        answer->type = Statement::SError;
        answer->errMsg = formError(answer->lineNumber,"spindle speed must be followed by M03 or M04");
        readToEOLOrSemi();
        return;
      }
  } catch (const Token* badToken) {
    answer->type = Statement::SError;
    answer->errMsg = badToken->errMsg;
    readToEOLOrSemi();
    return;
  }
  
  // Got here, so all is well.
  if (nextToken->i == 3)
    answer->type = Statement::M03;
  else
    answer->type = Statement::M04;
  
  answer->data = new IntState(startToken->i);
}

void Parser::parseXYZ(Statement *answer,const Token *startToken)
{
  // Have just read an X, Y or Z token (in startToken). Read to the end of the 
  // move. The form of a general statment of this type is
  // [X#][Y#][Z#][F#]. The statement is considered complete as soon as either an
  // F or anything other than X,Y,Z,F is peeked.
  answer->type = Statement::MOVE;
  bool xDefined = false;
  bool yDefined = false;
  bool zDefined = false;
  bool fDefined = false;
  double xValue = 0.0;
  double yValue = 0.0;
  double zValue = 0.0;
  double fValue = 0.0;
  
  // Read stuff until the statment runs out, starting with the startToken.
  if (startToken->letter == 'X')
    {
      xDefined = true;
      xValue = startToken->d;
    }
  else if (startToken->letter == 'Y')
    {
      yDefined = true;
      yValue = startToken->d;
    }
  else
    {
      zDefined = true;
      zValue = startToken->d;
    }

  // Continue for up to two more letters.
  try {
    const Token *peekToken = theLexer.peekToken();
    while ((peekToken->letter == 'X') || (peekToken->letter == 'Y') || 
           (peekToken->letter == 'Z'))
      {
        // Go ahead and get it instead of just peeking.
        peekToken = getToken();
        if (peekToken->letter == 'X')
          {
            if (xDefined == true)
              {
                // Error. Already read this value.
                answer->type = Statement::SError;
                answer->errMsg = formError(answer->lineNumber,"two x-values");
                readToEOLOrSemi();
                return;
              }
            xDefined = true;
            xValue = peekToken->d;
          }
        else if (peekToken->letter == 'Y')
          {
            if (yDefined == true)
              {
                // Error. Already read this value.
                answer->type = Statement::SError;
                answer->errMsg = formError(answer->lineNumber,"two y-values");
                readToEOLOrSemi();
                return;
              }
            yDefined = true;
            yValue = peekToken->d;
          }
        else
          {
            if (zDefined == true)
              {
                // Error. Already read this value.
                answer->type = Statement::SError;
                answer->errMsg = formError(answer->lineNumber,"two z-values");
                readToEOLOrSemi();
                return;
              }
            zDefined = true;
            zValue = peekToken->d;
          }
        
        // Peek to the next one.
        peekToken = theLexer.peekToken();
      }
    
    // Have read any X,Y,Z values. See if there is an F-value.
    if (peekToken->letter == 'F')
      {
        // Yes, move past the F-token.
        peekToken = getToken();
        fDefined = true;
        fValue = peekToken->d;
      }
    
    // Statement has been read. Combine the data into a single object.
    answer->data = new MoveState(xDefined,yDefined,zDefined,fDefined,
                                 xValue,yValue,zValue,fValue);
  } catch (const Token* badToken) {
    answer->type = Statement::SError;
    answer->errMsg = badToken->errMsg;
    readToEOLOrSemi();
  }
}

void Parser::reset()
{
  // Restart from the very beginning of theCode.
  theLexer.reset();
  fillOneBuffer(this->statements.bufA);
  fillOneBuffer(this->statements.bufB);
  
  this->statements.curBuf = this->statements.bufA;
  this->statements.index = 0;
}

void Parser::moveTo(int n,int lineNumber) 
{
  
  // Restart the parsing process at the n-th character, which is assumed
  // to be on the given line number.
  theLexer.moveTo(n,lineNumber);
  
  // Restart the buffers.
  fillOneBuffer(this->statements.bufA);
  fillOneBuffer(this->statements.bufB);
  
  this->statements.curBuf = this->statements.bufA;
  this->statements.index = 0;
}

const Statement *Parser::getStatement()
{
  // Return the next Statement in the buffer. As in the Lexer, we must
  // first see if we've run off the end of the buffer.
  //
  // This is a bit tricker than what I did in the lexer since there
  // are three sub-buffers. When we come to the end of bufA, we need to
  // move to the start of bufB, and fill bufC. We do not refill bufA
  // because a higher layer (Layer05, in fact) might be holding a past
  // statement in memory. When we come to the end of bufB, move to the
  // start of bufC, and refill bufA, leaving bufB alone. When we come
  // to the end of bufC, move to the start of bufA, and refill bufB,
  // leaving bufC alone.
  if (statements.index >= statements.BufferSize)
    {
      if (statements.curBuf == statements.bufA)
        {
          statements.curBuf = statements.bufB;
          fillOneBuffer(statements.bufC);
        }
      else if (statements.curBuf == statements.bufB)
        {
          statements.curBuf = statements.bufC;
          fillOneBuffer(statements.bufA);
        }
      else
        {
          // Reached the end of bufC;
          statements.curBuf = statements.bufA;
          fillOneBuffer(statements.bufB);
        }
      statements.index = 0;
    }
  
  Statement *answer = &(statements.curBuf[statements.index]);
  ++statements.index;
  
  return answer;
}

const Statement *Parser::peekStatement()
{
  // Like the Lexer, we have to be tricky in case the next
  // getStatment() goes to a new buffer.
  if (statements.index >= statements.BufferSize)
    {
      if (statements.curBuf == statements.bufA)
        return &(statements.bufB[0]);
      else
        return &(statements.bufA[0]);
    }
  else
    return &(statements.curBuf[statements.index]);
}




*/