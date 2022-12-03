package vcnc.tpile.lex;

/*

Used when a string runs beyond the EOL (or to EOF).

*/


class MultilineStringException extends Exception {
  
  public String msg = null;

  public MultilineStringException(String msg) {
    this.msg = msg;
  }
  
  public String toString() {
    return msg;
  }

}
