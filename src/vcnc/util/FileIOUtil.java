package vcnc.util;

import java.io.*;
import java.util.*;

//import vcnc.util.StringUtil;

// Various methods to make reading and writing simpler.

public class FileIOUtil {
  
  public static String[] getFileList(String parent) {
    
    // Return a list of all the files and directories in parent.
    // Very similar to getDirList().
    String[] answer = null;
    try {
      File f = new File(parent);
      answer = f.list();
    } catch (Exception e) {
      System.err.println("Problem getting list of files in " +parent+ ": "+ e);
      return null;
    }
    
    if ((answer == null) || (answer.length == 0))
      return null;
    
    return answer;
  }
  
  public static String[] getFileOnlyList(String parent) {
    
    // Like getDirList(), but returns only files, no dirs. Like it, it
    // may be slow.
    String[] rawList = null;
    try {
      File f = new File(parent);
      rawList = f.list();
    } catch (Exception e) {
      System.err.println("Problem getting files/dirs in " +parent+ ": "+ e);
      return null;
    }
    
    if ((rawList == null) || (rawList.length == 0))
      return null;
      
    // Go through these and cull out only the files.
    boolean[] file = new boolean[rawList.length];
    int fileCount = 0;
    for (int i = 0; i < rawList.length; i++)
      {
        try {
          File f = new File(parent,rawList[i]);
          if (f.isFile() == true)
            {
              file[i] = true;
              ++fileCount;
            }
          else
            file[i] = false;
        } catch (Exception e) {
          System.err.println("Problem with file " +rawList[i]+
                             " in " +parent+ ": " +e);
          return null;
        }
      }
    
    // Copy over only the file strings.
    String[] answer = new String[fileCount];
    fileCount = 0;
    for (int i = 0; i < rawList.length; i++)
      {
        if (file[i] == true)
          {
            answer[fileCount] = rawList[i];
            ++fileCount;
          }
      }
    
    return answer;
  }
  
  public static String[] getDirList(String parent) {
    
    // Return a list of all the directories in the given parent dir.
    // Returns null on error.
    // 
    // BUG: This can be *very* slow. The reason may be that when it culls
    // out non-directories it's doing a stat() system call behind the scenes
    // and for large directories this is slow?
    String[] rawList = null;
    try {
      File f = new File(parent);
      rawList = f.list();
    } catch (Exception e) {
      System.err.println("Problem getting list of dirs in " +parent+ ": "+ e);
      return null;
    }
    
    if ((rawList == null) || (rawList.length == 0))
      return null;
      
    // Go through these an cull out only the directories.
    boolean[] dir = new boolean[rawList.length];
    int dirCount = 0;
    for (int i = 0; i < rawList.length; i++)
      {
        try {
          File f = new File(parent,rawList[i]);
          if (f.isDirectory() == true)
            {
              dir[i] = true;
              ++dirCount;
            }
          else
            dir[i] = false;
        } catch (Exception e) {
          System.err.println("Problem with file " +rawList[i]+ " in " +parent+ ": " +e);
          return null;
        }
      }
    
    // Copy over only the dir strings.
    String[] answer = new String[dirCount];
    dirCount = 0;
    for (int i = 0; i < rawList.length; i++)
      {
        if (dir[i] == true)
          {
            answer[dirCount] = rawList[i];
            ++dirCount;
          }
      }
    
    return answer;
  }
  
  public static String[] lsStar(String parent,String start) {
    
    // Returns a list of the files in the given parent directory of the
    // for "start*". So this is like saying "ls start*". This will also
    // return any directories of this form. Returns null if there are none.
    String[] answer = null;
    
    try {
      File f = new File(parent);
      NameStarFileFilter fileFilter = new NameStarFileFilter(start);
      answer = f.list(fileFilter);
    } catch (Exception e) {
      System.err.println("Problem getting list of files in " +parent+ ": "+ e);
      return null;
    }
    
    if ((answer == null) || (answer.length == 0))
      return null;
    
    return answer;
  }
  
  public static void delete(String parent,String[] files) {
    
    // Delete all the given files in the parent directory.
    if (files == null)
      return;
    
    for (int i = 0; i < files.length; i++)
      {
        File f = new File(parent,files[i]);
        f.delete(); 
      }
    
  }
  
  public static char readCharTo(BufferedInputStream bin,char delim) 
    throws Exception {
    
    // In theory, there should be only one character before the delim char, but
    // if there are several, this returns the last character before delim.
    // If the field is blank (the next character to read is delim), then an
    // EmptyReadException is thrown.
    // This reads the delim character too (as it must).
    // NOTE: The way this is written, the EOF is not detected. If you hit EOF,
    // it simply returns ' '.
    
    int i = bin.read();
    char c = ' ';
    boolean readChar = false;
    while (((char) i != delim) && (i != -1))
      {
        c = (char) i;
        i = bin.read();
        readChar = true;
      }
    
    // This is to indicate that the there was nothing there. The char is
    // undefined.
    if (readChar == false) 
      throw new EmptyReadException("readCharTo() gives undefined value");
    
    return c;
  }
  
  public static char readCharToLFCR(BufferedInputStream bin) throws Exception {

    // Read the char up to the EOL, like readCharTo() above, but the EOF could
    // be given by 0x0A (LF) and/or 0x0D (CR).
    // Use this when you know that the line ends with LF *and* CR. Have to
    // distinguish this case since these characters are read and can't be put
    // back. This also handles the possibility that the EOL is really the EOF.
    // If the field is blank (the next character to read is lf/cr), then an
    // EmptyReadException is thrown.
    
    char cr = 0x0D;
    char lf = 0x0A;
    
    int i = bin.read();
    char c = ' ';
    boolean readChar = false;
    while (((char) i != cr) && ((char) i != lf) && (i != -1))
      {
        c = (char) i;
//        System.out.println(Character.toString(c));
        i = bin.read();
        readChar = true;
      }
    
    // Read the other EOL character too.
    i = bin.read();
    
//    System.out.println(Character.toString((char) i));
    
    // This is to indicate that the there was nothing there. The char is
    // undefined.
    if (readChar == false) 
      throw new EmptyReadException("readCharTo() gives undefined value");

    return c;
  }
  
  public static char[] readCharsToPadded(int len,BufferedInputStream bin,char delim) 
    throws Exception {
    
    // Like readCharsTo() below, but it will always return len
    // characters. If there are fewer than len chars to the next delim, then
    // the rest is padded with spaces. 
    // BEWARE: If there are more than len bytes, this will read them and 
    // throw them away.
    
    char[] answer = new char[len];
    int i = bin.read();
    int count = 0;
    while (((char) i != delim) && (i != -1) && (count < len))
      {
        answer[count] = (char) i;
        count++;
        i = bin.read();
      }
    
    // Pad the end with spaces.
    for (int j = count; j < len; j++)
      answer[j] = ' ';
    
    // Read any extra characters beyond the first len to get us up to delim.
    while (((char) i != delim) && (i != -1))
      i = bin.read();
      
    return answer;
  }
    
  public static char[] readCharsTo(BufferedInputStream bin,char delim) 
    throws Exception {

    // Returns all the characters up to the delim.
    // If the field is blank (the next character to read is delim), then a
    // null is returned, with no exception thrown.
    // This reads the delim character too (as it must).
    // NOTE: The way this is written, the EOF is not detected. If you hit EOF,
    // it simply returns null.
    // BUG: It is assumed that no more than 100 characters will appear in
    // the result.
    
    char[] temp = new char[100];
    int i = bin.read();
    int count = 0;
    while (((char) i != delim) && (i != -1))
      {
        temp[count] = (char) i;
        count++;
        i = bin.read();
      }
    
    if (count == 0)
      return null;
    
    // Pull out only the chars we want.
    char[] answer = new char[count];
    for (i = 0; i < count; i++)
      answer[i] = temp[i];
      
    return answer;
  }
  
  public static byte readByteTo(BufferedInputStream bin,char delim) 
    throws Exception {
    
    // Read the characters up to delim. Assume that they form a byte. Parse
    // this byte and return it. This reads the delim character (as it must).

    // Read the characters into a StringBuffer.
    StringBuffer sbuf = new StringBuffer(100);
    int i = bin.read();
    char c = (char) i;
    while ((c != delim) && (i != -1))
      {
        sbuf.append(c);
        i = bin.read();
        c = (char) i;
      }

    // This is most likely to happen if you call when the buffer is already at
    // EOF.
    // BUG: Ideally, this should be handled as a special case.
    if (sbuf.length() == 0)
      throw new EmptyReadException("readByteTo() gives empty char array!");

    return Byte.parseByte(sbuf.toString());    
  }
  
  public static short readShortTo(BufferedInputStream bin,char delim) 
    throws Exception {

    // Read the characters up to delim. Assume that they form a short. Parse
    // this short and return it. This reads the delim character (as it must).

    // Read the characters into a StringBuffer.
    StringBuffer sbuf = new StringBuffer(100);
    int i = bin.read();
    char c = (char) i;
    while ((c != delim) && (i != -1))
      {
        sbuf.append(c);
        i = bin.read();
        c = (char) i;
      }

    // This is most likely to happen if you call when the buffer is already at
    // EOF.
    // BUG: Ideally, this should be handled as a special case.
    if (sbuf.length() == 0)
      throw new EmptyReadException("readShortTo() gives empty value");

    return Short.parseShort(sbuf.toString());
  }

  public static int readIntTo(BufferedInputStream bin,char delim) 
    throws EOFException,IOException,EmptyReadException {

    // Read the characters up to delim. Assume that they form an int. Parse
    // this int and return it. This reads the delim character (as it must).
    // If the value occurs at the end of the file, so that the read is 
    // terminated by hitting the EOF, the proper value is returned, but you
    // won't know that you hit the EOF till the next read is attempted.
    // BUG: This method is the most complete as far as how the exceptions
    // are handled. I really should use this as a template for the other
    // methods.
    
    // Read the characters into a StringBuffer.
    StringBuffer sbuf = new StringBuffer(100);
    int i = bin.read();
    char c = (char) i;
    while ((c != delim) && (i != -1))
      {
        // For debugging.
//        StaticPrintFile.println(c);
//        System.out.println(Character.toString(c));
        
        
        sbuf.append(c);
        i = bin.read();
        c = (char) i;
      }
    
    if ((c == delim) && (sbuf.length() == 0))
      // Did read in delimiter, but nothing more.
      throw new EmptyReadException("readIntTo() gives undefined value");
    
    if ((sbuf.length() == 0) && (i == -1))
      // Hit EOF as the first thing read.
      throw new EOFException("readIntTo() hit EOF");
    
    return Integer.parseInt(sbuf.toString());
  }
  
  public static long readLongTo(BufferedInputStream bin,char delim) 
    throws Exception {

    // Read the characters up to delim. Assume that they form a long. Parse
    // this long and return it. This reads the delim character (as it must).

    // Read the characters into a StringBuffer.
    StringBuffer sbuf = new StringBuffer(100);
    int i = bin.read();
    char c = (char) i;
    while ((c != delim) && (i != -1))
      {
        sbuf.append(c);
        i = bin.read();
        c = (char) i;
      }

    // This is most likely to happen if you call when the buffer is already at
    // EOF.
    // BUG: Ideally, this should be handled as a special case.
    if (sbuf.length() == 0)
      throw new Exception("readLongTo() gives empty char array!");

    return Long.parseLong(sbuf.toString());
  }

  public static double readDoubleTo(BufferedInputStream bin,char delim)
    throws Exception {

    // Read the characters up to delim. Assume that they form a double. Parse
    // this double and return it. This reads the delim character (as it must).
    // BUG: I think that scientific notation will confuse this.
    
    // Read the characters into a StringBuffer.
    StringBuffer sbuf = new StringBuffer(100);
    int i = bin.read();
    char c = (char) i;
    while ((c != delim) && (i != -1))
      {
        sbuf.append(c);
        i = bin.read();
        c = (char) i;
      }

    // This is most likely to happen if you call when the buffer is already at
    // EOF.
    // BUG: Ideally, this should be handled as a special case.
    if (sbuf.length() == 0)
      throw new Exception("readDoubleTo() gives empty char array!");

    return Double.parseDouble(sbuf.toString());
  }
  
  public static double readDoubleToLFCR(BufferedInputStream bin)
    throws Exception {

    // Read the double up to EOL. Like readDoubleTo() above, but the EOL could be
    // given by 0x0A (LF) and/or 0x0D (CR).
    // Use this when you know that the line ends with LF *and* CR. Have to
    // distinguish this case since these characters are read and can't be put 
    // back. This also handles the possibility that the EOL is really the EOF.
    // When there is nothing there (field is blank), throw an EmptyReadException.
    // BUG: I think that scientific notation will confuse this.

    // Read the characters into a StringBuffer.
    StringBuffer sbuf = new StringBuffer(100);
    char cr = 0x0D;
    char lf = 0x0A;
    int i = bin.read();
    char c = (char) i;
    while ((c != cr) && (c != lf) && (i != -1))
      {
        sbuf.append(c);
        i = bin.read();
        c = (char) i;
      }
   
    // Read the other EOL character too.
    i = bin.read();
   
    // This is most likely to happen if you call when the buffer is already at
    // EOF.
    // BUG: Ideally, this should be handled as a special case.
    if (sbuf.length() == 0)
      throw new EmptyReadException("readDoubleToLFCR() gives undefined value");

    return Double.parseDouble(sbuf.toString());
  }
  
  public static void saveDoubleVector(String destDir,String filename,
                                      Vector data) {

    // The data Vector is all doubles. Convert to an array and serialize it
    // out to a file.
    double[] doubleArray = new double[data.size()];
    for (int i = 0; i < doubleArray.length; i++)
      doubleArray[i] = ((Double) data.elementAt(i)).doubleValue();
//    saveDoubleArray(destDir,filename,doubleArray);
    serializeObject(destDir,filename,doubleArray);
  }

  public static void saveIntegerVector(String destDir,String filename,
                                       Vector data) {

    // The data Vector is all integers. As in saveDoubleVector().
    int[] intArray = new int[data.size()];
    for (int i = 0; i < intArray.length; i++)
      intArray[i] = ((Integer) data.elementAt(i)).intValue();
//    saveIntegerArray(destDir,filename,intArray);
    serializeObject(destDir,filename,intArray);
  }

  public static void saveCharacterVector(String destDir,String filename,
                                         Vector data) {

    char[] charArray = new char[data.size()];
    for (int i = 0; i < charArray.length; i++)
      charArray[i] = ((Character) data.elementAt(i)).charValue();
//    saveCharacterArray(destDir,filename,charArray);
    serializeObject(destDir,filename,charArray);
  }
  
  public static void saveCharArrayVector(String destDir,String filename,
                                         Vector data) {

    char[][] charArray = new char[data.size()][];
    for (int i = 0; i < charArray.length; i++)
      charArray[i] = (char[]) data.elementAt(i);
//    saveCharacterArray(destDir,filename,charArray);
    serializeObject(destDir,filename,charArray);
  }


  public static void saveByteVector(String destDir,String filename,
                                    Vector data) {

    byte[] byteArray = new byte[data.size()];
    for (int i = 0; i < byteArray.length; i++)
      byteArray[i] = ((Byte) data.elementAt(i)).byteValue();
//    saveByteArray(destDir,filename,byteArray);
    serializeObject(destDir,filename,byteArray);
  }

  public static void saveLongVector(String destDir,String filename,
                                    Vector data) {

    long[] longArray = new long[data.size()];
    for (int i = 0; i < longArray.length; i++)
      longArray[i] = ((Long) data.elementAt(i)).longValue();
//    saveLongArray(destDir,filename,longArray);
    serializeObject(destDir,filename,longArray);
  }

  public static void saveShortVector(String destDir,String filename,
                                     Vector data) {

    short[] shortArray = new short[data.size()];
    for (int i = 0; i < shortArray.length; i++)
      shortArray[i] = ((Short) data.elementAt(i)).shortValue();
//    saveShortArray(destDir,filename,shortArray);
    serializeObject(destDir,filename,shortArray);
  }
  
  public static void saveStringAsAscii(String destDir,String filename,
                                       String s) {
    
    // Save the given text to an ascii file.
    try {
      File f = new File(destDir,filename);

      FileOutputStream fileOutput = new FileOutputStream(f);
      PrintStream out = new PrintStream(fileOutput);
      out.print(s);
      out.close();
      fileOutput.close();
    } catch (Exception e) {
      System.out.println("Problem saving text to file at " 
                         +destDir+ " as " +filename+ ": " + e);
    }
  }
  
  public static void serializeObject(String destDir,String filename,Object o) {

    // Save data, by serializing, to destDir/filename.
    try {
      File f = new File(destDir);
      if (f.exists() == false)
        // This directory doesn't exist. Create it and any part of the path
        // leading to it.
        f.mkdirs();
      f = new File(destDir,filename);

      FileOutputStream fout = new FileOutputStream(f);
      ObjectOutputStream oout = new ObjectOutputStream(fout);
      oout.writeObject(o);
      oout.close();
      fout.close();
      
    } catch (Exception e) {
      System.err.println("Problem writing " +destDir+ " and " +filename+
                         ": " +e);
      System.exit(1);
    }
  } 

  public static Object loadSerializedObject(String dir,String filename) {
    
    // Return null if the file does not exist.
    Object answer = null;
    try {
      File f = new File(dir,filename);
      if (f.exists() == false)
        return null;
      
      FileInputStream fin = new FileInputStream(f);
      ObjectInputStream oin = new ObjectInputStream(fin);
      answer = oin.readObject();
      oin.close();
      fin.close();
      
    } catch (Exception e) {
      System.err.println("Problem reading " +dir+ " and " +filename+ ": " +e);
      System.exit(1);
    }
    return answer;
  }
  
  public static byte[] loadFileToMemory(String dir,String fname) {
    
    // Load the given file in memory. If there's an IO problem, return null.
    //
    // This is done by reading the entire file into a ByteArrayOutputStream,
    // then pulling the bytes out of that.
    ByteArrayOutputStream bout = new ByteArrayOutputStream(4096);
    try {
      // Create the input stream.
      File f = new File(dir,fname);
      FileInputStream fin = new FileInputStream(f);
      
      byte[] tempb = new byte[4096];
      while (fin.available() > 0)
        {
          int len = fin.read(tempb);
          bout.write(tempb,0,len);
        }
      
      fin.close();
      bout.close();
    } catch (Exception e) {
      System.err.println("Problem: " +e);
      e.printStackTrace();
      return null;
    }
    
    return bout.toByteArray();
  }
  
  public static String loadFileToString(String dir,String fname) {
    
    // Load the given file in memory. If there's an IO problem, return null.
    // Exactly as above, but it returns a String.
    ByteArrayOutputStream bout = new ByteArrayOutputStream(4096);
    try {
      // Create the input stream.
      File f = new File(dir,fname);
      FileInputStream fin = new FileInputStream(f);
      
      byte[] tempb = new byte[4096];
      while (fin.available() > 0)
        {
          int len = fin.read(tempb);
          bout.write(tempb,0,len);
        }
      
      fin.close();
      bout.close();
    } catch (Exception e) {
//      System.err.println("Problem: " +e);
//      e.printStackTrace();
      return null;
    }
    
    return bout.toString();
  }
  
  public static String getMostRecentFilename(String dir, String prefix) {
    // return the most recent (as determined by the date and time in the file
    // name, not by timestamp) file of the form dir/prefix.yyyymmddhhmmss
    
    String[] fileNames = getFileList(dir);
    
    if (fileNames == null)
      {
        System.err.println("The prefix " +prefix+ 
            " does not have any files in " + dir);
        (new Exception()).printStackTrace();
        return null;
      }
      
    Arrays.sort(fileNames);
    
    // Return the predictor stored at the last one of these files.
    return fileNames[fileNames.length - 1];
  }
  
  public static String getMostRecentFilename(String dir,String prefix,
                                             int date) {
      
    // same as above except we use a fixed date so we are looking for something
    // of the form dir/prefix.yyyymmddhhmmss where date == yyyymmdd
    String[] fileNames = FileIOUtil.getFileList(dir);
    
    if (fileNames == null)
      {
        System.err.println("The prefix " +prefix+ 
            " does not have any files in " + dir + "on date" + date);
        (new Exception()).printStackTrace();
        return null;
      }
      
    Arrays.sort(fileNames);
    
    // Find the correct file.
    String theFileName = fileNames[0];
    for (int i = 0; i < fileNames.length; i++)
      {
      	if(fileNames[i].startsWith(prefix) == false)
      		continue;
      	
        // Split off the yyyymmddhhmmss.
        String dateTime = StringUtil.splitOutLast(fileNames[i],".")[1];
        
        // Pull out the date and time along.
        String datePart = dateTime.substring(0,8);
        String timePart = dateTime.substring(8);
        int curDate = Integer.parseInt(datePart);
        
        if (curDate <= date)
          theFileName = fileNames[i];
        else
          break;
      }
      
    return theFileName;
  }

  public static String getMostRecentTimeInDir(String dir,String prefix,
      int date) {
    // here we are looking just for the time of the most recent file in dir
    // with this prefix on this date
    
    String filename = getMostRecentFilename(dir, prefix, date);
    
    if(filename == null)
      return null;
    
    String dateTime = StringUtil.splitOutLast(filename,".")[1];
    
    // Pull out the date and time along.
    String datePart = dateTime.substring(0,8);
    String timePart = dateTime.substring(8);
    
    return timePart;
  }
}




// Helper class to find only a file starts with a given symbol.
// It works like "ls symbol*" where symbol is what we want. Used with
// File.listFiles() by FileIOUtil.lsStar().

class NameStarFileFilter implements FilenameFilter {
  
  private String symbol;
  
  NameStarFileFilter(String symbol) {
    this.symbol = symbol;
  }
  
  public boolean accept(File dir,String name) {
    return name.startsWith(symbol);
  }
}


