package vcnc.ui.TabMgmt;

/*
Each "displayed thing," like what would appear under a tab in the main
window, has a type. This way, it's easy to determine what kind of information
is in any given tab. These "displayed things" can also appear in their own
window, without a tab, and the same information is useful there too.

The way this is used is far from ideal, but I see no better alternative
since Java does not have multiple inheritance. One alternative
is using instanceof, which seems error-prone. Also, various things that
will be the same "type" as far as Java is concerned will hold different
types of information; e.g., a JTextArea might hold G-code input, transpiled
output or some intermediate form of the code -- it's all text.

*/

public enum TabbedType {
  
  UNKNOWN
  ,G_INPUT      // A G-code file to be used as input; i.e., suitable for 
                // transpiling.
                // This may be displayed with or without line numbers.
  ,LEXER_OUT    // Display of the tokens that come from the Lexer.
  ,PARSER_OUT   // Display of full statements.
  ;
}

