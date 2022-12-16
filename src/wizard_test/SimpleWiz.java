package wizard_test;

/*

A trivial wizard to test the framework.


*/

import java.util.ArrayList;

import vcnc.wizard.WizardBase;


public class SimpleWiz extends WizardBase {
  
  public void definition(ArrayList<Object> args) {
    
//    System.out.println("inside wizard");
    
    G01();
    Move().X(3.0).F(40.0);
    
    G00();
    Move().Y(2.0).Z(5.0);
    
    
    
  }
}
