package groovy.transform;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

@java.lang.annotation.Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE})
@GroovyASTTransformationClass("org.codehaus.groovy.transform.PojoBuilderASTTransformation")
public @interface PojoBuilder {
	Class forClass();
	
	String[] excludes() default {};
	
	String[] includes() default {};
}
