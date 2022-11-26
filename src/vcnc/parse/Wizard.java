package vcnc.parse;

// The wizard command and any arguments.

import java.util.ArrayList;


public class Wizard extends StateData {

  public String cmd = null;
  
  // The arguments (if any) to the wizard. Each entry should be one of
  // String or Double.
  public ArrayList<Object> args = new ArrayList<Object>();
  
  
}
