package vcnc.tpile;

/*

The first layer above the Parser and Lexer. It removes any user-defined
functions (wizards) and any "directives" that occur before the O-number that
opens the program proper. These directives are for things like specifying
the entries in the work offsets table, the tool table or the billet dimensions.

*/

import java.util.Stack;
import java.util.ArrayList;
import java.util.Enumeration;

import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.net.URL;
import java.net.URLClassLoader;

import javax.tools.ToolProvider;
import javax.tools.JavaCompiler;

import vcnc.tpile.parse.Parser;
import vcnc.tpile.parse.Statement;
import vcnc.tpile.parse.StatementBuffer;
import vcnc.tpile.parse.SubProgState;
import vcnc.tpile.parse.SubroutineCall;
import vcnc.tpile.parse.Wizard;
import vcnc.wizard.WizardBase;


public class LayerPre {
  
  private Parser theParser = null;
  
  // Wizards may create an entire set of statements, and they are produced
  // on one step so must be held in a buffer to be handed out one
  // Statement at a time.
  private StatementBuffer statements = null;
  
  // Whether the parsing process has reached the O-command that starts the program.
  private boolean seenOCommand = false;
  
  
  public LayerPre(TextBuffer theText) throws Exception {
    
    this.theParser = new Parser(theText);
    
    this.statements = new StatementBuffer();
    statements.first = fillOneBuffer();
    statements.second = fillOneBuffer();
    
    
  }

  private String formError(int lineNumber,String msg) {
    
    System.err.println("Forming pre error");
    
    //System.err.println()
    new Exception().printStackTrace();
    
    return "Error on line " +lineNumber+ ": " +msg;
  }
  
  private Statement[] fillOneBuffer() {
    
    // Similar to Parser, but I don't know in advance what the buffer
    // size needs to be. If one of the source statements is a wizard
    // command, then it could expand to a huge number of Statements.
    ArrayList<Statement> buf = new ArrayList<>();
    
    int i = 0;
    while (i < StatementBuffer.BufferSize)
      {
        // Get a single statement from the parser and expanded it if
        // it's a wizard.
        Statement s = theParser.getStatement();
        
        // BUG: What if we get an EOF? Especially if there is no O-command.

        if (s.type == Statement.PROG)
          {
            this.seenOCommand = true;
            continue;
          }
        else if (s.type == Statement.WIZARD)
          {
            // Wizards don't expand to anything when they appear before
            // the O-commnand.
            if (this.seenOCommand == false)
              {
                
                // A machine setup wizard that doesn't expand.
                try {
                  doSetupWizard(s);
                } catch (Exception e) {
                  // An error in a setup wizard *does* generate a Statement.
                  Statement err = new Statement();
                  err.type = Statement.ERROR;
                  err.error = formError(s.lineNumber,e.getMessage());
                  buf.add(err);
                  ++i;
                }
              }
            else
              {
                // O-command is past. Must be a real wizard requiring 
                // expansion.
                ArrayList<Statement> exp = expandWizard(s);
                buf.addAll(exp);
                i += exp.size();
              }
          }
        else
          {
            buf.add(s);
            ++i;
          }
      }
    
    // Convert to an ordinary array.
    Statement[] answer = (Statement[]) buf.toArray(new Statement[0]);
    return answer;
  }
  
  private void doSetupWizard(Statement w) throws Exception {
    
    // For the wizards that may appear before the O-command that starts the
    // program. These are to set up the machine state -- things like the
    // work offsets table.
    Wizard wiz = (Wizard) w.data;
    if (wiz.cmd.equals("SetUnits"))
      {
        if (wiz.args.size() != 1)
          throw new Exception("SetUnits has the wrong number of arguments");
        
        Object arg = wiz.args.get(0);
        
        if (arg instanceof String == false)
          throw new Exception("SetUnits takes 'inch' or 'mm' as argument");  
        
        String choice = (String) arg;
        
        if (choice.equals("inch"))
          MachineState.machineInchUnits = true;
        else
          MachineState.machineInchUnits = false;
      }
    else
      throw new Exception("Unknown machine setup command: " +wiz.cmd);
  }
  
  private ArrayList<Statement> expandWizard(Statement w) {
    
    // Convert the given wizard statement to a series of straight G-code
    // Statements.
    Wizard wiz = (Wizard) w.data;
    
    System.out.println("listing classes");
    
    //Enumeration<URL> roots = (new ClassLoader()).getResources("*");
//    Enumeration<URL> roots = null;
//    try {
//      roots = ClassLoader.getSystemClassLoader().getResources("");
//    } catch (IOException e) {
//      System.err.println("expandWizard err: " +e);
//    }
//    
////    System.out.println("size of roots: " +roots.size())
//    
//    for ( ; roots.hasMoreElements();)
//      {
//        System.out.println(roots.nextElement());
//      }
    
      //Class<WizardBase> theWizard = Class.forName("wizard_test." +wiz.cmd);
    try {
      
      Class<?> loaded = Class.forName("wizard_test." +wiz.cmd);
      
      WizardBase theWizard = (WizardBase) loaded.newInstance();
      
      return theWizard.execute();
      
      
    } catch (ClassNotFoundException e) {
      System.err.println("Not found: " +e);
    } catch (IllegalAccessException e) {
      System.err.println("Bad access: " +e);
    } catch (InstantiationException e) {
      System.err.println("Bad instance: " +e);
    }
    
//    URL url = ClassLoader.getSystemClassLoader().getResource(wiz.cmd);
//    System.out.println("found: " +url);
    
    
    
    return new ArrayList<Statement>(0);
    
    /*
    BUG: Don't bother doing this for now. Assume that the wizard is part
    of the usual body of code, compiled normally.
    // Check whether this wizard is defined
    
    // Compile if needed
    
    
    
    
    // Taken from https://blog.frankel.ch/compilation-java-code-on-the-fly/
    
    String srcpath = "C:\\Users\\rsf\\Documents\\WorkArea\\vcnc\\src\\wizard_test";
    
    srcpath += "\\SimpleWiz.java";
    
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    Path javaFile = Paths.get(srcpath);
    compiler.run(null, null, null, javaFile.toFile().getAbsolutePath());
    Path cpath = javaFile.getParent().resolve("SimpleWiz.class");
    
    try {
      
      URL classURL = cpath.getParent().toFile().toURI().toURL();
      URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] {classURL});
      Class<?> clazz = Class.forName("SimpleWiz",true,classLoader);
      clazz.newInstance();
      
    } catch (Exception e) {
      System.err.println("Bad failure");
      return null;
    }
    // BUG: NOT done.
    return null;
    */
    
    
    
    
  }
  
  Statement nextStatement() {
    
    // Pull off the next Statement from the buffer.
    Statement answer = statements.first[statements.index];
    ++statements.index;
    
    if (statements.index >= statements.first.length)
      {
        // Refresh.
        statements.first = statements.second;
        statements.second = fillOneBuffer();
        statements.index = 0;
      }
    
    return answer;
  }

  public void reset() {
    
    theParser.reset();
  }

}
