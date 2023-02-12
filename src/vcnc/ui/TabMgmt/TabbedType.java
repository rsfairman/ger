package vcnc.ui.TabMgmt;

import vcnc.tpile.Translator;

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

Most of these correspond to the constants defined in Translator.
*/


public enum TabbedType {
  
  UNKNOWN
  ,G_INPUT      // A G-code file to be used as input; i.e., suitable for 
                // transpiling.
                // This may be displayed with or without line numbers.
  ,LEXER_OUT    // Display of the tokens that come from the Lexer.
  ,PARSER_OUT   // Display of full statements.
  ,DIRECTIVES_OUT // Etc., from Translator
  ,SUBPROGS_OUT
  ,WIZARDS_OUT
  ,UNITS_OUT
  ,OFFSETS_OUT
  ,POLAR_OUT
  ,INCREMENTAL_OUT
  ,CUTTERCOMP_OUT
  ;
  
  public static TabbedType toEnum(int layer) {
    
    // Translate from the constants in Translator.
   switch (layer) {
     case Translator.ThruLexer       : return LEXER_OUT;
     case Translator.ThruParser      : return PARSER_OUT;
     case Translator.ThruDirectives  : return DIRECTIVES_OUT;
     case Translator.ThruSubProgs    : return SUBPROGS_OUT;
     case Translator.ThruWizards     : return WIZARDS_OUT;
     case Translator.ThruUnits       : return UNITS_OUT;
     case Translator.ThruWorkOffsets : return OFFSETS_OUT;
     case Translator.ThruPolar       : return POLAR_OUT;
     case Translator.ThruIncremental : return INCREMENTAL_OUT;
     case Translator.ThruCutterComp  : return CUTTERCOMP_OUT;
     default                         : break;
   }
   return UNKNOWN;
  }
  
  public static String toName(int layer) {
    return toEnum(layer).toName();
  }
  
  public String toName() {
    
    switch (this) {
      case UNKNOWN         : return "Unknown";
      case G_INPUT         : return "G-code";
      case LEXER_OUT       : return "Lexer";
      case PARSER_OUT      : return "Parser";
      case DIRECTIVES_OUT  : return "Directives";
      case SUBPROGS_OUT    : return "Sub-programs";
      case WIZARDS_OUT     : return "Wizards";
      case UNITS_OUT       : return "Units";
      case OFFSETS_OUT     : return "Work Offsets";
      case POLAR_OUT       : return "Polars";
      case INCREMENTAL_OUT : return "Incremental";
      case CUTTERCOMP_OUT  : return "Cutter Comp";
      default              : break;
    }
    
    return "";
    
  }
  
}

