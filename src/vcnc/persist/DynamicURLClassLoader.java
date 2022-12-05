package vcnc.persist;

import java.net.URL;
import java.net.URLClassLoader;

// BUG: Trash. Won't work as of Java 9.

public class DynamicURLClassLoader extends URLClassLoader {

  public DynamicURLClassLoader(URLClassLoader classLoader) {
      super(classLoader.getURLs());
  }

  public void addURL(URL url) {
      super.addURL(url);
  }
}
