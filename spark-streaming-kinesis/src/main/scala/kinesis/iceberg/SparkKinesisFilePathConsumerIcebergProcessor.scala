package kinesis.iceberg

// Copy to run from Spark Shell ---start
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
// Copy to run from Spark Shell ---end 
/**
 * The file consumes messages that contains S3 path to CDC pushed to S3 from DMS. The message content look like
 * {
 * "filePath": "s3://<bucket-name>/dms-full-load-path/salesdb/SALES_ORDER_DETAIL/20211118-100428844.parquet"
 * }
 * The parameters expected are -
 * s3_bucket  Ex. <akshaya-firehose-test>
 * streamName Ex. <hudi-stream-ingest>
 * region Ex. <us-west-2>
 * tableType Ex. <COW/MOR>
 * hudiTableNamePrefix Ex. <hudi_trade_info>
 *
 */
object SparkKinesisFilePathConsumerIcebergProcessor {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder
      .appName("SparkHudi")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .config("spark.default.parallelism", 9)
      .config("spark.sql.shuffle.partitions", 9)
      .enableHiveSupport()
      .getOrCreate()
    // Copy to run from Spark Shell ---start
    import spark.implicits._
    import spark.sql
    // For Spark Shell -- hardcode these parameters
    val s3_bucket = "akshaya-firehose-test"
    val streamName = "hudi-stream-ingest"
    val region = "ap-south-1"
    val tableName = "iceberg_trade_info"
    val tablePartitionKey = "record_partition_key"

    if (!Option(args).isEmpty) {
      val s3_bucket = args(0)
      val streamName = args(1)
      val region = args(2)
      val tableName = args(3)
    }


    val recordKey = "record_key"
    val checkpoint_path = s"s3://$s3_bucket/kinesis-stream-data-checkpoint/iceberg/$tableName/"
    val endpointUrl = s"https://kinesis.$region.amazonaws.com"

    println("s3_bucket:" + s3_bucket)
    println("streamName:" + streamName)
    println("region:" + region)
    println("tableName:" + tableName)
    println("recordKey:" + recordKey)
    println("checkpoint_path:" + checkpoint_path)
    println("endpointUrl:" + endpointUrl)

    val creatTableString =
      s"""CREATE TABLE IF NOT EXISTS $tableName
              (  line_id int,  
                  line_number int, 
                  order_id int, 
                  product_id int, 
                  quantity int, 
                  unit_price decimal(38, 10),    
                  discount decimal(38, 10), 
                  supply_cost decimal(38, 10), 
                  tax decimal(38, 10) , 
                  order_date date, 
                  record_key string, 
                  year string,
                  month string,
                  record_partition_key string
              )    
              USING iceberg    
              PARTITIONED BY (year,month)"""

    val table = sql(creatTableString)
    table.printSchema()

    val streamingInputDF = (spark
      .readStream.format("kinesis")
      .option("streamName", streamName)
      .option("startingposition", "TRIM_HORIZON")
      .option("endpointUrl", endpointUrl)
      .load())


    val schema = StructType(Array(StructField("filePath", StringType, true)))


    val jsonDF = (streamingInputDF.selectExpr("CAST(data AS STRING)").as[(String)]
      .withColumn("jsonData", from_json(col("data"), schema))
      .select(col("jsonData.filePath")))


    jsonDF.printSchema()
    val query = (jsonDF.writeStream.foreachBatch { (batchDF: DataFrame, _: Long) => {
      print(batchDF)
      batchDF.collect().foreach(s => {
        print(s)
        var parDF = spark.read.format("parquet").load(s.getString(0));
        parDF = parDF.drop("Op")
        parDF = parDF.select(parDF.columns.map(x => col(x).as(x.toLowerCase)): _*)
        parDF = parDF.withColumn(recordKey, concat(col("order_id"), lit("#"), col("line_id")))
        parDF = parDF.withColumn("order_date", parDF("order_date").cast(DateType))
        parDF = parDF.withColumn("year", year($"order_date").cast(StringType)).withColumn("month", month($"order_date").cast(StringType))
        parDF = parDF.withColumn(tablePartitionKey, concat(lit("year="), $"year", lit("/month="), $"month"))
        parDF.printSchema()
        parDF.select("month", "record_key", "quantity").show()

        parDF.write.format("iceberg")
          .mode("append")
          .save(tableName);
      })
    }
    }.option("checkpointLocation", checkpoint_path).start())
    // Copy to run from Spark Shell ---end

    query.awaitTermination()

  }

}
