package vcnc.tpile;

/*

This filters out calls to sub-programs (M98, M99), which has the effect of 
making the program longer -- maybe *much* longer. Because G-code has no 
concept of variables or loops, this is conceptually a matter of inserting
some known number of copies of the sub-programs. 

Up until v08, this was done by rewinding the lexer (and parser, etc.) to 
the appropriate point and repeatedly generating the code for any sub-program.
When wizards were introduced, this caused problems, and everything was
refactored so that each layer digests everything in that layer before passing
the entire result along. 

Note that this also removes O-codes from the stream. After M98/99 have been
unraveled, O-codes serve no purpose. It's tempting to put the main O-code 
back (at the head of the file) since the user's physical CNC machine may 
require one, but he can always put it back.

*/

import java.util.ArrayList;
import java.util.Stack;
import java.util.Hashtable;

import vcnc.tpile.parse.DataSubProg;
import vcnc.tpile.parse.DataSubroutineCall;


public class Layer00 {
  
  // The Statement objects from the lower layer, and the mark of where we
  // are in processing them.
  private ArrayList<St0B> theStatements = null;
  private int statementIndex = 0;

  // A lookup table showing where all subprograms begin (where the O### appears).
  // The key is the program number (O-code) and the value is the index in
  // theStatements at which that program number appears.
  private Hashtable<Integer,Integer> subprogs = new Hashtable<>();
  
  // The call stack. It's a series of line numbers (really, indices into
  // theStatements). Every time there's a "return," the "program counter"
  // goes to the indicated line number. See handleCall(). 
  private Stack<Integer> theStack = new Stack<>();
  
  // The maximum level of recursion. In theory, there is no reason to limit
  // this, but at some point it is almost certainly an error on the 
  // user's part.
  // BUG: Allow the user to control this?
  private static final int MaxStackDepth = 1000; 
  
  
  public Layer00(CodeBuffer buf) {
    
    // BUG...BOGUS. DON'T USE THIS. IT'S HERE AS A TEMPORARY THING TO QUIET
    // THE COMPILER
  }
  
  
  
  private Layer00(ArrayList<St0B> smnts) {

    this.theStatements = smnts;
    this.statementIndex = 0;
    
    // Create the table of subprograms.
    for (int i = 0; i < theStatements.size() ; i++)
      {
        St0B s = theStatements.get(i);
        if (s.type == St0B.PROG)
          {
            DataSubProg o = (DataSubProg) s.data;
            this.subprogs.put(o.progNumber,i);
          }
      }  
  }

  private St0B getLower() {
    
    if (statementIndex >= theStatements.size())
      return new St0B(St0B.EOF);
    
    St0B answer = theStatements.get(statementIndex);
    ++statementIndex;
    return answer;
  }
  
  private St0B peekLower() {
    
    if (statementIndex >= theStatements.size())
      return new St0B(St0B.EOF);
    
    return theStatements.get(statementIndex);
  }

  private String formError(int lineNumber,String msg) {
    
    return "Error on line " +lineNumber+ ": " +msg;
  }
  
  private St00 formError(St00 s,String msg) {
    
    // BUG: This is a much better way to do things.
    // See I can get rid of the "other" formError() in all classes that do this.
    St00 answer = new St00(St00.ERROR);
    answer.lineNumber = s.lineNumber;
    answer.charNumber = s.charNumber;
    answer.error = formError(s.lineNumber,msg);
    return answer;
  }

  private void handleCall(St0B cmd) {
    
    // Subroutines calls are done with a simple stack. The entries of the stack
    // are indices in theStatements to which the program the program should
    // return from a sub-program. Because a subroutine can be called multiple
    // times by the same M98, we push the same return value on the stack for 
    // as many times as the subroutine is called (less 1), then the final 
    // return point. It's tempting to try to use a simple integer count of the 
    // number of M98 invocations, but sub-programs can call other sub-programs,
    // so this wouldn't eliminate the need for a stack.
    //
    // If there's a problem, convert cmd to an ERROR statement.
    
    // See where, in theStatements, the sub-program is that's to be called.
    DataSubroutineCall call = (DataSubroutineCall) cmd.data;
    if (subprogs.contains(call.programNumber) == false)
      {
        // Attempt to call a sub-program that was never defined.
        // That is, the P-value in the M98 refers to an O-value that
        // was never defined.
        cmd.type = St0B.ERROR;
        cmd.error = formError(cmd.lineNumber,"no such sub-program was defined");
        return;
      }
    
    int subProgLookup = subprogs.get(call.programNumber);
    
    // Push the ultimate return point (after every call has been made) onto 
    // the stack. This "ultimate return point" is just where we are now.
    theStack.push(this.statementIndex);
    
    // Push the same subroutine (the one we are calling) onto the stack as
    // many times as the L-value indicates (minus one since we're about to
    // jump there).
    int count = 1;
    while (count < call.invocations)
      {
        // BUG: Should this be subProgLookup + 1? So as to skip the O-code
        // itself for a tiny savings?
        theStack.push(subProgLookup);
        ++count;
      }
    
    // Finally, jump to the right place for the first invocation.
    // BUG: again, plus 1?
    this.statementIndex = subProgLookup;
  }
  
  private boolean handleReturn() {
    
    // Pop the return point off the stack, and jump there.
    // If there's a random M99, it could happen that we are trying to return
    // when we are not in fact in a sub-program. In that case, return false;
    // otherwise return true.
    
    if (theStack.size() == 0)
      // The stack is empty. Error!
      return false;
    
    int retPoint = theStack.pop();
    this.statementIndex = retPoint;
    
    return true;
  }
  
  St00 nextStatement() {
    
    // This is where the transformation is done.
    
    
    // BUG: MAKE THIS PRIVATE. I'VE LEFT IT THIS WAY TO QUIET THE COMPILER
    // SINCE Layer01 USES IT, EVEN THOUGH IT USES IT AN ENTIRELY WRONG WAY.
    
    
    St0B cmd = getLower();
    
    if (cmd.type == St0B.PROG)
      // Filter these out by making a recursive call.
      return nextStatement();
    else if (cmd.type == St0B.M98)
      {
        // Call a sub-program.
        if (theStack.size() > MaxStackDepth)
          // Stack is too deep, probably due to runaway recursion.
          return formError(cmd,"too many sub-programs");
        
        handleCall(cmd);
        
        if (cmd.type == St0B.ERROR)
          // Error on the sub-program call.
          return cmd;
      }
    else if (cmd.type == St0B.M99)
      {
        // Return from subprogram.
        boolean ok;
        ok = handleReturn();
        if (ok == false)
          // Attempt to return from a sub-program when we're not IN a   
          // sub-program.
          return formError(cmd,"M99 appeared, but not in a sub-program");
      }
    
    // Not handled above; just pass it along. Note the implicit cast.
    return cmd;
  }

  
  
  
  public void reset() {
    
    // BUG: leaving this here temporarily to quiet the compiler
    // DON'T USE THIS
    
    
//    theStack.clear();
//    lowerLayer.reset();
//    readProgramNumber();
  }
  
  
  

  public static ArrayList<St00> process(String gCode) {
    
    // Transform the Statement objects from Layer0B to a more limited
    // subset.
    ArrayList<St00> answer = new ArrayList<>();
    
    Layer00 curLayer = new Layer00(Layer0B.process(gCode));
    
    St00 s = curLayer.nextStatement();
    
    while (s.type != St00.EOF)
      {
        answer.add(s);
        s = curLayer.nextStatement();
      }
    
    return answer;
  }

  public static String digestAll(String gcode) {
    
    // Take the given g-code and feed it through, producing a single String
    // suitable for output to the user, or for use with unit tests.
    // BUG: Isn't this method identical in every case? And the process() method
    // is pretty close to identical too.
    ArrayList<St00> theStatements = process(gcode);

    StringBuffer answer = new StringBuffer();
    
    for (St00 s : theStatements)
      {
        answer.append(s.toString());
        answer.append("\n");
      }
    
    return answer.toString();
  }

}
