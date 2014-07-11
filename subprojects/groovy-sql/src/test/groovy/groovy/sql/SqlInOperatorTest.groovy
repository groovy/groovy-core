/*
 * Copyright 2003-2013 the original author or authors.
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
package groovy.sql

/**
 * Tests for special handling of the SQL IN operator (see GROOVY-5436)
 */
class SqlInOperatorTest extends SqlHelperTestCase {

    Sql sql

    @Override
    protected void setUp() {
        super.setUp()
        sql = createSql()
    }

    @Override
    protected void tearDown() {
        super.tearDown()
        sql.close()
    }

    void testSimple() {
        def rows = sql.rows('select * from FOOD where type in ?', [['cheese', 'beer']])
        assert 3 == rows.size()
    }

    void testCompatibleWithOldSyntax() {
        def rows = sql.rows('select * from FOOD where type in (?,?)', ['cheese', 'beer'])
        assert 3 == rows.size()
    }

    void testUsingNamedParameter() {
        def rows = sql.rows('select * from FOOD where type in :foo', [foo: ['cheese', 'beer']])
        assert 3 == rows.size()
    }

    void testWithArrayParameter() {
        def rows

        String[] types = ['cheese', 'beer'] as String[]
        rows = sql.rows('select * from FOOD where type in ?', [types])
        assert 3 == rows.size()

        int[] locations = [10, 30] as int[]
        rows = sql.rows('select * from PERSON where location_id in ?', [locations])
        assert 2 == rows.size()
    }

    void testUsingGString() {
        def locations = [10,20]
        def rows = sql.rows("select * from PERSON where location_id in ${locations}")
        assert 2 == rows.size()
    }

    void testWithMultilineSql() {
        String sqlText = '''
select *
from PERSON
where location_id IN
?
  and lastname = ?
'''
        def rows = sql.rows(sqlText, [[10,30], 'Strachan'])
        assert 1 == rows.size()
    }

    void testCompatibleWhenCombinedWithOldSyntax() {
        def rows = sql.rows('select * from PERSON where location_id in (?,?) and lastname in ?', [10,30,['Strachan', 'Mcwhirter']])
        assert 1 == rows.size()
    }

    void testWithPlaceholderWithinString() {
        def rows = sql.rows('''SELECT * FROM FEATURE WHERE name = 'WHERE IN ?' OR id IN ?''', [[2,3]])
        assert 2 == rows.size()
    }

    void testWithUpdate() {
        sql.executeUpdate('update FEATURE set name = ? where id in ?', ['where in', [1,3]])

        def rows = sql.rows('select * from FEATURE where name = ?', ['where in'])
        assert 2 == rows.size()
    }

    void testWithInsert() {
        sql.executeInsert('''insert into FEATURE(id,name) select 9, name from FEATURE where id in ?''', [[3]])

        def rows = sql.rows('select * from FEATURE where id = ?', [9])
        assert 1 == rows.size()
        assert 'GroovyMarkup' == rows[0].name
    }

    void testWithDelete() {
        sql.execute('delete from FEATURE where id in ?', [[2,3]])

        def rows = sql.rows('select * from FEATURE')
        assert 1 == rows.size()
        assert 'GDO' == rows[0].name
    }

    void testWithCaching() {
        def params = [1,2,3]

        assert 3 == params.get(params.size()-1)

        // this test requires caching for named queries is enabled which is the default,
        // so this is just check to make sure the default doesn't change
        assert sql.cacheNamedQueries

        def query = 'select * from FEATURE where id in ?'
        def paramsA = [[1,2,3]]
        def paramsB = [[2,3]]

        def rowsA = sql.rows(query, paramsA)
        assert 3 == rowsA.size()

        def rowsB = sql.rows(query, paramsB)
        assert 2 == rowsB.size()
    }

}
