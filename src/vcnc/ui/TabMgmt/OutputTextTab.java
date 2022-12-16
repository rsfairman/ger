package vcnc.ui.TabMgmt;

/*
When G-code is translated, it generates text output, and this translation
could have been partial, only to a certain layer. This is the base class
for these various types of output tab. They're all the same, except for the
type.

*/

import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class OutputTextTab extends JScrollPane implements TypedDisplayItem {

  private TabbedType type = TabbedType.UNKNOWN;

  public JTextArea theText = new JTextArea();
  
  // This output came from some particular G-code, found in this tab.
  GInputTab parentCode = null;
  

  public TabbedType type() {
    return type; 
  }
  
  public OutputTextTab(TabbedType theType,String text,GInputTab parent) {
    
    this.parentCode = parent;
    this.type = theType;
    
    theText = new JTextArea(new String(text));
    
    theText.setEditable(false);
//    theText.setVisible(true);
    
    Font theFont = new Font("Monospaced",Font.PLAIN,12);
    theText.setFont(theFont);
    
    // I thought that this would work:
    // this.add(theText);
    // but this seems to be the right thing to do.
    this.setViewportView(theText);
  }
  
}
