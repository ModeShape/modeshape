/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.sequencer.esbMessage;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.monitor.ProgressMonitor;
import org.modeshape.common.util.StringUtil;
import org.jboss.internal.soa.esb.util.Encoding;
import org.jboss.soa.esb.addressing.EPR;
import org.jboss.soa.esb.message.Message;
import org.jboss.soa.esb.util.Util;

/**
 * Utility for extracting metadata from MP3 files.
 * 
 * @author Stefano Maestri
 */
public class ESBMessageMetaData {

    private URI messageType;
    private EPR to = null;
    private EPR from = null;
    private EPR faultTo = null;
    private EPR replyTo = null;
    private URI relatesTo = null;
    private URI action = null;
    private URI messageID = null;
    private Map<String, Object> bodyMap = null;
    private Map<String, Object> propertiesMap = null;
    private Map<String, Object> attachmentsMap = null;
    private URI faultCode = null;
    private String faultReason;
    private Throwable faultCause;

    private ESBMessageMetaData() {

    }

    public static ESBMessageMetaData instance( InputStream stream,
                                               ProgressMonitor progressMonitor ) {

        ESBMessageMetaData me = null;
        Message message = null;
        progressMonitor.beginTask(10, ESBMessageSequencerI18n.sequencerTaskName);
        try {
            me = new ESBMessageMetaData();
            message = Util.deserialize((Serializable)Encoding.decodeToObject(StringUtil.read(stream)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        progressMonitor.worked(2);
        if (progressMonitor.isCancelled()) return null;

        me.messageType = message.getType();
        // Context not yet implemented in JBossESB
        // message.getContext();

        me.to = message.getHeader().getCall().getTo();
        me.from = message.getHeader().getCall().getFrom();
        me.faultTo = message.getHeader().getCall().getFaultTo();
        me.replyTo = message.getHeader().getCall().getReplyTo();
        me.relatesTo = message.getHeader().getCall().getRelatesTo();
        me.action = message.getHeader().getCall().getAction();
        me.messageID = message.getHeader().getCall().getMessageID();
        progressMonitor.worked(1);
        if (progressMonitor.isCancelled()) return null;

        if (message.getBody().getNames().length != 0) {
            me.bodyMap = new HashMap<String, Object>();
            ProgressMonitor subtask = progressMonitor.createSubtask(2);
            subtask.beginTask(message.getBody().getNames().length, ESBMessageSequencerI18n.sequencerTaskName);
            for (String name : message.getBody().getNames()) {
                me.bodyMap.put(name, message.getBody().get(name));
                subtask.worked(1);
                if (progressMonitor.isCancelled()) return null;
            }
            subtask.done();
        }

        if (message.getProperties().getNames().length != 0) {
            me.propertiesMap = new HashMap<String, Object>();
            ProgressMonitor subtask = progressMonitor.createSubtask(2);
            subtask.beginTask(message.getProperties().getNames().length, ESBMessageSequencerI18n.sequencerTaskName);
            for (String name : message.getProperties().getNames()) {
                me.propertiesMap.put(name, message.getProperties().getProperty(name));
                subtask.worked(1);
                if (progressMonitor.isCancelled()) return null;
            }
            subtask.done();
        }

        if (message.getAttachment().getNames().length != 0) {
            me.attachmentsMap = new HashMap<String, Object>();
            ProgressMonitor subtask = progressMonitor.createSubtask(2);
            subtask.beginTask(message.getAttachment().getNames().length, ESBMessageSequencerI18n.sequencerTaskName);
            for (String name : message.getAttachment().getNames()) {
                me.attachmentsMap.put(name, message.getAttachment().get(name));
                subtask.worked(1);
                if (progressMonitor.isCancelled()) return null;
            }
            subtask.done();
        }

        me.faultCause = message.getFault().getCause();
        me.faultCode = message.getFault().getCode();
        me.faultReason = message.getFault().getReason();
        // do last unit of work implicit in done
        progressMonitor.done();
        return me;

    }

    public URI getMessageType() {
        return messageType;
    }

    public EPR getTo() {
        return to;
    }

    public EPR getFrom() {
        return from;
    }

    public EPR getFaultTo() {
        return faultTo;
    }

    public EPR getReplyTo() {
        return replyTo;
    }

    public URI getRelatesTo() {
        return relatesTo;
    }

    public URI getAction() {
        return action;
    }

    public URI getMessageID() {
        return messageID;
    }

    public Map<String, Object> getBodyMap() {
        return bodyMap;
    }

    public Map<String, Object> getPropertiesMap() {
        return propertiesMap;
    }

    public Map<String, Object> getAttachmentsMap() {
        return attachmentsMap;
    }

    public URI getFaultCode() {
        return faultCode;
    }

    public String getFaultReason() {
        return faultReason;
    }

    public Throwable getFaultCause() {
        return faultCause;
    }
}
