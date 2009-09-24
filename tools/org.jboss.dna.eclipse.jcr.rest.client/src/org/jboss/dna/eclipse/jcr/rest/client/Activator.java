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

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.log4j.BasicConfigurator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.web.jcr.rest.client.ServerManager;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;
import org.jboss.dna.web.jcr.rest.client.domain.IDnaObject;
import org.jboss.dna.web.jcr.rest.client.domain.Repository;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;
import org.osgi.framework.BundleContext;

/**
 * The <code>Activator</code> controls the plug-in life cycle, provides utility functions, and keeps track of the
 * {@link ServerManager}.
 */
public final class Activator extends AbstractUIPlugin implements IUiConstants {

    // ===========================================================================================================================
    // Class Fields
    // ===========================================================================================================================

    /**
     * The shared plugin instance.
     */
    private static Activator plugin;

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    /**
     * @return the shared instance or <code>null</code> if not constructed
     */
    public static Activator getDefault() {
        return plugin;
    }

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The image used when the requested image cannot be found.
     */
    private Image missingImage;

    /**
     * The manager in charge of the server registry.
     */
    private ServerManager serverMgr;

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * @param key the plugin relative path to the image (never <code>null</code>)
     * @return the image or an image indicating the requested image could not be found
     */
    private ImageDescriptor createImageDescriptor( String key ) {
        CheckArg.isNotNull(key, "key"); //$NON-NLS-1$

        try {
            URL url = new URL(getBundle().getEntry("/").toString() + key); //$NON-NLS-1$
            return ImageDescriptor.createFromURL(url);
        } catch (final MalformedURLException e) {
            log(new Status(Severity.ERROR, RestClientI18n.missingImage.text(key), e));
            return null;
        }
    }

    /**
     * @return the image to use when a requested image cannot be found
     */
    private Image getMissingImage() {
        if (this.missingImage == null) {
            this.missingImage = ImageDescriptor.getMissingImageDescriptor().createImage();
        }

        return this.missingImage;
    }

    /**
     * @param imageId the shared image identifier (never <code>null</code>)
     * @return the image or an image indicating the requested image could not be found
     * @see ISharedImages
     */
    public Image getSharedImage( String imageId ) {
        CheckArg.isNotNull(imageId, "imageId"); //$NON-NLS-1$
        Image result = PlatformUI.getWorkbench().getSharedImages().getImage(imageId);
        return ((result == null) ? getMissingImage() : result);
    }

    /**
     * @param imageId the shared image identifier (never <code>null</code>)
     * @return the image descriptor or a descriptor indicating the requested descriptor could not be found
     * @see ISharedImages
     */
    public ImageDescriptor getSharedImageDescriptor( String imageId ) {
        CheckArg.isNotNull(imageId, "imageId"); //$NON-NLS-1$
        ImageDescriptor result = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(imageId);

        if (result != null) {
            return result;
        }

        return ImageDescriptor.getMissingImageDescriptor();
    }

    /**
     * @param object the object whose image is being requested (domain object or plugin-relative path)
     * @return the image or <code>null</code> if not found
     */
    public Image getImage( Object object ) {
        String key = null;

        if (object instanceof Workspace) {
            key = WORKSPACE_IMAGE;
        } else if (object instanceof Repository) {
            key = REPOSITORY_IMAGE;
        } else if (object instanceof Server) {
            key = SERVER_IMAGE;
        } else if (object instanceof IDnaObject) {
            // should have an icon for every business object
            assert false;
        } else if (object instanceof String) {
            key = (String)object;
        }

        if (key != null) {
            ImageRegistry registry = getImageRegistry();
            Image image = registry.get(key);

            if (image == null) {
                ImageDescriptor descriptor = createImageDescriptor(key);

                if (descriptor == null) {
                    return getMissingImage();
                }

                image = descriptor.createImage();
                registry.put(key, image);
            }

            return image;
        }

        return null;
    }

    /**
     * @param object the object whose image descriptor is being requested
     * @return the image descriptor or <code>null</code> if not found
     */
    public ImageDescriptor getImageDescriptor( Object object ) {
        String key = null;

        if (object instanceof Workspace) {
            key = WORKSPACE_IMAGE;
        } else if (object instanceof Repository) {
            key = REPOSITORY_IMAGE;
        } else if (object instanceof Server) {
            key = SERVER_IMAGE;
        } else if (object instanceof IDnaObject) {
            // should have an icon for every business object
            assert false;
        } else if (object instanceof String) {
            key = (String)object;
        }

        if (key != null) {
            ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ((String)object));
            return ((descriptor == null) ? ImageDescriptor.getMissingImageDescriptor() : descriptor);
        }

        return null;
    }

    /**
     * @return the server manager or <code>null</code> if activator has not been initialized or started
     * @see #start(BundleContext)
     */
    public ServerManager getServerManager() {
        return this.serverMgr;
    }

    /**
     * @param status the status being logged (never <code>null</code>)
     */
    public void log( Status status ) {
        CheckArg.isNotNull(status, "status"); //$NON-NLS-1$
        IStatus eclipseStatus = new org.eclipse.core.runtime.Status(Utils.convertSeverity(status.getSeverity()),
                                                                    IUiConstants.PLUGIN_ID, status.getMessage(),
                                                                    status.getException());
        getLog().log(eclipseStatus);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start( BundleContext context ) throws Exception {
        BasicConfigurator.configure();
        
        super.start(context);
        plugin = this;

        this.serverMgr = new ServerManager(getStateLocation().toFile().getAbsolutePath());
        Status status = this.serverMgr.restoreState();

        // problem restoring server registry
        if (!status.isOk()) {
            log(status);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop( BundleContext context ) throws Exception {
        if (missingImage != null) {
            missingImage.dispose();
        }

        if (this.serverMgr != null) {
            Status status = this.serverMgr.saveState();

            if (!status.isOk()) {
                log(status);
            }

            this.serverMgr = null;
        }

        super.stop(context);

        plugin = null;
    }

}
