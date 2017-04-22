package application;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.ImageObserver;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.AttributedCharacterIterator;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class MyController implements Initializable, CallBack{

	@FXML
	private MenuItem setAutoReply, appAbout;
	@FXML
	private Button getQR, send, autoReplyBtn;
	@FXML
	private TextArea loginLog, sendText;
	@FXML
	private ListView accountList, FriendList, talkRecord;
	private ObservableList<AccountLabel> item = FXCollections.observableArrayList();
	private ObservableList<Label> talkList = FXCollections.observableArrayList();
	private AccountLabel checkedAccount = null;
	private FriendLabel checkedFriend = null;
	private CloseableHttpClient hc = null;
	private HttpPost hp = null;
	private HttpEntity he = null;
	private JSONObject jo = null;
	private StringEntity strEntity = null;
	private String sendTime = null, autoReplyApi, autoReplyApiKey, fileName = "autoReply.json", encoding = "utf-8", line, sql;
	private Connection connection = null;
	private Statement statement = null;
	private ResultSet res = null;
	private Label[] talkLabel, talkTime;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private int msgCount = 0, resRowNum = 0;
	private File autoReplyF = null;
	private BufferedReader br = null;
	private StringBuffer strBul = new StringBuffer();
	private Alert qrAlert = null;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		qrAlert = new Alert(AlertType.INFORMATION, "", null);
		qrAlert.getDialogPane().setPrefWidth(430);
		qrAlert.setHeaderText("");
		qrAlert.setTitle("请扫描二维码");
		
		autoReplyBtn.setOnMouseClicked((e)->{
			if(checkedAccount != null){
				if(checkedAccount.autoReply){
					checkedAccount.autoReply = false;
					autoReplyBtn.setText("设置自动回复");
				}else{
					checkedAccount.autoReply = true;
					autoReplyBtn.setText("取消自动回复");
				}
			}
		});
		
		//set auto reply config
		try{
			autoReplyF = new File(fileName);
			if(autoReplyF.exists()){
				br = new BufferedReader(new FileReader(fileName));
				while((line = br.readLine()) != null){
					strBul.append(line);
				}
				jo = new JSONObject(strBul.toString());
				autoReplyApi = jo.getString("autoReplyApi");
				autoReplyApiKey = jo.getString("autoReplyApiKey");
			}
		}catch(Exception e){
			System.out.println("读取自动回复的配置文件发生错误: " + e.getMessage());
		}
		
		setAutoReply.setOnAction((e)->{
			try {
				Parent root = FXMLLoader.load(getClass().getResource("/setScene.fxml"));
				Stage setStage = new Stage();
				setStage.setTitle("设置自动回复");
				setStage.setScene(new Scene(root));
				setStage.show();
			} catch (IOException e1) {
				System.out.println("show set scene err: " + e1.getMessage());
			}
			
		});
		
		accountList.setItems(item);
		talkRecord.setItems(talkList);
		
		//connection db
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:multiWx");
			statement = connection.createStatement();
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			loginLog.appendText(e2.getMessage()+"\n");
		}

		System.setProperty("jsse.enableSNIExtension", "false");
		
		//create HttpClient
		hc = HttpClients.createDefault();
		
		getQR.setOnMouseClicked((event)->{
			loginClass login = new loginClass();
			login.start();
		});
		
		//account list click event
		accountList.setOnMouseClicked((e)->{
			checkedAccount = (AccountLabel) accountList.getSelectionModel().getSelectedItem();
			if(checkedAccount != null){
				//loginLog.appendText("click accountList, and checked user: " + checkedAccount.myNick + "\n");
				Platform.runLater(()->{
					checkedAccount.setTextFill(Color.web("#000000"));
					FriendList.setItems(checkedAccount.item);
				});
			}
		});
		
		//friend list click event
		FriendList.setOnMouseClicked((e)->{
			checkedFriend = (FriendLabel) FriendList.getSelectionModel().getSelectedItem();
			if(checkedFriend != null){
				//loginLog.appendText("click friend list, and checked friend: " + checkedFriend.HeadImgUrl + "\n");
				Platform.runLater(()->{
					try {
						checkedFriend.setTextFill(Color.web("#000000"));
						
						talkList.clear();
						
						res = statement.executeQuery("select count(*) as count from " + checkedAccount.tableName + " where userName = '" + checkedFriend.UserName + "'");
						msgCount = res.getInt("count");
						
						res = statement.executeQuery("select * from " + checkedAccount.tableName + " where userName = '" + checkedFriend.UserName + "' order by contentTime");
						
						//get msg count
						//msgCount = res.getFetchSize();
						talkTime = new Label[msgCount];
						talkLabel = new Label[msgCount];
						resRowNum = 0;
						
						while(res.next()){
							talkTime[resRowNum] = new Label();
							talkTime[resRowNum].setText(sdf.format(Long.valueOf(res.getString("contentTime"))));
							talkTime[resRowNum].setTextFill(Color.web("#888888"));
							talkTime[resRowNum].setAlignment(Pos.CENTER);
							
							talkLabel[resRowNum] = new Label();
							talkLabel[resRowNum].setText(res.getString("content"));
							talkLabel[resRowNum].setWrapText(true);
							
							if(res.getInt("path") == 1){
								talkLabel[resRowNum].setTextFill(Color.web("#ff0000"));
							}
							loginLog.appendText("add msg to list\n");
							talkList.add(talkTime[resRowNum]);
							talkList.add(talkLabel[resRowNum]);
							
							resRowNum++;
						}
					} catch (SQLException e1) {
						System.out.println(e1.getMessage());
					}
					sendText.requestFocus();
				});
				
			}
		});
		
		//sendBtn click event
		send.setOnMouseClicked((e)->{
			if(checkedAccount != null && checkedFriend != null){
				System.out.println("start send msg");
				loginLog.appendText("准备发送信息");
				jo = new JSONObject();
				sendTime = System.currentTimeMillis() + "";
				jo.put("BaseRequest", checkedAccount.baseRequestJo.get("BaseRequest")).put("Scene", 0).put("Msg", new JSONObject().put("Type", 1).put("Content", sendText.getText()).put("FromUserName", checkedAccount.myUserName).put("ToUserName", checkedFriend.UserName).put("LocalID", sendTime).put("CliendMsgId", sendTime));
				hp = new HttpPost(checkedAccount.httpStart + ".qq.com/cgi-bin/mmwebwx-bin/webwxsendmsg?lang=zh_CN&pass_ticket="+checkedAccount.pass_ticket);
				System.out.println(jo.toString());
				strEntity = new StringEntity(jo.toString(), ContentType.APPLICATION_JSON);
				hp.setEntity(strEntity);
				try {
					he = hc.execute(hp).getEntity();
					System.out.println(EntityUtils.toString(he));
					statement.execute("insert into " + checkedAccount.tableName + " (content, path, userName,  contentType, contentTime, autoReply) values('" + sendText.getText() + "', 1, '" + checkedFriend.UserName + "', 1, '" + System.currentTimeMillis() + "', 0)");
					//send sucess, clear sendText
					sendText.clear();
				} catch (IOException | SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
	}
	
	private class loginClass extends Service<ImageView> {
		
		protected Task<ImageView> createTask() {
			
			// TODO Auto-generated method stub
			return new Task<ImageView>(){
				protected ImageView call(){
					
					Platform.runLater(()->{
						getQR.setText("正在获取，请稍后...");
						getQR.setDisable(true);
					});
					
					AccountLabel account = new AccountLabel(MyController.this, autoReplyApi, autoReplyApiKey);
					if(account.create){
						account.getMsg(MyController.this);
					}
					
					Platform.runLater(()->{
						getQR.setText("获取二维码");
						getQR.setDisable(false);
					});
					
					return null;
				}
			};
		}
	}
	
	public void newAccount(AccountLabel account) {
		loginLog.appendText("新用户登陆成功: " + account.myNick + "\n");
		Platform.runLater(()->{
			item.add(account);
		});
	}
	
	public void checkAccount(ObservableList<Label> item){
		Platform.runLater(()->{
			FriendList.setItems(item);
		});
	}

	@Override
	public void newMsg(AccountLabel account, String time, String content) {
		
		try{
			System.out.println("checkedAccount: " + checkedAccount.myUserName);
			System.out.println("checkedFriend: " + checkedFriend.UserName);
			System.out.println("account: " + account.myUserName);
			System.out.println("fromFriend: " + account.fromFriend.UserName);
		}catch(Exception e){
			System.out.println("print username err: " + e.getMessage());
		}
		
		loginLog.appendText("当前用户来了一条新消息\n");
		// TODO Auto-generated method stub
		if(checkedAccount == null || !checkedAccount.myUserName.equals(account.myUserName)){
			Platform.runLater(()->{
				item.remove(account);
				account.setTextFill(Color.web("#aa0000"));
				item.add(0, account);
			});
			
		}
		
		if(checkedFriend == null || !account.fromFriend.UserName.equals(checkedFriend.UserName)){
			Platform.runLater(()->{
				account.item.remove(account.fromFriend);
				account.fromFriend.setTextFill(Color.web("#aa0000"));
				account.item.add(0, account.fromFriend);
			});
			
		}
		
		if(checkedAccount!=null && checkedFriend!=null && checkedAccount.myUserName.equals(account.myUserName) && account.fromFriend.UserName.equals(checkedFriend.UserName)){
			Label talkTime = new Label(sdf.format(Long.valueOf(time)));
			talkTime.setTextFill(Color.web("#888888"));
			talkTime.setAlignment(Pos.CENTER);
			
			Label talkContent = new Label(content);
			talkContent.setWrapText(true);
			
			Platform.runLater(()->{

				talkList.add(talkTime);
				talkList.add(talkContent);
			});
		}
	}
	
	public void rmAccount(AccountLabel account){
		loginLog.appendText("用户退出: " + account.myNick + "\n");
		Platform.runLater(()->{
			item.remove(account);
		});
	}
	
	public void setQrCode(){
		qrAlert.setGraphic(new ImageView(new Image("file:QRImg.jpg")));
		Platform.runLater(()->{
			qrAlert.show();
			getQR.setDisable(false);
			getQR.setText("获取二维码");
		});
	}
	
	public void hideQr(){
		Platform.runLater(()->{
			loginLog.appendText("新用户登陆中...\n");
			qrAlert.close();
		});
	}
}
