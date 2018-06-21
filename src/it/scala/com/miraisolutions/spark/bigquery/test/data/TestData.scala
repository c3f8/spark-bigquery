/*
 * Copyright (c) 2018 Mirai Solutions GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.miraisolutions.spark.bigquery.test.data

import org.apache.spark.sql.types._

private[bigquery] object TestData {

  private val atomicTypes: Set[DataType] = Set(BooleanType, ByteType, ShortType, IntegerType, LongType, FloatType,
    DoubleType, StringType, BinaryType, TimestampType, DateType, DataTypes.createDecimalType(38, 9),
    DataTypes.createDecimalType(12, 4), DataTypes.createDecimalType(33, 4),
    DataTypes.createDecimalType(7,7))

  private def createName(dt: DataType, nullable: Boolean): String = {
    val base = dt.simpleString.replaceAll("[^A-Za-z]+", "")
    if(nullable) {
      base + "0"
    } else {
      base
    }
  }

  val atomicFields: Set[StructField] = {
    for {
      dt <- atomicTypes
      nullable <- Set(true, false)
    } yield StructField(createName(dt, nullable), dt, nullable)
  }

  private def createName(dt: ArrayType, nullable: Boolean): String = {
    val baseType = createName(dt.elementType, dt.containsNull)
    val array = if(nullable) "array0" else "array"
    s"${array}_${baseType}_"
  }

  private val arrayTypes: Set[ArrayType] = {
    for {
      bt <- atomicTypes.filter {
        // TODO: We ignore arrays of decimals with precision < 38 for now since they cannot easily be cast;
        // may use higher-order functions in the future: https://issues.apache.org/jira/browse/SPARK-19480
        case dt: DecimalType if dt.precision < 38 => false
        case BinaryType => false
        case _ => true
      }
      containsNull <- Set(true, false)
    } yield ArrayType(bt, containsNull)
  }

  val arrayFields: Set[StructField] = {
    for {
      at <- arrayTypes
      nullable <- Set(true, false)
    } yield StructField(createName(at, nullable), at, nullable)
  }
}
