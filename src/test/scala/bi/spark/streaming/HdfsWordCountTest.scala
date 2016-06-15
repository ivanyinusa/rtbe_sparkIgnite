package bi.spark.streaming

import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * Counts words in new text files created in the given directory
  * Usage: HdfsWordCount <directory>
  *   <directory> is the directory that Spark Streaming will use to find and read new text files.
  *
  * To run this on your local machine on directory `localdir`, run this example
  *    $ bin/run-example \
  *       org.apache.spark.examples.streaming.HdfsWordCount localdir
  *
  * Then create a text file in `localdir` and the words in the file will get counted.
  */

object HdfsWordCountTest {
  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      System.err.println("Usage: HdfsWordCount <directory>")
      System.exit(1)
    }

    StreamingExamples.setStreamingLogLevels()
    val sparkconf = new org.apache.spark.SparkConf().setMaster("local[2]").setAppName("test-consumer-group")
    val ssc = new StreamingContext(sparkconf, Seconds(10))

    val lines = ssc.textFileStream(args(0))
    val words = lines.flatMap(_.split(" "))
    val wordcount = words.map(x => (x, 1)).reduceByKey(_ + _)
    wordcount.print()
    ssc.start()
    ssc.awaitTermination()
  }
}

