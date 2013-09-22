/*
 * Copyright 2012 two forty four a.m. LLC <http://www.twofortyfouram.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.twofortyfouram.locale;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

/**
 * A helper for dynamically loading resources from the Locale Developer Platform host.
 * <p>
 * Localized strings and resources will be returned if they are available within the host.
 */
/* package */final class SharedResources
{
    /**
     * {@code String} name of the resource for the primary message in the {@link MarketActivity}.
     */
    /*
     * This is NOT part of the public Locale Developer Platform API
     */
    /* package */static final String STRING_PLUGIN_MESSAGE_PRIMARY = "plugin_dialog_message"; //$NON-NLS-1$

    /**
     * {@code String} name of the resource for the informative message in the {@link MarketActivity}.
     */
    /*
     * This is NOT part of the public Locale Developer Platform API
     */
    /* package */static final String STRING_PLUGIN_INFORMATIVE_SETTING = "plugin_dialog_informative_setting"; //$NON-NLS-1$

    /**
     * {@code String} name of the resource for the informative message in the {@link MarketActivity}.
     */
    /*
     * This is NOT part of the public Locale Developer Platform API
     */
    /* package */static final String STRING_PLUGIN_INFORMATIVE_CONDITION = "plugin_dialog_informative_condition"; //$NON-NLS-1$

    /**
     * {@code String} name of the resource for the informative message in the {@link MarketActivity}.
     */
    /*
     * This is NOT part of the public Locale Developer Platform API
     */
    /* package */static final String STRING_PLUGIN_INFORMATIVE_CONDITION_AND_SETTING = "plugin_dialog_informative_condition_and_setting"; //$NON-NLS-1$

    /**
     * {@code String} name of the resource the open button in the {@link MarketActivity}.
     */
    /*
     * This is NOT part of the public Locale Developer Platform API
     */
    /* package */static final String STRING_PLUGIN_OPEN = "plugin_open"; //$NON-NLS-1$

    /**
     * Loads a string resource from the Locale Developer Platform host.
     * <p>
     * Note: This method may be slow.
     *
     * @param packageManager an instance of {@code PackageManager}. Cannot be null.
     * @param callingPackageHint hint as to which package is the calling package, from which resources might be preferred. May be
     *            null.
     * @param resourceName the {@code String} name of the resource to load. This must be one of the strings defined as a static
     *            constant in this class. Cannot be null or empty.
     * @return the resource requested. May return null if the requested resource isn't available.
     * @throws IllegalArgumentException if {@code packageManager} is null.
     * @throws IllegalArgumentException if {@code resourceName} is null or empty.
     */
    /* package */static CharSequence getTextResource(final PackageManager packageManager, final String callingPackageHint, final String resourceName)
    {
        if (null == packageManager)
        {
            throw new IllegalArgumentException("packageManager cannot be null"); //$NON-NLS-1$
        }

        if (TextUtils.isEmpty(resourceName))
        {
            throw new IllegalArgumentException("resourceName cannot be null or empty"); //$NON-NLS-1$
        }

        final String compatiblePackage = PackageUtilities.getCompatiblePackage(packageManager, callingPackageHint);

        if (null != compatiblePackage)
        {
            try
            {
                final Resources hostResources = packageManager.getResourcesForApplication(compatiblePackage);

                return hostResources.getText(hostResources.getIdentifier(resourceName, "string", compatiblePackage)); //$NON-NLS-1$
            }
            catch (final Exception e)
            {
                /*
                 * In an ideal world, this error will never happen. This catch is necessary, however, due to a TOCTOU error where
                 * the compatible package could be uninstalled at any time.
                 */

                Log.w(Constants.LOG_TAG, "TOCTOU error occurred", e); //$NON-NLS-1$

                return null;
            }
        }

        return null;
    }
}