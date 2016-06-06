package bi.spark.streaming

import java.io.{InputStreamReader, BufferedReader, InputStream}
import java.net.Socket

import org.apache.spark.{SparkConf, Logging}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.receiver.Receiver

/**
  * Custom Receiver that receives data over a socket. Received bytes is interpreted as
  * text and \n delimited lines are considered as records. They are then counted and printed.
  *
  * To run this on your local machine, you need to first run a Netcat server
  *    `$ nc -lk 44444`
  * and then run the example
  *    `$ bin/run-example org.apache.spark.examples.streaming.CustomReceiver localhost 44444`
  */

object CustomReceiverTest {
  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println("Usage:  CustomReceiverTest <hostname> <port>")
      System.exit(1)
    }

    StreamingExamples.setStreamingLogLevels()

    //Create spark Context with 1 second batch size
    val conf = new SparkConf().setMaster("local[2]").setAppName("CustomReceiver")
    val sc = new StreamingContext(conf, Seconds(10))
    val lines = sc.receiverStream(new CustomReceiverTest(args(0), args(1).toInt))
    val words = lines.flatMap(_.split(" "))
    val wordcount = words.map(x => (x, 1)).reduceByKey(_ + _) //words.count()
    wordcount.print()
    sc.start()
    sc.awaitTermination()
  }

}

class CustomReceiverTest(host: String, port: Int)
    extends Receiver[String](StorageLevel.MEMORY_AND_DISK_2) with Logging {

    def onStart() = {

      //start thread that receives data for a connection
      new Thread("Socket Receiver") {receive()}.start()

    }

    def onStop() = {

    }

    def receive() =
    {
      var socket: Socket = null
      var userInput: String = null

      try {

        logInfo("Connecting to :  " + host + "  port: " + port)
        socket = new Socket(host, port)
        logInfo("Connected to : " + host + "port: " + port)
        val reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))
        userInput = reader.readLine()
        while (!isStopped && userInput != null) {
          store(userInput)
          userInput = reader.readLine()
        }

        reader.close()
        socket.close()
        logInfo("Socket stopped....")
        restart("trying to reconnect......")
      }catch {
        case e: java.net.ConnectException =>
          restart("Error connecting to " + host + ":" + port, e)
        case t: Throwable =>
          restart("Error receiving data", t)
      }
    }
}
