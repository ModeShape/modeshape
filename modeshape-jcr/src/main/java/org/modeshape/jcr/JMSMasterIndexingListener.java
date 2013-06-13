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

package org.modeshape.jcr;

import java.util.List;
import java.util.Properties;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.jms.JmsBackendQueueProcessor;
import org.hibernate.search.backend.impl.jms.JmsBackendQueueTask;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;

/**
 * JMS listener that is created & run on nodes which have {@code jms-master} backend configured for indexing. Its primary
 * responsibility is to read Hibernate Search indexing jobs from a configured queue and pass those jobs to the index manager.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
class JMSMasterIndexingListener implements MessageListener, ExceptionListener {
    private static final Logger LOGGER = Logger.getLogger(JMSMasterIndexingListener.class);

    private static final String CONNECTION_LOGIN = JmsBackendQueueProcessor.JMS_CONNECTION_LOGIN;
    private static final String CONNECTION_PASSWORD = JmsBackendQueueProcessor.JMS_CONNECTION_PASSWORD;

    private final Properties configuration;

    private QueueConnection queueConnection;
    private IndexManagerHolder allIndexManager;

    JMSMasterIndexingListener( Properties configuration ) {
        this.configuration = configuration;
    }

    void start( IndexManagerHolder allIndexManager ) {
        this.allIndexManager = allIndexManager;

        QueueConnectionFactory connectionFactory = getConnectionFactory();
        Queue queue = getQueue();
        try {
            this.queueConnection = establishConnection(connectionFactory);
            startSession(queue);
        } catch (JMSException e) {
            LOGGER.error(e.getLinkedException(), JcrI18n.errorWhileStartingUpListener, e.getErrorCode(), e.getMessage());
        }
        LOGGER.debug("Started JMS indexing listener on master node...");
    }

    private void startSession( Queue queue ) throws JMSException {
        QueueSession queueSession = queueConnection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
        QueueReceiver queueReceiver = queueSession.createReceiver(queue);
        queueReceiver.setMessageListener(this);
        queueConnection.setExceptionListener(this);
        queueConnection.start();
    }

    @Override
    public void onMessage( Message message ) {
        LOGGER.trace("Received JMS message");
        if (!(message instanceof ObjectMessage)) {
            LOGGER.error(JcrI18n.incorrectJMSMessageType, ObjectMessage.class.getName(), message.getClass().getName());
            return;
        }
        final ObjectMessage objectMessage = (ObjectMessage)message;

        try {
            String indexName = objectMessage.getStringProperty(JmsBackendQueueTask.INDEX_NAME_JMS_PROPERTY);
            IndexManager indexManager = allIndexManager.getIndexManager(indexName);
            if (indexManager == null) {
                LOGGER.warn(JcrI18n.unknownIndexName, indexName);
                return;
            }
            List<LuceneWork> workQueue = indexManager.getSerializer().toLuceneWorks((byte[])objectMessage.getObject());
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Received " + workQueue.size()
                             + " lucene work item(s) from JMS message. Submitting to the index manager");
            }
            indexManager.performOperations(workQueue, null);
        } catch (JMSException e) {
            LOGGER.error(e.getLinkedException(), JcrI18n.cannotReadJMSMessage, e.getErrorCode(), e.getMessage());
        }
    }

    @Override
    public void onException( JMSException exception ) {
        LOGGER.error(exception.getLinkedException(),
                     JcrI18n.unexpectedJMSException,
                     exception.getErrorCode(),
                     exception.getMessage());
    }

    void shutdown() {
        LOGGER.debug("Stopping JMS indexing listener on master node...");
        try {
            queueConnection.close();
            this.allIndexManager = null;
            this.queueConnection = null;
        } catch (JMSException e) {
            LOGGER.error(e.getLinkedException(), JcrI18n.errorWhileShuttingDownListener, e.getErrorCode(), e.getMessage());
        }
    }

    private QueueConnectionFactory getConnectionFactory() {
        String connectionFactoryJndiName = configuration.getProperty(RepositoryConfiguration.FieldName.INDEXING_BACKEND_JMS_CONNECTION_FACTORY_JNDI_NAME);
        try {
            // create the initial context so that additional (custom) jndi properties can be passed down
            InitialContext initialContext = new InitialContext(configuration);
            return (QueueConnectionFactory)initialContext.lookup(connectionFactoryJndiName);
        } catch (NamingException e) {
            throw new ConfigurationException(JcrI18n.cannotLocateConnectionFactory.text(connectionFactoryJndiName));
        }
    }

    private Queue getQueue() {
        String queueJndiName = configuration.getProperty(RepositoryConfiguration.FieldName.INDEXING_BACKEND_JMS_QUEUE_JNDI_NAME);

        try {
            // create the initial context so that additional (custom) jndi properties can be passed down
            InitialContext initialContext = new InitialContext(configuration);
            return (Queue)initialContext.lookup(queueJndiName);
        } catch (NamingException e) {
            throw new ConfigurationException(JcrI18n.cannotLocateConnectionFactory.text(queueJndiName));
        }
    }

    private QueueConnection establishConnection( QueueConnectionFactory factory ) throws JMSException {
        String login = configuration.getProperty(CONNECTION_LOGIN);
        String password = configuration.getProperty(CONNECTION_PASSWORD);

        if (StringUtil.isBlank(login) && StringUtil.isBlank(password)) {
            return factory.createQueueConnection();
        }
        return factory.createQueueConnection(login, password);
    }
}
