package vcnc.transpile;

// BUG: Why am I basing this on JTextArea? Wouldn't it be better to 
// base it on the underlying String? Same goes for TextBuffer.

// This simplifies (only slightly) the process of getting the contents of
// a JTextArea a few bytes at a time.
// 
// Use this with a JTextArea object to create a buffer so that you can read from
// this object in bites rather than getting the entire contents as a single string.

import javax.swing.JTextArea;


public class TextGetter {
	
	private JTextArea theText = null;
	
	// Where the mark is at.
	private int mark = 0;
	
	// The total length of the text in the JTextArea. The mark can't go any
	// higher than this value (and still make sense).
	private int textLength = 0;
	
	
	public TextGetter() {
		// Do nothing.
	}
	
	public TextGetter(JTextArea theText) {
		this.theText = theText;
		this.mark = 0;
		this.textLength = theText.getDocument().getLength();
		
//		System.out.println("Chars is " +textLength);
	}
	
	public TextGetter spinOff(int n) {
		
		// Create a new TextGetter based on the same underlying JTextArea, but
		// with a different mark.
		TextGetter answer = new TextGetter();
		answer.theText = theText;
		answer.mark = n;
		answer.textLength = textLength;
		return answer;
	}
	
	public char[] read( ) {
		
		// Returns the entire text buffer from the current mark to the end.
		// The only way that I can see to do this is by asking for way more text than
		// could exist.
		//
		// BUG: Is this right? It changes the mark *before* calling read(int).
		this.mark = Integer.MAX_VALUE;
		return read(Integer.MAX_VALUE-1);
	}
	
	public char[] read(int n) {
		
		// Returns n characters from the current mark. If n is too large, then
		// read whatever characters are left. Java is not smart enough to do this
		// automatically. If you request too many characters, then it throws an
		// exception.
		if (n + mark >= textLength)
			n = textLength - mark;
		char[] answer = null;
		try	{
			answer = (theText.getText(this.mark,n)).toCharArray();
		} catch (Exception e) {
			// This might happen if we try to read text that doesn't exist.
			return null;
		}
		
		this.mark += n;
		return answer;
	}
	
	public void setMark(int n) {
		
		// Move mark so that the next character to be read is at the n-th position.
		// The mark should be at zero to get the first character.
		this.mark = n;
	}
}
