import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

/**
 * Class that provides menu functionality for Gitlet commands and handles
 * the reading and writing of History objects.
 */
public class Gitlet {

    /**
     * Main method of Gitlet class, which calls the corresponding
     * helper method depending on which command is passed in.
     */
    public static void main(String[] args) {
        String command = "invalid command";
        History hist = null;
        if (args.length > 0) {
            command = args[0];
            if (!command.equals("init")) {
                hist = readHistory();
                if (hist == null) {
                    return;
                }
            }
        }

        switch (command) {
            case "init":
                init(args);
                break;
            case "add":
                add(args, hist);
                break;
            case "commit":
                commit(args, hist);
                break;
            case "rm":
                remove(args, hist);
                break;
            case "log":
                log(args, hist);
                break;
            case "global-log":
                globalLog(args, hist);
                break;
            case "find":
                find(args, hist);
                break;
            case "status":
                status(args, hist);
                break;
            case "checkout":
                checkout(args, hist);
                break;
            case "branch":
                branch(args, hist);
                break;
            case "rm-branch":
                rmBranch(args, hist);
                break;
            case "reset":
                reset(args, hist);
                break;
            case "merge":
                merge(args, hist);
                break;
            case "rebase":
                rebase(args, hist);
                break;
            case "i-rebase":
                iRebase(args, hist);
                break;
            default:
                System.out.println("Unrecognized command.");
                break;
        }
    }

    /**
     * Instantiates new History object and saves it, if a .gitlet
     * folder does not already exist in the current directory. The
     * args array must contain no additional arguments.
     */
    private static void init(String[] args) {
        if (args.length != 1) {
            System.out.println("Init requires no additional arguments.");
            return;
        }
        File gitlet = new File(".gitlet");
        if (gitlet.exists()) {
            System.out.println("A gitlet version control system "
                + "already exists in the current directory.");
            return;
        }
        gitlet.mkdir();
        History hist = new History();
        writeHistory(hist);
    }

    /**
     * Calls the add method of the History object
     * if only one additional argument (the filename) is supplied.
     */
    private static void add(String[] args, History hist) {
        if (args.length != 2) {
            System.out.println("Add requires one argument.");
            return;
        }
        hist.add(args[1]);
        writeHistory(hist);
    }

    /**
     * Calls the commit method of the History object if only
     * one additional argument (the commit message) is supplied.
     */
    private static void commit(String[] args, History hist) {
        if (args.length != 2 || "".equals(args[1].trim())) {
            System.out.println("Please enter a commit message.");
            return;
        }
        hist.commit(args[1]);
        writeHistory(hist);
    }

    /**
     * Calls the remove method of the History object if
     * only one additional argument (the filename) is supplied.
     */
    private static void remove(String[] args, History hist) {
        if (args.length != 2) {
            System.out.println("Remove requires one argument.");
            return;
        }
        hist.remove(args[1]);
        writeHistory(hist);
    }

    /**
     * Calls the log method of the History object if no
     * additional arguments are supplied.
     */
    private static void log(String[] args, History hist) {
        if (args.length != 1) {
            System.out.println("Log requires no additional arguments.");
            return;
        }
        hist.log();
    }

    /**
     * Calls the globalLog method of the History object if no
     * additional arguments are supplied.
     */
    private static void globalLog(String[] args, History hist) {
        if (args.length != 1) {
            System.out.println("Global-log requires no additional arguments.");
            return;
        }
        hist.globalLog();
    }

    /**
    * Calls the find method of the History object if only one
    * additional argument (the commit message) is supplied.
    */
    private static void find(String[] args, History hist) {
        if (args.length != 2) {
            System.out.println("Find requires one argument.");
            return;
        }
        hist.find(args[1]);
    }

    /**
     * Calls the status method of the History object if no
     * additional arguments are supplied.
     */
    private static void status(String[] args, History hist) {
        if (args.length != 1) {
            System.out.println("Status requires no additional arguments.");
            return;
        }
        hist.status();
    }

    /**
     * Calls the checkout method of the History object if either
     * one (branch or filename) or two (commit id and filename)
     * additional arguments are supplied.
     */
    private static void checkout(String[] args, History hist) {
        if (args.length != 2 && args.length != 3) {
            System.out.println("Please input the correct number of arguments for checkout.");
            return;
        }
        if (args.length == 2) {
            hist.checkout(args[1]);
            writeHistory(hist);
        } else if (args.length == 3) {
            hist.checkout(Integer.parseInt(args[1]), args[2]);
        }
    }

    /**
     * Calls the branch method of the History object if only one
     * additional argument (branch name) is supplied.
     */
    private static void branch(String[] args, History hist) {
        if (args.length != 2) {
            System.out.println("Branch requires one argument.");
            return;
        }
        hist.branch(args[1]);
        writeHistory(hist);
    }

    /**
     * Calls the rmBranch method of the History object if only
     * one additional argument (branch name) is supplied.
     */
    private static void rmBranch(String[] args, History hist) {
        if (args.length != 2) {
            System.out.println("Rm-branch requires one argument.");
            return;
        }
        hist.rmBranch(args[1]);
        writeHistory(hist);
    }

    /**
     * Calls the reset method of the History object if only
     * one additional argument (commit id) is supplied.
     */
    private static void reset(String[] args, History hist) {
        if (args.length != 2) {
            System.out.println("Reset requires one argument.");
            return;
        }
        hist.reset(Integer.parseInt(args[1]));
        writeHistory(hist);
    }

    /**
     * Calls the merge method of the History object if only
     * one additional argument (branch name) is supplied.
     */
    private static void merge(String[] args, History hist) {
        if (args.length != 2) {
            System.out.println("Merge requires one argument.");
            return;
        }
        hist.merge(args[1]);
    }

    /**
     * Calls the rebase method of the History object if only
     * one additional argument (branch name) is supplied.
     */
    private static void rebase(String[] args, History hist) {
        if (args.length != 2) {
            System.out.println("Rebase requires one argument.");
            return;
        }
        hist.rebase(args[1]);
        writeHistory(hist);
    }

    /**
     * Calls the iRebase method of the History object if only
     * one additional argument (branch name) is supplied.
     */
    private static void iRebase(String[] args, History hist) {
        if (args.length != 2) {
            System.out.println("I-rebase requires one argument.");
            return;
        }
        hist.iRebase(args[1]);
        writeHistory(hist);
    }

    /**
     * Code adapted from http://www.tutorialspoint.com/java/java_serialization.htm
     * Serializes the given History object and writes it to the .gitlet directory.
     */
    private static void writeHistory(History hist) {
        try {
            FileOutputStream fileOut = new FileOutputStream(".gitlet/history.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(hist);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Code adapted from http://www.tutorialspoint.com/java/java_serialization.htm
     * Reads and deserializes the .gitlet/history.ser History object and returns it.
     * If it doesn't exist, returns null.
    */
    private static History readHistory() {
        try {
            FileInputStream fileIn = new FileInputStream(".gitlet/history.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            History hist = (History) in.readObject();
            in.close();
            fileIn.close();
            return hist;
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        }
        return null;
    }
}
