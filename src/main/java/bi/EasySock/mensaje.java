package bi.EasySock;

public class mensaje{
    public String messageContext;
    public messagetypeset messagetype=messagetypeset.unknown;
    public connectiontypeset connectiontype=connectiontypeset.unknown; //server,client,info
    public String querytype=""; //item,category,subcategory
    public String bu="";
    //_inc/usa/b2b/smb/whosale/can
    public String invoke_ip="";
    //invoke ip address
    public String data="";
    public String parent="";
    public String preparent="";
    public String token="";
    public String grp_code="";
    public String ctlg_code="";

    public mensaje(String message){
        this.messageContext=message;
        this.parse(message);
    }
    private void parse(String message){
        String ar[]=message.split(":");
        //client:dashboard:gis:usa:
        //client:overview:_inc
        if(message.toLowerCase().startsWith("client:overview:")){
            this.messagetype=messagetypeset.bu_overview;
            this.connectiontype=connectiontypeset.client;
            this.bu=ar[2].replace("_","");
        }else if (message.toLowerCase().startsWith("client:category:")){
            //client:category:inc
            this.messagetype=messagetypeset.bu_category;
            this.connectiontype=connectiontypeset.client;
            this.bu=ar[2].replace("_","");

        }else if (message.toLowerCase().startsWith("client:subcategory:")){
            //client:subcategory:inc:CPU
            this.messagetype=messagetypeset.bu_subcategory;
            this.connectiontype=connectiontypeset.client;
            this.bu=ar[2].replace("_","");

            this.parent=ar[3];
        }else if (message.toLowerCase().startsWith("client:item:")){
            //client:item:inc:CPU Gifts
            this.messagetype=messagetypeset.bu_item;
            this.connectiontype=connectiontypeset.client;
            this.bu=ar[2].replace("_","");

            this.parent=ar[4];
            this.preparent=ar[3];
        }else if (message.toLowerCase().startsWith("client:topcateitem:")){
            //client:subcategory:inc:CPU
            this.messagetype=messagetypeset.bu_topcateitem;
            this.connectiontype=connectiontypeset.client;
            this.bu=ar[2].replace("_","");

            this.parent=ar[3];
            this.token=ar[4];
        }else if(message.toLowerCase().startsWith("client:status:running")){
            //client:status:running:2015-09-11
            this.messagetype=messagetypeset.runningstatus;
            this.connectiontype=connectiontypeset.client;
        }else if(message.toLowerCase().startsWith("client:dashboard:gis")){
            this.messagetype=messagetypeset.bu_gis;
            //client:dashboard:gis:usa:
        }else if(message.toLowerCase().startsWith("client:map:gis:usa_states")){
            this.messagetype=messagetypeset.gis_usa_states;
        }else if(message.toLowerCase().startsWith("client:map:gis:can_states")){
            this.messagetype=messagetypeset.gis_can_states;
        }else if(message.toLowerCase().startsWith("client:map:gis:usa_counties")){
            this.messagetype=messagetypeset.gis_usa_counties;
        }else if(message.toLowerCase().startsWith("client:buybox:items:")){
            this.messagetype=messagetypeset.itemalarm;
            this.connectiontype=connectiontypeset.client;
        }
        else if(message.toLowerCase().startsWith("client:buybox:ap:itempriceinfo:")){
            this.messagetype=messagetypeset.ap_itemalarm_client;
            this.connectiontype=connectiontypeset.client;
        }
        else if(message.toLowerCase().startsWith("client:buybox:ap:")){
            this.messagetype=messagetypeset.ap_itemalarm_server;
            this.connectiontype=connectiontypeset.server;
        }
        else if(message.toLowerCase().startsWith("client:buybox:rst_summary:")){
            //"client:buybox:tree_source:12345:"+tree_source
            this.messagetype=messagetypeset.buybox_rst_summary;
            this.connectiontype=connectiontypeset.client;
            this.token=ar[3];
        }else if(message.toLowerCase().startsWith("client:buybox:rst_subcategory:")){
            //"client:buybox:rst_subcategory:"+token+":"+r
            this.messagetype=messagetypeset.buybox_rst_subcategory;
            this.connectiontype=connectiontypeset.client;
            this.token=ar[3];
        }else if(message.toLowerCase().startsWith("client:")){
            this.messagetype=messagetypeset.server2client;
            this.connectiontype=connectiontypeset.client;
            this.querytype=ar[1];
            this.invoke_ip=ar[2];
            this.data=ar[ar.length-1];
        }

        //so retriever
        if (message.toLowerCase().startsWith("generator:")){
            this.messagetype=messagetypeset.soretriever;
            this.connectiontype=connectiontypeset.unknown;
        }
        //info:client:page:homepage
        if(message.toLowerCase().startsWith("info:client:page")){
            this.messagetype=messagetypeset.clientregistration;
            this.connectiontype=connectiontypeset.info;
        }else if(message.toLowerCase().startsWith("info:dataready")){
            //info:dataready
            this.messagetype=messagetypeset.dataready;
            this.connectiontype=connectiontypeset.server;
            this.querytype=ar[2];
            this.bu=ar[3];
        }else if(message.toLowerCase().startsWith("info:users")){
            //info:dataready
            this.messagetype=messagetypeset.users;
            this.connectiontype=connectiontypeset.server;
        }

        if(message.toLowerCase().startsWith("server:")){
            //server:item:_inc:20-233-383|;|34-735-042|;|17-994-162|;|20-231-122|;|30-950-020:192.168.1.101
            this.messagetype=messagetypeset.client2server;
            this.connectiontype=connectiontypeset.server;
            this.querytype=ar[1];
            this.bu=ar[2].replace("_","");
            this.invoke_ip=ar[ar.length-1];
        }

        if(message.toLowerCase().startsWith("monitor:kafka:itemalarm:")){
            this.messagetype=messagetypeset.itemalarm;
            this.connectiontype=connectiontypeset.client;
        }

    }


    public enum connectiontypeset {
        server,client,info,unknown
    }
}
