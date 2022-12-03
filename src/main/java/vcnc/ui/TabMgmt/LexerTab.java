package vcnc.ui.TabMgmt;

/*

To display the tokens that come from the Lexer.

All of these text-based tabs are similar.

BUG: In fact, there should be a single base class for them. 

BUG: The LexerTab, ParserTab, etc. are all basically the same thing.
The only real difference is the type.

*/

import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class LexerTab extends JScrollPane implements TypedDisplayItem {
  

  private TabbedType type = TabbedType.LEXER_OUT;
  public TabbedType type() { return type; }

  public JTextArea theText = new JTextArea();
  
  // This lexer output came from some particular G-code.
  GInputTab parentCode = null;
  

  public LexerTab(String text,GInputTab parent) {
    
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
