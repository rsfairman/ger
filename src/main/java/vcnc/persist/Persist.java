package vcnc.persist;

/*

Management of persistent data. This is for values that need to remain the
same every time the program is launched, like the tool table and wizard 
definitions.

BUG: Currently in its own package. Maybe it should just go into the vcnc 
package. Will there be other classes?

The data that persists is:

* Inch/mm choice. This is mostly important for the tool table and the
  work offsets table, since the values in these tables are given using these
  units. This is also the choice the machine starts off with (G20 or G21).
  Finally, these are the units used by the machine internally and that will
  be assumed for the ultimate output.
* Work offsets table. This is just a table of (X,Y,Z) values to be used
  with G55,...,G59.
  




*/

/*

BUG: I have not really used this (from John Dunlap), but this might be the 
way to go. The user.dir really isn't the best choice. With this, you get
different folders, depending on whether it's being run inside a jar or not.

import java.io.File;
import java.net.URISyntaxException;

public class Main {
    public static String configFolder() throws URISyntaxException {
        String folder = System.getProperty("file.separator") + ".vcnc";
        File f = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        if (f.isDirectory()) {
            return f.getPath() + folder;
        }

        return f.getParentFile().getPath() + folder;
    }

    public static void main(String[] args) throws URISyntaxException {
        System.out.println(configFolder());
    }
}



*/


import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import vcnc.tpile.MachineState;
import vcnc.util.FileIOUtil;


public class Persist {
  
  // The location of the .ger directory (which many be renamed). This is 
  // where anything persistent is saved.
  public static String gerDir = null;
  
  // The default location for the .ger directory.
  //
  // BUG: Currently this uses the working directory, which is probably
  // not what is desired. user.home is the home directory, which I do not
  // particularly like since it's just one more messy thing sitting in
  // that directory.
  // Alternative suggested by John D. May (hope?) point to where the
  // .jar sits.
  // System.out.println(new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getPath());
  private static String defaultDir = System.getProperty("user.dir") 
      + File.pathSeparator + ".ger";
  
  // The various items of persistent data are in different files with
  // these names.
  private static String inchName = "inch.txt";
  
  // You'd think that it would work to declare 
  // public static String gerDir = loadGerDirLoc();
  // above, and it would load the gerDir the very first time this class is
  // referenced, but that's not what happens. This is needed.
  static {
    gerDir = loadGerDirLoc();
  }
  
  private static String loadGerDirLoc() {
    
    // The location of this directory is stored in the user preferences.
    // Using this to store a significant amount of data is not a good idea,
    // so there's only a single key-value pair for the path (as a String).
    Preferences p = Preferences.userRoot();
    return p.get("gerPersist",defaultDir);
  }
  
  public static boolean usingDefault() {
    
    // Whether gerDir currently points to the default location.
    return gerDir.equals(defaultDir);
  }
  
  private static void loadInch() {
    
    // Read the entire file into memory.
    String s = FileIOUtil.loadFileToString(gerDir,inchName);
    
    if (s == null)
      {
        // File was never created. Defaults to inch.
        MachineState.machineInchUnits = true;
        return;
      }
    
    // Either 'inch' or 'mm'.
    if (s.equals("inch"))
      MachineState.machineInchUnits = true;
    else if (s.equals("mm"))
      MachineState.machineInchUnits = false;
    else
      {
        // Maybe the user tried to edit the file himself and made a mistake.
        JOptionPane.showMessageDialog(null,"Units read as " +s+ ".\n" +
          "That doesn't make sense; 'inch' or 'mm' expected.\n" +
          "Did you edit the file directly?\n" +
          "Defaulting to inches.");
      }
    
    // BUG: I may end up putting additional data in this file. Anything
    // that's fairly trivial could go in here. If so, I should rename
    // all these variables and methods not to use 'inch'.
    
  }
  
  public static void loadSettings() {
    
    // Load the various settings to the MachineState. This needs to be called
    // before running any G-code through the transpiler. Call this when the
    // program launches, or whenever the user changes the .ger directory.
    loadInch();
  }
  
  
  
}
