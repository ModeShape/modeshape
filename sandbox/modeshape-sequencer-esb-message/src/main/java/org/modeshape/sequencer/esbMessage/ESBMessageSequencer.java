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
import java.util.Map.Entry;
import org.modeshape.common.monitor.ProgressMonitor;
import org.modeshape.spi.graph.NameFactory;
import org.modeshape.spi.graph.Path;
import org.modeshape.spi.graph.PathFactory;
import org.modeshape.spi.sequencers.SequencerOutput;
import org.modeshape.spi.sequencers.StreamSequencer;

/**
 * A sequencer that processes the binary content of an ESB Message, extracts the metadata for the message, and then writes this
 * metadata to the repository.
 * <p>
 * This sequencer produces data that corresponds to the following structure:
 * <ul>
 * <li><strong>esbMessage:metadata</strong> node of type <code>esbMessage:metadata</code>
 * <ul>
 * <li><strong>esbMessage:to</strong> - optional property for the destination EPR of the message</li>
 * <li><strong>esbMessage:from</strong> - optional property for the source EPR of the message</li>
 * <li><strong>esbMessage:replyTo</strong> - optional property for the reply destination EPR</li>
 * <li><strong>esbMessage:relatesTo</strong> - optional property for the relatesTo URI</li>
 * <li><strong>esbMessage:action</strong> - optional property specifying the action URI</li>
 * <li><strong>esbMessage:messageID</strong> - optional property specifying the messageID URI</li>
 * <li><strong>esbMessage:body</strong> - optional node specifying the message body. It contains a set of properties
 * representing Object contained in body. Properties names reflect object names used in message body</li>
 * <li><strong>esbMessage:properties</strong> - optional node specifying the message properties. It contains a set of properties
 * representing Object contained in message properties. Properties names reflect object names used in message properties</li>
 * <li><strong>esbMessage:attachments</strong> - optional node specifying the message attachments. It contains a set of
 * properties representing Object contained in message attachments. Properties names reflect object names used in message
 * attachments</li>
 * <li><strong>esbMessage:faultCode</strong> - optional property specifying the message fault code. As defined in ESB message
 * it's an URI</li>
 * <li><strong>esbMessage:faultReason</strong> - optional property containing a String describing the fault in a human readable
 * form</li>
 * <li><strong>esbMessage:faultCause</strong> - optional property specifying the Throwable Cause of the fault.</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 * 
 * @author Stefano Maestri
 */
public class ESBMessageSequencer implements StreamSequencer {

    public static final String METADATA_NODE = "esbMessage:metadata";
    public static final String ESB_MESSAGE_PRIMARY_TYPE = "jcr:primaryType";
    public static final String ESB_MESSAGE_TO = "esbMessage:to";
    public static final String ESB_MESSAGE_FROM = "esbMessage:from";
    public static final String ESB_MESSAGE_REPLY_TO = "esbMessage:replyTo";
    public static final String ESB_MESSAGE_RELATES_TO = "esbMessage:relatesTo";
    public static final String ESB_MESSAGE_ACTION = "esbMessage:action";
    public static final String ESB_MESSAGE_MESSAGE_ID = "esbMessage:messageID";
    public static final String ESB_MESSAGE_MESSAGE_TYPE = "esbMessage:messageType";
    public static final String ESB_MESSAGE_BODY = "esbMessage:metadata/esbMessage:body";
    public static final String ESB_MESSAGE_PROPERTIES = "esbMessage:metadata/esbMessage:properties";
    public static final String ESB_MESSAGE_ATTACHMENTS = "esbMessage:metadata/esbMessage:attachments";
    public static final String ESB_MESSAGE_FAULT_CODE = "esbMessage:faultCode";
    public static final String ESB_MESSAGE_FAULT_REASON = "esbMessage:faultReason";
    public static final String ESB_MESSAGE_FAULT_CAUSE = "esbMessage:faultCause";

    public static final String ESB_MESSAGE_NAMESPACE = "esbMessage";

    public void sequence( InputStream stream,
                          SequencerOutput output,
                          ProgressMonitor progressMonitor ) {

        progressMonitor.beginTask(20, ESBMessageSequencerI18n.sequencerTaskName);
        ESBMessageMetaData metadata = ESBMessageMetaData.instance(stream, progressMonitor.createSubtask(10));
        if (progressMonitor.isCancelled()) return;

        if (metadata != null) {
            NameFactory nameFactory = output.getFactories().getNameFactory();
            PathFactory pathFactory = output.getFactories().getPathFactory();
            Path metadataNode = pathFactory.create(METADATA_NODE);

            output.setProperty(metadataNode, nameFactory.create(ESB_MESSAGE_PRIMARY_TYPE), "esbMessage:metadata");
            output.setProperty(metadataNode, nameFactory.create(ESB_MESSAGE_TO), metadata.getTo());
            output.setProperty(metadataNode, nameFactory.create(ESB_MESSAGE_FROM), metadata.getFrom());
            output.setProperty(metadataNode, nameFactory.create(ESB_MESSAGE_REPLY_TO), metadata.getReplyTo());
            output.setProperty(metadataNode, nameFactory.create(ESB_MESSAGE_RELATES_TO), metadata.getRelatesTo());
            output.setProperty(metadataNode, nameFactory.create(ESB_MESSAGE_ACTION), metadata.getAction());
            output.setProperty(metadataNode, nameFactory.create(ESB_MESSAGE_MESSAGE_ID), metadata.getMessageID());
            output.setProperty(metadataNode, nameFactory.create(ESB_MESSAGE_MESSAGE_TYPE), metadata.getMessageType());
            output.setProperty(metadataNode, nameFactory.create(ESB_MESSAGE_FAULT_CODE), metadata.getFaultCode());
            output.setProperty(metadataNode, nameFactory.create(ESB_MESSAGE_FAULT_REASON), metadata.getFaultReason());
            output.setProperty(metadataNode, nameFactory.create(ESB_MESSAGE_FAULT_CAUSE), metadata.getFaultCause());
            progressMonitor.worked(1);
            if (progressMonitor.isCancelled()) return;

            ProgressMonitor subtask = progressMonitor.createSubtask(3);
            subtask.beginTask(metadata.getBodyMap().entrySet().size(), ESBMessageSequencerI18n.sequencerTaskName);
            Path bodyNode = pathFactory.create(metadataNode, nameFactory.create(ESB_MESSAGE_BODY));
            for (Entry<String, Object> entry : metadata.getBodyMap().entrySet()) {
                output.setProperty(bodyNode, nameFactory.create(ESB_MESSAGE_NAMESPACE, entry.getKey()), entry.getValue());
                subtask.worked(1);
                if (progressMonitor.isCancelled()) return;
            }
            subtask.done();

            subtask = progressMonitor.createSubtask(3);
            subtask.beginTask(metadata.getPropertiesMap().entrySet().size(), ESBMessageSequencerI18n.sequencerTaskName);
            Path propertiesNode = pathFactory.create(metadataNode, nameFactory.create(ESB_MESSAGE_PROPERTIES));
            for (Entry<String, Object> entry : metadata.getPropertiesMap().entrySet()) {
                output.setProperty(propertiesNode, nameFactory.create(ESB_MESSAGE_NAMESPACE, entry.getKey()), entry.getValue());
                subtask.worked(1);
                if (progressMonitor.isCancelled()) return;
            }
            subtask.done();

            subtask = progressMonitor.createSubtask(3);
            subtask.beginTask(metadata.getAttachmentsMap().entrySet().size(), ESBMessageSequencerI18n.sequencerTaskName);
            Path attachmentsNode = pathFactory.create(metadataNode, nameFactory.create(ESB_MESSAGE_ATTACHMENTS));
            for (Entry<String, Object> entry : metadata.getAttachmentsMap().entrySet()) {
                output.setProperty(attachmentsNode, nameFactory.create(ESB_MESSAGE_NAMESPACE, entry.getKey()), entry.getValue());
                subtask.worked(1);
                if (progressMonitor.isCancelled()) return;
            }
            subtask.done();

            progressMonitor.done();

        }
    }

}
