import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

public class Test {
	public static Connection getConnection(String db) {
		//String url = "jdbc:mysql://192.168.139.251:3306/"+db;
		String url = "jdbc:mysql://127.0.0.1:2222/"+db;
		System.out.println("haoning:"+url);
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
					System.out.println(instance);
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
	public static void main(String[] args) {
		Test t = new Test();
		System.out.println(t.getInstances());

	}

}
