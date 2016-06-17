package com.matburt.mobileorg2.Synchronizers;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

import java.util.Arrays;

public class CredentialsProviderAllowHost extends CredentialsProvider {
    private String username;

    private char[] password;

    /**
     * Initialize the provider with a single username and password.
     *
     * @param username
     * @param password
     */
    public CredentialsProviderAllowHost(String username, String password) {
        this(username, password.toCharArray());
    }

    /**
     * Initialize the provider with a single username and password.
     *
     * @param username
     * @param password
     */
    public CredentialsProviderAllowHost(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public boolean supports(CredentialItem... items) {
        for (CredentialItem i : items) {
            if (i instanceof CredentialItem.Username)
                continue;

            else if (i instanceof CredentialItem.Password)
                continue;
            else if ((i instanceof CredentialItem.YesNoType)) {
                return true;
            }
            else
                return false;
        }
        return true;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items)
            throws UnsupportedCredentialItem {
        for (CredentialItem i : items) {
            if (i instanceof CredentialItem.Username) {
                ((CredentialItem.Username) i).setValue(username);
                continue;
            }
            if (i instanceof CredentialItem.Password) {
                ((CredentialItem.Password) i).setValue(password);
                continue;
            }
            if (i instanceof CredentialItem.StringType) {
                if (i.getPromptText().equals("Password: ")) {
                    ((CredentialItem.StringType) i).setValue(new String(
                            password));
                    continue;
                }
            }
            if (i instanceof CredentialItem.YesNoType) {
                ((CredentialItem.YesNoType) i).setValue(true);
                continue;
            }
            throw new UnsupportedCredentialItem(uri, i.getClass().getName()
                    + ":" + i.getPromptText());
        }
        return true;
    }

    /** Destroy the saved username and password.. */
    public void clear() {
        username = null;

        if (password != null) {
            Arrays.fill(password, (char) 0);
            password = null;
        }
    }
}
