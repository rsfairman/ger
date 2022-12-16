package vcnc.tpile;

import vcnc.Statement;
import vcnc.tpile.parse.DataCircular;
import vcnc.tpile.parse.DataMove;


public class Layer04 {
  
  private Layer03 lowerLayer = null;


  // Tool position, in absolute coordinates.
  // BUG: As in Layer03, I suspect that this could be make part of
  // MachineState.
  private double machX;
  private double machY;
  private double machZ;
  
  // Needed if we reset.
  // BUG: Again, isn't this going to be (0,0,0)?
  private double X0;
  private double Y0;
  private double Z0;
  
  
  public Layer04(CodeBuffer theText,double X0,double Y0,double Z0)
      throws Exception {
    ;
    this.lowerLayer = new Layer03(theText,X0,Y0,Z0);
    
    this.machX = X0;
    this.machY = Y0;
    this.machZ = Z0;
    
    this.X0 = X0;
    this.Y0 = Y0;
    this.Z0 = Z0;
    
    MachineState.absolute = true;
    MachineState.curAxis = AxisChoice.XY;
  }
  
  public String formError(int lineNumber,String msg) {
    
    return "Error on line " +lineNumber+ ": " +msg;
  }
  
  private void changeToError(Statement cmd,String msg) {
    
    // Change the given statement to an ERROR with the given error msg.
    // BUG: It seems likely that this would be useful in other classes too.
    cmd.type = Statement.ERROR;
    cmd.error = formError(cmd.lineNumber,msg);
  }
  
  private void handleLine(Statement cmd) {
    
    // Convert from incremental to absolute coordinates.
    DataMove theMove = (DataMove) cmd.data;
    
    if (MachineState.absolute == true)
      {
        // Note where the tool ends up in case we enter incremental mode soon.
        // No changes needed to the move.
        if (theMove.xDefined == true)
          machX = theMove.xValue;
        if (theMove.yDefined == true)
          machY = theMove.yValue;
        if (theMove.zDefined == true)
          machZ = theMove.zValue;
        
        return;
      }
    
    // In incremental mode. Modify cmd and note where the tool ends up.
    if (theMove.xDefined == true)
      {
        theMove.xValue += machX;
        machX = theMove.xValue;
      }
    if (theMove.yDefined == true)
      {
        theMove.yValue += machY;
        machY = theMove.yValue;
      }
    if (theMove.zDefined == true)
      {
        theMove.zValue += machZ;
        machZ = theMove.zValue;
      }
  }

  private boolean checkArcErr(Statement cmd) {
    
    // Check whether the arc makes geometric sense. Return true if there is 
    // an error; false otherwise.
    
    // Use the ArcCurve class to do this. If the constructor accepts the given
    // cmd, then it must be OK.
    try {
      ArcCurve testArc = new ArcCurve(machX,machY,machZ,
          MachineState.curAxis,cmd);
    } catch (Exception e) {
      cmd.type = Statement.ERROR;
      cmd.error = formError(cmd.lineNumber,e.getMessage());
      return true;
    }
    
    // Got here, so no error, and nothing to do.
    return false;
  }

  private void handleArc(Statement cmd) {
    
    // Convert from incremental to absolute coordinates.
    DataCircular theMove = (DataCircular) cmd.data;
    
    if (MachineState.absolute == true)
      {
        // Not much to do in incremental mode.
        // Make sure that the arc is geometrically correct first.
        boolean hasError = checkArcErr(cmd);
        
        // Note where the tool ends up in case we enter incremental mode soon.
        if (theMove.xDefined == true)
          machX = theMove.X;
        if (theMove.yDefined == true)
          machY = theMove.Y;
        if (theMove.zDefined == true)
          machZ = theMove.Z;
        return;
      }
    
    // In incremental mode. Modify cmd.
    if (theMove.xDefined == true)
      theMove.X += machX;
    if (theMove.yDefined == true)
      theMove.Y += machY;
    if (theMove.zDefined == true)
      theMove.Z += machZ;
    
    // Check the arc for geometric errors. If there are none, note where the tool ended up.
    if (checkArcErr(cmd) == false)
      { 
        if (theMove.xDefined == true)
          machX = theMove.X;
        if (theMove.yDefined == true)
          machY = theMove.Y;
        if (theMove.zDefined == true)
          machZ = theMove.Z;
      }
  }

  public Statement nextStatement() throws Exception {
      
    // Read and translate statements to be passed up to the next layer.
    Statement answer = null;
    Statement cmd = lowerLayer.nextStatement();
    
    // Don't return until some Statement have been found.
    while (answer == null)
      {
        switch (cmd.type)
          {
            case Statement.ERROR : // These simply propagate upward.
                                   answer = cmd;
            case Statement.EOF   : return cmd;
            case Statement.MOVE  : handleLine(cmd);
                                   answer = cmd;
                                   break;
            case Statement.G00 : 
            case Statement.G01 : answer = cmd;
                                 break;
            case Statement.G02 : 
            case Statement.G03 : handleArc(cmd);
                                 answer = cmd;
                                 break;
            case Statement.G17 : MachineState.curAxis = AxisChoice.XY;
                                 answer = cmd;
                                 break;
            case Statement.G18 : MachineState.curAxis = AxisChoice.ZX;
                                 answer = cmd;
                                 break;
            case Statement.G19 : MachineState.curAxis = AxisChoice.YZ;
                                 answer = cmd;
                                 break;
            case Statement.G40 : 
            case Statement.G41 : 
            case Statement.G42 : answer = cmd;
                                 break;
            case Statement.G90 : // Note that G90 and G91 are removed from
                                 // the statement stream.
                                 MachineState.absolute = true;
                                 break;
            case Statement.G91 : MachineState.absolute = false;
                                 break;
            case Statement.M03   : 
            case Statement.M04   : 
            case Statement.M05   : 
            case Statement.M06   : answer = cmd;
                                   break;
            default : // Shouldn't be possible...
              changeToError(cmd,
                       "unknown command fell through to layer 04");
              answer = cmd;
              break;
          }
        
        if (answer == null)
          // Not done. Look at next Statement.
          cmd = lowerLayer.nextStatement();
      }
    
    return answer;
  }

  public void reset() {
    
    machX = X0;
    machY = Y0;
    machZ = Z0;
    
    MachineState.absolute = true;
    
    lowerLayer.reset();
  }

}



