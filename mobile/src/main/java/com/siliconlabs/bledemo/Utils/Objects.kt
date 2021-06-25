/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.siliconlabs.bledemo.Utils

import java.util.*

/**
 * Utility methods for objects.
 *
 * @since 1.7
 */
object Objects {

    /**
     * Returns true if both arguments are null,
     * the result of [Arrays.equals] if both arguments are primitive arrays,
     * the result of [Arrays.deepEquals] if both arguments are arrays of reference types,
     * and the result of [.equals] otherwise.
     */

    fun deepEquals(a: Any?, b: Any?): Boolean {
        if (a == null || b == null) {
            return a === b
        } else if (a is Array<*> && b is Array<*>) {
            return Arrays.deepEquals(a as Array<Any?>?, b as Array<Any?>?)
        } else if (a is BooleanArray && b is BooleanArray) {
            return Arrays.equals(a as BooleanArray?, b as BooleanArray?)
        } else if (a is ByteArray && b is ByteArray) {
            return Arrays.equals(a as ByteArray?, b as ByteArray?)
        } else if (a is CharArray && b is CharArray) {
            return Arrays.equals(a as CharArray?, b as CharArray?)
        } else if (a is DoubleArray && b is DoubleArray) {
            return Arrays.equals(a as DoubleArray?, b as DoubleArray?)
        } else if (a is FloatArray && b is FloatArray) {
            return Arrays.equals(a as FloatArray?, b as FloatArray?)
        } else if (a is IntArray && b is IntArray) {
            return Arrays.equals(a as IntArray?, b as IntArray?)
        } else if (a is LongArray && b is LongArray) {
            return Arrays.equals(a as LongArray?, b as LongArray?)
        } else if (a is ShortArray && b is ShortArray) {
            return Arrays.equals(a as ShortArray?, b as ShortArray?)
        }
        return a == b
    }

    /**
     * Null-safe equivalent of `a.equals(b)`.
     */
    fun equals(a: Any?, b: Any?): Boolean {
        return if (a == null) b == null else a == b
    }

    /**
     * Convenience wrapper for [Arrays.hashCode], adding varargs.
     * This can be used to compute a hash code for an object's fields as follows:
     * `Objects.hash(a, b, c)`.
     */
    fun hash(vararg values: Any?): Int {
        return Arrays.hashCode(values)
    }


    /**
     * Returns "null" for null or `o.toString()`.
     */
    fun toString(o: Any?): String {
        return o?.toString() ?: "null"
    }

    /**
     * Returns `nullString` for null or `o.toString()`.
     */
    fun toString(o: Any?, nullString: String): String {
        return o?.toString() ?: nullString
    }
}