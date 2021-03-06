/**
 * Copyright (c) 2015, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */

package com.cloudera.sparkts

import breeze.linalg._
import breeze.stats._

object UnivariateTimeSeries {
  def autocorr(ts: Array[Double], numLags: Int): Array[Double] = {
    autocorr(new DenseVector(ts), numLags).toDenseVector.data
  }

  /**
   * Computes the sample autocorrelation of the given series.
   */
  def autocorr(ts: Vector[Double], numLags: Int): Vector[Double] = {
    val corrs = new Array[Double](numLags)
    var i = 1
    while (i <= numLags) {
      val slice1 = ts(i until ts.length)
      val slice2 = ts(0 until ts.length - i)
      val mean1 = mean(slice1)
      val mean2 = mean(slice2)
      var variance1 = 0.0
      var variance2 = 0.0
      var covariance = 0.0
      var j = 0
      while (j < ts.length - i) {
        val diff1 = slice1(j) - mean1
        val diff2 = slice2(j) - mean2
        variance1 += diff1 * diff1
        variance2 += diff2 * diff2
        covariance += diff1 * diff2
        j += 1
      }

      corrs(i - 1) = covariance / (math.sqrt(variance1) * math.sqrt(variance2))
      i += 1
    }
    new DenseVector[Double](corrs)
  }

  /**
   * Trim leading NaNs from a series.
   */
  def trimLeading(ts: Vector[Double]): Vector[Double] = {
    val start = firstNotNaN(ts)
    if (start < ts.length) {
      ts(start until ts.length)
    } else {
      DenseVector.zeros[Double](0)
    }
  }

  /**
   * Trim trailing NaNs from a series.
   */
  def trimTrailing(ts: Vector[Double]): Vector[Double] = {
    val end = lastNotNaN(ts)
    if (end > 0) {
      ts(0 until end)
    } else {
      DenseVector.zeros[Double](0)
    }
  }

  def firstNotNaN(ts: Vector[Double]): Int = {
    var i = 0
    while (i < ts.length) {
      if (!java.lang.Double.isNaN(ts(i))) {
        return i
      }
      i += 1
    }
    i
  }

  def lastNotNaN(ts: Vector[Double]): Int = {
    var i = ts.length - 1
    while (i >= 0) {
      if (!java.lang.Double.isNaN(ts(i))) {
        return i
      }
      i -= 1
    }
    i
  }

  def fillts(ts: Vector[Double], fillMethod: String): Vector[Double] = {
    fillMethod match {
      case "linear" => fillLinear(ts)
      case "nearest" => fillNearest(ts)
      case _ => throw new UnsupportedOperationException()
    }
  }

  def fillNearest(values: Array[Double]): Array[Double] = {
    fillNearest(new DenseVector(values)).data
  }

  def fillNearest(values: Vector[Double]): DenseVector[Double] = {
    val result = new DenseVector(values.toArray)
    var lastExisting = -1
    var nextExisting = -1
    var i = 1
    while (i < result.length) {
      if (result(i).isNaN) {
        if (nextExisting < i) {
          nextExisting = i + 1
          while (nextExisting < result.length && result(nextExisting).isNaN) {
            nextExisting += 1
          }
        }

        if (lastExisting < 0 && nextExisting >= result.length) {
          throw new IllegalArgumentException("Input is all NaNs!")
        } else if (nextExisting >= result.length || // TODO: check this
          (lastExisting >= 0 && i - lastExisting < nextExisting - i)) {
          result(i) = result(lastExisting)
        } else {
          result(i) = result(nextExisting)
        }
      } else {
        lastExisting = i
      }
      i += 1
    }
    result
  }

  def fillNext(values: Array[Double]): Array[Double] = {
    fillNext(new DenseVector(values)).data
  }

  def fillNext(values: Vector[Double]): DenseVector[Double] = {
    throw new UnsupportedOperationException()
  }

  def fillPrevious(values: Array[Double]): Array[Double] = {
    fillPrevious(new DenseVector(values)).data
  }

  def fillPrevious(values: Vector[Double]): DenseVector[Double] = {
    throw new UnsupportedOperationException()
  }

  def fillLinear(values: Array[Double]): Array[Double] = {
    fillLinear(new DenseVector(values)).data
  }

  def fillLinear(values: Vector[Double]): DenseVector[Double] = {
    val result = new DenseVector(values.toArray)
    var i = 1
    while (i < result.length - 1) {
      val rangeStart = i
      while (i < result.length - 1 && result(i).isNaN) {
        i += 1
      }
      val before = result(rangeStart - 1)
      val after = result(i)
      if (i != rangeStart && !before.isNaN && !after.isNaN) {
        val increment = (after - before) / (i - (rangeStart - 1))
        for (j <- rangeStart until i) {
          result(j) = result(j - 1) + increment
        }
      }
      i += 1
    }
    result
  }

  def ar(values: Vector[Double], maxLag: Int): ARModel = Autoregression.fitModel(values, maxLag)
}
