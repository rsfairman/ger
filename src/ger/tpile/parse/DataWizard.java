package ger.tpile.parse;

// The wizard command and any arguments.

import java.util.ArrayList;


public class DataWizard extends StatementData {

  public String cmd = null;
  
  // The arguments (if any) to the wizard. Each entry should be one of
  // String or Double.
  public ArrayList<Object> args = new ArrayList<Object>();
  
  
  public DataWizard deepCopy() {
    
    DataWizard answer = new DataWizard();
    answer.cmd = this.cmd;
    
    // NOTE: I could probably skip doing a deep copy here, and just reuse
    // the Object references, but go ahead. 
    answer.args = new ArrayList<>();
    for (Object o : this.args)
      {
        if (o instanceof Double)
          answer.args.add(Double.valueOf((Double) o));
        else if (o instanceof String)
          answer.args.add(new String((String)o));
        else
          System.err.println("Error in DataWizard.deepCopy()");
      }
    
    return answer;
  }
}
