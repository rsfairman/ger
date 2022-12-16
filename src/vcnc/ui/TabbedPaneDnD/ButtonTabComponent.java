package vcnc.ui.TabbedPaneDnD;

/*

Use this to create an X-looking close button in the tabs of a JTabbedPane.

*/ 


import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;


class ButtonTabComponent extends JPanel {
    
  final JTabbedPane pane;
  
  private JLabel theLabel = null; 

  //public ButtonTabComponent(final JTabbedPane pane,TabCloser closer) {
  public ButtonTabComponent(final JTabbedPane pane) {
    
    // This makes things a little tighter, visually.
    super(new FlowLayout(FlowLayout.LEFT, 0, 0));
    
    if (pane == null)
      throw new NullPointerException("TabbedPane is null");
    
    this.pane = pane;
    setOpaque(false);
    
    //make JLabel read titles from JTabbedPane
    // BUG: Why do people code this way!? The point is to look 
    // up the current tab (this) and use the text as a JLabel.
    // Could be done more cleanly since the title never changes.
    this.theLabel = new JLabel() {
      public String getText() {
        int i = pane.indexOfTabComponent(ButtonTabComponent.this);
        if (i != -1)
          return pane.getTitleAt(i);
        
        return null;
      }
      
      
      
      
    };
    
    add(this.theLabel);
    
    // add more space between the label and the button
    this.theLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
    
    // tab button
    JButton button = new TabButton(this);
    add(button);
    
    //add more space to the top of the component
    setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
  }
  
  public JLabel getLabel() {
    return this.theLabel;
  }
}



class TabButton extends JButton implements ActionListener {
  
  JTabbedPane theTabs = null;
  ButtonTabComponent whichTab;
//  TabCloser theCloser = null;
  
  
  //public TabButton(ButtonTabComponent whichTab,TabCloser closer) {
  public TabButton(ButtonTabComponent whichTab) {
    
    this.theTabs = whichTab.pane;
    this.whichTab = whichTab;
//    this.theCloser = closer;
    
    int size = 17;
    setPreferredSize(new Dimension(size, size));
    
    // BUG: This is obvious. Get rid.
    setToolTipText("close this tab");
    
    //Make the button looks the same for all Laf's
    setUI(new BasicButtonUI());
    
    //Make it transparent
    setContentAreaFilled(false);
    
    //No need to be focusable
    setFocusable(false);
    setBorder(BorderFactory.createEtchedBorder());
    setBorderPainted(false);
    
    //Making nice rollover effect
    //we use the same listener for all buttons
    //addMouseListener(buttonMouseListener);
    addMouseListener(new TabButtonMouseAdapter());
    
    
    // BUG: Do we care about "rollovers?" Think not.
    setRolloverEnabled(true);
    
    // Close the proper tab by clicking the button
    addActionListener(this);
  }

  public void actionPerformed(ActionEvent e) {
    
    int i = theTabs.indexOfTabComponent(this.whichTab);
    
    // BUG: This should be impossible -- i.e. i should never equal -1.
    if (i != -1)
      {
        System.out.println("going away");
        
//        if (this.theCloser != null)
//          theCloser.doClose();
        
        // Note that this *removes* the tab, but it doesn't eliminate all
        // references to the corresponding Component.
        theTabs.remove(i);
      }
  }

  //we don't want to update UI for this button
  public void updateUI() {
    
  }

  //paint the cross
  protected void paintComponent(Graphics g) {
    
    super.  paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    
    //shift the image for pressed buttons
    if (getModel().isPressed())
      g2.translate(1, 1);
    
    g2.setStroke(new BasicStroke(2));
    g2.setColor(Color.BLACK);
    if (getModel().isRollover())
        g2.setColor(Color.MAGENTA);
    
    int delta = 6;
    g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
    g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
    g2.dispose();
  }
}



class TabButtonMouseAdapter extends MouseAdapter {

  // BUG: Get rid of mouseEntered/exited?
  public void mouseEntered(MouseEvent e) {
    
    Component component = e.getComponent();
    if (component instanceof AbstractButton)
      {
        AbstractButton button = (AbstractButton) component;
        button.setBorderPainted(true);
      }
  }

  public void mouseExited(MouseEvent e) {
    
    Component component = e.getComponent();
    if (component instanceof AbstractButton)
      {
        AbstractButton button = (AbstractButton) component;
        button.setBorderPainted(false);
      }
  }
  
}

