package bi.ignite

import java.io._
import java.util.Calendar

// ar(0), ar(1), item_of_usa, rank, dt, item_desc, domain, grp_code, category, ctlg_code,
// subcategory, 1, q4s, warehouse_type, brand, runrate_type, OPC, VF, final_price, timestamp,
// winner_seller, winner_price, newegg_price, winner_score, newegg_score, newegg_pcode, newegg_eggpoint, newegg_mir, newegg_aatc, winner_vl,
// winner_vm, newegg_vl, newegg_vm, newegg_instock

class ItemAlarm extends Serializable {
  /** Person ID (indexed). */
  var counter:Int = 0
  var arg2:Int = 0
  var item_of_usa:String = ""
  var rank:String = ""
  var dt = Calendar.getInstance().getTime()
  var item_desc:String = ""
  var domain:String = ""
  var grp_code:Int = 0
  var category:String = ""
  var ctlg_code:Int = 0
  var subcategory:String = ""
  var qty:Int = 0
  var q4s:Int = 0
  var warehouse_type:String = ""
  var brand:String = ""
  var runrate_type:String = ""
  var OPC:String = ""
  var VF:String = ""
  var final_price:Double = 0.00
  var timestamp:String = ""
  var winner_seller:String = ""
  var winner_price:Double = 0.00
  var newegg_price:Double = 0.00
  var winner_score:Double = 0.00
  var newegg_score:Double = 0.00
  var newegg_pcode:Double = 0.00
  var newegg_eggpoint:Double = 0.00
  var newegg_mir:Double = 0.00
  var newegg_aatc:Double = 0.00
  var winner_vl:Double = 0.00
  var winner_vm:Double = 0.00
  var newegg_vl:Double = 0.00
  var newegg_vm:Double = 0.00
  var newegg_instock:String = ""

}
