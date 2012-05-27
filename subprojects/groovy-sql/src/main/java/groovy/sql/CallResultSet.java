/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package groovy.sql;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author rfuller
 *         <p/>
 *         Represents a ResultSet retrieved as a callable statement out parameter.
 */
class CallResultSet extends GroovyResultSetExtension {
    int indx;
    CallableStatement call;
    ResultSet resultSet;
    boolean firstCall = true;

    CallResultSet(CallableStatement call, int indx) {
        super(null);
        this.call = call;
        this.indx = indx;
    }

    protected ResultSet getResultSet() throws SQLException {
        if (firstCall) {
            resultSet = (ResultSet) call.getObject(indx + 1);
            firstCall = false;
        }
        return resultSet;
    }

    protected static GroovyResultSet getImpl(CallableStatement call, int idx) {
        GroovyResultSetProxy proxy = new GroovyResultSetProxy(new CallResultSet(call, idx));
        return proxy.getImpl();
    }
}
