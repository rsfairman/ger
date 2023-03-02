package ger.ui.LineNumbers;

/*
This class will display line numbers for a related text component. The text
component must use the same line height for each line. TextLineNumber
supports wrapped lines and will highlight the line number of the current
line in the text component.

This class was designed to be used as a component added to the row header
of a JScrollPane.

Version History

01 Only modest changes from some example code I found.

02 The original is has lots of things I don't need: caret listener, the
   use of a JTextPane, and maybe other things. Try to get rid of them and see
   if it's simpler.
   
03 Mostly, I just cleaned things up in v02, although it's clear that the 
   caret listener isn't doing anything critical. Get rid of that, plus
   the coloring of the "active" line number.

04 I don't need/want to use JTextPane as the basis. I want JTextArea.
   It's simpler and I have no need for the kind of "styling" that JTextPane
   provides.
   
   In fact, this thing is agnostic about which kind of JTextComponent is 
   being used; JTextArea will work fine.
   
   Added the fact that this now knows about being toggled. There could be
   a TextLineNumber object allocated and ready to go, even though the line
   numbers aren't actually displayed. So, to use this, one would normally
   allocated it in the user's constructor, then turn it on and off as needed.
   Alternatively, I could have assumed that the user would allocated and
   deallocate this entire thing whenever display of line numbers is toggled.
   The problem with that approach is that you'd also have to track the 
   listeners so that they could be deallocated by the caller -- the caller
   would need some kind of dispose() method for this class. 
   
 
 BUG: This thing could be cleaned up a little more.
 For one thing, the way toggling works, the user still needs to toggle
 the actual *display* of the line numbers, even if the toggler here deals
 with routing any events. See GInputTab; it shouldn't need to mess with 
 setRowHeaderView().
 
 There are other minor things to tidy too.
 
*/

import java.awt.*;
import java.beans.*;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;


public class TextLineNumber extends JPanel implements DocumentListener, 
	PropertyChangeListener {
  ;  
  // BUG: Are these used?
  public final static float LEFT = 0.0f;
  public final static float CENTER = 0.5f;
  public final static float RIGHT = 1.0f;
  
  private final static Border OUTER = new MatteBorder(0,0,0,2,Color.GRAY);
    
  private final static int HEIGHT = Integer.MAX_VALUE - 1000000;
  
	// Text component this TextTextLineNumber component is in sync with
	// Note that JTextArea extends the (abstract) JTextComponent class,
	// as does JEditorPane which is in, in turn, extended by JTextPane.
	private JTextComponent component;

	// updateFont is (?) whether to update the font used for line numbers
	// when the text font is changed.
	private boolean updateFont;
	
	// Gap on each side of the line numbers. Should be small, like 5.
	private int borderGap;
	
	// Digits can be aligned LEFT RIGHT or CENTER (see above).
	// BUG: It's hard to see wanting anything other than right.
	private float digitAlignment;
	
	// Number of digits for which to leave space.
	private int minimumDisplayDigits;

	//  Keep history information to reduce the number of times the component
	//  needs to be repainted.
  private int lastDigits;
  private int lastHeight;
  
  // Whether the line numbers are currently being displayed. Users should 
  // call toggleLineNumbers() to adjust this.
  private boolean numbersDisplayed = false;
  

  private HashMap<String,FontMetrics> fonts;
  
    
	public TextLineNumber(JTextComponent component) {
	  
	  // Constructor that defaults to using 6 digits (which *is* a lot).
		this(component,6);
	}

	public TextLineNumber(JTextComponent component,int minimumDisplayDigits) {
	  
		this.component = component;

		setFont(component.getFont());

		setBorderGap(5);
		setDigitAlignment(RIGHT);
		setMinimumDisplayDigits(minimumDisplayDigits);
	}
  	
	public void toggleLineNumbers() {
	  
	  // Toggle display of the line numbers.
	  // I am a little surprised that this works. I'd have thought that it 
	  // would be necessary to recalculate the line numbers every time they 
	  // are toggled to "on." Maybe I should be firing a PropertyChangeEvent 
	  // or DocumentEvent so that it's recalculated -- but it does work now. 
	  if (this.numbersDisplayed == true)
	    {
	      numbersDisplayed = false;
        component.getDocument().removeDocumentListener(this);
        component.removePropertyChangeListener("font", this);
	    }
	  else
	    {
	      numbersDisplayed = true;
        component.getDocument().addDocumentListener(this);
        component.addPropertyChangeListener("font",this);
	    }
	}
	
	public boolean getUpdateFont() {
		return updateFont;
	}

  public void setUpdateFont(boolean updateFont) {
  	this.updateFont = updateFont;
  }
  
	public int getBorderGap() {
		return borderGap;
	}

  public void setBorderGap(int borderGap) {
    
  	this.borderGap = borderGap;
  	Border inner = new EmptyBorder(0,borderGap,0,borderGap);
  	setBorder(new CompoundBorder(OUTER,inner));
  	lastDigits = 0;
  	setPreferredWidth();
  }
    
	public float getDigitAlignment() {
		return digitAlignment;
	}

  public void setDigitAlignment(float digitAlignment) {
  	this.digitAlignment =
  		digitAlignment > 1.0f ? 1.0f : digitAlignment < 0.0f ? -1.0f : digitAlignment;
  }
  
	public int getMinimumDisplayDigits() {
		return minimumDisplayDigits;
	}

	public void setMinimumDisplayDigits(int minimumDisplayDigits)	{
		this.minimumDisplayDigits = minimumDisplayDigits;
		setPreferredWidth();
	}
	
	private void setPreferredWidth() {
	  
	  // Calculate the width needed to display the maximum line number
		Element root = component.getDocument().getDefaultRootElement();
		int lines = root.getElementCount();
		int digits = Math.max(String.valueOf(lines).length(), minimumDisplayDigits);

		//  Update sizes when number of digits in the line number changes
		if (lastDigits != digits)
  		{
  			lastDigits = digits;
  			FontMetrics fontMetrics = getFontMetrics( getFont() );
  			int width = fontMetrics.charWidth( '0' ) * digits;
  			Insets insets = getInsets();
  			int preferredWidth = insets.left + insets.right + width;
  
  			Dimension d = getPreferredSize();
  			d.setSize(preferredWidth, HEIGHT);
  			setPreferredSize( d );
  			setSize( d );
  		}
	}

	public void paintComponent(Graphics g) {
	  
		super.paintComponent(g);

		//	Determine the width of the space available to draw the line number
		FontMetrics fontMetrics = component.getFontMetrics( component.getFont() );
		Insets insets = getInsets();
		int availableWidth = getSize().width - insets.left - insets.right;

		//  Determine the rows to draw within the clipped bounds.
		Rectangle clip = g.getClipBounds();
		int rowStartOffset = component.viewToModel( new Point(0, clip.y) );
		int endOffset = component.viewToModel( new Point(0, clip.y + clip.height) );

		while (rowStartOffset <= endOffset)
  		{
  			try {
    			//  Get the line number as a string and then determine the
    			//  "X" and "Y" offsets for drawing the string.
    			String lineNumber = getTextLineNumber(rowStartOffset);
    			int stringWidth = fontMetrics.stringWidth( lineNumber );
    			int x = getOffsetX(availableWidth, stringWidth) + insets.left;
    			int y = getOffsetY(rowStartOffset, fontMetrics);
    			g.drawString(lineNumber, x, y);

    			//  Move to the next row
    			rowStartOffset = Utilities.getRowEnd(component, rowStartOffset) + 1;
  			}	catch(Exception e) {
  			  break;
  			}
  		}
	}
  	
	protected String getTextLineNumber(int rowStartOffset) {
	 
	  // The line number to be drawn. Return an empty string if the line of 
	  // text has wrapped.
	  // BUG: Do we care about this?
		Element root = component.getDocument().getDefaultRootElement();
		int index = root.getElementIndex( rowStartOffset );
		Element line = root.getElement( index );

		if (line.getStartOffset() == rowStartOffset)
			return String.valueOf(index + 1);
		else
			return "";
	}
	
	private int getOffsetX(int availableWidth, int stringWidth) {
	  
	  // Determine the X offset to properly align the line number when drawn 
		return (int)((availableWidth - stringWidth) * digitAlignment);
	}
  	
	private int getOffsetY(int rowStartOffset, FontMetrics fontMetrics)
		throws BadLocationException	{
		;
		// Determine the Y offset for the current row.
		
		// Get the bounding rectangle of the row
		Rectangle r = component.modelToView( rowStartOffset );
		int lineHeight = fontMetrics.getHeight();
		int y = r.y + r.height;
		int descent = 0;

		//  The text needs to be positioned above the bottom of the bounding
		//  rectangle based on the descent of the font(s) contained on the row.
		if (r.height == lineHeight)
		  // default font is being used
		  descent = fontMetrics.getDescent();
		else  
  		{
  		  // We need to check all the attributes for font changes
  			if (fonts == null)
  				fonts = new HashMap<String, FontMetrics>();
  
  			Element root = component.getDocument().getDefaultRootElement();
  			int index = root.getElementIndex( rowStartOffset );
  			Element line = root.getElement( index );
  
  			for (int i = 0; i < line.getElementCount(); i++)
    			{
    				Element child = line.getElement(i);
    				AttributeSet as = child.getAttributes();
    				String fontFamily = (String)as.getAttribute(StyleConstants.FontFamily);
    				Integer fontSize = (Integer)as.getAttribute(StyleConstants.FontSize);
    				String key = fontFamily + fontSize;
    
    				FontMetrics fm = fonts.get( key );
    
    				if (fm == null)
      				{
      					Font font = new Font(fontFamily, Font.PLAIN, fontSize);
      					fm = component.getFontMetrics( font );
      					fonts.put(key, fm);
      				}
    
    				descent = Math.max(descent, fm.getDescent());
    			}
  		}

		return y - descent;
	}

	// Implement DocumentListener interface
	public void changedUpdate(DocumentEvent e) { documentChanged();	}
	public void insertUpdate(DocumentEvent e) {	documentChanged(); }
	public void removeUpdate(DocumentEvent e)	{	documentChanged(); }

	private void documentChanged() {
	  
	  // A document change may affect the number of displayed lines of text.
	  // Therefore the lines numbers will also change.
	  
		//  View of the component has not been updated at the time
		//  the DocumentEvent is fired
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run() {
				try {
					int endPos = component.getDocument().getLength();
					Rectangle rect = component.modelToView(endPos);

					if (rect != null && rect.y != lastHeight)
					{
						setPreferredWidth();
						getParent().repaint();
						lastHeight = rect.y;
					}
				}	catch (BadLocationException ex) { 
				  /* nothing to do */
				}
			}
		});
	}

	public void propertyChange(PropertyChangeEvent evt) {

	  // Implement PropertyChangeListener interface.
	  // The only "property change" we listen for is a change to the font.
	  // BUG: Not tested what happens when the font changes.
		if (evt.getNewValue() instanceof Font)
  		{
  			if (updateFont)
    			{
    				Font newFont = (Font) evt.getNewValue();
    				setFont(newFont);
    				lastDigits = 0;
    				setPreferredWidth();
    			}
  			else
  			  getParent().repaint();
  		}
	}
	

  	/*
  public static void main(String[] args) {
	  
	  // Test code
	  System.out.println("Started");
	  JTextPane textPane = new JTextPane();
    JScrollPane scrollPane = new JScrollPane(textPane);
    TextLineNumber tln = new TextLineNumber(textPane);
    scrollPane.setRowHeaderView( tln );
    
    
    JFrame f = new JFrame("something");
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.getContentPane().add(scrollPane);
    f.setSize(400, 300);
    f.setLocationRelativeTo(null);
    f.setVisible(true);
	}
	*/
}



