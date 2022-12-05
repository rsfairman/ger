package vcnc.tpile;

/*

Comes after Layer0A, which removes (and acts on) any "machine directives."
This removes any user-defined functions (wizards), all of which must appear
after the O-number that opens the program proper. 

There are two problems to be solved here. The easier problem is converting
a wizard statement to G-code. The harder one is compiling the wizard code
from .java to .class and loading it.

Ideally, the user should be able to drop a .java file into the .ger directory
and have it compile automatically, but that requires too much UI infrastructure
to be reasonable as the normal course of events. Instead, compilation should 
be done through the CLI -- although it is attempted here too.

*/

import java.util.Stack;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Arrays;

import java.io.File;
import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;

import javax.tools.ToolProvider;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import vcnc.tpile.parse.Statement;
import vcnc.tpile.parse.StatementBuffer;
import vcnc.WizCompile;
import vcnc.persist.Persist;
import vcnc.tpile.parse.DataWizard;
import vcnc.wizard.WizardBase;


public class Layer0B {
  
  // The layer prior to this one.
  private Layer0A lowerLayer = null;
  
  // Wizards may create an entire set of statements, and they are produced
  // from a single incoming Statement, so the output must be held in a buffer 
  // to be handed out one Statement at a time to the next layer.
  private StatementBuffer statements = null;
  
  // Wizards must appear after the O-statement. In theory, this requirement
  // could be relaxed, but being strict about this should make the distinction
  // between wizards and directives clearer to the user.
  private boolean seenOCommand = false; 
  
  
  Layer0B(CodeBuffer theText) throws Exception {
    
    this.lowerLayer = new Layer0A(theText);
    
    this.statements = new StatementBuffer();
    statements.first = fillOneBuffer();
    statements.second = fillOneBuffer();
  }

  private String formError(int lineNumber,String msg) {
    
    return "Error on line " +lineNumber+ ": " +msg;
  }
  
  private Statement formError(Statement s,String msg) {
    
    // BUG: This is a much better way to do things.
    // See I can get rid of the "other" formError() in all classes that do this.
    Statement answer = new Statement(Statement.ERROR);
    answer.lineNumber = s.lineNumber;
    answer.charNumber = s.charNumber;
    answer.error = formError(s.lineNumber,msg);
    return answer;
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
        Statement s = lowerLayer.nextStatement();
        
        // BUG: What if we get an EOF? Especially if there is no O-command.

        if (s.type == Statement.PROG)
          {
            this.seenOCommand = true;
            buf.add(s);
            ++i;
            continue;
          }
        else if (s.type == Statement.WIZARD)
          { 
            if (this.seenOCommand == false)
              {
                // Machine directives (that occur before the O-statement) have
                // been filtered out by the previous layer of the translator.
                // So any wizard before the O-statement is in error.
                DataWizard wizData = (DataWizard) s.data;
                Statement err = 
                    formError(s,"Unknown machine directive: " +wizData.cmd);
                buf.add(err);
                ++i;
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
            // Some ordinary Statement.
            buf.add(s);
            ++i;
          }
      }
    
    // Convert to an ordinary array.
    Statement[] answer = (Statement[]) buf.toArray(new Statement[0]);
    return answer;
  }

  private WizardBase classLoadable(String cName) {
    
    // Attempt to load the given class. If that's possible, then load it and
    // return it. Otherwise, return null.
    //
    // BUG: the package name 'wizard_test' is hard-coded.
    // BUG: Change method name to attemptLoad().
    
    // Try loading "normally," without messing with the class loader.
    // This should work if the wizard class is "built in" and was compiled 
    // as part of the original ger framework.
    try {
      // Package name, 'wizard_test', followed by class name.
      Class<?> loaded = Class.forName("wizard_test." +cName);
      
      WizardBase answer = (WizardBase) loaded.newInstance();
      return answer;
      
    } catch (ClassNotFoundException e) {
      // BUG: To stderr for debugging.
      System.err.println("Not found: " +e);
    } catch (IllegalAccessException e) {
      System.err.println("Bad access: " +e);
    } catch (InstantiationException e) {
      System.err.println("Bad instance: " +e);
    }
    
    // Not a normal "built in" wizard. See if it's available as a class
    // file in the .ger directory.
    try {
      Path p = Paths.get(Persist.getGerLocation());
      URL url = p.toUri().toURL();
      
      System.out.println("url: " +url);
      
      URLClassLoader loader = new URLClassLoader(new URL[] {url});
      Class<?> loaded = Class.forName("wizard_test." +cName,true,loader);
      
      WizardBase answer = (WizardBase) loaded.newInstance();
      return answer;
      
    } catch (MalformedURLException e) {
      // BUG: THis one really shouldn't happen. The gerDir must be a valid
      // directory, hence a valid URL.
      System.err.println("Malformed URL: " +Persist.getGerLocation());
    } catch (ClassNotFoundException e) {
      // BUG: To stderr for debugging.
      System.err.println("Not found: " +e);
    } catch (IllegalAccessException e) {
      System.err.println("Bad access: " +e);
    } catch (InstantiationException e) {
      System.err.println("Bad instance: " +e);
    }
    
    // Got here, so the class is unknown.
    return null;
  }
  
  private WizardBase compileAndLoad(String cName) {
    
    // Although this is called *compile* and load, compiling here should
    // be unusual since that should have been done earlier (from the CLI).
    // Allowing for compilation is more of a nicety in case the user forgot 
    // (and he was fortunate enough to have error-free java code).
    // Note that WizCompile checks whether running as CLI or GUI.
    WizCompile.compile(cName);
    return classLoadable(cName);
  }
  
  private ArrayList<Statement> expandWizard(Statement w) {
    
    // Convert the given wizard statement to a series of straight G-code
    // Statements. If there's a problem, return an ArrayList consisting of
    // a single error Statement. Or, the wizard might have some kind of 
    // internal problem, and *it* might return a mixture of valid G-code
    // and error statements.
    DataWizard wiz = (DataWizard) w.data;
    
    System.out.println("Attempting standard load");
    
    WizardBase theWizard = classLoadable(wiz.cmd);
    
    if (theWizard == null)
      theWizard = compileAndLoad(wiz.cmd);
    
    if (theWizard == null)
      {
        // Still null, so not a known class. Return an error Statement.
        ArrayList<Statement> answer = new ArrayList<>(1);
        Statement e = formError(w,"Unknown wizard: " +wiz.cmd);
        answer.add(e);
        return answer;
      }
    
    System.out.println("class loaded");
    
    // Got here, so the class is known and loaded. Run it.
    return theWizard.execute(); 
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

  void reset() {
    
    lowerLayer.reset();
    this.seenOCommand = false;
  }

}
