package com.matburt.mobileorg.Synchronizers;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Parsing.MobileOrgApplication;
import com.matburt.mobileorg.Parsing.OrgFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

public class UbuntuOneSynchronizer extends Synchronizer {

	private String remoteIndexPath;
	private String remotePath;
    private String username;
    private String password;

    private static final String authUrl
        = "https://login.ubuntu.com/api/1.0/authentications?ws.op=authenticate&token_name=Ubuntu%20One%20@%20";
    private static final String authUrlSubmit
        = "https://one.ubuntu.com/";
	
    public UbuntuOneSynchronizer(Context parentContext, MobileOrgApplication appInst) {
        super(parentContext, appInst);

		this.remoteIndexPath = sharedPreferences.getString("webUrl", "");
		this.remotePath = getRootUrl();

		this.username = sharedPreferences.getString("webUser", "");
		this.password = sharedPreferences.getString("webPass", "");
    }

}
		private static final String URL_BASE 			= "https://one.ubuntu.com/";
		private static final String URL_BASE_FILES 		= "https://files.one.ubuntu.com";
		
		private static final String URL_AUTHENTICATE 		
			= "https://login.ubuntu.com/api/1.0/authentications?ws.op=authenticate&token_name=Ubuntu%20One%20@%20";
		private static final String URL_AUTHENTICATE_TELL 	
			= URL_BASE + "oauth/sso-finished-so-get-tokens/" + MY_EMAIL;
		
		private static final String URL_FILE_INFO = URL_BASE + "api/file_storage/v1";

	/*
	 *  Some global variables to hold the tokens and keys for our OAuth authentication.	
	 */
	    private static String consumer_secret;
	    private static String consumer_key;
	    private static String token;
	    private static String token_secret;
	    
	/*
	 *  A variable to hold a JSON object containing basic information about the User and 
	 *  their synced folders.     
	 */
	    private static JSONObject user_info;
	
	/**
	 *  Main function.
	 */
	public static void main(String[] argv) throws Exception {
		
		/*
		 *  Set up the passing of our credentials to HTTP Authentication calls.
		 */
			Authenticator.setDefault(
				new Authenticator(){
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(MY_EMAIL, MY_PASSWORD.toCharArray()); 
					}
				}
			);
		
		/*
		 *  We authenticate using Ubuntu Single Sign On (SSO) via a pre-defined URL, 
		 *  with the name of our current machine appended to it. This returns JSON 
		 *  with the keys/secrets that we'll need to authenticate requests to the 
		 *  API with.
		 */
			String machine_name = URLEncoder.encode( InetAddress.getLocalHost().getHostName(), "UTF-8" );
			String oauth_information = ""; 
			try{
				oauth_information = getResponse(URL_AUTHENTICATE + machine_name, false);
			} catch( Exception e ) {
				System.out.println( "Authentication Failed with error: " + e.toString());
				System.out.println( "Did you set the Email & Password variables correctly?");
				return;
			}
			
			JSONObject o = JSONObject.fromObject(oauth_information);
	        	consumer_secret	= o.getString("consumer_secret");
	        	consumer_key	= o.getString("consumer_key");
	        	token			= o.getString("token");
	        	token_secret	= o.getString("token_secret");
        
	    /*
	     *  The next step is to tell Ubuntu One about our newly-created tokens, before
	     *  it will let us use them.     	
	     */
	        getResponse(URL_AUTHENTICATE_TELL, false);
        
	    /*
	     *  Now that we're authenticated, we make our first request to the API to get some
	     *  basic information about the User and their synched folders.  
	     * 
	     *  This Object will be in the following format:
	     * 
	     * 	{
	     * 		"resource_path" 	: "",
	     *		"user_id" 			: 9999,
	     *		"visible_name" 		: "Fred Flintstone",
	     *		"max_bytes" 		: 123456789,
	     *		"used_bytes" 		: 0,
	     *		"root_node_path" 	: "/~/Ubuntu One", (When including user folders)
	     *		"user_node_paths" 	: [<list of volume folder URIs>,]
	     *	}
	     *
	     */
	        user_info = JSONObject.fromObject( getResponse(URL_FILE_INFO, false) );
        
	     /*
	      *  Get the path of the Ubuntu One directory and fetch a JSON Object representing it.
	      * 
	      *  This Object will be in the following format:
	      * 
	      *  {
	      *		"resource_path" 		: The path to this resource,
	      * 	"kind" 					: "file|directory", (ignored on change)
	      * 	"path" 					: the path of the node relative to the volume, (ignored on creation)
	      * 	"is_public" 			: true|false, (ignored on creation; files only)
	      * 	"parent_path" 			: The path to the parent directory resource,
	      * 	"volume_path" 			: The path to the volume resource,
	      * 	"key" 					: nodekey,
	      * 	"when_created" 			: when the node was created,
	      * 	"when_changed" 			: node.when_last_modified,
	      * 	"generation" 			: generation,
	      * 	"generation_created" 	: generation_created,
	      * 	"content_path" 			: path for GET & PUT content,
	      * 	"has_children" 			: True|False,
	      * 	"is_root" 				: True|False,
	      * 	"children" 				: [Node Representations of child nodes]
	      *  }    
	      */
	        String root_node_path = user_info.getString("root_node_path").replaceAll(" ", "%20");
	        String ubuntu_one_dir = URL_FILE_INFO + root_node_path + "?include_children=true"; 
	        
	        JSONObject rootInfo = JSONObject.fromObject(getResponse(ubuntu_one_dir, false));

	     /*
	      *  If we've chosen to download files from Ubuntu One, check that the directory we're 
	      *  downloading to exists.   
	      */
	        if( DO_DOWNLOAD ) { new File(LOCAL_PATH).mkdir(); }
	        
	     /*
	      *  Pass the root node to a recursive function to print the directory tree.  
	      */
	        downloadAndOrPrintTree( rootInfo, 0 );
	       
	}
	
	/**
	 *  Recursive function that recreates the Ubuntu One filesystem locally 
	 *  and/or prints the tree to the console.
	 */
	private static void downloadAndOrPrintTree( JSONObject j, int level ) throws MalformedURLException, OAuthException, IOException {
		
		/*
		 *  If printing to the console is enabled, we print the name of this file/directory,
		 *  prepending it with a number of spaces depending on its depth within the tree.
		 */
		if( DO_PRINT ) {
			for( int x = 0; x < level; x++ ) { System.out.print("    "); }
			System.out.print( j.getString("path") + '\n');
		}
		
		/*
		 *  If downloading is enabled, we either try to create a new local folder or download
		 *  some data, depending on whether the current node type is a Directory or File.
		 */
		if( DO_DOWNLOAD ) {
			if( isFile(j) ) {
				FileOutputStream fstream = new FileOutputStream(new File(LOCAL_PATH + j.getString("path")));
					getFile(URL_BASE_FILES + j.getString("content_path").replaceAll(" ", "%20"), fstream);
				fstream.close();
			} else {
				new File(LOCAL_PATH + j.getString("path")).mkdir();
			}
		}
		
		/*
		 *  If it's a directory and has child nodes then recurse down.
		 */
		if( ! isFile(j) && hasChildren(j) ) {
			
			JSONArray children = j.getJSONArray("children");
			
			for( int x = 0; x < children.size(); x++ ) {
	    		
				JSONObject node = children.getJSONObject(x);
	    		
				if( isFile(children.getJSONObject(x)) ) {
					downloadAndOrPrintTree(node, level + 1);
				} else {
					String obj = getResponse( URL_FILE_INFO + node.getString("resource_path").replaceAll(" ", "%20") + "?include_children=true", true);
					downloadAndOrPrintTree( JSONObject.fromObject(obj), level + 1 );
				}
	    		
			}
	    	
		}
		
	}
	
	/**
	 *  Returns whether or not the given JSON Object represents a Ubuntu One File.
	 */
	private static boolean isFile( JSONObject j ){
		return "file".equals(j.getString("kind"));
	}
	
	/**
	 *  Returns whether or not the given JSON Object represents a Ubuntu One 
	 *  directory that has child nodes.
	 */
	private static boolean hasChildren( JSONObject j ) {
		return "true".equals( j.getString("has_children") );
	}
	
	/**
	 * Returns the response of a request to the given URL, signed 
	 * with OAuth tokens (if available).
	 */
	private static String getResponse( String url, boolean binary ) throws MalformedURLException, IOException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
		
		HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
      	
		/*
		 * If we have the appropriate keys/secrets then we sign this as an OAuth request.
		 * If not (i.e we haven't authenticated and received them yet) then we just send it
		 * as a regular request.
		 */
		if( consumer_key != null ) {
			OAuthConsumer consumer = new DefaultOAuthConsumer(consumer_key, consumer_secret);
			consumer.setTokenWithSecret(token, token_secret);
			consumer.sign(conn);
		}
		
		conn.setUseCaches(false);
		conn.connect();

		BufferedReader rd  	= new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder sb 	= new StringBuilder();

		String line = "";
		while( (line = rd.readLine()) != null ) {
			sb.append(line);
			if( ! binary ){ sb.append('\n'); }
		}

		conn.disconnect();  
	        
		return sb.toString();
	
	}
	
	/**
	 * Reads a file from the given URL and writes the data to the passed output stream.
	 */
	public static void getFile( String url, FileOutputStream fstream ) throws MalformedURLException, IOException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
		
		HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
      	
		/*
		 * If we have the appropriate keys/secrets then we sign this as an OAuth request.
		 * If not (i.e we haven't authenticated and received them yet) then we just send it
		 * as a regular request.
		 */
		if( consumer_key != null ) {
			OAuthConsumer consumer = new DefaultOAuthConsumer(consumer_key, consumer_secret);
			consumer.setTokenWithSecret(token, token_secret);
			consumer.sign(conn);
		}
		
		conn.setUseCaches(false);
		conn.setConnectTimeout(0);
		conn.connect();

		InputStream in = conn.getInputStream();
		byte[] buffer = new byte[1024];
		int len1 = 0;
		while ( (len1 = in.read(buffer)) != -1 ) {
			fstream.write(buffer, 0, len1);
		}

		conn.disconnect();  

	}
		
}