import java.io.Serializable;

/**
 * Class that contains the name and head Commit for a Branch.
 */
public class Branch implements Serializable {
    private String name;
    private Commit head;

    /**
     * Constructor for a new Branch object, with the name of the
     * Branch and the head Commit passed in as arguments.
     */
    public Branch(String name, Commit head) {
        this.name = name;
        this.head = head;
    }

    /**
     * Returns the Commit object which is the head of the Branch.
     */
    public Commit getHead() {
        return head;
    }

    /**
     * Sets the head of the Branch to the Commit object passed in.
     */
    public void setHead(Commit head) {
        this.head = head;
    }

    /**
     * Returns the name of the Branch.
     */
    public String getName() {
        return name;
    }
}
