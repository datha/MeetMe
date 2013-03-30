package com.atha.dj;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class ConfirmContactDialog extends Dialog {

	private Button okButton;
	private Button cancelButton;
	private TextView messageTopTV;
	private TextView messageBottomTV;

	public ConfirmContactDialog(final Context context) {

		// Hide Dialog so we can use custom
		super(context, R.style.AlertDialogCustom);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setCancelable(true);
		setContentView(R.layout.customalertdialog);
		messageTopTV = (TextView) findViewById(R.id.messageAlertTV);
		messageBottomTV = (TextView) findViewById(R.id.phoneAlertTV);
		okButton = (Button) findViewById(R.id.okAlertButton);
		cancelButton = (Button) findViewById(R.id.cancelAlertButton);
	}

	public void setMessageTop(String message) {
		messageTopTV.setText(message);
	}

	public void setMessageBottom(String message) {
		messageBottomTV.setText(message);
	}

	public Button getOkButton() {
		return okButton;

	}

	public Button getCancelButton() {
		return cancelButton;
	}

}
