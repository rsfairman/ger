package ger.tpile.parse;

/*

After a G41 or G42, there should be a D or H register. 
This indicates the register.

*/

// BUG: This really doesn't belong in this package since it isn't used
// until Layer05. Maybe none of these StateData classes belong here.
// Not sure.


public class DataRegister extends StatementData {

  // If this is true, then it's a D-register; otherwise, it's an H-register.
  public boolean D = true;
    
  // The register.
  public int regValue = -1;


  public String toString() {
    
    String answer = new String();
    
    if (this.D == true)
      answer += "D";
    else
      answer += "H";
    
    answer += String.format("%02d",regValue);
    
    return answer;
  }
  
  public DataRegister deepCopy() {
    
    DataRegister answer = new DataRegister();
    answer.D = this.D;
    answer.regValue = this.regValue;
    return answer;
  }
}
