package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import org.json.JSONObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class setController implements Initializable {

	@FXML
	private Text prompt;
	@FXML
	private Button setAutoReplyBtn, changeFastReplyBtn, addFastReplyBtn, saveFastReplyBtn;
	@FXML
	private TextField autoReplyApi, autoReplyApiKey, autoReplyApiSecret;
	@FXML
	private ListView fastReplyList;
	@FXML
	private TextArea fastReplyTextField;
	private File autoReplyF =null, fastReplyF;
	private JSONObject jo = null, fastReplyJo;
	private StringBuffer strBul = new StringBuffer();
	private String joStr = "", autoReplyFileName = "autoReply.json", encodeing = "utf-8", line, fastReplyFileName = "fastReply.json";
	private BufferedReader br = null;
	private int fastReplyCount = 0;
	private ObservableList<Text> fastReplyItem = FXCollections.observableArrayList();
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		fastReplyList.setItems(fastReplyItem);
		
		try{
			autoReplyF = new File(autoReplyFileName);
			if(autoReplyF.exists()){
				br = new BufferedReader(new FileReader(autoReplyFileName));
				while((line = br.readLine()) != null){
					strBul.append(line);
				}
				joStr = strBul.toString();
				jo = new JSONObject(joStr);
				autoReplyApi.setText(jo.getString("autoReplyApi"));
				autoReplyApiKey.setText(jo.getString("autoReplyApiKey"));
				autoReplyApiSecret.setText(jo.getString("autoReplyApiSecret"));
			}
			
			
			//read fast reply json
			fastReplyF = new File(fastReplyFileName);
			if(fastReplyF.exists()){
				br = new BufferedReader(new FileReader(fastReplyFileName));
				while((line = br.readLine()) != null){
					strBul.append(line);
				}
				joStr = strBul.toString();
				fastReplyJo = new JSONObject(joStr);
				fastReplyCount = jo.getInt("fastReplyCount");
				for(int i=0; i<fastReplyCount; i++){
					fastReplyItem.add(generalText(fastReplyJo.getJSONObject("i").getString("content")));
				}
			}
		}catch(Exception e){
			prompt.setText("读取自动回复的配置文件发生错误: " + e.getMessage());
		}
		
		setAutoReplyBtn.setOnMouseClicked((e)->{
			jo = new JSONObject();
			jo.put("autoReplyApi", autoReplyApi.getText()).put("autoReplyApiKey", autoReplyApiKey.getText()).put("autoReplyApiSecret", autoReplyApiSecret.getText());
			try {
				FileWriter fw = new FileWriter(autoReplyFileName);
				fw.write(jo.toString());
				fw.flush();
				fw.close();
			} catch (IOException e1) {
				prompt.setText("存储自动回复的配置文件发生错误: " + e1.getMessage());
				return;
			}
			prompt.setText("存储成功");
		});
		
		addFastReplyBtn.setOnMouseClicked((e)->{
			if(fastReplyJo == null){
				fastReplyJo = new JSONObject();
				fastReplyJo.put("fastReplyCount", 0);
				fastReplyCount = 0;
			}
			
			fastReplyCount++;
			fastReplyJo.put("fastReplyCount", fastReplyCount);
			fastReplyJo.put(fastReplyCount + "", new JSONObject().put("content", fastReplyTextField.getText()));
			
			fastReplyItem.add(generalText(fastReplyTextField.getText()));
			fastReplyTextField.clear();
		});
		
		saveFastReplyBtn.setOnMouseClicked((e)->{
			try {
				FileWriter fw = new FileWriter(fastReplyFileName);
				fw.write(fastReplyJo.toString());
				fw.flush();
				fw.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				System.out.println("save fast reply err: " + e1.getMessage());
				e1.printStackTrace();
			}
		});
	}
	
	public Text generalText(String content){
		Text t = new Text(content);
		return t;
	}
}
