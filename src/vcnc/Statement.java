package vcnc;

// BUG: Get rid of this. It's hanging around while I refactor.


import vcnc.tpile.parse.DataCircular;
import vcnc.tpile.parse.DataInt;
import vcnc.tpile.parse.DataMove;
import vcnc.tpile.parse.DataRegister;
import vcnc.tpile.parse.DataSubProg;
import vcnc.tpile.parse.DataSubroutineCall;
import vcnc.tpile.parse.DataWizard;
import vcnc.tpile.parse.StatementData;

/*

This is used to represent a single "statement" of some G-code. A statement is
something that is actionable. It could be a move, or it could be some change
to the internal state of the machine, like moving in and out of incremental
mode.

*/


public class Statement {
	
	// There are a finite number of possible statements. Here they are.
	// Some of these (most) are in one-to-one correspondence with G/M-codes; 
  // others are more complex.
	
	// These are simple statements that stand alone, and require no additional
	// data to interpret.
	public static final short ERROR =  0;   // A show-stopper. Causes a halt.
	public static final short EOF 	=  1;
	public static final short G00		=  2;		// rapid mode
	public static final short G01		=  3;		// normal mode
	public static final short N			=  5;		
	    // line number. BUG: I think these are weeded out of the input
	    // and should never appear as statements, so get rid of this?
	public static final short M05		=  6;		// spindle off
	public static final short M00   =  7;   // Hard program stop
	public static final short M01   =  8;   // Optional program stop
	public static final short M02   =  9;   // Program end.
	public static final short M07   = 10;		// Coolant on.
	public static final short M08   = 11;		// Coolant on.
	public static final short M09   = 12; 	// Coolant off.
	public static final short M30   = 13;		// End of program.
	public static final short M40   = 14;		// Spindle high
	public static final short M41   = 15; 	// Spindle low
	public static final short M47   = 16;   // Repeat program
	public static final short M48		= 17;		// Enable feed & speed overrides.
	public static final short M49		= 18; 	// Disable overrides.
	public static final short M99		= 19;		// Return from subprogram
	
	public static final short NIL   = 20;   // Codes that should simply be
	                                        // dropped and ignored.
	                                        // BUG: Used?
	public static final short G20   = 21;   // Inches
	public static final short G21		= 22; 	// Millimeters.
	public static final short G40		= 23; 	// Cancel cutter comp.
	public static final short G49		= 27;		// Cancel TLO.
	public static final short G90		= 28;		// Absolute mode.
	public static final short G91		= 29;		// Incremental mode.
	public static final short G15   = 30;   // Polar coordinates off
	public static final short G16   = 31;   // Polar coordinates on
	public static final short G17   = 32;   // Choose plane for interpolation.
	public static final short G18   = 33;
	public static final short G19   = 34;
	public static final short G54   = 35;   // Work offsets.
	public static final short G55   = 36;
	public static final short G56   = 37;
	public static final short G57   = 38;
	public static final short G58   = 39;
	public static final short G59   = 40;
	public static final short G28   = 100; // BUG: Not implemented in translator?
	
	// This is used for G/M-codes that Ger doesn't know what to do with.
	// By default they pass through the entire translation process unchanged.
	// BUG: After some thought, I don't think this makes sense. The idea
	// was to allow certain codes that might make sense on a physical machine
	// pass through the entire translation unchanged. That's fine in theory,
	// but I need to be able to parse these thing to Statement objects, and 
	// if I don't know what they do, then that is hard. 
//	public static final short PASS = 500;
	
	
	// These are more complex statements that do require some additional data.
	public static final short M03		= 10000;	// spindle on, CW. Requires RPM in data.
	public static final short M04		= 10001;	// spindle on, CCW. Requires RPM in data.
	public static final short M06   = 10002;  // Tool change. Tool number in data
	public static final short M98		= 10003;	// Call subprogram.
	public static final short MOVE  = 10004;	// tool move, requires coordinates in data.
	public static final short IJK   = 10005;	// For circles, requires coordinates in data.
	public static final short LINE  = 10006;  // Line number. BUG: Used?
	public static final short PROG  = 10007;	// Program number -- an O-command like O1234
	public static final short G02   = 10008;  // CW circular interpolation.
	public static final short G03   = 10009;  // CCW circular interpolation.
	public static final short G41   = 10010; 	// Cutter comp left.
	public static final short G42		= 10011;	// Cutter comp right.
	public static final short G43   = 10012;	// TLO, positive.
	public static final short G44   = 10013;  // TLO, negative.
	public static final short G52   = 10014;  // Temporary change in PRZ.
	
	// A special case:
	public static final short WIZARD = 20000;
	
	// One of the constants above.
	public short type;
	
	// Line number on which statement occurred. Needed to report errors.
	// This is the line number with respect to carriage returns, not N-codes.
	public int lineNumber;
	
  // The character count, in the entire program, at which this statement
  // starts. We need this to return from subroutines.
  public int charNumber;
	
	// Typically, this is empty.
	public String error = null;
	
  // If this is true, then the error above is merely a warning
	// BUG: Is this actually used?
  public boolean warning = false;
	
	// This will often be null. For the first set of simple statements (the ones
	// that use a low number for the type), this will be null. It will be non-null 
	// for the more complex statements. The exact nature of the data depends on 
	// the statement.
	public StatementData	data = null;
	
	
	public Statement() {
	  // Do-nothing.
	  // BUG: Don't like this do-nothing. A Statement should require a type,
	  // so that only the other constructor makes sense.]
	  // Make this private or get rid of entirely.
	}
	
	public Statement(short type) {
		this.type = type;
	}
	
	public String toString() {
		
		// Convert the current statement to a String.
		String answer = null;
		
		switch (type)
			{
				case ERROR	:	return(String.format("N%05d\tERROR\t%s",lineNumber,error));
				case EOF		:	return(String.format("N%05d\tEOF",lineNumber));
				case G00		:	return(String.format("N%05d\tG00",lineNumber));
				case G01		: return(String.format("N%05d\tG01",lineNumber));
				case N			: return("");
				case M05		: return(String.format("N%05d\tM05",lineNumber));
				case M01		: return(String.format("N%05d\tM01",lineNumber));
				case M02		: return(String.format("N%05d\tM02",lineNumber));
				case M07		: return(String.format("N%05d\tM07",lineNumber));
				case M08		: return(String.format("N%05d\tM08",lineNumber));
				case M09		: return(String.format("N%05d\tM09",lineNumber));
				case M30		: return(String.format("N%05d\tM30",lineNumber));
				case M40		: return(String.format("N%05d\tM40",lineNumber));
				case M41		: return(String.format("N%05d\tM41",lineNumber));
				case M47		: return(String.format("N%05d\tM47",lineNumber));
				case M48		: return(String.format("N%05d\tM48",lineNumber));
				case M49		: return(String.format("N%05d\tM49",lineNumber));
				case M99		: return(String.format("N%05d\tM99",lineNumber));
				
				case G20		: return(String.format("N%05d\tG20",lineNumber));
				case G21		: return(String.format("N%05d\tG21",lineNumber));
				case G40		: return(String.format("N%05d\tG40",lineNumber));
				case G49		: return(String.format("N%05d\tG49",lineNumber));
				case G90		: return(String.format("N%05d\tG90",lineNumber));
				case G91		: return(String.format("N%05d\tG91",lineNumber));
				case G15		: return(String.format("N%05d\tG15",lineNumber));
				case G16		: return(String.format("N%05d\tG16",lineNumber));
				case G17		: return(String.format("N%05d\tG17",lineNumber));
				case G18		: return(String.format("N%05d\tG18",lineNumber));
				case G19		: return(String.format("N%05d\tG19",lineNumber));
				case G54		: return(String.format("N%05d\tG54",lineNumber));
				case G55		: return(String.format("N%05d\tG55",lineNumber));
				case G56		: return(String.format("N%05d\tG56",lineNumber));
				case G57		: return(String.format("N%05d\tG57",lineNumber));
				case G58		: return(String.format("N%05d\tG58",lineNumber));
				case G59		: return(String.format("N%05d\tG59",lineNumber));
				
				case M03		: answer = String.format("N%05d\tM03 ",lineNumber);
											DataInt state = (DataInt) data;
											answer += String.format("S%3d",state.value);
											return answer;
				case M04		: answer = String.format("N%05d\tGM04 ",lineNumber);
											state = (DataInt) data;
											answer += String.format("S%03d",state.value);
											return answer;
				case M06		: answer = String.format("N%05d\tGM06 ",lineNumber);
											state = (DataInt) data;
											answer += String.format("T%02d",state.value);
											return answer;
				case M98		: answer = String.format("N%05d\tM98 ",lineNumber);
											DataSubroutineCall call = (DataSubroutineCall) data;
											answer += String.format("P%05d ",call.programNumber);
											if (call.invocations > 1)
												answer += String.format("L%04d",call.invocations);
											return answer;
				case MOVE		: answer = String.format("N%05d\t",lineNumber);
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
				case IJK		: return String.format("N%05d\tIJK Weirdness",lineNumber);
				case LINE		: return("");
				case PROG		: answer = String.format("N%05d\t",lineNumber);
											DataSubProg ovalue = (DataSubProg) data;
											answer += String.format("O%05d",ovalue.progNumber);
											return answer;
				case G02		: answer = String.format("N%05d\tG02 ",lineNumber);
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
				case G03		: answer = String.format("N%05d\tG03 ",lineNumber);
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
				case G41		: answer = String.format("N%05d\tG41 ",lineNumber);
											DataRegister r = (DataRegister) data;
											if (r.D) answer += "D"; else answer += "H";
											answer += String.format("%02d",r.regValue);
											return answer;
				case G42		: answer = String.format("N%05d\tG42 ",lineNumber);
											r = (DataRegister) data;
											if (r.D) answer += "D"; else answer += "H";
											answer += String.format("%02d",r.regValue);
											return answer;
				case G43		: answer = String.format("N%05d\tG43 ",lineNumber);
											r = (DataRegister) data;
											if (r.D) answer += "D"; else answer += "H";
											answer += String.format("%02d",r.regValue);
											return answer;
				case G44		: answer = String.format("N%05d\tG44 ",lineNumber);
											r = (DataRegister) data;
											if (r.D) answer += "D"; else answer += "H";
											answer += String.format("%02d",r.regValue);
											return answer;
				case G52		: answer = String.format("N%05d\tG52 ",lineNumber);
											theMove = (DataMove) data;
											if (theMove.xDefined == true)
												answer += "X"+String.format("%+07.3f\t",theMove.xValue);
											if (theMove.yDefined == true)
												answer += "Y"+String.format("%+07.3f\t",theMove.yValue);
											if (theMove.zDefined == true)
												answer += "Z"+String.format("%+07.3f\t",theMove.xValue);
											return answer;
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
				                    answer += " unknown weirdness";
				                }
				              return answer;
				default     : answer = "Unknown Statement type";
			}
		
		return answer;
	}
}




