package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

/**
 * Class representing pointers to saved commits.
 *
 * @author Randy Nguyen
 */
public class Branches implements Serializable {

  /** Pathway representing all the existing pointers. */
  static final File FOLDER = Utils.join(Command.GITLET_FOLDER, "branches");

  /** Branch denoting the current/active commit. */
  static final File HEAD = Utils.join(FOLDER, Utils.readContentsAsString(Command.HEAD));

  /** Branch denoting the initial main branch. */
  static final File MAIN = Utils.join(FOLDER, "main");

  /**
   * Returns a String that represents the current head branch's Commit sha1 ID.
   *
   * @return String
   */
  public static String headId() {
    return Utils.sha1(Utils.readContents(HEAD));
  }

  /**
   * Returns a Commit object from the current head branch by reading into it.
   *
   * @return Commit
   */
  public static Commit headCommit() {
    return Utils.readObject(HEAD, Commit.class);
  }

  /**
   * Creates a new branch with the given name, and points it at the current head node.
   *
   * @param args {"branch", name} Input array.
   */
  public static void branch(String[] args) throws IOException {
    List<String> branchNames = Utils.plainFilenamesIn(FOLDER);
    if (branchNames.contains(args[1])) {
      System.out.println("A branch with that name already exists.");
      return;
    }
    File newBranch = Utils.join(FOLDER, args[1]);
    newBranch.createNewFile();
    Utils.writeContents(newBranch, Utils.readContents(HEAD));
  }

  /**
   * Deletes the branch with the given name.
   *
   * @param args {'rm-branch', name}
   */
  public static void removeBranch(String[] args) {
    List<String> branchNames = Utils.plainFilenamesIn(FOLDER);
    if (!branchNames.contains(args[1])) {
      System.out.println("A branch with that name does not exist.");
    } else if (HEAD.getName().equals(args[1])) {
      System.out.println("Cannot remove the current branch.");
    } else {
      Utils.join(FOLDER, args[1]).delete();
    }
  }

  /**
   * Takes all files in the commit at the head of the given branch, and puts them in the working
   * directory, overwriting the versions of the files that are already there if they exist.
   *
   * <p>Also, at the end of this command, the given branch will now be considered the current branch
   * (HEAD).
   *
   * <p>Any files that are tracked in the current branch but are not present in the checked-out
   * branch are deleted.
   *
   * <p>The staging area is cleared, unless the checked-out branch is the current branch
   *
   * @param branchName name of the branch
   */
  public static void checkoutBranch(String branchName) throws IOException {
    List<String> branches = Utils.plainFilenamesIn(FOLDER);
    if (!branches.contains(branchName)) {
      System.out.println("No such branch exists.");
    } else if (HEAD.getName().equals(branchName)) {
      System.out.println("No need to checkout the current branch.");
    } else {
      Commit givenC = Utils.readObject(Utils.join(FOLDER, branchName), Commit.class);
      TreeMap<String, String> givenFiles = givenC.getFiles();
      TreeMap<String, String> headFiles = headCommit().getFiles();
      if (Command.untracked(givenFiles)) {
        return;
      }
      for (String name : givenFiles.keySet()) {
        Command.checkoutFile(givenC, name);
      }
      Utils.writeContents(Command.HEAD, branchName);
      for (String name : headFiles.keySet()) {
        if (headFiles.containsKey(name) && !givenFiles.containsKey(name)) {
          Utils.join(Command.CWD, name).delete();
        }
      }
      Stage.clearStage();
    }
  }

  /**
   * Checks for merge errors. Returns true if there are some.
   *
   * @param branchName the name of branch to check in merge
   * @return boolean whether there are errors
   */
  public static boolean mergeErrors(String branchName) {
    if (Stage.ADD.listFiles().length != 0 || Stage.RM.listFiles().length != 0) {
      System.out.println("You have uncommitted changes.");
      return true;
    } else if (!Utils.join(FOLDER, branchName).exists()) {
      System.out.println("A branch with that name does not exist.");
      return true;
    } else if (branchName.equals(HEAD.getName())) {
      System.out.println("Cannot merge a branch with itself.");
      return true;
    } else {
      return Command.untracked(
          Utils.readObject(Utils.join(FOLDER, branchName), Commit.class).getFiles());
    }
  }

  /**
   * Returns a boolean indictating if there exists a merge conflict given a head branch, the given
   * merged branch, and the split point.
   *
   * @param split the split files
   * @param head the head files
   * @param given the given files
   * @param name the file name to be compared
   * @return true iff there is a conflict
   */
  public static boolean existConflict(
      TreeMap<String, String> split,
      TreeMap<String, String> head,
      TreeMap<String, String> given,
      String name) {
    return ((!split.containsKey(name)
            && head.containsKey(name)
            && !given.get(name).equals(head.get(name)))
        || (split.containsKey(name)
            && !head.containsKey(name)
            && !split.get(name).equals(given.get(name)))
        || (split.containsKey(name)
            && head.containsKey(name)
            && !split.get(name).equals(given.get(name))
            && !split.get(name).equals(head.get(name))
            && !given.get(name).equals(head.get(name))));
  }

  /**
   * Merges files from the given branch into the current branch.
   *
   * @param args {'merge', branch name}
   */
  public static void merge(String[] args) throws IOException {
    if (mergeErrors(args[1])) {
      return;
    }
    Commit latest = latestSplit(args[1]);
    if (latest != null) {
      TreeMap<String, String> head = headCommit().getFiles();
      Commit givenC = Utils.readObject(Utils.join(FOLDER, args[1]), Commit.class);
      TreeMap<String, String> given = givenC.getFiles();
      TreeMap<String, String> split = latest.getFiles();
      HashSet<String> files = new HashSet<>(split.keySet());
      files.addAll(head.keySet());
      files.addAll(given.keySet());
      boolean emerge = false;
      for (String name : files) {
        boolean conflict = false;
        if (given.containsKey(name)) {
          if (existConflict(split, head, given, name)) {
            conflict = true;
          } else if (!given.get(name).equals(split.get(name))) {
            Command.checkoutFile(givenC, name);
            Stage.add(new String[] {"add", name});
          }
        } else if (split.containsKey(name)) {
          if (head.containsKey(name) && !split.get(name).equals(head.get(name))) {
            conflict = true;
          } else if (split.get(name).equals(head.get(name))) {
            Utils.join(Command.CWD, name).delete();
            Stage.rm(new String[] {"rm", name});
          }
        }
        if (conflict) {
          Formatter out = new Formatter();
          out.format("<<<<<<< HEAD\n");
          if (head.containsKey(name)) {
            out.format(Utils.readContentsAsString(Utils.join(Blobs.FOLDER, head.get(name))));
          }
          out.format("=======\n");
          if (given.containsKey(name)) {
            out.format(Utils.readContentsAsString(Utils.join(Blobs.FOLDER, given.get(name))));
          }
          out.format(">>>>>>>\n");
          Utils.writeContents(Utils.join(Command.CWD, name), out.toString());
          Stage.add(new String[] {"add", name});
          emerge = true;
        }
      }
      Formatter mergemsg = new Formatter();
      mergemsg.format("Merged %1$s into %2$s.", args[1], HEAD.getName());
      Commit.commit(
          new String[] {
            "commit",
            mergemsg.toString(),
            Utils.sha1(Utils.readContents(Utils.join(FOLDER, args[1])))
          });
      if (emerge) {
        System.out.println("Encountered a merge conflict.");
      }
    }
  }

  /**
   * Finds the latest split point of the current branch with the name of the given branch.
   *
   * @param name the given branch name
   * @return Commit the latest split commit
   */
  public static Commit latestSplit(String name) throws IOException {
    String currID = Branches.headId();
    String origID = Branches.headId();
    Commit curr = Branches.headCommit();
    String givenID = Utils.sha1(Utils.readContents(Utils.join(FOLDER, name)));
    Commit given = Utils.readObject(Utils.join(Commit.FOLDER, givenID), Commit.class);
    HashSet<String> givenAncestors = getAncestors(given);
    while (true) {
      if (currID.equals(givenID)) {
        System.out.println("Given branch is an ancestor " + "of the current branch.");
        break;
      }
      if (givenAncestors.contains(origID)) {
        System.out.println("Current branch fast-forwarded.");
        checkoutBranch(name);
        break;
      }
      String parent = curr.getParent();
      String mparent = curr.getMparent();
      if (givenAncestors.contains(parent)) {
        return Utils.readObject(Utils.join(Commit.FOLDER, parent), Commit.class);
      } else if (curr.getMparent() != null) {
        return Utils.readObject(Utils.join(Commit.FOLDER, mparent), Commit.class);
      }
      curr = Utils.readObject(Utils.join(Commit.FOLDER, curr.getParent()), Commit.class);
      currID = Utils.sha1(Utils.readContents(Utils.join(Commit.FOLDER, curr.getParent())));
    }
    return null;
  }

  /**
   * Returns a collection of all the ancestors of a given commit.
   *
   * @param c the given commit
   * @return HashSet<String> the ancestors of that commit
   */
  public static HashSet<String> getAncestors(Commit c) {
    HashSet<String> result = new HashSet<>();
    if (c.getParent() == null && c.getMparent() == null) {
      return result;
    }
    if (c.getParent() != null) {
      result.add(c.getParent());
      result.addAll(
          getAncestors(Utils.readObject(Utils.join(Commit.FOLDER, c.getParent()), Commit.class)));
    }
    if (c.getMparent() != null) {
      result.add(c.getMparent());
      result.addAll(
          getAncestors(Utils.readObject(Utils.join(Commit.FOLDER, c.getMparent()), Commit.class)));
    }
    return result;
  }
}
