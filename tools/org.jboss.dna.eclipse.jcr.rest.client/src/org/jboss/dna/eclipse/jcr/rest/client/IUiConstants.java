/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.eclipse.jcr.rest.client;

public interface IUiConstants {

    /**
     * The Plug-in's identifier.
     */
    String PLUGIN_ID = "org.jboss.dna.eclipse.jcr.rest.client"; //$NON-NLS-1$

    String ICON_FOLDER = "icons/"; //$NON-NLS-1$

    //
    // /icons/objects/
    //

    String OBJECT_ICONS_FOLDER = ICON_FOLDER + "objects/"; //$NON-NLS-1$

    String CHECKMARK_IMAGE = OBJECT_ICONS_FOLDER + "checkmark.gif"; //$NON-NLS-1$

    String REPOSITORY_IMAGE = OBJECT_ICONS_FOLDER + "repository.gif"; //$NON-NLS-1$

    String SERVER_IMAGE = OBJECT_ICONS_FOLDER + "server.gif"; //$NON-NLS-1$

    String WORKSPACE_IMAGE = OBJECT_ICONS_FOLDER + "workspace.gif"; //$NON-NLS-1$

    //
    // /icons/views/
    //

    String VIEWS_ICON_FOLDER = ICON_FOLDER + "views/"; //$NON-NLS-1$

    String BLANK_IMAGE = VIEWS_ICON_FOLDER + "blank.gif"; //$NON-NLS-1$

    String COLLAPSE_ALL_IMAGE = VIEWS_ICON_FOLDER + "collapse_all.gif"; //$NON-NLS-1$

    String DELETE_SERVER_IMAGE = VIEWS_ICON_FOLDER + "delete_server.gif"; //$NON-NLS-1$

    String DNA_IMAGE_16x = VIEWS_ICON_FOLDER + "dna_icon_16x.png"; //$NON-NLS-1$

    String EDIT_SERVER_IMAGE = VIEWS_ICON_FOLDER + "edit_server.gif"; //$NON-NLS-1$

    String ERROR_OVERLAY_IMAGE = VIEWS_ICON_FOLDER + "error_overlay.gif"; //$NON-NLS-1$

    String NEW_SERVER_IMAGE = VIEWS_ICON_FOLDER + "new_server.gif"; //$NON-NLS-1$

    String PUBLISH_IMAGE = VIEWS_ICON_FOLDER + "publish.png"; //$NON-NLS-1$

    String PUBLISHED_OVERLAY_IMAGE = VIEWS_ICON_FOLDER + "published_overlay.png"; //$NON-NLS-1$

    String REFRESH_IMAGE = VIEWS_ICON_FOLDER + "refresh.gif"; //$NON-NLS-1$

    String UNPUBLISH_IMAGE = VIEWS_ICON_FOLDER + "unpublish.png"; //$NON-NLS-1$

    //
    // /icons/wizards/
    //

    String WIZARD_ICONS_FOLDER = ICON_FOLDER + "wizards/"; //$NON-NLS-1$

    String DNA_WIZARD_BANNER_IMAGE = WIZARD_ICONS_FOLDER + "dna_wizard_banner.gif"; //$NON-NLS-1$

    //
    // Help Contexts
    //

    String HELP_CONTEXT_PREFIX = PLUGIN_ID + '.';

    String DNA_CONSOLE_HELP_CONTEXT = HELP_CONTEXT_PREFIX + "dnaConsoleHelpContext"; //$NON-NLS-1$
    
    String PREFERENCE_PAGE_HELP_CONTEXT = HELP_CONTEXT_PREFIX + "preferencesHelpContext"; //$NON-NLS-1$

    String PUBLISH_DIALOG_HELP_CONTEXT = HELP_CONTEXT_PREFIX + "publishDialogHelpContext"; //$NON-NLS-1$

    String SERVER_DIALOG_HELP_CONTEXT = HELP_CONTEXT_PREFIX + "serverDialogHelpContext"; //$NON-NLS-1$

    String SERVER_VIEW_HELP_CONTEXT = HELP_CONTEXT_PREFIX + "serverViewHelpContext"; //$NON-NLS-1$

    //
    // Jobs
    //

    /**
     * The <code>Job</code> framework job family for the DNA publishing and unpublishing operations.
     */
    String DNA_PUBLISHING_JOB_FAMILY = "dna.publishing.job.family"; //$NON-NLS-1$

    //
    // Preferences
    //

    /**
     * A preference for a list of file extensions that will not be part of publishing operations.
     */
    String FILTERED_FILE_EXTENSIONS_PREFERENCE = "dna.preference.filteredFileExtensions"; //$NON-NLS-1$

    /**
     * A preference for a list of folder names whose contents will not be part of publishing operations.
     */
    String FILTERED_FOLDER_NAMES_PREFERENCE = "dna.preference.filteredFolderNames"; //$NON-NLS-1$

}
