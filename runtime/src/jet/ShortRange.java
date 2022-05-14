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


public final class ShortRange implements Range<Short>, Progression<Short> {
    public static final ShortRange EMPTY = new ShortRange((short) 1, (short) 0);

    private final short start;
    private final short end;

    public ShortRange(short start, short end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean contains(Short item) {
        return start <= item && item <= end;
    }

    public boolean contains(short item) {
        return start <= item && item <= end;
    }

    @Override
    public Short getStart() {
        return start;
    }

    @Override
    public Short getEnd() {
        return end;
    }

    @Override
    public Integer getIncrement() {
        return 1;
    }

    @Override
    public ShortIterator iterator() {
        return new ShortProgressionIterator(start, end, 1);
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

        ShortRange range = (ShortRange) o;
        return end == range.end && start == range.start;
    }

    @Override
    public int hashCode() {
        int result = (int) start;
        result = 31 * result + end;
        return result;
    }
}
