package openstacktopo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

//import net.sf.json.JSONObject;
//https://sourceforge.net/projects/json-lib/files/json-lib/json-lib-2.4/
public class Topo {
	static {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static Connection getConnection(String db) {
		//String url = "jdbc:mysql://192.168.139.251:3306/"+db;
		String url = "jdbc:mysql://localhost:3306/"+db;
		String username = "root";
		String password = "haoning";
		Connection con = null;
		try {
			con = DriverManager.getConnection(url, username, password);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return con;
	}
	public  Map getInstances() {//获取实例
		Map vm_processes=new HashMap();
		ResultSet rs = null;
		Connection con = null;
		try {
			con = getConnection("nova");
			PreparedStatement ps = null;
			String sql = "select i.uuid,i.display_name,i.launched_on,i.vm_state from  instances i where  deleted =0 ";
			ps = (PreparedStatement) con.prepareStatement(sql);
			if (ps.execute()) {	
				rs = ps.getResultSet();
				StringBuffer b = new StringBuffer();
				while (rs.next()) {
					JSONObject instance=new JSONObject();
					JSONObject metadata=new JSONObject();
					//System.out.println(rs.getString("uuid")+"\t");
					//System.out.println(rs.getString("display_name")+"\t");
					instance.put("component", "vm/"+rs.getString("uuid"));
					metadata.put("type", "vm");
					metadata.put("vm_id", rs.getString("uuid"));
					metadata.put("vm_status", rs.getString("vm_state"));
					metadata.put("label", rs.getString("display_name"));
					instance.put("metadata", metadata);
					//System.out.println(instance);
					vm_processes.put("vm/"+rs.getString("uuid"), instance);
				}
			} else {
				int i = ps.getUpdateCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				rs.close();
				con.close();
			} catch (Exception e2) {}		
		}
		return vm_processes;
	}
	public  List<JSONObject> get_instance_net() {//获取vm和网络的关系
		List list_i_net=new ArrayList();
		ResultSet rs = null;
		Connection con = null;
		try {
			con = getConnection("nova");
			PreparedStatement ps = null;
			String sql = "select iic.network_info,i.uuid,i.display_name,i.launched_on from instance_info_caches iic ,  instances i where i.id=iic.id and  i.deleted = 0";
			ps = (PreparedStatement) con.prepareStatement(sql);
			if (ps.execute()) {
				rs = ps.getResultSet();
				StringBuffer b = new StringBuffer();
				while (rs.next()) {
					//System.out.println(rs.getString("uuid")+"\t");//vm-id
					//System.out.println(rs.getString("display_name")+"\t");//vm-name
					String network_info = rs.getString("network_info");
					//System.out.println(rs.getString("network_info"));
					JSONArray ja = new JSONArray().fromObject(network_info);
					for(int i=0;i<ja.size();i++){
						JSONObject jo= (JSONObject) ja.get(i);
						//System.out.println(jo.get("id"));//port-id ★★★★★
						JSONObject network= (JSONObject) jo.get("network");
						//System.out.println("---------");
						//System.out.println(((JSONObject)((JSONArray)((JSONObject)((JSONArray)(network.get("subnets"))).get(0)).get("ips")).get(0)).get("address"));
						//System.out.println(((JSONObject)(((JSONObject)((JSONArray)(network.get("subnets"))).get(0)).get("meta"))).get("dhcp_server"));
						String dhcp_server = (String) ((JSONObject)(((JSONObject)((JSONArray)(network.get("subnets"))).get(0)).get("meta"))).get("dhcp_server");
						String vm_address = (String) ((JSONObject)((JSONArray)((JSONObject)((JSONArray)(network.get("subnets"))).get(0)).get("ips")).get(0)).get("address");
						//System.out.println(network.get("id"));//network-id
						//System.out.println(network.get("label"));//network-name
						//System.out.println(network);
						JSONObject one_connection=new JSONObject();
						one_connection.put("src", new JSONObject().fromObject("{\"process\":\"switch/"+network.get("id")+"\",\"port\":\""+dhcp_server+"\"}"));//有port
						one_connection.put("tgt", new JSONObject().fromObject("{\"process\":\"vm/"+rs.getString("uuid")+"\",\"port\":\""+vm_address+"\"}"));
					//	System.out.println(one_connection);
						list_i_net.add(one_connection);
					}
					//System.out.println();
				}
				
			} else {
				int i = ps.getUpdateCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				rs.close();
				con.close();
			} catch (Exception e2) {}		
		}
		return list_i_net;
	}
	public  List<JSONObject> get_router_net() {//获取router和网络的关系
		List list_router_net=new ArrayList();
		ResultSet rs = null;
		Connection con = null;
		try {
			con = getConnection("neutron");
			PreparedStatement ps = null;
			String sql = "select * from routerports rp ,ports p,ipallocations i where rp.port_id=p.id and i.port_id=p.id";
			ps = (PreparedStatement) con.prepareStatement(sql);
			if (ps.execute()) {
				rs = ps.getResultSet();
				StringBuffer b = new StringBuffer();
				while (rs.next()) {
					JSONObject one_connection=new JSONObject();
					String ip_address = rs.getString("ip_address");
					if("network:router_interface".equals(rs.getString("port_type"))){
						one_connection.put("src", new JSONObject().fromObject("{\"process\":\"router/"+rs.getString("router_id")+"\",\"port\":\""+ip_address+"\"}"));
						one_connection.put("tgt", new JSONObject().fromObject("{\"process\":\"switch/"+rs.getString("network_id")+"\",\"port\":\""+"in"+"\"}"));
										
					}else{//network:router_gateway
						//one_connection.put("src", new JSONObject().fromObject("{\"process\":\"router/"+rs.getString("router_id")+"\",\"port\":\""+jo.get("port_id")+"\"}"));
						one_connection.put("src", new JSONObject().fromObject("{\"process\":\"switch/"+rs.getString("network_id")+"\",\"port\":\""+"out"+"\"}"));
						one_connection.put("tgt", new JSONObject().fromObject("{\"process\":\"router/"+rs.getString("router_id")+"\",\"port\":\""+ip_address+"\"}"));	
					}
					//System.out.println(one_connection);
					list_router_net.add(one_connection);
						
					//System.out.println();
				}
				
			} else {
				int i = ps.getUpdateCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				rs.close();
				con.close();
			} catch (Exception e2) {}		
		}
		return list_router_net;
	}
	public  Map<String,JSONObject> getRouters() {
		Map<String,JSONObject>  routers=new HashMap<String,JSONObject>();
		Connection con = null;
		ResultSet rs = null;
		try {
			con = getConnection("neutron");
			PreparedStatement ps = null;
			String sql = "select * from routers";
			ps = (PreparedStatement) con.prepareStatement(sql);
			if (ps.execute()) {
				rs = ps.getResultSet();
				StringBuffer b = new StringBuffer();
				while (rs.next()) {
					JSONObject instance=new JSONObject();
					JSONObject metadata=new JSONObject();
					instance.put("component", "router/"+rs.getString("id"));
					metadata.put("type", "router");
					metadata.put("router_id", rs.getString("id"));
					metadata.put("router_status", rs.getString("status"));
					metadata.put("label", rs.getString("name"));
					instance.put("metadata", metadata);
					//System.out.println(instance);
					routers.put("router/"+rs.getString("id"), instance);
				}
			} else {
				int i = ps.getUpdateCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				rs.close();
				con.close();
			} catch (Exception e2) {}		
		}
		return routers;
	}
	public  Map getNetworkExternal() {//获取网络是外部网络的集合
		Map external_map = new HashMap();
		Connection con = null;
		ResultSet rs = null;
		try {
			con = getConnection("neutron");
			PreparedStatement ps = null;
			String sql = "select * from networkrbacs where action='access_as_external'";//where tenant_id= scsssdfs;
			ps = (PreparedStatement) con.prepareStatement(sql);
			if (ps.execute()) {
				rs = ps.getResultSet();
				StringBuffer b = new StringBuffer();
				while (rs.next()) {
					external_map.put(rs.getString("object_id"),"access_as_external");
				}
			} else {
				int i = ps.getUpdateCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				rs.close();
				con.close();
			} catch (Exception e2) {}		
		}
		//System.out.println(external_map.size());
		return external_map;
	}
	public  Map getNetworkShared() {//获取网络是共享网络的集合
		Map shared_map = new HashMap();
		Connection con = null;
		ResultSet rs = null;
		try {
			con = getConnection("neutron");
			PreparedStatement ps = null;
			String sql = "select * from networkrbacs where action='access_as_shared'";//where tenant_id= scsssdfs;
			ps = (PreparedStatement) con.prepareStatement(sql);
			if (ps.execute()) {
				rs = ps.getResultSet();
				StringBuffer b = new StringBuffer();
				while (rs.next()) {
					shared_map.put(rs.getString("object_id"), "access_as_shared");
				}
			} else {
				int i = ps.getUpdateCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				rs.close();
				con.close();
			} catch (Exception e2) {}		
		}
		//System.out.println(shared_map.size());
		return shared_map;
	}
	public  Map getSwitchs() {
		Map<String,JSONObject> switchs=new HashMap<String,JSONObject>();
		Connection con = null;
		ResultSet rs = null;
		Map shared_map = getNetworkShared();
		Map external_map = getNetworkExternal();
		try {
			con = getConnection("neutron");
			PreparedStatement ps = null;
			String sql = "select * from networks";//where tenant_id= scsssdfs;
			ps = (PreparedStatement) con.prepareStatement(sql);
			if (ps.execute()) {
				rs = ps.getResultSet();
				StringBuffer b = new StringBuffer();
				while (rs.next()) {
					JSONObject instance=new JSONObject();
					JSONObject metadata=new JSONObject();
					instance.put("component", "switch/"+rs.getString("id"));
					metadata.put("type", "switch");
					metadata.put("switch_id", rs.getString("id"));
					metadata.put("switch_status", rs.getString("status"));
					metadata.put("label", rs.getString("name"));
					if(shared_map.get(rs.getString("id"))!=null){
						metadata.put("shared", "true");
					}
					if(external_map.get(rs.getString("id"))!=null){
						metadata.put("router_external", "true");
					}
					instance.put("metadata", metadata);
					//System.out.println(instance);
					switchs.put("switch/"+rs.getString("id"), instance);
				}
			} else {
				int i = ps.getUpdateCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				rs.close();
				con.close();
			} catch (Exception e2) {}		
		}
		return switchs;
	}
	public  JSONObject getTopo(){
		JSONObject topojson = new JSONObject();
		JSONObject processes = new JSONObject();
		Map ins	=getInstances();
		Iterator<Map.Entry<String, JSONObject>> it = ins.entrySet().iterator();
		while (it.hasNext()) {
		   Map.Entry<String, JSONObject> entry = it.next();
		   processes.put(entry.getKey(), entry.getValue());
		}
		Map routers= getRouters();
		Iterator<Map.Entry<String, JSONObject>> router = routers.entrySet().iterator();
		while (router.hasNext()) {
		   Map.Entry<String, JSONObject> entry = router.next();
		   processes.put(entry.getKey(), entry.getValue());
		}
		Map switchs= getSwitchs();
		Iterator<Map.Entry<String, JSONObject>> switcher = switchs.entrySet().iterator();
		while (switcher.hasNext()) {
		   Map.Entry<String, JSONObject> entry = switcher.next();
		   processes.put(entry.getKey(), entry.getValue());
		}
		topojson.put("processes", processes);

		List<JSONObject> lin = get_instance_net();
		List<JSONObject> lrn = get_router_net();
		for(int i=0;i<lin.size();i++){
			lrn.add(lin.get(i));
		}
		topojson.put("connections", lrn);
		System.out.println(topojson);
		return topojson;
	}
	public static void main(String[] args) {
		JSONObject jo = new JSONObject();
		//get_instance_net();
		//getInstances();
		//getRouters();
		//getSwitchs();
		//get_router_net();
		Topo topo=new Topo();
		topo.getTopo();
	}
//	public static void main(String[] args) {
//		
//		//getNetworkExternal();
//		//getNetworkShared();
//	}

}
