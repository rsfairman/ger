package vcnc.tpile.lex;

/*

Used when a whole number is expect while parsing, and you get something else.

*/


class ExpectedWholeNumberException extends Exception {
  
  public String msg = null;

  public ExpectedWholeNumberException(String msg) {
    this.msg = msg;
  }
  
  public String toString() {
    return msg;
  }
}
