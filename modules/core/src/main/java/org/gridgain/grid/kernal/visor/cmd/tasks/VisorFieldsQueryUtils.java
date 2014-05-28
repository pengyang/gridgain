/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.visor.cmd.tasks;

import org.gridgain.grid.GridException;
import org.gridgain.grid.cache.query.GridCacheQueryFuture;
import org.gridgain.grid.kernal.visor.cmd.dto.node.VisorFieldsQueryColumn;
import org.gridgain.grid.lang.GridBiTuple;
import org.gridgain.grid.util.GridUtils;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Contains utility methods for Visor query fields tasks and jobs.
 */
public class VisorFieldsQueryUtils {
    /** How long to store future. */
    public static final Integer RMV_DELAY = 5 * 60; // 5 minutes.

    public static final String SQL_QRY_NAME = "VISOR_FIELDS_QUERY";

    public static final String SCAN_QRY_NAME = "VISOR_SCAN_QUERY";

    public static final VisorFieldsQueryColumn[] SCAN_COL_NAMES = new VisorFieldsQueryColumn[] {
        new VisorFieldsQueryColumn("", "Key Class"), new VisorFieldsQueryColumn("", "Key"),
        new VisorFieldsQueryColumn("", "Value Class"), new VisorFieldsQueryColumn("", "Value")
    };

    private static String typeOf(Object o) {
        if (o != null) {
            Class<?> clazz = o.getClass();

            return clazz.isArray() ? GridUtils.compact(clazz.getComponentType().getName()) + "[]"
                : GridUtils.compact(o.getClass().getName());
        }
        else
            return "n/a";
    }

    private static String valueOf(Object o) {
        if (o == null)
            return "null";
        if (o instanceof byte[])
            return "size=" + ((byte[]) o).length;
        if (o instanceof Byte[])
            return "size=" + ((Byte[]) o).length;
        if (o instanceof Object[])
            return "size=" + ((Object[]) o).length + ", values=[" + mkString((Object[]) o, 60) + "]";
        return o.toString();
    }

    private static String mkString(Object[] arr, int maxSz) {
        String end = "...";
        String sep = ", ";

        StringBuilder sb = new StringBuilder();

        Boolean first = true;

        for (Object v: arr) {
            if (first)
                first = false;
            else
                sb.append(sep);

            sb.append(v);

            if (sb.length() < maxSz)
                break;
        }

        if (sb.length() >= maxSz) {
            sb.setLength(maxSz - end.length());

            sb.append(end);
        }

        return sb.toString();
    }

    /**
     * Fetch rows from SCAN query future.
     *
     * @param fut Query future to fetch rows from.
     * @param savedNext Last processed element from future.
     * @param pageSize Number of rows to fetch.
     * @return Fetched rows and last processed element.
     */
    public static GridBiTuple<List<Object[]>, Map.Entry<Object, Object>> fetchScanQueryRows(
        GridCacheQueryFuture<Map.Entry<Object, Object>> fut, Map.Entry<Object, Object> savedNext, int pageSize
    ) throws GridException {
        List<Object[]> rows = new ArrayList<>();

        int cnt = 0;

        Map.Entry<Object, Object> next = savedNext != null ? savedNext : fut.next();

        while (next != null && cnt < pageSize) {
            Object k = next.getKey();
            Object v = next.getValue();

            rows.add(new Object[] { typeOf(k), valueOf(k), typeOf(v), valueOf(v) });

            cnt ++;

            next = fut.next();
        }

        return new GridBiTuple<>(rows, next);
    }

    /**
     * Checks is given object is one of known types.
     *
     * @param obj Object instance to check.
     * @return `true` if it is one of known types.
     */
    private static Boolean isKnownType(Object obj) {
        return obj instanceof String||
            obj instanceof Boolean ||
            obj instanceof Byte ||
            obj instanceof Integer ||
            obj instanceof Long ||
            obj instanceof Short ||
            obj instanceof Date ||
            obj instanceof Double ||
            obj instanceof Float ||
            obj instanceof BigDecimal ||
            obj instanceof URL;
    }

    /**
     * Collects rows from sql query future, first time creates meta and column names arrays.
     *
     * @param fut Query future to fetch rows from.
     * @param savedNext Last processed element from future.
     * @param pageSize Number of rows to fetch.
     * @return Fetched rows and last processed element.
     */
    public static GridBiTuple<List<Object[]>, List<?>> fetchSqlQueryRows(GridCacheQueryFuture<List<?>> fut,
        List<?> savedNext, int pageSize) throws GridException {
        List<Object[]> rows = new ArrayList<>();

        int cnt = 0;

        List<?> next = savedNext != null ? savedNext : fut.next();

        while (next != null && cnt < pageSize) {
            Object[] row = new Object[next.size()];

            for (int i =0; i < next.size(); i++) {
                Object o = next.get(i);

                if (o == null)
                    row[i] = null;
                else if (isKnownType(o))
                    row[i] = o;
                else if (o.getClass().isArray())
                    row[i] = "binary";
                else
                    row[i] = o.toString(); // We can not pass unknown classes to Visor.
            }

            rows.add(row);
            cnt ++;

            next = fut.next();
        }

        return new GridBiTuple<List<Object[]>, List<?>>(rows, next);
    }
}
