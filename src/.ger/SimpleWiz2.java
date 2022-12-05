package wizard_test;

/*

To test on the fly compilation


*/

import java.util.ArrayList;

import vcnc.tpile.parse.Statement;
import vcnc.wizard.WizardBase;


public class SimpleWiz2 extends WizardBase {

  public SimpleWiz2() {
  
    System.out.println("inside wizard constructor");
  }
  
  public ArrayList<Statement> execute() {
    
    System.out.println("inside wizard2");
    
    ArrayList<Statement> answer = new ArrayList<>();
    
    answer.add(G01());
    answer.add(Move().X(1.0).F(10.0).close());
    
    
    return answer;
  }
}
