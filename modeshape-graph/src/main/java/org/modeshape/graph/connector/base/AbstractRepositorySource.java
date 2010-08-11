package org.modeshape.graph.connector.base;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.naming.BinaryRefAddr;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySourceException;

/**
 * Basic implementation of {@link BaseRepositorySource}, providing default implementations of the accessors and mutators in that
 * interface.
 */
@SuppressWarnings( "serial" )
public abstract class AbstractRepositorySource implements BaseRepositorySource {

    /**
     * The default UUID that is used for root nodes in a store.
     */
    public static final String DEFAULT_ROOT_NODE_UUID = "cafebabe-cafe-babe-cafe-babecafebabe";

    /**
     * The default number of times that a request that failed due to system error should be retried
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    /**
     * The default cache policy for this repository source (no caching)
     */
    public static final CachePolicy DEFAULT_CACHE_POLICY = null;

    @Description( i18n = GraphI18n.class, value = "retryLimitPropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "retryLimitPropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "retryLimitPropertyCategory" )
    protected int retryLimit = DEFAULT_RETRY_LIMIT;

    @Description( i18n = GraphI18n.class, value = "namePropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "namePropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "namePropertyCategory" )
    private volatile String name;

    @Description( i18n = GraphI18n.class, value = "rootNodeUuidWithDefaultPropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "rootNodeUuidPropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "rootNodeUuidPropertyCategory" )
    protected transient UUID rootNodeUuid = UUID.fromString(DEFAULT_ROOT_NODE_UUID);

    protected transient RepositoryContext repositoryContext;
    protected transient CachePolicy cachePolicy = DEFAULT_CACHE_POLICY;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.BaseRepositorySource#areUpdatesAllowed()
     */
    public boolean areUpdatesAllowed() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.BaseRepositorySource#getRepositoryContext()
     */
    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#initialize(RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        CheckArg.isNotNull(context, "context");
        this.repositoryContext = context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.BaseRepositorySource#getDefaultCachePolicy()
     */
    public CachePolicy getDefaultCachePolicy() {
        return this.cachePolicy;
    }

    /**
     * Sets the cache policy for the repository and replaces the path repository cache with a new path repository cache tied to
     * the new cache policy
     * 
     * @param cachePolicy the new cache policy; may not be null
     */
    public void setCachePolicy( CachePolicy cachePolicy ) {
        CheckArg.isNotNull(cachePolicy, "cachePolicy");
        this.cachePolicy = cachePolicy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.BaseRepositorySource#getRootNodeUuidObject()
     */
    public UUID getRootNodeUuidObject() {
        return rootNodeUuid;
    }

    /**
     * @param rootNodeUuid Sets rootNodeUuid to the specified value.
     * @throws IllegalArgumentException if the string value cannot be converted to UUID
     */
    public void setRootNodeUuidObject( String rootNodeUuid ) {
        if (rootNodeUuid != null && rootNodeUuid.trim().length() == 0) rootNodeUuid = DEFAULT_ROOT_NODE_UUID;
        this.rootNodeUuid = UUID.fromString(rootNodeUuid);
    }

    /**
     * Get the UUID of the root node for the cache. If the cache exists, this UUID is not used but is instead set to the UUID of
     * the existing root node.
     * 
     * @return the UUID of the root node for the cache.
     */
    public String getRootNodeUuid() {
        return this.rootNodeUuid.toString();
    }

    /**
     * Set the UUID of the root node in this repository. If the cache exists, this UUID is not used but is instead set to the UUID
     * of the existing root node.
     * 
     * @param rootNodeUuid the UUID of the root node for the cache, or null if the UUID should be randomly generated
     */
    public synchronized void setRootNodeUuid( String rootNodeUuid ) {
        UUID uuid = null;
        if (rootNodeUuid == null) uuid = UUID.randomUUID();
        else uuid = UUID.fromString(rootNodeUuid);
        if (this.rootNodeUuid.equals(uuid)) return; // unchanged
        this.rootNodeUuid = uuid;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    public void close() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the repository source. The name should be unique among loaded repository sources.
     * 
     * @param name the new name for the repository source; may not be empty
     */
    public void setName( String name ) {
        if (name != null) {
            name = name.trim();
            if (name.length() == 0) name = null;
        }

        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#setRetryLimit(int limit)
     */
    public void setRetryLimit( int limit ) {
        this.retryLimit = limit < 0 ? 0 : limit;
    }

    /**
     * Extracts the values from the given reference, automatically translating {@link BinaryRefAddr} instances into the
     * deserialized classes that they represent.
     * 
     * @param ref the reference from which the values should be extracted
     * @return a map of value names to values from the reference
     * @throws IOException if there is an error deserializing a {@code BinaryRefAddr}
     * @throws ClassNotFoundException if a serialized class cannot be deserialized because its class is not in the class path
     */
    protected Map<String, Object> valuesFrom( Reference ref ) throws IOException, ClassNotFoundException {
        Map<String, Object> values = new HashMap<String, Object>();

        Enumeration<?> en = ref.getAll();
        while (en.hasMoreElements()) {
            RefAddr subref = (RefAddr)en.nextElement();
            if (subref instanceof StringRefAddr) {
                String key = subref.getType();
                Object value = subref.getContent();
                if (value != null) values.put(key, value.toString());
            } else if (subref instanceof BinaryRefAddr) {
                String key = subref.getType();
                Object value = subref.getContent();
                if (value instanceof byte[]) {
                    // Deserialize ...
                    ByteArrayInputStream bais = new ByteArrayInputStream((byte[])value);
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    value = ois.readObject();
                    values.put(key, value);
                }
            }
        }

        return values;
    }
}
