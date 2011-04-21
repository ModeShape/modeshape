package org.modeshape.connector.store.jpa;

import org.hibernate.cfg.Environment;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.transaction.TransactionManagerLookup;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;

/**
 * A JPA Source that supports JTA.
 * For more information, check MODE-1123
 * 
 * @author gastaldi (George Gastaldi)
 *
 */
public class JpaJtaSource extends JpaSource {
	

	private static final long serialVersionUID = 1L;
	
	protected static final String DEFAULT_TRANSACTION_MODE = "jta";
	protected static final String DEFAULT_TRANSACTION_STRATEGY = "org.hibernate.transaction.JTATransactionFactory";
	protected static final String DEFAULT_TRANSACTION_MANAGER_STRATEGY = "org.hibernate.transaction.JBossTransactionManagerLookup";
	
	@Description(i18n = JpaConnectorI18n.class, value = "transactionManagerStrategyPropertyDescription")
    @Label(i18n = JpaConnectorI18n.class, value = "transactionManagerStrategyPropertyLabel")
    @Category(i18n = JpaConnectorI18n.class, value = "transactionManagerStrategyPropertyCategory")
    private volatile String transactionManagerStrategy = DEFAULT_TRANSACTION_MANAGER_STRATEGY;

	/**
     * Get the Hibernate setting dictating what is the {@link TransactionManagerLookup} implementation. For more information, see
     * {@link #setTransactionManagerStrategy(String)}.
     * 
     * @return the setting
     */
	public String getTransactionManagerStrategy() {
		return transactionManagerStrategy;
	}
	
	/**
	 * Set the Hibernate setting dictating what is the {@link TransactionManagerLookup} implementation.
	 * 
	 * @param transactionManagerLookupClassName
	 */
	public void setTransactionManagerStrategy(String transactionManagerStrategy) {
		assert transactionManagerStrategy!= null;
		assert transactionManagerStrategy.trim().length() != 0;
		this.transactionManagerStrategy = transactionManagerStrategy;
	}
	
    @Override
    protected void configure(Ejb3Configuration configuration) {
    	super.configure(configuration);
    	// DDL generation should be disabled
    	setProperty(configuration, Environment.HBM2DDL_AUTO, AUTO_GENERATE_SCHEMA_DISABLE);
    	setProperty(configuration, Environment.CURRENT_SESSION_CONTEXT_CLASS, DEFAULT_TRANSACTION_MODE);
    	setProperty(configuration, Environment.TRANSACTION_STRATEGY, DEFAULT_TRANSACTION_STRATEGY);
    	setProperty(configuration, Environment.TRANSACTION_MANAGER_STRATEGY, getTransactionManagerStrategy());
    }
}
