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

package jet;


public final class FloatRange implements Range<Float>, Progression<Float> {
    public static final FloatRange EMPTY = new FloatRange(1, 0);

    private final float start;
    private final float end;

    public FloatRange(float start, float end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean contains(Float item) {
        return start <= item && item <= end;
    }

    public boolean contains(float item) {
        return start <= item && item <= end;
    }

    @Override
    public Float getStart() {
        return start;
    }

    @Override
    public Float getEnd() {
        return end;
    }

    @Override
    public Float getIncrement() {
        return 1.0f;
    }

    @Override
    public FloatIterator iterator() {
        return new FloatProgressionIterator(start, end, 1);
    }

    @Override
    public String toString() {
        return start + ".." + end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FloatRange range = (FloatRange) o;

        return Float.compare(range.end, end) == 0 && Float.compare(range.start, start) == 0;
    }

    @Override
    public int hashCode() {
        int result = (start != +0.0f ? Float.floatToIntBits(start) : 0);
        result = 31 * result + (end != +0.0f ? Float.floatToIntBits(end) : 0);
        return result;
    }
}
