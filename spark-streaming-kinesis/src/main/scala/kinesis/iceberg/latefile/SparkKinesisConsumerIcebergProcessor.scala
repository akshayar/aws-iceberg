package kinesis.iceberg.latefile

// Spark Shell ---start

import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, Row, SaveMode}
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.types.{LongType, StringType, StructField, StructType}
import org.apache.spark.sql.SparkSession
import org.apache.log4j.Logger
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.kinesis.KinesisInputDStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kinesis.KinesisInitialPositions
import java.util.Date
import java.text.SimpleDateFormat
import org.apache.spark.sql.streaming._
import org.apache.spark.sql.streaming.StreamingQueryListener
import org.apache.spark.sql.streaming.StreamingQueryListener._
import java.util.concurrent.TimeUnit
// Spark Shell ---end 
/**
 * The file consumes messages pushed to Kinesis. The message content look like
 * {
 * "tradeId":"211124204181756",
 * "symbol":"GOOGL",
 * "quantity":"39",
 * "price":"39",
 * "timestamp":1637766663,
 * "description":"Traded on Wed Nov 24 20:41:03 IST 2021",
 * "traderName":"GOOGL trader",
 * "traderFirm":"GOOGL firm"
 * }
 * The parameters expected are -
 * s3_bucket  Ex. <akshaya-firehose-test>
 * streamName Ex. <hudi-stream-ingest>
 * region Ex. <us-west-2>
 * tableNamePrefix Ex. <hudi_trade_info>
 *
 */
object SparkKinesisConsumerIcebergProcessor {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder
      .appName("SparkHudi")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .config("spark.default.parallelism", 9)
      .config("spark.sql.shuffle.partitions", 9)
      .enableHiveSupport()
      .getOrCreate()

    // Spark Shell ---start
    import spark.implicits._
    import spark.sql
    spark.conf.set("spark.sql.streaming.metricsEnabled", "true")
    // Spark Shell -- hardcode these parameters
    var s3_bucket = "akshaya-firehose-test"
    var streamName = "hudi-stream-ingest"
    var region = "ap-south-1"
    var tableName = "my_catalog.default.iceberg_trade_info_simulated"
    // Spark Shell ---end 
    if (!Option(args).isEmpty) {
      s3_bucket = args(0)
      streamName = args(1)
      region = args(2)
      tableName = args(3)
    }

    // Spark Shell ---start 
    val endpointUrl = s"https://kinesis.$region.amazonaws.com"
    val checkpoint_path = s"s3://$s3_bucket/kinesis-stream-data-checkpoint/iceberg/$tableName/"
    val recordKey = "record_key"

    println("s3_bucket:" + s3_bucket)
    println("streamName:" + streamName)
    println("region:" + region)
    println("tableName:" + tableName)
    println("recordKey:" + recordKey)
    println("checkpoint_path:" + checkpoint_path)
    println("endpointUrl:" + endpointUrl)

    val creatTableString =
      s"""CREATE TABLE IF NOT EXISTS $tableName
              (  tradeid string,  
                  symbol string, 
                  quantity string, 
                  price string, 
                  timestamp string, 
                  description string,    
                  tradername string, 
                  traderfirm String, 
                  record_key string , 
                  trade_datetime string, 
                  day string, 
                  hour string
              )    
              USING iceberg    
              PARTITIONED BY (day,hour)"""

    val table = sql(creatTableString)
    table.printSchema()

    val streamingInputDF = (spark
      .readStream.format("kinesis")
      .option("streamName", streamName)
      .option("startingposition", "TRIM_HORIZON")
      .option("endpointUrl", endpointUrl)
      .load())

    val decimalType = DataTypes.createDecimalType(38, 10)
    val dataSchema = StructType(Array(
      StructField("tradeId", StringType, true),
      StructField("symbol", StringType, true),
      StructField("quantity", StringType, true),
      StructField("price", StringType, true),
      StructField("timestamp", StringType, true),
      StructField("description", StringType, true),
      StructField("traderName", StringType, true),
      StructField("traderFirm", StringType, true)
    ))
    var jsonDF = (streamingInputDF.selectExpr("CAST(data AS STRING)").as[(String)]
      .withColumn("jsonData", from_json(col("data"), dataSchema))
      .select(col("jsonData.*")))


    jsonDF.printSchema()


    jsonDF = jsonDF.select(jsonDF.columns.map(x => col(x).as(x.toLowerCase)): _*)
    jsonDF = jsonDF.filter(jsonDF.col("tradeid").isNotNull)
    jsonDF = jsonDF.withColumn(recordKey, concat(col("tradeid"), lit("#"), col("timestamp")))
    jsonDF = jsonDF.withColumn("trade_datetime", from_unixtime(jsonDF.col("timestamp")))
    jsonDF = jsonDF.withColumn("day", dayofmonth($"trade_datetime").cast(StringType)).withColumn("hour", hour($"trade_datetime").cast(StringType))
    jsonDF.printSchema()


    val query = (jsonDF.writeStream
      .format("iceberg")
      .outputMode("append")
      .trigger(Trigger.ProcessingTime(1, TimeUnit.MINUTES))
      .option("path", tableName)
      .option("fanout-enabled", "true")
      .option("checkpointLocation", checkpoint_path)
      .start())

    spark.streams.addListener(new StreamingQueryListener() {
      override def onQueryStarted(queryStarted: QueryStartedEvent): Unit = {
        println("Query started: " + queryStarted.id)
      }

      override def onQueryTerminated(queryTerminated: QueryTerminatedEvent): Unit = {
        println("Query terminated: " + queryTerminated.id)
      }

      override def onQueryProgress(queryProgress: QueryProgressEvent): Unit = {
        println("Query made progress: " + queryProgress.progress)
      }
    })

    // Spark Shell ---end
    query.awaitTermination()

  }

}
