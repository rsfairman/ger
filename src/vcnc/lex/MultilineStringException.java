package vcnc.lex;

/*

Used when a string runs beyond the EOL (or EOF).
*/


public class MultilineStringException extends Exception {
  
  public String msg = null;

  public MultilineStringException(String msg) {
    this.msg = msg;
  }
  
  public String toString() {
    return msg;
  }

}
