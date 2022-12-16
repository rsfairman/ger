package vcnc.ui.TabMgmt;

/*

To be used for input G-code.

This is the only tab type whose contents are editable.

*/

import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;


import vcnc.ui.LineNumbers.TextLineNumber;


public class GInputTab extends JScrollPane implements TypedDisplayItem {
  
  TabbedType type = TabbedType.G_INPUT;
  
  // What is shown -- the G-code.
  private JTextArea theText = new JTextArea();
  
  // Needed when line numbers are visible. Created by contructor and always
  // present.
  TextLineNumber lineNumberWidget = null;
  
  private boolean hasLineNumbers = false;
  
  // G-code leads to various outputs: partially or fully transpiled code,
  // renderings, etc. This tracks what has been generated from this G-code.
  // G-code can lead to any of these.

  // BUG: This will need to be completed for the additional layers.
  public LexerTab lexOut = null;
  public OutputTextTab parseOut = null;
  public OutputTextTab layer0AOut = null;
  public OutputTextTab layer0BOut = null;
  public OutputTextTab layer00Out = null;

  
  public TabbedType type() {
    return type;
  }
  
  public GInputTab(String text) {
    
    theText = new JTextArea(new String(text));
    theText.setEditable(true);
    
    Font theFont = new Font("Monospaced",Font.PLAIN,12);
    theText.setFont(theFont);
    
    // I thought that this would work:
    // this.add(theText);
    // but this seems to be the right thing to do.
    //this.setViewportView(theText);
    // or this works too:
    this.getViewport().add(theText);
    
    // For potential use later, if the user asks to see line numbers.
    this.lineNumberWidget = new TextLineNumber(theText);
  }
  
  public JTextArea getTextArea() {
    return this.theText;
  }
  
  public void toggleLineNumbers() {
    
    // Turn line numbers off or on. Typically (always?) only used for
    // G_INPUT type.
    this.lineNumberWidget.toggleLineNumbers();
    
    if (this.hasLineNumbers == false)
      {
        this.hasLineNumbers = true;
        this.setRowHeaderView(this.lineNumberWidget);
      }
    else
      {
        this.hasLineNumbers = false;
        this.setRowHeaderView(null);
      }
  }
  
  public JTextArea duplicate() {
    
    // Return a new JTextArea that's a copy of the current one.
    JTextArea answer = new JTextArea(theText.getText());
    return answer;
  }
}
