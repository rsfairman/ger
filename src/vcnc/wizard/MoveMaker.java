package vcnc.wizard;

import vcnc.tpile.parse.DataMove;
import vcnc.tpile.parse.Statement;


public class MoveMaker {

  public boolean xDefined = false;
  public boolean yDefined = false;
  public boolean zDefined = false;
  public boolean fDefined = false;
  
  public double xValue = 0.0;
  public double yValue = 0.0;
  public double zValue = 0.0;
  public double fValue = 0.0;

  public MoveMaker X(double value) {
    
    this.xDefined = true;
    this.xValue = value;
    return this;
  }
  
  public MoveMaker Y(double value) {
    
    this.yDefined = true;
    this.yValue = value;
    return this;
  }
  
  public MoveMaker Z(double value) {
    
    this.zDefined = true;
    this.zValue = value;
    return this;
  }
  
  public MoveMaker F(double value) {
    
    this.fDefined = true;
    this.fValue = value;
    return this;
  }
  
  public Statement close() {
    
    // This must be called last to "seal" the move and convert it to a 
    // Statement.
    Statement answer = new Statement();
    answer.type = Statement.MOVE;
    
    DataMove m = new DataMove();
    
    m.xDefined = this.xDefined;
    m.yDefined = this.yDefined;
    m.zDefined = this.zDefined;
    m.fDefined = this.fDefined;
    m.xValue = this.xValue;
    m.yValue = this.yValue;
    m.zValue = this.zValue;
    m.fValue = this.fValue;
    
    answer.data = m;
    
    return answer;
  }
}
