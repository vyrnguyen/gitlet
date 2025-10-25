package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * CLass representing commit related actions.
 *
 * @author Randy Nguyen
 */
public class Commit implements Serializable {

  /** File containing the saved commit objects. */
  static final File FOLDER = Utils.join(Command.GITLET_FOLDER, "commits");

  /** The message provided with the commit. */
  private final String _message;

  /** The timestamp provided with the commit. */
  private final Instant _timestamp;

  /** The parent of this commit represented as its SHA1. */
  private final String _parent;

  /** The merged parent of this commit represented as its SHA1. */
  private String _mparent;

  /** Denotes whether this commit is the result of a merge. */
  private boolean _merge;

  /**
   * A TreeMap with the key being the File abstract name and value being the SHA1 of its contents.
   */
  private final TreeMap<String, String> _trackedFiles;

  /** Typical long SHA-1 code length. */
  private static final int SHA = 40;

  /**
   * Contructor for a commit object.
   *
   * @param message given user message
   * @param parent sha1 of the parent of this commit
   * @param mparent the second parent of this commit
   * @param merge denotes whether if this commit is from a merge
   */
  public Commit(String message, String parent, String mparent, boolean merge) {
    this._message = message;
    this._parent = parent;
    this._mparent = mparent;
    this._merge = merge;
    this._trackedFiles = new TreeMap<>();
    if (parent == null) {
      this._timestamp = Instant.EPOCH;
    } else {
      this._timestamp = Instant.now(Clock.systemDefaultZone());
    }
  }

  /**
   * Creates a new Commit object with its parent being the Commit of the head branch. Then it adds
   * all the files being Staged, replacing the ones that its parent tracked. Clears the staging
   * area.
   *
   * <p>The head branch now points to this new Commit object.
   *
   * <p>Untracks anything staged for removal that was tracked by its parent.
   *
   * @param args message
   * @throws IOException for abnormal files
   */
  public static void commit(String[] args) throws IOException {
    List<String> stageNames = Utils.plainFilenamesIn(Stage.ADD);
    List<String> rmNames = Utils.plainFilenamesIn(Stage.RM);
    if (stageNames.size() > 0 || rmNames.size() > 0 || args.length > 2) {
      Commit curr = new Commit(args[1], Branches.headId(), null, false);
      if (args.length > 2) {
        curr = new Commit(args[1], Branches.headId(), args[2], true);
      }
      TreeMap<String, String> parentFiles =
          Utils.readObject(Utils.join(Commit.FOLDER, curr.getParent()), Commit.class).getFiles();
      Set<String> names = parentFiles.keySet();
      for (String name : names) {
        if (!rmNames.contains(name)) {
          curr.getFiles().put(name, parentFiles.get(name));
        } else {
          File r = Utils.join(Command.CWD, name);
          r.delete();
        }
      }
      TreeMap<String, String> stageFiles = new TreeMap<>();
      for (String name : stageNames) {
        File f = Utils.join(Stage.ADD, name);
        String sha = Utils.sha1(Utils.readContents(f));
        stageFiles.put(name, sha);
        File copy = Utils.join(Blobs.FOLDER, sha);
        Utils.writeContents(copy, Utils.readContents(f));
        copy.createNewFile();
      }
      curr.getFiles().putAll(stageFiles);
      Utils.writeObject(Branches.HEAD, curr);
      File snap = Utils.join(FOLDER, Branches.headId());
      Utils.writeObject(snap, curr);
      snap.createNewFile();
      Stage.clearStage();
    } else {
      System.out.println("No changes added to the commit.");
    }
  }

  /**
   * Given a SHA1 ID of a commit object and the name of one of its tracked files, FILENAME,
   * overwrites the CWD version with the same filename.
   *
   * @param id the sha1 of a commit
   * @param filename name of the tracked file
   * @param reset denoting if a reset was called
   * @throws IOException if the Commit ID does not exist
   */
  public static void checkoutId(String id, String filename, boolean reset) throws IOException {
    List<String> commitNames = Utils.plainFilenamesIn(FOLDER);
    boolean found = false;
    boolean shortened = id.length() < SHA;
    int abbreviated = id.length();
    if (shortened) {
      for (String name : commitNames) {
        if (id.equals(name.substring(0, abbreviated))) {
          found = true;
          id = name;
          break;
        }
      }
    }
    if ((shortened && !found) || !Utils.join(FOLDER, id).exists()) {
      System.out.println("No commit with that id exists.");
    } else if (!reset) {
      Command.checkoutFile(Utils.readObject(Utils.join(FOLDER, id), Commit.class), filename);
    } else {
      Commit c = Utils.readObject(Utils.join(FOLDER, id), Commit.class);
      TreeMap<String, String> tracked = c.getFiles();
      if (Command.untracked(tracked)) {
        return;
      }
      Set<String> filenames = tracked.keySet();
      List<String> cwdNames = Utils.plainFilenamesIn(Command.CWD);
      for (String name : filenames) {
        Command.checkoutFile(c, name);
      }
      for (String name : cwdNames) {
        if (!filenames.contains(name)) {
          Utils.join(Command.CWD, name).delete();
        }
      }
      Utils.writeContents(Branches.HEAD, Utils.readContents(Utils.join(FOLDER, id)));
      Stage.clearStage();
    }
  }

  /**
   * Prints out the ids of all commits that have the given commit message, one per line. If there
   * are multiple such commits, it prints the ids out on separate lines.
   *
   * @param args {'find', commit message}
   */
  public static void find(String[] args) {
    List<String> commitIds = Utils.plainFilenamesIn(FOLDER);
    boolean found = false;
    for (String id : commitIds) {
      String msg = Utils.readObject(Utils.join(FOLDER, id), Commit.class).getMessage();
      if (msg.equals(args[1])) {
        System.out.println(id);
        found = true;
      }
    }
    if (!found) {
      System.out.println("Found no commit with that message.");
    }
  }

  /**
   * Checks out all the files tracked by the given commit. Removes tracked files that are not
   * present in that commit. Also moves the current branch's head to that commit node. The staging
   * area is cleared.
   *
   * @param args {'reset', commit id}
   */
  public static void reset(String[] args) throws IOException {
    checkoutId(args[1], "", true);
  }

  /**
   * Retrieves the message of a commit.
   *
   * @return String message
   */
  public String getMessage() {
    return this._message;
  }

  /**
   * Retrieves the timestamp of commit.
   *
   * @return Instant timestamp
   */
  public Instant getTimestamp() {
    return this._timestamp;
  }

  /**
   * Retrieves the parent of this commit.
   *
   * @return String parent commit sha1
   */
  public String getParent() {
    return this._parent;
  }

  /**
   * Returns the tracked files of this commit.
   *
   * @return TreeMap<String, String> tracked files
   */
  public TreeMap<String, String> getFiles() {
    return this._trackedFiles;
  }

  /**
   * Returns whether this commit object is from a merge.
   *
   * @return true if merge, else false
   */
  public boolean isMerge() {
    return this._merge;
  }

  /**
   * Returns the second merged parent from this commit.
   *
   * @return String the merged parent.
   */
  public String getMparent() {
    return this._mparent;
  }
}
