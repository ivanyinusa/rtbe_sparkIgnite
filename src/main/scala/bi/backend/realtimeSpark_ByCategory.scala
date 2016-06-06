package bi.backend

import org.apache.ignite.configuration._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

import scala.collection.mutable._
import scala.util.parsing._
import java.util.concurrent.{Callable, Executors, Future}

import com.firebase.tubesock.{WebSocket, WebSocketEventHandler, WebSocketException, WebSocketMessage}
import bi.ignite.ItemAlarm
import org.apache.ignite.spark.IgniteContext

object realtimeSpark_ByCategory extends App {

  //val sparkConf = new SparkConf().setMaster("local").setAppName("EC_BuyBox_BI_Consumer")
  val sparkConf = new SparkConf()
    .setMaster("local[2]")
    .setAppName("test_consumer_group")
    .set("spark.ui.port", "4041" )
  val sc = new SparkContext(sparkConf)
  val conf = new SparkConf(true).setMaster("local[2]").setAppName("ServiceProvider").set("spark.ui.port", "4041" )

  //ServiceProvider_v2.5.1  modify history data path
  //spark-shell --jars "/software/websocket/spark-indexedrdd-0.1.jar,/software/websocket/EasySock.jar,$(echo $IGNITE_HOME/jars/*.jar | tr ' ' ',')"
  // --conf "spark.executor.extraClassPath=/opt/cloudera/parcels/CDH-5.3.0-1.cdh5.3.0.p0.30/jars/guava-14.0.jar"
  // --num-executors 8 --executor-cores 12 --driver-memory 10g --executor-memory 10g

  case class paramProp(mission: String, bu: String, category: String, subcategory: String, start_time: Long, duration_in_minute: Int)

  def ic_conf(): org.apache.ignite.configuration.IgniteConfiguration = {
    val spi = new org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi()
    val ipFinder = new org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder(true)
    val attr = java.util.Arrays.asList("192.168.1.101")
    //val attr = java.util.Arrays.asList("10.1.41.244", "10.1.41.245", "10.1.41.246", "10.1.41.247", "10.1.41.248", "10.1.41.249")
    ipFinder.setAddresses(attr)
    spi.setIpFinder(ipFinder)
    val marshaller = new org.apache.ignite.marshaller.optimized.OptimizedMarshaller()
    //val marshaller = new org.apache.ignite.internal.binary.BinaryMarshaller
    marshaller.setRequireSerializable(false)
    val cfg = new org.apache.ignite.configuration.IgniteConfiguration()
    cfg.setDiscoverySpi(spi)
    cfg.setMarshaller(marshaller)
    cfg.setClientMode(true);
    cfg
  }

  def ic_cache(cache_name: String, max_size_in_gb: Integer): org.apache.ignite.configuration.CacheConfiguration[String, Any] = {
    val cache_cfg = new CacheConfiguration[String, Any](cache_name)
    cache_cfg.setAtomicityMode(org.apache.ignite.cache.CacheAtomicityMode.ATOMIC)
    cache_cfg.setCacheMode(org.apache.ignite.cache.CacheMode.PARTITIONED)
    cache_cfg.setAtomicWriteOrderMode(org.apache.ignite.cache.CacheAtomicWriteOrderMode.CLOCK)
    cache_cfg.setMemoryMode(org.apache.ignite.cache.CacheMemoryMode.OFFHEAP_TIERED)
    cache_cfg.setOffHeapMaxMemory(max_size_in_gb * 1024L * 1024L * 1024L)
    cache_cfg
  }

  val ic = new IgniteContext[String, String](sc, () => ic_conf())
  val ignite_instance = org.apache.ignite.Ignition.ignite();


  var subscribers = new HashMap[String, String]
  val pool = java.util.concurrent.Executors.newFixedThreadPool(50)
  var ws = new WebSocket(java.net.URI.create("ws://192.168.1.101:8889/so_bybu/"))
  //var ws = new WebSocket(java.net.URI.create("ws://10.1.54.8:8080/so_bybu/"))
  val subscribed_categories = new HashMap[String, String]
  val subscribed_subcategories = new HashMap[String, String]

  //--resolve_dataready("info:dataready:subcategory:inc:5df21b01"), the data ready messages that comes from job #1
  def resolve_dataready(message: String): paramProp = {
    //info:dataready:subcategory:inc
    val ar = message.split(":")
    val r = paramProp(ar(2), ar(3).replace("_", ""), "", "", (new java.util.Date().getTime().toInt), 120)
    r
  }

  //resolve_sublayer("info:client:page=subcategory:bu=inc|category=CPU")
  //resolve_sublayer("info:client:page=item:bu=inc|subcategory=Monitors - Touchscreen|category=Monitors")
  //resolve_sublayer("info:client:page=subcategory:bu=inc|category=VGA")
  //resolve_sublayer("info:client:page=querycateitem:bu=inc|category=VGA|token=123456")
  def resolve_sublayer(message: String): paramProp = {
    val ar = message.split(":")
    var bu = ""
    var category = ""
    var subcategory = ""
    var page = ar(2).replace("page=", "")
    var token = ar(3)
    if (ar(3) != "") {
      val br = ar(3).split("\\|")
      br.foreach(el => {
        val cr = el.split("=")
        el match {
          case s if s.startsWith("bu") => bu = s.split("=")(1)
          case s if s.startsWith("category") => category = s.split("=")(1)
          case s if s.startsWith("subcategory") => subcategory = s.split("=")(1)
          case s if s.startsWith("token") => token = s.split("=")(1)
          case _ => ""
        }
      })
    }

    //var token = token(new java.util.Date().getTime().toInt)

    //val r = paramProp(page, bu, category, subcategory, token(new java.util.Date().getTime().toInt), 120)
    val r = paramProp(page, bu, category, subcategory, token(new java.util.Date().getTime().toInt), 120)
    r
  }

  val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd")
  val formatter2 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def proc_dataready_hiscategory(bu: String): String = {
    try {
      val L1D = formatter.format(new java.util.Date().getTime() - 86400000 * 1)
      val L7D = formatter.format(new java.util.Date().getTime() - 86400000 * 7)
      val L14D = formatter.format(new java.util.Date().getTime() - 86400000 * 14)
      val L28D = formatter.format((new java.util.Date().getTime() - 86400000 * 14) - 86400000 * 14)
      //overday
      val L8D = formatter.format(new java.util.Date().getTime() - 86400000 * 8)
      val L15D = formatter.format(new java.util.Date().getTime() - 86400000 * 15)
      val L29D = formatter.format((new java.util.Date().getTime() - 86400000 * 14) - 86400000 * 15)
      val timer = formatter2.format(new java.util.Date().getTime()).substring(11, 16).trim()

      val hisfile_category = sc.textFile("hdfs://nameservice1/user/sparkrealtime/sparkso_history_DB05/category/suma/" + bu + "/")
      // val hisfile_category=sc.textFile("hdfs://nameservice1/user/sparkrealtime/sparkso_history_DB05/category/suma/inc/")
      val hisincoming_category = hisfile_category.map(line => (line.split(",")))

      val hisar_category = hisincoming_category.map(n => (n(0), n(1), n(2), n(3), n(5), n(10))).filter(_._4 == "Valid").filter(_._2 != L7D).filter(_._2 != L14D).filter(_._2 != L28D).filter(_._2 != L8D).filter(_._2 != L15D).filter(_._2 != L29D).filter(_._3 <= timer).collect.toArray
      val category_history = hisar_category.map { case (v1, v2, v3, v4, v5, v6) => ((v1, v2, v6), v5.toDouble) }.groupBy(_._1).map { case (k, v) => (k, (
        v.map(_._2).sum
        ))
      }.map { case ((v1, v2, v3), (k1)) => Array(v2, v3, "%.2f".format(k1)).mkString(",") }.mkString(";")

      val category_hishour = hisar_category.map { case (v1, v2, v3, v4, v5, v6) => ((v2.substring(0, 10), v3.substring(0, 2)), v5.toDouble) }.groupBy(_._1).map { case (k, v) => (k, (
        v.map(_._2).sum
        ))
      }.map { case ((v1, v2), (k1)) => Array(v1, v2, "%.2f".format(k1)).mkString(",") }.mkString(";")

      category_history + "','cathourinfo':'" + category_hishour

    }
  }

  // proc_dataready_hiscategory("inc")

  var source_cache = new HashMap[String, Array[(String, String, String, String, Double, Double, Int, Double, Double, Double, String, String, String, String, String, String, String, Double)]]

  def proc_dataready_source(bu: String): String = {
    try {
      val ignite_source = ignite_instance.getOrCreateCache[String, Any](ic_cache("SOJobCache", 2)).get("item_" + bu)
      val ar = ignite_source.asInstanceOf[Array[(String, String, String, String, Double, Double, Int, Double, Double, Double, String, String, String, String, String, String, String, Double)]]
      if (ar.length > 0) source_cache.put(bu, ar.filter(_._3 != "COM").filter(_._3 != "NULL"))
      println("source[" + bu + "] reinitialized")
    } catch {
      case e: Throwable => {
        println(e)
      }
    }
    "done"
  }

  ( /*
    Input trans_byitem_list:
     01 Hour
     02 Minute
     03 item
     04 status
     05 SOAmount
     06 GrossProductMargin
     07 Quantity
     08 Discount
     09 EIMS
     10 item_Shippingcharge
     11  Hour:Min
     12 category
     13 subcategory
     14 description
     15 grp_code
     16 domain
     17 sonumberlist
     18 eims_draft
*/ )

  def proc_dataready_category_list(bu: String): String = {
    val mission = "category"
    val history_r = proc_dataready_hiscategory(bu)
    if (!source_cache.contains(bu)) proc_dataready_source(bu)
    val ar_category = source_cache(bu)

    val category_kpi = ar_category.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v4, v12, v16), v5.toDouble, v6.toDouble, v7.toInt, v8.toDouble, v9.toDouble, v10.toDouble, v18.toDouble) }.groupBy(_._1).map { case (k, v) => (k, (
      v.map(_._2).sum,
      v.map(_._3).sum,
      v.map(_._4).sum,
      v.map(_._5).sum,
      v.map(_._6).sum,
      v.map(_._7).sum,
      v.map(_._8).sum
      ))
    }.toMap

    val category_order = ar_category.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v4, v12, v16), (v17.split("#"))) }.flatMap { case (c, innerList) => innerList.map(c -> _) }.distinct.groupBy(_._1).mapValues(_.size).toMap

    val category_current = (for ((k, v) <- category_kpi) yield (k ->(category_order.getOrElse(k, 0), v))).map { case ((k1, k2, k3), (v1, (w1, w2, w3, w4, w5, w6, w7))) => (k2, k1, "%.2f".format(w1), "%.2f".format(w2), w3, "%.2f".format(w4), "%.2f".format(w5), v1, "%.2f".format(w6), k3, "%.2f".format(w7)) }.mkString(";").replace(")", "").replace("(", "").replace("\"", "-inches").replace("''", "-inches").replace("'", "")
    val r = "client:category:" + bu + ":{'catinfo':'" + category_current + "','cathisinfo':'" + history_r + "'}"
    if (category_current != "") ws.send(r)
    "done"
  }

  //proc_dataready_category_list("inc")

  ( /*
        Output sotransaction_bycategory:
         01 Hour
         02 Minute
         03 category
         04 status
         05 SOAmount
         06 GrossProductMargin
         07 Quantity
         08 Discount
         09 EIMS
         10 Order
         11 item_Shippingcharge
         12 Hour:min
         13 Domain
    */ )

  def proc_dataready_category_single(bu: String, category_name: String): String = {
    try {
      val mission = "category"
      if (!source_cache.contains(bu)) proc_dataready_source(bu)
      val ar_category = source_cache(bu).filter(_._12 == category_name)

      val category_recent = ar_category.filter(_._4 == "Valid").map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v11), v5.toDouble) }.groupBy(_._1).map { case (k, v) => (k, (
        "%.2f".format(v.map(_._2).sum)
        ))
      }.toArray.sortBy(_._1)(Ordering[String].reverse).take(30).mkString(";").replace(")", "").replace("(", "")

      val category_kpi1 = ar_category.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v1, v4, v12, v16), v5.toDouble, v6.toDouble, v7.toInt, v8.toDouble, v9.toDouble, v10.toDouble, v18.toDouble) }.groupBy(_._1).map { case (k, v) => (k, (
        v.map(_._2).sum,
        v.map(_._3).sum,
        v.map(_._4).sum,
        v.map(_._5).sum,
        v.map(_._6).sum,
        v.map(_._7).sum,
        v.map(_._8).sum
        ))
      }.toMap
      val category_order1 = ar_category.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v1, v4, v12, v16), (v17.split("#"))) }.flatMap { case (c, innerList) => innerList.map(c -> _) }.distinct.groupBy(_._1).mapValues(_.size).toMap
      val category_hour = (for ((k, v) <- category_kpi1) yield (k ->(category_order1.getOrElse(k, 0), v))).map { case ((k1, k2, k3, k4), (v1, (w1, w2, w3, w4, w5, w6, w7))) => (k1, k3, k2, "%.2f".format(w1), "%.2f".format(w2), w3, "%.2f".format(w4), "%.2f".format(w5), v1, "%.2f".format(w6), "%.2f".format(w7)) }.mkString(";").replace(")", "").replace("(", "").replace("\"", "-inches").replace("''", "-inches").replace("'", "")

      val category_kpi2 = ar_category.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v4, v12, v16), v5.toDouble, v6.toDouble, v7.toInt, v8.toDouble, v9.toDouble, v10.toDouble, v18.toDouble) }.groupBy(_._1).map { case (k, v) => (k, (
        v.map(_._2).sum,
        v.map(_._3).sum,
        v.map(_._4).sum,
        v.map(_._5).sum,
        v.map(_._6).sum,
        v.map(_._7).sum,
        v.map(_._8).sum
        ))
      }.toMap
      val category_order2 = ar_category.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v4, v12, v16), (v17.split("#"))) }.flatMap { case (c, innerList) => innerList.map(c -> _) }.distinct.groupBy(_._1).mapValues(_.size).toMap
      val category_current = (for ((k, v) <- category_kpi2) yield (k ->(category_order2.getOrElse(k, 0), v))).map { case ((k1, k2, k3), (v1, (w1, w2, w3, w4, w5, w6, w7))) => (k2, k1, "%.2f".format(w1), "%.2f".format(w2), w3, "%.2f".format(w4), "%.2f".format(w5), v1, "%.2f".format(w6), "%.2f".format(w7)) }.mkString(";").replace(")", "").replace("(", "").replace("\"", "-inches").replace("''", "-inches").replace("'", "")

      val r = "client:subcategory:" + bu + ":" + category_name + ":{'recent':'" + category_recent + "','day':'" + category_hour + "','current':'" + category_current + "'}"
      ws.send(r)
    } catch {
      case e: Throwable => {
        println(e)
      }
    }
    "done"
  }

  // proc_dataready_category_single("inc","CPU")

  ( /*
        Output sotransaction_byitem_rst:
         01 Hour
         02 Minute
         03 subcategory
         04 status
         05 SOAmount
         06 GrossProductMargin
         07 Quantity
         08 Discount
         09 EIMS
         10 Order
         11 item_Shippingcharge
         12 Hour:Min
         13 category
    */ )

  def proc_dataready_subcategory_list(bu: String, category_name: String): String = {
    val mission = "subcategory"
    val key = bu + ":" + category_name;
    if (!source_cache.contains(bu)) proc_dataready_source(bu)
    val ar_subcategory = source_cache(bu).filter(_._12 == category_name)
    val subcategory_kpi = ar_subcategory.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v4, v13, v12), v5.toDouble, v6.toDouble, v7.toInt, v8.toDouble, v9.toDouble, v10.toDouble, v18.toDouble) }.groupBy(_._1).map { case (k, v) => (k, (
      v.map(_._2).sum,
      v.map(_._3).sum,
      v.map(_._4).sum,
      v.map(_._5).sum,
      v.map(_._6).sum,
      v.map(_._7).sum,
      v.map(_._8).sum
      ))
    }.toMap
    val subcategory_order = ar_subcategory.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v4, v13, v12), (v17.split("#"))) }.flatMap { case (c, innerList) => innerList.map(c -> _) }.distinct.groupBy(_._1).mapValues(_.size).toMap
    val subcategory_current = (for ((k, v) <- subcategory_kpi) yield (k ->(subcategory_order.getOrElse(k, 0), v))).map { case ((k1, k2, k3), (v1, (w1, w2, w3, w4, w5, w6, w7))) => (k2, k3, k1, "%.2f".format(w1), "%.2f".format(w2), w3, "%.2f".format(w4), "%.2f".format(w5), v1, "%.2f".format(w6), "%.2f".format(w7)) }.mkString(";").replace(")", "").replace("(", "").replace("\"", "-inches").replace("''", "-inches").replace("'", "")

    val r = "client:subcategory:" + bu + ":" + category_name + ":{'subcatinfo':'" + subcategory_current + "'}"
    //println(r)
    ws.send(r)
    "done"
  }

  //proc_dataready_subcategory_list("inc","Monitors")

  def proc_dataready_subcategory_single(bu: String, category_name: String, subcategory_name: String): String = {
    val mission = "subcategory"
    val key = bu + ":" + category_name;
    if (!source_cache.contains(bu)) proc_dataready_source(bu)
    val ar_subcategory = source_cache(bu).filter(_._12 == category_name).filter(_._13 == subcategory_name)

    val subcategory_recent = ar_subcategory.filter(_._4 == "Valid").map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v11), v5.toDouble) }.groupBy(_._1).map { case (k, v) => (k, (
      "%.2f".format(v.map(_._2).sum)
      ))
    }.toArray.sortBy(_._1)(Ordering[String].reverse).take(30).mkString(";").replace(")", "").replace("(", "")

    val subcategory_kpi1 = ar_subcategory.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v1, v4, v12, v13), v5.toDouble, v6.toDouble, v7.toInt, v8.toDouble, v9.toDouble, v10.toDouble, v18.toDouble) }.groupBy(_._1).map { case (k, v) => (k, (
      v.map(_._2).sum,
      v.map(_._3).sum,
      v.map(_._4).sum,
      v.map(_._5).sum,
      v.map(_._6).sum,
      v.map(_._7).sum,
      v.map(_._8).sum
      ))
    }.toMap
    val subcategory_order1 = ar_subcategory.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v1, v4, v12, v13), (v17.split("#"))) }.flatMap { case (c, innerList) => innerList.map(c -> _) }.distinct.groupBy(_._1).mapValues(_.size).toMap
    val subcategory_hour = (for ((k, v) <- subcategory_kpi1) yield (k ->(subcategory_order1.getOrElse(k, 0), v))).map { case ((k1, k2, k3, k4), (v1, (w1, w2, w3, w4, w5, w6, w7))) => (k1, k4, k3, k2, "%.2f".format(w1), "%.2f".format(w2), w3, "%.2f".format(w4), "%.2f".format(w5), v1, "%.2f".format(w6), "%.2f".format(w7)) }.mkString(";").replace(")", "").replace("(", "").replace("\"", "-inches").replace("''", "-inches").replace("'", "")

    val subcategory_kpi2 = ar_subcategory.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v4, v12, v13), v5.toDouble, v6.toDouble, v7.toInt, v8.toDouble, v9.toDouble, v10.toDouble, v18.toDouble) }.groupBy(_._1).map { case (k, v) => (k, (
      v.map(_._2).sum,
      v.map(_._3).sum,
      v.map(_._4).sum,
      v.map(_._5).sum,
      v.map(_._6).sum,
      v.map(_._7).sum,
      v.map(_._8).sum
      ))
    }.toMap
    val subcategory_order2 = ar_subcategory.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v4, v12, v13), (v17.split("#"))) }.flatMap { case (c, innerList) => innerList.map(c -> _) }.distinct.groupBy(_._1).mapValues(_.size).toMap
    val subcategory_current = (for ((k, v) <- subcategory_kpi2) yield (k ->(subcategory_order2.getOrElse(k, 0), v))).map { case ((k1, k2, k3), (v1, (w1, w2, w3, w4, w5, w6, w7))) => (k2, k3, k1, "%.2f".format(w1), "%.2f".format(w2), w3, "%.2f".format(w4), "%.2f".format(w5), v1, "%.2f".format(w6), "%.2f".format(w7)) }.mkString(";").replace(")", "").replace("(", "").replace("\"", "-inches").replace("''", "-inches").replace("'", "")
    //
    val r = "client:item:" + bu + ":" + category_name + ":" + subcategory_name + ":{'recent':'" + subcategory_recent + "','day':'" + subcategory_hour + "','current':'" + subcategory_current + "'}"
    //if (bu=="inc") {println(r)}
    ws.send(r)
    "done"
  }

  //proc_dataready_subcategory_single("inc","Monitors","Monitors - Touchscreen")
  ( /*
        Output sotransaction_byitem_rst:
         01 Hour
         02 Minute
         03 item
         04 status
         05 SOAmount
         06 GrossProductMargin
         07 Quantity
         08 Discount
         09 EIMS
         10 Order
         11 item_Shippingcharge
         12  Hour:Min
         13 category
         14 subcategory
         15 description
         16 grp_code
    */ )

  def proc_dataready_item_list(bu: String, category_name: String, subcategory_name: String): String = {
    val mission = "item"
    try {
      if (!source_cache.contains(bu)) proc_dataready_source(bu)
      val ar_item = source_cache(bu).filter(_._12 == category_name).filter(_._13 == subcategory_name)
      // val ar_item=ar.filter(_._3=="74-103-354")
      val item_kpi = ar_item.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v4, v3, v12, v13, v14, v15), v5.toDouble, v6.toDouble, v7.toInt, v8.toDouble, v9.toDouble, v10.toDouble, v18.toDouble) }.groupBy(_._1).map { case (k, v) => (k, (
        v.map(_._2).sum,
        v.map(_._3).sum,
        v.map(_._4).sum,
        v.map(_._5).sum,
        v.map(_._6).sum,
        v.map(_._7).sum,
        v.map(_._8).sum
        ))
      }.toMap

      val item_order = ar_item.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v4, v3, v12, v13, v14, v15), (v17.split("#"))) }.flatMap { case (c, innerList) => innerList.map(c -> _) }.distinct.groupBy(_._1).mapValues(_.size).toMap

      //val item_current=(for ( (k,v) <- item_kpi ) yield (k -> (item_order.getOrElse(k,0),v))).map{ case((k1,k2,k3,k4,k5,k6),(v1,(w1,w2,w3,w4,w5,w6,w7))) => (k2,k3,k4,k1,"%.2f".format(w1),"%.2f".format(w2),w3,"%.2f".format(w4),"%.2f".format(w5),v1,"%.2f".format(w6),java.net.URLEncoder.encode(k5,"UTF-8"),k6,"%.2f".format(w7))}.mkString(";").replace(")","").replace("(","")
      val item_current = (for ((k, v) <- item_kpi) yield (k ->(item_order.getOrElse(k, 0), v))).map { case ((k1, k2, k3, k4, k5, k6), (v1, (w1, w2, w3, w4, w5, w6, w7))) => (k2, k3, k4, k1, "%.2f".format(w1), "%.2f".format(w2), w3, "%.2f".format(w4), "%.2f".format(w5), v1, "%.2f".format(w6), k5, k6, "%.2f".format(w7)) }.mkString(";").replace(")", "").replace("(", "")
      val r = "client:item:" + bu + ":" + category_name + ":" + subcategory_name + ":{'iteminfo':'" + item_current + "'}"
      //if (bu=="inc") {println(r)}
      ws.send(r)
    } catch {
      case e: Throwable => {
        println(e)
      }
    }
    "done"
  }

  //proc_dataready_item_list("inc","Monitors","Monitors - Touchscreen")

  def proc_dataready_categoryitem_list(bu: String, category_name: String, token: String): String = {
    val mission = "topcateitem"
    try {
      if (!source_cache.contains(bu)) proc_dataready_source(bu)
      val ar_cateitem = source_cache(bu).filter(_._12 == category_name)
      val cateitem_kpi = ar_cateitem.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v4, v3, v12, v13, v14, v15), v5.toDouble, v6.toDouble, v7.toInt, v8.toDouble, v9.toDouble, v10.toDouble, v18.toDouble) }.groupBy(_._1).map { case (k, v) => (k, (
        v.map(_._2).sum,
        v.map(_._3).sum,
        v.map(_._4).sum,
        v.map(_._5).sum,
        v.map(_._6).sum,
        v.map(_._7).sum,
        v.map(_._8).sum
        ))
      }.toMap

      val cateitem_order = ar_cateitem.map { case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18) => ((v4, v3, v12, v13, v14, v15), (v17.split("#"))) }.flatMap { case (c, innerList) => innerList.map(c -> _) }.distinct.groupBy(_._1).mapValues(_.size).toMap

      val cateitem_current = (for ((k, v) <- cateitem_kpi) yield (k ->(cateitem_order.getOrElse(k, 0), v))).map { case ((k1, k2, k3, k4, k5, k6), (v1, (w1, w2, w3, w4, w5, w6, w7))) => (k2, k3, k4, k1, "%.2f".format(w1).toDouble, "%.2f".format(w2), w3, "%.2f".format(w4), "%.2f".format(w5), v1, "%.2f".format(w6), k5, k6, "%.2f".format(w7)) }

      val cateitem_top1000 = cateitem_current.toList.sortBy(_._5)(Ordering[Double].reverse).take(1000).mkString(";").replace(")", "").replace("(", "")
      val r = "client:topcateitem:" + bu + ":" + category_name + ":" + token + ":{'iteminfo':'" + cateitem_top1000 + "'}"
      //if (bu=="inc") {println(r)}
      ws.send(r)
    } catch {
      case e: Throwable => {
        println(e)
      }
    }
    "done"
  }

  //proc_dataready_categoryitem_list("inc","Monitors","123456")

  def userRequest(data: String) {
    println(data)
    if (data.startsWith("info:dataready:item")) {
      val source = resolve_dataready(data)
      pool.submit(new Callable[String] {
        def call() = {
          proc_dataready_source(source.bu)
          proc_dataready_category_list(source.bu)
        }
      })
      //inc:VGA:VGA Gifts
      subscribed_categories.keys.foreach(el => {
        if (source.bu == el.split(":")(0)) {
          // println("processing["+el+"]...................................................")
          pool.submit(new Callable[String] {
            def call() = {
              proc_dataready_category_single(el.split(":")(0), el.split(":")(1))
            }
          })
        }
      });

      subscribed_categories.keys.foreach(el => {
        if (source.bu == el.split(":")(0)) {
          // println("processing["+el+"]...................................................")
          pool.submit(new Callable[String] {
            def call() = {
              proc_dataready_subcategory_list(el.split(":")(0), el.split(":")(1))
            }
          })
        }
      });
      //inc:VGA:VGA Gifts
      subscribed_subcategories.keys.foreach(el => {
        if (source.bu == el.split(":")(0)) {
          // println("processing["+el+"].......................................................")
          pool.submit(new Callable[String] {
            def call() = {
              proc_dataready_subcategory_single(el.split(":")(0), el.split(":")(1), el.split(":")(2))
            }
          })
        }
      });

      subscribed_subcategories.keys.foreach(el => {
        if (source.bu == el.split(":")(0)) {
          // println("processing["+el+"]...................................................")
          pool.submit(new Callable[String] {
            def call() = {
              proc_dataready_item_list(el.split(":")(0), el.split(":")(1), el.split(":")(2))
            }
          })
        }
      });
    }

    if (data.startsWith("info:clear:subcategories:")) {
      //val data="info:clear:subcategories:inc:CPU"
      val ar = data.split(":")
      val key = ar(3) + ":" + ar(4)
      println("queue clear -> subcategories of " + key);
      subscribed_categories.remove(key)
      //subscribed_categories.keys.foreach(println)
    }
    if (data.startsWith("info:clear:items:")) {
      //val data="info:clear:items:inc:VGA:Video Card - Nvidia"
      val ar = data.split(":")
      val key = ar(3) + ":" + ar(4) + ":" + ar(5)
      println("queue clear -> items of " + key);
      subscribed_subcategories.remove(key)
      //subscribed_subcategories.keys.foreach(el=>{println("item->"+el)})
    }

    if (data.startsWith("info:client:page=subcategory")) {
      //--val source=resolve_sublayer("info:client:page=subcategory:bu=inc|category=VGA")
      val source = resolve_sublayer(data)
      //add parent categories to a store for periodical data refresh
      val key = source.bu + ":" + source.category
      subscribed_categories.put(key, source.mission)
      subscribed_categories.keys.foreach(println)
      pool.submit(new Callable[String] {
        def call() = {
          proc_dataready_category_single(source.bu, source.category)
          proc_dataready_subcategory_list(source.bu, source.category)
        }
      })
    }

    if (data.startsWith("info:client:page=querycateitem")) {
      //--val source=resolve_sublayer("info:client:page=querycateitem:bu=inc|category=VGA")
      val source = resolve_sublayer(data)
      //add parent categories to a store for periodical data refresh
      pool.submit(new Callable[String] {
        def call() = {
          proc_dataready_categoryitem_list(source.bu, source.category,source.start_time.toString)
        }
      })

    }

    if (data.startsWith("info:client:page=item")) {
      //info:client:page=item:bu=inc|subcategory=TV Brackets
      //--val source=resolve_sublayer("info:client:page=item:bu=inc|category=VGA|subcategory=Video Card - Nvidia")
      println("client req -> item");
      val source = resolve_sublayer(data)
      //add parent categories to a store for periodical data refresh
      val key = source.bu + ":" + source.category + ":" + source.subcategory
      subscribed_subcategories.put(key, source.mission)
      //subscribed_subcategories.keys.foreach(el=>{println("item->"+el)})
      pool.submit(new Callable[String] {
        def call() = {
          proc_dataready_subcategory_single(source.bu, source.category, source.subcategory)
          proc_dataready_item_list(source.bu, source.category, source.subcategory)
        }
      })
    }
  }

  def init_ws() {
    ws.setEventHandler(new WebSocketEventHandler() {
      def onOpen() {
        println("websocket connected")
      }

      def onMessage(message: WebSocketMessage) {
        //println(message.getText());
        userRequest(message.getText());
      }

      def onClose() {
        println("connection lost, re-try in 5 seconds...")
        //Thread.sleep(5000)
        //10.1.54.8:8080
        //192.168.1.101:8889
        ws = new WebSocket(java.net.URI.create("ws://192.168.1.101:8889/so_bybu/"))
        //ws = new WebSocket(java.net.URI.create("ws://10.1.54.8:8080/so_bybu/"))
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

}
