/*
 * Copyright 2003-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package gdk

import groovy.io.FileType
import groovy.io.FileVisitResult
import groovy.transform.CompileStatic

class WorkingWithIOSpecTest extends GroovyTestCase {

    private final static boolean unixlike =
            System.getProperty('os.name').contains('Linux') ||
                    System.getProperty('os.name').contains('Mac OS')
   private final static boolean windoz =
            System.getProperty('os.name').contains('Windows')

    @CompileStatic
    private void doInTmpDir(Closure cl) {
        def baseDir = File.createTempDir()
        try {
            cl.call(new FileTreeBuilder(baseDir))
        } finally {
            baseDir.deleteDir()
        }
    }

    void testFileIntro() {
        doInTmpDir { dir ->
            File baseDir = dir.baseDir
            dir.'haiku.txt'('''Un vieil étang et
Une grenouille qui plonge,
Le bruit de l'eau.''')
            // tag::print_file_lines[]
            new File(baseDir, 'haiku.txt').eachLine { line ->
                println line
            }
            // end::print_file_lines[]

            // tag::print_file_lines2[]
            new File(baseDir, 'haiku.txt').eachLine { line, nb ->
                println "Line $nb: $line"
            }
            // end::print_file_lines2[]

            // tag::collect_lines[]
            def list = new File(baseDir, 'haiku.txt').collect {it}
            // end::collect_lines[]
            // tag::lines_as_strings[]
            def array = new File(baseDir, 'haiku.txt') as String[]
            // end::lines_as_strings[]
            assert list.size()==3
            assert array.length==3

            def file = new File(baseDir, 'haiku.txt')
            // tag::file_bytes[]
            byte[] contents = file.bytes
            // end::file_bytes[]
            assert contents.length==64

        }
    }

    void testWithReader() {
        doInTmpDir { dir ->
            File baseDir = dir.baseDir
            dir.'haiku.txt'('''Un vieil étang et
Une grenouille qui plonge,
Le bruit de l'eau.
Fin.''')
            try {
                // tag::withreader_exception[]
                def count = 0, MAXSIZE = 3
                new File(baseDir,"haiku.txt").withReader { reader ->
                    while (reader.readLine()) {
                        if (++count > MAXSIZE) {
                            throw new RuntimeException('Haiku should only have 3 verses')
                        }
                    }
                }
                // end::withreader_exception[]
                assert false
            } catch (RuntimeException e) {
                assert e.message == 'Haiku should only have 3 verses'
            }
        }
    }

    void testWithWriter() {
        doInTmpDir { dir ->
            File baseDir = dir.baseDir
            // tag::withwriter_example[]
            new File(baseDir,'haiku.txt').withWriter('utf-8') { writer ->
                writer.writeLine 'Into the ancient pond'
                writer.writeLine 'A frog jumps'
                writer.writeLine 'Water’s sound!'
            }
            // end::withwriter_example[]
        }
    }

    void testLeftShift() {
        doInTmpDir { dir ->
            File baseDir = dir.baseDir
            // tag::file_leftshift[]
            new File(baseDir,'haiku.txt') << '''Into the ancient pond
            A frog jumps
            Water’s sound!'''
            // end::file_leftshift[]
        }
    }

    void testSetBytes() {
        doInTmpDir { dir ->
            File baseDir = dir.baseDir
            def file = new File(baseDir, 'binary.bin')
            // tag::file_setbytes[]
            file.bytes = [66,22,11]
            // end::file_setbytes[]
        }
    }

    void testEachFile() {
        doInTmpDir { builder ->
            File dir = builder {
                'file1.txt'('file 1')
                'file2.txt'('file 2')
                'file3.bin'('file 3')
            }
            // tag::eachfile[]
            dir.eachFile { file ->                      // <1>
                println file.name
            }
            dir.eachFileMatch(~/.*\.txt/) { file ->     // <2>
                println file.name
            }
            // end::eachfile[]
        }
    }

    void testEachFileRecurse() {
        doInTmpDir { builder ->
            File dir = builder {
                'file1.txt'('file 1')
                'file2.txt'('file 2')
                bin {
                    'file3.bin'('file 3')
                }
            }
            // tag::eachfilerecurse[]
            dir.eachFileRecurse { file ->                      // <1>
                println file.name
            }

            dir.eachFileRecurse(FileType.FILES) { file ->      // <2>
                println file.name
            }
            // end::eachfilerecurse[]
        }
    }

    void testTraverse() {
        doInTmpDir { builder ->
            File dir = builder {
                'file1.txt'('file 1')
                'file2.txt'('file 2')
                bin {
                    'file3.bin'('file 3')
                }
            }
            // tag::traverse[]
            dir.traverse { file ->
                if (file.directory && file.name=='bin') {
                    FileVisitResult.TERMINATE                   // <1>
                } else {
                    println file.name
                    FileVisitResult.CONTINUE                    // <2>
                }

            }
            // end::traverse[]
        }
    }

    void testGetInputStreamFromFile() {
        doInTmpDir { dir ->
            File baseDir = dir.baseDir
            dir.'haiku.txt'('''Un vieil étang et
Une grenouille qui plonge,
Le bruit de l'eau.
Fin.''')
            // tag::newinputstream[]
            def is = new File(baseDir,'haiku.txt').newInputStream()
            // do something ...
            is.close()
            // end::newinputstream[]

            // tag::withinputstream[]
            new File(baseDir,'haiku.txt').withInputStream { stream ->
                // do something ...
            }
            // end::withinputstream[]
        }
    }

    void testFileOutputStream() {
        doInTmpDir { dir ->
            File baseDir = dir.baseDir
            // tag::newoutputstream[]
            def os = new File(baseDir,'data.bin').newOutputStream()
            // do something ...
            os.close()
            // end::newoutputstream[]

            // tag::withoutputstream[]
            new File(baseDir,'data.bin').withOutputStream { stream ->
                // do something ...
            }
            // end::withoutputstream[]
        }
    }

    void testDataInputOutput() {
        doInTmpDir { dir ->
            File baseDir = dir.baseDir
            def file = new File(baseDir, 'data.bin')
            // tag::data_in_out[]
            boolean b = true
            String message = 'Hello from Groovy'
            // Serialize data into a file
            file.withDataOutputStream { out ->
                out.writeBoolean(b)
                out.writeUTF(message)
            }
            // ...
            // Then read it back
            file.withDataInputStream { input ->
                assert input.readBoolean() == b
                assert input.readUTF() == message
            }
            // end::data_in_out[]
        }
    }

    void testObjectInputOutput() {
        doInTmpDir { dir ->
            File baseDir = dir.baseDir
            def file = new File(baseDir, 'data.bin')
            // tag::object_in_out[]
            Person p = new Person(name:'Bob', age:76)
            // Serialize data into a file
            file.withObjectOutputStream { out ->
                out.writeObject(p)
            }
            // ...
            // Then read it back
            file.withObjectInputStream { input ->
                def p2 = input.readObject()
                assert p2.name == p.name
                assert p2.age == p.age
            }
            // end::object_in_out[]
        }
    }

    void testProcess1() {
        if (unixlike) {
            // tag::process_list_files[]
            def process = "ls -l".execute()             // <1>
            println "Found text ${process.text}"        // <2>
            // end::process_list_files[]
            assert process instanceof Process
        }
        if (windoz) {
            try {
                // tag::dir_windows[]
                def process = "dir".execute()
                println "${process.text}"
                // end::dir_windows[]
                assert false
            } catch (e) {
                // tag::dir_windows_fixed[]
                def process = "cmd /c dir".execute()
                println "${process.text}"
                // end::dir_windows_fixed[]
            }
        }
    }

   void testProcess2() {
        if (unixlike) {
            // tag::process_list_files_line_by_line[]
            def process = "ls -l".execute()             // <1>
            process.in.eachLine { line ->               // <2>
                println line                            // <3>
            }
            // end::process_list_files[]
            assert process instanceof Process
        }
    }

    void testProcessConsumeOutput() {
        if (unixlike) {
            doInTmpDir { b ->
                File file = null
                def tmpDir = b.tmp {
                    file = 'foo.tmp'('foo')
                }
                assert file.exists()
                // tag::consumeoutput[]
                def p = "rm -f foo.tmp".execute([], tmpDir)
                p.consumeProcessOutput()
                // end::consumeoutput[]
                p.waitFor()
                assert !file.exists()
            }

        }
    }

    void testProcessPipe() {
        if (unixlike) {
            doInTmpDir { b ->
                def proc1, proc2, proc3, proc4
                // tag::pipe_example_1[]
                proc1 = 'ls'.execute()
                proc2 = 'tr -d o'.execute()
                proc3 = 'tr -d e'.execute()
                proc4 = 'tr -d i'.execute()
                proc1 | proc2 | proc3 | proc4
                proc4.waitFor()
                if (proc4.exitValue()) {
                    println proc4.err.text
                } else {
                    println proc4.text
                }
                // end::pipe_example_1[]

                // tag::pipe_example_2[]
                def sout = new StringBuilder()
                def serr = new StringBuilder()
                proc2 = 'tr -d o'.execute()
                proc3 = 'tr -d e'.execute()
                proc4 = 'tr -d i'.execute()
                proc4.consumeProcessOutput(sout, serr)
                proc2 | proc3 | proc4
                [proc2, proc3].each { it.consumeProcessErrorStream(serr) }
                proc2.withWriter { writer ->
                    writer << 'testfile.groovy'
                }
                proc4.waitForOrKill(1000)
                println "Standard output: $sout"
                println "Standard error: $serr"
                // end::pipe_example_2[]
            }
        }
    }

    public static class Person implements Serializable {
        String name
        int age
    }
}
