package ger.tpile.parse;

public class DataError extends StatementData {
  
  public String message;
  
  public DataError(String message) {
    this.message = message;
  }

  public DataError deepCopy() {
    
    DataError answer = new DataError(this.message);
    return answer;
  }
}
