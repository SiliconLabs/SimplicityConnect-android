/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.siliconlabs.bledemo.features.demo.channel_sounding.filters

import java.util.Locale

/**
 * A simple matrix class for Kalman filter operations.
 * Based on Android Open Source Project's Matrix implementation for Kalman filtering.
 *
 * @param rows Number of rows
 * @param cols Number of columns
 */
class Matrix(val rows: Int, val cols: Int) {

    private val data: Array<DoubleArray> = Array(rows) { DoubleArray(cols) }

    /**
     * Get element at position (i, j)
     */
    operator fun get(i: Int, j: Int): Double = data[i][j]

    /**
     * Set element at position (i, j)
     */
    operator fun set(i: Int, j: Int, value: Double) {
        data[i][j] = value
    }

    /**
     * Matrix multiplication: this * other
     */
    fun dot(other: Matrix): Matrix {
        require(cols == other.rows) { "Matrix dimensions don't match for multiplication" }
        val result = Matrix(rows, other.cols)
        for (i in 0 until rows) {
            for (j in 0 until other.cols) {
                var sum = 0.0
                for (k in 0 until cols) {
                    sum += data[i][k] * other.data[k][j]
                }
                result.data[i][j] = sum
            }
        }
        return result
    }

    /**
     * Multiply this matrix by the transpose of other: this * other^T
     */
    fun dotTranspose(other: Matrix): Matrix {
        require(cols == other.cols) { "Matrix dimensions don't match for dotTranspose" }
        val result = Matrix(rows, other.rows)
        for (i in 0 until rows) {
            for (j in 0 until other.rows) {
                var sum = 0.0
                for (k in 0 until cols) {
                    sum += data[i][k] * other.data[j][k]
                }
                result.data[i][j] = sum
            }
        }
        return result
    }

    /**
     * Matrix addition: this + other
     */
    fun plus(other: Matrix): Matrix {
        require(rows == other.rows && cols == other.cols) { "Matrix dimensions must match for addition" }
        val result = Matrix(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result.data[i][j] = data[i][j] + other.data[i][j]
            }
        }
        return result
    }

    /**
     * Matrix subtraction: this - other
     */
    fun minus(other: Matrix): Matrix {
        require(rows == other.rows && cols == other.cols) { "Matrix dimensions must match for subtraction" }
        val result = Matrix(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result.data[i][j] = data[i][j] - other.data[i][j]
            }
        }
        return result
    }

    /**
     * Transpose this matrix
     */
    fun transpose(): Matrix {
        val result = Matrix(cols, rows)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result.data[j][i] = data[i][j]
            }
        }
        return result
    }

    /**
     * Matrix inverse (for 1x1 and 2x2 matrices)
     * For larger matrices, a more sophisticated algorithm would be needed.
     */
    fun inverse(): Matrix {
        require(rows == cols) { "Matrix must be square for inversion" }

        return when (rows) {
            1 -> {
                val result = Matrix(1, 1)
                result[0, 0] = 1.0 / data[0][0]
                result
            }
            2 -> {
                val det = data[0][0] * data[1][1] - data[0][1] * data[1][0]
                require(det != 0.0) { "Matrix is singular, cannot invert" }
                val result = Matrix(2, 2)
                result[0, 0] = data[1][1] / det
                result[0, 1] = -data[0][1] / det
                result[1, 0] = -data[1][0] / det
                result[1, 1] = data[0][0] / det
                result
            }
            else -> {
                // For larger matrices, use Gaussian elimination or LU decomposition
                // For now, only support 1x1 and 2x2 which is sufficient for our Kalman filter
                throw UnsupportedOperationException("Inverse only supported for 1x1 and 2x2 matrices")
            }
        }
    }

    /**
     * Scalar multiplication
     */
    fun times(scalar: Double): Matrix {
        val result = Matrix(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result.data[i][j] = data[i][j] * scalar
            }
        }
        return result
    }

    /**
     * Create a copy of this matrix
     */
    fun copy(): Matrix {
        val result = Matrix(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result.data[i][j] = data[i][j]
            }
        }
        return result
    }

    override fun toString(): String {
        val sb = StringBuilder("[")
        for (i in 0 until rows) {
            if (i > 0) sb.append(", ")
            sb.append("[")
            for (j in 0 until cols) {
                if (j > 0) sb.append(", ")
                sb.append(String.format(Locale.US, "%.6f", data[i][j]))
            }
            sb.append("]")
        }
        sb.append("]")
        return sb.toString()
    }

    companion object {
        /**
         * Create an identity matrix of size n x n
         */
        fun identity(n: Int): Matrix {
            val result = Matrix(n, n)
            for (i in 0 until n) {
                result[i, i] = 1.0
            }
            return result
        }

        /**
         * Create a matrix from a 2D array
         */
        fun fromArray(data: Array<DoubleArray>): Matrix {
            val rows = data.size
            val cols = if (rows > 0) data[0].size else 0
            val result = Matrix(rows, cols)
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    result[i, j] = data[i][j]
                }
            }
            return result
        }

        /**
         * Create a column vector from an array
         */
        fun columnVector(data: DoubleArray): Matrix {
            val result = Matrix(data.size, 1)
            for (i in data.indices) {
                result[i, 0] = data[i]
            }
            return result
        }

        /**
         * Create a row vector from an array
         */
        fun rowVector(data: DoubleArray): Matrix {
            val result = Matrix(1, data.size)
            for (i in data.indices) {
                result[0, i] = data[i]
            }
            return result
        }
    }
}

