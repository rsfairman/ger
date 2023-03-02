package ger.graphics;

/*

To test using a monochrome raster. Actually, I'm not sure Java has a
truly monochrome raster. It may be that everything is  

The basic idea was taken from the ancient version, in flatimage.Cut2DPane

*/

import java.awt.Graphics;
import java.awt.Dimension;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;


public class Bogus2 extends RenderDisplay {

  
  public Bogus2() {
    this.setPreferredSize(new Dimension(300,300));
  }
  
  protected void paintComponent(Graphics g) {
    
    // For a test, just create something here. The goal is for this
    // source image to be fixed, and we just tweak how it is shown.
    
    int width = 500;
    int height = 300;
    byte[] data = new byte[width*height];
    
    // Make a diagonal line. This is white on black since Java allocates
    // the byte array above with all zeros (black). I suppose I could
    // reverse the color map below, but whatever.
    for (int i = 0; i < height; i++)
      data[i*width + i] = (byte) 0xFF; 
    
    DataBufferByte db = new DataBufferByte(data,data.length);
    WritableRaster rast = Raster.createPackedRaster(db,width,height,8,null);
    
    // Create a map from short values to RGB triples. This is the color model.
    // BUG: There may be a gray map built into Java.
    byte[] red = new byte[256];
    byte[] green = new byte[256];
    byte[] blue = new byte[256];
    for (int i = 0; i < 256; i++)
      {
        red[i] = (byte) i;
        green[i] = (byte) i;
        blue[i] = (byte) i;
      }

    IndexColorModel icm = new IndexColorModel(8,256,red,green,blue);
    BufferedImage image = new BufferedImage(icm,rast,false,null);

    g.drawImage(image,0,0,null);
    
    g.setColor(Color.RED);
    g.drawOval(10, 10, 20, 20);
    
    
  }
  
}


