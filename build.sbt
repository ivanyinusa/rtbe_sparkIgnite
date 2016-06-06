name := "rtbe_sparkIgnite"

version := "1.0"

scalaVersion := "2.10.4"

//resolvers += "ignite-scalar" at "http://mvnrepository.com/artifact/org.apache.ignite/"

//producer
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.9.0.0"
libraryDependencies += "org.apache.kafka" % "kafka_2.10" % "0.9.0.0"
libraryDependencies += "org.apache.kafka" % "kafka-log4j-appender" % "0.9.0.0"

//streaming center
libraryDependencies += "com.firebase" % "tubesock" % "0.0.12"

libraryDependencies += "org.eclipse.jetty.websocket" % "websocket-common" % "9.2.15.v20160210"
libraryDependencies += "org.eclipse.jetty.websocket" % "websocket-api" % "9.2.15.v20160210"
libraryDependencies += "org.eclipse.jetty.websocket" % "websocket-server" % "9.2.15.v20160210"
libraryDependencies += "org.eclipse.jetty.websocket" % "websocket-client" % "9.2.15.v20160210"
libraryDependencies += "org.eclipse.jetty.websocket" % "javax-websocket-server-impl" % "9.2.15.v20160210"

//backend
libraryDependencies += "org.apache.ignite" % "ignite-core" % "1.5.0.final"
libraryDependencies += "org.apache.ignite" % "ignite-indexing" % "1.5.0.final"
libraryDependencies += "org.apache.ignite" % "ignite-kafka" % "1.5.0.final"
libraryDependencies += "org.apache.ignite" % "ignite-log4j2" % "1.5.0.final"
libraryDependencies += "org.apache.ignite" % "ignite-log4j" % "1.5.0.final"
libraryDependencies += "org.apache.ignite" % "ignite-spark_2.10" % "1.5.0.final"
libraryDependencies += "org.apache.ignite" % "ignite-spring" % "1.5.0.final"

libraryDependencies += "org.apache.spark" % "spark-core_2.10" % "1.6.1"
libraryDependencies += "org.apache.spark" % "spark-streaming_2.10" % "1.6.1"
libraryDependencies += "org.apache.spark" % "spark-streaming-kafka_2.10" % "1.6.1"
libraryDependencies += "com.google.code.gson" % "gson" % "2.6.2"

//oreilly
//libraryDependencies += "org.eclipse.jetty" % "jetty-client" % "9.3.3.v20150827"
//libraryDependencies += "com.typesafe.play" % "play-json_2.10" % "2.4.0"

//web

//test
libraryDependencies += "org.scalanlp" % "breeze_2.10" % "0.12"
libraryDependencies += "com.github.scopt" % "scopt_2.10" % "3.3.0"

libraryDependencies += "org.apache.spark" % "spark-graphx_2.10" % "1.6.1"
libraryDependencies += "org.apache.spark" % "spark-hive_2.10" % "1.6.1"
libraryDependencies += "org.apache.spark" % "spark-mllib_2.10" % "1.6.1"
libraryDependencies += "org.apache.spark" % "spark-sql_2.10" % "1.6.1"
libraryDependencies += "org.apache.spark" % "spark-streaming-twitter_2.10" % "1.6.1"
//libraryDependencies += "org.apache.spark" % "spark-streaming-flume_2.10" % "1.6.1"
//libraryDependencies += "org.apache.spark" % "spark-catalyst_2.10" % "1.6.1"
//libraryDependencies += "org.apache.spark" % "spark-launcher_2.10" % "1.6.1"
//libraryDependencies += "org.apache.spark" % "spark-network-common_2.10" % "1.6.1"
//libraryDependencies += "org.apache.spark" % "spark-network-shuffle_2.10" % "1.6.1"
//libraryDependencies += "org.apache.spark" % "spark-unsafe_2.10" % "1.6.1"

libraryDependencies += "com.twitter" % "algebird-core_2.10" % "0.12.0"
//libraryDependencies += "org.apache.ignite" % "ignite-scalar" % "1.5.0.final"

//libraryDependencies += "com.h2database" % "h2" % "1.4.191"
libraryDependencies += "net.spy" % "spymemcached" % "2.12.0"
libraryDependencies += "com.sclasen" % "akka-kafka_2.10" % "0.1.0"
libraryDependencies += "com.typesafe.akka" % "akka-testkit_2.10" % "2.3.11"
libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.2.6"
libraryDependencies += "org.spire-math" % "spire_2.10" % "0.8.2"

//libraryDependencies += "org.apache.cassandra" % "cassandra-all" % "2.1.2"
//libraryDependencies += "org.apache.hbase" % "hbase-client" % "1.2.1"

//libraryDependencies += "org.springframework" % "spring-core" % "4.2.6.RELEASE"
//libraryDependencies += "org.springframework" % "spring-jdbc" % "4.2.6.RELEASE"
//libraryDependencies += "org.springframework" % "spring-dao" % "2.0.8"
