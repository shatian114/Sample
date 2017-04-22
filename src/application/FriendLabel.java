package application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FriendLabel extends Label {
	public String UserName, NickName, RemarkName, Province, City, HeadImgUrl;
	public int sex = 0;
	
	public FriendLabel(String UserName, String NickName, String RemarkName, String Province, String City, String HeadImgUrl, int sex, final CloseableHttpClient hc, String httpStart){
		this.UserName = UserName;
		this.NickName = NickName;
		this.RemarkName = RemarkName;
		this.Province = Province;
		this.City = City;
		this.HeadImgUrl = HeadImgUrl;
		this.sex = sex;
		if(!this.RemarkName.equals("")){
			this.setText(this.RemarkName);
		}else{
			this.setText(this.NickName);
		}
		
		new Thread(new Runnable(){
			
			HttpEntity he = null;
			public void run(){
				CloseableHttpClient hcc = hc;
				File avatarFile = new File(UserName + ".jpg");
				if(!avatarFile.exists()){
					try {
						he = hcc.execute(new HttpGet(HeadImgUrl)).getEntity();
						byte[] avatarByte = EntityUtils.toByteArray(he);
						FileOutputStream fos = new FileOutputStream(avatarFile);
						fos.write(avatarByte, 0, avatarByte.length);
						fos.flush();
						fos.close();
						Platform.runLater(()->{
							FriendLabel.this.setGraphic(new ImageView(new Image("file:" + UserName + ".jpg", 40, 40, false, false)));
						});
					} catch (IOException e) {
						System.out.println(e.getMessage());
					}
				}
			}
		}).start();
	}
}
