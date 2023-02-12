package vcnc.tpile;

/*

This is used to represent a single "statement" of some G-code. This could be 
a move, or it could be some change to the internal state of the machine, like 
moving in and out of incremental mode.

Error Handling... 
BUG: Get rid of the error field below. A statement should be either of
ERROR type or known to be correct.

Another way to deal with errors would be to *leave* the error field and
set it to a non-null value when there is an error on that line. In some
ways I like that solution better since error messages can then be tied
directly to the line that's a problem. OTOH, the fact that there's an
error is no longer indicated by the type. Then again, there are times
when I *replace* a statement by an error message, and that's less than
ideal.

There's been a lot of variation in how this was dealt with. I started with
essentially this, then moved to a method of sub-typing (see v08-10), but found
that it was more trouble than it was worth. There was a lot of casting up/down
the chain of sub-types, which defeats the purpose, while adding boilerplate
and confusion. In theory, the idea of using the type system so that the
compiler would catch mistakes has value; it was just to fiddly and too
leaky to really work. Now (v11) that the transpilation no longer has such
explicit layers, it makes even less sense.


Some of the codes that people *might* want or expect that have not been
included are:

M02
On modern machines, this is the same thing as M30 (end program). Back when 
machines used paper tape, this acted as a "stop," but did not rewind the tape.
 
M47
Usually, this means "repeat program," but it can mean other things on some 
machines. I saw something on a Haas website about it being used for engraving 
(of all things). If someone really wants a program to repeat using M47, it 
will be easy enough for them to add it after translation.

G28
See the manual for a discussion. It's meant as a "return to home" (i.e.,
machine zero) command. There's no real need for this outside setup of a
physical machine, and trying to include it would lead to all sorts of strange
edge-cases.  It is tempting to allow a machine directive to change machine 
zero, but it would complicate things, has the potential to confuse the user,
and doesn't seem useful.


And these codes might act in a way that is not what the user would prefer.

G52, G54-G59 and G92
Again, see the user manual for a discussion. These are filtered away by 
translation. They work in a way that's consistent with most machines, but
there's too much variation among machines for them to work *exactly* like
any particular machine. The usage of G92 is particularly likely to differ
from what some users expect. My older FANUC machine uses G92 this way, but
not every machine does.

G40, G43 and G44
These are for TLO (tool length offset). They don't really do anything in
the context of the program; they simply pass through unchanged. Their
intended to take into account the physical arrangement of their machine,
but it would be too difficult to take that stuff into account here. In theory,
it would be possible for the tool table to specify various "offsets" that
the user's G-code would be expected to compensate for, but that seems like
an unnecessary complication. The whole purpose of TLO is to compensate for
something that's irritating about physical machines and the value of a
simulation is that you can abstract away certain real-world irritations.
That said, I can imagine a user who wants the simulation to take these things
into account so that he can "check his work" before going turning to a
physical machine. Allowing for that would only frustrate a larger number of
users.
 
  
*/

import vcnc.tpile.parse.StatementData;
import vcnc.tpile.parse.DataCircular;
import vcnc.tpile.parse.DataInt;
import vcnc.tpile.parse.DataMove;
import vcnc.tpile.parse.DataRegister;
import vcnc.tpile.parse.DataTLO;
import vcnc.tpile.parse.DataSubProg;
import vcnc.tpile.parse.DataSubroutineCall;
import vcnc.tpile.parse.DataWizard;


public class Statement {

  // These are the possible statements. Most are in one-to-one correspondence
  // with G/M-codes; a few are more complex.
  // The 2xxx values are used for M-codes and the 3xxx values are G-codes.
  
  
  // Housekeeping codes.
  public static final short UNKNOWN = -1; // Occasionally used when allocating 
  public static final short ERROR =  0;   // Problem to be reported to user
  
  // These are filtered out very early when sub-programs are dealt with,
  // at the Translator.ThruSubProgs stage.
  //
  // BUG: I can imagine (?) wizards wanting a "terminate program" option.
  // M30 is off the table, but we could provide something else -- sort
  // of a "chop the WIP off here in the translator." Basically, call
  // LList.truncateAt() since "chopping off" is how the program terminates.
  public static final short EOF   = 100;
  public static final short PROG  = 101;  // An O-code, like O1234
  public static final short M98   = 2098; // Call subprogram
  public static final short M99   = 2099; // Return from subprogram
  public static final short M30   = 2030; // End of program
  
  // Dealt with at the Translator.ThruWizards stage.
  public static final short WIZARD = 1;
  
  // Dealt with at the Translator.ThruUnits stage.
  public static final short G20   = 3020; // Inches
  public static final short G21   = 3021; // Millimeters
  
  // Dealt with at the Translator.ThruWorkOFfsets stage.
  public static final short G52   = 3052; // Temporary change in PRZ
  public static final short G54   = 3054; // Work offsets
  public static final short G55   = 3055;
  public static final short G56   = 3056;
  public static final short G57   = 3057;
  public static final short G58   = 3058;
  public static final short G59   = 3059;
  public static final short G92   = 3092; // another way to change PRZ
  
  // Dealt with at the Translator.ThruPolar stage.  
  public static final short G15   = 3015; // Polar coordinates off
  public static final short G16   = 3016; // Polar coordinates on
  
  // Dealt with at the Translator.ThruIncremental stage.
  public static final short G90   = 3090; // Absolute mode
  public static final short G91   = 3091; // Incremental mode
  
  
  // These pass all the way through, perhaps with modification, to the fully
  // translated result.
  // Remember: G00 and G01 merely change the travel mode; MOVE moves the cutter.
  // Also, I keep wanting to filter away G17/18/19, but they matter when it
  // comes time to interpret G02 and G03.
  public static final short MOVE  =  999; // tool move
  public static final short G00   = 3000; // rapid mode 
  public static final short G01   = 3001; // normal mode
  public static final short G02   = 3002; // CW circular interpolation
  public static final short G03   = 3003; // CCW circular interpolation
  public static final short G17   = 3017; // Choose plane for interpolation
  public static final short G18   = 3018; //  influences the meaning of G02/03
  public static final short G19   = 3019;
  
  // These also pass through. They have no effect on translation, but
  // could be important on a physical machine or in simulation.
  // 
  // Note that M06 (tool change) is a special case since it *does* affect
  // translation, but only due to cutter comp. Also, the TLO commands
  // G43/44/49 might be modified to use machine units. TLO commands don't
  // "matter," but converting this way is being nice to the user.
  // 
  // The meaning of M00 and M01 seem to vary from machine to machine. They
  // are treated here as "temporary pause." On a real machine, you could put
  // one of these in your code, the program would pause, then you hit some
  // button to continue. Unlike M02, which some people might expect to act
  // like M30, these can be ignored by the translator so they don't hurt to
  // allow in the stream.
  public static final short M00   = 2000; // pause
  public static final short M01   = 2001; // pause
  public static final short M03   = 2003; // spindle on, CW 
  public static final short M04   = 2004; // spindle on, CCW
  public static final short M05   = 2005; // spindle off
  public static final short M06   = 2006; // Tool change
  public static final short M07   = 2007; // Coolant on
  public static final short M08   = 2008; // Coolant on
  public static final short M09   = 2009; // Coolant off
  public static final short M40   = 2040; // Spindle high
  public static final short M41   = 2041; // Spindle low
  public static final short M48   = 2048; // Enable feed & speed overrides
  public static final short M49   = 2049; // Disable overrides

  public static final short G43   = 3043; // TLO, positive.
  public static final short G44   = 3044; // TLO, negative.
  public static final short G49   = 3049; // Cancel TLO.
  
  
  
  
  

  
  // Yet to handle...
  

  
  
  public static final short G41   = 10010;  // Cutter comp left.
  public static final short G42   = 10011;  // Cutter comp right.
  public static final short G40   = 23;   // Cancel cutter comp.
  
  
  
  
  
  

  
  
  
  // One of the constants above.
  public short type;
  
  // Line number on which statement occurred. Needed to report errors.
  // This is the line number with respect to carriage returns, not N-codes.
  public int lineNumber;
  
  // Typically, this is empty.
  public String error = null;
  
  // For many simple statements, this will be null. The exact nature 
  // of the data (and sub-type of StatementData) depends on the statement.
  // BUG: Maybe errors/warnings could be put in here instead of above. Cleaner.
  // Yes, I think that would be better. A statement is either of type ERROR
  // or it is correct.
  public StatementData  data = null;

  
  public Statement(short type) {
    this.type = type;
  }
  
  public Statement deepCopy() {
    
    // A deep copy is needed when shuffling Statements for subprograms.
    Statement answer = new Statement(this.type);
    answer.lineNumber = this.lineNumber;
    answer.error = this.error;
    if (this.data != null)
      answer.data = this.data.deepCopy();
    
    return answer;
  }
  
  public String toString() {
    
    // Convert the current statement to a String.
    String answer = null;
    
    switch (type)
      {
        case ERROR  : return(String.format("N%05d\tERROR\t%s",lineNumber,error));
        case EOF    : return(String.format("N%05d\tEOF",lineNumber));
        
        case M00    : return(String.format("N%05d\tM00",lineNumber));
        case M01    : return(String.format("N%05d\tM01",lineNumber));
        case M03    : answer = String.format("N%05d\tM03 ",lineNumber);
                      DataInt state = (DataInt) data;
                      answer += String.format("S%3d",state.value);
                      return answer;
        case M04    : answer = String.format("N%05d\tM04 ",lineNumber);
                      state = (DataInt) data;
                      answer += String.format("S%03d",state.value);
                      return answer;
        case M05    : return(String.format("N%05d\tM05",lineNumber));
        case M06    : answer = String.format("N%05d\tM06 ",lineNumber);
                      state = (DataInt) data;
                      answer += String.format("T%02d",state.value);
                      return answer;
        case M07    : return(String.format("N%05d\tM07",lineNumber));
        case M08    : return(String.format("N%05d\tM08",lineNumber));
        case M09    : return(String.format("N%05d\tM09",lineNumber));
        case M30    : return(String.format("N%05d\tM30",lineNumber));
        case M40    : return(String.format("N%05d\tM40",lineNumber));
        case M41    : return(String.format("N%05d\tM41",lineNumber));
        case M48    : return(String.format("N%05d\tM48",lineNumber));
        case M49    : return(String.format("N%05d\tM49",lineNumber));
        case M98    : DataSubroutineCall call = (DataSubroutineCall) data;
                      answer = String.format("N%05d\tM98 ",lineNumber);
                      answer += String.format("P%05d ",call.programNumber);
                      if (call.invocations > 1)
                        answer += String.format("L%04d",call.invocations);
                      return answer;
        case M99    : return(String.format("N%05d\tM99",lineNumber));
        
        
        case G00    : return(String.format("N%05d\tG00",lineNumber));
        case G01    : return(String.format("N%05d\tG01",lineNumber));

        
        case G40    : return(String.format("N%05d\tG40",lineNumber));
        case G49    : return(String.format("N%05d\tG49",lineNumber));
        case G90    : return(String.format("N%05d\tG90",lineNumber));
        case G91    : return(String.format("N%05d\tG91",lineNumber));
        case G15    : return(String.format("N%05d\tG15",lineNumber));
        case G16    : return(String.format("N%05d\tG16",lineNumber));
        case G17    : return(String.format("N%05d\tG17",lineNumber));
        case G18    : return(String.format("N%05d\tG18",lineNumber));
        case G19    : return(String.format("N%05d\tG19",lineNumber));
        case G54    : return(String.format("N%05d\tG54",lineNumber));
        case G55    : return(String.format("N%05d\tG55",lineNumber));
        case G56    : return(String.format("N%05d\tG56",lineNumber));
        case G57    : return(String.format("N%05d\tG57",lineNumber));
        case G58    : return(String.format("N%05d\tG58",lineNumber));
        case G59    : return(String.format("N%05d\tG59",lineNumber));
        
        case MOVE   : answer = String.format("N%05d\t",lineNumber);
                      DataMove theMove = (DataMove) data;
                      if (theMove.xDefined == true)
                        answer += "X"+String.format("%+07.3f\t",theMove.xValue);
                      else
                        answer += "\t\t";
                      if (theMove.yDefined == true)
                        answer += "Y"+String.format("%+07.3f\t",theMove.yValue);
                      else
                        answer += "\t\t";
                      if (theMove.zDefined == true)
                        answer += "Z"+String.format("%+07.3f\t",theMove.zValue);
                      if (theMove.fDefined == true)
                        answer += "F"+String.format("%05.2f",theMove.fValue);
                      return answer;
        case G02    : answer = String.format("N%05d\tG02 ",lineNumber);
                      DataCircular circ = (DataCircular) data;
                      if (circ.xDefined)
                        answer += String.format("X%+07.3f\t",circ.X);
                      else
                        answer += "\t\t";
                      if (circ.yDefined)
                        answer += String.format("Y%+07.3f\t",circ.Y);
                      else
                        answer += "\t\t";
                      if (circ.zDefined)
                        answer += String.format("Z%+07.3f\t",circ.Z);
                      else
                        answer += "\t\t";
                      if (circ.rDefined)
                        answer += String.format("R%+07.3f\t",circ.R);
                      else
                        answer += "\t\t";
                      if (circ.iDefined)
                        answer += String.format("I%+07.3f\t",circ.I);
                      else
                        answer += "\t\t";
                      if (circ.jDefined)
                        answer += String.format("J%+07.3f\t",circ.J);
                      else
                        answer += "\t\t";
                      if (circ.kDefined)
                        answer += String.format("K%+07.3f\t",circ.K);
                      else
                        answer += "\t\t";
                      if (circ.F > 0)
                        answer += String.format("F%05.2f ",circ.F);
                      return answer;
        case G03    : answer = String.format("N%05d\tG03 ",lineNumber);
                      circ = (DataCircular) data;
                      if (circ.xDefined)
                        answer += String.format("X%+07.3f\t",circ.X);
                      else
                        answer += "\t\t";
                      if (circ.yDefined)
                        answer += String.format("Y%+07.3f\t",circ.Y);
                      else
                        answer += "\t\t";
                      if (circ.zDefined)
                        answer += String.format("Z%+07.3f\t",circ.Z);
                      else
                        answer += "\t\t";
                      if (circ.rDefined)
                        answer += String.format("R%+07.3f\t",circ.R);
                      else
                        answer += "\t\t";
                      if (circ.iDefined)
                        answer += String.format("I%+07.3f\t",circ.I);
                      else
                        answer += "\t\t";
                      if (circ.jDefined)
                        answer += String.format("J%+07.3f\t",circ.J);
                      else
                        answer += "\t\t";
                      if (circ.kDefined)
                        answer += String.format("K%+07.3f\t",circ.K);
                      else
                        answer += "\t\t";
                      if (circ.F > 0)
                        answer += String.format("F%5.2f ",circ.F);
                      return answer;
        case G41    : answer = String.format("N%05d\tG41 ",lineNumber);
                      DataRegister r = (DataRegister) data;
                      if (r.D) answer += "D"; else answer += "H";
                      answer += String.format("%02d",r.regValue);
                      return answer;
        case G42    : answer = String.format("N%05d\tG42 ",lineNumber);
                      r = (DataRegister) data;
                      if (r.D) answer += "D"; else answer += "H";
                      answer += String.format("%02d",r.regValue);
                      return answer;
        case G43    : answer = String.format("N%05d\tG43 ",lineNumber);
                      DataTLO tlo = (DataTLO) data;
                      if (tlo.hasZ == true) 
                        answer += String.format(" Z%04f",tlo.zValue);
                      answer += String.format(" H%02d",tlo.hRegister);
                      return answer;
        case G44    : answer = String.format("N%05d\tG44 ",lineNumber);
                      tlo = (DataTLO) data;
                      if (tlo.hasZ == true) 
                        answer += String.format(" Z%04f",tlo.zValue);
                      answer += String.format(" H%02d",tlo.hRegister);
                      return answer;
        case G52    : answer = String.format("N%05d\tG52 ",lineNumber);
                      theMove = (DataMove) data;
                      if (theMove.xDefined == true)
                        answer += "X"+String.format("%+07.3f\t",theMove.xValue);
                      if (theMove.yDefined == true)
                        answer += "Y"+String.format("%+07.3f\t",theMove.yValue);
                      if (theMove.zDefined == true)
                        answer += "Z"+String.format("%+07.3f\t",theMove.xValue);
                      return answer;

        case G20    : return(String.format("N%05d\tG20",lineNumber));
        case G21    : return(String.format("N%05d\tG20",lineNumber));
        
        case PROG   : DataSubProg ovalue = (DataSubProg) data;
                      return String.format("N%05d\tO%05d",lineNumber,ovalue.progNumber);
        
        case WIZARD : DataWizard wiz = (DataWizard) this.data;
                      answer = wiz.cmd;
                      for (int i = 0; i < wiz.args.size(); i++)
                        {
                          Object o = wiz.args.get(i);
                          if (o instanceof Double)
                            answer += " " + (Double) o;
                          else if (o instanceof String)
                            answer += " \"" + (String) o + "\"";
                          else
                            // Should be impossible.
                            answer += " unknown weirdness";
                        } 
                      return answer;
        

        default     : answer = "Unknown Statement type: " +type;
      }
    return answer;
  }
}
