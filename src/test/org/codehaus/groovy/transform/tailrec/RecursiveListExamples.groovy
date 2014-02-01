package org.codehaus.groovy.transform.tailrec

import groovy.transform.CompileStatic
import groovy.transform.TailRecursive
import org.junit.Test

/**
 * @author Johannes Link
 */
class RecursiveListExamples {

    def rlist

    @Test
    void creating() {
        rlist = RecursiveList.rlist([1, 2, 3, 4])
        assert RecursiveList.toList(rlist) == [1, 2, 3, 4]
    }

    @Test
    void counting() {
        List integers = new ArrayList(1..9999)
        rlist = RecursiveList.rlist(integers)
        assert RecursiveList.count(rlist) == 9999
    }
}

@CompileStatic
class RecursiveList {
    static final Map empty = [:]

    static Map cons(Object element, Map list) {
        [head: element, tail: list]
    }

    static Map tail(Map rlist) {
        (Map) rlist.tail
    }

    static Object head(Map rlist) {
        rlist.head
    }

    @TailRecursive
    static Map rlist(List elements, Map result = empty) {
        if (!elements)
            return result
        Object head = elements.pop()
        rlist(elements, cons(head, result))
    }

    @TailRecursive
    static List toList(Map rlist, List result = []) {
        if (!rlist)
            return result
        toList(tail(rlist), result << head(rlist) as List)
    }

    @TailRecursive
    static int count(Map rlist, int result = 0) {
        if (!rlist)
            return result
        count(tail(rlist), ++result)
    }
}
