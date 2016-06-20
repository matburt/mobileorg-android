package com.matburt.mobileorg2.Synchronizers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.matburt.mobileorg2.OrgData.OrgDatabase;
import com.matburt.mobileorg2.OrgData.OrgFile;
import com.matburt.mobileorg2.OrgData.OrgFileParser;
import com.matburt.mobileorg2.OrgData.OrgProviderUtils;
import com.matburt.mobileorg2.R;
import com.matburt.mobileorg2.util.FileUtils;
import com.matburt.mobileorg2.util.OrgFileNotFoundException;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class JGitWrapper {

    // The git dir inside the Context.getFilesDir() folder
    public static String GIT_DIR = "git_dir";

    final static String CONFLICT_FILES = "conflict_files";

    public static void add(String filename, Context context) {
        File repoDir = new File(context.getFilesDir() + "/" + GIT_DIR + "/.git");
        try {
            Git git = Git.open(repoDir);
            git.add()
                    .addFilepattern(filename)
                    .call();

            String addMsg = context.getResources().getString(R.string.sync_git_file_added);
            git.commit()
                    .setMessage(addMsg + filename)
                    .call();

            new PushGitRepoTask(context).execute();
        } catch (GitAPIException | IOException e) {
            e.printStackTrace();
        }
    }



    public static SyncResult pull(final Context context) {
        File repoDir = new File(context.getFilesDir() + "/" + GIT_DIR + "/.git");
        Log.v("git", "pulling");
        SyncResult result = new SyncResult();
        AuthData authData = AuthData.getInstance(context);
        Git git = null;
        try {
            git = Git.open(repoDir);
        } catch (IOException e) {
            e.printStackTrace();
            return result;
        }

        Log.v("git", "got git");

        try {

            Repository repository = git.getRepository();

            ObjectId oldHead = repository.resolve("HEAD^{tree}");

//
//            git.commit()
//                    .setAll(true)
//                    .setMessage("Commit before pulling")
//                    .call();

            git
                    .pull()
                    .setCredentialsProvider(new CredentialsProviderAllowHost(authData.getUser(), authData.getPassword()))
                    .call();
            ObjectId head = repository.resolve("HEAD^{tree}");

            ObjectReader reader = repository.newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, oldHead);
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, head);
            List<DiffEntry> diffs = git.diff()
                    .setNewTree(newTreeIter)
                    .setOldTree(oldTreeIter)
                    .call();

            Log.v("sync", "Pulling succeeded");

            for (DiffEntry entry : diffs) {
                String newpath = entry.getNewPath();
                String oldpath = entry.getOldPath();
                Log.v("sync", "change old : " + oldpath + " -> " + newpath);
                if (newpath.equals("/dev/null")) {
                    result.deletedFiles.add(oldpath);
                } else if (oldpath.equals("/dev/null")) {
                    result.newFiles.add(entry.getNewPath());
                } else {
                    result.changedFiles.add(entry.getNewPath());
                }
            }
            result.setState(SyncResult.State.kSuccess);
            return result;
        } catch(WrongRepositoryStateException e){
            e.printStackTrace();
            handleMergeConflict(git, context);
        } catch (IOException
                | DetachedHeadException
                | TransportException
                | InvalidConfigurationException
                | NoHeadException
                | RefNotFoundException
                | InvalidRemoteException
                | CanceledException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return result;
    }


    static public class CloneGitRepoTask extends AsyncTask<String, Void, Object> {
        Context context;
        ProgressDialog progress;

        public CloneGitRepoTask(Context context){
            this.context = context;
        }

        protected Object doInBackground(String... params) {
            AuthData authData = AuthData.getInstance(context);

            File localPath = new File(context.getFilesDir() + "/" + GIT_DIR);
            FileUtils.deleteFile(localPath);

//			String REMOTE_URL = "ssh://" + userActual + ":" + passActual + "@" + hostActual + ":" + portActual + pathActual;
            String REMOTE_URL = authData.getHost() + "/" + authData.getPath();
            Git git;
            System.setProperty("user.home", context.getFilesDir().getAbsolutePath() );
            Log.v("git", "user home : " + System.getProperty("user.home"));
            Log.v("git","after");

            try {
                git = Git.cloneRepository()
                        .setURI(REMOTE_URL)
                        .setDirectory(localPath)
//						.setCredentialsProvider(allowHosts)
                        .setCredentialsProvider(new CredentialsProviderAllowHost(authData.getUser(), authData.getPassword()))
                        .setBare(false)
                        .call();
            } catch (Exception e) {
                e.printStackTrace();
                return e;
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = new ProgressDialog(context);
            progress.setMessage(context.getString(R.string.please_wait));
            progress.setTitle(context.getString(R.string.signing_in));
            progress.show();
        }

        @Override
        protected void onPostExecute(Object exception) {
            OrgProviderUtils
                    .clearDB(context.getContentResolver());


            parseAll();

            progress.dismiss();
            if (exception == null) {
                Toast.makeText(context, "Synchronization successful !", Toast.LENGTH_LONG).show();
                ((Activity) context).finish();
                return;
            }

            if(exception instanceof TransportException){
                Toast.makeText(context, "Authentification failed", Toast.LENGTH_LONG).show();
            }else if (exception instanceof InvalidRemoteException) {
                Toast.makeText(context, "Path does not exist or is not a valid repository", Toast.LENGTH_SHORT).show();
            }else if(exception instanceof UnableToPushException){
//				git config receive.denyCurrentBranch ignore
                Toast.makeText(context, "Push test failed. Make sure the repository is bare.", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(context, exception.toString(), Toast.LENGTH_LONG).show();
                ((Exception)exception).printStackTrace();
            }

        }

        void parseAll(){
            SSHSynchronizer syncher = new SSHSynchronizer(context);
            File f = new File(syncher.getAbsoluteFilesDir(context));
            File file[] = f.listFiles();
            if (file == null) return;
            Log.d("Files", "Size: "+ file.length);
            for (int i=0; i < file.length; i++)
            {
                String filename = file[i].getName();
                if(filename.equals(".git")) continue;
                OrgFile orgFile = new OrgFile(filename, filename);
                FileReader fileReader = null;
                try {
                    fileReader = new FileReader(syncher.getAbsoluteFilesDir(context) + "/" + filename);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                OrgDatabase db = OrgDatabase.getInstance(context);

                final OrgFileParser parser = new OrgFileParser(db, context);

                OrgFileParser.parseFile(orgFile, bufferedReader, parser, context);
                Log.d("Files", "FileName:" + file[i].getName());
            }
        }

    }

    static public class PushGitRepoTask extends AsyncTask<String, Void, Void> {
        Context context;

        public PushGitRepoTask(Context context) {
            this.context = context;
        }

        protected Void doInBackground(String... params) {
            Log.v("git", "pushing");

            File repoDir = new File(context.getFilesDir() + "/" + GIT_DIR + "/.git");
            Git git = null;
            try {
                git = Git.open(repoDir);
                // Stage all changed files, omitting new files, and commit with one command
                git.commit()
                        .setAll(true)
                        .setMessage("Commit changes to all files")
                        .call();

                AuthData authData = AuthData.getInstance(context);
                git.push()
                        .setCredentialsProvider(new CredentialsProviderAllowHost(authData.getUser(), authData.getPassword()))
                        .call();
                System.out.println("Committed all changes to repository at ");
            } catch (IOException | UnmergedPathsException e) {
                e.printStackTrace();
            } catch (WrongRepositoryStateException e) {
                e.printStackTrace();
                handleMergeConflict(git,context);
            } catch (ConcurrentRefUpdateException e) {
                e.printStackTrace();
            } catch (NoHeadException e) {
                e.printStackTrace();
            } catch (NoMessageException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    static public class MergeTask extends AsyncTask<String, Void, Void> {
        Context context;

        public MergeTask(Context context) {
            this.context = context;
        }

        protected Void doInBackground(String... params) {
            Log.v("git", "merging");

            File repoDir = new File(context.getFilesDir() + "/" + GIT_DIR + "/.git");
            Git git = null;
            try {
                git = Git.open(repoDir);

                CheckoutCommand coCmd = git.checkout();
// Commands are part of the api module, which include git-like calls
                coCmd.setName("master");
                coCmd.setCreateBranch(false); // probably not needed, just to make sure
                Ref ref = coCmd.call();

                // Stage all changed files, omitting new files, and commit with one command
                git.merge()
                        .setStrategy(MergeStrategy.OURS)
                        .include(ref)
                        .call();

                git.add()
                        .addFilepattern("google.org").call();

                org.eclipse.jgit.api.Status status = git.status().call();
                System.out.println("Added: " + status.getAdded());
                System.out.println("Changed: " + status.getChanged());
                System.out.println("Conflicting: " + status.getConflicting());
                                System.out.println("Missing: " + status.getMissing());
                System.out.println("Modified: " + status.getModified());
                System.out.println("Removed: " + status.getRemoved());
                System.out.println("Untracked: " + status.getUntracked());

                AuthData authData = AuthData.getInstance(context);
                git.push()
                        .setCredentialsProvider(new CredentialsProviderAllowHost(authData.getUser(), authData.getPassword()))
                        .call();
                System.out.println("Committed all changes to repository at ");
            } catch (IOException | UnmergedPathsException e) {
                e.printStackTrace();
            } catch (WrongRepositoryStateException e) {
                e.printStackTrace();
                handleMergeConflict(git,context);
            } catch (ConcurrentRefUpdateException e) {
                e.printStackTrace();
            } catch (NoHeadException e) {
                e.printStackTrace();
            } catch (NoMessageException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    static class UnableToPushException extends Exception {

    }

    private static void handleMergeConflict(Git git, Context context){
        Status status = null;

        try {
            status = git.status().call();
            ContentResolver resolver = context.getContentResolver();
            for(String file: status.getConflicting()){
                OrgFile f = new OrgFile(file, resolver);
                ContentValues values = new ContentValues();
                values.put("comment","conflict");
                f.updateFileInDB(resolver, values);
            }

        } catch (GitAPIException e1) {
            e1.printStackTrace();
            return;
        } catch (OrgFileNotFoundException e) {
            e.printStackTrace();
        }


    }

}
