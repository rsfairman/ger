package vcnc.ui.TabMgmt;

/*

Holds the same thing a single tab that might appear in MainWindow, but
the contents are static.

*/

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JFrame;

public class StaticWindow extends JFrame {
  
  
  public StaticWindow(String title, Component c) {
    
    super(title);
    
    this.setSize(300,500);
    this.setPreferredSize(new Dimension(600,500));
    this.getContentPane().add(c);
    
    this.pack();
    this.setVisible(true);
  }

}
