package vcnc.wizard;

/*

Used by WizardBase to form move commands.

Up through v09, indicating a move in a wizard went something like

Move().X(3.0).F(40.0).close();

where Move() creates a MoveMaker, and the close() was to "seal" it, indicating
that all of the X/Y/Z/F values intended have been specified. As of v10, the
close() is no longer needed.

*/


import java.util.ArrayList;

import vcnc.tpile.Statement;

import vcnc.tpile.parse.DataMove;


public class MoveMaker {
  
  // This move is going to be added to this longer list of G-codes as
  // part of a wizard implementation.
  private DataMove theMove = null;
  
  
  public MoveMaker(ArrayList<Statement> wizout) {
    
    // Insert a place-holder move into wizout, and each time one of X/Y/Z/F
    // is changed, this move is updated.
    Statement theCode = new Statement(Statement.MOVE);
    
    this.theMove = new DataMove();

    theMove.xDefined = false;
    theMove.yDefined = false;
    theMove.zDefined = false;
    theMove.fDefined = false;
    
    theCode.data = theMove;
    
    wizout.add(theCode);
  }
  
  public MoveMaker X(double value) {
    
    theMove.xDefined = true;
    theMove.xValue = value;
    return this;
  }
  
  public MoveMaker Y(double value) {
    
    theMove.yDefined = true;
    theMove.yValue = value;
    return this;
  }
  
  public MoveMaker Z(double value) {
    
    theMove.zDefined = true;
    theMove.zValue = value;
    return this;
  }
  
  public MoveMaker F(double value) {
    
    theMove.fDefined = true;
    theMove.fValue = value;
    return this;
  }
  
}



