import gls.CompilableTestSupport

class ProgramStructureTest extends CompilableTestSupport {
	void testPackageName() {
		assertScript '''
// tag::package_name_example[]
package org.example.packagename 

class Example { <1>
}
// end::package_name_example[]
'''
	}

}