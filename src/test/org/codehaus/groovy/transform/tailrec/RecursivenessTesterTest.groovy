package org.codehaus.groovy.transform.tailrec

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.junit.Before
import org.junit.Test

import static org.objectweb.asm.Opcodes.ACC_PUBLIC
import static org.objectweb.asm.Opcodes.ACC_STATIC


/**
 * @author Johannes Link
 */
class RecursivenessTesterTest {

	RecursivenessTester tester

	@Before
	void init() {
		tester = new RecursivenessTester()
	}

	@Test
	public void recursiveCallWithoutParameter() throws Exception {
		/*
		 public void myMethod() {}
		 */
		def method = new AstBuilder().buildFromSpec {
			method('myMethod', ACC_PUBLIC, Void.TYPE) {
				parameters {}
				exceptions {}
				block {
				}
			}
		}[0]

		/*
		 this.myMethod();
		 */
		def innerCall = new AstBuilder().buildFromSpec {
			methodCall {
				variable "this"
				constant "myMethod"
				argumentList {}
			}
		}[0]

		assert tester.isRecursive(method: method, call: innerCall)
	}

	@Test
	public void recursiveCallWithParameters() throws Exception {
		/*
		 public void myMethod(String a, String b) {}
		 */
		def method = new AstBuilder().buildFromSpec {
			method('myMethod', ACC_PUBLIC, Void.TYPE) {
				parameters {
					parameter 'a': String.class
					parameter 'b': String.class
				}
				exceptions {}
				block {
				}
			}
		}[0]

		/*
		 this.myMethod("a", "b");
		 */
		def innerCall = new AstBuilder().buildFromSpec {
			methodCall {
				variable "this"
				constant "myMethod"
				argumentList {
					constant "a"
					constant "b"
				}
			}
		}[0]

		assert tester.isRecursive(method: method, call: innerCall)
	}

	@Test
	public void callWithDifferentNumberOfParameters() throws Exception {
		/*
		 public void myMethod(String a, String b) {}
		 */
		def method = new AstBuilder().buildFromSpec {
			method('myMethod', ACC_PUBLIC, Void.TYPE) {
				parameters {
					parameter 'a': String.class
					parameter 'b': String.class
				}
				exceptions {}
				block {
				}
			}
		}[0]

		/*
		 this.myMethod("a", "b", "c");
		 */
		def innerCall = new AstBuilder().buildFromSpec {
			methodCall {
				variable "this"
				constant "myMethod"
				argumentList {
					constant "a"
					constant "b"
					constant "c"
				}
			}
		}[0]

		assert !tester.isRecursive(method: method, call: innerCall)
	}

    @Test
    public void callWithNonCompatibleArgType() throws Exception {
        /*
         public void myMethod(String a, String b) {}
         */
        def method = new AstBuilder().buildFromSpec {
            method('myMethod', ACC_PUBLIC, Void.TYPE) {
                parameters {
                    parameter 'a': String.class
                }
                exceptions {}
                block {
                }
            }
        }[0]

        /*
         this.myMethod("a", "b", "c");
         */
        def innerCall = new AstBuilder().buildFromSpec {
            methodCall {
                variable "this"
                constant "myMethod"
                argumentList {
                    constant 1
                }
            }
        }[0]

        assert !tester.isRecursive(method: method, call: innerCall)
    }

    @Test
    public void callWithArgASubtypeOfParam() throws Exception {
        /*
         public void myMethod(Number a) {}
         */
        def method = new AstBuilder().buildFromSpec {
            method('myMethod', ACC_PUBLIC, Void.TYPE) {
                parameters {
                    parameter 'a': Number.class
                }
                exceptions {}
                block {
                }
            }
        }[0]

        /*
         this.myMethod(1);
         */
        def innerCall = new AstBuilder().buildFromSpec {
            methodCall {
                variable "this"
                constant "myMethod"
                argumentList {
                    constant 1
                }
            }
        }[0]

        assert tester.isRecursive(method: method, call: innerCall)
    }

    @Test
    public void callWithParamASubtypeOfArg() throws Exception {
        /*
         public void myMethod(int a) {}
         */
        def method = new AstBuilder().buildFromSpec {
            method('myMethod', ACC_PUBLIC, Void.TYPE) {
                parameters {
                    parameter 'a': Number.class
                }
                exceptions {}
                block {
                }
            }
        }[0]

        /*
         this.myMethod(dynamicVariable);
         */
        def innerCall = new AstBuilder().buildFromSpec {
            methodCall {
                variable "this"
                constant "myMethod"
                argumentList {
                    variable "dynamicVariable"
                }
            }
        }[0]

        assert tester.isRecursive(method: method, call: innerCall)
    }

    @Test
	public void recursiveCallWithImplicitThis() throws Exception {
		/*
		 public void myMethod() {}
		 */
		def method = new AstBuilder().buildFromSpec {
			method('myMethod', ACC_PUBLIC, Void.TYPE) {
				parameters {}
				exceptions {}
				block {
				}
			}
		}[0]

		/*
		 myMethod();
		 */
		def innerCall = new MethodCallExpression(null, "myMethod", new ArgumentListExpression());

		assert tester.isRecursive(method: method, call: innerCall)
	}

	@Test
	public void callWithDifferentName() {
		/*
		 public void myMethod() {}
		 */
		def method = new AstBuilder().buildFromSpec {
			method('myMethod', ACC_PUBLIC, Void.TYPE) {
				parameters {}
				exceptions {}
				block {
				}
			}
		}[0]

		/*
		 yourMethod();
		 */
		def innerCall = new AstBuilder().buildFromSpec {
			methodCall {
				variable "this"
				constant "yourMethod"
				argumentList {}
			}
		}[0]

		assert !tester.isRecursive(method: method, call: innerCall)
	}

    @Test
    public void callWithGStringMethodNameIsNotConsideredRecursive() {
        /*
         public void myMethod() {}
         */
        def method = new AstBuilder().buildFromSpec {
            method('myMethod', ACC_PUBLIC, Void.TYPE) {
                parameters {}
                exceptions {}
                block {
                }
            }
        }[0]

        /*
         "$methodName"();
         */
        def innerCall = new AstBuilder().buildFromSpec {
            methodCall {
                variable "this"
                gString '$methodName', {
                    strings {
                        constant ''
                        constant ''
                    }
                    values {
                        variable 'methodName'
                    }
                }
                argumentList {}
            }
        }[0]

        assert !tester.isRecursive(method: method, call: innerCall)
    }

	@Test
	public void callOnDifferentTarget() {
		/*
		 public void myMethod() {}
		 */
		def method = new AstBuilder().buildFromSpec {
			method('myMethod', ACC_PUBLIC, Void.TYPE) {
				parameters {}
				exceptions {}
				block {
				}
			}
		}[0]

		/*
		 other.myMethod();
		 */
		def innerCall = new AstBuilder().buildFromSpec {
			methodCall {
				variable "other"
				constant "myMethod"
				argumentList {}
			}
		}[0]

		assert !tester.isRecursive(method: method, call: innerCall)
	}

	@Test
	public void staticRecursiveCallWithParameter() {
		/*
		 public static void myMethod(String a) {}
		 */
		def method = new AstBuilder().buildFromSpec {
			method('myMethod', (ACC_PUBLIC | ACC_STATIC), Void.TYPE) {
				parameters { 
					parameter 'a': String.class 
				}
				exceptions {}
				block {
				}
			}
		}[0]
		method.declaringClass = ClassHelper.make("MyClass")

		/*
		 myMethod("a");
		 */
		def expressions = [new ConstantExpression("a")]
		def args = new ArgumentListExpression(expressions)
		def innerCall = new StaticMethodCallExpression(ClassHelper.make("MyClass"), "myMethod", args)

		assert tester.isRecursive(method: method, call: innerCall)
	}

	@Test
	public void staticCallWithDifferentNumberOfParameters() {
		/*
		 public static void myMethod(String a) {}
		 */
		def method = new AstBuilder().buildFromSpec {
			method('myMethod', (ACC_PUBLIC | ACC_STATIC), Void.TYPE) {
				parameters {
					parameter 'a': String.class
				}
				exceptions {}
				block {
				}
			}
		}[0]
		method.declaringClass = ClassHelper.make("MyClass")
		
		/*
		 myMethod("a", "b");
		 */
		def expressions = [new ConstantExpression("a"), new ConstantExpression("b")]
		def args = new ArgumentListExpression(expressions)
		def innerCall = new StaticMethodCallExpression(ClassHelper.make("MyClass"), "myMethod", args)

		assert !tester.isRecursive(method: method, call: innerCall)
	}

	@Test
	public void staticCallOnNonStaticMethod() {
		/*
		 public void myMethod() {}
		 */
		def method = new AstBuilder().buildFromSpec {
			method('myMethod', ACC_PUBLIC, Void.TYPE) {
				parameters {}
				exceptions {}
				block {
				}
			}
		}[0]
		method.declaringClass = ClassHelper.make("MyClass")
		
		/*
		 myMethod();
		 */
		def innerCall = new StaticMethodCallExpression(ClassHelper.make("MyClass"), "myMethod", new ArgumentListExpression())

		assert !tester.isRecursive(method: method, call: innerCall)
	}

	@Test
	public void staticCallWithDifferentName() {
		/*
		 public static void myMethod() {}
		 */
		def method = new AstBuilder().buildFromSpec {
			method('myMethod', (ACC_PUBLIC | ACC_STATIC), Void.TYPE) {
				parameters {}
				exceptions {}
				block {
				}
			}
		}[0]
		method.declaringClass = ClassHelper.make("MyClass")
		
		/*
		 yourMethod();
		 */
		def innerCall = new StaticMethodCallExpression(ClassHelper.make("MyClass"), "yourMethod", new ArgumentListExpression())

		assert !tester.isRecursive(method: method, call: innerCall)
	}

	@Test
	public void staticCallOnDifferentClass() {
		/*
		 public static void myMethod() {}
		 */
		def method = new AstBuilder().buildFromSpec {
			method('myMethod', (ACC_PUBLIC | ACC_STATIC), Void.TYPE) {
				parameters {}
				exceptions {}
				block {
				}
			}
		}[0]
		method.declaringClass = ClassHelper.make("MyClass")
		
		/*
		 OtherClass.myMethod();
		 */
		def innerCall = new StaticMethodCallExpression(ClassHelper.make("OtherClass"), "myMethod", new ArgumentListExpression())

		assert !tester.isRecursive(method: method, call: innerCall)
	}

}
