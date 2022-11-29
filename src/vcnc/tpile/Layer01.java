package vcnc.tpile;

import vcnc.tpile.parse.Circular;
import vcnc.tpile.parse.MoveState;
import vcnc.tpile.parse.Statement;


public class Layer01 {

  private Layer00 lowerLayer = null;
 
  
  public Layer01(TextBuffer theText) throws Exception {
    
    this.lowerLayer = new Layer00(theText);
  }
  
  public String formError(int lineNumber,String msg) {
    
    return "Error on line " +lineNumber+ ": " +msg;
  }

  private double toNatural(double u) {
    
    // Convert the given coordinate value to the coordinate that's natural to 
    // the machine. E.g., if this is a metric machine, but we are currently in
    // inch mode, then u inches must be converted to millimeters.
    
    if (MachineState.machineInchUnits == MachineState.curInch)
      // Nothing to do. 
      return u;
    
    if (MachineState.machineInchUnits == true)
      // This is an inch machine, but u is in millimeters.
      return u / 25.40;
    
    // Else, this is a metric machine, but u is in inches.
    return u * 25.40;
  }

  private boolean checkArcErr(Statement cmd) {
    
    // Check whether there are any inconsistencies in the given Statement.
    // Return true if there is an error; false otherwise.
    
    // Not allowed in polar mode.
    if (MachineState.usingPolar == true)
      {
        cmd.type = Statement.ERROR;
        cmd.error = formError(cmd.lineNumber,
          "circular interpolation not allowed with polar coordinates");
        return true;
      }
    
    // Make sure that the arc specification is consistent with the choice
    // of plane. For instance, you can't use a K-value with G17.
    Circular theMove = (Circular) cmd.data;
    if (MachineState.curAxis == AxisChoice.XY)
      {
        if (theMove.kDefined == true)
          {
            cmd.type = Statement.ERROR;
            cmd.error = formError(cmd.lineNumber,
                "may not use K in the XY-plane (G17)");
            return true;
          }
      }
    else if (MachineState.curAxis == AxisChoice.ZX)
      {
        if (theMove.jDefined == true)
          {
            cmd.type = Statement.ERROR;
            cmd.error = formError(cmd.lineNumber,
                "may not use J in the ZX-plane (G18)");
            return true;
          }
      }
    else
      {
        // Must by the YZ-plane.
        if (theMove.iDefined == true)
          {
            cmd.type = Statement.ERROR;
            cmd.error = formError(cmd.lineNumber,
                "may not use I in the YZ-plane (G19)");
            return true;
          }
      }
    
    // No errors.
    return false;
  }

  private void handleArc(Statement cmd) {
    
    // Adjust arcs to be in the standard units: inches or mm.
    
    boolean hasErr = checkArcErr(cmd);
    if (hasErr == true)
      return;
    
    // Got here, so no errors.
    if (MachineState.machineInchUnits == MachineState.curInch)
      // Nothing to do.
      return;
    
    Circular theMove = (Circular) cmd.data;
    if (theMove.xDefined == true)
      theMove.X = toNatural(theMove.X);
    if (theMove.yDefined == true)
      theMove.Y = toNatural(theMove.Y);
    if (theMove.zDefined == true)
      theMove.Z = toNatural(theMove.Z);
  
    if (theMove.rDefined == true)
      theMove.R = toNatural(theMove.R);
    
    if (theMove.iDefined == true)
      theMove.I = toNatural(theMove.I);
    if (theMove.jDefined == true)
      theMove.J = toNatural(theMove.J);
    if (theMove.kDefined == true)
      theMove.K = toNatural(theMove.K);
    
    if (theMove.F > 0.0)
      theMove.F = toNatural(theMove.F);
  }

  private void handleLine(Statement cmd) {
    
    // Convert to standard units. The only tricky thing here is polar coordinate 
    // mode.
    if (MachineState.machineInchUnits == MachineState.curInch)
      // Nothing to do.
      return;
    
    MoveState theMove = (MoveState) cmd.data;
    if (MachineState.usingPolar == false)
      {
        if (theMove.xDefined == true)
          theMove.xValue = toNatural(theMove.xValue);
        if (theMove.yDefined == true)
          theMove.yValue = toNatural(theMove.yValue);
        if (theMove.zDefined == true)
          theMove.zValue = toNatural(theMove.zValue);
        if (theMove.fDefined == true)
          theMove.fValue = toNatural(theMove.fValue);
      }
    else
      {
        // In polar coordinates. The plane chosen (G17/18/19) determines which
        // of the variables need to be changed. We don't change the angle, 
        // which is in degrees.
        switch (MachineState.curAxis)
          {
            case XY : // X = radius, Y = angle.
                      if (theMove.xDefined == true)
                        theMove.xValue = toNatural(theMove.xValue);
                      if (theMove.zDefined == true)
                        theMove.zValue = toNatural(theMove.zValue);
                      break;
            case ZX : // Z = radius, X = angle.
                      if (theMove.yDefined == true)
                        theMove.yValue = toNatural(theMove.yValue);
                      if (theMove.zDefined == true)
                        theMove.zValue = toNatural(theMove.zValue);
                      break;
            case YZ : // Y = radius, Z = angle.
                      if (theMove.xDefined == true)
                        theMove.xValue = toNatural(theMove.xValue);
                      if (theMove.yDefined == true)
                        theMove.yValue = toNatural(theMove.yValue);
                      break;
          }
      }
  }

  public Statement nextStatement() throws Exception {
    
    // Read and translate statements to be passed up to the next layer.
  
    Statement cmd = lowerLayer.nextStatement();
  
    // Don't return until some Statement have been found.
    while (true)
      {
        switch (cmd.type)
          {
            case Statement.ERROR : 
              // Don't throw these; they simply propagate upward.
              return cmd;
            case Statement.EOF  : return cmd;
            case Statement.MOVE : handleLine(cmd);
                                  return cmd;
            case Statement.G00 : 
            case Statement.G01 : return cmd;
            case Statement.G02 : 
            case Statement.G03 : handleArc(cmd);
                                  return cmd;
            case Statement.G15 : MachineState.usingPolar = false;
                                  return cmd;
            case Statement.G16 : MachineState.usingPolar = true;
                                  return cmd;
            case Statement.G17 : MachineState.curAxis = AxisChoice.XY;
                                  return cmd;
            case Statement.G18 : MachineState.curAxis = AxisChoice.ZX;
                                  return cmd;
            case Statement.G19 : MachineState.curAxis = AxisChoice.YZ;
                                  return cmd;
            case Statement.G20 : MachineState.curInch = true;
                                  break;
            case Statement.G21 : MachineState.curInch = false;
                                  break;
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
            case Statement.M03 : 
            case Statement.M04 : 
            case Statement.M05 : 
            case Statement.M06 : return cmd;
            
            default : // Not sure how this could happen...
              cmd.type = Statement.ERROR;
              cmd.error = formError(cmd.lineNumber,
                "unknown command fell through to layer 01");
              return cmd;
          }
        
        // Not done. Look at next statement.
        cmd = lowerLayer.nextStatement();
      }
  }
  
  public void reset() {
    
    MachineState.curInch = MachineState.machineInchUnits;
    MachineState.curAxis = AxisChoice.XY;
    MachineState.usingPolar = false;
    
    lowerLayer.reset();
  }

}
