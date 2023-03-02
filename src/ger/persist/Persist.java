package ger.persist;

/*

Management of persistent data. This is for values that need to remain the
same every time the program is launched, like the tool table and wizard 
definitions.

BUG: Currently in its own package. Maybe it should just go into the vcnc 
package. Will there be other classes?

BUG: (or not?) When you choose another directory for the machine settings,
you can choose *any* directory. It doesn't create a new '.ger' directory;
it uses whatever you choose. 


The data that persists is:

* Inch/mm choice. This is mostly important for the tool table and the
  work offsets table, since the values in these tables are given using these
  units. This is also the choice the machine starts off with (G20 or G21).
  Finally, these are the units used by the machine internally and that will
  be assumed for the ultimate output.
* Choice of scale, to be used when rendering. This has nothing to do with
  G-code or a physical machine, but this is the natural place for it.
* Work offsets table. This is just a table of (X,Y,Z) values to be used
  with G55,...,G59.
* Tool turret. Specifying the cutters is inherently complicated once you
  allow for arbitrary tool profiles.
  





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

import java.nio.file.Path;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;

import javax.swing.JOptionPane;

import ger.tpile.DefaultMachine;
import ger.util.FileIOUtil;
import ger.workoffsets.WorkOffsets;


public class Persist {
  
  // The key used in Preferences.
  private static final String theKey = "gerPersist";
  
  // The location of the .ger directory (which many be renamed, and isn't 
  // required to be called 'ger'). This is where anything persistent is saved.
  private static String gerDir = null;
  
  // The default location for the .ger directory.
  //
  // BUG: Currently this uses the working directory, which is probably
  // not what is desired. user.home is the home directory, which I do not
  // particularly like since it's just one more messy thing sitting in
  // that directory.
  // Alternative suggested by John D... May (hope?) point to where the
  // .jar sits.
  // System.out.println(new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getPath());
  private static String defaultDir = System.getProperty("user.dir") 
      + File.separator + ".ger";
  
  // The various items of persistent data are in different files with
  // these names.
  private static String indivItemsName = "indivs.txt";
  private static String woName = "workoffsets.txt";
  
  // You'd think that it would work to declare 
  // public static String gerDir = loadGerDirLoc();
  // above, and it would load the gerDir the very first time this class is
  // referenced, but that's not what happens. This is needed.
  static {
    gerDir = loadGerDirLoc();
    
    ensureExistence();
    
    reload();
  }
  
  
  private static String loadGerDirLoc() {
    
    // The location of this directory is stored in the user preferences.
    // Using this to store a significant amount of data is not a good idea,
    // so there's only a single key-value pair for the path (as a String).
    Preferences p = Preferences.userRoot();
    return p.get(theKey,defaultDir);
  }
  
  public static void setGerLocation(String path) {
    
    // It is assumed that path points to a valid location.
    
    // Normalize the path first, to eliminate things like '..'.
    String norm = null;
    
    try {
      norm = (new File(path)).getCanonicalPath();
    } catch (Exception e) {
      // This really shouldn't happen. The path is assumed to be valid.
      System.err.println("Strange error in Persist.setGerLocation().");
      return;
    }
    
    Preferences p = Preferences.userRoot();
    p.put(theKey,norm);
    gerDir = path;
    try {
      p.flush();
    } catch (BackingStoreException e) {
      // BUG: Could this really happen? Message dialog instead?
      System.err.println("Could not persistently save machine settings location.");
    }
  }
  
  public static String getGerLocation() {
    return gerDir;
  }
  
  private static void ensureExistence() {
    
    // Ensure that the .ger directory pointed to by gerDir exists on
    // the file system.
    File f = new File(gerDir);
    if (f.exists() == false)
      f.mkdirs();
  }
  
  public static boolean usingDefault() {
    
    // Whether gerDir currently points to the default location.
    return gerDir.equals(defaultDir);
  }
  
  private static void loadIndividual() {
    
    // Various small things, like inch/mm choice and scale.
    
    // Read the entire file into memory.
    String s = FileIOUtil.loadFileToString(gerDir,indivItemsName);
    
    if (s == null)
      {
        // File was never created. Defaults to inch.
        DefaultMachine.inchUnits = true;
        DefaultMachine.scale = 0.005;
        return;
      }
    
    String[] lines = s.split("\n");
    
    // Either 'inch' or 'mm'.
    if (lines[0].equals("inch"))
      DefaultMachine.inchUnits = true;
    else if (lines[0].equals("mm"))
      DefaultMachine.inchUnits = false;
    else
      {
        // Maybe the user tried to edit the file himself and made a mistake.
        JOptionPane.showMessageDialog(null,
          "When loading setup, units read as '" +lines[0]+ "'.\n" +
          "That doesn't make sense; 'inch' or 'mm' expected.\n" +
          "Did you edit the file directly?\n" +
          "Defaulting to inches.");
      }
    
    // A double value for scale.
    if (lines.length == 1)
      {
        // Scale not there. Maybe (?) the user edited the file.
        // Don't worry about inch vs mm, just choose a value that is (more or 
        // less) reasonable in either case.  
        DefaultMachine.scale = 0.010;
        return;
      }
    
    try {
      DefaultMachine.scale = Double.parseDouble(lines[1]);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(null,
          "When loading setup, scale read as '" +lines[1]+ "'.\n" +
          "Did you edit the file directly?\n" +
          "Using default value.");

        DefaultMachine.scale = 0.010;
        return;
    }
    
//    System.out.println("machine set to inch == " +MachineState.machineInchUnits);
  }
  
  private static void saveIndividual() {
    
    // Reverse of loadIndividual().
    String theText = null;
    if (DefaultMachine.inchUnits == true)
      theText = "inch\n";
    else
      theText = "mm\n";
    
    theText += Double.toString(DefaultMachine.scale);
    
    FileIOUtil.saveStringAsAscii(gerDir,indivItemsName,theText);
  }

  private static void loadWorkOffsets() {
    
    // The work offsets is an array of 6 x/y/z coordinates, expressed
    // as text.

    String s = FileIOUtil.loadFileToString(gerDir,woName);
    if (s == null)
      // File was never created. Allow to default to all zeros.
      return;
    
    String[] lines = s.split("\n");
    for (int i = 0; i < lines.length; i++)
      {
        String[] vs = lines[i].split(" ");
        for (int j = 0; j < 3; j++)
          {
            try {
              DefaultMachine.workOffsets.offset[i][j] = Double.parseDouble(vs[j]);
            } catch (Exception e) {
              // Shouldn't be possible, but...
              System.err.println(
                  "Parse error when loading persistent work offsets.");
            }
          }
      }
  }
  
  private static void saveWorkOffsets() {
    
    // Reverse of loadWorkOffsets().
    StringBuffer sbuf = new StringBuffer();
    for (int i = 0; i < WorkOffsets.Rows; i++)
      {
        for (int j = 0; j < WorkOffsets.Cols; j++)
          {
            String v = String.format("%.3f",DefaultMachine.workOffsets.offset[i][j]);
            sbuf.append(v).append(" ");
          }
        sbuf.append("\n");
      }

    FileIOUtil.saveStringAsAscii(gerDir,woName,sbuf.toString());
  }
  
  private static void loadSettings() {
    
    // Load the various settings to the MachineState. This needs to be called
    // before running any G-code through the transpiler. Call this when the
    // program launches, or whenever the user changes the .ger directory.
    loadIndividual();
    loadWorkOffsets();
  }
  
  public static void reload() {
    
    // Call this to reset the machine to whatever is stored on the disk.
    
    // BUG: Won't work as of Java 9.
//    try {
//      // Ensure that the current .ger file is in the classpath.
//      URLClassLoader urlClassLoader = 
//          (URLClassLoader) ClassLoader.getSystemClassLoader();
//      DynamicURLClassLoader dynaLoader = 
//          new DynamicURLClassLoader(urlClassLoader);
//      dynaLoader.addURL(new URL(gerDir));
//    } catch (MalformedURLException e) {
//      
//      // BUG: This shouldn't happen.
//      System.err.println("Problem with class loader");
//      
//    }
    
    loadSettings();
    
//    System.out.println("current .ger directory: " +gerDir);
  }
  
  public static void save() {
    
    // Save the persistent aspects of the MachineState. Everything in 
    // MachineState is static, so no argument (which is not ideal).
    
    // BUG: Save tool table and ???
    
    saveIndividual();
    saveWorkOffsets();
    

    
    
    
    
  }
  
  
}
