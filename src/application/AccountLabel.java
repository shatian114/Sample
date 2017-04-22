package application;

import javafx.scene.paint.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class AccountLabel extends Label {
	
	private static CloseableHttpClient hc = null;
	private static HttpPost hp = null;
	private static HttpEntity he = null;
	public String avatar = "", uuid = "";
	public static String responseStr = "";
	public String urlStr = "";
	public String ticket = "";
	public String scan = "";
	public String skey = "";
	public String wxsid = "";
	public String wxuin = "";
	public String pass_ticket = "";
	public String DeviceID = "";
	public String myNick = "";
	public String myUserName = "";
	public String syncKey = "";
	public String httpStart = "";
	public String tableName, robotSay = "";
	private String sql = "";
	public static String autoReplyApi, autoReplyKey;
	private String[] getParamsArr = null;
	private Document xmlDoc = null;
	private Element docElement = null;
	private static StringEntity strEntity = null;
	private JSONArray contactArr = null, syncKeyList = null;
	private JSONObject contactJson = null, contact;
	private static JSONObject jo = null;
	private JSONObject syncKeyJo = null;
	private JSONObject autoReplyJo = null;
	public JSONObject baseRequestJo = null;
	private int contactSum = 0, AddMsgCount = 0, syncKeyCount = 0;
	public ObservableList<FriendLabel> item = FXCollections.observableArrayList();
	private FileOutputStream fos =null;
	private byte[] QrCodeByte;
	public boolean create = false, autoReply = false, haveAutoReplyConfig;
	public FriendLabel fromFriend = null;
	
	public AccountLabel(CallBack cb, String autoReplyApi, String autoReplyApiKey){
		super();
		
		this.autoReplyApi = autoReplyApi;
		this.autoReplyKey = autoReplyApiKey;

		System.setProperty("jsse.enableSNIExtension", "false");

		//create httpclient
		hc = HttpClients.createDefault();
		
		//create DeviceID
		DeviceID = "e" + System.currentTimeMillis();

		try{
			//get UUID
			he = hc.execute(new HttpGet("https://login.wx2.qq.com/jslogin?appid=wx782c26e4c19acffb")).getEntity();
			responseStr = EntityUtils.toString(he);
			uuid = responseStr.substring(responseStr.indexOf("\"")+1, responseStr.lastIndexOf("\""));
			System.out.println(uuid);
			
			//get QrCode
			he = hc.execute(new HttpGet("https://login.weixin.qq.com/qrcode/"+uuid)).getEntity();
			fos = new FileOutputStream("./QRImg.jpg");
			QrCodeByte = EntityUtils.toByteArray(he);
			fos.write(QrCodeByte);
			fos.flush();
			fos.close();
			System.out.println("wait scan...");
			cb.setQrCode();
			
			//get scan result and userAvatar
			he = hc.execute(new HttpGet("https://login.wx2.qq.com/cgi-bin/mmwebwx-bin/login?loginicon=true&tip=0&uuid="+uuid+"&r=-"+System.currentTimeMillis()+"&_="+System.currentTimeMillis())).getEntity();
			cb.hideQr();
			responseStr = EntityUtils.toString(he);
			//check scan result
			if(responseStr.substring(responseStr.indexOf("=")+1, responseStr.indexOf(";")).equals("201")){
				System.out.println("scan success, wait login..." + responseStr);
				//set avatar
				if(responseStr.contains("window.userAvatar")){
					avatar = responseStr.substring(responseStr.indexOf(",")+1, responseStr.lastIndexOf("'"));
				}else{
					avatar = "";
				}
			}else{
				return;
			}
			
			//storage avatar data
			if(!avatar.equals("")){
				File avatarFile = new File(uuid+"Avatar.png");
				if(avatarFile.exists()){
					avatarFile.delete();
				}
				avatarFile.createNewFile();
				FileOutputStream os = new FileOutputStream(avatarFile);
				byte[] avatarByte = Base64.getDecoder().decode(avatar);
				os.write(avatarByte, 0, avatarByte.length);
				os.flush();
				os.close();
				this.setGraphic(new ImageView(new Image("file:"+uuid+"Avatar.png", 60, 60, false, false)));
			}
			
			//start login
			he = hc.execute(new HttpGet("https://login.wx.qq.com/cgi-bin/mmwebwx-bin/login?loginicon=true&tip=0&uuid="+uuid+"&r=-"+System.currentTimeMillis()+"&_="+System.currentTimeMillis())).getEntity();
			responseStr = EntityUtils.toString(he);
			System.out.println("login sucess, start init..." + responseStr);
			
			//start init
			urlStr = "https" + responseStr.substring(responseStr.indexOf("\"")+6, responseStr.lastIndexOf("\""));
			getParamsArr = urlStr.substring(urlStr.indexOf("?")+1).split("&");
			for(int i=0; i<getParamsArr.length; i++){
				if(getParamsArr[i].substring(0, getParamsArr[i].indexOf("=")).equals("ticket")){
					ticket = getParamsArr[i].substring(getParamsArr[i].indexOf("=")+1);
				}
				if(getParamsArr[i].substring(0, getParamsArr[i].indexOf("=")).equals("scan")){
					scan = getParamsArr[i].substring(getParamsArr[i].indexOf("=")+1);
				}
			}
			
			//get httpStart str
			if(urlStr.startsWith("https://wx2")){
				httpStart = "https://wx2";
			}else{
				httpStart = "https://wx";
			}
			
			//get init params
			he = hc.execute(new HttpGet(urlStr+"&fun=new&version=v2&lang=zh_CN")).getEntity();
			responseStr = EntityUtils.toString(he);
			System.out.println(responseStr);
			xmlDoc = DocumentHelper.parseText(responseStr);
			docElement = xmlDoc.getRootElement();
			
			skey = docElement.elementText("skey");
			wxsid = docElement.elementText("wxsid");
			wxuin = docElement.elementText("wxuin");
			pass_ticket = docElement.elementText("pass_ticket");
			baseRequestJo = new JSONObject();
			baseRequestJo.put("BaseRequest", new JSONObject().put("Uin", wxuin).put("Sid", wxsid).put("Skey", skey).put("DeviceID", DeviceID));
			System.out.println("get params success." + skey);
			
			//init login
			jo = new JSONObject();
			jo.put("BaseRequest", baseRequestJo.get("BaseRequest"));
			hp = new HttpPost(httpStart + ".qq.com/cgi-bin/mmwebwx-bin/webwxinit?lang=zh_CN&pass_ticket=" + pass_ticket+"&r=-"+System.currentTimeMillis());
			strEntity = new StringEntity(baseRequestJo.toString(), ContentType.APPLICATION_JSON);
			hp.setEntity(strEntity);
			he = hc.execute(hp).getEntity();
			responseStr = EntityUtils.toString(he, "utf-8");
			jo = new JSONObject(responseStr);
			myUserName = jo.getJSONObject("User").getString("UserName");
			myNick = jo.getJSONObject("User").getString("NickName");
			syncKey = jo.getJSONObject("SyncKey").toString();
			tableName = "t" + myUserName.substring(2);
			System.out.println("init success, start status notify...");
			this.setText(myNick);
			
			//status notify
			jo = new JSONObject();
			jo.put("BaseRequest", baseRequestJo.get("BaseRequest")).put("ClientMsgId", System.currentTimeMillis()).put("Code", 3).put("FormUserName", myUserName).put("ToUserName", myUserName);
			hp = new HttpPost(httpStart + ".qq.com/cgi-bin/mmwebwx-bin/webwxstatusnotify?lang=zh_CN&pass_ticket=" + pass_ticket);
			strEntity = new StringEntity(baseRequestJo.toString(), ContentType.APPLICATION_JSON);
			hp.setEntity(strEntity);
			he = hc.execute(hp).getEntity();
			System.out.println("start get contact list...");
			
			//get contact
			he = hc.execute(new HttpGet(httpStart + ".qq.com/cgi-bin/mmwebwx-bin/webwxgetcontact?lang=zh_CN&seq=0&r="+System.currentTimeMillis()+"&skey="+skey+"&pass_ticket="+pass_ticket)).getEntity();
			responseStr = EntityUtils.toString(he, "utf-8");
			
			contactJson = new JSONObject(responseStr);
			contactSum = contactJson.getInt("MemberCount");
			contactArr = contactJson.getJSONArray("MemberList");
			try{
				for(int i=0; i<contactSum; i++){
					contact = contactArr.getJSONObject(i);
					item.add(new FriendLabel(contact.getString("UserName"), contact.getString("NickName"), contact.getString("RemarkName"), contact.getString("Province"), contact.getString("City"), httpStart + ".qq.com" + contact.getString("HeadImgUrl"), contact.getInt("Sex"), hc, httpStart));
				}
			}catch(Exception e){
				System.out.println("get contact err: " + e.getMessage());
			}
			
			System.out.println("get contact success..." + contactArr.length() + "\n");
			
			create = true;
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void setNewAccount(CallBack cb){
		cb.newAccount(this);
	}
	
	public void setNewMsg(CallBack cb){
		
	}
	
	public void rmAccount(CallBack cb){
		cb.rmAccount(this);
	}
	
	public void getMsg(CallBack cb){
		cb.newAccount(this);
		syncKeyJo = new JSONObject(syncKey);
		
		new Thread(new Runnable(){
			
			public void run(){
				
				Connection connection = null;
				Statement statement = null;
				ResultSet res = null;
				String syncKeyStr = "", line, sendTime = "", friendUserName;
				JSONArray AddMsgJa = null;
				BufferedReader br = null;
				File f = null;
				StringBuffer strBuf = new StringBuffer();
				
				try{
					//get autoReply config
					f = new File("autoReply.json");
					if(f.exists()){
						br = new BufferedReader(new FileReader("autoReply.json"));
						while((line = br.readLine()) != null){
							strBuf.append(line);
						}
						autoReplyJo = new JSONObject(strBuf.toString());
						haveAutoReplyConfig = true;
					}else{
						haveAutoReplyConfig = false;
					}
					
					//connect db and create table
					connection = DriverManager.getConnection("jdbc:sqlite:multiWx");
					statement = connection.createStatement();
					res = statement.executeQuery("select count(*) from sqlite_master where type='table' and name='" + tableName + "'");
					if(res.getRow() > 0){
						System.out.println("table is exists, now drop\n");
						statement.execute("drop table " + tableName);
					}
					statement.execute("create table " + tableName + "(content text, path Integer, userName text, contentType Integer, contentTime text, autoReply Integer)");
					System.out.println("table create success");
				}catch(Exception e){
					System.out.println(e.getMessage());
				}
				
				if(httpStart.equals("https://wx2")){
					urlStr = "https://webpush.wx2";
				}else{
					urlStr = "https://webpush.wx";
				}
				urlStr += ".qq.com/cgi-bin/mmwebwx-bin/synccheck?skey="+skey+"&sid="+wxsid+"&uin="+wxuin+"&deviceid="+DeviceID+"&_="+System.currentTimeMillis()+"&r="+System.currentTimeMillis()+"&synckey=";
				
				while(true){
					try {
						syncKeyCount = syncKeyJo.getInt("Count");
						syncKeyList = syncKeyJo.getJSONArray("List");
						syncKeyStr = "";
						for(int i=0; i<syncKeyCount; i++){
							jo = syncKeyList.getJSONObject(i);
							//System.out.println(jo.toString());
							syncKeyStr += jo.getInt("Key") + "_" + jo.getInt("Val");
							if(i != syncKeyCount-1){
								syncKeyStr += "%7C";
							}
						}
						he = hc.execute(new HttpGet(urlStr + syncKeyStr)).getEntity();
						responseStr = EntityUtils.toString(he);
						//System.out.println(responseStr);
						jo = new JSONObject(responseStr.substring(responseStr.indexOf("{"), responseStr.lastIndexOf("}")+1));
						if(jo.getString("retcode").equals("0")){
							if(jo.getString("selector").equals("2")){
								System.out.println("start read msg");
								jo = new JSONObject();
								jo.put("BaseRequest", baseRequestJo).put("SyncKey", syncKeyJo).put("rr", System.currentTimeMillis());
								strEntity = new StringEntity(jo.toString(), ContentType.APPLICATION_JSON);
								hp = new HttpPost(httpStart + ".qq.com/cgi-bin/mmwebwx-bin/webwxsync?lang=zh_CN&sid="+wxsid+"&skey="+skey+"&pass_tickey="+pass_ticket);
								hp.setEntity(strEntity);
								he = hc.execute(hp).getEntity();
								responseStr = EntityUtils.toString(he, "utf-8");
								jo = new JSONObject(responseStr);

								//System.out.println("read a msg: " + responseStr);
								syncKeyJo = jo.getJSONObject("SyncKey");
								AddMsgCount = jo.getInt("AddMsgCount");
								if(AddMsgCount > 0){
									
									//add new msg to db
									AddMsgJa = jo.getJSONArray("AddMsgList");
									for(int i=0; i<AddMsgCount; i++){
										jo = AddMsgJa.getJSONObject(i);
										friendUserName = jo.getString("FromUserName");
										sql = "insert into " + tableName + "(content, path, userName,  contentType, contentTime) values('" + jo.get("Content") + "', 0, '" + friendUserName + "', " + jo.getInt("MsgType") + ", '" + jo.getLong("CreateTime") + "000')";
										System.out.println(sql);
										statement.execute(sql);
										
										for(int j=0; j<contactSum; j++){
											fromFriend = item.get(j);
											if(fromFriend.UserName.equals(jo.getString("FromUserName"))){
												break;
											}
										}
										cb.newMsg(AccountLabel.this, jo.getLong("CreateTime")+"000", jo.getString("Content"));
										//check autoReply
										if(autoReply && haveAutoReplyConfig){
											robotSay = getRobot(jo.getString("Content"));
											if(!robotSay.equals("")){
												//robot send msg
												System.out.println("start auto reply");
												jo = new JSONObject();
												sendTime = System.currentTimeMillis() + "";
												jo.put("BaseRequest", baseRequestJo.get("BaseRequest")).put("Scene", 0).put("Msg", new JSONObject().put("Type", 1).put("Content", robotSay).put("FromUserName", myUserName).put("ToUserName", friendUserName).put("LocalID", sendTime).put("CliendMsgId", sendTime));
												hp = new HttpPost(httpStart + ".qq.com/cgi-bin/mmwebwx-bin/webwxsendmsg?lang=zh_CN&pass_ticket="+pass_ticket);
												System.out.println(jo.toString());
												strEntity = new StringEntity(jo.toString(), ContentType.APPLICATION_JSON);
												hp.setEntity(strEntity);
												he = hc.execute(hp).getEntity();
												System.out.println(EntityUtils.toString(he));
												statement.execute("insert into " + tableName + " (content, path, userName,  contentType, contentTime, autoReply) values('" + robotSay + "', 1, '" + friendUserName + "', 1, '" + System.currentTimeMillis() +  "', 1)");
											}
										}
									}
									System.out.println(jo.toString());
									System.out.println("get msg: " + jo.getJSONArray("AddMsgList").getJSONObject(0).getString("Content"));
									
								}
							}else{

								//System.out.println("selector: " + jo.getString("selector"));
							}
						}else{
							System.out.println("retcode: " + jo.getString("retcode"));
							statement.close();
							connection.close();
							cb.rmAccount(AccountLabel.this);
							break;
						}
					} catch (IOException | JSONException | SQLException e) {
						System.out.println("read msg exception: " + e.getMessage());
					}
				}
			}
		}).start();
	}
	
	private static String getRobot(String friendSay){
		System.out.println(autoReplyApi + "\n" + autoReplyKey);
		String robotStr = "";
		hp = new HttpPost(autoReplyApi);
		jo = new JSONObject();
		jo.put("key", autoReplyKey).put("info", friendSay).put("userid", "123432");
		strEntity = new StringEntity(jo.toString(), ContentType.APPLICATION_JSON);
		hp.setEntity(strEntity);
		try {
			he = hc.execute(hp).getEntity();
			responseStr = EntityUtils.toString(he, "utf-8");
			jo = new JSONObject(responseStr);
			switch(jo.getInt("code")){
			case 100000:
				robotStr += jo.getString("text");
				break;
			case 200000:
				robotStr +=jo.getString("text") + jo.getString("url");
				break;
			case 302000:
				robotStr += jo.getString("text") + jo.getString("list");
				break;
			//ker err
			case 40001:
			//key user num is zero
			case 40004:
				robotStr = "";
				break;
			//info is ""
			case 40002:
			//info err
			case 40007:
			default:
				robotStr = "";
				break;
			}
		} catch (IOException e) {
			System.out.println("get robot say err: " + e.getMessage());
		}
		
		return robotStr;
	}
}
