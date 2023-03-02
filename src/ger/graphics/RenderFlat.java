package ger.graphics;

/*

Base class for rendering with "flat" output, meaning that it is 2D.

In 2D, to zoom, use the mouse wheel or Ctrl +/-. To pan, click and
drag with the mouse, ignoring the shift key. There shouldn't be a scroll bar.
 

*/


import java.awt.Color;
import java.awt.Graphics;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import java.awt.image.BufferedImage;




public class RenderFlat extends RenderDisplay implements 
    MouseWheelListener,KeyListener,MouseListener,MouseMotionListener {
  ;
  
  private BufferedImage image = null;
  
  // The number of "notches" the mouse wheel has rotated or the number
  // of times Ctrl +/- has been typed. Negative values mean "zoom in" and
  // positive values mean "zoom out."
  private int zoomFactor = 0;
  
  // Whether the mouse was clicked, so that we're dragging, and where clicked.
  private boolean dragging = false;
  private int initialX = 0;
  private int initialY = 0;
  
  // The location to which the image has been panned: the coordinates of the
  // top-left corner of the image relative to the window.
  private int destX = 0;
  private int destY = 0;
  
  // When dragging, these are non-zero and act as an adjustment to destX/Y.
  private int deltaX = 0;
  private int deltaY = 0;
  
  
  public RenderFlat(BufferedImage image) {
    
    this.image = image;
    
    this.setBackground(Color.BLACK);
    
    this.setFocusable(true);
    
    this.addMouseWheelListener(this);
    this.addMouseListener(this);
    this.addMouseMotionListener(this);
    
    // NOTE: Using "key bindings" instead may be easer than a listener. 
    // see https://docs.oracle.com/javase/tutorial/uiswing/misc/keybinding.html
    this.addKeyListener(this);
    
    this.requestFocusInWindow();
  }
  
  private double getScale() {
    
    // Convert this.zoomFactor to a scale.

    // This is expressed in a tedious way, but being tedious allows it to be 
    // easily tweaked. Using something like a square root would work too,
    // but then you're locked into a single particular growth rate.
    // I guess I could just multiply or divide by something like 1.1^n.
    // BUG: Decide on this.
    if (zoomFactor < -11)
      zoomFactor = -11;
    if (zoomFactor > 10)
      zoomFactor = 10;

    double scale;
    if (zoomFactor == -11) scale = 0.05;
    else if (zoomFactor == -10) scale = 0.075;
    else if (zoomFactor == -9) scale = 0.10;
    else if (zoomFactor == -8) scale = 0.20;
    else if (zoomFactor == -7) scale = 0.30;
    else if (zoomFactor == -6) scale = 0.40;
    else if (zoomFactor == -5) scale = 0.50;
    else if (zoomFactor == -4) scale = 0.60;
    else if (zoomFactor == -3) scale = 0.70;
    else if (zoomFactor == -2) scale = 0.80;
    else if (zoomFactor == -1) scale = 0.90;
    else if (zoomFactor == 0) scale = 1.0;
    else if (zoomFactor == 1) scale = 1.10;
    else if (zoomFactor == 2) scale = 1.25;
    else if (zoomFactor == 3) scale = 1.50;
    else if (zoomFactor == 4) scale = 1.75;
    else if (zoomFactor == 5) scale = 2.0;
    else if (zoomFactor == 6) scale = 3.0;
    else if (zoomFactor == 7) scale = 4.0;
    else if (zoomFactor == 8) scale = 5.0;
    else if (zoomFactor == 9) scale = 7.0;
    else scale = 10.0;
    
    return scale;
  }
  
  protected void paintComponent(Graphics g) {
    
    // It was tempting to use an AffineTransform so that I could work with a
    // right-handed coordinate system everywhere, but suspect that it will be 
    // more trouble than it's worth. If I do that, then remember (see Java 
    // docs), you're not supposed to call setTransform() directly, except for 
    // saving and restoring. It would go something like this:
    // 
    // Graphics2D g2 = (Graphics2D) g;
    // AffineTransform saveAT = g2.getTransform();
    // g2d.transform(...);
    // g2d.draw(...);
    // g2d.setTransform(saveAT);
    
    // Double-buffer to reduce flicker when dragging.
    BufferedImage offscreenBuf = new BufferedImage(
        this.getWidth(),this.getHeight(),BufferedImage.TYPE_INT_RGB);
    Graphics offscreenG = offscreenBuf.getGraphics();
    offscreenG.setColor(Color.BLACK);
    offscreenG.fillRect(0,0,this.getWidth(),this.getHeight());
    
    // Translation is easy, but scaling is not as easy.
    // The delta values are used while actively dragging/panning, but are
    // zero otherwise.
    offscreenG.translate(destX+deltaX,destY+deltaY);
    
    double scale = getScale();
    int srcW = image.getWidth();
    int srcH = image.getHeight();
    
    // BUG: I like that it's blocky when zooming in, but when scaling
    // out (to make the image smaller) you lose detail. It might be that 
    // Image.getScaledInstance() is the way to go. I really don't want to
    // write my own copybits() (for the zillionth time).
    
    // There are many drawImage() methods. This one maps the entire source
    // image to the given rectangle. So we are taking the src image and scaling
    // it to the given rectangle and displaying at (0,0) -- after having
    // changed the origin with Graphics.translate().
    offscreenG.drawImage(image,
        0,0,
        (int) Math.round(srcW * scale),(int) Math.round(srcH * scale),
        null);
    
    g.drawImage(offscreenBuf,0,0,null);
  }
  
  private void limitPan() {
    
    // Tweak destX and destY so that the image isn't panned beyond
    // the edge of the window.so large that the image is pushed beyond the
    // edge of the window.
    // It is assumed that deltaX and deltaY are irrelevant (are zero).
    double scale = getScale();
    int srcW = (int) Math.round(scale * image.getWidth());
    int srcH = (int) Math.round(scale * image.getHeight());
    
    // The number of pixels that must remain visible, expressed as pixels
    // in the window, not pixels of the src image.
    // BUG: Make a static final in the class.
    int requiredMargin = 30;
    
    if (destX > this.getWidth() - requiredMargin)
      destX = this.getWidth() - requiredMargin;
    if (destX < -srcW + requiredMargin)
      destX = -srcW + requiredMargin;
    if (destY > this.getHeight() - requiredMargin)
      destY = this.getHeight() - requiredMargin;
    if (destY < -srcH + requiredMargin)
      destY = -srcH + requiredMargin;
  }
  
  private void changeZoom(int delta) {
    
    // Change the magnification factor. We may need to adjust the location of 
    // the image in the window (the "pan") so that the image "zooms from the 
    // center" instead of jumping around. Whatever point of the image is 
    // currently in the center of the window should remain in the center, with 
    // the caveat that zooming in may not move the image beyond the edge of 
    // the window.
    //
    // This assumes deltaX = deltaY = 0, so not actively dragging/panning. 
    
    // Determine the coordinates of the point in the src image that
    // appears in the center of the window. This point need not actually
    // be on the image, but it is relative to that origin.
    //
    // So we need a map between the coordinates relative to the window and
    // the coordinates relative to the src image.
    double cx = this.getWidth() / 2.0;
    double cy = this.getHeight() / 2.0;
    
    // Coordinates relative to the src image, but not yet scaled.
    double ix = cx - destX;
    double iy = cy - destY;
    
    // And scaled.
    // Overall, the map is
    // window  <--> src image
    // (wx,wy) <--> ( (wx - destX)/s, (wy-destY)/s )
    double s = getScale();
    double sx = ix / s;
    double sy = iy / s;
    
    // Change the zoom.
    this.zoomFactor += delta;
    
    // Now we may need to change destX, destY. The point is that we
    // want ( (cx - destX)/s, (cy-destY)/s ) to equal (sx,sy), using the
    // new value for s, but the values for sx and sy from above.
    // Solve this for destX and destY to obtain:
    s = getScale();
    destX = (int) Math.round(cx - s * sx);
    destY = (int) Math.round(cy - s * sy); 
    
    // Don't let zooming "pan the image away."
    limitPan();
  }
  
  
  public void mouseWheelMoved(MouseWheelEvent e) {
    
    // In theory, the wheel rotation is the number of clicks or detents.
    changeZoom(e.getWheelRotation());
    
    // See commentary below, with dragging.
    paintComponent(this.getGraphics());
  }
  
  public void mouseEntered(MouseEvent e) { ;; }
  public void mouseExited(MouseEvent e) { ;; }
  public void mouseClicked(MouseEvent e) { ;; }
  
  public void mousePressed(MouseEvent e) {
    
    // Don't care about shift or control key when in 2D.
    // Any mouse click drags (pans) the image.
    this.dragging = true;
    this.initialX = e.getX();
    this.initialY = e.getY();
  }
  
  public void mouseReleased(MouseEvent e) {
    
    // Similar to mouseDragged(), with some extra cleanup.
    int curX = e.getX();
    int curY = e.getY();

    deltaX = curX - initialX;
    deltaY = curY - initialY;
    
    destX += deltaX;
    destY += deltaY;
    
    deltaX = 0;
    deltaY = 0;
    
    // Don't allow destX/Y to be so large that the image is pushed beyond the
    // edge of the window.
    limitPan();
    
    // repaint is fine here (unlike while actively dragging).
    this.repaint();
  }

  public void mouseDragged(MouseEvent e) {
    
    int curX = e.getX();
    int curY = e.getY();
    
    deltaX = curX - initialX;
    deltaY = curY - initialY;
    
    // The event dispatch thread complicates things.
    // You can't just call repaint() or paintImmediately().
    // 
    // In particular, this won't work because it means "repaint whenever it's 
    // convenient"
    // this.repaint();
    // 
    // This doesn't work either. It invokes it "later" instead of right now, 
    // and SwingUtilities.invokeAndWait() can't be used either because you 
    // can't call that method from the event dispatch thread (which we are in).
    // SwingUtilities.invokeLater(new Runnable() {
    //  public void run() {
    //    repaint(); // or below; neither will work
    //    paintImmediately(getVisibleRect());
    //  }
    // });
    
    // So just do it directly, sort of "immediate mode GUI" style.
    paintComponent(this.getGraphics());
  }
  
  public void mouseMoved(MouseEvent e) {
    // Don't care.
  }
  
  
  public void keyPressed(KeyEvent e) { 
    
    if (e.isControlDown() == false)
      return;
    
    char c = e.getKeyChar();
    int i = e.getKeyCode();
    
    if (c == '+')
      changeZoom(1);
    else if (c == '-')
      changeZoom(-1);
    else if (i == KeyEvent.VK_EQUALS)
      changeZoom(1);
    else if (i == KeyEvent.VK_MINUS)
      changeZoom(-1);
    else
      return;

    // See commentary with dragging event.
    paintComponent(this.getGraphics());
  }
  
  public void keyReleased(KeyEvent e) { ;; }
  
  public void keyTyped(KeyEvent e) {
    
    // I don't understand why, but typing ctrl-anything-other-than-a-letter
    // never comes here as a key *typed* event. So you can't catch
    // things like Ctrl-'+' here. They do arrive as key pressed events.
  }
}
