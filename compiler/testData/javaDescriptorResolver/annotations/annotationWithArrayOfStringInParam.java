package annotations;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@interface MyAnnotation {
    String[] value();
}

@MyAnnotation({"a", "b", "c"})
class A {
    
}
