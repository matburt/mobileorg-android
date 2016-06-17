package com.matburt.mobileorg2.Synchronizers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.matburt.mobileorg2.OrgData.OrgDatabase;
import com.matburt.mobileorg2.OrgData.OrgFile;
import com.matburt.mobileorg2.OrgData.OrgFileParser;
import com.matburt.mobileorg2.R;
import com.matburt.mobileorg2.util.FileUtils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
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
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.FS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;


public class JGitWrapper {

    public static String GIT_DIR = "git_dir";



    class CustomConfigSessionFactory extends JschConfigSessionFactory
    {
        @Override
        protected void configure(OpenSshConfig.Host hc, Session session) {
            session.setConfig("StrictHostKeyChecking", "yes");
        }

        @Override
        protected JSch getJSch(final OpenSshConfig.Host hc, FS fs) throws JSchException {
            JSch jsch = super.getJSch(hc, fs);
            jsch.removeAllIdentity();
            jsch.addIdentity( "/data/data/yoyo" );
            return jsch;
        }
    }

    static public class CloneGitRepoTask extends AsyncTask<String, Void, Object> {
        Context context;
        ProgressDialog progress;

        public CloneGitRepoTask(Context context){
            this.context = context;
        }

		protected Object doInBackground(String... params) {

			File localPath = new File(context.getFilesDir() + "/" + GIT_DIR);
			FileUtils.deleteFile(localPath);
			final String pathActual = params[0];
			final String passActual = params[1];
			final String userActual = params[2];
			final String hostActual = params[3];
			final String portActual = params[4];

//			String REMOTE_URL = "ssh://" + userActual + ":" + passActual + "@" + hostActual + ":" + portActual + pathActual;
            String REMOTE_URL = hostActual+"/"+pathActual;
			Git git;
            System.setProperty("user.home", context.getFilesDir().getAbsolutePath() );
           Log.v("git","user home : "+System.getProperty("user.home"));
            Log.v("git","after");

			try {
				git = Git.cloneRepository()
						.setURI(REMOTE_URL)
						.setDirectory(localPath)
//						.setCredentialsProvider(allowHosts)
                        .setCredentialsProvider(new CredentialsProviderAllowHost(userActual, passActual))
						.setBare(false)
						.call();
			} catch (Exception e) {
                e.printStackTrace();
				return e;
			}

			try {
				File file = new File(context.getFilesDir()+"/"+GIT_DIR+"/testPushIsWorking");
				file.createNewFile();
				Log.v("git"," here");
				git.add().addFilepattern("testPushIsWorking").call();
				git.commit().setMessage("adding file for testing").call();
				Iterable<PushResult> res = git.push().call();

				for(PushResult r: res){
					if(!r.getMessages().equals("")){
						return new UnableToPushException();
					}
				}
			} catch (GitAPIException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
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
            File f = new File(syncher.getFilesDir());
            File file[] = f.listFiles();
            Log.d("Files", "Size: "+ file.length);
            for (int i=0; i < file.length; i++)
            {
                String filename = file[i].getName();
                if(filename.equals(".git")) continue;
                OrgFile orgFile = new OrgFile(filename, filename,"");
                FileReader fileReader = null;
                try {
                    fileReader = new FileReader(syncher.getFilesDir() + filename);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                OrgDatabase db = OrgDatabase.getInstance(context);

                final OrgFileParser parser = new OrgFileParser(db, context.getContentResolver());

                OrgFileParser.parseFile(orgFile, bufferedReader, parser, context);
                Log.d("Files", "FileName:" + file[i].getName());
            }
        }

    }


    public static void push(Context context){
        Log.v("git","pushing");

        File repoDir = new File(context.getFilesDir()+"/"+ GIT_DIR+"/.git");
        Git git = null;
        try {
            git = Git.open(repoDir);
            // Stage all changed files, omitting new files, and commit with one command
            git.commit()
                    .setAll(true)
                    .setMessage("Commit changes to all files")
                    .call();

            git.push()
                    .setCredentialsProvider(new CredentialsProviderAllowHost("wizmer", ""))
                    .call();
            System.out.println("Committed all changes to repository at ");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnmergedPathsException e) {
            e.printStackTrace();
        } catch (WrongRepositoryStateException e) {
            e.printStackTrace();
        } catch (ConcurrentRefUpdateException e) {
            e.printStackTrace();
        } catch (NoHeadException e) {
            e.printStackTrace();
        } catch (NoMessageException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }


    }

    public static HashSet<String> pull(Context context){
        File repoDir = new File(context.getFilesDir()+"/"+ GIT_DIR+"/.git");
        Log.v("git","pulling");
        HashSet<String> result = new HashSet<>();
        try {
            Git git = Git.open(repoDir);
            Repository repository = git.getRepository();

            ObjectId oldHead = repository.resolve("HEAD^{tree}");

            new Git(repository)
                    .pull()
                    .setCredentialsProvider(new CredentialsProviderAllowHost("wizmer", ""))
                    .call();
            ObjectId head = repository.resolve("HEAD^{tree}");

            ObjectReader reader = repository.newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, oldHead);
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, head);
            List<DiffEntry> diffs= git.diff()
                    .setNewTree(newTreeIter)
                    .setOldTree(oldTreeIter)
                    .call();


            for(DiffEntry entry: diffs){
                result.add(entry.getNewPath());
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (DetachedHeadException e) {
            e.printStackTrace();
        } catch (NoHeadException e) {
            e.printStackTrace();
        } catch (TransportException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        } catch (InvalidRemoteException e) {
            e.printStackTrace();
        } catch (CanceledException e) {
            e.printStackTrace();
        } catch (WrongRepositoryStateException e) {
            e.printStackTrace();
        } catch (RefNotFoundException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        return result;
    }

    static class UnableToPushException extends Exception {

    }
}
