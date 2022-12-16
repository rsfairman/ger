package vcnc.tpile;

import vcnc.Statement;
import vcnc.tpile.parse.DataCircular;
import vcnc.tpile.parse.DataMove;


public class Layer03 {
  
  private Layer02 lowerLayer = null;

  // For every tool move, we note where the tool ended up. This is needed when
  // we enter polar coordinate mode.
  private double machX = 0.0;
  private double machY = 0.0;
  private double machZ = 0.0;

  // Using polar coordinates (G16) requires that we know where the tool was at at the
  // time we entered polar coordinates. For every tool move, we note where
  // the tool ended up.
  private double pOriginX = 0.0;
  private double pOriginY = 0.0;
  private double pOriginZ = 0.0;
//
//  // We need to keep track of recent polar coordinates positions too.
//  private double oldR = 0.0;
//  private double oldAngle = 0.0;
  
  // Needed if we reset.
  private double X0;
  private double Y0;
  private double Z0;
  
  
  
  public Layer03(CodeBuffer theText,double X0,double Y0,double Z0) 
      throws Exception {
    ;
    // X0, Y0, Z0 is the intial postion of the tool.
    // BUG: Shouldn't this always be (0,0,0)? 
    // BUG: Also, the use of this.machX/Y/Z seems redundant. Isn't this just
    // the tool position and could be in MachineState?
    
    lowerLayer = new Layer02(theText);
    
    this.machX = X0;
    this.machY = Y0;
    this.machZ = Z0;
    this.pOriginX = X0;
    this.pOriginY = X0;
    this.pOriginZ = X0;
    
    this.X0 = X0;
    this.Y0 = Y0;
    this.Z0 = Z0;
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
  
  private boolean handleLine(Statement cmd) {
    
    // Update the machX/Y/Z values, and also check whether the 
    // move really does move the tool. What we want to avoid are
    // things like this
    // G01 X1 Y1
    // X1 Y1
    // which don't move the tool. A "line" that's really a degenerate
    // point could lead to problems with cutter comp.
    // 
    // If we are in polar coordinate mode, then the meaning of the coordinates
    // that make up a move is different.
    DataMove theMove = (DataMove) cmd.data;
    
    if (MachineState.usingPolar == false)
      {
        // No changes to the cmd are needed, but we do need to note
        // the position of the tool at the end of the line just in case the
        // program goes into polar coordinates at the next line.
        if (MachineState.absolute == true)
          {
            // First, check that the tool really does move.
            boolean valid = false;
            if ((theMove.xDefined == true) && (theMove.xValue != machX))
              valid = true;
            else if ((theMove.yDefined == true) && (theMove.yValue != machY))
              valid = true;
            else if ((theMove.zDefined == true) && (theMove.zValue != machZ))
              valid = true;
              
            if (valid == false)
              // Don't bother. This move does nothing and should be filtered out.
              return false;
            
            if (theMove.xDefined == true)
              machX = theMove.xValue;
            if (theMove.yDefined == true)
              machY = theMove.yValue;
            if (theMove.zDefined == true)
              machZ = theMove.zValue;
          }
        else
          {
            // Relative move. Check whether at least one coordinate is non-zero.
            boolean valid = false;
            if ((theMove.xDefined == true) && (theMove.xValue != 0.0))
              valid = true;
            else if ((theMove.yDefined == true) && (theMove.yValue != 0.0))
              valid = true;
            else if ((theMove.zDefined == true) && (theMove.zValue != 0.0))
              valid = true;
            if (valid == false)
              return false;
            
            if (theMove.xDefined == true)
              machX += theMove.xValue;
            if (theMove.yDefined == true)
              machY += theMove.yValue;
            if (theMove.zDefined == true)
              machZ += theMove.zValue;
          }
        return true;
      }
   
    // A linear move in polar coordinates. The motion of the tool *is* linear,
    // but the meaning of the coordinate values is different. The machine
    // may currently be in incremental or absolute mode, and the form in
    // which the move is expressed depends on that. 
    if (MachineState.absolute == true)
      {
        // Slightly easier case of absolute mode.
        if (MachineState.curAxis == AxisChoice.XY)
          {
            // X = radius, Y = angle. 
            // There must be an X value; otherwise, there is no move.
            // You *are* allowed to have a radius of zero, but it must
            // be explicitly given. A zero radius just moves the tool
            // to pOrigin.
            if (theMove.xDefined == false)
              {
                changeToError(cmd,"no radius (X) given");
                return true;
              }
            
            double radius = theMove.xValue;
            
            // It's OK if no angle is given; it defaults to zero.
            double angle = 0.0;
            if (theMove.yDefined == true)
              angle = theMove.yValue;
            
            // Convert to radians.
            angle = angle * Math.PI / 180.0;
            
            double x = pOriginX + radius * Math.cos(angle);
            double y = pOriginY + radius * Math.sin(angle);
            
            // Change the underling MoveState to reflect this.
            // The Z value is left alone (as it should be).
            theMove.xDefined = true;
            theMove.xValue = x;
            theMove.yDefined = true;
            theMove.yValue = y;
            
            // Update the absolute position of the tool.
            // Here is also where we check whether the tool actually moved.
            boolean valid = false;
            if ((x != machX) || (y != machY))
              valid = true;
            if (valid == false)
              return false;
            
            machX = x;
            machY = y;
            if (theMove.zDefined == true)
              machZ = theMove.zValue;
            
            return true;
          }
        
        if (MachineState.curAxis == AxisChoice.ZX)
          {
            // As above... See Smid, p. 280 for a picture that make it
            // clear how to treat the two axes (which gets cosine and
            // which gets sine). Note that, as he shows things, the
            // way the ZX plane is handled may vary. I implement what
            // he calls "standard," as opposed to "vertical machining
            // center."
            
            // Z = radius, X = angle.
            if (theMove.zDefined == false)
              {
                changeToError(cmd,
                    "no radius (Z) given -- you are in G18 mode (ZX-plane)");
                return true;
              }
            
            double radius = theMove.zValue;
            
            double angle = 0.0;
            if (theMove.xDefined == true)
              angle = theMove.xValue;
            
            angle = angle * Math.PI / 180.0;
            
            double z = pOriginZ + radius * Math.cos(angle);
            double x = pOriginX + radius * Math.sin(angle);
            
            theMove.zDefined = true;
            theMove.zValue = z;
            theMove.xDefined = true;
            theMove.xValue = x;
            
            boolean valid = false;
            if ((x != machX) || (z != machZ))
              valid = true;
            if (valid == false)
              return false;
            
            machX = x;
            if (theMove.yDefined == true)
              machY = theMove.yValue;
            machZ = z;
            
            return true;
          }
        
        // Got here, so must be in YZ mode.
        // Y = radius, Z = angle.
        if (theMove.yDefined == false)
          {
            changeToError(cmd,
                      "no radius (Y) given -- you are in G19 mode (YZ-plane)");
            return true;
          }
        
        double radius = theMove.yValue;
        
        double angle = 0.0;
        if (theMove.zDefined == true)
          angle = theMove.zValue;
        
        angle = angle * Math.PI / 180.0;
        
        double y = pOriginY + radius * Math.cos(angle);
        double z = pOriginZ + radius * Math.sin(angle);
        
        boolean valid = false;
        if ((y != machY) || (z != machZ))
          valid = true;
        if (valid == false)
          return false;
        
        theMove.yDefined = true;
        theMove.yValue = y;
        theMove.zDefined = true;
        theMove.zValue = z;
        
        if (theMove.xDefined == true)
          machX = theMove.xValue;
        machY = y;
        machZ = z;
        
        return true;
      }
    
    // Got here, so the move is being made while in incremental mode.
    // This is similar to the above, but the pOrigin changes after
    // each move. Also, because we are in incremental mode, the next
    // higher layer will interpret the move as an incremental move,
    // and it must be given in those terms.
    // We don't have to check whether the tool actually moves in this
    // case. The tool will move if the radius is non-zero, and if the radius
    // is zero, that's an automatic error.
    if (MachineState.curAxis == AxisChoice.XY)
      {
        // X = radius, Y = angle. 
        if (theMove.xDefined == false)
          {
            changeToError(cmd,"no radius (X) given");
            return true;
          }
        
        double radius = theMove.xValue;
        
        double angle = 0.0;
        if (theMove.yDefined == true)
          angle = theMove.yValue;
        
        angle = angle * Math.PI / 180.0;
        
        // Here is where being in incremental mode matters.
        // The MoveState should be expressed in relative terms,
        // thus the pOrigin values are not included.
        double x = radius * Math.cos(angle);
        double y = radius * Math.sin(angle);
        
        // Change the underling MoveState to reflect this.
        // The Z value is left alone (as it should be).
        theMove.xDefined = true;
        theMove.xValue = x;
        theMove.yDefined = true;
        theMove.yValue = y;
        
        // We need to note the position of the tool for the next move.
        // If we continued to the end of the program in incremental
        // mode, we would not need this, but as soon as we go back
        // to absolute mode, we will.
        machX += x;
        machY += y;
        if (theMove.zDefined == true)
          machZ += theMove.zValue;
        
        pOriginX = machX;
        pOriginY = machY;
        pOriginZ = machZ;
        
        return true;
      }
    
    if (MachineState.curAxis == AxisChoice.ZX)
      {
        // Z = radius, X = angle. 
        if (theMove.zDefined == false)
          {
            changeToError(cmd,
                     "no radius (Z) given -- you are in G18 mode (ZX-plane)");
            return true;
          }
        
        double radius = theMove.zValue;
        
        double angle = 0.0;
        if (theMove.xDefined == true)
          angle = theMove.xValue;
        
        angle = angle * Math.PI / 180.0;
        
        double z = radius * Math.cos(angle);
        double x = radius * Math.sin(angle);
        
        theMove.zDefined = true;
        theMove.zValue = z;
        theMove.xDefined = true;
        theMove.xValue = x;
        
        machX += x;
        if (theMove.yDefined == true)
          machY += theMove.yValue;
        machZ += z;
        
        pOriginX = machX;
        pOriginY = machY;
        pOriginZ = machZ;
        
        return true;
      }
    
    // Got here, so must be the YZ plane.
    // Y = radius, Z = angle.
    if (theMove.yDefined == false)
      {
        changeToError(cmd,
                  "no radius (Y) given -- you are in G19 mode (YZ-plane)");
        return true;
      }
    
    double radius = theMove.yValue;
    
    double angle = 0.0;
    if (theMove.zDefined == true)
      angle = theMove.zValue;
    
    angle = angle * Math.PI / 180.0;
    
    double y = radius * Math.cos(angle);
    double z = radius * Math.sin(angle);
    
    theMove.yDefined = true;
    theMove.yValue = y;
    theMove.zDefined = true;
    theMove.zValue = z;
    
    if (theMove.xDefined == true)
      machX += theMove.xValue;
    machY += y;
    machZ += z;
    
    pOriginX = machX;
    pOriginY = machY;
    pOriginZ = machZ;
    
    return true;
  }

  private void noteArcPos(Statement cmd) {
    
    // Note the final position of the tool after an arc move. As with 
    // handleLine(), this is needed in case the program goes into polar 
    // coordinates at the next line. It's simpler because, first, no change
    // to the cmd is made, and second, I don't have to worry about different 
    // planes. That's becuase G02/03 work by specifying the final position of 
    // the tool, the position at the end of the arc, and that's unaffected by
    // which plane we are in (G17/18/19).
    //
    // Note that I am allowing G02/03 even when in polar coordinate mode.
    // Most real machines may not allow this.
    DataCircular theMove = (DataCircular) cmd.data;
    
    // If none of X,Y,Z are defined, then it's a complete circle.
    // Not every real machine allows this.
    if ((theMove.xDefined == false) && (theMove.yDefined == false) 
        && (theMove.zDefined == false))
      // Tool returns to where it started.
      return;
    
    if (MachineState.absolute == true)
      {
        if (theMove.xDefined == true)
          machX = theMove.X;
        if (theMove.yDefined == true)
          machY = theMove.Y;
        if (theMove.zDefined == true)
          machZ = theMove.Z;
      }
    else
      {
        // Incremental mode.
        if (theMove.xDefined == true)
          machX += theMove.X;
        if (theMove.yDefined == true)
          machY += theMove.Y;
        if (theMove.zDefined == true)
          machZ += theMove.Z;
      }
  }

  private boolean handlePolar(Statement cmd) {
    
    // Handle turning on/off polar coordinate mode.
    // Return false if there's an error (calling G16 twice).
    if (cmd.type == Statement.G15)
      // Turn it off. G15 is OK even if it's already off.
      MachineState.usingPolar = false;
    else
      {
        // Turn it on with G16
        if (MachineState.usingPolar == true)
          {
            // Change cmd to an error.
            changeToError(cmd,"G16 called already");
            return false;
          }
        
        MachineState.usingPolar = true;
        
        // Note the tool position when we entered polar coordinate mode.
        this.pOriginX = this.machX;
        this.pOriginY = this.machY;
        this.pOriginZ = this.machZ;
      }
    
    return true;
  }

  public Statement nextStatement() throws Exception {
    
    // Read and translate statements to be passed up to the next layer.
    Statement answer = null;
    Statement cmd = lowerLayer.nextStatement();
    if (cmd == null)
      // Must be an M06 in the works (look at what Layer02 does when it sees
      // and M06). So get another statement.
      cmd = lowerLayer.nextStatement();
    
    // Don't return until some Statement have been found.
    while (answer == null)
      {
        switch (cmd.type)
          {
            case Statement.ERROR : // These simply propagate upward.
                                   return cmd;
            case Statement.EOF   : return cmd;
            case Statement.MOVE :
              // This is a little messy since there are two special cases here.
              // Normally, what this does is convert moves while in PC mode to 
              // moves expressed in non-PC terms. However, it may be necessary 
              // to remove a move from the stream entirely (when the move does 
              // nothing). In that case, handleLine() returns false. Also, 
              // there could be an error in the MOVE, like if the radius was 
              // omitted. In that case, handleLine() will return true as well, 
              // but the contents of cmd will have been changed to an ERROR.
              // No special action is required in that case; the cmd is passed 
              // on as usual.
              if (handleLine(cmd) == true)
                answer = cmd;
              break;
            case Statement.G00 : 
            case Statement.G01 : 
              // These two are not allowed while in PC mode.
              if (MachineState.usingPolar == true)
                {
                  changeToError(cmd,
                      "G00/G01 not allowed in polar coordinate mode");
                  return cmd;
                }
              answer = cmd;
                    break;
            case Statement.G02 : 
            case Statement.G03 : answer = cmd;
                                 noteArcPos(cmd);
                                 break;
            case Statement.G15 : 
            case Statement.G16 : // Note that this eats G15/G16. They disappear 
                                 // from the stream (unless there's an error).
                                  if (handlePolar(cmd) == false)
                                    return cmd;
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
            case Statement.G90 : MachineState.absolute = true;
                                 answer = cmd;
                                 break;
            case Statement.G91 : MachineState.absolute = false;
                                 answer = cmd;
                                 break;
            case Statement.M03   : 
            case Statement.M04   : 
            case Statement.M05   : 
            case Statement.M06   : answer = cmd;
                                   break;
            default : // Not sure how this is possible, but...
              changeToError(cmd,
                "unknown command fell through to layer 03");
          }
        
        if (answer == null)
          // Not done. Look at next statement.
          cmd = lowerLayer.nextStatement();
      }
    
    return answer;
  }
  
  public void reset() {
    
    machX = X0;
    machY = Y0;
    machZ = Z0;
    
    pOriginX = X0;
    pOriginY = X0;
    pOriginZ = X0;
    
    lowerLayer.reset();
  }

}



