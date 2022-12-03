package wizard_test;

/*

BUG: Test the wizard framework. For now, these wizards are a normal part
of the compilation process, but I want the user to be able to drop .java
files in a directory so that they can be run.

I could do this by requiring that the user run javac to create a class file --
and that is not such a bad solution -- or I could try to compile these on
the fly. One advantage of having the user run javac is that the errors are
seen and can be debugged in the usual way.

The crucial thing throughout is that the names of these wizards can't be
known in advance. They need to be loaded by name.

See https://blog.frankel.ch/compilation-java-code-on-the-fly/
and I did something like this for the FigPut code. See MainWindow.compileJava()
and .compileStuff() of v17. I dropped Java after that.



*/

import java.util.ArrayList;

import vcnc.tpile.parse.Statement;
import vcnc.wizard.WizardBase;


public class SimpleWiz extends WizardBase {

  public SimpleWiz() {
  
    System.out.println("inside wizard constructor");
  }
  
  public ArrayList<Statement> execute() {
    
    System.out.println("inside wizard");
    
    ArrayList<Statement> answer = new ArrayList<>();
    
    answer.add(G01());
    answer.add(Move().X(3.0).F(40.0).close());
    
    
    return answer;
  }
}
