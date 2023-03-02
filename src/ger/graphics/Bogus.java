package ger.graphics;

/*
Show a smiley face or something.

*/

import java.awt.Graphics;
import java.awt.Dimension;


public class Bogus extends RenderDisplay {

  
  public Bogus() {
    this.setPreferredSize(new Dimension(300,300));
  }
  
  protected void paintComponent(Graphics g) {
    
    g.drawOval(10, 10, 200, 200);
  }
  
}
