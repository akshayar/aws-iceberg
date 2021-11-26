name := "Spark-Structured-Streaming-Kinesis-Iceberg"

version := "1.0"

val sparkVersion = "3.1.1"
val scala_tool_version="2.12"

libraryDependencies += "log4j" % "log4j" % "1.2.14"

libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion


libraryDependencies += "org.apache.spark" % "spark-streaming-kinesis-asl_2.12" % sparkVersion
libraryDependencies += "com.qubole.spark" % "spark-sql-kinesis_2.12" % "1.2.0_spark-3.0"
libraryDependencies += "org.apache.iceberg" % "iceberg-spark3-runtime" % "0.12.1"
libraryDependencies += "org.apache.iceberg" % "iceberg-spark3-extensions" % "0.12.1"

fork in run := true

libraryDependencies += "org.elasticsearch" % "elasticsearch-hadoop" % "6.3.0"
assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case x => MergeStrategy.first
}

assemblyExcludedJars in assembly := {
    val cp = (fullClasspath in assembly).value
    cp filter { f =>
      f.data.getName.contains("spark-core") ||
      f.data.getName.contains("spark-core") ||
      f.data.getName.contains("iceberg")
    }
  }
