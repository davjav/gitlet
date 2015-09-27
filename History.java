import java.io.Serializable;
import java.util.Date;
import java.util.Scanner;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Class that contains all of the Commit tree and Branches data,
 * as well as any added or removed files to be included in the
 * next Commit. Furthermore, provides methods for accessing and
 * appending the Commit tree and Branches.
 */
public class History implements Serializable {
    private HashMap<Integer, Commit> commits;
    private HashMap<String, Branch> branches;
    private HashMap<String, HashSet<Commit>> messages;
    private HashSet<String> add;
    private HashSet<String> remove;
    private int nextID;
    private Branch current;

    /**
     * No arguments constructor to instantiate new History object.
     * Adds an empty Commit to the commit tree and inializes the
     * master branch.
     */
    public History() {
        commits = new HashMap<Integer, Commit>();
        branches = new HashMap<String, Branch>();
        messages = new HashMap<String, HashSet<Commit>>();
        add = new HashSet<String>();
        remove = new HashSet<String>();
        nextID = 1;

        Commit initial = new Commit(0, "initial commit");
        commits.put(0, initial);

        HashSet<Commit> hs = new HashSet<Commit>();
        hs.add(initial);
        messages.put("initial commit", hs);

        Branch master = new Branch("master", initial);
        branches.put("master", master);
        current = master;
    }

    /**
     * Stages the file to be included in the next Commit,
     * only if the file exists and has been changed since
     * the last Commit. If the file has been previously marked
     * for removal, only unmarks it and does not stage it.
     */
    public void add(String filename) {
        if (remove.contains(filename)) {
            remove.remove(filename);
            return;
        }

        byte[] newFile;
        try {
            newFile = Files.readAllBytes(Paths.get(filename));
        } catch (IOException e) {
            System.out.println("File does not exist.");
            return;
        }
        HashSet<String> files = new HashSet<String>(current.getHead().fileSet());
        if (files.contains(filename)) {
            int id = current.getHead().getFileCommit(filename);
            String path = ".gitlet/" + id + "/" + filename;
            try {
                byte[] oldFile = Files.readAllBytes(Paths.get(path));
                if (oldFile.length == newFile.length) {
                    boolean changed = false;
                    for (int i = 0; i < oldFile.length; i++) {
                        if (oldFile[i] != newFile[i]) {
                            changed = true;
                        }
                    }
                    if (!changed) {
                        System.out.println("File has not been modified since the last commit.");
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("Previous version of file not found.");
            }
        }
        add.add(filename);
    }

    /**
     * Creates a new Commit reflecting any files which have been added
     * or removed, only if such files exist, and adds it to the Commit tree.
     * Also, it creates copies of newly added files.
     */
    public void commit(String message) {
        if (add.isEmpty() && remove.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        HashMap<String, Integer> files = new HashMap<String, Integer>();
        Commit prevCommit = current.getHead();
        for (String filename : prevCommit.fileSet()) {
            files.put(filename, prevCommit.getFileCommit(filename));
        }
        for (String filename : remove) {
            files.remove(filename);
        }

        if (!add.isEmpty()) {
            File fldr = new File(".gitlet/" + nextID);
            fldr.mkdir();
        }
        for (String filename : add) {
            File source = new File(filename);
            File target = new File(".gitlet/" + nextID + "/" + filename);
            if (makeCopy(source, target)) {
                files.put(filename, nextID);
            }
        }
        Commit newCommit = new Commit(nextID, message, prevCommit, files);
        current.setHead(newCommit);
        commits.put(nextID, newCommit);

        if (messages.containsKey(message)) {
            messages.get(message).add(newCommit);
        } else {
            HashSet<Commit> hs = new HashSet<Commit>();
            hs.add(newCommit);
            messages.put(message, hs);
        }

        nextID += 1;
        add.clear();
        remove.clear();
    }

    /**
     * Marks the file for removal, only if it was included in the previous Commit.
     * If the file is currently staged, it only unstages it.
     */
    public void remove(String filename) {
        HashSet<String> files = new HashSet<String>(current.getHead().fileSet());
        if (add.contains(filename)) {
            add.remove(filename);
        } else if (files.contains(filename)) {
            remove.add(filename);
        } else {
            System.out.println("No reason to remove the file.");
        }
    }

    /**
     * Prints the history starting from the current Commit to the initial Commit.
     */
    public void log() {
        Commit curr = current.getHead();
        while (curr != null) {
            Date date = curr.getDate();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            System.out.println("====");
            System.out.println("Commit " + curr.getID() + ".");
            System.out.println(dateFormat.format(date));
            System.out.println(curr.getMessage());
            System.out.println();
            curr = curr.getPrevious();
        }
    }

    /**
     * Prints the history of all Commits.
     */
    public void globalLog() {
        for (Integer commitID : commits.keySet()) {
            Commit commit = commits.get(commitID);
            Date date = commit.getDate();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            System.out.println("====");
            System.out.println("Commit " + commitID + ".");
            System.out.println(dateFormat.format(date));
            System.out.println(commit.getMessage());
            System.out.println();
        }
    }

    /**
     * Prints the IDs of all Commits with the given message,
     * if such Commits exist.
     */
    public void find(String message) {
        HashSet<Commit> results = messages.get(message);
        if (results == null) {
            System.out.println("Found no commit with that message.");
        } else {
            for (Commit commit : results) {
                System.out.println(commit.getID());
            }
        }
    }

    /**
     * Prints the status, including the branches, staged files, and
     * files marked for removal.
     */
    public void status() {
        System.out.println("=== Branches ===");
        for (String branch : branches.keySet()) {
            if (branch.equals(current.getName())) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }

        System.out.println("\n=== Staged Files ===");
        for (String staged : add) {
            System.out.println(staged);
        }

        System.out.println("\n=== Files Marked for Removal ===");
        for (String marked : remove) {
            System.out.println(marked);
        }
        System.out.println();
    }

    /**
     * Either switches to the branch with the given name and copies
     * all of its files, or attempts to restore the file with the
     * given name from the last Commit, if it exists.
     */
    public void checkout(String name) {
        if (branches.containsKey(name)) {
            if (name.equals(current.getName())) {
                System.out.println("No need to checkout the current branch.");
                return;
            }
            if (dangerousOK()) {
                current = branches.get(name);
                Commit curr = current.getHead();
                HashSet<String> files = new HashSet<String>(curr.fileSet());
                for (String file : files) {
                    int id = curr.getFileCommit(file);
                    File source = new File(".gitlet/" + id + "/" + file);
                    File target = new File(file);
                    makeCopy(source, target);
                }
            }
            return;
        }

        Commit curr = current.getHead();
        HashSet<String> files = new HashSet<String>(curr.fileSet());
        if (files.contains(name)) {
            if (dangerousOK()) {
                int id = curr.getFileCommit(name);
                File source = new File(".gitlet/" + id + "/" + name);
                File target = new File(name);
                makeCopy(source, target);
            }
            return;
        }

        System.out.println("File does not exist in the most recent commit, "
            + "or no such branch exists.");
    }

    /**
     * Restores the file from the Commit with the given ID, if it exists.
     */
    public void checkout(Integer id, String file) {
        if (!commits.containsKey(id)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit curr = commits.get(id);
        HashSet<String> files = new HashSet<String>(curr.fileSet());
        if (!files.contains(file)) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        if (dangerousOK()) {
            File source = new File(".gitlet/" + id + "/" + file);
            File target = new File(file);
            makeCopy(source, target);
        }
    }

    /**
     * Creates a new Branch with the given name if it does not already exist.
     */
    public void branch(String name) {
        if (branches.containsKey(name)) {
            System.out.println("A branch with that name already exists.");
        } else {
            Branch newBranch = new Branch(name, current.getHead());
            branches.put(name, newBranch);
        }
    }

    /**
     * Removes the Branch with the given name if it exists and is not the current branch.
     */
    public void rmBranch(String name) {
        if (!branches.containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
        } else if (name.equals(current.getName())) {
            System.out.println("Cannot remove the current branch.");
        } else {
            branches.remove(name);
        }
    }

    /**
     * Revents the files and the current Commit to the Commit with
     * the given ID, if one exists.
     */
    public void reset(Integer id) {
        if (!commits.containsKey(id)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        if (dangerousOK()) {
            Commit curr = commits.get(id);
            for (String file : curr.fileSet()) {
                int commitID = curr.getFileCommit(file);
                File source = new File(".gitlet/" + commitID + "/" + file);
                File target = new File(file);
                makeCopy(source, target);
            }
            current.setHead(curr);
        }
    }

    /**
     * Copies files from the Branch with the given name, if it exists
     * and is not the current branch. Only copies files which have
     * changed in the given Branch from the splitting point. If the
     * file has also changed in the current branch, .conflicted is
     * appened to the end of the filename.
     */
    public void merge(String name) {
        if (!branches.containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (name.equals(current.getName())) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        Commit split = findSplit(current.getHead(), branches.get(name).getHead());
        Commit other = branches.get(name).getHead();
        Commit curr = current.getHead();
        if (dangerousOK()) {
            for (String file : other.fileSet()) {
                HashSet<String> splitFiles = new HashSet<String>(split.fileSet());
                HashSet<String> otherFiles = new HashSet<String>(other.fileSet());
                HashSet<String> currFiles = new HashSet<String>(curr.fileSet());
                int currCompSplit = 0;
                int otherCompSplit = 0;
                if (splitFiles.contains(file)) {
                    Commit splitFC = commits.get(split.getFileCommit(file));
                    otherCompSplit = commits.get(other.getFileCommit(file)).compareTo(splitFC);
                    if (currFiles.contains(file)) {
                        currCompSplit = commits.get(curr.getFileCommit(file)).compareTo(splitFC);
                    }
                }

                boolean otherAdded = (!splitFiles.contains(file) || otherCompSplit > 0);
                if (otherAdded) {
                    boolean conflict = (splitFiles.contains(file) && currFiles.contains(file)
                        && currCompSplit > 0);
                    conflict = (conflict
                        || (!splitFiles.contains(file) && currFiles.contains(file)));
                    boolean currNotAdded = (splitFiles.contains(file) && currFiles.contains(file)
                        && currCompSplit == 0);
                    currNotAdded = (currNotAdded || !currFiles.contains(file));
                    if (conflict) {
                        File source = new File(".gitlet/" + other.getFileCommit(file)
                            + "/" + file);
                        File target = new File(file + ".conflicted");
                        makeCopy(source, target);
                    } else if (currNotAdded) {
                        File source = new File(".gitlet/" + other.getFileCommit(file)
                            + "/" + file);
                        File target = new File(file);
                        makeCopy(source, target);
                    }
                }
            }
        }
    }

    /**
     * Recreates all the commits of the current Branch from the split
     * point onwards onto the given Branch, if it exists. Finally, it
     * moves the head of the current Branch to the last newly created
     * Commit. If the head of the current Branch is in the history of
     * the given Branch, simply moves the head of the current Branch
     * to the head of the given Branch.
     */
    public void rebase(String name) {
        if (!branches.containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (name.equals(current.getName())) {
            System.out.println("Cannot rebase a branch onto itself.");
            return;
        }

        Commit split = findSplit(current.getHead(), branches.get(name).getHead());
        Commit other = branches.get(name).getHead();
        Commit curr = current.getHead();
        if (split == other) {
            System.out.println("Already up-to-date.");
            return;
        }
        if (dangerousOK()) {
            if (split == curr) {
                current.setHead(other);
            } else {
                Stack<Commit> commitsToRebase = new Stack<Commit>();
                Commit ptr = curr;
                while (ptr != split) {
                    commitsToRebase.push(ptr);
                    ptr = ptr.getPrevious();
                }
                Commit last = other;

                while (!commitsToRebase.empty()) {
                    Commit oldCommit = commitsToRebase.pop();
                    String message = oldCommit.getMessage();
                    HashMap<String, Integer> files = oldCommit.fileMap();
                    for (String file : last.fileSet()) {
                        if (!files.containsKey(file)) {
                            int lastCompSplit = 0;
                            if (split.fileSet().contains(file)) {
                                Commit lastFC = commits.get(last.getFileCommit(file));
                                Commit splitFC = commits.get(split.getFileCommit(file));
                                lastCompSplit = lastFC.compareTo(splitFC);
                            }
                            if (!split.fileSet().contains(file) || lastCompSplit > 0) {
                                files.put(file, last.getFileCommit(file));
                            }
                        } else if (split.fileSet().contains(file)) {
                            Commit oldFC = commits.get(oldCommit.getFileCommit(file));
                            Commit lastFC = commits.get(last.getFileCommit(file));
                            Commit splitFC = commits.get(split.getFileCommit(file));
                            int lastCompSplit = lastFC.compareTo(splitFC);
                            int oldCompSplit = oldFC.compareTo(splitFC);
                            if (lastCompSplit > 0 && oldCompSplit == 0) {
                                files.put(file, last.getFileCommit(file));
                            }
                        }
                    }
                    Commit newCommit = new Commit(nextID, message, last, files);
                    commits.put(nextID, newCommit);
                    messages.get(message).add(newCommit);

                    nextID += 1;
                    last = newCommit;
                }
                current.setHead(last);
            }

            for (String file : current.getHead().fileSet()) {
                int id = current.getHead().getFileCommit(file);
                File source = new File(".gitlet/" + id + "/" + file);
                File target = new File(file);
                makeCopy(source, target);
            }
        }
    }

    /**
     * Recreates all the commits of the current Branch from the split
     * point onwards onto the given Branch, if it exists. Interactively
     * prompts the user whether to skip each Commit (they will be unable
     * to skip the first and last ones) or whether to change the Commit message.
     * Finally, it moves the head of the current Branch to the last newly
     * created Commit. If the head of the current Branch is in the history
     * of the given Branch, simply moves the head of the current Branch
     * to the head of the given Branch.
     */
    public void iRebase(String name) {
        if (!branches.containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else if (name.equals(current.getName())) {
            System.out.println("Cannot rebase a branch onto itself.");
            return;
        }
        Commit split = findSplit(current.getHead(), branches.get(name).getHead());
        Commit other = branches.get(name).getHead();
        Commit curr = current.getHead();
        if (split == other) {
            System.out.println("Already up-to-date.");
            return;
        }
        if (dangerousOK()) {
            if (split == curr) {
                current.setHead(other);
            } else {
                Stack<Commit> commitsToRebase = new Stack<Commit>();
                Commit ptr = curr;
                while (ptr != split) {
                    commitsToRebase.push(ptr);
                    ptr = ptr.getPrevious();
                }
                Commit last = other;
                while (!commitsToRebase.empty()) {
                    Commit oldCommit = commitsToRebase.pop();
                    iRebasePrintCommit(oldCommit);
                    String response = iRebasePrompt();
                    while (response.equals("s")
                        && (oldCommit.getPrevious() == split || commitsToRebase.empty())) {
                        response = iRebasePrompt();
                    }
                    if (!response.equals("s")) {
                        String message = oldCommit.getMessage();
                        if (response.equals("m")) {
                            message = iRebaseCommitMessage();
                        }
                        HashMap<String, Integer> files = oldCommit.fileMap();
                        for (String file : last.fileSet()) {
                            if (!files.containsKey(file)) {
                                int lastCompSplit = 0;
                                if (split.fileSet().contains(file)) {
                                    Commit lastFC = commits.get(last.getFileCommit(file));
                                    Commit splitFC = commits.get(split.getFileCommit(file));
                                    lastCompSplit = lastFC.compareTo(splitFC);
                                }
                                if (!split.fileSet().contains(file) || lastCompSplit > 0) {
                                    files.put(file, last.getFileCommit(file));
                                }
                            } else if (split.fileSet().contains(file)) {
                                Commit oldFC = commits.get(oldCommit.getFileCommit(file));
                                Commit lastFC = commits.get(last.getFileCommit(file));
                                Commit splitFC = commits.get(split.getFileCommit(file));
                                int lastCompSplit = lastFC.compareTo(splitFC);
                                int oldCompSplit = oldFC.compareTo(splitFC);
                                if (lastCompSplit > 0 && oldCompSplit == 0) {
                                    files.put(file, last.getFileCommit(file));
                                }
                            }
                        }
                        Commit newCommit = new Commit(nextID, message, last, files);
                        commits.put(nextID, newCommit);
                        if (messages.containsKey(message)) {
                            messages.get(message).add(newCommit);
                        } else {
                            HashSet<Commit> hs = new HashSet<Commit>();
                            hs.add(newCommit);
                            messages.put(message, hs);
                        }
                        nextID += 1;
                        last = newCommit;
                    }
                }
                current.setHead(last);
            }
            copyCurrent();
        }
    }

    /**
     * Warns that a dangerous operation is about to take place.
     * Return true if the user responds "yes" to the prompt.
     */
    private boolean dangerousOK() {
        Scanner in = new Scanner(System.in);
        System.out.println("Warning: The command you entered may alter the files in your working "
            + "directory. Uncommitted changes may be lost. Are you sure you want to continue? "
            + "(yes/no)");
        String response = in.nextLine();
        return response.equals("yes");
    }

    /**
     * Attempts to copy the source File to the target File.
     * If the target is contained in a directory which does not
     * yet exist, it will recursively create all necessary directories.
     * Returns true only if the file copied successfully.
     */
    private boolean makeCopy(File source, File target) {
        boolean success = false;
        File destination = target.getParentFile();
        if (destination != null && !destination.exists()) {
            destination.mkdirs();
        }

        try {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            success = true;
        } catch (IOException e) {
            System.out.println("Could not copy " + target.getName());
        }

        return success;
    }

    /**
     * Returns the Commit which is the most recent common ancestor,
     * otherwise known as the splitting point, of the first and
     * second Commits.
     * WARNING: Will result in a NullPointerException if the first
     * or second Commit is null.
     */
    private Commit findSplit(Commit first, Commit second) {
        int val = first.compareTo(second);
        if (val == 0) {
            return first;
        } else if (val > 0) {
            return findSplit(first.getPrevious(), second);
        }
        return findSplit(first, second.getPrevious());
    }

    /**
     * Returns the String a user inputs for the interactive
     * rebase prompt.
     */
    private String iRebasePrompt() {
        System.out.println("Would you like to (c)ontinue, (s)kip this commit, "
            + "or change this commit's (m)essage?");
        Scanner in = new Scanner(System.in);
        String response = in.nextLine();
        while (!response.equals("c") && !response.equals("s") && !response.equals("m")) {
            System.out.println("Would you like to (c)ontinue, (s)kip this commit, "
                + "or change this commit's (m)essage?");
            in = new Scanner(System.in);
            response = in.nextLine();
        }
        return response;
    }

    /**
     * Prints the data (commit id, message, time) of the given Commit
     * for replaying Commits.
     */
    private void iRebasePrintCommit(Commit curr) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Currently replaying:");
        Date date = curr.getDate();
        System.out.println("====");
        System.out.println("Commit " + curr.getID() + ".");
        System.out.println(dateFormat.format(date));
        System.out.println(curr.getMessage());
        System.out.println();
    }

    /**
     * Returns the String a user inputs to change a Commit's message
     * during an interactive rebase.
     */
    private String iRebaseCommitMessage() {
        System.out.println("Please enter a new message for this commit.");
        Scanner in = new Scanner(System.in);
        String message = in.nextLine();
        while ("".equals(message.trim())) {
            System.out.println("Please enter a new message for this commit.");
            in = new Scanner(System.in);
            message = in.nextLine();
        }
        return message;
    }

    /**
     * Copies all files of the current Branch to the working directory.
     */
    private void copyCurrent() {
        for (String file : current.getHead().fileSet()) {
            int id = current.getHead().getFileCommit(file);
            File source = new File(".gitlet/" + id + "/" + file);
            File target = new File(file);
            makeCopy(source, target);
        }
    }
}
