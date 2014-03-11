/* @cpp.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

#include "gridgain/impl/utils/gridclientdebug.hpp"

#include "gridgain/impl/utils/gridfutureimpl.hpp"
#include "gridgain/gridclienttypedef.hpp"
#include "gridgain/gridconf.hpp"

template class GRIDGAIN_API GridClientFuture<std::vector<std::string> >;
template class GRIDGAIN_API GridClientFuture<TGridClientVariantMap>;
template class GRIDGAIN_API GridClientFuture<GridClientDataMetrics>;
template class GRIDGAIN_API GridClientFuture<GridClientVariant>;
template class GRIDGAIN_API GridClientFuture<TGridClientNodePtr>;
template class GRIDGAIN_API GridClientFuture<TGridClientNodeList>;
