package vcnc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Util {
    private Util() {
        throw new RuntimeException("This class cannot be instantiated");
    }

    public static String fromFile(String fileName) throws IOException {
        return Files.readString(Path.of(fileName));
    }

    public static void toFile(String filename, String contents) throws IOException {
        Files.writeString(Path.of(filename.replaceAll("\\\\", File.separator)), contents, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void appendFile(String filename, String contents) throws IOException {
        Files.writeString(Path.of(filename.replaceAll("\\\\", File.separator)), contents, StandardOpenOption.APPEND);
    }
}
