/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.dna.common.monitor;

import java.util.Locale;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.Logger;

/**
 * @author Randall Hauch
 */
public class LoggingProgressMonitor extends ProgressMonitorWrapper {

    private final Logger logger;
    private final Logger.Level level;
    private final Locale locale;

    public LoggingProgressMonitor( ProgressMonitor delegate, Logger logger, Logger.Level level ) {
        this(delegate, logger, level, null);
    }

    public LoggingProgressMonitor( ProgressMonitor delegate, Logger logger, Logger.Level level, Locale locale ) {
        super(delegate);
        assert level != null;
        assert logger != null;
        this.level = level;
        this.logger = logger;
        this.locale = locale;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beginTask( double totalWork, I18n name, Object... params ) {
        super.beginTask(totalWork, name, params);
        this.logger.log(level, CommonI18n.progressMonitorBeginTask, getActivityName(), name.text(params));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void done() {
        super.done();
        this.logger.log(level, CommonI18n.progressMonitorStatus, super.getStatus(this.locale));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCancelled( boolean value ) {
        super.setCancelled(value);
        this.logger.log(level, CommonI18n.progressMonitorStatus, super.getStatus(this.locale));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void worked( double work ) {
        super.worked(work);
        this.logger.log(level, CommonI18n.progressMonitorStatus, super.getStatus(this.locale));
    }

}
