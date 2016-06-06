package bi.info;

/**
 * Created by root on 4/6/16.
 *
 var item = [
 { "firstName":"Bill" , "lastName":"Gates" },
 { "firstName":"George" , "lastName":"Bush" },
 { "firstName":"Thomas" , "lastName": "Carter" }
 ];

 USA,9SIA67S2181555,22-136-279,1,2016-01-18 11:01:29,TECH EXPERTS,90.29,93.990,0.9195907007855459544383346426,0.9108652286724273369992144540,0.0,00,0.0
 Biz Unit,winner item#,newegg item#,newegg item position,timestamp,winner seller name, winner price,newegg price, winner score, newegg score,newegg pcode,
 newegg eggpoint, newegg mir, newegg AATC

 val itemJson=gson.toJson(itemInstance(item.country,item.item_of_mktpl,item.item_of_newegg,item.rank,formatter.format(item.dt.getTime()),item.item_desc,item.domain,item.grp_code,item.category,item.ctlg_code,item.subcategory,item.counter,item.q4s,item.warehouse_type,item.brand,item.runrate_type,item.neweggwh_only_avail,item.vf_only_avail,item.final_price))

 {
 "Biz Unit": "USA",
 "winner item#": "9SIA67S2181555",
 "newegg item#": "22-136-279",
 "newegg item position": "1",
 "timestamp": "2016-01-18 11:01:29",
 "winner seller name": "TECH EXPERTS",
 "winner price": "90.29",
 "winner score": "93.990",
 "newegg pcode": "0.9195907007855459544383346426",
 "newegg eggpoint": "0.9108652286724273369992144540",
 "newegg mir": "0.0",
 "newegg AATC": "0.0"
 }

 */
public class itemREST {

    public String getItemExtend (String item){

        return "";

    }
    public String getItemDesc (String item){

        return "";
    }
}
