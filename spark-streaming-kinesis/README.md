
# Prerequisites
## EMR Prerequisites
1. Create EMR cluster with Spark, Hive and Hadoop enabled. Refer EMRSparkHudiCluster in the template at [cloudformation/hudi-workshop-emr-spark.yaml](../cloudformation/hudi-workshop-emr-spark.yaml).
2. SSH to master node and execute command to update log level to [log4j.rootCategory=WARN,console] --this is an optional step 

```
vi /etc/spark/conf/log4j.properties 

```
3. Ensure that EMR role has permission on Kinesis and S3. 
## Spark Submit Prerequisite
1.  Build Environment
```
java --version
openjdk 15.0.2 2021-01-19
OpenJDK Runtime Environment Corretto-15.0.2.7.1 (build 15.0.2+7)
OpenJDK 64-Bit Server VM Corretto-15.0.2.7.1 (build 15.0.2+7, mixed mode, sharing)

sbt --version
sbt version in this project: 1.5.5
sbt script version: 1.5.5

```

3. Build and copy jar by running spark-streaming-kinesis/build.sh. 
```
./build.sh <S3-Bucket-Name>
```

2. SSH to master node and copy jar which was pushed to S3.
    
```
   aws s3 cp s3://<S3-Bucket-Name>/spark-structured-streaming-kinesis-iceberg_2.12-1.0.jar .   
```

# Use Case 1 - Events Published to Kinesis with simulation of late arriving events
## Message Content pushed to the topic
timestamp has epoch value in seconds. 

```
{
   "tradeId":"211124204181756",
   "symbol":"GOOGL",
   "quantity":"39",
   "price":"39",
   "timestamp":1637766663,
   "description":"Traded on Wed Nov 24 20:41:03 IST 2021",
   "traderName":"GOOGL trader",
   "traderFirm":"GOOGL firm"
}

```
## Spark Scala Code
[kinesis.iceberg.latefile.SparkKinesisConsumerIcebergProcessor](src/main/scala/kinesis/iceberg/latefile/SparkKinesisConsumerIcebergProcessor.scala)

## Spark Submit 
SSH to master node and then run the spark submit command.

```
spark-submit \
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
--conf spark.sql.catalog.my_catalog=org.apache.iceberg.spark.SparkCatalog    \
--conf spark.sql.catalog.my_catalog.warehouse=s3://akshaya-firehose-test/iceberg \
--conf spark.sql.catalog.my_catalog.catalog-impl=org.apache.iceberg.aws.glue.GlueCatalog \
--conf spark.sql.catalog.my_catalog.io-impl=org.apache.iceberg.aws.s3.S3FileIO \
--conf spark.sql.catalog.my_catalog.lock-impl=org.apache.iceberg.aws.glue.DynamoLockManager \
--conf spark.sql.catalog.my_catalog.lock.table=myGlueLockTable \
--packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,\
org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0,\
software.amazon.awssdk:bundle:2.15.40,software.amazon.awssdk:url-connection-client:2.15.40 \
--class kinesis.iceberg.latefile.SparkKinesisConsumerIcebergProcessor spark-structured-streaming-kinesis-iceberg_2.12-1.0.jar \
<bucket-name> <kinesis-stream-name> <kineis-region> <table-name>


```
Example
```
spark-submit \
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
--conf spark.sql.catalog.my_catalog=org.apache.iceberg.spark.SparkCatalog    \
--conf spark.sql.catalog.my_catalog.warehouse=s3://akshaya-firehose-test/iceberg \
--conf spark.sql.catalog.my_catalog.catalog-impl=org.apache.iceberg.aws.glue.GlueCatalog \
--conf spark.sql.catalog.my_catalog.io-impl=org.apache.iceberg.aws.s3.S3FileIO \
--conf spark.sql.catalog.my_catalog.lock-impl=org.apache.iceberg.aws.glue.DynamoLockManager \
--conf spark.sql.catalog.my_catalog.lock.table=myGlueLockTable \
--packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,\
org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0,\
software.amazon.awssdk:bundle:2.15.40,software.amazon.awssdk:url-connection-client:2.15.40 \
--class kinesis.iceberg.latefile.SparkKinesisConsumerIcebergProcessor spark-structured-streaming-kinesis-iceberg_2.12-1.0.jar \
akshaya-firehose-test data-stream-ingest ap-south-1 my_catalog.iceberg.iceberg_trade_info_simulated
	
```

## Spark Shell
Run the shell with command below and copy paste code from   [kinesis.iceberg.latefile.SparkKinesisConsumerIcebergProcessor](src/main/scala/kinesis/iceberg/latefile/SparkKinesisConsumerIcebergProcessor.scala)
. The code that needs to be copied is between  (Spark Shell ---Start ) and (Spark Shell ---End ). Also ensure that the you hard code the paremeters like s3_bucket, streamName, region ,tableType and hudiTableNamePrefix.  

```
spark-shell
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
--conf spark.sql.catalog.my_catalog=org.apache.iceberg.spark.SparkCatalog    \
--conf spark.sql.catalog.my_catalog.warehouse=s3://akshaya-firehose-test/iceberg \
--conf spark.sql.catalog.my_catalog.catalog-impl=org.apache.iceberg.aws.glue.GlueCatalog \
--conf spark.sql.catalog.my_catalog.io-impl=org.apache.iceberg.aws.s3.S3FileIO \
--conf spark.sql.catalog.my_catalog.lock-impl=org.apache.iceberg.aws.glue.DynamoLockManager \
--conf spark.sql.catalog.my_catalog.lock.table=myGlueLockTable \
--packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,\
org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0,\
software.amazon.awssdk:bundle:2.15.40,software.amazon.awssdk:url-connection-client:2.15.40 

```

# Use Case 2 - Consume CDC events Published to Kinesis by DMS
    
## Message Content pushed to the topic
DMS publishes the changes to Kineiss 
```
{
		"data": {
		"LINE_ID": 144611,
		"LINE_NUMBER": 1,
		"ORDER_ID": 11363,
		"PRODUCT_ID": 927,
		"QUANTITY": 142,
		"UNIT_PRICE": 36,
		"DISCOUNT": 3,
		"SUPPLY_COST": 15,
		"TAX": 0,
		"ORDER_DATE": "2015-10-17"
		},
		"metadata": {
		"timestamp": "2021-11-19T13:24:43.297344Z",
		"record-type": "data",
		"operation": "update",
		"partition-key-type": "schema-table",
		"schema-name": "salesdb",
		"table-name": "SALES_ORDER_DETAIL",
		"transaction-id": 47330445004
		}
} 
```
## Spark Scala Code
[kinesis.iceberg.SparkKinesisConsumerIcebergProcessor](src/main/scala/kinesis/iceberg/SparkKinesisConsumerIcebergProcessor.scala)

## Spark Submit 

SSH to master node and then run the spark submit command.

```
spark-submit \
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
--conf spark.sql.catalog.my_catalog=org.apache.iceberg.spark.SparkCatalog    \
--conf spark.sql.catalog.my_catalog.warehouse=s3://akshaya-firehose-test/iceberg \
--conf spark.sql.catalog.my_catalog.catalog-impl=org.apache.iceberg.aws.glue.GlueCatalog \
--conf spark.sql.catalog.my_catalog.io-impl=org.apache.iceberg.aws.s3.S3FileIO \
--conf spark.sql.catalog.my_catalog.lock-impl=org.apache.iceberg.aws.glue.DynamoLockManager \
--conf spark.sql.catalog.my_catalog.lock.table=myGlueLockTable \
--packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,\
org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0,\
software.amazon.awssdk:bundle:2.15.40,software.amazon.awssdk:url-connection-client:2.15.40 \
--class kinesis.iceberg.latefile.SparkKinesisConsumerIcebergProcessor spark-structured-streaming-kinesis-iceberg_2.12-1.0.jar \
<bucket-name> <kinesis-stream-name> <kineis-region> <table-name>


```
Example
```
spark-submit \
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
--conf spark.sql.catalog.my_catalog=org.apache.iceberg.spark.SparkCatalog    \
--conf spark.sql.catalog.my_catalog.warehouse=s3://akshaya-firehose-test/iceberg \
--conf spark.sql.catalog.my_catalog.catalog-impl=org.apache.iceberg.aws.glue.GlueCatalog \
--conf spark.sql.catalog.my_catalog.io-impl=org.apache.iceberg.aws.s3.S3FileIO \
--conf spark.sql.catalog.my_catalog.lock-impl=org.apache.iceberg.aws.glue.DynamoLockManager \
--conf spark.sql.catalog.my_catalog.lock.table=myGlueLockTable \
--packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,\
org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0,software.amazon.awssdk:bundle:2.15.40,software.amazon.awssdk:url-connection-client:2.15.40 \
--class kinesis.iceberg.SparkKinesisConsumerIcebergProcessor spark-structured-streaming-kinesis-iceberg_2.12-1.0.jar \
akshaya-firehose-test data-stream-ingest ap-south-1 my_catalog.iceberg.iceberg_customer_order_details
	
```


## Spark Shell
Run the shell with command below and copy paste code from   [kinesis.iceberg.SparkKinesisConsumerIcebergProcessor](src/main/scala/kinesis/iceberg/SparkKinesisConsumerIcebergProcessor.scala). The code that needs to be copied is between  (Spark Shell ---Start ) and (Spark Shell ---End ). Also ensure that the you hard code the paremeters like s3_bucket, streamName, region ,tableType and hudiTableNamePrefix.  

```
spark-shell
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
--conf spark.sql.catalog.my_catalog=org.apache.iceberg.spark.SparkCatalog    \
--conf spark.sql.catalog.my_catalog.warehouse=s3://akshaya-firehose-test/iceberg \
--conf spark.sql.catalog.my_catalog.catalog-impl=org.apache.iceberg.aws.glue.GlueCatalog \
--conf spark.sql.catalog.my_catalog.io-impl=org.apache.iceberg.aws.s3.S3FileIO \
--conf spark.sql.catalog.my_catalog.lock-impl=org.apache.iceberg.aws.glue.DynamoLockManager \
--conf spark.sql.catalog.my_catalog.lock.table=myGlueLockTable \
--packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,\
org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0,\
software.amazon.awssdk:bundle:2.15.40,software.amazon.awssdk:url-connection-client:2.15.40 

```


# Use Case 3 - CDC event Published to S3 by DMS. S3 event triggered Lambda pushes file path to Kinesis. 
## Message Content pushed to the topic
The filePath here is the path to the file which got added to S3 by DMS. An S3 event gets published which is consumed by Lambda. The lambda then pushes the event below to the Kinesis stream which the file path of the file that got ingested. 
```
{
    "filePath": "s3://<bucket-name>/dms-full-load-path/salesdb/SALES_ORDER_DETAIL/20211118-100428844.parquet"
}
```
## Spark Scala Code
[kinesis.iceberg.SparkKinesisFilePathConsumerIcebergProcessor](src/main/scala/kinesis/iceberg/SparkKinesisFilePathConsumerIcebergProcessor.scala)

## Spark Submit 
    
    
```
spark-submit \
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
--conf spark.sql.catalog.my_catalog=org.apache.iceberg.spark.SparkCatalog    \
--conf spark.sql.catalog.my_catalog.warehouse=s3://akshaya-firehose-test/iceberg \
--conf spark.sql.catalog.my_catalog.catalog-impl=org.apache.iceberg.aws.glue.GlueCatalog \
--conf spark.sql.catalog.my_catalog.io-impl=org.apache.iceberg.aws.s3.S3FileIO \
--conf spark.sql.catalog.my_catalog.lock-impl=org.apache.iceberg.aws.glue.DynamoLockManager \
--conf spark.sql.catalog.my_catalog.lock.table=myGlueLockTable \
--packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,\
org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0,\
software.amazon.awssdk:bundle:2.15.40,software.amazon.awssdk:url-connection-client:2.15.40 \
--class kinesis.iceberg.latefile.SparkKinesisConsumerIcebergProcessor spark-structured-streaming-kinesis-iceberg_2.12-1.0.jar \
<bucket-name> <kinesis-stream-name> <kineis-region> <table-name>


```
Example
```
spark-submit \
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
--conf spark.sql.catalog.my_catalog=org.apache.iceberg.spark.SparkCatalog    \
--conf spark.sql.catalog.my_catalog.warehouse=s3://akshaya-firehose-test/iceberg \
--conf spark.sql.catalog.my_catalog.catalog-impl=org.apache.iceberg.aws.glue.GlueCatalog \
--conf spark.sql.catalog.my_catalog.io-impl=org.apache.iceberg.aws.s3.S3FileIO \
--conf spark.sql.catalog.my_catalog.lock-impl=org.apache.iceberg.aws.glue.DynamoLockManager \
--conf spark.sql.catalog.my_catalog.lock.table=myGlueLockTable \
--packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,\
org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0,\
software.amazon.awssdk:bundle:2.15.40,software.amazon.awssdk:url-connection-client:2.15.40 \
--class kinesis.iceberg.SparkKinesisFilePathConsumerIcebergProcessor spark-structured-streaming-kinesis-iceberg_2.12-1.0.jar \
akshaya-firehose-test data-stream-ingest ap-south-1 my_catalog.iceberg.iceberg_customer_order_details
	
```

## Spark Shell

Run the shell with command below and copy paste code from   [kinesis.iceberg.SparkKinesisFilePathConsumerIcebergProcessor](src/main/scala/kinesis/iceberg/SparkKinesisFilePathConsumerIcebergProcessor.scala). The code that needs to be copied is between  (Spark Shell ---Start ) and (Spark Shell ---End ). Also ensure that the you hard code the paremeters like s3_bucket, streamName, region ,tableType and hudiTableNamePrefix.  

```
spark-shell
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
--conf spark.sql.catalog.my_catalog=org.apache.iceberg.spark.SparkCatalog    \
--conf spark.sql.catalog.my_catalog.warehouse=s3://akshaya-firehose-test/iceberg \
--conf spark.sql.catalog.my_catalog.catalog-impl=org.apache.iceberg.aws.glue.GlueCatalog \
--conf spark.sql.catalog.my_catalog.io-impl=org.apache.iceberg.aws.s3.S3FileIO \
--conf spark.sql.catalog.my_catalog.lock-impl=org.apache.iceberg.aws.glue.DynamoLockManager \
--conf spark.sql.catalog.my_catalog.lock.table=myGlueLockTable \
--packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,\
org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0,\
software.amazon.awssdk:bundle:2.15.40,software.amazon.awssdk:url-connection-client:2.15.40 
```
# Possible Issues 

2. 
