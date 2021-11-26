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

````



