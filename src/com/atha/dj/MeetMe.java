/*
 * Copyright (c) 2012, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.atha.dj;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.json.JSONArray;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.salesforce.androidsdk.app.ForceApp;
import com.salesforce.androidsdk.rest.ClientManager.LoginOptions;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.NativeMainActivity;

/**
 * Main activity
 */
public class MeetMe extends NativeMainActivity {

	private RestClient client;
	private ArrayAdapter<String> listAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup view
		setContentView(R.layout.main);

	}

	@Override
	public void onResume() {
		// Hide everything until we are logged in
		findViewById(R.id.root).setVisibility(View.INVISIBLE);

		// Create list adapter
		listAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, new ArrayList<String>());
		((ListView) findViewById(R.id.contacts_list)).setAdapter(listAdapter);

		super.onResume();
	}

	@Override
	protected LoginOptions getLoginOptions() {
		LoginOptions loginOptions = new LoginOptions(
				null, // login host is chosen by user through the server picker
				ForceApp.APP.getPasscodeHash(),
				getString(R.string.oauth_callback_url),
				getString(R.string.oauth_client_id), new String[] { "api" });
		return loginOptions;
	}

	@Override
	public void onResume(RestClient client) {
		// Keeping reference to rest client
		this.client = client;

		// Show everything
		findViewById(R.id.root).setVisibility(View.VISIBLE);
		try {
			//Fech contact list from salesforce
			sendRequest("SELECT Name FROM Contact");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Called when "Logout" button is clicked.
	 * 
	 * @param v
	 */
	public void onLogoutClick(View v) {
		ForceApp.APP.logout(this);
	}

	public void fetchContactPhoneNumber(final String name)
			throws UnsupportedEncodingException {
		String soql = "SELECT Phone FROM Contact WHERE Name=" + '\'' + name
				+ '\'';
		RestRequest restRequest = RestRequest.getRequestForQuery(
				getString(R.string.api_version), soql);
		client.sendAsync(restRequest, new AsyncRequestCallback() {

			@Override
			public void onSuccess(RestRequest request, RestResponse response) {
				try {
					JSONArray records = response.asJSONObject().getJSONArray(
							"records");
					String phone = records.getJSONObject(0).getString("Phone");

					showDialog(name, phone);
				} catch (Exception e) {
					onError(e);
				}
			}

			@Override
			public void onError(Exception exception) {
			}
		});
	}

	
	private void sendRequest(String soql) throws UnsupportedEncodingException {
		
		RestRequest restRequest = RestRequest.getRequestForQuery(
				getString(R.string.api_version), soql);
		
		client.sendAsync(restRequest, new AsyncRequestCallback() {
			@Override
			public void onSuccess(RestRequest request, RestResponse result) {
				try {
					//Clear list before re-populating
					listAdapter.clear();
					JSONArray records = result.asJSONObject().getJSONArray(
							"records");
					//Add contacts to list adapter
					for (int i = 0; i < records.length(); i++) {
						listAdapter.add(records.getJSONObject(i).getString(
								"Name"));
					}
					final ListView contactList = ((ListView) findViewById(R.id.contacts_list));
					contactList
							.setOnItemClickListener(new OnItemClickListener() {
								//Fetch contact phone number on item click
								@Override
								public void onItemClick(AdapterView<?> parent,
										View view, int position, long id) {
									String name = (String) contactList
											.getItemAtPosition(position);
									try {
										fetchContactPhoneNumber(name);
									} catch (UnsupportedEncodingException e) {
										e.printStackTrace();
									}
								}
							});
				} catch (Exception e) {
					onError(e);
				}
			}

			@Override
			public void onError(Exception exception) {
				Toast.makeText(
						MeetMe.this,
						MeetMe.this.getString(ForceApp.APP.getSalesforceR()
								.stringGenericError(), exception.toString()),
						Toast.LENGTH_LONG).show();
			}
		});
	}
	/*
	 * Shows slide up dialog when Contact is clicked
	 * 
	 */
	private void showDialog(String message, final String phone) {
		final ConfirmContactDialog alert = new ConfirmContactDialog(MeetMe.this);
		alert.setMessageTop(message);
		alert.setMessageBottom(phone);
		alert.getOkButton().setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				launchMapActivity(phone);
				alert.dismiss();
			}
		});
		
		alert.getCancelButton().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alert.dismiss();
			}
		});
		alert.show();
	}

	private void launchMapActivity(String phone) {

		Intent intent = new Intent(MeetMe.this, LocationMap.class);
		intent.putExtra("PhoneNumber", phone);
		startActivity(intent);
	}
}
