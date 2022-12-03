package vcnc.wizard;

import java.util.ArrayList;

import vcnc.tpile.parse.Statement;

/*

All wizard definitions must extend this base class.

*/

public class WizardBase {
  
  public ArrayList<Statement> execute() {
    
    // Override this...
    return new ArrayList<Statement>(0);
  }
  
  public static Statement G01() {
    
    Statement answer = new Statement();
    answer.type = Statement.G01;
    
    // BUG: What about line number and char number?
    // Maybe as arguments to constructor?
    return answer;
  }
  
  public static MoveMaker Move() {
    
    // This is a little weird, but seems more natural and easier for the user.
    // To use this from WizardBase.execute(), say
    // Statement moveStatement = Move().x(1.0).f(20.0).close();
    // or whatever. The point is that Move() (this method) returns a MoveMaker,
    // which can then be given the arguments, and close() must be called
    // to convert the MoveMaker object to a Statement.
    //
    // BUG: I am not convinced that this is the best way. I especially don't
    // like having to call close().
    return new MoveMaker();
  }
}
