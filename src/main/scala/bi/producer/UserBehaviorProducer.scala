package bi.producer

import java.util.Properties
import kafka.producer._
import scala.util.Random

class UserBehaviorMsgProducer(brokers: String, topic: String) extends Runnable {
  private val brokerList = brokers
  private val targetTopic = topic
  private val props = new Properties()
  props.put("metadata.broker.list", this.brokerList)
  props.put("serializer.class", "kafka.serializer.StringEncoder")
  props.put("producer.type", "async")
  private val config = new ProducerConfig(this.props)
  private val producer = new Producer[String, String](this.config)

  private val PAGE_NUM = 100
  private val MAX_MSG_NUM = 3
  private val MAX_CLICK_TIME = 5
  private val MAX_STAY_TIME = 10
  //Like,1;Dislike -1;No Feeling 0
  private val LIKE_OR_NOT = Array[Int](1, 0, -1)

  def removeAt[A](n: Int, ls: List[A]): (List[A], A) = ls.splitAt(n) match {
    case (Nil, _) if n < 0 => throw new NoSuchElementException
    case (pre, e :: post)  => (pre ::: post, e)
    case (pre, Nil)        => throw new NoSuchElementException
  }

  // Alternate, with fewer builtins.
  def removeAt2[A](n: Int, ls: List[A]): (List[A], A) =
    if (n < 0) throw new NoSuchElementException
    else (n, ls) match {
      case (_, Nil) => throw new NoSuchElementException
      case (0, h :: tail) => (tail, h)
      case (_, h :: tail) => {
        val (t, e) = removeAt(n - 1, ls.tail)
        (ls.head :: t, e)
      }
    }

  def randomSelect[A](n: Int, ls: List[A]): List[A] = {
    def randomSelectR(n: Int, ls: List[A], r: util.Random): List[A] =
      if (n <= 0) Nil
      else {
        val (rest, e) = removeAt(r.nextInt(ls.length), ls)
        e :: randomSelectR(n - 1, rest, r)
      }
    randomSelectR(n, ls, new util.Random)
  }

  private val mission = List(
    "info:client:page=subcategory:bu=inc|category=CPU"
    ,"info:client:page=item:bu=inc|subcategory=Monitors - Touchscreen|category=Monitors"
    ,"info:client:page=subcategory:bu=inc|category=VGA"
    ,"info:client:page=querycateitem:bu=inc|category=VGA|token=123456"
    ,"info:client:page=item:bu=inc|subcategory=TV Brackets"
    ,"info:client:page=item:bu=inc|category=VGA|subcategory=Video Card - Nvidia"
    ,"info:dataready:subcategory:inc:5df21b01"
    ,"info:dataready:item"
    ,"info:client:page=dashboard"
    ,"info:client:page=console"
    ,"info:client:page=overview:bu=inc"
    ,"info:client:page=category:bu=inc"
    ,"info:client:page=subcategory:bu=inc|category=CPU"
    ,"info:client:page=item:bu=inc|subcategory=CPU Gifts"
    ,"server:item:_inc:20-233-383|;|34-735-042|;|17-994-162|;|20-231-122|;|30-950-020:192.168.1.101"
    ,"monitor:kafka:itemalarm:"
    ,"client:overview:_inc"
    ,"client:category:inc"
    ,"client:subcategory:inc:CPU"
    ,"client:item:inc:CPU Gifts"
    ,"client:topcateitem:"
    ,"client:status:running:2015-09-11"
    ,"client:dashboard:gis:usa:"
    ,"client:map:gis:usa_states"
    ,"client:map:gis:usa_counties"
    ,"client:buybox:items:"
    ,"client:buybox:ap:itempriceinfo:"
    ,"client:buybox:rst_summary:"
    ,"client:buybox:rst_subcategory:"
  )

  /*1. info:client:page=subcategory:bu=inc|category=CPU
    2. info:client:page=item:bu=inc|subcategory=Monitors - Touchscreen|category=Monitors
    3. info:client:page=subcategory:bu=inc|category=VGA
    4. info:client:page=querycateitem:bu=inc|category=VGA|token=123456
    5. info:client:page=item:bu=inc|subcategory=TV Brackets
    6. info:client:page=item:bu=inc|category=VGA|subcategory=Video Card - Nvidia
    7. info:dataready:subcategory:inc:5df21b01
    8. info:dataready:item
    9. info:client:page=dashboard
    10. info:client:page=console
    11. info:client:page=overview:bu=inc
    12. info:client:page=category:bu=inc
    13. info:client:page=subcategory:bu=inc|category=CPU
    14. info:client:page=item:bu=inc|subcategory=CPU Gifts
    15. server:item:_inc:20-233-383|;|34-735-042|;|17-994-162|;|20-231-122|;|30-950-020:192.168.1.101
    16. monitor:kafka:itemalarm:
    17. client:overview:_inc
    18. client:category:inc
    19. client:subcategory:inc:CPU
    20. client:item:inc:CPU Gifts
    21. client:topcateitem:
    22. client:status:running:2015-09-11
    23. client:dashboard:gis:usa:
    24. client:map:gis:usa_states
    25. client:map:gis:usa_counties
    26. client:buybox:items:
    27. client:buybox:ap:itempriceinfo:
    28. client:buybox:rst_summary:
    29. client:buybox:rst_subcategory:

   */

  def run(): Unit = {
    val rand = new Random()
    while (true) {
      //how many user behavior messages will be produced
      val msgNum = rand.nextInt(MAX_MSG_NUM) + 1
      try {
        //generate the message with format like page1|2|7.123|1
        for (i <- 0 to msgNum) {
          //var msg = new StringBuilder()
          //msg.append("page" + (rand.nextInt(PAGE_NUM) + 1))
          //msg.append("|")
          //msg.append(rand.nextInt(MAX_CLICK_TIME) + 1)
          //msg.append("|")
          //msg.append(rand.nextInt(MAX_CLICK_TIME) + rand.nextFloat())
          //msg.append("|")
          //msg.append(LIKE_OR_NOT(rand.nextInt(3)))
          //println(msg.toString())
          //send the generated message to broker
          var msgtest = new StringBuilder()
          var strpara = randomSelect(1, mission)
          msgtest.append(strpara.head)
          println(msgtest.toString())

          sendMessage(msgtest.toString())
        }
        println("%d user behavior messages produced.".format(msgNum+1))
      } catch {
        case e: Exception => println(e)
      }
      try {
        //sleep for 5 seconds after send a micro batch of message
        Thread.sleep(5000)
      } catch {
        case e: Exception => println(e)
      }
    }
  }
  def sendMessage(message: String) = {
    try {
      val data = new KeyedMessage[String, String](this.topic, message);
      producer.send(data);
    } catch {
      case e:Exception => println(e)
    }
  }
}
object UserBehaviorMsgProducerClient {
  def main(args: Array[String]) {
    if (args.length < 2) {
      println("Usage:UserBehaviorMsgProducerClient localhost:9092 %targetTopic -- CDH")
      println("Usage:UserBehaviorMsgProducerClient sandbox.hortonworks.com:6667 %targetTopic -- HDP")
      System.exit(1)
    }
    //start the message producer thread
    new Thread(new UserBehaviorMsgProducer(args(0), args(1))).start()
  }
}
