# Quick Start

```
spark-shell --packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1   \
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
--conf spark.sql.catalog.spark_catalog=org.apache.iceberg.spark.SparkSessionCatalog    \
--conf spark.sql.catalog.spark_catalog.type=hive    \
--conf spark.sql.catalog.local=org.apache.iceberg.spark.SparkCatalog   \
--conf spark.sql.catalog.local.type=hadoop   \
--conf spark.sql.catalog.local.warehouse=s3://<bucket-name>/<base-path>

val data = spark.range(0, 5)
data.writeTo("local.default.first_iceberg").create()

```


# Spark Streaming

## Spark Submit

```
spark-submit \
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
--conf spark.sql.catalog.spark_catalog=org.apache.iceberg.spark.SparkSessionCatalog    \
--conf spark.sql.catalog.spark_catalog.type=hive    \
--conf spark.sql.catalog.local=org.apache.iceberg.spark.SparkCatalog   \
--conf spark.sql.catalog.local.type=hadoop   \
--conf spark.sql.catalog.local.warehouse=s3://<bucket-name>/<base-path> \
--conf spark.serializer=org.apache.spark.serializer.KryoSerializer \
--conf spark.sql.hive.convertMetastoreParquet=false \
--packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0 \
--class <class-name> <jar-file> \
<parameters>
```

## Spark Shell

```

spark-shell --packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0    \
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
--conf spark.sql.catalog.spark_catalog=org.apache.iceberg.spark.SparkSessionCatalog    \
--conf spark.sql.catalog.spark_catalog.type=hive    \
--conf spark.sql.catalog.local=org.apache.iceberg.spark.SparkCatalog   \
--conf spark.sql.catalog.local.type=hadoop   \
--conf spark.sql.catalog.local.warehouse=s3://<bucket-name>/<base-path> \
--conf spark.serializer=org.apache.spark.serializer.KryoSerializer \
--conf spark.sql.hive.convertMetastoreParquet=false 

spark-shell \
    --conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
    --conf spark.sql.catalog.my_catalog=org.apache.iceberg.spark.SparkCatalog    \
    --conf spark.sql.catalog.my_catalog.warehouse=s3://akshaya-firehose-test/iceberg \
    --conf spark.sql.catalog.my_catalog.catalog-impl=org.apache.iceberg.aws.glue.GlueCatalog \
    --conf spark.sql.catalog.my_catalog.io-impl=org.apache.iceberg.aws.s3.S3FileIO \
    --conf spark.sql.catalog.my_catalog.lock-impl=org.apache.iceberg.aws.glue.DynamoLockManager \
    --conf spark.sql.catalog.my_catalog.lock.table=myGlueLockTable \
    --packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0,software.amazon.awssdk:bundle:2.15.40,software.amazon.awssdk:url-connection-client:2.15.40


````

## Spark SQL
```
spark-sql \
    --conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions    \
    --conf spark.sql.catalog.my_catalog=org.apache.iceberg.spark.SparkCatalog    \
    --conf spark.sql.catalog.my_catalog.warehouse=s3://akshaya-firehose-test/iceberg \
    --conf spark.sql.catalog.my_catalog.catalog-impl=org.apache.iceberg.aws.glue.GlueCatalog \
    --conf spark.sql.catalog.my_catalog.io-impl=org.apache.iceberg.aws.s3.S3FileIO \
    --conf spark.sql.catalog.my_catalog.lock-impl=org.apache.iceberg.aws.glue.DynamoLockManager \
    --conf spark.sql.catalog.my_catalog.lock.table=myGlueLockTable \
    --packages org.apache.iceberg:iceberg-spark3-runtime:0.12.1,org.apache.iceberg:iceberg-spark3-extensions:0.12.1,org.apache.spark:spark-streaming-kinesis-asl_2.12:3.1.1,com.qubole.spark:spark-sql-kinesis_2.12:1.2.0_spark-3.0,software.amazon.awssdk:bundle:2.15.40,software.amazon.awssdk:url-connection-client:2.15.40


CREATE OR REPLACE TABLE my_catalog.default.akshaya_table (
    id bigint,
    data string,
    category string)
USING iceberg
PARTITIONED BY (category);

INSERT INTO my_catalog.default.akshaya_table VALUES (1, "Pizza", "orders");
```

