/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.cache.spring;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.typedef.*;
import org.springframework.cache.*;
import org.springframework.cache.support.*;

import java.io.*;

/**
 * Spring cache implementation.
 */
class GridSpringCache implements Cache, Serializable {
    /** */
    private static final boolean DEBUG = Boolean.getBoolean(GridSystemProperties.GG_SPRING_CACHE_DEBUG);

    /** */
    private GridLogger log;

    /** */
    private String name;

    /** */
    private GridCacheProjection<Object, Object> cache;

    /** */
    private GridClosure<Object, Object> keyFactory;

    /**
     * @param log Logger.
     * @param name Cache name.
     * @param cache Cache.
     * @param keyFactory Key factory.
     */
    GridSpringCache(GridLogger log, String name, GridCacheProjection<?, ?> cache,
        GridClosure<Object, Object> keyFactory) {
        assert cache != null;

        this.log = log;
        this.name = name;
        this.cache = (GridCacheProjection<Object, Object>)cache;
        this.keyFactory = keyFactory != null ? keyFactory : F.identity();
    }

    /** {@inheritDoc} */
    @Override public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override public Object getNativeCache() {
        return cache;
    }

    /** {@inheritDoc} */
    @Override public ValueWrapper get(Object key) {
        try {
            long s = DEBUG ? System.currentTimeMillis() : 0;

            Object val = cache.get(keyFactory.apply(key));

            if (DEBUG) {
                long d = System.currentTimeMillis() - s;

                System.out.println(">>> Get [cache=" + name + ", key=" + key + ", time=" + d + "ms.]");

                if (log.isDebugEnabled())
                    log.debug(">>> Get [cache=" + name + ", key=" + key + ", time=" + d + "ms.]");
            }

            return val != null ? new SimpleValueWrapper(val) : null;
        }
        catch (GridException e) {
            throw new GridRuntimeException("Failed to get value from cache [cacheName=" + cache.name() +
                ", key=" + key + ']', e);
        }
    }

    /** {@inheritDoc} */
    @Override public <T> T get(Object key, Class<T> type) {
        try {
            long s = DEBUG ? System.currentTimeMillis() : 0;

            Object val = cache.get(keyFactory.apply(key));

            if (DEBUG) {
                long d = System.currentTimeMillis() - s;

                System.out.println(">>> Typed get [cache=" + name + ", key=" + key + ", time=" + d + "ms.]");

                if (log.isDebugEnabled())
                    log.debug(">>> Typed get [cache=" + name + ", key=" + key + ", time=" + d + "ms.]");
            }

            if (val != null && type != null && !type.isInstance(val))
                throw new IllegalStateException("Cached value is not of required type [cacheName=" + cache.name() +
                    ", key=" + key + ", val=" + val + ", requiredType=" + type + ']');

            return (T)val;
        }
        catch (GridException e) {
            throw new GridRuntimeException("Failed to get value from cache [cacheName=" + cache.name() +
                ", key=" + key + ']', e);
        }
    }

    /** {@inheritDoc} */
    @Override public void put(Object key, Object val) {
        try {
            long s = DEBUG ? System.currentTimeMillis() : 0;

            cache.putx(keyFactory.apply(key), val);

            if (DEBUG) {
                long d = System.currentTimeMillis() - s;

                System.out.println(">>> Put [cache=" + name + ", key=" + key + ", time=" + d + "ms.]");

                if (log.isDebugEnabled())
                    log.debug(">>> Put [cache=" + name + ", key=" + key + ", time=" + d + "ms.]");
            }
        }
        catch (GridException e) {
            throw new GridRuntimeException("Failed to put value to cache [cacheName=" + cache.name() +
                ", key=" + key + ", val=" + val + ']', e);
        }
    }

    /** {@inheritDoc} */
    @Override public ValueWrapper putIfAbsent(Object key, Object val) {
        try {
            long s = DEBUG ? System.currentTimeMillis() : 0;

            Object old = cache.putIfAbsent(keyFactory.apply(key), val);

            if (DEBUG) {
                long d = System.currentTimeMillis() - s;

                System.out.println(">>> Put if absent [cache=" + name + ", key=" + key + ", time=" + d + "ms.]");

                if (log.isDebugEnabled())
                    log.debug(">>> Put if absent [cache=" + name + ", key=" + key + ", time=" + d + "ms.]");
            }

            return old != null ? new SimpleValueWrapper(old) : null;
        }
        catch (GridException e) {
            throw new GridRuntimeException("Failed to put value to cache [cacheName=" + cache.name() +
                ", key=" + key + ", val=" + val + ']', e);
        }
    }

    /** {@inheritDoc} */
    @Override public void evict(Object key) {
        try {
            cache.removex(keyFactory.apply(key));
        }
        catch (GridException e) {
            throw new GridRuntimeException("Failed to remove value from cache [cacheName=" + cache.name() +
                ", key=" + key + ']', e);
        }
    }

    /** {@inheritDoc} */
    @Override public void clear() {
        try {
            cache.gridProjection().compute().broadcast(new ClearClosure(cache)).get();
        }
        catch (GridException e) {
            throw new GridRuntimeException("Failed to clear cache [cacheName=" + cache.name() + ']', e);
        }
    }

    /**
     * Closure that removes all entries from cache.
     */
    private static class ClearClosure extends CAX implements Externalizable {
        /** */
        private static final long serialVersionUID = 0L;

        /** Cache projection. */
        private GridCacheProjection<Object, Object> cache;

        /**
         * For {@link Externalizable}.
         */
        public ClearClosure() {
            // No-op.
        }

        /**
         * @param cache Cache projection.
         */
        private ClearClosure(GridCacheProjection<Object, Object> cache) {
            this.cache = cache;
        }

        /** {@inheritDoc} */
        @Override public void applyx() throws GridException {
            cache.removeAll();
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(cache);
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            cache = (GridCacheProjection<Object, Object>)in.readObject();
        }
    }
}
