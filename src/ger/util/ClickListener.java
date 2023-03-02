package ger.util;

// To find out if the user clicked. This could be used to wait for the
// user to click the mouse before doing something.
//
// BUG: Trash?

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

public class ClickListener implements MouseListener {

	private static boolean clicked = false;

	// We don't care about these.
	public void mouseEntered(MouseEvent e) { ;;; }
	public void mouseExited(MouseEvent e) { ;;; }
	public void mousePressed(MouseEvent e) { ;;; }
	public void mouseReleased(MouseEvent e) { ;;; }
	
	
	public void mouseClicked(MouseEvent e) {
		clicked = true;
	}
	
	public boolean clicked() {
		
		if (clicked == true)
			{
				clicked = false;
				return true;
			}
		else
			return false;
	}
	
}
