package application;

import javafx.collections.ObservableList;
import javafx.scene.control.Label;

public interface CallBack {

	//get a new account
	public void newAccount(AccountLabel account);
	
	//get a new message
	public void newMsg(AccountLabel account, String time, String content);
	
	//rm account
	public void rmAccount(AccountLabel account);
	
	//set QrCode
	public void setQrCode();
	
	//hide QrCode
	public void hideQr();
}
