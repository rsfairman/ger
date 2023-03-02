module vcnc {
	
	// For Swing (and other stuff too, no doubt).
	requires java.desktop;
	
	 // For javax.tools
  requires java.compiler;
  
  // To use java.uti.prefs.Preferences.
  requires java.prefs;
	
  // So the class loader can find wizards that are dynamically loaded.
  exports ger.wizard;
}