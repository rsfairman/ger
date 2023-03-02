package ger.graphics;


/*

The base class for rendering.

BUG: This is used as a base class for RenderFlat, and that may be 
pointless since there won't (?) be any other sub-classes.

*/

import java.awt.Graphics;



import javax.swing.JComponent;

public abstract class RenderDisplay extends JComponent {

  abstract protected void paintComponent(Graphics g);
  
}
