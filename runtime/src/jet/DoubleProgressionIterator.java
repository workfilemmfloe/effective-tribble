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

class DoubleProgressionIterator extends DoubleIterator {
    private double next;
    private final double end;
    private final double increment;

    public DoubleProgressionIterator(double start, double end, double increment) {
        this.next = start;
        this.end = end;
        this.increment = increment;
    }

    @Override
    public boolean hasNext() {
        return increment > 0 ? next <= end : next >= end;
    }

    @Override
    public double nextDouble() {
        double value = next;
        next += increment;
        return value;
    }
}
