package ger.graphics;

/*

Procedurally, this is something like the Translator, except that it
converts a fully digested series of Statements into something that can
be graphically rendered.

*/
import java.util.ArrayList;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import ger.tpile.CartCoord;
import ger.tpile.DefaultMachine;
import ger.tpile.Statement;
import ger.tpile.parse.DataMove;
import ger.tpile.RenderPacket;
import ger.util.LLNode;
import ger.util.LList;

import static ger.tpile.SType.*;


public class Renderer {

  private static ArrayList<IntCoord> stepLineOLD(int ix,int iy,int iz,
      double x0,double y0,double z0, double x1,double y1,double z1,double s) {
    ;
    // Return a series of values for stepping from (x0,y0,z0) to (x1,y1,z1)
    // in steps of size s (the scale). 
    // This is the point at which the moves are quantized -- that is, converted
    // from the continuous world of doubles to the discrete world of ints.
    // It is assumed that (x0,y0,z0) rounds to (ix,iy,iz), and that the
    // steps are to be given relative to that position. In theory, providing
    // both (ix,iy,iz) and (x0,y0,z0) is redundant, but it seems less likely
    // to lead to issues with rounding error.
    
    // BUG: Maybe it shouldn't be redundant.
    // Better would be to take (x0,y0,z0) as ints? Or maybe make them
    // all ints and assume that the caller has dealt with rounding?
    
    ArrayList<IntCoord> answer = new ArrayList<>();
    
    // The algorithm is something like Bresenham's algorithm, but simplified.
    // If we are at a given (x,y,z), in integer terms, then the next position
    // should be one of (x+/-1,y+/-1,z+-/1) and we take whichever one of these
    // eight choices is "most needy."
    //
    // BUG: A true Bresenham is probably faster, but it's more fiddly.
    //
    // BUG: Yes, I am doing this with Bresenham, so there is no point
    // in dealing with doubles.
//    int ix2
    
    
    
    
    
    
    return answer;
  }
  
  private static ArrayList<IntCoord> stepLine(int x1,int y1,int z1,
      int x2,int y2,int z2) {
    ;
    // Return a series of values for stepping from (x1,y1,z1) to (x2,y2,z2).
    
    // BUG: THis is another thing that belongs in some general graphics 
    // class and/or package.
    //
    // NOTE: This could be made faster by breaking in into multiple octants
    // so that there's no if-statement in the inner loop.
    
    ArrayList<IntCoord> answer = new ArrayList<>();
    
    // A brief form of Brensenham's algorithm.
    int dx = Math.abs(x2 - x1);
    int dy = Math.abs(y2 - y1);
    int dz = Math.abs(z2 - z1);
    
    int xs;
    int ys;
    int zs;
    
    if (x2 > x1) xs = 1; else xs = -1;
    if (y2 > y1) ys = 1; else ys = -1;
    if (z2 > z1) zs = 1; else zs = -1;
 
    if (dx >= dy && dx >= dz)
      {
        // dx is biggest, so x-axis is what drives things.
        int p1 = 2 * dy - dx;
        int p2 = 2 * dz - dx;
        
        while (x1 != x2)
          {
            x1 += xs;
            if (p1 >= 0)
              {
                y1 += ys;
                p1 -= 2 * dx;
              }
            if (p2 >= 0)
              {
                z1 += zs;
                p2 -= 2 * dx;
              }
            
            p1 += 2 * dy;
            p2 += 2 * dz;
            
            answer.add(new IntCoord(x1, y1, z1));
        }
      } 
    else if (dy >= dx && dy >= dz)
      {
        // dy is is biggest, so y-axis is the driver.
        int p1 = 2 * dx - dy;
        int p2 = 2 * dz - dy;
        while (y1 != y2)
          {
            y1 += ys;
            if (p1 >= 0)
              {
                x1 += xs;
                p1 -= 2 * dy;
              }
            if (p2 >= 0)
              {
                z1 += zs;
                p2 -= 2 * dy;
              }
            
            p1 += 2 * dx;
            p2 += 2 * dz;
            
            answer.add(new IntCoord(x1, y1, z1));
          }
      }
    else 
      {
        // z-axis must be the driver.
        int p1 = 2 * dy - dz;
        int p2 = 2 * dx - dz;
        while (z1 != z2)
          {
            z1 += zs;
            if (p1 >= 0)
              {
                y1 += ys;
                p1 -= 2 * dz;
              }
            if (p2 >= 0)
              {
                x1 += xs;
                p2 -= 2 * dz;
              }

            p1 += 2 * dy;
            p2 += 2 * dx;
            
            answer.add(new IntCoord(x1, y1, z1));
          }
      }
    
    return answer;
  }
  
  public static int getFlatPathSize(RenderPacket rPacket) {
    
    // Return the area (in pixels) of the image.
    // The caller can use this to check whether the size is reasonable
    // before actually rendering.
    
    // BUG: This code is exactly as when rendering, which is not DRY.
    
    double unitWidth = rPacket.xRight - rPacket.xLeft;
    double unitHeight = rPacket.yTop - rPacket.yBot;
    
    double scale = DefaultMachine.scale;
    
    int width = (int) Math.round(unitWidth/scale);
    int height = (int) Math.round(unitHeight/scale);
    
    return width*height;
  }
  
  public static RenderFlat flatPathRender(RenderPacket rPacket) {
    
    // The simplest method of rendering. The output is monochrome, only the
    // location of the tool is tracked (cutter of radius one pixel) and
    // there's a cut whenever Z < 0.
    
    double scale = DefaultMachine.scale;
    
    double unitWidth = rPacket.xRight - rPacket.xLeft;
    double unitHeight = rPacket.yTop - rPacket.yBot;
    
    // BUG: Do I need to add 1? Doing it for safety so that I don't go outside
    // the array bounds.
    int width = (int) Math.round(unitWidth/scale); // + 1;
    int height = (int) Math.round(unitHeight/scale); // + 1;

    int leftMarg = (int) Math.round(Math.abs(rPacket.xLeft) / scale);
    int botMarg = (int) Math.round(Math.abs(rPacket.yBot) / scale);
    
    byte[] data = new byte[width*height];
    
    // Make a diagonal line. This is white on black since Java allocates
    // the byte array above with all zeros (black). I suppose I could
    // reverse the color map below, but whatever.
//    for (int i = 0; i < height; i++)
//      data[i*width + i] = (byte) 0xFF;
    
    // This ignores everything but tool position since the cutter is only
    // a single pixel.
    int curX = 0;
    int curY = 0;
    int curZ = 0;
    
    LLNode<Statement> curNode = rPacket.code.head;
    while (curNode != null)
      {
        // Only two types of Statement are possible: MOVE and M06, and
        // we ignore M06 (tool change). The particular tool doesn't matter
        // for this form of rendering.
        Statement s = curNode.data;
        if (s.type != MOVE)
          {
            curNode = curNode.next;
            continue;
          }
        
        // We have a linear move from (curX,curY,curZ) to the given coordinate.
        DataMove m = (DataMove) s.data;
        
        int nextX;
        int nextY;
        int nextZ;
        if (m.xDefined) nextX = (int) Math.round(m.xValue/scale); else nextX = curX;
        if (m.yDefined) nextY = (int) Math.round(m.yValue/scale); else nextY = curY;
        if (m.zDefined) nextZ = (int) Math.round(m.zValue/scale); else nextZ = curZ;
        
        ArrayList<IntCoord> p = stepLine(curX,curY,curZ,nextX,nextY,nextZ);
        for (IntCoord c : p)
          {
            if (c.z < 0)
              // This is confusing. G-code uses a normal coordinate system, but
              // Java uses an upside-down system, so set the pixel as you 
              // step up (or is it down?) from the edge at "height." 
              //data[(c.x+leftMarg)*width + c.y + botMarg] = (byte) 0xFF;
              //data[(c.x)*width + c.y ] = (byte) 0xFF;
              data[(height - botMarg - c.y)*width + c.x + leftMarg ] = (byte) 0xFF;
          }
        
        // Next move starts here.
        curX = nextX;
        curY = nextY;
        curZ = nextZ;
        
        curNode = curNode.next;
      }
    
    DataBufferByte db = new DataBufferByte(data,data.length);
    WritableRaster rast = Raster.createPackedRaster(db,width,height,8,null);
    
    // Create a map from short values to RGB triples. This is the color model.
    // BUG: There many be a gray map built into Java.
    byte[] red = new byte[256];
    byte[] green = new byte[256];
    byte[] blue = new byte[256];
    for (int i = 0; i < 256; i++)
      {
        // Swap the order so that 0xFF is black instead of white.
        red[i] = (byte) (255-i);
        green[i] = (byte) (255-i);
        blue[i] = (byte) (255-i);
      }

    IndexColorModel icm = new IndexColorModel(8,256,red,green,blue);
    BufferedImage image = new BufferedImage(icm,rast,false,null);

    
    
    return new RenderFlat(image);
    
    
  }
}
