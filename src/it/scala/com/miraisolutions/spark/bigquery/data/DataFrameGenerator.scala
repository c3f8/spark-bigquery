package com.miraisolutions.spark.bigquery.data

import java.sql.{Date, Timestamp}
import java.time._

import com.holdenkarau.spark.testing.RDDGenerator
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.apache.spark.sql.types._
import org.scalacheck.{Arbitrary, Gen}

/**
  * Generator of arbitrary Spark data frames used in property-based testing.
  */
object DataFrameGenerator {

  // Min and max BigQuery timestamp and date values
  // See https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types
  private val MIN_INSTANT = Instant.parse("0001-01-01T00:00:00.000000Z")
  private val MAX_INSTANT = Instant.parse("9999-12-31T23:59:59.999999Z")

  /**
    * Generates an arbitrary Spark data frame with the specified schema and minimum number of partitions.
    * @param sqlContext Spark SQL context
    * @param schema Schema of data frame to generate
    * @param minPartitions Minimum number of partitions
    * @return Arbitrary Spark data frame
    */
  def generate(sqlContext: SQLContext, schema: StructType, minPartitions: Int = 1): Arbitrary[DataFrame] = {
    val genRow = getRowGenerator(schema)
    val genDataFrame = RDDGenerator.genRDD[Row](sqlContext.sparkContext, minPartitions)(genRow)
    Arbitrary(genDataFrame.map(sqlContext.createDataFrame(_, schema)))
  }

  /**
    * Creates a generator of a row in a Spark data frame.
    * @param schema Schema of row to generate
    * @return Generator for a row
    */
  private def getRowGenerator(schema: StructType): Gen[Row] = {
    import scala.collection.JavaConverters._
    val fieldGenerators = schema.fields.map(field => getGeneratorForType(field.dataType))
    val rowGen = Gen.sequence(fieldGenerators)
    rowGen.map(values => Row.fromSeq(values.asScala))
  }

  /**
    * Creates a generator for a target data type.
    * @param dataType Data type
    * @return Generator of values of the specified data type
    */
  private def getGeneratorForType(dataType: DataType): Gen[Any] = {
    dataType match {
      case ByteType =>
        Arbitrary.arbitrary[Byte]

      case ShortType =>
        Arbitrary.arbitrary[Short]

      case IntegerType =>
        Arbitrary.arbitrary[Int]

      case LongType =>
        Arbitrary.arbitrary[Long]

      case FloatType =>
        Arbitrary.arbitrary[Float]

      case DoubleType =>
        Arbitrary.arbitrary[Double]

      case StringType =>
        Arbitrary.arbitrary[String]

      case BinaryType =>
        Arbitrary.arbitrary[Array[Byte]]

      case BooleanType =>
        Arbitrary.arbitrary[Boolean]

      case TimestampType =>
        // See https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types
        // BigQuery allowed timestamp range: [0001-01-1 00:00:00.000000, 9999-12-31 23:59:59.999999]
        Gen.chooseNum[Long](MIN_INSTANT.toEpochMilli, MAX_INSTANT.toEpochMilli).map(new Timestamp(_))

      case DateType =>
        // See https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types
        // BigQuery allowed date range: [0001-01-1, 9999-12-31]
        Gen.chooseNum[Long](MIN_INSTANT.toEpochMilli, MAX_INSTANT.toEpochMilli).map(new Date(_))

      case arr: ArrayType =>
        val elementGenerator = getGeneratorForType(arr.elementType)
        Gen.listOf(elementGenerator)

      case map: MapType =>
        val keyGenerator = getGeneratorForType(map.keyType)
        val valueGenerator = getGeneratorForType(map.valueType)
        val keyValueGenerator: Gen[(Any, Any)] = for {
          key <- keyGenerator
          value <- valueGenerator
        } yield (key, value)

        Gen.mapOf(keyValueGenerator)

      case row: StructType =>
        getRowGenerator(row)

      case _ =>
        throw new UnsupportedOperationException(s"Data type '$dataType' is not supported")
    }
  }
}
