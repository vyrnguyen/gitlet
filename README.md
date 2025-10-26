# Gitlet

A mini version control system that emulates some of Git's basic functionalities.

## Differences from Git

Real Git distinguishes several different kinds of objects. For this project, the
important ones are

- **_blobs_**: Essentially the contents of files.
- **_trees_**: Directory structures mapping names to references to blobs and
  other trees (subdirectories).
- **_commits_**: Combinations of log messages, other metadata (commit date,
  author, etc.), a reference to a tree, and references to parent commits. This
  repository also maintains a mapping from branch heads.

Additionally, this project is simplified from Git further by:

- Incorporating trees into commits and not dealing with subdirectories (so there
  will be one "flat" directory of plain files for each repository).
- Limiting to merges that reference two parents (in real Git, there can be any
  number of parents.)
- Having our metadata consist only of a timestamp and log message. A commit,
  therefore, will consist of a log message, timestamp, a mapping of file names to
  blob references, a parent reference, and (for merges) a second parent reference.

## Usage & Documentation

First, make sure to compile all files by running `make` in the `gitlet`
directory.

1. **init**

   - `java gitlet.Main init`

   - Creates a new Gitlet version-control system in the current directory. This system will automatically start with
     one commit: a commit that contains no files and has the commit message initial
     commit (just like that, with no punctuation). It will have a single branch:
     `main`, which initially points to this initial commit and will be the current
     branch. The timestamp for this initial commit will be 00:00:00 UTC, Thursday, 1
     January 1970. (called "The (Unix) Epoch", represented internally by the time 0.)
     Since the initial commit in all repositories created by Gitlet will have exactly
     the same content, it follows that all repositories will automatically share this
     commit (they will all have the same UID) and all commits in all repositories
     will trace back to it.

   - If there is already a Gitlet version-control system in
     the current directory, it aborts and prints the error message
     `A Gitlet version-control system already exists in the current directory.`

1. **add**

   - `java gitlet.Main add [file name]`

   - Adds a copy of the file as it currently exists to the staging area (see the
     description of the commit command). For this reason, adding a file is also
     called *staging* the file for addition.

   - Staging an already-staged file overwrites the previous entry in the staging
     area with the new contents.

   - If the current working version of the file is identical to the version in
     the current commit, it does not stage it to be added, and removes it from the
     staging area if it is already there (as can happen when a file is changed,
     added, and then changed back). The file will no longer be staged for removal
     (see gitlet rm) if it was at the time of the command.

   - If the file doesn't exist, it prints `File does not exist.` and exits
     without changing anything.

1. **commit**

   - `java gitlet.Main commit [message]`

   - Saves a snapshot of tracked files in the current commit and
     staging area so they can be restored at
     a later time, creating a new commit. The commit is said to be tracking the saved
     files. By default, each commit's snapshot of files will be exactly the same as
     its parent commit's snapshot of files; it will keep versions of files exactly as
     they are and not update them. A commit will only update the contents of files
     it is tracking that have been staged for addition at the time of commit, in
     which case the commit will now include the version of the file that was staged
     instead of the version it got from its parent. A commit will save and start
     tracking any files that were staged for addition but weren't tracked by its
     parent. Finally, files tracked in the current commit may be untracked in the new
     commit as a result of being staged for removal by the rm command (below).

   > **TLDR**: By default a commit is the same as its parent. Files
   > staged for addition and removal are the updates to the commit. Of course,
   > the date (and likely the message) will also differ from the parent.

   - Some additional points about commit:

     - The staging area is cleared after a commit.

     - The commit command never adds, changes, or removes files in the working
       directory (other than those in the .gitlet directory). The rm command
       will remove such files, as well as stage them for removal so that
       they will be untracked after a commit.

     - Any changes made to files after staging for addition or removal are
       ignored by the commit command, which only modifies the contents of the
       .gitlet directory. For example, if you remove a tracked file using the
       Unix rm command (rather than Gitlet's command of the same name), it has
       no effect on the next commit, which will still contain the deleted
       version of the file.

     - After the commit command, the new commit is added as a new node in the
       commit tree.

     - The commit just made becomes the "current commit", and the head pointer
       now points to it. The previous head commit is this commit's parent
       commit.

     - Each commit should contain the date and time it was made.

     - Each commit has a log message associated with it that describes the
       changes to the files in the commit. This is specified by the user. The
       entire message should take up only one entry in the array args that is
       passed to main. To include multiword messages, you'll have to surround
       them in quotes.

     - Each commit is identified by its SHA-1 id, which must include the file
       (blob) references of its files, parent reference, log message, and
       commit time.

     - If no files have been staged, it will abort and print the message,
       `No changes added to the commit. Every commit must have a non-blank message.`
       If it doesn't, print the error message`Please enter a commit message.` It is
       not a failure for tracked files to be missing from the working directory or
       changed in the working directory. Just ignore everything outside the .gitlet
       directory entirely.
> [!IMPORTANT]  
> **Differences from real git**:
> In real git, commits may have multiple parents
> (due to merging) and also have considerably more metadata.

1. **rm**

   - `java gitlet.Main rm [file name]`

   - Unstage the file if it is currently staged for addition. If the file is
     tracked in the current commit, stage it for removal and remove the file from
     the working directory if the user has not already done so (do not remove it
     unless it is tracked in the current commit).

   - If the file is neither staged nor tracked by the head commit, print the
     error message `No reason to remove the file.`

1. **log**

   - `java gitlet.Main log`

   - Starting at the current head commit, display information about each commit
     backwards along the commit tree until the initial commit, following the first
     parent commit links, ignoring any second parents found in merge commits. (In
     regular Git, this is what you get with git log --first-parent). This set of
     commit nodes is called the commit's history. For every node in this history,
     the information it should display is the commit id, the time the commit was
     made, and the commit message.

1. **global-log**

   - `java gitlet.Main global-log`

   - Like log, except displays information about all commits ever made in an
     unordered fashion.

1. **find**

   - `java.gitletMain find [commit message]`

   - Prints out the ids of
     all commits that have the given commit message, one per line. If there are
     multiple such commits, it prints the ids out on separate lines. The commit
     message is a single operand; to indicate a multiword message, put the operand in
     quotation marks, as for the commit command above.

   - If no such commit exists,
     print the error message, `Found no commit with that message.`

> [!IMPORTANT]  
> **Differences from real git**:
> Doesn't exist in real git. Similar effects can be achieved by grepping
> the output of log.

1. **status**

   - `java gitlet.Main status`

   - Displays what branches currently exist, and marks the current branch with a
     \*. Also displays what files have been staged for addition or removal.

   - Entries are listed in lexicographic order.

   - A file in the working directory is "modified but not staged" if it is:

     - Tracked in the current commit, changed in the working directory, but not
       staged; or
     - Staged for addition, but with different contents than in the working
       directory; or
     - Staged for addition, but deleted in the working directory; or
     - Not staged for removal, but tracked in the current commit and deleted from
       the working directory.
     - The final category ("Untracked Files") is for files present in the working
       directory but neither staged for addition nor tracked. This includes files
       that have been staged for removal, but then re-created without Gitlet's
       knowledge. Ignore any subdirectories that may have been introduced, since
       Gitlet does not deal with them.

1. **checkout**

   - `java gitlet.Main checkout -- [file name]`

     - Takes the version of the file as it exists in the head commit, the front
       of the current branch, and puts it in the working directory, overwriting the
       version of the file that's already there if there is one. The new version of
       the file is not staged.

     - If the file does not exist in the previous commit, abort, printing the
       error message File does not exist in that commit.

   - `java gitlet.Main checkout [commit id] -- [file name]`

     - Takes the version of the file as it exists in the commit with the given
       id, and puts it in the working directory, overwriting the version of the file
       that's already there if there is one. The new version of the file is not
       staged.

     - If no commit with the given id exists, print No commit with that id
       exists. Otherwise, if the file does not exist in the given commit, print the
       same message as for failure case 1.

   - `java gitlet.Main checkout [branch name]`

     - Takes all files in the commit at the head of the given branch, and puts
       them in the working directory, overwriting the versions of the files that are
       already there if they exist. Also, at the end of this command, the given
       branch will now be considered the current branch (HEAD). Any files that are
       tracked in the current branch but are not present in the checked-out branch
       are deleted. The staging area is cleared, unless the checked-out branch is
       the current branch (see point below).

     - If no branch with that name exists, print No such branch exists. If that
       branch is the current branch, print No need to checkout the current branch.
       If a working file is untracked in the current branch and would be overwritten
       by the checkout, print There is an untracked file in the way; delete it, or
       add and commit it first. and exit; perform this check before doing anything
       else.

> [!IMPORTANT]  
> **Differences from real git**: Real git does not clear the staging area and
> stages the file that is checked out. Also, it won't do a checkout that would
> overwrite or undo changes (additions or removals) that you have staged.

1. **branch**

   - `java gitlet.Main branch [branch name]`

   - Creates a new branch with the given name, and points it at the current head
     node. A branch is nothing more than a name for a reference (a SHA-1
     identifier) to a commit node. This command does NOT immediately switch to the
     newly created branch (just as in real Git). Before you ever call branch, your
     code should be running with a default branch called "main".

   - If a branch with the given name already exists, it prints the error message, `A branch with that name already exists.`

1. **rm-branch**

   - `java gitlet.Main rm-branch [branch name]`
   - Deletes the branch with the given name. This only means to delete the
     pointer associated with the branch; it does not mean to delete all commits
     that were created under the branch.
   - If a branch with the given name does not exist, then abort and print the error
     message `A branch with that name does not exist.`
   - If you try to remove the
     branch you're currently on, abort, and print the error message `Cannot remove the current branch.`

1. **reset**

   - `java gitlet.Main reset [commit id]`

   - Checks out all the files tracked by the given commit. Removes tracked files
     that are not present in that commit. Also moves the current branch's head to
     that commit node. See the intro for an example of what happens to the head
     pointer after using reset. The [commit id] may be abbreviated as for
     checkout. The staging area is cleared. The command is essentially checkout of
     an arbitrary commit that also changes the current branch head.

   - If no commit with the given id exists, print `No commit with that id exists.`

   - If a working file is untracked in the current branch and would be overwritten
     by the reset, print `There is an untracked file in the way; delete it, or add and commit it first.`
     and exit; perform this check before doing anything else.

> [!IMPORTANT]  
> **Differences from real git**: This command is closest to using the --hard
> option, as in git reset --hard [commit hash].

1. **merge**

   - `java gitlet.Main merge [branch name]`

   - Merges files from the given branch into the current branch.

   - $O(N lg N + D)$, where $N$ is the total number of ancestor commits for the two
     branches and $D$ is the total amount of data in all the files under these
     commits.

   - If there are staged additions or removals present, print the
     error message `You have uncommitted changes.` and exit.

   - If a branch with the
     given name does not exist, print the error message `A branch with that name does not exist.`

   - If attempting to merge a branch with itself, print the error
     message `Cannot merge a branch with itself.`

   - If merge would generate an error
     because the commit that it does has no changes in it, just let the normal
     commit error message for this go through.

   - If an untracked file in the current
     commit would be overwritten or deleted by the merge, print `There is an untracked file in the way; delete it, or add and commit it first.` and exit;
     perform this check before doing anything else.
     
> [!IMPORTANT]  
> **Differences from real git**:
> - Real Git does a more subtle job of merging
>   files, displaying conflicts only in places where both files have changed
>   since the split point.
> - Real Git has a different way to decide which of
>   multiple possible split points to use.
> - Real Git will force the user to
>   resolve the merge conflicts before committing to complete the merge. Gitlet
>   just commits the merge, conflicts and all, so that you must use a separate
>   commit to resolve problems.
> - Real Git will complain if there are unstaged
>   changes to a file that would be changed by a merge. You may do so as well if
>   you want, but we will not test that case.

# Deletion

Simply remove the `.gitlet` folder from the directory that you initialized it in.
