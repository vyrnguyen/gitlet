package gitlet;

import java.io.IOException;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Randy Nguyen
 */
public class Main {
  /** Usage: java gitlet.Main ARGS, where ARGS contains <COMMAND> <OPERAND> .... */
  public static void main(String... args) throws IOException {
    if (args.length == 0) {
      System.out.println("Please enter a command.");
    } else if (!args[0].equals("init") && !Command.GITLET_FOLDER.exists()) {
      System.out.println("Not in an initialized Gitlet directory.");
    } else {
      switch (args[0]) {
        case "init":
          if (Command.GITLET_FOLDER.exists()) {
            System.out.println(
                "A Gitlet version-control system " + "already exists in the current directory.");
          } else {
            Command.init(args);
          }
          break;
        case "add":
          if (Utils.join(Command.CWD, args[1]).exists()) {
            Stage.add(args);
          } else {
            System.out.println("File does not exist.");
          }
          break;
        case "commit":
          if (args[1].length() <= 0) {
            System.out.println("Please enter a commit message.");
          } else {
            Commit.commit(args);
          }
          break;
        case "log":
          Command.log(Branches.headId(), false, args);
          break;
        case "checkout":
          Command.checkout(args);
          break;
        case "rm":
          Stage.rm(args);
          break;
        case "global-log":
          Command.globalLog(args);
          break;
        case "find":
          Commit.find(args);
          break;
        case "status":
          Command.status();
          break;
        case "branch":
          Branches.branch(args);
          break;
        case "rm-branch":
          Branches.removeBranch(args);
          break;
        case "reset":
          Commit.reset(args);
          break;
        case "merge":
          Branches.merge(args);
          break;
        default:
          System.out.println("No command with that name exists.");
      }
    }
  }
}
