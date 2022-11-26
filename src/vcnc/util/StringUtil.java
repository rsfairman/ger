package vcnc.util;

// Some utilities for dealing with strings and chars.

// NOTE: This requires Java 1.5 since the makeCSV() method takes a variable
// number of arguments.

public class StringUtil {
  
  public static boolean charInString(char c,String s) {
    
    // Whether the character c appears anywhere in the String s.
    return charInArray(c,s.toCharArray());
  }
  
  public static boolean charInArray(char c,char[] s) {
    
    // Whether c appears as an element of s.
    for (int i = 0; i < s.length; i++)
      if (s[i] == c) return true;
    return false;
  }
  
  public static int charInArrayIndex(char c,char[] s) {
    
    // Returns the (first) index of c as an element of s, or -1 if it's 
    // not there.
    for (int i = 0; i < s.length; i++)
      {
        if (s[i] == c) 
          return i;
      }
    return -1;
  }
  
  public static boolean equals(char[] x,char[] y) {
    
    // Return whether these char arrays are equal.
    if ((x == null) && (y == null))
      return true;
    if ((x == null) && (y != null))
      return false;
    if ((x != null) && (y == null))
      return false;
    if (x.length != y.length)
      return false;
    for (int i = 0; i < x.length; i++)
      {
        if (x[i] != y[i])
          return false;
      }
    return true;
  }
  
  public static boolean stringInArray(String s,String[] a) {
      
      // Whether s appears in the array a.
      for (int i= 0; i < a.length; i++)
        if (s.equals(a[i]) == true) return true;
      return false;
  }
  
  public static int getStringArrayIndex(String s,String[] a) {
    
    // Return the index of the first occurence of s in a, or -1 if
    // it does not appear.
    for (int i = 0; i < a.length; i++)
      {
        if (s.equals(a[i]) == true)
          return i;
      }
    
    // Not found.
    return -1;
  }
    
  public static String[] splitOutLast(String src,String exp) {
    
    // Given src, split it into parts deliminted by exp. The last part is
    // returned in answer[1] and everything in src up to the last occurence 
    // of exp is returned in answer[0].
    // Useful for splitting full file paths into the directory path and file
    // name. To do that, say
    // String[] part = splitOutLast("this/is/path/to/file","/");
    // Much easier would be to use String.split(), though this is probably
    // faster.
    
    String[] answer = new String[2];
    int last = src.lastIndexOf(exp);
    answer[0] = src.substring(0,last);
    answer[1] = src.substring(last+1,src.length());
        
    return answer;
  }
  
  public static String makeCSV(Object first,Object... rest) {
    
    // Use this little guy to convert a list of values to a comma-seperated
    // String. Just say makeCSV(1.2,"test",4,whatever).
    
    String answer = "";
    answer += first.toString();
    
    for (int i = 0; i < rest.length; i++)
      {
        answer += ',';
        answer += rest[i].toString();
      }
    
    return answer;
  }
  
  public static String makeTabDelim(Object first,Object... rest) {
    
    // Just like makeCSV(), but tab-delimines the arguments.
    String answer = "";
    answer += first.toString();
    
    for (int i = 0; i < rest.length; i++)
      {
        answer += '\t';
        answer += rest[i].toString();
      }
    
    return answer;
  }
  
  public static int[] getLineLocs(String s) {
  	
  	// Return an array whose i-th entry is the location of the begining of
  	// the i-th line.
  	//
  	// Be aware that this converts the entire string to a char array,
  	// so it's sort of hoggish.
  	
  	// Count the number of newlines.
  	char[] c = s.toCharArray();
  	
  	// There's always at least the 0-th line, so linecount starts at 1.
  	int linecount = 1;
  	for (int i = 0; i < c.length; i++)
  		{
  			if (c[i] == '\n')
  				++linecount;
  		}
  	
  	// Generate the result. Here, either \n or \r terminate a line, and
  	// we not the first character after either of these.
  	int[] answer = new int[linecount];
  	
  	// The 0-th line starts at the 0-th character.
  	answer[0] = 0;
  	
  	linecount = 1;
  	int i = 0;
  	while (i < c.length)
  		{
  			if ((c[i] == '\n') || (c[i] == '\r'))
  				{
  					// Found a new line. See if the next character should be skipped too.
  					++i;
  					if ((c[i] == '\n') || (c[i] == '\r'))
  						++i;
  					
						answer[linecount] = i;
						++linecount;
  				}
  			else
  				++i;
  		}
  	
  	return answer;
  }
  
  public static void main(String[] args) {
    
    int x = 1;
    int y = 2;
    double z = 2.1;
    short u = 3;
    byte b = 17;
    char c = 'a';
    String t = "spew1";
    String t2 = "spew2";
    
    System.out.println(makeCSV(x,t,y,z,t2,u,b,c));
  }
}
