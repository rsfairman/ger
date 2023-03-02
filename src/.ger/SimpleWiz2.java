package wizard_test;

/*

To test on the fly compilation


*/

import java.util.ArrayList;

import ger.wizard.WizardBase;


public class SimpleWiz2 extends WizardBase {

  public SimpleWiz2() {
  
    System.out.println("inside wizard constructor");
  }
  
  public void definition(ArrayList<Object> args) {
    
    System.out.println("inside wizard2");
    

    
    G01();
    Move().X(3.0).F(40.0);
    
    G00();
    Move().Y(2.0).Z(5.0);
    
    
  }
}
