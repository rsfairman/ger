package vcnc;

import org.junit.Test;
import vcnc.tpile.Translator;
import vcnc.tpile.lex.Lexer;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class LexerTest {
    public static final String BASE_DIR = "src/test/resources/";

    @Test
    public void verifyLexerOutput() throws IOException {
        String input = fromTestResourcesFile("unit_tests/lex_test_01.in");
        String actual = Lexer.digestAll(input);
        toTestResourcesFile("unit_tests/lex_test_01.out", actual);
        String expected = fromTestResourcesFile("unit_tests/lex_test_01.ref");

        assertEquals(expected, actual);
    }

    @Test
    public void verifyParserOutput() throws Exception {
        String input = fromTestResourcesFile("unit_tests/parse_test_01.in");
        String actual = Translator.digestAll(input,-1);
        toTestResourcesFile("unit_tests/parse_test_01.out", actual);
        String expected = fromTestResourcesFile("unit_tests/parse_test_01.ref");

        assertEquals(expected, actual);
    }

    private String fromTestResourcesFile(String filename) throws IOException {
        return Util.fromFile(BASE_DIR + "/" + filename);
    }

    private void toTestResourcesFile(String filename, String content) throws IOException {
        Util.toFile(BASE_DIR + "/" + filename, content);
    }
}
