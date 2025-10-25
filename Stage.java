package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A class holding all staging and removal actions.
 *
 * @author Randy Nguyen
 */
public class Stage {

  /** The staging folder for addition and removal. */
  static final File FOLDER = Utils.join(Command.GITLET_FOLDER, "stage");

  /** A directory of files staged for addition. */
  static final File ADD = Utils.join(FOLDER, "add");

  /** A directory of files staged for removal. */
  static final File RM = Utils.join(FOLDER, "rm");

  /**
   * Adds the given a copy of the filename in the staging area. If it already exists, the contents
   * are replaced with the CWD version. If it's the same, it is removed from the stage if there. If
   * the file is staged for removal, remove it from there. Runs in O(lg(N))
   *
   * @param args Array in format: {'add', filename}
   */
  public static void add(String[] args) throws IOException {
    File copy = Utils.join(ADD, args[1]);
    File cwd = Utils.join(Command.CWD, args[1]);
    copy.createNewFile();
    Utils.writeContents(copy, Utils.readContents(cwd));
    Commit headCommit = Branches.headCommit();
    if (headCommit.getFiles().containsKey(args[1])) {
      String headSha = headCommit.getFiles().get(args[1]);
      String copySha = Utils.sha1(Utils.readContents(copy));
      if (headSha.equals(copySha)) {
        copy.delete();
      }
    }
    File rmVer = Utils.join(RM, args[1]);
    if (rmVer.exists()) {
      rmVer.delete();
    }
  }

  /**
   * Unstage the file if it is currently staged for addition. If the file is tracked in the current
   * commit, stage it for removal and remove the file from the working directory if possible.
   *
   * @param args {'rm', filename}
   */
  public static void rm(String[] args) throws IOException {
    List<String> stagedFiles = Utils.plainFilenamesIn(ADD);
    boolean inStage = stagedFiles.contains(args[1]);
    boolean inTracked = Branches.headCommit().getFiles().containsKey(args[1]);
    if (!inStage && !inTracked) {
      System.out.println("No reason to remove the file.");
    } else {
      if (inStage) {
        File stageVer = Utils.join(Stage.ADD, args[1]);
        stageVer.delete();
      }
      if (inTracked) {
        File rmVer = Utils.join(Stage.RM, args[1]);
        rmVer.createNewFile();
        Utils.join(Command.CWD, args[1]).delete();
      }
    }
  }

  /** Clears the staging area (the ADD and RM directories). */
  public static void clearStage() {
    for (File f : Stage.ADD.listFiles()) {
      f.delete();
    }
    for (File f : Stage.RM.listFiles()) {
      f.delete();
    }
  }
}
