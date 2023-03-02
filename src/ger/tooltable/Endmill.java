package ger.tooltable;

//import rsf.util.ArraysUtil;
//import voxelframe.VoxelSet;


public class Endmill extends Tool {
	
	// The Tool class suffices to define the features of an endmill. 
	
	public Endmill(double diameter) {
		this.diameter = diameter;
		this.D = diameter;
	}
	
	
	// This is what defines a particular end-mill.
	// NOTE: I am not considering the number of cutters, the material from which
	// it is made or other things that might be of practical interest.
//	public int diameter = 10;
	
	// BUG: Testing. It counts the number of times that testCube() is called.
//	private int testCount = 0;
	

	/*
	private boolean ptInTool(int[] testP,int[] toolP) {
		
		// Return true iff testP is inside the tool at toolP. 
		// BUG: It is assumed that the tool extends upwards to infinity.
		if (testP[2] < toolP[2])
			return false;
		
		// Need to be a little fussy here about the fact that coordinates
		// are rounded to integers.
		// BUG: I'm not sure that adding 0.50 here is right.
		double sqdist = (testP[0] + 0.50 - toolP[0])*(testP[0] + 0.50 - toolP[0]) +
											(testP[1] + 0.50 - toolP[1])*(testP[1] + 0.50  - toolP[1]);
		
		if (sqdist <= (diameter*diameter/4))
			return true;
		else
			return false;
	}
	*/
	
	/*
	 
  This was originally implemented in the context of octrees, and that does not seem to
	be the way this is headed. This method was required by the Tool super-class. 

	public CubeStatus testCube(int size,int[] corner,int[] toolp) {
		
		// BUG: testing.
		++testCount;
		if (testCount % 1000 == 0)
			System.out.println("Num cube visits: " +testCount);
		if (testCount > 1000000)
			return CubeStatus.Empty;
		
		// See if this cube is inside the given tool position for an end mill.
		// 
		// The essence of the test is this.
		// * If all eight vertices are in the tool volume, then the entire cube is too.
		//   That's where convexity of the tool is assumed.
		// * If any one of the eight vertices are in the tool volume, but not all eight,
		//   then the cube is partially contained.
		// * If none of the vertices are inside the tool, then it could still happen
		//   that there is partial containment. This is the case if all four sides of
		//   the cube have their inward pointing normals pointing to the center of the tool.
		//   A simpler way to express this test is to compare the x and y coordinates of
		//   the center of the tool to the x and y coordinates of the plane of each face.
		//   Actually, this doesn't work. You could have a circle with a square, all four
		//   of whose vertices are outside the circle, and with the circle just crossing
		//   the edge of the square in the center of that side of the square.
		//   NOTE: This assumes that the CNC machine has only three axes since I am not
		//   considering the z-axis.
		// BUG: This is done in a very heirarchical manner. That makes it easy to
		// understand the reasoning, but there is a lot of redundance in what's going on.
		// For instance, various vertices may be tested for inclusion multiple times as
		// the code recurses.

		// BUG: This whole method of testing seems pretty ramshackle to me. There
		// must be a more efficient algorithm. Furthermore, I assume throughout that
		// this is not a four (or more) axis machine.
		
		// First test (just to be sure): is the bottom of the tool at least as low as the
		// top of the cube? 
		// BUG: This test should have been done earlier, so this is (?) redundant.
		if (toolp[2] > corner[2]+size)
			return CubeStatus.Empty;
		
		// See if the (x,y)-distance of the tool from the center of the cube is more than
		// (1/2) (sqrt(2) size) + radius. If it is, then the tool can't intersect the cube.
		// This test isn't strictly necessary given the tests below, but it is fast and 
		// should eliminate lots of trivial cases.
		int s = size;
		int cx = corner[0] + s/2;
		int cy = corner[1] + s/2;
		double xyDist = Math.sqrt((cx - toolp[0]) * (cx - toolp[0]) +
											(cy - toolp[1]) * (cy - toolp[1]));
		if (xyDist > (Math.sqrt(2) * size + diameter) / 2.0)
			return CubeStatus.Empty;
		
		
		// Test the eight vertices. This can't easily be done in a loop.
		boolean[] vtest = new boolean[8];
		int[] v = new int[3];
		
		v[0] = corner[0]; v[1] = corner[1]; v[2] = corner[2];
		vtest[0] = ptInTool(v,toolp);
		
		v[0] = corner[0]; v[1] = corner[1]+s; v[2] = corner[2];
		vtest[1] = ptInTool(v,toolp);
		
		v[0] = corner[0]+s; v[1] = corner[1]; v[2] = corner[2];
		vtest[2] = ptInTool(v,toolp);
		
		v[0] = corner[0]+s; v[1] = corner[1]+s; v[2] = corner[2];
		vtest[3] = ptInTool(v,toolp);

		v[0] = corner[0]; v[1] = corner[1]; v[2] = corner[2]+s;
		vtest[4] = ptInTool(v,toolp);
		
		v[0] = corner[0]; v[1] = corner[1]+s; v[2] = corner[2]+s;
		vtest[5] = ptInTool(v,toolp);
		
		v[0] = corner[0]+s; v[1] = corner[1]; v[2] = corner[2]+s;
		vtest[6] = ptInTool(v,toolp);
		
		v[0] = corner[0]+s; v[1] = corner[1]+s; v[2] = corner[2]+s;
		vtest[7] = ptInTool(v,toolp);
		
		// If they are all inside the tool, then done.
		boolean inside = true;
		for (int i = 0; i < 8; i++)
			{
				if (vtest[i] == false)
					{
						inside = false;
						break;
					}
			}
		
		if (inside == true)
			return CubeStatus.Solid;
		
		// Got here, so either Empty or Partial.
		// See if ANY of the vertices are in the tool volume.
		boolean anyin = false;
		for (int i = 0; i < 8; i++)
			{
				if (vtest[i] == true)
					{
						inside = true;
						break;
					}
			}
		
		if (anyin == true)
			{
				// Yes, some vertices are inside, and we know that not all of them are.
				// If the size is more than 1, then it's partial. If the size is equal to
				// 1, then we force it to be either Solid or Empty based on how many
				// vertices are in the volume.
				// BUG: This is arbitrary and may not be right.
				if (size > 1)
					return CubeStatus.Partial;
				
				int vcount = 0;
				for (int i = 0; i < 8; i++)
					{
						if (vtest[i] == true)
							++vcount;
					}
				if (vcount >=4)
					return CubeStatus.Solid;
				else
					return CubeStatus.Empty;
			}
		
		// Got here, so none of the vertices are inside the volume. It could still be 
		// either Partial or Empty. It's definitely Partial if all four faces have the
		// center of the tool on the inside side of each face. In this situation, the
		// tool (or a cross-section of it) is contained in the cube -- actually, it
		// doesn't have to be totally inside the cube, but it is mostly in the cube.
		// The intersection is definitly Empty if all four corners of the square
		// are on the "same side" of the center. That test is easy to understand,
		// but I'm not sure whether it's the most efficient.
		
		// Make sure that the size is more than 1. If it's not, then return Empty.
		if (size <= 1)
			return CubeStatus.Empty;
		
		// First test: is the center of the tool inside the cube? Easy one.
		// Not testing for z since it must be OK.
		if ((toolp[0] >= corner[0]) && (toolp[0] <= corner[0]+s) &&
				(toolp[1] >= corner[1]) && (toolp[1] <= corner[1]+s))
			// Yes, inside the cube.
			return CubeStatus.Partial;
		
		// Center of tool is known to be outside the cube, and we also know that
		// none of the vertices of the cube are in the tool. The only way that we
		// can have partial intersection is if the tool dips into the side of the cube
		// in between the vertices. The question is whether any of the points along
		// the cube's faces is within the radius of the tool from the tool's center.
		// Of course, I could have started with this question, but my suspision is
		// that those other cases are so much faster to test, and will so often be
		// satisfied, that it's best to do them first.
		
		// This is easier to determine than it first appears. Think about the situation.
		// The center of the tool is outside the square and none of the vertices
		// of the square are inside the tool. The only way that the intersection is not
		// Empty is if the center of the tool is in one of the very small regions along
		// each side of the cube. Because the cube is orthogonally aligned, it's even
		// easier:
		// * For each side, check whether the x or y coordinate is within the radius
		//   of the center. If it's not, then that side of the square can't dip into the tool.
		//   Only one of the four sides might be within this distance (remember: the center
		//   of the tool is outside the square). If none of them are within this distance, 
		//   then the intersection is Empty.
		// * For any side that might dip into the tool, determine the distance from the
		//   center of the tool to the line, along with the point of the line at this
		//   minimum distance. If this distance is less than the radius, and the
		//   point is also less than the radius, then the intersection is Partial; Empty
		//   otherwise.
		// * Answering this question is very easy since the sides of the square are
		//   orthogonal to the axes -- the sides are either horizontal or vertical in the
		//   (x,y)-plane. For intance, in the horizontal case, the line goes from (a0,a1) 
		//   to (b0,b1) and a1=b1. The nearest point along the horizontal line will have 
		//   x coordinate equal to the x coordinate of the tool position. So I only have
		//   to check whether toolp[0] is between a0 and b0.
		
		// Check each side to see which (if any) might dip into the tool.
		int radius = diameter/2;
		
		// Side from (corner[0],corner[1]) to (corner[0]+side,corner[1]), a horizontal
		// line. Need difference from toolp[1]. -- i.e., distance in y direction.
		// BUG: Not sure if I need these abs() applications. 
		if (Math.abs(corner[1]-toolp[1]) < radius)
			{
				// Question is now whether the x coordinate of the tool is between the
				// ends of the line segment forming the side.
				if ((toolp[0] > corner[0]) && (toolp[0] < corner[0]+s))
					return CubeStatus.Partial;
				else
					return CubeStatus.Empty;
			}
		// Side from (corner[0]+side,corner[1]) to (corner[0]+side,corner[1]+side) -- 
		// distance in x direction. This is a vertical side.
		else if (Math.abs(corner[0]+s - toolp[0]) < radius)
			{
				// Check y coordinate of tool.
				if ((toolp[1] > corner[1]) && (toolp[1] < corner[1]+s))
					return CubeStatus.Partial;
				else
					return CubeStatus.Empty;
			}
		// Side from (corner[0],corner[1]+side) to (corner[0]+side,corner[1]+side) -- 
		// distance in y direction. This is a horizontal side.
		else if (Math.abs(corner[1]+s - toolp[1]) < radius)
			{
				// Check x coordinate of tool.
				if ((toolp[0] > corner[0]) && (toolp[0] < corner[0]+s))
					return CubeStatus.Partial;
				else
					return CubeStatus.Empty;
			}
		// Side from (corner[0],corner[1]+side) to (corner[0],corner[1]) -- 
		// distance in x direction. This is a vertical side.
		else if (Math.abs(corner[0] - toolp[0]) < radius)
			{
				// Check y coordinate of tool.
				if ((toolp[1] > corner[1]) && (toolp[1] < corner[1]+s))
					return CubeStatus.Partial;
				else
					return CubeStatus.Empty;
			}

		// None of the sides of the square are even close to the edge of the tool.
		return CubeStatus.Empty;
	}
*/
}
