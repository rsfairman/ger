package vcnc.transpile;

/*

Another layer to the interpreter.

Layer00 handles subroutine calls and returns. This removes M98 and M99 from
  the input stream, along with a bunch of things that I don't handle
  or that don't mean anything in a virtual context, like coolant on/off.
Layer01 converts all values to the standard units of the machine, inches or mm.
  G20 and G21 disappear from the statement stream. Also checks that 
  G02/03 are given correctly (e.g., no K-value when in G17/XY-plane mode).

This layer eliminates TLO and work offsets. 

I can totally eliminate these commands from the stream. In fact, that's about 
the only thing that makes sense, and it's also why you aren't allowed to apply
these while in incremental mode. For instance, if you are in incremental mode, 
have done some cutting, and the tool is currently in the material, what does 
it mean to enter one of these other frames of reference? Where does the next 
move take you? So, we must be in absolute mode when these commands are applied.
Then there must be at least one absolute move (preferably above the material)
to bring these changes into full effect. So...we do eliminate these commands
from the statement stream.

This class does need to keep track of whether we are in polar coordinate 
mode and G17/18/19 so that the angle value is not changed,
and because polar coordinate mode is something like incremental mode.
Likewise, it needs to keep track of incremental/absolute mode so that 
incremental coordinates are not modified. I do keep track of whether we are in 
cutter comp mode, but only because I want to disallow changing TLO and work 
offsets while using cutter comp.

The G-codes involved here are esoteric, and it's easy to forget what they do.
The three codes related to tool length compensation are G43 (positive 
compensation), G44 (negative compensation) and G49 (cancel compensation). The 
codes related to work offsets are G52, G54-G59

What G43, G44 and G49 do...
First, the user must have stored a set of H-values somewhere (so-called 
registers). For the discussion below, assume that all of these values are 
positive to make is clear what's going on in terms of tool motion. If you say
G43 H7
then the tool will be moved away from the material by the value of the H7 
register. G44 does the opposite: the tool will move closer to the material by 
H7. G49 cancels either choice. Some operators measure the distance from the 
bottom of the tool to the project, with the spindle at some set position, and 
use that as the H value. See Smid, p. 141, in particular: "For example, if 
H06 has been measured as 7.639, it will be entered as a negative value if 
G43 is used." From his picture, it's clear that the tool is to move downward 
to the material in this example. So, G43 is called "positive" compensation 
because the H-value is added to the target Z position; thus, to bring the 
tool lower, the H-value must be negative.
 
What G52, G54-G59 do...
These are what you could call local coordinate systems. In other words,
they are a way of changing the origin. The machine defaults to using G54,
so it can be thought of as the way to cancel G54-G59. In each case, the values
associated with the G-value are added to the positions in the program. So, if
the X-value for G55 is 3.000, and that coordinate system is in use, then
G01 X2.000
will move the tool to a position 5.000 = 3.000 + 2.000 to the right of
PRZ_x. In the case of G55-G59, these X, Y and Z values comes from a table that's
set offline. For G52, you supply the values as part of the program.
 

What's left for higher layers:
* Eliminating incremental coordinates and making everything absolute.
* Convert all polar coordinate moves to linear moves. This needs to be after
  the elimination of incremental moves so that we know where the cuts begin.
  One way to deal with the conversion would be to enter incremental mode,
  convert the polar moves to incremental line moves, then run the previous step
  again...That seems awfully indirect though. I should be able to do this
  in a single step.
* Translate cutter comp coordinates. This really should be last.



*/

import vcnc.parse.Statement;
import vcnc.parse.MoveState;
import vcnc.parse.Circular;
import vcnc.parse.TLOState;



public class Layer02 {
  
  private Layer01 lowerLayer = null;
  
  public Layer02(TextBuffer theText) throws Exception {
    
    lowerLayer = new Layer01(theText);
    
  }
  
  public String formError(int lineNumber,String msg) {
    
    return "Error on line " +lineNumber+ ": " +msg;
  }
  
  
  
  
  

  /*

Layer02::Layer02(QString theCode,bool inch,const ToolTurret *turret,const WorkOffsets *offsets) 
       throw(QString) : lowerLayer(theCode,inch)
{
  this->offsets = offsets;
  this->turret = turret;
  this->axis = XY;
  this->absolute = true;
  this->polar = false;
  this->cutterComp = false;
  this->TLO = false;
  this->tloValue = 0.0;
  this->offsetX = 0.0;
  this->offsetY = 0.0;
  this->offsetZ = 0.0;
  
//  this->m06TLOCancel = false;
//  this->m06WOCancel = false;
//  this->m06IncrementalCancel = false;
//  this->m06CCCancel = false;
//  this->m06PCCancel = false;
//  
//  // Set up some fixed Statement objects that are needed when dealing with M06.
//  STLOCancel.type = Statement::G49;
//  SWOCancel.type = Statement::G54;
//  SIncrementalCancel.type = Statement::G90;
//  SCCCancel.type = Statement::G40;
//  SPCCancel.type = Statement::G15;
// 
}
*/

  private void changeToError(Statement cmd,String msg) {
    
    // Change the given statement to an ERROR with the given error msg.
    // BUG: It seems likely that this would be useful in other classes too.
    cmd.type = Statement.ERROR;
    cmd.error = formError(cmd.lineNumber,msg);
  }

  private void handleLine(Statement cmd) {
    
    // Translate the line by any offset.
    if (MachineState.absolute == false)
      // Don't change these when in incremental mode.
      // It doesn't make sense since incremental moves are *relative*.
      return;
    
    MoveState theMove = (MoveState) cmd.data;
    if (MachineState.usingPolar == false)
      {
        // Ordinary line.
        if (theMove.xDefined == true)
          theMove.xValue += MachineState.offsetX;
        if (theMove.yDefined == true)
          theMove.yValue += MachineState.offsetY;
        if (theMove.zDefined == true)
          theMove.zValue += MachineState.offsetZ;
      }
    else
      {
        // In polar coordinates. The plane chosen (G17/18/19) determines which
        // of the variables need to be changed. We don't change the angle, 
        // which is in degrees. The radius coordinate should not be changed 
        // either. Only the "extra" coordinate is modified.
        switch (MachineState.curAxis)
          {
            case XY : // X = radius, Y = angle.
                      if (theMove.zDefined == true)
                        theMove.zValue += MachineState.offsetZ;
                      break;
            case ZX : // Z = radius, X = angle.
                      if (theMove.yDefined == true)
                        theMove.yValue += MachineState.offsetY;
                      break;
            case YZ : // Y = radius, Z = angle.
                      if (theMove.xDefined == true)
                        theMove.xValue += MachineState.offsetX;
                      break;
          }
      }
    
    // In all cases, we need to adjust the Z-coordinate for any TLO.
    if (MachineState.TLO == true)
      {
        if (theMove.zDefined == true)
          theMove.zValue += MachineState.tloValue;
      }
  }

  private void handleArc(Statement cmd) {
    
    // Translate an arc by any offset. 
    
    // Not allowed in polar mode.
    if (MachineState.usingPolar == true)
      {
        changeToError(cmd,
            "circular interpolation not allowed with polar coordinates");
        return;
      }
    
    if (MachineState.absolute == false)
      // Nothing to do in incremental mode.
      return;
    
    Circular theMove = (Circular) cmd.data;
    
    // Note that we are not adjusting I/J/K. These are relative values,
    // so they need no adjustment.
    if (theMove.xDefined == true)
      theMove.X += MachineState.offsetX;
    if (theMove.yDefined == true)
      theMove.Y += MachineState.offsetY;
    if (theMove.zDefined == true)
      {
        theMove.Z += MachineState.offsetZ;
        if (MachineState.TLO == true)
          theMove.Z += MachineState.tloValue;
      }
  }

  private void handleTLO(Statement cmd) {
    
    // This handles G43, G44 and G49. The value of TLO and tloValue are updated.
    // TLO statements may also have an optional Z-move, in which case the
    // given cmd is transformed into that move, which occurs after the TLO
    // offset is in place. Also, if there is an error, then the given cmd
    // is transformed into an SError statement.
    if (MachineState.absolute == false)
      {
        changeToError(cmd,"cannot change TLO while in incremental mode");
        return;
      }
    if (MachineState.cutterComp == true)
      {
        changeToError(cmd,"cannot change TLO while using cutter comp");
        return;
      }
    if (MachineState.usingPolar == true)
      {
        changeToError(cmd,"cannot change TLO while using polar coordinates");
        return;
      }
    
    if (cmd.type == Statement.G49)
      {
        // Cancel TLO.
        MachineState.TLO = false;
        MachineState.tloValue = 0.0;
        return;
      }
        
    // Entering TLO mode. Pull out the value from the tool turret table.
    
    // Make sure that we're not already in TLO mode.
    if (MachineState.TLO == true)
      {
        changeToError(cmd,
          "cancel previous tool length offset before giving a new one");
        return;
      }
    
    // Get the register value.
    TLOState reg = (TLOState) cmd.data;
    double h = 0.0;
    if (reg.hRegister > ToolTurret.TableSize)
      {
        changeToError(cmd,"there are only 20 tools in the turret");
        return;
      }
      
    // From the H register.
    h = ToolTurret.hRegister[reg.hRegister - 1];
    MachineState.TLO = true;
    
    if (cmd.type == Statement.G43)
      MachineState.tloValue = h;
    else
      MachineState.tloValue = -h;
    
    // Now see if there was an optional Z-move as part of the statement.
    if (reg.hasZ == true)
      {
        // Yes. Convert cmd to this Z-move, then adjust the given coordinate
        // to take new TLO into account.
        double newZ = reg.zValue;
        
        cmd.type = Statement.MOVE;
        MoveState data = new MoveState();
        cmd.data = data;
        
        data.zDefined = true;
        data.zValue = newZ;
        
        // Let the existing code make the adjustment.
        handleLine(cmd);
      }
  }

  private void handleWorkOffsets(Statement cmd) {
    
    // Could be G54...G59 or it could be a G52, which is slightly different.
    
    // Don't allow this to happen when in polar coordinate, incremental mode, or
    // when cutter comp is on.
    if (MachineState.usingPolar == true)
      {
        changeToError(cmd,
          "changing reference frame not allowed in polar coordinate mode");
        return;
      }
    if (MachineState.absolute == false)
      {
        changeToError(cmd,
                  "changing reference frame not allowed in incremental mode");
        return;
      }
    if (MachineState.cutterComp == true)
      {
        changeToError(cmd,
          "changing reference frame not allowed while using cutter comp");
        return;
      }
      
    if (cmd.type == Statement.G52)
      {
        // Kind of a special case. The offset is given with the command rather
        // than pulled out of the work offset table.
        MoveState move = (MoveState) cmd.data;
        
        MachineState.offsetX = move.xValue;
        MachineState.offsetY = move.yValue;
        MachineState.offsetZ = move.zValue;
        
        return;
      }
    
    // The normal case of G54,...,G59. Just copy the values from the work offset
    // table.
    switch (cmd.type)
      {
        case Statement.G54 : MachineState.offsetX = 0.0;
                             MachineState.offsetY = 0.0;
                             MachineState.offsetZ = 0.0; 
                             break;
        case Statement.G55 : MachineState.offsetX = WorkOffsets.g55[0];
                             MachineState.offsetY = WorkOffsets.g55[1];
                             MachineState.offsetZ = WorkOffsets.g55[2];
                             break;
        case Statement.G56 : MachineState.offsetX = WorkOffsets.g56[0];
                             MachineState.offsetY = WorkOffsets.g56[1];
                             MachineState.offsetZ = WorkOffsets.g56[2];
                             break;
        case Statement.G57 : MachineState.offsetX = WorkOffsets.g57[0];
                             MachineState.offsetY = WorkOffsets.g57[1];
                             MachineState.offsetZ = WorkOffsets.g57[2];
                             break;
        case Statement.G58 : MachineState.offsetX = WorkOffsets.g58[0];
                             MachineState.offsetY = WorkOffsets.g58[1];
                             MachineState.offsetZ = WorkOffsets.g58[2];
                             break;
        case Statement.G59 : MachineState.offsetX = WorkOffsets.g59[0];
                             MachineState.offsetY = WorkOffsets.g59[1];
                             MachineState.offsetZ = WorkOffsets.g59[2];
                             break;
      }
  }

  public Statement nextStatement() throws Exception {
    
    // Read and translate statements to be passed up to the next layer.
    
    
    // First, make sure that there aren't any special cancel commands
    // that must be sent due to a previous M06.
  //  if (m06TLOCancel == true) { m06TLOCancel = false; return &STLOCancel; }
  //  if (m06WOCancel == true) { m06WOCancel = false; return &SWOCancel; }
  //  if (m06IncrementalCancel == true) { m06IncrementalCancel = false; return &SIncrementalCancel; }
  //  if (m06CCCancel == true) { m06CCCancel = false; return &SCCCancel; }
  //  if (m06PCCancel == true) { m06PCCancel = false; return &SPCCancel; }
  //  if (m06Final == true) { m06Final = false; return m06WaitCmd; }
    
    
    Statement answer = null;
    Statement cmd = lowerLayer.nextStatement();
    
    // Don't return until some Statement have been found.
    while (answer == null)
      {
        switch (cmd.type)
          {
            case Statement.ERROR : // These simply propagate upward.
                                   return cmd;
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
            case Statement.G15 : MachineState.usingPolar = false;
                                 answer = cmd;
                                 break;
            case Statement.G16 : MachineState.usingPolar = true;
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
            case Statement.G40 : MachineState.cutterComp = false;
                                 answer = cmd;
                                 break;
            case Statement.G41 : 
            case Statement.G42 : MachineState.cutterComp = true;
                                 answer = cmd;
                                 break;
            case Statement.G43 :
            case Statement.G44 : 
            case Statement.G49 : 
              // Note the change to the internal state due to TLO.
              // If there was an optional Z-move, then cmd is transformed to 
              // that MOVE. The cmd could also be transformed to an ERROR.
              // If neither of these transformations is done, then we throw 
              // away the statement.
              handleTLO(cmd);
              if (cmd.type == Statement.ERROR)
                return cmd;
              if (cmd.type == Statement.MOVE)
                return cmd;
              break;
            case Statement.G52 : 
            case Statement.G54 : 
            case Statement.G55 : 
            case Statement.G56 : 
            case Statement.G57 : 
            case Statement.G58 : 
            case Statement.G59 : // Like TLO.
                                  handleWorkOffsets(cmd);
                                  if (cmd.type == Statement.ERROR)
                                    return cmd;
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
                                    
  //                                  // Set the various flags for anything that needs to
  //                                  // be canceled before the M06.
  //                                  if (TLO == true)
  //                                    m06TLOCancel = true;
  //                                  if ((offsetX != 0.0) || (offsetY != 0.0) || (offsetZ != 0.0))
  //                                    m06WOCancel = true;
  //                                  if (absolute == false)
  //                                    m06IncrementalCancel = true;
  //                                  if (cutterComp == true)
  //                                    m06CCCancel = true;
  //                                  if (polar == true)
  //                                    m06PCCancel = true;
  //                                  
  //                                  // The all belong to the same line.
  //                                  STLOCancel.lineNumber = cmd->lineNumber;
  //                                  SWOCancel.lineNumber = cmd->lineNumber;
  //                                  SIncrementalCancel.lineNumber = cmd->lineNumber;
  //                                  SCCCancel.lineNumber = cmd->lineNumber;
  //                                  SPCCancel.lineNumber = cmd->lineNumber;
  //                                  
  //                                  // I suppose that I need this too for returning from sub-programs.
  //                                  STLOCancel.charNumber = cmd->charNumber;
  //                                  SWOCancel.charNumber = cmd->charNumber;
  //                                  SIncrementalCancel.charNumber = cmd->charNumber;
  //                                  SCCCancel.charNumber = cmd->charNumber;
  //                                  SPCCancel.charNumber = cmd->charNumber;
  //                                  
  //                                  // Note the cmd just read and the fact that it still
  //                                  // needs to be sent out.
  //                                  m06Final = true;
  //                                  m06WaitCmd = cmd;
  //                                  return NULL;
                                    
            default : // Not sure how this is possible...
              changeToError(cmd,
                "unknown command fell through to layer 02");
              return cmd;
          }
        
        if (answer == null)
          // Not done. Look at next statement.
          cmd = lowerLayer.nextStatement();
      }
    
    return answer;
  }

  public void reset() {
    
    // This may not be the best approach, but we are resetting only the
    // globals that are specific to this layer; e.g., the axis is reset
    // by Layer01, so it's not done here.
    MachineState.absolute = true;
    MachineState.cutterComp = false;
    MachineState.TLO = false;
    MachineState.tloValue = 0.0;
    MachineState.offsetX = 0.0;
    MachineState.offsetY = 0.0;
    MachineState.offsetZ = 0.0;
    
    lowerLayer.reset();
  }


}






