package news.cnr.cn.servers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import news.cnr.cn.HiveViewCNRApplication;
import news.cnr.cn.constant.ApiConstant;
import news.cnr.cn.entity.AnchorDetailEntity;
import news.cnr.cn.entity.AnchorEntity;
import news.cnr.cn.entity.AnchorProgramEntity;
import news.cnr.cn.entity.AnchorTrendEntity;
import news.cnr.cn.entity.ApiKeyEntity;
import news.cnr.cn.entity.BidOrTrackCommentEntity;
import news.cnr.cn.entity.BroadcastEntity;
import news.cnr.cn.entity.ChannelItem;
import news.cnr.cn.entity.CommentEntity;
import news.cnr.cn.entity.DiscloseEntity;
import news.cnr.cn.entity.DiscloseOrTrackDetailEntity;
import news.cnr.cn.entity.EveryAlbumItemEntity;
import news.cnr.cn.entity.ExtendNewsEntity;
import news.cnr.cn.entity.FocusNewsEntity;
import news.cnr.cn.entity.InquireEntity;
import news.cnr.cn.entity.LoginEntity;
import news.cnr.cn.entity.NewsDetailEntity;
import news.cnr.cn.entity.NewsEntity;
import news.cnr.cn.entity.PersonInfoById;
import news.cnr.cn.entity.ReadLetterEntity;
import news.cnr.cn.entity.TemplateEntity;
import news.cnr.cn.entity.UpdateVersionEntity;
import news.cnr.cn.entity.User_Collection;
import news.cnr.cn.entity.User_Friend;
import news.cnr.cn.entity.User_Message;
import news.cnr.cn.entity.MyAttentEntity;
import news.cnr.cn.entity.User_sendjoinconcerned_entity;
import news.cnr.cn.entity.UserinfoEntity;
import news.cnr.cn.entity.VerifyFriend;
import news.cnr.cn.entity.VoteEntity;
import news.cnr.cn.utils.Logger;
import news.cnr.cn.utils.SharedPreferencesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
//
public class NetWorkController {
	private Context mContext;
	private NetWorkCallBack mCallBack;
	private Gson gson = new Gson();
	private boolean canCache = false;
//实例化的时候，可自己控制是否缓存；
	public NetWorkController(Context ctx, NetWorkCallBack callback,
			boolean isCache) {
		this.mContext = ctx;
		this.mCallBack = callback;
		canCache = isCache;
	}
//下面是一个post请i球的例子和一个get请求的例子 需要post的map提前准备好
//其中采用gson解析，需要你自己的bean类和json数据里的字段完全对应
	public void getApiKey(String devInfo, HashMap<String, String> map) {
		String url = String.format(ApiConstant.CHECK_SERVICE, devInfo);
		Logger.e("==", "dev check url :" + url);
		volleyPost(url, ApiKeyEntity.class, map);
	}

	/**
	 * get the news in splash
	 */
	public void getSplahNews(){
		String url=ApiConstant.GET_SPLASH_NEWS;
		volleyGet(url, NewsEntity.class);
	}

	// ------------------------------------我才不是分割线---------------------------------------
	/**
	 * @param url
	 * @param cls
	 *            通用的volley get请求 统一Response为String 通过GSON 解析成类或者lsit
	 *            所用的entity里每个属性需要按照后台接口提供的数据来创建
	 */
	public <T> void volleyGet(final String url, final Class<T> cls) {
		Log.e("==", "请求的url:" + url);
		StringRequest getRequest = new StringRequest(url,
				new Response.Listener<String>() {
					@Override
					public void onResponse(String response) {
						Logger.e("==", "controller response:" + response);
						if(canCache){
							SharedPreferencesUtil.saveCache(mContext, url, response);
						}
						Object json;
						try {
							JSONObject jo1 = new JSONObject(response);
							mCallBack.respCode(jo1.getString("code"));
							if (cls != null) {
								json = new JSONTokener(jo1.getString("data"))
										.nextValue();
								// json = new JSONTokener(response).nextValue();
								if (json instanceof JSONObject) {
									JSONObject obj = (JSONObject) json;
									T entity = gson.fromJson(obj.toString(),
											(Class<T>) cls);
									mCallBack.loadDone(entity);
								} else if (json instanceof JSONArray) {
									JSONArray ja = (JSONArray) json;
									List<T> list = new ArrayList<T>();
									for (int i = 0; i < ja.length(); i++) {
										list.add(gson.fromJson(ja.get(i)
												.toString(), cls));
									}
									mCallBack.loadDone(list);
								}
							}
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						if (error != null) {
							Logger.e("==", "volley error :" + error.toString());
						}
					}
				});
		if(NetWorkUtils.isNetworkConnected(mContext)){//这里的网络环境判断调用系统的就行很简单，就不单独说明了
			HiveViewCNRApplication.getInstances().getQueue().add(getRequest);
		}else{//如果网络环境不佳  读取这个json的缓存
			String response=SharedPreferencesUtil.getCache(mContext, url);
			if(response==null) return;
			Object json;
			try {
				JSONObject jo1 = new JSONObject(response);
				mCallBack.respCode(jo1.getString("code"));
				if (cls != null) {
					json = new JSONTokener(jo1.getString("data"))
							.nextValue();
					// json = new JSONTokener(response).nextValue();
					if (json instanceof JSONObject) {
						JSONObject obj = (JSONObject) json;
						T entity = gson.fromJson(obj.toString(),
								(Class<T>) cls);
						mCallBack.loadDone(entity);
					} else if (json instanceof JSONArray) {
						JSONArray ja = (JSONArray) json;
						List<T> list = new ArrayList<T>();
						for (int i = 0; i < ja.length(); i++) {
							list.add(gson.fromJson(ja.get(i)
									.toString(), cls));
						}
						mCallBack.loadDone(list);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param url
	 * @param cls
	 * @param map
	 *            volley post方法
	 */
	public <T> void volleyPost(final String url, final Class<T> cls,
			final HashMap<String, String> map) {
		Log.e("==", "请求的url:" + url);
		StringRequest postRequest = new StringRequest(Method.POST, url,
				new Response.Listener<String>() {

					@Override
					public void onResponse(String response) {
						Logger.e("==", "controller response:" + response);
						if(canCache){
							SharedPreferencesUtil.saveCache(mContext, url, response);
						}
						Object json;
						try {
							JSONObject jo1 = new JSONObject(response);
							mCallBack.respCode(jo1.getString("code"));
							if (cls != null) {
								json = new JSONTokener(jo1.getString("data"))
										.nextValue();
								if (json instanceof JSONObject) {
									JSONObject obj = (JSONObject) json;
									T entity = gson.fromJson(obj.toString(),
											(Class<T>) cls);
									mCallBack.loadDone(entity);
								} else if (json instanceof JSONArray) {
									JSONArray ja = (JSONArray) json;
									List<T> list = new ArrayList<T>();
									for (int i = 0; i < ja.length(); i++) {
										list.add(gson.fromJson(ja.get(i)
												.toString(), cls));
									}
									mCallBack.loadDone(list);
								}
							}
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}, new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						// TODO Auto-generated method stub

					}
				}) {

			@Override
			protected Map<String, String> getParams() throws AuthFailureError {
				// TODO Auto-generated method stub
				return map;
			}

		};
		if(NetWorkUtils.isNetworkConnected(mContext)){
			HiveViewCNRApplication.getInstances().getQueue().add(postRequest);
		}else{//如果网络环境不佳  读取这个json的缓存
			String response=SharedPreferencesUtil.getCache(mContext, url);
			if(response==null) return;
			Object json;
			try {
				JSONObject jo1 = new JSONObject(response);
				mCallBack.respCode(jo1.getString("code"));
				if (cls != null) {
					json = new JSONTokener(jo1.getString("data"))
							.nextValue();
					// json = new JSONTokener(response).nextValue();
					if (json instanceof JSONObject) {
						JSONObject obj = (JSONObject) json;
						T entity = gson.fromJson(obj.toString(),
								(Class<T>) cls);
						mCallBack.loadDone(entity);
					} else if (json instanceof JSONArray) {
						JSONArray ja = (JSONArray) json;
						List<T> list = new ArrayList<T>();
						for (int i = 0; i < ja.length(); i++) {
							list.add(gson.fromJson(ja.get(i)
									.toString(), cls));
						}
						mCallBack.loadDone(list);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
//这块是接口回调  只是应用这个技术 这个接口怎么定义看自己的需求
	public interface NetWorkCallBack {
		public <T> void loadDone(T entity);

		public void respCode(String code);

		public <T> void loadDone(List<T> list);
	}

}
