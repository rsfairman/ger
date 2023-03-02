package ger.tpile;

import static ger.tpile.SType.*;

import ger.tpile.parse.DataCircular;
import ger.tpile.parse.DataError;
import ger.tpile.parse.DataInt;
import ger.tpile.parse.DataMove;
import ger.tpile.parse.DataRegister;
import ger.tpile.parse.DataSubProg;
import ger.tpile.parse.DataSubroutineCall;
import ger.tpile.parse.DataTLO;
import ger.tpile.parse.DataWizard;
import ger.tpile.parse.StatementData;


public class Statement {
  
  // Which G/M-code we're dealing with.
  public SType type;
  
  // Line number on which statement occurred. Needed to report errors.
  // This is the line number with respect to carriage returns, not N-codes.
  public int lineNumber;
  
  // For many simple statements, this will be null. The exact nature 
  // of the data (and sub-type of StatementData) depends on the statement.
  // If the type is ERROR, then the error message appears here, in a 
  // DataError object.
  public StatementData  data = null;

  
  public Statement(SType type) {
    
    // Although the type is set, this does not fill in any required data.
    this.type = type;
  }
  
  public Statement deepCopy() {
    
    // A deep copy is needed when shuffling Statements for subprograms.
    Statement answer = new Statement(this.type);
    answer.lineNumber = this.lineNumber;
    if (this.data != null)
      answer.data = this.data.deepCopy();
    
    return answer;
  }
  
  public void makeError(String msg) {
    
    // Change the current statement to have ERROR type with the given message.
    type = ERROR;
    DataError e = new DataError("Error on line " +lineNumber+ ": " +msg);
    data = e;
  }
  
  public String toString() {
    
    // Convert the current statement to a String.
    String answer = null;
    
    switch (type)
      {
        case ERROR  : DataError e = (DataError) data;
                      return  e.message;
        case EOF    : return "EOF";
        
        case M00    : return "M00";
        case M01    : return "M01";
        case M03    : DataInt state = (DataInt) data;
                      return String.format("M03 S%d",state.value);
        case M04    : state = (DataInt) data;
                      return String.format("M04 S%d",state.value);
        case M05    : return "M05";
        case M06    : state = (DataInt) data;
                      return String.format("M06 T%02d",state.value);
        case M07    : return "M07";
        case M08    : return "M08";
        case M09    : return "M09";
        case M30    : return "M30";
        case M40    : return "M40";
        case M41    : return "M41";
        case M48    : return "M48";
        case M49    : return "M49";
        case M98    : DataSubroutineCall call = (DataSubroutineCall) data;
                      answer = String.format("M98 P%d",call.programNumber);
                      if (call.invocations > 1)
                        answer += String.format(" L%d",call.invocations);
                      return answer;
        case M99    : return "M99";
        
        case G00    : return "G00";
        case G01    : return "G01";

        
        case G40    : return "G40";
        case G49    : return "G49";
        case G90    : return "G90";
        case G91    : return "G91";
        case G15    : return "G15";
        case G16    : return "G16";
        case G17    : return "G17";
        case G18    : return "G18";
        case G19    : return "G19";
        case G54    : return "G54";
        case G55    : return "G55";
        case G56    : return "G56";
        case G57    : return "G57";
        case G58    : return "G58";
        case G59    : return "G59";
        
        case MOVE   : answer = "";
                      DataMove theMove = (DataMove) data;
                      if (theMove.xDefined == true)
                        answer += String.format("X%.4f ",theMove.xValue);
                      if (theMove.yDefined == true)
                        answer += String.format("Y%.4f ",theMove.yValue);
                      if (theMove.zDefined == true)
                        answer += String.format("Z%.4f ",theMove.zValue);
                      if (theMove.fDefined == true)
                        answer += String.format("F%.2f",theMove.fValue);
                      return answer;
        case G02    : answer = String.format("G02");
                      DataCircular circ = (DataCircular) data;
                      if (circ.xDefined)
                        answer += String.format(" X%.4f",circ.X);
                      if (circ.yDefined)
                        answer += String.format(" Y%.4f",circ.Y);
                      if (circ.zDefined)
                        answer += String.format(" Z%.4f",circ.Z);
                      if (circ.rDefined)
                        answer += String.format(" R%.4f",circ.R);
                      if (circ.iDefined)
                        answer += String.format(" I%.4f",circ.I);
                      if (circ.jDefined)
                        answer += String.format(" J%.4f",circ.J);
                      if (circ.kDefined)
                        answer += String.format(" K%.4f",circ.K);
                      if (circ.F > 0)
                        answer += String.format(" F%.2f ",circ.F);
                      return answer;
        case G03    : answer = String.format("G03");
                      circ = (DataCircular) data;
                      if (circ.xDefined)
                        answer += String.format(" X%.4f",circ.X);
                      if (circ.yDefined)
                        answer += String.format(" Y%.4f",circ.Y);
                      if (circ.zDefined)
                        answer += String.format(" Z%.4f",circ.Z);
                      if (circ.rDefined)
                        answer += String.format(" R%.4f",circ.R);
                      if (circ.iDefined)
                        answer += String.format(" I%.4f",circ.I);
                      if (circ.jDefined)
                        answer += String.format(" J%.4f",circ.J);
                      if (circ.kDefined)
                        answer += String.format(" K%.4f",circ.K);
                      if (circ.F > 0)
                        answer += String.format(" F%.2f ",circ.F);
                      return answer;
        case G41    : answer = "G41 ";
                      DataRegister r = (DataRegister) data;
                      if (r.D) answer += "D"; else answer += "H";
                      answer += String.format("%02d",r.regValue);
                      return answer;
        case G42    : answer = "G42 ";
                      r = (DataRegister) data;
                      if (r.D) answer += "D"; else answer += "H";
                      answer += String.format("%02d",r.regValue);
                      return answer;
        case G43    : answer = "G43";
                      DataTLO tlo = (DataTLO) data;
                      if (tlo.hasZ == true) 
                        answer += String.format(" Z%.4f",tlo.zValue);
                      answer += String.format(" H%02d",tlo.hRegister);
                      return answer;
        case G44    : answer = "G44 ";
                      tlo = (DataTLO) data;
                      if (tlo.hasZ == true) 
                        answer += String.format(" Z%.4f",tlo.zValue);
                      answer += String.format(" H%02d",tlo.hRegister);
                      return answer;
        case G52    : answer = "G52";
                      theMove = (DataMove) data;
                      if (theMove.xDefined == true)
                        answer += String.format(" X%.4f",theMove.xValue);
                      if (theMove.yDefined == true)
                        answer += String.format(" Y%.4f",theMove.yValue);
                      if (theMove.zDefined == true)
                        answer += String.format(" Z%.4f",theMove.xValue);
                      return answer;

        case G20    : return "G20";
        case G21    : return "G20";
        
        case PROG   : DataSubProg ovalue = (DataSubProg) data;
                      return String.format("O%05d",ovalue.progNumber);
        
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
