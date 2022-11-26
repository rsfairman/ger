package vcnc.lex;

/*

Used when a floating-point number is expect while parsing, and you get 
something else.

*/


public class ExpectedFloatNumberException extends Exception {
  
  public String msg = null;

  public ExpectedFloatNumberException(String msg) {
    this.msg = msg;
  }
  
  public String toString() {
    return msg;
  }

}
