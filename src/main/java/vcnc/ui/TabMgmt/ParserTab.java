package vcnc.ui.TabMgmt;

import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ParserTab extends JScrollPane implements TypedDisplayItem {

  private TabbedType type = TabbedType.PARSER_OUT;
  public TabbedType type() { return type; }

  public JTextArea theText = new JTextArea();
  
  // This lexer output came from some particular G-code.
  GInputTab parentCode = null;
  

  public ParserTab(String text,GInputTab parent) {
    
    this.parentCode = parent;
    
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
