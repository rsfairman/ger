package vcnc.tpile.trash;

import vcnc.tpile.parse.DataCircular;
import vcnc.tpile.parse.DataInt;
import vcnc.tpile.parse.DataMove;
import vcnc.tpile.parse.DataRegister;

public class St01 extends St05 {

  

  public static final short WIZARD = 1001;

  public static final short M05   =  6;   // spindle off
  public static final short M00   =  7;   // Hard program stop
  public static final short M01   =  8;   // Optional program stop
  public static final short M02   =  9;   // Program end.
  public static final short M07   = 10;   // Coolant on.
  public static final short M08   = 11;   // Coolant on.
  public static final short M09   = 12;   // Coolant off.
  public static final short M30   = 13;   // End of program.
  public static final short M40   = 14;   // Spindle high
  public static final short M41   = 15;   // Spindle low
  public static final short M47   = 16;   // Repeat program
  public static final short M48   = 17;   // Enable feed & speed overrides.
  public static final short M49   = 18;   // Disable overrides.
  
  public static final short G40   = 23;   // Cancel cutter comp.
  public static final short G49   = 27;   // Cancel TLO.
  public static final short G90   = 28;   // Absolute mode.
  public static final short G91   = 29;   // Incremental mode.
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
//  public static final short PASS = 500;
  
  
  // These are more complex statements that do require some additional data.
  public static final short M03   = 10000;  // spindle on, CW. Requires RPM in data.
  public static final short M04   = 10001;  // spindle on, CCW. Requires RPM in data.
  public static final short M06   = 10002;  // Tool change. Tool number in data
  public static final short MOVE  = 10004;  // tool move, requires coordinates in data.
  public static final short IJK   = 10005;  // For circles, requires coordinates in data.
  public static final short LINE  = 10006;  // Line number. BUG: Used?
  public static final short G02   = 10008;  // CW circular interpolation.
  public static final short G03   = 10009;  // CCW circular interpolation.
  public static final short G41   = 10010;  // Cutter comp left.
  public static final short G42   = 10011;  // Cutter comp right.
  public static final short G43   = 10012;  // TLO, positive.
  public static final short G44   = 10013;  // TLO, negative.
  public static final short G52   = 10014;  // Temporary change in PRZ.


  public St01(short type) {
    super(type);
  }

  public String toString() {
    
    // Convert the current statement to a String.
    String answer = null;
    
    switch (type)
      {
        case M05    : return(String.format("N%05d\tM05",lineNumber));
        case M01    : return(String.format("N%05d\tM01",lineNumber));
        case M02    : return(String.format("N%05d\tM02",lineNumber));
        case M07    : return(String.format("N%05d\tM07",lineNumber));
        case M08    : return(String.format("N%05d\tM08",lineNumber));
        case M09    : return(String.format("N%05d\tM09",lineNumber));
        case M30    : return(String.format("N%05d\tM30",lineNumber));
        case M40    : return(String.format("N%05d\tM40",lineNumber));
        case M41    : return(String.format("N%05d\tM41",lineNumber));
        case M47    : return(String.format("N%05d\tM47",lineNumber));
        case M48    : return(String.format("N%05d\tM48",lineNumber));
        case M49    : return(String.format("N%05d\tM49",lineNumber));
        
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
        
        case M03    : answer = String.format("N%05d\tM03 ",lineNumber);
                      DataInt state = (DataInt) data;
                      answer += String.format("S%3d",state.value);
                      return answer;
        case M04    : answer = String.format("N%05d\tGM04 ",lineNumber);
                      state = (DataInt) data;
                      answer += String.format("S%03d",state.value);
                      return answer;
        case M06    : answer = String.format("N%05d\tGM06 ",lineNumber);
                      state = (DataInt) data;
                      answer += String.format("T%02d",state.value);
                      return answer;
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
        case IJK    : return String.format("N%05d\tIJK Weirdness",lineNumber);
        case LINE   : return("");
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
                      r = (DataRegister) data;
                      if (r.D) answer += "D"; else answer += "H";
                      answer += String.format("%02d",r.regValue);
                      return answer;
        case G44    : answer = String.format("N%05d\tG44 ",lineNumber);
                      r = (DataRegister) data;
                      if (r.D) answer += "D"; else answer += "H";
                      answer += String.format("%02d",r.regValue);
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
      }
    
    return super.toString();
  }
}
