import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * Class that contains all of the pertinent information of a
 * Commit, including its ID, message, the time it was taken,
 * its previous Commit, and all the files it contains.
 */
public class Commit implements Serializable, Comparable<Commit> {
    private int id;
    private String message;
    private Date time;
    private Commit previous;
    private HashMap<String, Integer> files;

    /**
     * Constructs a Commit with the given ID and messages, with no
     * previous Commits or files contained in it.
     */
    public Commit(int id, String message) {
        this.id = id;
        this.message = message;
        this.previous = null;
        files = new HashMap<String, Integer>();
        time = new Date();
    }

    /**
     * Constructs a Commit with the given ID, message, previous Commit,
     * and a HashMap mapping filenames to their commits.
     */
    public Commit(int id, String message, Commit previous, HashMap<String, Integer> files) {
        this.id = id;
        this.message = message;
        this.previous = previous;
        this.files = files;
        time = new Date();
    }

    /**
     * Returns a Set with all of the files in this Commit.
     */
    public Set<String> fileSet() {
        return files.keySet();
    }

    /**
     * Returns the ID of the Commit containing the file in this Commit.
     * WARNING: Will result in a NullPointerException if the filename doesn't
     * exist in this Commit.
     */
    public int getFileCommit(String filename) {
        return files.get(filename);
    }

    /**
     * Returns a copy of the HashMap mapping filenames contained in this Commit
     * to their Commits.
     */
    public HashMap<String, Integer> fileMap() {
        return new HashMap<String, Integer>(files);
    }

    /**
     * Returns the ID of this Commit.
     */
    public int getID() {
        return id;
    }

    /**
     * Returns the message of this Commit.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the time of this Commit as a Date object.
     */
    public Date getDate() {
        return time;
    }

    /**
     * Returns the previous Commit of this Commit.
     */
    public Commit getPrevious() {
        return previous;
    }

    /**
     * Compares two Commits based on the time they were made.
     * It returns a value greater than 0 if this Commit is
     * more recent than the other Commit, a value
     * less than zero if the other Commit is more recent than
     * this commit, or 0 if the two Commits were made at the same
     * time.
     * WARNING: Will result in a NullPointerException if the other
     * Commit is null.
     */
    public int compareTo(Commit other) {
        return time.compareTo(other.getDate());
    }
}
