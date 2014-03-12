/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.android.browser;

import android.app.ListActivity;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.teleal.android.util.FixedAndroidHandler;
import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.action.ActionArgumentValue;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UDAServiceId;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;
import org.teleal.common.logging.LoggingUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Identity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Christian Bauer
 */
public class MainActivity extends ListActivity {

	private static Logger log = Logger.getLogger(MainActivity.class.getName());
	String identity;
	private ArrayAdapter<DeviceDisplay> deviceListAdapter;
	private ContentBrowserAdapter contentAdapter;

	private ListView listview;

	private BrowseRegistryListener registryListener = new BrowseRegistryListener();

	private AndroidUpnpService upnpService;

	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			upnpService = (AndroidUpnpService) service;

			// Refresh the list with all known devices
			deviceListAdapter.clear();
			for (Device device : upnpService.getRegistry().getDevices()) {
				registryListener.deviceAdded(device);
			}

			// Getting ready for future device advertisements
			upnpService.getRegistry().addListener(registryListener);
		}

		public void onServiceDisconnected(ComponentName className) {
			upnpService = null;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		// identity =
		// getIntent().getExtras().getString(ContentActivity.DEVICE_UUID);

		

		deviceListAdapter = new ArrayAdapter(this,
				android.R.layout.simple_list_item_1);
		listview = getListView();
		switchToDeviceList();

		getApplicationContext().bindService(
				new Intent(this, BrowserUpnpService.class), serviceConnection,
				Context.BIND_AUTO_CREATE);
	}



	

	public void switchToContentList(DeviceIdentity identity) {
		Intent intent = new Intent(this, UserList.class);
		intent.putExtra(ContentActivity.DEVICE_UUID, identity.getUdn()
				.toString());
		startActivity(intent);
	}

	public void switchToDeviceList() {
		setListAdapter(deviceListAdapter);

		/*
		 * Executes when the user (long) clicks on a device:
		 */
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				DeviceDisplay clickedDisplay = deviceListAdapter
						.getItem(position);
				if (clickedDisplay != null) {
					// ... clickedDisplay.getDevice();
					Service service = clickedDisplay.getDevice().findService(
							new UDAServiceId("SwitchPower"));
					if (service != null) {
						Action getStatusAction = service.getAction("GetTarget");
						ActionInvocation getStatusInvocation = new ActionInvocation(
								getStatusAction);

						new ActionCallback.Default(getStatusInvocation,
								upnpService.getControlPoint()).run();

						boolean value = ((Boolean) getStatusInvocation
								.getOutput("RetTargetValue").getValue())
								.booleanValue();

						Action setTargetAction = service.getAction("SetTarget");
						ActionInvocation setTargetInvocation = new ActionInvocation(
								setTargetAction);
						setTargetInvocation.setInput("NewTargetValue", !value);

						ActionCallback setTargetCallback = new ActionCallback(
								setTargetInvocation) {

							@Override
							public void success(ActionInvocation invocation) {
								ActionArgumentValue[] output = invocation
										.getOutput();
								// assertEquals(output.length, 0);
							}

							@Override
							public void failure(ActionInvocation invocation,
									UpnpResponse operation, String defaultMsg) {
								System.err.println(defaultMsg);
							}
						};

						upnpService.getControlPoint()
								.execute(setTargetCallback);
					}

					Service service2 = clickedDisplay.getDevice().findService(
							new UDAServiceId("ContentDirectory"));
					if (service2 != null) {
						DeviceIdentity identity = clickedDisplay.getDevice()
								.getIdentity();
						
						
						WifiManager mng = (WifiManager) getSystemService(WIFI_SERVICE);
						WifiInfo info = mng.getConnectionInfo();
						String mac = info.getMacAddress();

						SendPost2 sendPost = new SendPost2(MainActivity.this);
						sendPost.execute(mac.toString());
																		
						switchToContentList(identity);
					}
				}
			}
		});

	}
	
	
	private class SendPost2 extends AsyncTask<String, Void, String> {
		private Context context;

		public SendPost2(Context context) {
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
			post.add(new BasicNameValuePair("address", str));

			// 연결 HttpClient 객체 생성
			HttpClient client = new DefaultHttpClient();

			// 객체 연결 설정 부분, 연결 최대시간 등등
			HttpParams params = client.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 5000);
			HttpConnectionParams.setSoTimeout(params, 5000);

			// Post객체 생성
			HttpPost httpPost = new HttpPost(
					"http://192.168.2.24:52273/mac_search.json");

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
				JSONArray jsonArray = new JSONArray(content);
				
				if (jsonArray.length() == 0) {
					// 없음
					switchToContentList();
				} else {
					
					
					
					
					
					
					
					
					// 있음
				}
				return content;

				// return EntityUtils.getContentCharSet(entity1);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public void switchToContentList() {
		Intent intent = new Intent(this, UserList.class);
		intent.putExtra(ContentActivity.DEVICE_UUID, identity);
		startActivity(intent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (upnpService != null) {
			upnpService.getRegistry().removeListener(registryListener);
		}
		getApplicationContext().unbindService(serviceConnection);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, R.string.search_lan).setIcon(
				android.R.drawable.ic_menu_search);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			searchNetwork();
			break;

		}
		return false;
	}

	protected void searchNetwork() {
		if (upnpService == null)
			return;
		Toast.makeText(this, R.string.searching_lan, Toast.LENGTH_SHORT).show();
		upnpService.getRegistry().removeAllRemoteDevices();
		upnpService.getControlPoint().search();
	}

	protected class BrowseRegistryListener extends DefaultRegistryListener {

		/* Discovery performance optimization for very slow Android devices! */
		@Override
		public void remoteDeviceDiscoveryStarted(Registry registry,
				RemoteDevice device) {
			deviceAdded(device);
		}

		@Override
		public void remoteDeviceDiscoveryFailed(Registry registry,
				final RemoteDevice device, final Exception ex) {
			runOnUiThread(new Runnable() {
				public void run() {
					// Toast.makeText(
					// BrowseActivity.this,
					// "Discovery failed of '" + device.getDisplayString() +
					// "': " +
					// (ex != null ? ex.toString() :
					// "Couldn't retrieve device/service descriptors"),
					// Toast.LENGTH_LONG
					// ).show();
				}
			});
			deviceRemoved(device);
		}

		/*
		 * End of optimization, you can remove the whole block if your Android
		 * handset is fast (>= 600 Mhz)
		 */

		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
			deviceAdded(device);
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
			deviceRemoved(device);
		}

		@Override
		public void localDeviceAdded(Registry registry, LocalDevice device) {
			deviceAdded(device);
		}

		@Override
		public void localDeviceRemoved(Registry registry, LocalDevice device) {
			deviceRemoved(device);
		}

		public void deviceAdded(final Device device) {
			runOnUiThread(new Runnable() {
				public void run() {
					DeviceDisplay d = new DeviceDisplay(device);

					int position = deviceListAdapter.getPosition(d);
					if (position >= 0) {
						// Device already in the list, re-set new value at same
						// position
						deviceListAdapter.remove(d);
						deviceListAdapter.insert(d, position);
					} else {
						deviceListAdapter.add(d);
					}

					// Sort it?
					// listAdapter.sort(DISPLAY_COMPARATOR);
					// listAdapter.notifyDataSetChanged();
				}
			});
		}

		public void deviceRemoved(final Device device) {
			runOnUiThread(new Runnable() {
				public void run() {
					deviceListAdapter.remove(new DeviceDisplay(device));
				}
			});
		}
	}

	protected class DeviceDisplay {

		Device device;

		public DeviceDisplay(Device device) {
			this.device = device;
		}

		public Device getDevice() {
			return device;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			DeviceDisplay that = (DeviceDisplay) o;
			return device.equals(that.device);
		}

		@Override
		public int hashCode() {
			return device.hashCode();
		}

		@Override
		public String toString() {
			// Display a little star while the device is being loaded (see
			// performance optimization earlier)
			return device.isFullyHydrated() ? device.getDisplayString()
					: device.getDisplayString() + " *";
		}
	}

	static final Comparator<DeviceDisplay> DISPLAY_COMPARATOR = new Comparator<DeviceDisplay>() {
		public int compare(DeviceDisplay a, DeviceDisplay b) {
			return a.toString().compareTo(b.toString());
		}
	};

}
