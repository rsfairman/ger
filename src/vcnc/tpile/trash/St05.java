package vcnc.tpile.trash;

/*

The sort of statement that will be output by the final layer.

See StxP for some background.


BUG: I'm wondering if the various fields here shouldn't be removed and put
into sub-classes of the data field. E.g., only the ERROR type needs to know
about the error/warning fields. Similarly, the line/char number are not 
often needed. 

*/

import vcnc.tpile.parse.DataCircular;
import vcnc.tpile.parse.DataInt;
import vcnc.tpile.parse.DataMove;
import vcnc.tpile.parse.DataRegister;
import vcnc.tpile.parse.StatementData;


public class St05 {
  
  public static final short ERROR =  0;
  public static final short EOF   =  1;
  
  // These actually move the tool.
  public static final short G00   =  2;   // rapid mode
  public static final short G01   =  3;   // normal mode
  
  
  // Other things that pass through all the layers, pretty much untouched.
  
  
  
  
  // One of the constants above (or in an extension of this class).
  public short type;
  
  // Line number on which statement occurred. Needed to report errors.
  // This is the line number with respect to carriage returns, not N-codes.
  public int lineNumber;
  
  // The character count, in the entire program, at which this statement
  // starts. We need this to return from subroutines.
  // BUG: Do we need this anymore?
  public int charNumber;
  
  // Typically, this is empty.
  public String error = null;
  
  // If this is true, then the error above is merely a warning
  // BUG: Is this actually used?
  public boolean warning = false;
  
  // This will often be null. For many simple statements, this will be null. 
  // It will be non-null for the more complex statements. The exact nature 
  // of the data (and sub-type of StatementData) depends on the statement.
  // BUG: Maybe errors/warnings could be put in here instead of above. Cleaner.
  public StatementData  data = null;
  
  
//  public St05() {
//    // Do-nothing.
//    // BUG: Don't like this do-nothing. A Statement should require a type,
//    // so that only the other constructor makes sense.]
//    // Make this private or get rid of entirely.
//  }
  
  public St05(short type) {
    this.type = type;
  }
  
  public String toString() {
    
    // Convert the current statement to a String.
    String answer = null;
    
    switch (type)
      {
        case ERROR  : return(String.format("N%05d\tERROR\t%s",lineNumber,error));
        case EOF    : return(String.format("N%05d\tEOF",lineNumber));
        case G00    : return(String.format("N%05d\tG00",lineNumber));
        case G01    : return(String.format("N%05d\tG01",lineNumber));
        

        default     : answer = "Unknown Statement type: " +type;
      }
    return answer;
  }
  

}
