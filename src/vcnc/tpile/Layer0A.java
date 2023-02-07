package vcnc.tpile;

/*

This comes immediately after the Parser and acts on certain "machine 
directives" that must appear before the O-code. These are for things like 
setting the billet dimensions or changing the work offsets table or tool 
table in way that is independent of the persistent machine setup.  

To the user, syntactically, these are a lot like wizards, and they come
in that way from the parser. In essence, any "wizard" that occurs before
the O-code must be one of these directives, and any "wizard" that comes after 
the O-code is a wizard in the usual sense.


BUG: None of these directives are actually handled, though "Billet"
will get filtered out as a valid directive and the arguments are checked 
for "SetUnits." Actually, maybe SetUnits is done (at least for now).

*/

import java.util.ArrayList;

import vcnc.tpile.parse.Parser;
import vcnc.tpile.parse.DataWizard;


public class Layer0A {
  
  
  /*

  
  // The statement objects from the lower layer, and the mark of where we
  // are in processing them.
  private ArrayList<StxP> theStatements = null;
  private int statementIndex = 0;
  
  // Whether we've read up to the first O-statement. Used primarily for
  // error checking.
  private boolean pastO = false;
  
  
  private Layer0A(ArrayList<StxP> smnts) {
    this.theStatements = smnts;
    this.statementIndex = 0;
  }
  
  private StxP getLower() {
    
    if (statementIndex >= theStatements.size())
      return new StxP(StxP.EOF);
    
    StxP answer = theStatements.get(statementIndex);
    ++statementIndex;
    return answer;
  }
  
  private String formError(int lineNumber,String msg) {
    
    return "Error on line " +lineNumber+ ": " +msg;
  }
  
  private StxP formError(StxP s,String msg) {
    
    // BUG: This is a much better way to do things.
    // See I can get rid of the "other" formError() in all classes that do this.
    StxP answer = new StxP(StxP.ERROR);
    answer.lineNumber = s.lineNumber;
    answer.charNumber = s.charNumber;
    answer.error = formError(s.lineNumber,msg);
    return answer;
  }
  
  private StxP handleDirective(StxP wizard) {
    
    // The given statement is known to be a wizard. Either: 
    // * Act on it and return null (null since the statement was consumed)
    // * Determine that there's an error and return an error statement.
    //   Using genuine wizards (not machine directives) before the O-code is
    //   such an error.
    DataWizard wizData = (DataWizard) wizard.data;
    
    // Remember, this only accepts "machine directives," not genuine wizards.
    if (wizData.cmd.equals("Billet"))
      {
        // BUG: Handle this case...and add others.
        // For now, just consume this and ignore it.
        System.out.println("Billet directive not handled yet.");
        return null;
      }
    else if (wizData.cmd.equals("SetUnits"))
      {
        // Allow the user to indicate, before the program starts, what the
        // units are. This is a little silly since the program could include
        // G20 or G21, but maybe somebody would want this and it's an easy
        // test for the code.
        if (wizData.args.size() != 1)
          return formError(wizard,"SetUnits has the wrong number of arguments");
        
        Object arg = wizData.args.get(0);
        
        if (arg instanceof String == false)
          return formError(wizard,"SetUnits takes 'inch' or 'mm' as argument");  
        
        String choice = (String) arg;
        
        if (choice.equals("inch"))
          MachineState.machineInchUnits = true;
        else
          MachineState.machineInchUnits = false;
      }
    else
      {
        // Something unexpected that shouldn't occur before the O-statement.
        // Could be a typo, or could be an attempt to use a genuine wizard
        // before it's permitted.
        return formError(wizard,
            "unexpected " +wizData.cmd+ " before O (program number)");
      }
    
    // Got here, so the machine directive wizard was properly handled, and
    // therefore consumed from the stream.
    return null;
  }
  
  private StxP nextStatement() {
    
    StxP answer = getLower();
    
    if (this.pastO == true)
      // We're past anything this class handles. Just pass the statement up
      // to the next layer.
      return answer;
    
    if (answer.type == StxP.PROG)
      {
        // Hit the O-statement.
        this.pastO = true;
        return answer;
      }
    
    if (answer.type != StxP.WIZARD)
      // Just a plain old statement, not a wizard directive. Pass it along.
      return answer;
    
    // Got here, so the statement is a wizard, hopefully a "machine directive."
    answer = handleDirective(answer);
    
    if (answer != null)
      // Wizard was not handled, so there must have been an error to return.
      return answer;
    
    // Got here, so the wizard was a valid "directive" and these disappear
    // from the stream. Make a recursive call to get the next valid Statement.
    // The depth of recursion here should be very limited, so that's fine.
    return nextStatement();
  }

  public static ArrayList<StxP> process(String gCode) {
    
    // Transform the Statement objects from the Parser to a more limited
    // subset.
    ArrayList<StxP> answer = new ArrayList<>();
    
    Layer0A curLayer = new Layer0A(Parser.process(gCode));
    
    StxP s = curLayer.nextStatement();
    
    while (s.type != StxP.EOF)
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
    ArrayList<StxP> theStatements = process(gcode);

    StringBuffer answer = new StringBuffer();
    
    for (StxP s : theStatements)
      {
        answer.append(s.toString());
        answer.append("\n");
      }
    
    return answer.toString();
  }
*/
  
  
  
}



