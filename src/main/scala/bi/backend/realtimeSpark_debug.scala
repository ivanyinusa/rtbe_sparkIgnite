package bi.backend

import org.apache.spark.SparkConf
import org.apache.spark._
import org.apache.spark.streaming.kafka._
import org.apache.spark.storage.StorageLevel._
import java.util.concurrent.{Callable, Executors, Future}
import java.sql.{Connection, DriverManager, ResultSet}
import java.util.HashMap

import scala.sys.process.Process
import com.firebase.tubesock.{WebSocket, WebSocketEventHandler, WebSocketException, WebSocketMessage}
import bi.ignite._
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.spark.IgniteContext

object realtimeSpark_debug extends App {

  //spark-shell --jars "$(echo /software/setting_class/*.jar | tr ' ' ','),$(echo /software/kalfka/*.jar | tr ' ' ','),$(echo /software/extra_class/*.jar | tr ' ' ','),$(echo /software/ignite/jars/*.jar | tr ' ' ',')" --driver-memory 2g
  case class itemInstance(country: String, item_of_mktpl: String, item_of_newegg: String, rank: String, dt: String, item_desc: String, domain: String, grp_code: Integer, category: String, ctlg_code: Integer, subcategory: String, counter: Integer, q4s: Integer, warehouse_type: String, brand: String, runrate_type: String, neweggwh_only_avail: Integer, vf_only_avail: Integer, final_price: Double)

  /*
Task 1: test RESTful service
//val REST = new bi.info.itemREST() => test1

Task 2: Item Information - Serializable Java Class
bi.ignite.ItemAlarm => test2
 */

  val REST = new bi.info.itemREST()

  //kalfka settings
  val topicMap = Map("test" -> 2)
  val sparkConf = new SparkConf()
    .setMaster("local[2]")
    .setAppName("test_consumer_group")
  //val sparkConf = new SparkConf().setMaster("local[2]").setAppName("test_consumer_group")
  val sc = new SparkContext(sparkConf)
  val ssc = new org.apache.spark.streaming.StreamingContext(sc, org.apache.spark.streaming.Seconds(10))
  //"metadata.broker.list" -> "192.168.1.101:6667,192.168.1.101:6667,192.168.1.101:6667",
  val kafkaConf = Map(
    "metadata.broker.list" -> "192.168.1.101:6667,192.168.1.101:6667,192.168.1.101:6667",
    "zookeeper.connect" -> "192.168.1.101:2181,192.168.1.101:2181,192.168.1.101:2181",
    "group.id" -> "test_consumer_group",
    "zookeeper.connection.timeout.ms" -> "10000"
  )

  val streamsByPartition = (1 to 1).map { _ => KafkaUtils.createStream[String, String, kafka.serializer.DefaultDecoder, kafka.serializer.StringDecoder](ssc, kafkaConf, topicMap, MEMORY_ONLY_SER).map(_._2) }
  //val streamsByPartition = (1 to 8).map { _ => KafkaUtils.createStream[String, String, kafka.serializer.DefaultDecoder, kafka.serializer.StringDecoder](ssc, kafkaConf, topicMap, MEMORY_ONLY_SER).map(_._2) }
  val unifiedStream = ssc.union(streamsByPartition)
  val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val gson = new com.google.gson.Gson
  //customized websocket assitance
  var ws = new WebSocket(java.net.URI.create("ws://192.168.1.101:8889/so_other/"))

  def ic_conf(): org.apache.ignite.configuration.IgniteConfiguration = {
    val spi = new org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi()
    val ipFinder = new org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder(true)
    val attr = java.util.Arrays.asList("192.168.1.101", "192.168.1.101") //ignite server
    ipFinder.setAddresses(attr)
    spi.setIpFinder(ipFinder)
    val marshaller = new org.apache.ignite.internal.binary.BinaryMarshaller
    //val marshaller = new org.apache.ignite.marshaller.optimized.OptimizedMarshaller()
    //marshaller.setRequireSerializable(false)
    val cfg = new org.apache.ignite.configuration.IgniteConfiguration()
    cfg.setDiscoverySpi(spi)
    cfg.setMarshaller(marshaller)
    cfg.setClientMode(true)
    cfg.setMetricsLogFrequency(0)
    cfg
  }

  val ic = new IgniteContext[String, bi.ignite.ItemAlarm](sc, () => ic_conf())

  def ic_cache(cache_name: String): org.apache.ignite.configuration.CacheConfiguration[String, ItemAlarm] = {
    //def ic_cache(cache_name:String): org.apache.ignite.configuration.CacheConfiguration[String,bi.ignite.ItemAlarm] ={
    val cache_cfg = new CacheConfiguration[String, ItemAlarm](cache_name)
    //val cache_cfg = new CacheConfiguration[String,bi.ignite.ItemAlarm](cache_name)
    cache_cfg.setAtomicityMode(org.apache.ignite.cache.CacheAtomicityMode.ATOMIC)
    cache_cfg.setCacheMode(org.apache.ignite.cache.CacheMode.PARTITIONED)
    cache_cfg.setAtomicWriteOrderMode(org.apache.ignite.cache.CacheAtomicWriteOrderMode.CLOCK)
    cache_cfg.setMemoryMode(org.apache.ignite.cache.CacheMemoryMode.OFFHEAP_TIERED)
    cache_cfg.setOffHeapMaxMemory(5 * 1024L * 1024L * 1024L)
    cache_cfg.setIndexedTypes(classOf[String], classOf[ItemAlarm])
    //cache_cfg.setIndexedTypes(classOf[String],classOf[bi.ignite.ItemAlarm])
    cache_cfg
  }

  val ignite_instance = org.apache.ignite.Ignition.ignite()
  val item_store = ignite_instance.getOrCreateCache(ic_cache("ItemAlarm"))
  val pool = java.util.concurrent.Executors.newFixedThreadPool(16)
  var counter: Int = 0

  val domain_map = new HashMap[String, String]()
  try {
    Class.forName("net.sourceforge.jtds.jdbc.Driver")
    val conn = DriverManager.getConnection("jdbc:jtds:sqlserver://10.1.70.100:1433/NEStaging", "etl_sp", "1qazxsw2")
    val st = conn.prepareCall("{call NEStaging.dbo.ETL_D_Domain_Retriever_forSpark_New}")
    val rs_domain = st.executeQuery()
    while (rs_domain.next()) {
      domain_map.put(rs_domain.getString("category").trim(), rs_domain.getString("domain").trim())
    }
    rs_domain.close
    conn.close()
  } catch {
    case _: Throwable => {}
  } finally {
    //
  }
  ( /*
USA,9SIA67S2181555,22-136-279,1,2016-01-18 11:01:29,TECH EXPERTS,90.29,93.990,0.9195907007855459544383346426,0.9108652286724273369992144540,0.0,00,0.0
Biz Unit,winner item#,newegg item#,newegg item position,timestamp,winner seller name, winner price,newegg price, winner score, newegg score,newegg pcode,newegg eggpoint, newegg mir, newegg AATC
item_store.clear
proccess_item("USA,9SIA21B1B62547,16-401-958,6")
*/ );

  def parseDouble(source: String): Double = {
    var r: Double = 0.0
    try {
      val pattern = "[+-]?(([1-9][0-9]*)|(0))([.,][0-9]+)?".r
      r = pattern.findAllIn(source).mkString.replace(",", "").toDouble
    } catch {
      case _: Throwable => {}
    } finally {
      //
    }
    r
  }

  def proccess_item(data: String): String = {
    val dt = new java.util.Date()
    var item_of_usa = ""
    try {
      Thread.sleep(scala.util.Random.nextInt(500))
      println(formatter.format(dt.getTime()) + "  >" + data)
      var ar = data.split(",")
      var BU = ar(0).toString
      if (BU == "USA") {
        item_of_usa = ar(2).toString
        val rank = ar(3)
        val timestamp = ar(4)
        val winner_seller = ar(5)
        val winner_price = parseDouble(ar(6))
        val newegg_price = parseDouble(ar(7))
        val winner_score = parseDouble(ar(8))
        val newegg_score = parseDouble(ar(9))
        val newegg_pcode = parseDouble(ar(10))
        val newegg_eggpoint = parseDouble(ar(11))
        val newegg_mir = parseDouble(ar(12))
        val newegg_aatc = 0.0 //parseDouble(ar(13))
        val winner_vl = 0.0 //parseDouble(ar(14))
        val winner_vm = 0.0 //parseDouble(ar(15))
        val newegg_vl = 0.0 //parseDouble(ar(16))
        val newegg_vm = 0.0 //parseDouble(ar(17))
        val newegg_instock = "" //parseDouble(ar(18))
        var q4s = 0
        var OPC = 0
        var VF = 0
        var final_price = 0.0
        var warehouse_type: String = ""
        var brand: String = ""
        var runrate_type: String = ""
        try {
          //val m2 = scala.util.parsing.json.JSON.parseFull("USA,9SIA67S2181555,22-136-279,1,2016-01-18 11:01:29,TECH EXPERTS,90.29,93.990,0.9195907007855459544383346426,0.9108652286724273369992144540,0.0,00,0.0").get.asInstanceOf[Map[String, Any]]
          val m2=scala.util.parsing.json.JSON.parseFull(REST.getItemExtend(item_of_usa).replace("\\\"","\"").split("\\[")(1).split("\\]")(0)).get.asInstanceOf[Map[String, Any]]
          q4s = m2.get("newegg_Avail").map(_.toString.toFloat.toInt).getOrElse(0)
          OPC = m2.get("neweggWH_Only_Avail").map(_.toString.toFloat.toInt).getOrElse(0)
          VF = m2.get("VF_Only_Avail").map(_.toString.toFloat.toInt).getOrElse(0)
          final_price = m2.get("UnitMinusIR_USA").map(_.toString.toDouble).getOrElse(0.0)
          if (OPC > 0 && VF > 0) {
            warehouse_type = "Hybrid"
          }
          if (OPC > 0 && VF <= 0) {
            warehouse_type = "OPC"
          }
          if (OPC <= 0 && VF > 0) {
            warehouse_type = "VF"
          }
          brand = java.net.URLEncoder.encode(m2.get("ManufacturerName").map(_.toString).getOrElse(""))
          runrate_type = m2.get("RunRate_QTY").map(_.toString).getOrElse("")
        } catch {
          case _: Throwable => {}
        } finally {
          //
        }
        if (item_of_usa != "") {
          //val m1 = scala.util.parsing.json.JSON.parseFull("USA,9SIA67S2181555,22-136-279,1,2016-01-18 11:01:29,TECH EXPERTS,90.29,93.990,0.9195907007855459544383346426,0.9108652286724273369992144540,0.0,00,0.0").get.asInstanceOf[Map[String, Any]]
          val m1=scala.util.parsing.json.JSON.parseFull(REST.getItemDesc(item_of_usa).toString).get.asInstanceOf[Map[String, Any]]
          val item_desc = java.net.URLEncoder.encode(m1.get("ShortDescription").map(_.toString).getOrElse(""))
          val grp_code = m1.get("CategoryCode").map(_.toString.toFloat.toInt).getOrElse(0)
          val category = java.net.URLEncoder.encode(m1.get("CategoryName").map(_.toString).getOrElse("").trim())
          val ctlg_code = m1.get("SubcategoryCode").map(_.toString.toFloat.toInt).getOrElse(0)
          val domain = java.net.URLEncoder.encode(domain_map.get(m1.get("CategoryName").map(_.toString).getOrElse("").trim()))
          val subcategory = java.net.URLEncoder.encode(m1.get("SubcategoryName").map(_.toString).getOrElse("").trim())
          //timestamp,winner_seller, winner_price,newegg_price, winner_score, newegg_score,newegg_pcode,newegg_eggpoint, newegg_mir, newegg_aatc
          val item = new ItemAlarm
          //val item=new bi.ignite.ItemAlarm(ar(0),ar(1),item_of_usa,rank,dt,item_desc,domain,grp_code,category,ctlg_code,subcategory,1,q4s,warehouse_type,brand,runrate_type,OPC,VF,final_price,timestamp,winner_seller, winner_price,newegg_price, winner_score, newegg_score,newegg_pcode,newegg_eggpoint, newegg_mir, newegg_aatc, winner_vl, winner_vm, newegg_vl,newegg_vm,newegg_instock)
          //         val itemJson=gson.toJson(itemInstance(item.country,item.item_of_mktpl,item.item_of_newegg,item.rank,formatter.format(item.dt.getTime()),item.item_desc,item.domain,item.grp_code,item.category,item.ctlg_code,item.subcategory,item.counter,item.q4s,item.warehouse_type,item.brand,item.runrate_type,item.neweggwh_only_avail,item.vf_only_avail,item.final_price))
          val itemJson = gson.toJson(item)
          ws.send("client:buybox:items:" + itemJson)
        }
      } else {
        println(item_of_usa + " not from USA")
        // ivan added
        ws.send(formatter.format(dt.getTime()) + "  >" + data)
        println("No right message format --> Message Center -- print out message")
      }
    } catch {
      case e: Exception => {
        println(item_of_usa + " > " + e.toString())
        proccess_item(data)
      }
    } finally {
      //
    }
    "done"
  }

  unifiedStream.foreachRDD(rdd => {
    if (rdd.count > 0) {
      val ar = rdd.distinct.collect
      // ar.foreach(println)
      ar.foreach(el => {
        //println("raw:"+el)
        pool.submit(new Callable[String] {
          def call() = {
            proccess_item(el)
          }
        })
      })
    }
  })

  def init_ws() {
    ws.setEventHandler(new WebSocketEventHandler() {
      def onOpen() {
        //Process("cmd", Seq("-oa"))
        println("websocket connected")
      }

      def onMessage(message: WebSocketMessage) {
        //  println(message.getText())
      }

      def onClose() {
        println("connection lost, re-try in 5 seconds...")
        Thread.sleep(10000)
        //192.168.1.101:8080
        ws = new WebSocket(java.net.URI.create("ws://192.168.1.101:8889/so_other/"))
        init_ws()
      }

      def onError(e: WebSocketException) {
        println("exception:" + e.getMessage())

      }

      def onLogMessage(msg: String) {
        println("disconnected")
      }
    })
    ws.connect()
  }

  init_ws()



  ssc.start()
  ssc.awaitTermination()
  ws.close
  pool.shutdown

}
