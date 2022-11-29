package vcnc.tpile;

/*

The first layer above the Parser and Lexer. It does nothing but filter out
calls to subroutines. G-SOMETHING OR OTHER. The has the effect of making
the program longer -- maybe *much* longer. Because G-code has no concept
of variables or loops, this is conceptually a matter of inserting some
known number of copies of the subroutines.

*/

import java.util.Stack;

import vcnc.tpile.parse.Parser;
import vcnc.tpile.parse.Statement;
import vcnc.tpile.parse.SubProgState;
import vcnc.tpile.parse.SubroutineCall;


public class Layer00 {
  
  private Parser theParser = null;

  // A lookup table showing where all subprograms begin (where the O### appears).
  private ProgramDict subprogs = null;
  
  // See handleCall() for a discussion of this. It's the call stack.
  private Stack<int[]> theStack = new Stack<int[]>();;
  
  // The maximum level of recursion. In theory, there is no reason to limit
  // this, but at some point, it is almost certainly an error on the 
  // user's part.
  // BUG: Allow the user to control this?
  private int MaxStackDepth = 500;
  
  public Layer00(TextBuffer theText) throws Exception {

    this.theParser = new Parser(theText);
    
    // Create the table of subprograms. This parses through the entire program
    // once. It's just like actually interpreting the program, except that 
    // the only thing we care about are appearances of the O### command.
    //
    // There's no obvious way to avoid throwing an exception here.
    this.subprogs = new ProgramDict();
    Statement cmd = theParser.getStatement();
    
    while ((cmd.type != Statement.ERROR) && (cmd.type != Statement.EOF))
      {
        // This ignores the context and assumes that any time that an 
        // O-statement appears, it gives a valid program number.
        if (cmd.type == Statement.PROG)
          {
            // Found a program statement.
            SubProgState subState = (SubProgState) cmd.data;
            
            // We need to note the place (line and character count) at which
            // the sub-program starts. This is the line and character *after*
            // the declaration of the O-value. This is the place we jump to
            // when a subroutine is called. These were noted by the Parser.
            int[] value = new int[2];
            value[0] = subState.characterNumber;
            value[1] = cmd.lineNumber;
            
            // Make sure that this program number is not already in use.
            if (subprogs.dict.containsKey(subState.progNumber) == true)
              throw new Exception(formError(cmd.lineNumber,"Duplicate program number"));
            
            this.subprogs.dict.put(subState.progNumber,value);
          }
        
        cmd = theParser.getStatement();
      }
  
    // Reset the parser to start over at the first character.
    // We are now doing the actual transpilation.
    theParser.reset();
    
    // The very first thing read should be a program number.
    
    // BUG: If I add things like settings to the abstract machine or the
    // specification of the billet, then these could occur before the
    // program number. Is that going to be a problem?
    readProgramNumber(); 
  }

  private String formError(int lineNumber,String msg) {
    
    return "Error on line " +lineNumber+ ": " +msg;
  }

  private void readProgramNumber() {
    
    // Read and ignore the program number. This should be the first thing in
    // the program (other than comments and white space). If it's there, then
    // it's read and trashed. If it's not here, then the non-O-value
    // statement is put back to be read later.
    //
    // There is one weird case: if the program starts with a comment and
    // that comment contains a '('. So, don't flag the lack of an O-value
    // as an error if the first statement is already an error.
    Statement cmd = theParser.peekStatement();
    if ((cmd.type != Statement.PROG) && (cmd.type != Statement.ERROR))
      {
        cmd.warning = true;
        cmd.error = "program does not start with a program number";
      }
    else if (cmd.type == Statement.ERROR)
      // Leave the statement where it is so that it is seen.
      return;
    else
      // It was the program number. Pull it out.
      cmd = theParser.getStatement();
  }


  private void handleCall(Statement cmd) {
    
    // Subroutines calls are done with a simple stack. The entries of the stack
    // are the character count in the input code to which the lexer should
    // return when the program returns from the subroutine. Because a 
    // subroutine can be called multiple times by the same M98, we push the
    // same return value on the stack for as many times as the subroutine is
    // called (less 1), then the final return point. It's tempting to try to 
    // use a simple integer count of the number of M98 invocations, but 
    // sub-programs can call other sub-programs, so this wouldn't eliminate 
    // the need for a stack.
    
    // Look up the location of the subroutine. We get the character number to
    // jump to, along with the line on which it occurs.
    SubroutineCall call = (SubroutineCall) cmd.data;
    
    // Doing a map lookup.
    int[] subProgLookup = subprogs.dict.get(call.programNumber);
    
    if (subProgLookup == null)
      {
        // Attempt to call a sub-program that was never defined.
        // That is, the P-value in the M98 refers to an O-value that
        // was never defined. Convert cmd to an Error.
        cmd.type = Statement.ERROR;
        cmd.error = formError(cmd.lineNumber,
            "no such sub-program was defined");
        return;
      }
    
    // Push the current return point onto the stack.
    // This is the line number to which we return followed by the character index.
    // We need to peek forward because we return to the statement *after*
    // this one (after the M98).
    Statement peekCmd = theParser.peekStatement();
    
    int[] retPoint = new int[2];
    retPoint[0] = peekCmd.charNumber;
    retPoint[1] = peekCmd.lineNumber;
    theStack.push(retPoint);
    
    // Push the same subroutine (the one we are calling) onto the stack as
    // many times as the L-value indicates (minus one since we're about to
    // jump there). This is character count, then line count.
    int count = 1;
    while (count < call.invocations)
      {
        // Each stack entry needs a separate pair.
        retPoint = new int[2];
        retPoint[0] = subProgLookup[0];
        retPoint[1] = subProgLookup[1];
        
        theStack.push(retPoint);
        ++count;
      }
    
    // Finally, jump to the right place.
    theParser.moveTo(subProgLookup[0],subProgLookup[1]);
  }
  
  private boolean handleReturn() {
    
    // Pop the return point off the stack, and jump there.
    // If there's a random M99, it could happen that we are trying to return
    // when we are not in fact in a sub-program. In that case, return false;
    // otherwise return true;
    
    if (theStack.size() == 0)
      // The stack is empty. Error!
      return false;
    
    int[] retPoint = theStack.pop();
    theParser.moveTo(retPoint[0],retPoint[1]);
    
    return true;
  }
  
  Statement nextStatement() {
    
    // Read and translate statements to be passed up to the next layer.
    // This layer does nothing but handle subroutines calls. Everything else
    // is just passed up (or trashed if it's something like a coolant command).
    // BUG: SHOULD I trash coolant commands and the like?
    Statement answer = null;
    
    Statement cmd = theParser.getStatement();
    
    // Don't return until a valid Statement has been generated.
    while (answer == null)
      {
        switch (cmd.type)
          {
            case Statement.ERROR : 
              // We don't throw these; they simply propagate upward.
              answer = cmd;
            case Statement.EOF : 
              return cmd;
            case Statement.NIL :
              break;
            case Statement.MOVE :
              answer = cmd;
              break;
            case Statement.PROG : 
              // Program numbers should not occur here. Any program numbers 
              // should either be at the beginning of the input (and it was 
              // read in the constructor), or it is a sub-program and we 
              // should never get there because an M30 comes before it.
              answer = cmd;
              cmd.type = Statement.ERROR;
              cmd.error = formError(cmd.lineNumber,
                  "unexpected O-value -- forget an M30?");
              break;
            case Statement.G00 : 
            case Statement.G01 : 
            case Statement.G02 : 
            case Statement.G03 : 
            case Statement.G15 : 
            case Statement.G16 : 
            case Statement.G17 : 
            case Statement.G18 : 
            case Statement.G19 : 
            case Statement.G20 : 
            case Statement.G21 : 
              // BUG: Why skipping G28?
            case Statement.G40 : 
            case Statement.G41 : 
            case Statement.G42 : 
            case Statement.G43 :
            case Statement.G44 : 
            case Statement.G49 : 
            case Statement.G52 : 
            case Statement.G54 : 
            case Statement.G55 : 
            case Statement.G56 : 
            case Statement.G57 : 
            case Statement.G58 : 
            case Statement.G59 : 
            case Statement.G90 : 
            case Statement.G91 : 
              answer = cmd;
              break;
            case Statement.N   : 
              // Ignore line numbers. The lexer considers line numbers to be 
              // whitespace anyway so this is redundant.
              break;
            case Statement.M00 :
            case Statement.M01 :
            case Statement.M02 : 
              // These are "program stops". On a real machine, the
              // program might not actually stop, but I do.
              return new Statement(Statement.EOF);
            case Statement.M03 : 
            case Statement.M04 : 
            case Statement.M05 : 
            case Statement.M06 : 
              answer = cmd;
              break;
            case Statement.M07 :
            case Statement.M08 : 
            case Statement.M09 :
              // Coolant on/off. Ignore.
              // BUG: Ignore this?
              break;
            case Statement.M30 : 
              // End of program. Treat as EOF by returning.
              return new Statement(Statement.EOF);
            case Statement.M40 : 
            case Statement.M41 :
              // Spindle high/low. Ignore.
              // BUG: Should this be ignored?
              break;
            case Statement.M47 : 
              // Repeat program. Not sure what to do, so we halt.
              return new Statement(Statement.EOF);
            case Statement.M48 : 
            case Statement.M49 :
              // Feed & speed overrides. Ignore.
              // BUG: Should be ignored?
              break;
            case Statement.M98 :
              // Call a subprogram.
              // 
              if (theStack.size() > MaxStackDepth)
                {
                  // Stack is too deep, probably due to runaway
                  // recursion. Make this an error and return.
                  cmd.type = Statement.ERROR;
                  cmd.error = formError(cmd.lineNumber,
                      "you've called too many sub-programs");
                  return cmd;
                }
              
              handleCall(cmd);
              if (cmd.type == Statement.ERROR)
                // Error on the sub-program call.
                return cmd;
              
              break;
            case Statement.M99 : 
              // Return from subprogram.
              boolean ok;
              ok = handleReturn();
              if (ok == false)
                {
                  // Attempt to return from a sub-program when we're not
                  // IN a sub-program. No way to recover. Halt the
                  // program and insert an error message.
                  Statement err = new Statement(Statement.EOF);
                  err.error = formError(cmd.lineNumber,
                      "M99 appeared, but not in a sub-program");
                  return err;
                }
              break;
              
            default : 
              answer = cmd;
              answer.type = Statement.ERROR;
              answer.error = formError(cmd.lineNumber,"unknown code");
              break;
          }
        
        if (answer == null)
          // Not done. Look at next statement.
          cmd = theParser.getStatement();
      }
    
    return answer;
  }

  public void reset() {
    
    theStack.clear();
    theParser.reset();
    readProgramNumber();
  }

}
