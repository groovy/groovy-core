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
package org.codehaus.groovy.ast.expr;

import org.codehaus.groovy.ast.GroovyCodeVisitor;

/**
 * Represents a range expression such as for iterating.
 * E.g.:
 * <pre>for i in 0..10 {...}</pre>
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public class RangeExpression extends Expression {

    private Expression from;
    private Expression to;
    private boolean inclusive;

    public RangeExpression(Expression from, Expression to, boolean inclusive) {
        this.from = from;
        this.to = to;
        this.inclusive = inclusive;
    }

    public void visit(GroovyCodeVisitor visitor) {
        visitor.visitRangeExpression(this);
    }

    public Expression transformExpression(ExpressionTransformer transformer) {
        Expression ret = new RangeExpression(transformer.transform(from), transformer.transform(to), inclusive); 
        ret.setSourcePosition(this);
        ret.copyNodeMetaData(this);
        return ret;
    }

    public Expression getFrom() {
        return from;
    }

    public Expression getTo() {
        return to;
    }

    public boolean isInclusive() {
        return inclusive;
    }

    public String getText() {
        return "(" + from.getText() +
               (!isInclusive()? "..<" : ".." ) +
               to.getText() + ")";
    }
}
