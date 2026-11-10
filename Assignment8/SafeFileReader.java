/*
 * Assignment 8, Q2: Reading a file in Java so that it fails safely
 * Secure Software Development, Fall 2026 - Ananya Solanki
 *
 * Fail-safe rules followed:
 *  1. Default deny: any doubt about the file means no content is returned.
 *  2. All-or-nothing: content is returned only after the whole file is read.
 *  3. Resources are always closed (try-with-resources, finally for the audit line).
 *  4. The caller gets a generic message; details go to the internal log only.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;
import java.util.logging.Logger;

public final class SafeFileReader {
    private static final Logger LOG = Logger.getLogger(SafeFileReader.class.getName());
    private static final long MAX_BYTES = 64 * 1024;   // 64 KB limit
    private final Path baseDir;

    public SafeFileReader(Path baseDir) throws IOException {
        this.baseDir = baseDir.toRealPath();            // resolve symlinks once
    }

    /** Returns the file text, or Optional.empty() if anything is wrong. */
    public Optional<String> read(String requestedName) {
        String outcome = "DENIED";
        try {
            Path target = baseDir.resolve(requestedName).normalize();
            if (!target.startsWith(baseDir)) {                  // stops ../ traversal
                throw new SecurityException("path outside base directory");
            }
            Path real = target.toRealPath();                    // NoSuchFileException if missing
            if (!real.startsWith(baseDir) || !Files.isRegularFile(real)) {
                throw new SecurityException("not a regular file inside base directory");
            }
            if (Files.size(real) > MAX_BYTES) {
                throw new SecurityException("file exceeds size limit");
            }
            CharsetDecoder strictUtf8 = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            StringBuilder content = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(Files.newInputStream(real), strictUtf8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    content.append(line).append('\n');
                }
            }                                                   // reader closed here, even on error
            outcome = "OK";
            return Optional.of(content.toString());             // only a complete read is returned
        } catch (NoSuchFileException e) {
            LOG.warning("Read failed, file not found: " + e.getMessage());
        } catch (AccessDeniedException e) {
            LOG.warning("Read failed, permission denied: " + e.getMessage());
        } catch (java.nio.charset.MalformedInputException e) {
            LOG.warning("Read failed, invalid UTF-8 content in: " + requestedName);
        } catch (SecurityException e) {
            LOG.warning("Read blocked by policy for '" + requestedName + "': " + e.getMessage());
        } catch (IOException | InvalidPathException e) {
            LOG.warning("Read failed, I/O error: " + e.getClass().getSimpleName());
        } finally {
            LOG.info("AUDIT read request='" + requestedName + "' outcome=" + outcome);
        }
        return Optional.empty();                                // the safe default
    }

    /* Demonstration: one good file and five ways the read can go wrong. */
    public static void main(String[] args) throws IOException {
        System.setProperty("java.util.logging.SimpleFormatter.format", "  [log] %4$s %5$s%n");
        Path dir = Files.createTempDirectory("a8-data");
        Files.writeString(dir.resolve("config.txt"), "mode=secure\nretries=3\n");
        Files.write(dir.resolve("big.log"), new byte[(int) MAX_BYTES + 1]);
        Files.write(dir.resolve("bad.txt"), new byte[]{0x48, 0x69, (byte) 0xC3, 0x28});
        Files.createDirectory(dir.resolve("folder"));

        SafeFileReader reader = new SafeFileReader(dir);
        String[] requests = {"config.txt", "missing.txt", "../../etc/passwd",
                             "big.log", "bad.txt", "folder"};
        for (String name : requests) {
            System.out.println("Request: " + name);
            String shown = reader.read(name)
                    .map(text -> "SUCCESS -> " + text.replace("\n", " | "))
                    .orElse("FAILED SAFELY -> \"The file could not be read.\"");
            System.out.println("  Result: " + shown);
        }
    }
}
