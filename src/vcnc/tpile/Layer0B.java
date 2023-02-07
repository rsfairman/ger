package vcnc.tpile;

/*

Comes after Layer0A, which removes (and acts on) any "machine directives."
This layer removes any user-defined functions (wizards), all of which must 
appear after the O-code that opens the program proper. So, that's one type 
of statement that's been removed from the stream. 

There are two problems to be solved here: converting a wizard statement to 
G-code (expansion), and compiling the wizard code from .java to .class and 
loading it.

Ideally, the user should be able to drop a .java file into the .ger directory
and have it compile automatically, but that requires too much UI infrastructure
(for the user to debug his code) to be reasonable as the normal course of 
events. Instead, compilation should be done through the CLI -- although it is 
attempted here too; maybe the user will get lucky and his code will compile 
with no issues. Or maybe he knows that the code is correct and just wants to
drop in the *.java file on a different machine.

*/

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;

import vcnc.persist.Persist;
import vcnc.tpile.parse.DataWizard;
import vcnc.WizCompile;

import vcnc.wizard.WizardBase;


public class Layer0B {

  
  /*


  // The statement objects from the lower layer, and the mark of where we
  // are in processing them.
  private ArrayList<StxP> theStatements = null;
  private int statementIndex = 0;
  
  // Wizards must appear after the O-statement. In theory, this requirement
  // could be relaxed, but being strict about this should make the distinction
  // between wizards and directives clearer to the user.
  private boolean seenOCommand = false; 
  
  
  Layer0B(ArrayList<StxP> smnts) {
    this.theStatements = smnts;
    this.statementIndex = 0;
  }
  
  private StxP getLower() {
    
    if (statementIndex >= theStatements.size())
      return new StxP(StxP.EOF);
    
    StxP answer = theStatements.get(statementIndex);
    ++statementIndex;
    return answer;
  }
  
  private StxP peekLower() {
    
    if (statementIndex >= theStatements.size())
      return new StxP(StxP.EOF);
    
    return theStatements.get(statementIndex);
  }

  private String formError(int lineNumber,String msg) {
    
    return "Error on line " +lineNumber+ ": " +msg;
  }
  
  private StxP formError(StxP s,String msg) {
    
    // BUG: This is a much better way to do things.
    // See I can get rid of the "other" formError() in all classes that do this.
    StxP answer = new StxP(StxP.ERROR);
    answer.lineNumber = s.lineNumber;
    answer.charNumber = s.charNumber;
    answer.error = formError(s.lineNumber,msg);
    return answer;
  }
  
  private ArrayList<StxP> nextStatements() {
    
    // Pull a single statement out of the array from Layer0A, and convert it.
    // Most of this time, there's nothing to do, but wizards may be transformed
    // to a large number of statements. That's why this returns an array.
    // 
    // So the result will either be an array with a single entry (the usual
    // case), or a longer array due to wizard expansion.
    
    // Get a single statement from Layer0A and expand it if it's a wizard.
    StxP s = getLower();
    
    // BUG: What if we get an EOF? Especially if there is no O-command.
        
    if (s.type != StxP.WIZARD)
      {
        // Any non-wizard statement is simply passed along.
        if (s.type == StxP.PROG)
          this.seenOCommand = true;

        // This implicitly converts to the correct type. It's an array (with 
        // one element) of StxP objects, but the only difference between these
        // types is a single static final value (WIZARD). So it's not really
        // of the correct type, but that doesn't hurt anything.
        return new ArrayList<>(List.of(s));
      }
    
    // Must be a wizard.
    if (this.seenOCommand == false)
      {
        // Machine directives (that occur before the O-statement) have been 
        // filtered out by the previous layer of the translator. So any 
        // wizard before the O-statement is in error. It should be impossible
        // for this to arise anyway.
        DataWizard wizData = (DataWizard) s.data;
        StxP err = 
            formError(s,"Unknown machine directive: " +wizData.cmd);
        return new ArrayList<>(List.of(err));
      } 
    
    // The error-free case. Expand the wizard.
    return expandWizard(s);
  }

  private WizardBase classLoadable(String cName) {
    
    // Attempt to load the given class. If that's possible, then load it and
    // return it. Otherwise, return null.
    //
    // IMPORTANT. For this to work, the module-info file must include the line
    // exports vcnc.wizard
    // I'm a little unclear why this is, but the gist seems to be that it's
    // because the *.class file being loaded is outside the usual directory
    // hierarchy (or the JaR file) and the class loader needs some kind (?)
    // of special permission 
    //
    // BUG: the package name 'wizard_test' is hard-coded.
    // BUG: Change method name to attemptLoad().
    
    // Try loading "normally," without messing with the class loader.
    // This should work if the wizard class is "built in" and was compiled 
    // as part of the original ger framework.
    try {
      // Package name, 'wizard_test', followed by class name.
      Class<?> loaded = Class.forName("wizard_test." +cName);
//      Class<?> loaded = Class.forName(cName);
      
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
      try {
        loader.close();
      } catch (IOException e) {
        // Really shouldn't happen.
        System.err.println("Strange error: " +e.getMessage());
        e.printStackTrace();
      }
      return answer;
      
    } catch (MalformedURLException e) {
      // BUG: This one really shouldn't happen. The gerDir must be a valid
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
    // be unusual since that should have been done earlier (by the user,
    // from the CLI). Allowing for compilation here is more of a nicety in 
    // case the user forgot (and he was fortunate enough to have error-free 
    // Java code).
    // Note that WizCompile checks whether running as CLI or GUI.
    WizCompile.compile(cName);
    return classLoadable(cName);
  }
  
  private ArrayList<StxP> expandWizard(StxP w) {
    
    // Convert the given wizard statement to a series of straight G-code
    // statements. If there's a problem, return an ArrayList consisting of
    // a single error statement. Or, the wizard might have some kind of 
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
        ArrayList<StxP> answer = new ArrayList<>(1);
        StxP e = formError(w,"Unknown wizard: " +wiz.cmd);
        answer.add(e);
        return answer;
      }
    
    System.out.println("class loaded");
    
    // Got here, so the class is known and loaded. Run it.
    return theWizard.execute(wiz.args); 
  }
  
  public static ArrayList<St0B> process(String gCode) {
    
    // Transform the statement objects from the Layer0A to a more limited
    // subset.
    ArrayList<St0B> answer = new ArrayList<>();
    
    Layer0B curLayer = new Layer0B(Layer0A.process(gCode));
    
    ArrayList<StxP> newOnes = curLayer.nextStatements();
    
    while (newOnes.get(0).type != St0B.EOF)
      {
        for (StxP s : newOnes)
          answer.add(s);
        newOnes = curLayer.nextStatements();
      }
    
    return answer;
  }

  public static String digestAll(String gcode) {
    
    // Take the given g-code and feed it through, producing a single String
    // suitable for output to the user, or for use with unit tests.
    // BUG: Isn't this method identical in every case? And the process() method
    // is pretty close to identical too.
    ArrayList<St0B> theStatements = process(gcode);

    StringBuffer answer = new StringBuffer();
    
    for (St0B s : theStatements)
      {
        answer.append(s.toString());
        answer.append("\n");
      }
    
    return answer.toString();
  }
  
  */
  
  
  
}
