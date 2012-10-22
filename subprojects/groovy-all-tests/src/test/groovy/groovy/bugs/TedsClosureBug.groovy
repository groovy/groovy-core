/*
 * Copyright 2003-2010 the original author or authors.
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
package groovy.bugs

import groovy.xml.MarkupBuilder

/**
 * @author Ted Leung
 * @version $Revision$
 */
class TedsClosureBug extends GroovyTestCase {
    def EXPECTED= '''<atom>
  <title>Ted Leung off the air</title>
  <link>http://www.sauria.com/noblog</link>
  <author>
    <person>
      <name>Ted Leung</name>
      <url>http://www.sauria.com/blog</url>
      <email>twl@sauria.com</email>
    </person>
  </author>
  <entry>
    <title>one</title>
    <summary>first post</summary>
  </entry>
  <entry>
    <title>two</title>
    <summary>the second post</summary>
  </entry>
  <entry>
    <title>three</title>
    <summary>post the third</summary>
  </entry>
  <entry>
    <title>four</title>
    <summary>the ponderous fourth post</summary>
  </entry>
</atom>'''

    void testBug() {
        def f = new Feed()
        f.author = new Person(name:'Ted Leung',url:'http://www.sauria.com/blog', email:'twl@sauria.com')
        f.entries = [ new Entry(title:'one',summary:'first post'), new Entry(title:'two',summary:'the second post'), new Entry(title:'three', summary:'post the third'), new Entry(title:'four',summary:'the ponderous fourth post') ]
        def sw = new StringWriter()
        def xml = new MarkupBuilder(sw)

        def atom = xml.atom() {
          title("Ted Leung off the air")
          link("http://www.sauria.com/noblog")
          author() {
            person() {
              name(f.author.name)
              url(f.author.url)
              email(f.author.email)
            }
          }
          for (e in f.entries) {
            entry() {
              title(e.title)
              summary(e.summary)
            }
          }
        }
        StringTestUtil.assertMultilineStringsEqual(EXPECTED, sw.toString())
    }
}

class Feed {
    String title
    String link
    Person author
    String tagline
    String generator
    String copyright
    String modified
    List entries
}

class Entry {
    String title
    String link
    String id
    String summary
    String content
    Person author
    String created
    String issued
    String modified
}

class Person {
    String name
    String url
    String email
}