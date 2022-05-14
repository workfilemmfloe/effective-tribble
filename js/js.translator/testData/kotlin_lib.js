/**
 * Copyright 2010 Tim Down.
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

(function () {
    "use strict";

    // Shims for String
    String.prototype.startsWith = function (s) {
        return this.indexOf(s) === 0;
    };

    String.prototype.endsWith = function (s) {
        return this.indexOf(s, this.length - s.length) !== -1;
    };

    String.prototype.contains = function (s) {
        return this.indexOf(s) !== -1;
    };

    // Kotlin stdlib

    Kotlin.equals = function (obj1, obj2) {
        if (obj1 == null) {
            return obj2 == null;
        }

        if (Array.isArray(obj1)) {
            return Kotlin.arrayEquals(obj1, obj2);
        }

        if (typeof obj1 == "object" && obj1.equals_za3rmp$ !== undefined) {
            return obj1.equals_za3rmp$(obj2);
        }

        return obj1 === obj2;
    };

    Kotlin.hashCode = function (obj) {
        if (obj == null) {
            return 0;
        }
        if ("function" == typeof obj.hashCode) {
            return obj.hashCode();
        }
        var objType = typeof obj;
        if ("object" == objType || "function" == objType) {
            return getObjectHashCode(obj);
        } else if ("number" == objType) {
            // TODO: a more elaborate code is needed for floating point values.
            return obj | 0;
        } if ("boolean" == objType) {
            return Number(obj)
        }

        var str = String(obj);
        return getStringHashCode(str);
    };

    Kotlin.toString = function (o) {
        if (o == null) {
            return "null";
        }
        else if (Array.isArray(o)) {
            return Kotlin.arrayToString(o);
        }
        else {
            return o.toString();
        }
    };

    Kotlin.arrayToString = function (a) {
        return "[" + a.join(", ") + "]";
    };

    Kotlin.compareTo = function (a, b) {
        var typeA = typeof a;
        var typeB = typeof a;
        if (Kotlin.isChar(a) && typeB == "number") {
            return Kotlin.primitiveCompareTo(a.charCodeAt(0), b);
        }
        if (typeA == "number" && Kotlin.isChar(b)) {
            return Kotlin.primitiveCompareTo(a, b.charCodeAt(0));
        }
        if (typeA == "number" || typeA == "string") {
            return a < b ? -1 : a > b ? 1 : 0;
        }
        return a.compareTo_za3rmp$(b);
    };

    Kotlin.primitiveCompareTo = function (a, b) {
        return a < b ? -1 : a > b ? 1 : 0;
    };

    Kotlin.isNumber = function (a) {
        return typeof a == "number" || a instanceof Kotlin.Long;
    };

    Kotlin.isChar = function (value) {
        return (typeof value) == "string" && value.length == 1;
    };

    Kotlin.charInc = function (value) {
        return String.fromCharCode(value.charCodeAt(0)+1);
    };

    Kotlin.charDec = function (value) {
        return String.fromCharCode(value.charCodeAt(0)-1);
    };

    Kotlin.toShort = function (a) {
        return (a & 0xFFFF) << 16 >> 16;
    };

    Kotlin.toByte = function (a) {
        return (a & 0xFF) << 24 >> 24;
    };

    Kotlin.toChar = function (a) {
       return String.fromCharCode((((a | 0) % 65536) & 0xFFFF) << 16 >>> 16);
    };

    Kotlin.numberToLong = function (a) {
        return a instanceof Kotlin.Long ? a : Kotlin.Long.fromNumber(a);
    };

    Kotlin.numberToInt = function (a) {
        return a instanceof Kotlin.Long ? a.toInt() : (a | 0);
    };

    Kotlin.numberToShort = function (a) {
        return Kotlin.toShort(Kotlin.numberToInt(a));
    };

    Kotlin.numberToByte = function (a) {
        return Kotlin.toByte(Kotlin.numberToInt(a));
    };

    Kotlin.numberToDouble = function (a) {
        return +a;
    };

    Kotlin.numberToChar = function (a) {
        return Kotlin.toChar(Kotlin.numberToInt(a));
    };

    Kotlin.intUpto = function (from, to) {
        return new Kotlin.NumberRange(from, to);
    };

    Kotlin.intDownto = function (from, to) {
        return new Kotlin.Progression(from, to, -1);
    };

    Kotlin.Exception = Error;

    function createClassNowWithMessage(base) {
        return Kotlin.createClassNow(base,
                   /** @constructs */
                   function (message) {
                       this.message = (message !== undefined) ? message : null;
                   }
               );
    }

    Kotlin.RuntimeException = createClassNowWithMessage(Kotlin.Exception);
    Kotlin.NullPointerException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.NoSuchElementException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.IllegalArgumentException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.IllegalStateException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.UnsupportedOperationException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.IndexOutOfBoundsException = createClassNowWithMessage(Kotlin.RuntimeException);
    Kotlin.IOException = createClassNowWithMessage(Kotlin.Exception);

    Kotlin.throwNPE = function (message) {
        throw new Kotlin.NullPointerException(message);
    };

    function throwAbstractFunctionInvocationError(funName) {
        return function () {
            var message;
            if (funName !== undefined) {
                message = "Function " + funName + " is abstract";
            }
            else {
                message = "Function is abstract";
            }
            throw new TypeError(message);
        };
    }

    /** @const */
    var POW_2_32 = 4294967296;
    // TODO: consider switching to Symbol type once we are on ES6.
    /** @const */
    var OBJECT_HASH_CODE_PROPERTY_NAME = "kotlinHashCodeValue$";

    function getObjectHashCode(obj) {
        if (!(OBJECT_HASH_CODE_PROPERTY_NAME in obj)) {
            var hash = (Math.random() * POW_2_32) | 0; // Make 32-bit singed integer.
            Object.defineProperty(obj, OBJECT_HASH_CODE_PROPERTY_NAME, { value:  hash, enumerable: false });
        }
        return obj[OBJECT_HASH_CODE_PROPERTY_NAME];
    }

    function getStringHashCode(str) {
        var hash = 0;
        for (var i = 0; i < str.length; i++) {
            var code  = str.charCodeAt(i);
            hash  = (hash * 31 + code) | 0; // Keep it 32-bit.
        }
        return hash;
    }

    var lazyInitClasses = {};

    /**
     * @class
     * @implements {Kotlin.Iterator.<T>}
     *
     * @constructor
     * @param {Array.<T>} array
     * @template T
     */
    lazyInitClasses.ArrayIterator = Kotlin.createClass(
        function () {
            return [Kotlin.modules['stdlib'].kotlin.MutableIterator];
        },
        /** @constructs */
        function (array) {
            this.array = array;
            this.index = 0;
        },
        /** @lends {ArrayIterator.prototype} */
        {
            next: function () {
                return this.array[this.index++];
            },
            hasNext: function () {
                return this.index < this.array.length;
            },
            remove: function () {
                if (this.index < 0 || this.index > this.array.length) throw new RangeError();
                this.index--;
                this.array.splice(this.index, 1);
            }
    });

    /**
     * @class
     * @extends {ArrayIterator.<T>}
     *
     * @constructor
     * @param {Kotlin.AbstractList.<T>} list
     * @template T
     */
    lazyInitClasses.ListIterator = Kotlin.createClass(
        function () {
            return [Kotlin.modules['stdlib'].kotlin.Iterator];
        },
        /** @constructs */
        function (list) {
            this.list = list;
            this.size = list.size();
            this.index = 0;
        }, {
            next: function () {
                return this.list.get(this.index++);
            }
    });

    Kotlin.Enum = Kotlin.createClassNow(null,
        function () {
            this.name$ = undefined;
            this.ordinal$ = undefined;
        }, {
            name: function () {
                return this.name$;
            },
            ordinal: function () {
                return this.ordinal$;
            },
            toString: function () {
                return this.name();
            }
        }
    );

    Kotlin.PropertyMetadata = Kotlin.createClassNow(null,
        function (name) {
            this.name = name;
        }
    );

    lazyInitClasses.AbstractCollection = Kotlin.createClass(
        function () {
            return [Kotlin.modules['stdlib'].kotlin.MutableCollection];
        }, null, {
        addAll_4fm7v2$: function (collection) {
            var modified = false;
            var it = collection.iterator();
            while (it.hasNext()) {
                if (this.add_za3rmp$(it.next())) {
                    modified = true;
                }
            }
            return modified
        },
        removeAll_4fm7v2$: function (c) {
            var modified = false;
            var it = this.iterator();
            while (it.hasNext()) {
                if (c.contains_za3rmp$(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified
        },
        retainAll_4fm7v2$: function (c) {
            var modified = false;
            var it = this.iterator();
            while (it.hasNext()) {
                if (!c.contains_za3rmp$(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified
        },
        containsAll_4fm7v2$: function (c) {
            var it = c.iterator();
            while (it.hasNext()) {
                if (!this.contains_za3rmp$(it.next())) return false;
            }
            return true;
        },
        isEmpty: function () {
            return this.size() === 0;
        },
        iterator: function () {
            return new Kotlin.ArrayIterator(this.toArray());
        },
        equals_za3rmp$: function (o) {
            if (this.size() !== o.size()) return false;

            var iterator1 = this.iterator();
            var iterator2 = o.iterator();
            var i = this.size();
            while (i-- > 0) {
                if (!Kotlin.equals(iterator1.next(), iterator2.next())) {
                    return false;
                }
            }

            return true;
        },
        toString: function () {
            var builder = "[";
            var iterator = this.iterator();
            var first = true;
            var i = this.size();
            while (i-- > 0) {
                if (first) {
                    first = false;
                }
                else {
                    builder += ", ";
                }
                builder += iterator.next();
            }
            builder += "]";
            return builder;
        },
        toJSON: function () {
            return this.toArray();
        }
    });

    /**
     * @interface // actually it's abstract class
     * @template T
     */
    lazyInitClasses.AbstractList = Kotlin.createClass(
        function () {
            return [Kotlin.modules['stdlib'].kotlin.MutableList, Kotlin.AbstractCollection];
        }, null, {
        iterator: function () {
            return new Kotlin.ListIterator(this);
        },
        remove_za3rmp$: function (o) {
            var index = this.indexOf_za3rmp$(o);
            if (index !== -1) {
                this.remove_za3lpa$(index);
                return true;
            }
            return false;
        },
        contains_za3rmp$: function (o) {
            return this.indexOf_za3rmp$(o) !== -1;
        }
    });

    //TODO: should be JS Array-like (https://developer.mozilla.org/en-US/docs/JavaScript/Guide/Predefined_Core_Objects#Working_with_Array-like_objects)
    lazyInitClasses.ArrayList = Kotlin.createClass(
        function () {
            return [Kotlin.AbstractList];
        },
        function () {
            this.array = [];
        }, {
            get_za3lpa$: function (index) {
                this.checkRange(index);
                return this.array[index];
            },
            set_vux3hl$: function (index, value) {
                this.checkRange(index);
                this.array[index] = value;
            },
            size: function () {
                return this.array.length;
            },
            iterator: function () {
                return Kotlin.arrayIterator(this.array);
            },
            add_za3rmp$: function (element) {
                this.array.push(element);
                return true;
            },
            add_vux3hl$: function (index, element) {
                this.array.splice(index, 0, element);
            },
            addAll_4fm7v2$: function (collection) {
                var it = collection.iterator();
                for (var i = this.array.length, n = collection.size(); n-- > 0;) {
                    this.array[i++] = it.next();
                }
            },
            remove_za3lpa$: function (index) {
                this.checkRange(index);
                return this.array.splice(index, 1)[0];
            },
            clear: function () {
                this.array.length = 0;
            },
            indexOf_za3rmp$: function (o) {
                for (var i = 0; i < this.array.length; i++) {
                    if (Kotlin.equals(this.array[i], o)) {
                        return i;
                    }
                }
                return -1;
            },
            lastIndexOf_za3rmp$: function (o) {
                for (var i = this.array.length - 1; i >= 0; i--) {
                    if (Kotlin.equals(this.array[i], o)) {
                        return i;
                    }
                }
                return -1;
            },
            toArray: function () {
                return this.array.slice(0);
            },
            toString: function () {
                return "[" + this.array.join(", ") + "]";
            },
            toJSON: function () {
                return this.array;
            },
            checkRange: function (index) {
                if (index < 0 || index >= this.array.length) {
                    throw new Kotlin.IndexOutOfBoundsException();
                }
            }
        });

    Kotlin.Runnable = Kotlin.createClassNow(null, null, {
        run: throwAbstractFunctionInvocationError("Runnable#run")
    });

    Kotlin.Comparable = Kotlin.createClassNow(null, null, {
        compareTo: throwAbstractFunctionInvocationError("Comparable#compareTo")
    });

    Kotlin.Appendable = Kotlin.createClassNow(null, null, {
        append: throwAbstractFunctionInvocationError("Appendable#append")
    });

    Kotlin.Closeable = Kotlin.createClassNow(null, null, {
        close: throwAbstractFunctionInvocationError("Closeable#close")
    });

    Kotlin.safeParseInt = function (str) {
        var r = parseInt(str, 10);
        return isNaN(r) ? null : r;
    };

    Kotlin.safeParseDouble = function (str) {
        var r = parseFloat(str);
        return isNaN(r) ? null : r;
    };

    Kotlin.arrayEquals = function (a, b) {
        if (a === b) {
            return true;
        }
        if (!Array.isArray(b) || a.length !== b.length) {
            return false;
        }

        for (var i = 0, n = a.length; i < n; i++) {
            if (!Kotlin.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    };

    var BaseOutput = Kotlin.createClassNow(null, null, {
            println: function (a) {
                if (typeof a !== "undefined") this.print(a);
                this.print("\n");
            },
            flush: function () {
            }
        }
    );

    Kotlin.NodeJsOutput = Kotlin.createClassNow(BaseOutput,
        function(outputStream) {
            this.outputStream = outputStream;
        }, {
            print: function (a) {
                this.outputStream.write(a);
            }
        }
    );

    Kotlin.OutputToConsoleLog = Kotlin.createClassNow(BaseOutput, null, {
            print: function (a) {
                console.log(a);
            },
            println: function (a) {
                this.print(typeof a !== "undefined" ? a : "");
            }
        }
    );

    Kotlin.BufferedOutput = Kotlin.createClassNow(BaseOutput,
        function() {
            this.buffer = ""
        }, {
            print: function (a) {
                this.buffer += String(a);
            },
            flush: function () {
                this.buffer = "";
            }
        }
    );

    Kotlin.BufferedOutputToConsoleLog = Kotlin.createClassNow(Kotlin.BufferedOutput,
        function() {
            Kotlin.BufferedOutput.call(this);
        }, {
            print: function (a) {
                var s = String(a);

                var i = s.lastIndexOf("\n");
                if (i != -1) {
                    this.buffer += s.substr(0, i);

                    this.flush();

                    s = s.substr(i + 1);
                }

                this.buffer += s;
            },
            flush: function () {
                console.log(this.buffer);
                this.buffer = "";
            }
        }
    );
    Kotlin.out = function() {
        var isNode = typeof process !== 'undefined' && process.versions && !!process.versions.node;

        if (isNode) return new Kotlin.NodeJsOutput(process.stdout);

        return new Kotlin.BufferedOutputToConsoleLog();
    }();

    Kotlin.println = function (s) {
        Kotlin.out.println(s);
    };

    Kotlin.print = function (s) {
        Kotlin.out.print(s);
    };

    lazyInitClasses.RangeIterator = Kotlin.createClass(
        function () {
            return [Kotlin.modules['stdlib'].kotlin.Iterator];
        },
        function (start, end, increment) {
            this.start = start;
            this.end = end;
            this.increment = increment;
            this.i = start;
        }, {
            next: function () {
                var value = this.i;
                this.i = this.i + this.increment;
                return value;
            },
            hasNext: function () {
                if (this.increment > 0)
                    return this.i <= this.end;
                else
                    return this.i >= this.end;
            }
    });

    function isSameNotNullRanges(other) {
        var classObject = this.constructor;
        if (this instanceof classObject && other instanceof classObject) {
            return this.isEmpty() && other.isEmpty() ||
                (this.start === other.start && this.end === other.end && this.increment === other.increment);
        }
        return false;
    }

    Kotlin.NumberRange = Kotlin.createClassNow(null,
        function (start, end) {
            this.start = start;
            this.end = end;
            this.increment = 1;
        }, {
            contains: function (number) {
                return this.start <= number && number <= this.end;
            },
            iterator: function () {
                return new Kotlin.RangeIterator(this.start, this.end, this.increment);
            },
            isEmpty: function () {
                return this.start > this.end;
            },
            hashCode: function() {
                return this.isEmpty() ? -1 : (31 * this.start|0 + this.end|0);
            },
            equals_za3rmp$: isSameNotNullRanges
        }, {
            object_initializer$: function () {
                return { EMPTY : new this(1, 0) };
            }
    });

    Kotlin.NumberProgression = Kotlin.createClassNow(null,
        function (start, end, increment) {
            this.start = start;
            this.end = end;
            this.increment = increment;
        }, {
        iterator: function () {
            return new Kotlin.RangeIterator(this.start, this.end, this.increment);
        },
        isEmpty: function() {
            return this.increment > 0 ? this.start > this.end : this.start < this.end;
        },
        hashCode: function() {
            return this.isEmpty() ? -1 : (31 * (31 * this.start|0 + this.end|0) + this.increment|0);
        },
        equals_za3rmp$: isSameNotNullRanges
    });

    lazyInitClasses.LongRangeIterator = Kotlin.createClass(
        function () {
            return [Kotlin.modules['stdlib'].kotlin.Iterator];
        },
         function (start, end, increment) {
             this.start = start;
             this.end = end;
             this.increment = increment;
             this.i = start;
         }, {
             next: function () {
                 var value = this.i;
                 this.i = this.i.add(this.increment);
                 return value;
             },
             hasNext: function () {
                 if (this.increment.isNegative())
                     return this.i.compare(this.end) >= 0;
                 else
                     return this.i.compare(this.end) <= 0;
             }
         });

    Kotlin.LongRange = Kotlin.createClassNow(null,
       function (start, end) {
           this.start = start;
           this.end = end;
           this.increment = Kotlin.Long.ONE;
       }, {
           contains: function (number) {
               return this.start.compare(number) <= 0 && number.compare(this.end) <= 0;
           },
           iterator: function () {
               return new Kotlin.LongRangeIterator(this.start, this.end, this.increment);
           },
           isEmpty: function () {
               return this.start.compare(this.end) > 0;
           },
            hashCode: function() {
                return this.isEmpty() ? -1 : (31 * this.start.toInt() + this.end.toInt());
            },
            equals_za3rmp$: isSameNotNullRanges
       }, {
           object_initializer$: function () {
               return { EMPTY : new this(Kotlin.Long.ONE, Kotlin.Long.ZERO) };
           }
   });

    Kotlin.LongProgression = Kotlin.createClassNow(null,
         function (start, end, increment) {
             this.start = start;
             this.end = end;
             this.increment = increment;
         }, {
             iterator: function () {
                 return new Kotlin.LongRangeIterator(this.start, this.end, this.increment);
             },
             isEmpty: function() {
                 return this.increment.isNegative() ? this.start.compare(this.end) < 0 : this.start.compare(this.end) > 0;
             },
            hashCode: function() {
                return this.isEmpty() ? -1 : (31 * (31 * this.start.toInt() + this.end.toInt()) + this.increment.toInt());
            },
            equals_za3rmp$: isSameNotNullRanges
        });

    lazyInitClasses.CharRangeIterator = Kotlin.createClass(
        function () {
            return [Kotlin.RangeIterator];
        },
        function (start, end, increment) {
            Kotlin.RangeIterator.call(this, start, end, increment);
        }, {
            next: function () {
                var value = this.i;
                this.i = this.i + this.increment;
                return String.fromCharCode(value);
            }
    });

    Kotlin.CharRange = Kotlin.createClassNow(null,
        function (start, end) {
            this.start = start;
            this.startCode = start.charCodeAt(0);
            this.end = end;
            this.endCode = end.charCodeAt(0);
            this.increment = 1;
        }, {
            contains: function (char) {
                return this.start <= char && char <= this.end;
            },
            iterator: function () {
                return new Kotlin.CharRangeIterator(this.startCode, this.endCode, this.increment);
            },
            isEmpty: function () {
                return this.start > this.end;
            },
            hashCode: function() {
                return this.isEmpty() ? -1 : (31 * this.startCode|0 + this.endCode|0);
            },
            equals_za3rmp$: isSameNotNullRanges
        }, {
            object_initializer$: function () {
                return { EMPTY : new this(Kotlin.toChar(1), Kotlin.toChar(0)) };
            }
    });

    Kotlin.CharProgression = Kotlin.createClassNow(null,
        function (start, end, increment) {
            this.start = start;
            this.startCode = start.charCodeAt(0);
            this.end = end;
            this.endCode = end.charCodeAt(0);
            this.increment = increment;
        }, {
        iterator: function () {
            return new Kotlin.CharRangeIterator(this.startCode, this.endCode, this.increment);
        },
        isEmpty: function() {
            return this.increment > 0 ? this.start > this.end : this.start < this.end;
        },
        hashCode: function() {
            return this.isEmpty() ? -1 : (31 * (31 * this.startCode|0 + this.endCode|0) + this.increment|0);
        },
        equals_za3rmp$: isSameNotNullRanges
    });

    /**
     * @interface
     * @template T
     */
    Kotlin.Comparator = Kotlin.createClassNow(null, null, {
        compare: throwAbstractFunctionInvocationError("Comparator#compare")
    });

    /**
     * @class
     * @implements {Kotlin.Comparator.<T>}
     *
     * @constructor
     * @param {function(T,T): Boolean} comparator
     * @template T
     */
    var ComparatorImpl = Kotlin.createClassNow(Kotlin.Comparator,
        function (comparator) {
            this.compare = comparator;
        }
    );

    /**
     * @param {function(T,T): Boolean} f
     * @returns {Kotlin.Comparator.<T>}
     * @template T
     */
    Kotlin.comparator = function (f) {
        return new ComparatorImpl(f);
    };

    Kotlin.collectionsMax = function (c, comp) {
        if (c.isEmpty()) {
            //TODO: which exception?
            throw new Error();
        }
        var it = c.iterator();
        var max = it.next();
        while (it.hasNext()) {
            var el = it.next();
            if (comp.compare(max, el) < 0) {
                max = el;
            }
        }
        return max;
    };

    Kotlin.collectionsSort = function (mutableList, comparator) {
        var boundComparator = undefined;
        if (comparator !== undefined) {
            boundComparator = comparator.compare.bind(comparator);
        }

        if (mutableList instanceof Array) {
            mutableList.sort(boundComparator);
        }

        //TODO: should be deleted when List will be JS Array-like (https://developer.mozilla.org/en-US/docs/JavaScript/Guide/Predefined_Core_Objects#Working_with_Array-like_objects)
        var array = [];
        var it = mutableList.iterator();
        while (it.hasNext()) {
            array.push(it.next());
        }

        array.sort(boundComparator);

        for (var i = 0, n = array.length; i < n; i++) {
            mutableList.set_vux3hl$(i, array[i]);
        }
    };

    Kotlin.copyToArray = function (collection) {
        var array = [];
        var it = collection.iterator();
        while (it.hasNext()) {
            array.push(it.next());
        }

        return array;
    };


    Kotlin.StringBuilder = Kotlin.createClassNow(null,
        function () {
            this.string = "";
        }, {
        append: function (obj, from, to) {
            if (from == undefined && to == undefined) {
                this.string = this.string + obj.toString();
            } else if (to == undefined) {
                this.string = this.string + obj.toString().substring(from);
            } else {
                this.string = this.string + obj.toString().substring(from, to);
            }

            return this;
        },
        reverse: function () {
            this.string = this.string.split("").reverse().join("");
            return this;
        },
        toString: function () {
            return this.string;
        }
    });

    Kotlin.splitString = function (str, regex, limit) {
        return str.split(new RegExp(regex), limit);
    };

    Kotlin.nullArray = function (size) {
        var res = [];
        var i = size;
        while (i > 0) {
            res[--i] = null;
        }
        return res;
    };

    Kotlin.numberArrayOfSize = function (size) {
        return Kotlin.arrayFromFun(size, function () {
            return 0;
        });
    };

    Kotlin.charArrayOfSize = function (size) {
        return Kotlin.arrayFromFun(size, function () {
            return '\0';
        });
    };

    Kotlin.booleanArrayOfSize = function (size) {
        return Kotlin.arrayFromFun(size, function () {
            return false;
        });
    };

    Kotlin.longArrayOfSize = function (size) {
        return Kotlin.arrayFromFun(size, function () {
            return Kotlin.Long.ZERO;
        });
    };

    Kotlin.arrayFromFun = function (size, initFun) {
        var result = new Array(size);
        for (var i = 0; i < size; i++) {
            result[i] = initFun(i);
        }
        return result;
    };

    Kotlin.arrayIndices = function (arr) {
        return new Kotlin.NumberRange(0, arr.length - 1);
    };

    Kotlin.arrayIterator = function (array) {
        return new Kotlin.ArrayIterator(array);
    };

    Kotlin.jsonFromTuples = function (pairArr) {
        var i = pairArr.length;
        var res = {};
        while (i > 0) {
            --i;
            res[pairArr[i][0]] = pairArr[i][1];
        }
        return res;
    };

    Kotlin.jsonAddProperties = function (obj1, obj2) {
        for (var p in obj2) {
            if (obj2.hasOwnProperty(p)) {
                obj1[p] = obj2[p];
            }
        }
        return obj1;
    };

    Kotlin.createDefinition(lazyInitClasses, Kotlin);
})();
