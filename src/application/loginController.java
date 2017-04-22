package application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import mysqlCenter.protoBuf.mysqlBuf.Buf;

public class loginController implements Initializable {

	@FXML
	private Button loginBtn;
	@FXML
	private TextField userNameField;
	@FXML
	private PasswordField passwordField;
	@FXML
	private Text prompt;
	private Socket client;
	private OutputStream os;
	private InputStream is;
	private int len, readCount;
	private byte[] lenByte = new byte[4], byteArr, sendByte;
	private String getStr;
	private Buf.Builder buf = Buf.newBuilder();
	Parent root;
	Stage setStage = new Stage();
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		//connect db server
		try {
			System.out.println("start connection server");
			client = new Socket("116.62.38.80", 23647);
			os = client.getOutputStream();
			is = client.getInputStream();
			root = FXMLLoader.load(getClass().getResource("/MyScene.fxml"));
			setStage.setTitle("微信多账户管理");
			setStage.setScene(new Scene(root));
			//main stage close
			setStage.setOnCloseRequest((e)->{
				buf.clear();
				buf.setOperateType("logout");
				buf.setUserName(userNameField.getText());
				try {
					os.write(getSendByte(buf.build().toByteArray()));
					is.close();
					os.close();
					client.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				System.out.println("close main stage");
			});
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		loginBtn.setOnMouseClicked((e)->{
			loginBtn.setText("正在登陆，请稍后...");
			loginBtn.setDisable(true);
			try{
				buf.clear();
				buf.setOperateType("login");
				buf.setUserName(userNameField.getText());
				buf.setPassword(passwordField.getText());
				prompt.setText(passwordField.getText());
				sendByte = buf.build().toByteArray();
				os.write(getSendByte(sendByte));
				os.flush();
				
				while(is.available() < 4) Thread.sleep(100);
				is.read(lenByte, 0, 4);
				len = byte2int(lenByte);
				while(is.available() < len) Thread.sleep(100);
				byteArr = new byte[len];
				readCount = 0;
				while(readCount < len){
					readCount += is.read(byteArr, readCount, len-readCount);
				}
				getStr = new String(byteArr);
				
				loginBtn.setText("登陆");
				loginBtn.setDisable(false);
				
				prompt.setText(getStr);
				if(getStr.equals("登陆成功")){
					prompt.setText("登陆成功");
					
					setStage.show();
					
					((Stage) loginBtn.getScene().getWindow()).close();
				}
				
			}catch(Exception e1){
				e1.printStackTrace();
			}
		});
	}
	
	public static byte[] getSendByte(byte[] bufByte){
		int bufLen = bufByte.length;
		byte[] sendByte = new byte[4+bufLen];
		System.arraycopy(int2byte(bufLen), 0, sendByte, 0, 4);
		System.arraycopy(bufByte, 0, sendByte, 4, bufLen);
		return sendByte;
	}
	
	private static byte[] int2byte(int m){
		byte[] bys = new byte[4];
		for(int i=0; i< 4; i++){
			bys[i] = (byte) ((m>>((i)*8))&0xff);
		}
		return bys;
	}
	
	private static int byte2int(byte[] bys){
		int m = 0;
		for(int i=0; i<4; i++){
			m |= (bys[i]&0xff)<<(8*i);
		}
		return m;
	}

}
