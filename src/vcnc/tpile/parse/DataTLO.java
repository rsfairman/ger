package vcnc.tpile.parse;

public class DataTLO extends StatementData {
  
  public int hRegister = -1;
  public boolean hasZ = false;
  public double zValue = 0.0;
  
  
  public String toString() {

    String answer = new String("");
    
    if (hasZ == true)
      answer = String.format("Z%+07.3f ",zValue);
    
    answer = answer + String.format("H%d",hRegister);
    return answer;
  }

  public DataTLO deepCopy() {
    
    DataTLO answer = new DataTLO();
    answer.hRegister = this.hRegister;
    answer.hasZ = this.hasZ;
    answer.zValue = this.zValue;
    return answer;
  }
}
