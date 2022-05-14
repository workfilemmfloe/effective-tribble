package test.kotlin.jtests;

import junit.framework.TestCase;
import kotlin.jvm.functions.Function1;

import java.util.Collection;
import java.util.List;

import static kotlin.KotlinPackage.*;
import static kotlin.util.UtilPackage.*;

/**
 * Lets try using the Kotlin standard library from Java code
 */
public class CollectionTest extends TestCase {

    public void testCollections() throws Exception {
        List<String> list = arrayList("foo", "bar");

        String text = makeString(list, ",", "(", ")", -1, "...");
        System.out.println("Have text: " + text);
        assertEquals("(foo,bar)", text);

        Collection<String> actual = filter(list, new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String text) {
                return text.startsWith("b");
            }
        });

        System.out.println("Filtered list is " + actual);
        assertEquals("(bar)", makeString(actual, ",", "(", ")", -1, "..."));
    }
}
