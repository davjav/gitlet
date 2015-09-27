import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

/**
 * Class that provides JUnit tests for Gitlet, as well as a couple of utility
 * methods.
 * 
 * @author Joseph Moghadam
 * 
 *         Some code adapted from StackOverflow:
 * 
 *         http://stackoverflow.com/questions
 *         /779519/delete-files-recursively-in-java
 * 
 *         http://stackoverflow.com/questions/326390/how-to-create-a-java-string
 *         -from-the-contents-of-a-file
 * 
 *         http://stackoverflow.com/questions/1119385/junit-test-for-system-out-
 *         println
 * 
 */
public class GitletPublicTest {
    private static final String GITLET_DIR = ".gitlet/";
    private static final String TESTING_DIR = "test_files/";

    /* matches either unix/mac or windows line separators */
    private static final String LINE_SEPARATOR = "\r\n|[\r\n]";

    /**
     * Deletes existing gitlet system, resets the folder that stores files used
     * in testing.
     * 
     * This method runs before every @Test method. This is important to enforce
     * that all tests are independent and do not interact with one another.
     */
    @Before
    public void setUp() {
        File f = new File(GITLET_DIR);
        if (f.exists()) {
            recursiveDelete(f);
        }
        f = new File(TESTING_DIR);
        if (f.exists()) {
            recursiveDelete(f);
        }
        f.mkdirs();
    }

    /**
     * Tests that init creates a .gitlet directory. Does NOT test that init
     * creates an initial commit, which is the other functionality of init.
     */
    @Test
    public void testBasicInitialize() {
        gitlet("init");
        File f = new File(GITLET_DIR);
        assertTrue(f.exists());
    }

    /**
     * Tests that checking out a file name will restore the version of the file
     * from the previous commit. Involves init, add, commit, and checkout.
     */
    @Test
    public void testBasicCheckout() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        writeFile(wugFileName, "This is not a wug.");
        gitlet("checkout", wugFileName);
        assertEquals(wugText, getText(wugFileName));
    }

    /**
     * Tests that log prints out commit messages in the right order. Involves
     * init, add, commit, and log.
     */
    @Test
    public void testBasicLog() {
        gitlet("init");
        String commitMessage1 = "initial commit";

        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("add", wugFileName);
        String commitMessage2 = "added wug";
        gitlet("commit", commitMessage2);

        String logContent = gitlet("log");
        assertArrayEquals(new String[] { commitMessage2, commitMessage1 },
                extractCommitMessages(logContent));
    }

    /**
     * Tests the checkout functionality with a file name and commit ID.
     */
    @Test
    public void testCheckoutFile() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        writeFile(wugFileName, "This is not a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added not wug");
        writeFile(wugFileName, "This might be a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added might be wug");
        gitlet("checkout", "1", wugFileName);
        assertEquals(wugText, getText(wugFileName));
        gitlet("checkout", "2", wugFileName);
        assertEquals("This is not a wug.", getText(wugFileName));
        gitlet("checkout", "3", wugFileName);
        assertEquals("This might be a wug.", getText(wugFileName));
    }

    /**
     * Test branching works properly by comparing the same file which
     * have been modified in different branching after running checkout.
     */
    @Test
    public void testBranch() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("branch", "test");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("checkout", "test");
        createFile(wugFileName, "This is not a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added not a wug");
        gitlet("checkout", "master");
        assertEquals(wugText, getText(wugFileName));
        gitlet("checkout", "test");
        assertEquals("This is not a wug.", getText(wugFileName));
    }

    /**
     * Tests removing a branch by verifying that running checkout
     * won't work after removing the branch.
     */
    @Test
    public void testRemoveBranch() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("branch", "test");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("checkout", "test");
        createFile(wugFileName, "This is not a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added not a wug");
        String error = gitlet("rm-branch", "test");
        assertEquals("Cannot remove the current branch.\n", error);
        error = gitlet("rm-branch", "other");
        assertEquals("A branch with that name does not exist.\n", error);
        gitlet("rm-branch", "master");
        error = gitlet("checkout", "master");
        String expectedError = "File does not exist in the most recent commit, ";
        expectedError += "or no such branch exists.\n";
        assertEquals(expectedError, error);
    }

    /**
     * Tests that reset works properly by resetting to an earlier
     * commit and verifying the files and log entires.
     */
    @Test
    public void testReset() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        writeFile(wugFileName, "This is not a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added not wug");
        gitlet("reset", "1");
        assertEquals(wugText, getText(wugFileName));
        String logContent = gitlet("log");
        assertArrayEquals(new String[] { "added wug", "initial commit" },
                extractCommitMessages(logContent));
        gitlet("reset", "2");
        assertEquals("This is not a wug.", getText(wugFileName));
        logContent = gitlet("log");
        assertArrayEquals(new String[] { "added not wug", "added wug", "initial commit" },
                extractCommitMessages(logContent));
    }

    /**
     * Tests that merge works properly when only the given branch has changes.
     */
    @Test
    public void testMerge() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("branch", "second");
        writeFile(wugFileName, "This is not a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "changed to not wug");
        gitlet("checkout", "second");
        gitlet("merge", "master");
        assertEquals("This is not a wug.", getText(wugFileName));
    }

    /**
     * Tests that merge works properly when the current branch doesn't have the file.
     */
    @Test
    public void testMergeMissing() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("branch", "second");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("checkout", "second");
        gitlet("merge", "master");
        assertEquals(wugText, getText(wugFileName));
    }

    /**
     * Tests that merge works properly when there are no changes in either branch.
     */
    @Test
    public void testMergeNothing() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("branch", "second");
        gitlet("checkout", "second");
        gitlet("merge", "master");
        assertEquals(wugText, getText(wugFileName));
    }

    /**
     * Tests that merge works properly when both branches have changes, and
     * the split point includes the file.
     */
    @Test
    public void testMergeConflict() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("branch", "second");
        writeFile(wugFileName, "This is not a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "changed to not wug");
        gitlet("checkout", "second");
        writeFile(wugFileName, "This might be a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "changed might be wug");
        gitlet("merge", "master");
        assertEquals("This is not a wug.", getText(TESTING_DIR + "wug.txt.conflicted"));
    }

    /**
     * Tests that merge works properly when both branches have changes, and
     * the split point exludes the file.
     */
    @Test
    public void testMergeConflictExclusive() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("branch", "second");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("checkout", "second");
        writeFile(wugFileName, "This might be a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "changed might be wug");
        gitlet("merge", "master");
        assertEquals("This is a wug.", getText(TESTING_DIR + "wug.txt.conflicted"));
    }

    /**
     * Tests that rebase works when rebasing to a branch which added an
     * additonal file. The common file between the two branches was only
     * changed in the current branch. Verify rebase occured properly
     * by comparing the file contents after the rebase and check the
     * log history.
     */
    @Test
    public void testRebase() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("branch", "second");
        createFile(TESTING_DIR + "wug2.txt", "Hi");
        gitlet("add", TESTING_DIR + "wug2.txt");
        gitlet("commit", "added wug2");
        gitlet("checkout", "second");
        createFile(wugFileName, "This is not a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added not wug");
        gitlet("rebase", "master");
        assertEquals("This is not a wug.", getText(wugFileName));
        String logContent = gitlet("log");
        assertArrayEquals(new String[] { "added not wug", "added wug2",
            "added wug", "initial commit" },
                extractCommitMessages(logContent));
    }

    /**
     * Tests that rebase takes the files from the current branch
     * when both branches contain changes.
     */
    @Test
    public void testRebaseConflict() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("branch", "second");
        createFile(wugFileName, "This might be a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added might be wug");
        gitlet("checkout", "second");
        createFile(wugFileName, "This is not a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added not wug");
        gitlet("rebase", "master");
        assertEquals("This is not a wug.", getText(wugFileName));
        String logContent = gitlet("log");
        assertArrayEquals(new String[] { "added not wug", "added might be wug",
            "added wug", "initial commit" },
                extractCommitMessages(logContent));
    }

    /**
     * Tests that rebase propagates changes from the branch
     * being rebased to when the branch does not have the file.
     */
    @Test
    public void testRebaseExclusive() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("branch", "second");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("checkout", "second");
        createFile(TESTING_DIR + "wug2.txt", "Hi");
        gitlet("add", TESTING_DIR + "wug2.txt");
        gitlet("commit", "added wug2");
        createFile(wugFileName, "");
        gitlet("rebase", "master");
        assertEquals(wugText, getText(wugFileName));
        String logContent = gitlet("log");
        assertArrayEquals(new String[] { "added wug2", "added wug", "initial commit" },
                extractCommitMessages(logContent));
    }

    /**
     * Tests that rebase propagate changes from the branch
     * being rebased to when both branches have the file, but
     * it has only been changed in the given branch.
     */
    @Test
    public void testRebaseInclusive() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("branch", "second");
        createFile(wugFileName, "This might be a wug.");
        gitlet("add", wugFileName);
        gitlet("commit", "added might be wug");
        gitlet("checkout", "second");
        createFile(TESTING_DIR + "wug2.txt", "Hi");
        gitlet("add", TESTING_DIR + "wug2.txt");
        gitlet("commit", "added wug2");
        gitlet("rebase", "master");
        assertEquals("This might be a wug.", getText(wugFileName));
        String logContent = gitlet("log");
        assertArrayEquals(new String[] { "added wug2", "added might be wug",
            "added wug", "initial commit" },
                extractCommitMessages(logContent));
    }

    /**
     * Test that rebase doesn't propagate files which have
     * been removed in the branch to be merged.
     */
    @Test
    public void testRebaseRemoved() {
        String wugFileName = TESTING_DIR + "wug.txt";
        String wugText = "This is a wug.";
        createFile(wugFileName, wugText);
        gitlet("init");
        gitlet("add", wugFileName);
        gitlet("commit", "added wug");
        gitlet("branch", "second");
        createFile(TESTING_DIR + "wug2.txt", "Hi");
        gitlet("add", TESTING_DIR + "wug2.txt");
        gitlet("commit", "added wug2");
        gitlet("checkout", "second");
        gitlet("rm", wugFileName);
        gitlet("commit", "removed wug");
        createFile(wugFileName, "");
        gitlet("rebase", "master");
        assertEquals("", getText(wugFileName));
        String logContent = gitlet("log");
        assertArrayEquals(new String[] { "removed wug", "added wug2",
            "added wug", "initial commit" },
                extractCommitMessages(logContent));
    }

    /**
     * Convenience method for calling Gitlet's main. Anything that is printed
     * out during this call to main will NOT actually be printed out, but will
     * instead be returned as a string from this method.
     * 
     * Prepares a 'yes' answer on System.in so as to automatically pass through
     * dangerous commands.
     * 
     * The '...' syntax allows you to pass in an arbitrary number of String
     * arguments, which are packaged into a String[].
     */
    private static String gitlet(String... args) {
        PrintStream originalOut = System.out;
        InputStream originalIn = System.in;
        ByteArrayOutputStream printingResults = new ByteArrayOutputStream();
        try {
            /*
             * Below we change System.out, so that when you call
             * System.out.println(), it won't print to the screen, but will
             * instead be added to the printingResults object.
             */
            System.setOut(new PrintStream(printingResults));

            /*
             * Prepares the answer "yes" on System.In, to pretend as if a user
             * will type "yes". You won't be able to take user input during this
             * time.
             */
            String answer = "yes";
            InputStream is = new ByteArrayInputStream(answer.getBytes());
            System.setIn(is);

            /* Calls the main method using the input arguments. */
            Gitlet.main(args);

        } finally {
            /*
             * Restores System.out and System.in (So you can print normally and
             * take user input normally again).
             */
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
        return printingResults.toString();
    }

    /**
     * Returns the text from a standard text file (won't work with special
     * characters).
     */
    private static String getText(String fileName) {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(fileName));
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Creates a new file with the given fileName and gives it the text
     * fileText.
     */
    private static void createFile(String fileName, String fileText) {
        File f = new File(fileName);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        writeFile(fileName, fileText);
    }

    /**
     * Replaces all text in the existing file with the given text.
     */
    private static void writeFile(String fileName, String fileText) {
        FileWriter fw = null;
        try {
            File f = new File(fileName);
            fw = new FileWriter(f, false);
            fw.write(fileText);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Deletes the file and all files inside it, if it is a directory.
     */
    private static void recursiveDelete(File d) {
        if (d.isDirectory()) {
            for (File f : d.listFiles()) {
                recursiveDelete(f);
            }
        }
        d.delete();
    }

    /**
     * Returns an array of commit messages associated with what log has printed
     * out.
     */
    private static String[] extractCommitMessages(String logOutput) {
        String[] logChunks = logOutput.split("====");
        int numMessages = logChunks.length - 1;
        String[] messages = new String[numMessages];
        for (int i = 0; i < numMessages; i++) {
            System.out.println(logChunks[i + 1]);
            String[] logLines = logChunks[i + 1].split(LINE_SEPARATOR);
            messages[i] = logLines[3];
        }
        return messages;
    }
}
