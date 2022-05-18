package kafka.iceberg

// Copy to run from Spark Shell ---start

import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, Row, SaveMode}
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.types.{LongType, StringType, StructField, StructType}
import org.apache.spark.sql.SparkSession
import org.apache.log4j.Logger
import org.apache.spark.storage.StorageLevel
import java.util.Date
import java.text.SimpleDateFormat
import org.apache.spark.sql.streaming._
import org.apache.spark.sql.streaming.StreamingQueryListener
import org.apache.spark.sql.streaming.StreamingQueryListener._
import java.util.concurrent.TimeUnit
// Copy to run from Spark Shell ---end 
/**
 * The file consumes messages pushed to Kinesis from DMS. The message content look like
 * {
 * "data": {
 * "LINE_ID": 144611,
 * "LINE_NUMBER": 1,
 * "ORDER_ID": 11363,
 * "PRODUCT_ID": 927,
 * "QUANTITY": 142,
 * "UNIT_PRICE": 36,
 * "DISCOUNT": 3,
 * "SUPPLY_COST": 15,
 * "TAX": 0,
 * "ORDER_DATE": "2015-10-17"
 * },
 * "metadata": {
 * "timestamp": "2021-11-19T13:24:43.297344Z",
 * "record-type": "data",
 * "operation": "update",
 * "partition-key-type": "schema-table",
 * "schema-name": "salesdb",
 * "table-name": "SALES_ORDER_DETAIL",
 * "transaction-id": 47330445004
 * }
 * }
 * The parameters expected are -
 * s3_bucket  Ex. <akshaya-firehose-test>
 * streamName Ex. <hudi-stream-ingest>
 * region Ex. <us-west-2>
 * tableType Ex. <COW/MOR>
 * hudiTableNamePrefix Ex. <hudi_trade_info>
 *
 */
object SparkKafkaConsumerIcebergProcessor {

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
    var s3_bucket = "akshaya-firehose-test"
    var kafkaBootstrap="ip-10-192-11-254.ap-south-1.compute.internal:9092"
    var topic = "data-stream-ingest-json"
    var tableName = "my_catalog.default.iceberg_sales_order_detail"
    var startingPosition = "LATEST"
    // Spark Shell ---end 
    if (!Option(args).isEmpty) {
      s3_bucket = args(0)
      kafkaBootstrap = args(1)
      topic = args(2)
      tableName = args(3)
      startingPosition = args(4)
    }


    val recordKey = "record_key"
    val checkpoint_path = s"s3://$s3_bucket/kafka-stream-data-checkpoint/iceberg/$tableName/"
    val tablePartitionKey = "record_partition_key"

    println("s3_bucket:" + s3_bucket)
    println("kafkaBootstrap:" + kafkaBootstrap)
    println("topic:" + topic)
    println("tableName:" + tableName)
    println("recordKey:" + recordKey)
    println("checkpoint_path:" + checkpoint_path)

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
      .readStream.format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrap)
      .option("subscribe", topic)
      .option("startingOffsets", startingPosition).load())


    val decimalType = DataTypes.createDecimalType(38, 10)
    val dataSchema = StructType(Array(
      StructField("LINE_ID", IntegerType, true),
      StructField("LINE_NUMBER", IntegerType, true),
      StructField("ORDER_ID", IntegerType, true),
      StructField("PRODUCT_ID", IntegerType, true),
      StructField("QUANTITY", IntegerType, true),
      StructField("UNIT_PRICE", decimalType, true),
      StructField("DISCOUNT", decimalType, true),
      StructField("SUPPLY_COST", decimalType, true),
      StructField("TAX", decimalType, true),
      StructField("ORDER_DATE", DateType, true)
    ))


    val jsonDF=(streamingInputDF
      .selectExpr("CAST(value AS STRING)").as[(String)]
      .withColumn("jsonData",from_json(col("value"),dataSchema))
      .select(col("jsonData.*")))

    jsonDF.printSchema()


    var parDF = jsonDF
    parDF = parDF.select(parDF.columns.map(x => col(x).as(x.toLowerCase)): _*)
    parDF = parDF.filter(parDF.col("order_date").isNotNull)
    parDF.printSchema()
    parDF = parDF.withColumn(recordKey, concat(col("order_id"), lit("#"), col("line_id")))
    parDF = parDF.withColumn("order_date", parDF("order_date").cast(DateType))
    parDF = parDF.withColumn("year", year($"order_date").cast(StringType)).withColumn("month", month($"order_date").cast(StringType))
    parDF = parDF.withColumn(tablePartitionKey, concat(lit("year="), $"year", lit("/month="), $"month"))
    parDF.printSchema()

    val query = (parDF.writeStream
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
