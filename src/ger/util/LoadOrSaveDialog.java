package ger.util;

// Use this to bring up a dialog that asks the user where to save or load
// a file.
// 
// Say
// String[] choice = LoadOrSaveDialog.getSaveChoice("Choose a file");
// to get the dir and filename that the user chooses. 

import java.awt.FileDialog;
import java.awt.Frame;

public class LoadOrSaveDialog {
  
    public static String[] getSaveChoice(String message) {
 
     // Open up file dialog. String[0] is dir, String[1] is file name.
     // Return null if canceled.
     FileDialog fdlog = new FileDialog(new Frame(),message,FileDialog.SAVE);
     fdlog.show();
     String dir = fdlog.getDirectory();
     String fname = fdlog.getFile();
     if (fname == null)
       return null;
     fdlog.dispose();
     
     String[] answer = new String[2];
     answer[0] = dir;
     answer[1] = fname;
     return answer;
   }
 
   public static String[] getLoadChoice(String message) {
 
     // Open up file dialog. String[0] is dir, String[1] is file name.
     // Return null if canceled.
     FileDialog fdlog = new FileDialog(new Frame(),message,FileDialog.LOAD);
     fdlog.show();
     String dir = fdlog.getDirectory();
     String fname = fdlog.getFile();
     if (fname == null)
       return null;
     fdlog.dispose();
 
     String[] answer = new String[2];
     answer[0] = dir;
     answer[1] = fname;
     return answer;
   }
}
