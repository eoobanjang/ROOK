package org.teleal.cling.android.browser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.teleal.cling.model.meta.DeviceIdentity;

import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint.Join;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class UserList extends ListActivity {

	String[] ulist;
	String identity;
	String mac;
	String no;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		identity = getIntent().getExtras().getString(ContentActivity.DEVICE_UUID);
		getJSONFromUrl();
		setListAdapter(new ArrayAdapter<String>(this, R.layout.userlist, ulist));

		ListView listView = getListView();
		listView.setTextFilterEnabled(true);

		listView.setOnItemClickListener(new OnItemClickListener() {			
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// When clicked, show a toast with the TextView text
				
			
				//send mac
				WifiManager mng = (WifiManager) getSystemService(WIFI_SERVICE);
				WifiInfo info = mng.getConnectionInfo();		
				mac = info.getMacAddress();	
				
				
			    onListItemClick(l, v, position, id)
			


											
				
				
				
				// send username
				SendPost4 sendPost1 = new SendPost4(UserList.this);
				sendPost1.execute(ulist.toString());
				
				
				SendPost3 sendPost = new SendPost3(UserList.this,mac,no);
				
				
				switchToContentList();		
				
			}
			
			
		});
		
	}
	
	
	private class SendPost3 extends AsyncTask<String, Void, String> {
		private Context context;
		private String mac;
		private String no;

		public SendPost3(Context context,String mac,String no) {
			// TODO Auto-generated constructor stub
			this.context = context;
			this.mac = mac;
			this.no = no;
		}
		

		protected String doInBackground(String... str) {
			String content = executeClient();
			return content;
		}

		protected void onPostExecsute(String result) {
			// 모두 작업을 마치고 실행할 일 (메소드 등등)

		}

		// 실제 전송하는 부분
		public String executeClient() {
			ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
			post.add(new BasicNameValuePair("address", mac));
			post.add(new BasicNameValuePair("no", no ));

			// 연결 HttpClient 객체 생성
			HttpClient client = new DefaultHttpClient();

			// 객체 연결 설정 부분, 연결 최대시간 등등
			HttpParams params = client.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 5000);
			HttpConnectionParams.setSoTimeout(params, 5000);

			// Post객체 생성
			HttpPost httpPost = new HttpPost(
					"http://192.168.2.24:52273/mac_add.json");

			try {
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(post, "UTF-8");
				httpPost.setEntity(entity);
				client.execute(httpPost);
				return EntityUtils.getContentCharSet(entity);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		}
	
	
	
	private class SendPost4 extends AsyncTask<String, Void, String> {
		private Context context;
		private String ulist;

		public SendPost4(Context context) {
			// TODO Auto-generated constructor stub
			this.context = context;
			
		}

		protected String doInBackground(String... str) {
			String content = executeClient(str[0]);
			return content;
		}

		protected void onPostExecute(String result) {
			// 모두 작업을 마치고 실행할 일 (메소드 등등)

		}

		// 실제 전송하는 부분
		public String executeClient(String str) {
			
			ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
			post.add(new BasicNameValuePair("username", ulist));

			// 연결 HttpClient 객체 생성
			HttpClient client = new DefaultHttpClient();

			// 객체 연결 설정 부분, 연결 최대시간 등등
			HttpParams params = client.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 5000);
			HttpConnectionParams.setSoTimeout(params, 5000);

			// Post객체 생성
			HttpPost httpPost = new HttpPost(
					"http://192.168.2.24:52273/userNo_search.json");

			try {
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(post,
						"UTF-8");
				httpPost.setEntity(entity);
				HttpResponse response = client.execute(httpPost);

				InputStream stream = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(stream));
				StringBuilder builder = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				String content = builder.toString();
				
				System.out.println(content);
//				JSONArray jsonArray = new JSONArray(content);
				
				
				JSONArray jArray = new JSONArray(content);
				no = jArray.toString();
				
				
				
				
				
				
				
			
								
							
				// return EntityUtils.getContentCharSet(entity1);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}
	

	public void switchToContentList() {
		Intent intent = new Intent(this, ContentActivity.class);
		intent.putExtra(ContentActivity.DEVICE_UUID, identity);
		startActivity(intent);
	}

	private void getJSONFromUrl() {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet("http://192.168.2.24:52273/user.json");
		HttpResponse response;
		try {
			response = httpClient.execute(httpGet);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = rd.readLine()) != null) {
				sb.append(line + "\n");
			}
			String json = sb.toString();
			rd.close();
			JSONArray jObj = new JSONArray(json);
			ulist = new String[jObj.length()];
			for (int i = 0; i < jObj.length(); i++) {
				String name = jObj.getJSONObject(i).getString("USERNAME");
				ulist[i] = name;
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		
		menu.add(0, 0, 0, R.string.add_user).setIcon(android.R.drawable.ic_menu_add);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
        case 0:
            AddUser();
            break;
	}
		return false;
}

	private void AddUser() {
		// TODO Auto-generated method stub
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Add User");
		alert.setMessage("추가할 사용자의 이름을 입력하십시오.");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				value.toString();
				
				
				
				SendPost sendPost = new SendPost(UserList.this);
				sendPost.execute(value.toString());
				
				
			}
		});


		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});
		
		alert.show(); 
		
	}
	
	private class SendPost extends AsyncTask<String, Void, String> {
		private Context context;
		public SendPost(Context context) {
			// TODO Auto-generated constructor stub
			this.context = context;
		}
		
		protected String doInBackground(String... str) {
			String content = executeClient(str[0]);
			return content;
		}

		protected void onPostExecute(String result)  {
			// 모두 작업을 마치고 실행할 일 (메소드 등등)
			
			
			getJSONFromUrl();
			((ListActivity)context).setListAdapter(new ArrayAdapter<String>(context, R.layout.userlist, ulist));
		}
		
	
		
		// 실제 전송하는 부분
		public String executeClient(String str) {
			ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
			post.add(new BasicNameValuePair("username",str));
			

			// 연결 HttpClient 객체 생성
			HttpClient client = new DefaultHttpClient();
			
			// 객체 연결 설정 부분, 연결 최대시간 등등
			HttpParams params = client.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 5000);
			HttpConnectionParams.setSoTimeout(params, 5000);
			
			// Post객체 생성
			HttpPost httpPost = new HttpPost("http://192.168.2.24:52273/user_add.json");
			
			try {
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(post, "UTF-8");
				httpPost.setEntity(entity);
				client.execute(httpPost);
				return EntityUtils.getContentCharSet(entity);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
}
	
	
	
}
