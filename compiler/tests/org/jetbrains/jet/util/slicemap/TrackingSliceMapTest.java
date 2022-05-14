/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.util.slicemap;

import junit.framework.TestCase;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.Slices;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

public class TrackingSliceMapTest extends TestCase {
    public void testSimpleSlice() {
        WritableSlice<String, Integer> SUPER_COMPUTER = Slices.<String, Integer>sliceBuilder().setDebugName("SUPER_COMPUTER").build();
        BindingTraceContext traceContext = BindingTraceContext.createTraceableBindingTrace();

        traceContext.record(SUPER_COMPUTER, "Answer", 42);
        Integer answer = traceContext.get(SUPER_COMPUTER, "Answer");

        assertNotNull(answer);
        assertEquals(42, (int) answer);
    }

    public void testOppositeSlice() {
        WritableSlice<Integer, String> COLOR_NAME = Slices.<Integer, String>sliceBuilder().setDebugName("COLOR_NAME").build();
        WritableSlice<String, Integer> NAME_COLOR = Slices.<String, Integer>sliceBuilder().setOpposite(COLOR_NAME).setDebugName("NAME_COLOR").build();

        BindingTraceContext traceContext = BindingTraceContext.createTraceableBindingTrace();

        traceContext.record(NAME_COLOR, "RED", 0xff0000);
        String redName = traceContext.get(COLOR_NAME, 0xff0000);

        assertEquals("RED", redName);
    }

    public void testFurtherSlices() {
        WritableSlice<String, Integer> NAME_COLOR = Slices.<String, Integer>sliceBuilder().setDebugName("NAME_COLOR").build();

        @SuppressWarnings("unchecked")
        WritableSlice<String, Object> NAME_OBJECT = Slices.<String, Object>sliceBuilder()
                .setFurtherLookupSlices(new ReadOnlySlice[] {NAME_COLOR})
                .setDebugName("NAME_OBJECT").build();

        BindingTraceContext traceContext = BindingTraceContext.createTraceableBindingTrace();

        traceContext.record(NAME_COLOR, "RED", 0xff0000);
        Object object = traceContext.get(NAME_OBJECT, "RED");

        assertNotNull(object);

        Integer color = (Integer) object;
        assertEquals(0xff0000, (int) color);
    }
}
