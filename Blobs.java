package gitlet;

import java.io.File;
import java.io.Serializable;

/**
 * Class representing a directory of File contents.
 *
 * @author Randy Nguyen
 */
public class Blobs implements Serializable {

  /** The pathway to the objects directory. */
  static final File FOLDER = Utils.join(Command.GITLET_FOLDER, "objects");
}
