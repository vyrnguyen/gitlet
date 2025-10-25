package gitlet;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Class organizing commands to be called in other classes. Serves as the central hub class pathway.
 * Also provides general service methods.
 *
 * @author Randy Nguyen
 */
public class Command {

  /** Current Working Directory. */
  static final File CWD = new File(System.getProperty("user.dir"));

  /** Main metadata folder. */
  static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");

  /** File containing information (literal name) about the head branch. */
  static final File HEAD = Utils.join(GITLET_FOLDER, "HEAD");

  /**
   * Creates a new Gitlet VCS in the CWD. Initializes one commit that contains no files with the
   * message "initial commit" in Epoch time. Sets up all persistence (branches, directories, etc.)
   *
   * @param args arguments
   * @throws IOException when creating abnormal file
   */
  public static void init(String[] args) throws IOException {
    GITLET_FOLDER.mkdirs();
    HEAD.createNewFile();
    Utils.writeContents(HEAD, "master");
    Commit.FOLDER.mkdirs();
    Branches.FOLDER.mkdir();
    Stage.FOLDER.mkdirs();
    Blobs.FOLDER.mkdirs();
    Stage.ADD.mkdirs();
    Stage.RM.mkdirs();
    Commit initCommit = new Commit("initial commit", null, null, false);
    Branches.MASTER.createNewFile();
    Utils.writeObject(Branches.MASTER, initCommit);
    File initFile = Utils.join(Commit.FOLDER, Branches.headId());
    initFile.createNewFile();
    Utils.writeObject(initFile, initCommit);
  }

  /**
   * Prints out the metadata of all Commit objects starting from the current one (head), going
   * backwards until there are no more. Set to be in PST timezone.
   *
   * <p>If a global log is called, it only prints out the log info of the given String ID.
   *
   * @param id A commit sha1
   * @param args arguments
   * @param global boolean indicating if this is a global log
   */
  public static void log(String id, boolean global, String[] args) {
    Formatter info = new Formatter();
    Commit curr = Utils.readObject(Utils.join(Commit.FOLDER, id), Commit.class);
    while (true) {
      info.format("===\ncommit %s\n", id);
      if (curr.isMerge()) {
        info.format(
            "Merge: %1$s %2$s\n",
            curr.getParent().substring(0, 7), curr.getMparent().substring(0, 7));
      }
      ZonedDateTime zdt = curr.getTimestamp().atZone(ZoneId.systemDefault());
      int date = zdt.getDayOfMonth();
      int yr = zdt.getYear();
      String pst = "-0800";
      String month = zdt.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
      String day = zdt.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
      String time = timeform(zdt.getHour(), zdt.getMinute(), zdt.getSecond());
      info.format(
          "Date: %1$s %2$s %3$d %4$s %5$d %6$s\n%7$s\n\n",
          day, month, date, time, yr, pst, curr.getMessage());
      if (global) {
        break;
      } else {
        id = curr.getParent();
        if (id == null) {
          break;
        }
        curr = Utils.readObject(Utils.join(Commit.FOLDER, curr.getParent()), Commit.class);
      }
    }
    System.out.println(info);
  }

  /**
   * Displays information about all commits ever made in a random order.
   *
   * @param args arguments
   */
  public static void globalLog(String[] args) {
    List<String> commitIds = Utils.plainFilenamesIn(Commit.FOLDER);
    for (String id : commitIds) {
      log(id, true, args);
    }
  }

  /**
   * Returns a string of the current time in the form XX:YY:ZZ. Each paramater is concatenated with
   * a 0 iff it X/Y/Z is < 10.
   *
   * @param x hour
   * @param y min
   * @param z sec
   */
  public static String timeform(int x, int y, int z) {
    Formatter time = new Formatter();
    String[] times = new String[] {Integer.toString(x), Integer.toString(y), Integer.toString(z)};
    for (int i = 0; i < times.length; i++) {
      if (Integer.parseInt(times[i]) < 10) {
        times[i] = "0" + times[i];
      }
    }
    time.format("%1$s:%2$s:%3$s", times[0], times[1], times[2]);
    return time.toString();
  }

  /**
   * Displays what branches currently exist, and marks the current branch with a *. Also displays
   * what files have been staged for addition, removal, were modified (but not staged) and untracked
   * files.
   */
  public static void status() {
    Formatter out = new Formatter();
    List<String> branches = Utils.plainFilenamesIn(Branches.FOLDER);
    String head = Utils.readContentsAsString(Command.HEAD);
    out.format("=== Branches ===\n*%1$s\n", head);
    for (String name : branches) {
      if (!name.equals(head)) {
        out.format(name + "\n");
      }
    }
    out.format("\n=== Staged Files ===\n");
    List<String> staged = Utils.plainFilenamesIn(Stage.ADD);
    for (String name : staged) {
      out.format(name + "\n");
    }
    out.format("\n=== Removed Files ===\n");
    List<String> removed = Utils.plainFilenamesIn(Stage.RM);
    for (String name : removed) {
      out.format(name + "\n");
    }
    out.format("\n=== Modifications Not Staged For Commit ===\n");
    List<String> modified = Utils.plainFilenamesIn(CWD);
    TreeMap<String, String> tracked = Branches.headCommit().getFiles();
    for (String name : modified) {
      File curr = Utils.join(CWD, name);
      if (tracked.containsKey(name)
              && (!tracked.get(name).equals(Utils.sha1(Utils.readContents(curr))))
              && (!staged.contains(name))
              && !removed.contains(name)
          || (staged.contains(name)
              && !Utils.sha1(Utils.readContents(curr))
                  .equals(Utils.sha1(Utils.readContents(Utils.join(Stage.ADD, name)))))) {
        out.format(name + " (modified)\n");
      }
    }
    for (String name : tracked.keySet()) {
      if (!removed.contains(name) && !Utils.join(CWD, name).exists()) {
        out.format(name + " (deleted)\n");
      }
    }
    out.format("\n=== Untracked Files ===\n");
    for (String name : modified) {
      if (!tracked.containsKey(name) && !Utils.join(Stage.ADD, name).exists()) {
        out.format(name + "\n");
      }
    }
    out.format("\n");
    System.out.println(out);
  }

  /**
   * A method that calls a particular checkout depending on the length of its arguments ARGS.
   *
   * @param args String[]
   * @throws IOException in event of abnormal files
   */
  public static void checkout(String[] args) throws IOException {
    if (args.length == 3) {
      if (!args[1].equals("--")) {
        System.out.println("Incorrect operands.");
        return;
      }
      Commit headCommit = Branches.headCommit();
      Command.checkoutFile(headCommit, args[2]);
    } else if (args.length == 4) {
      if (!args[2].equals("--")) {
        System.out.println("Incorrect operands.");
        return;
      }
      Commit.checkoutId(args[1], args[3], false);
    } else {
      Branches.checkoutBranch(args[1]);
    }
  }

  /**
   * Given a Commit C and String FILENAME, updates the CWD with the file named FILENAME in that
   * commit. Throws error if the filename is not in the Commit.
   *
   * @param c Commit
   * @param filename String
   * @throws IOException in event of abnormal files
   */
  public static void checkoutFile(Commit c, String filename) throws IOException {
    if (!c.getFiles().containsKey(filename)) {
      System.out.println("File does not exist in that commit.");
    } else {
      File cwdVer = Utils.join(CWD, filename);
      String sha = c.getFiles().get(filename);
      File desired = Utils.join(Blobs.FOLDER, sha);
      cwdVer.createNewFile();
      Utils.writeContents(cwdVer, Utils.readContents(desired));
    }
  }

  /**
   * Checks if there is an untracked file in the CWD compared to another tracked file map.
   *
   * @param givenFiles tracked files
   * @return boolean
   */
  public static boolean untracked(TreeMap<String, String> givenFiles) {
    List<String> cwdNames = Utils.plainFilenamesIn(CWD);
    TreeMap<String, String> headFiles = Branches.headCommit().getFiles();
    for (String name : cwdNames) {
      File curr = Utils.join(Command.CWD, name);
      if (!headFiles.containsKey(name)
          && givenFiles.containsKey(name)
          && (!givenFiles.get(name).equals(Utils.sha1(Utils.readContents(curr))))) {
        System.out.println(
            "There is an untracked file in the way; delete it, or add and commit it first.");
        return true;
      }
    }
    return false;
  }
}
