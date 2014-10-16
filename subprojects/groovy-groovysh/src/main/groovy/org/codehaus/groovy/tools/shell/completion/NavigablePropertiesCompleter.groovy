package org.codehaus.groovy.tools.shell.completion

import java.util.regex.Pattern

class NavigablePropertiesCompleter {

    private static final Pattern NO_CONTROL_CHARS_PATTERN = ~'^[^\\p{Cntrl}]+$'

    // pattern describing particle that must not occur within a string for the string to be a possible identifier
    private static final Pattern INVALID_CHAR_FOR_IDENTIFIER_PATTERN = ~'[ @#%^&§()+\\-={}\\[\\]~`´<>,."\'/!?:;|\\\\]'

    /**
     * Adds navigable properties to the list of candidates if they match the prefix
     */
    void addCompletions(final Object instance, final String prefix, final Set<CharSequence> candidates) {
        if (instance == null) {
            return
        }
        this.addIndirectObjectMembers(instance, prefix, candidates)
    }


    void addIndirectObjectMembers(final Object instance, final String prefix, final Set<CharSequence> candidates) {
        if (instance instanceof Map) {
            Map map = (Map) instance
            addMapProperties(map, prefix, candidates)
        }
        if (instance instanceof Node) {
            Node node = (Node) instance
            addNodeChildren(node, prefix, candidates)
        }
        if (instance instanceof NodeList) {
            NodeList nodeList = (NodeList) instance
            addNodeListEntries(nodeList, prefix, candidates)
        }
    }

    static void addMapProperties(final Map instance, final String prefix, final Set<CharSequence> candidates) {
        // key can be any Object but only Strings will be completed
        for (String key in instance.keySet().findAll {it instanceof String}) {
            // if key has no Control characters
            if (key.matches(NO_CONTROL_CHARS_PATTERN) && key.startsWith(prefix)) {
                // if key cannot be parsed used as identifier name, (contains invalid char or ends with $)
                if (key.find(INVALID_CHAR_FOR_IDENTIFIER_PATTERN) || key.endsWith('$')) {
                    key = key.replace('\\', '\\\\').replace('\'', '\\\'')
                    key = '\'' + key + '\''
                }
                candidates.add(key)
            }
        }
    }

    void addNodeListEntries(final NodeList instance, final String prefix, final Set<CharSequence> candidates) {
        for (Object member : instance) {
            addIndirectObjectMembers(member, prefix, candidates)
        }
    }

    void addNodeChildren(final Node instance, final String prefix, final Set<CharSequence> candidates) {
        for (Object child in instance.children()) {
            String member = ''
            if (child instanceof String) {
                member = (String) child
            } else if (child instanceof Node) {
                member = ((Node) child).name()
            } else if (child instanceof NodeList) {
                for (Object node : ((NodeList) child)) {
                    addNodeChildren((Node) node, prefix, candidates)
                }
            } else {
                continue
            }
            if (member.startsWith(prefix)) {
                candidates.add(member)
            }
        }
    }
}
