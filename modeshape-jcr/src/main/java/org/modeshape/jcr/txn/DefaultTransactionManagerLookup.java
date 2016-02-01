/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.txn;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.Reflection;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.api.txn.TransactionManagerLookup;

/**
 * ModeShape's default implementation of a {@link TransactionManagerLookup}. This is used by default when no other explicit
 * lookup instance is present and looks for a transaction manager in the following order:
 * <pre>
 *     <ol>
 *         <li>JNDI: it searches JNDI for a number of known bindings of transaction managers, specific most JEE containers</li>
 *         <li>Standalone JBoss JTA: it searches for a local JBoss JTA instance</li>
 *         <li>Atomikos JTA: it searches for a local Atomikos instance</li>
 *     </ol>
 * </pre>
 * 
 * If none of the above steps are able to locate a transaction manager, ModeShape will fall back to a {@link LocalTransactionManager}
 * instance.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public class DefaultTransactionManagerLookup implements TransactionManagerLookup {

    /**
     * A list of known JNDI binding for different transaction manager implementations
     * 
     * see https://java.net/jira/browse/JAVAEE_SPEC-8
     */
    private static final List<String> JNDI_BINDINGS = Arrays.asList("java:jboss/TransactionManager",
                                                                    "java:comp/TransactionManager",
                                                                    "java:comp/UserTransaction",
                                                                    "java:/TransactionManager",
                                                                    "java:appserver/TransactionManager",
                                                                    "java:pm/TransactionManager",
                                                                    "javax.transaction.TransactionManager",
                                                                    "osgi:service/javax.transaction.TransactionManager");

    private static final Logger LOGGER = Logger.getLogger(DefaultTransactionManagerLookup.class); 
    
    @Override
    public TransactionManager getTransactionManager() {
        return Stream.of(getTransactionManagerSuppliers())
                     .map(Supplier::get)
                     .filter(Optional::isPresent)
                     .map(Optional::get)
                     .peek((transactionManager) ->
                                   LOGGER.debug("Found tx manager '{0}'", transactionManager.getClass().getName()))
                     .findFirst()
                     .orElseGet(() -> {
                         LOGGER.debug(JcrI18n.warnNoTxManagerFound.text());
                         return new LocalTransactionManager();
                     });
    }

    @SuppressWarnings("unchecked")
    protected Supplier<Optional<TransactionManager>>[] getTransactionManagerSuppliers() {
        return new Supplier[] { (Supplier<Optional<TransactionManager>>) this::lookInJNDI,
                                this::lookForStandaloneJBossJTA,
                                this::lookForAtomikosJTA };
    }

    protected Optional<TransactionManager> lookForAtomikosJTA() {
        LOGGER.debug("Looking for Atomikos JTA...");
        try {
            Class<?> clazz = getClass().getClassLoader().loadClass("com.atomikos.icatch.jta.UserTransactionManager");
            Object result = clazz.newInstance();
            return result instanceof TransactionManager ? Optional.of((TransactionManager) result) : Optional.empty();
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.debug(e, "Error while trying to instantiate an Atomikos JTA instance...");
            return Optional.empty();
        } 
    }

    protected Optional<TransactionManager> lookForStandaloneJBossJTA() {
        LOGGER.debug("Looking for JBoss Standalone JTA...");
        try {
            Class<?> clazz = getClass().getClassLoader().loadClass("com.arjuna.ats.jta.TransactionManager");
            Method method = Reflection.findMethod(clazz, "transactionManager");
            Object result = Reflection.invokeAccessibly(null, method, null);
            return result instanceof TransactionManager ? Optional.of((TransactionManager) result) : Optional.empty();
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.debug(e, "Error while trying to instantiate a standalone JBoss JTA instance...");
            return Optional.empty();            
        }
    }

    private Optional<TransactionManager> lookInJNDI() {
        return JNDI_BINDINGS.stream().map(this::lookForJNDIName).filter(Objects::nonNull).findFirst();
    }

    private TransactionManager lookForJNDIName(String jndiName) {
        InitialContext context = null;
        try {
            context = new InitialContext();
        } catch (NamingException e) {
            LOGGER.debug(e, "Cannot create initial JNDI context");
            return null;
        }

        try {
            LOGGER.debug("Looking up transaction manager at: '{0}'", jndiName);
            Object obj = context.lookup(jndiName);
            if (obj instanceof TransactionManager) {
                return (TransactionManager)obj;
            }
            LOGGER.debug("Transaction manager not found at: '{0}'", jndiName);
            return null;
        } catch (NamingException e) {
            LOGGER.debug(e, "Failed to lookup '{0}' in JNDI", jndiName);
            return null;
        } finally {
            try {
                context.close();
            } catch (NamingException e) {
                LOGGER.debug(e, "Cannot close JNDI context");
            }
        }
    }
}
