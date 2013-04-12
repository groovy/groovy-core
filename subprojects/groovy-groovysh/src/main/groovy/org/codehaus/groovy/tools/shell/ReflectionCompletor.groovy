package org.codehaus.groovy.tools.shell

import jline.Completor
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.tools.shell.util.PackageHelper

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Implements the Completor interface to provide completions for
 * GroovyShell by using reflection on global variables, global
 * variable names, keywords, and imported Classes
 *
 * @author <a href="mailto:probabilitytrees@gmail.com">Marty Saxton</a>
 */
class ReflectionCompletor implements Completor {

    private Groovysh shell;
    PackageHelper packageHelper
    List<String> preimportedClassNames = null


    ReflectionCompletor(Groovysh shell, PackageHelper packageHelper) {
        this.shell = shell
        this.packageHelper = packageHelper
    }

    int complete(String buffer, int cursor, List candidates) {

        int identifierStart = findIdentifierStart(buffer, cursor)
        String identifierPrefix = identifierStart != -1 ? buffer.substring(identifierStart, cursor) : ""
        int lastDot = buffer.lastIndexOf('.')

        // if there are no dots, and there is a valid identifier prefix
        if (lastDot == -1 ) {
            if (identifierStart != -1) {
                List myCandidates = findMatchingVariables(identifierPrefix)
                myCandidates.addAll(findMatchingImportedClasses(identifierPrefix, shell.imports))
                if (myCandidates.size() > 0) {
                    candidates.addAll(myCandidates)
                    return identifierStart

                }
            }
        }
        else {
            // there are 1 or more dots
            // if ends in a dot, or if there is a valid identifier prefix
            if (lastDot == cursor-1 || identifierStart != -1){
                // evaluate the part before the dot to get an instance
                String instanceRefExpression = buffer.substring(0, lastDot)
                try {
                    def instance = shell.interp.evaluate([instanceRefExpression])
                    if (instance != null) {
                        // look for public methods/fields that match the prefix
                        List myCandidates = getPublicFieldsAndMethods(instance, identifierPrefix)
                        if (myCandidates.size() > 0) {
                            candidates.addAll(myCandidates)
                            return lastDot+1
                        }
                    }
                } catch (MultipleCompilationErrorsException e) {
                    return -1
                }
            }
        }

        // no candidates
        return -1
    }

    /**
     * Parse a buffer to determine the start index of the groovy identifier
     * @param buffer the buffer to parse
     * @param endingAt the end index with the buffer
     * @return the start index of the identifier, or -1 if the buffer
     * does not contain a valid identifier that ends at endingAt
     */
    int findIdentifierStart(String buffer, int endingAt) {
        // if the string is empty then there is no expression
        if (endingAt == 0)
            return -1
        // if the last character is not valid then there is no expression
        char lastChar = buffer.charAt(endingAt-1)
        if (!Character.isJavaIdentifierPart(lastChar))
            return -1
        // scan backwards until the beginning of the expression is found
        int startIndex = endingAt-1
        while (startIndex > 0 && Character.isJavaIdentifierPart(buffer.charAt(startIndex-1)))
            --startIndex
        return startIndex
    }


    /**
     * Build a list of public fields and methods for an object
     * that match a given prefix.
     * @param instance the object
     * @param prefix the prefix that must be matched
     * @return the list of public methods and fields that begin with the prefix
     */
    List getPublicFieldsAndMethods(Object instance, String prefix) {
        def rv = []
        instance.class.fields.each {
            if (it.name.startsWith(prefix))
                rv << it.name
        }
        instance.class.methods.each {
            if (it.name.startsWith(prefix))
                rv << it.name + (it.parameterTypes.length == 0 ? "()" : "(")
        }
        InvokerHelper.getMetaClass(instance).metaMethods.each {
            if (it.name.startsWith(prefix))
                rv << it.name + (it.parameterTypes.length == 0 ? "()" : "(")
        }
        return rv.sort().unique()
    }

    /**
     * Build a list of variables defined in the shell that
     * match a given prefix.
     * @param prefix the prefix to match
     * @return the list of variables that match the prefix
     */
    List findMatchingVariables(String prefix) {
        def matches = []
        for (String varName in shell.interp.context.variables.keySet())
            if (varName.startsWith(prefix))
                matches << varName
        return matches
    }

    /**
     * Build a list of imported classes defined in the shell that
     * match a given prefix.
     * @param prefix the prefix to match
     * @return the list of variables that match the prefix
     */
    List<String> findMatchingImportedClasses(String prefix, List<String> imports) {
        def matches = []
        if (preimportedClassNames == null) {
            preimportedClassNames = []
            for (packname in org.codehaus.groovy.control.ResolveVisitor.DEFAULT_IMPORTS) {
                Set<String> packnames = packageHelper.getContents(packname[0..-2])
                if (packnames) {
                    preimportedClassNames.addAll(packnames.findAll{it[0] in "A".."Z"})
                }
            }
            preimportedClassNames.add("BigInteger")
            preimportedClassNames.add("BigDecimal")
        }
        // preimported names
        for (String varName in preimportedClassNames) {
            if (varName.startsWith(prefix)) {
                matches << varName
            }
        }
        Set<String> classnames = [] as Set
        // imported names, build pattern to use when looping over all known classes
        StringBuilder pattern = new StringBuilder("(")
        int counter = 0
        for (String importSpec in imports) {
            if (importSpec.endsWith('*')) {
                Set<String> packnames = packageHelper.getContents(importSpec.substring('import '.length()))
                if (packnames) {
                    classnames.addAll(packnames)
                }
            } else {
                classnames << importSpec.substring(importSpec.lastIndexOf('.') + 1)
            }
        }
        pattern.append(")")
        for (String varName in classnames) {
            if (varName.startsWith(prefix)) {
                matches << varName
            }
        }

        return matches
    }
}
