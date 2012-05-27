/*
 * Copyright 2003-2011 the original author or authors.
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

import groovy.lang.Tuple;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class which delegates to a PreparedStatement but keeps track of
 * a batch count size. If the batch count reaches the predefined number,
 * this Statement does an executeBatch() automatically. If batchSize is
 * zero, then no batching is performed.
 *
 * @author Daniel Henrique Alves Lima
 * @author Paul King
 */
public class BatchingPreparedStatementWrapper extends BatchingStatementWrapper {

    private PreparedStatement delegate;
    private List<Tuple> indexPropList;
    private Sql sql;

    public BatchingPreparedStatementWrapper(PreparedStatement delegate, List<Tuple> indexPropList, int batchSize, Logger log, Sql sql) {
        super(delegate, batchSize, log);
        this.delegate = delegate;
        this.indexPropList = indexPropList;
        this.sql = sql;
    }

    public void addBatch(Object[] parameters) throws SQLException {
        addBatch(Arrays.asList(parameters));
    }

    public void addBatch(List<Object> parameters) throws SQLException {
        if (indexPropList != null) {
            sql.setParameters(sql.getUpdatedParams(parameters, indexPropList), delegate);
        } else {
            sql.setParameters(parameters, delegate);
        }
        delegate.addBatch();
        batchCount++;
        if (batchCount == batchSize /* never true for batchSize of 0 */) {
            int[] result = delegate.executeBatch();
            processResult(result);
            batchCount = 0;
        }
    }
}
