package com.ileja.autoreply;
import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.IOException;
import java.util.List;
public class AutoReplyService extends AccessibilityService {
	private final static String MM_PNAME = "com.tencent.mm";
	boolean hasAction = false;
	boolean locked = false;
	boolean background = false;
	private String name;
	private String scontent;
	AccessibilityNodeInfo itemNodeinfo;
	private KeyguardManager.KeyguardLock kl;
	private Handler handler = new Handler();
	/**     * ������д�ķ�������Ӧ�����¼���     * @param event     */
	@Override
	public void onAccessibilityEvent(final AccessibilityEvent event) {
		int eventType = event.getEventType();
		android.util.Log.d("maptrix", "get event = " + eventType);
		switch (eventType) {
		case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:// ֪ͨ���¼�
			android.util.Log.d("maptrix", "get notification event");
			List<CharSequence> texts = event.getText();
			if (!texts.isEmpty()) {
for (CharSequence text : texts) {
					String content = text.toString();
					if (!TextUtils.isEmpty(content)) {
						if (isScreenLocked()) {
							locked = true;
							wakeAndUnlock();
							android.util.Log.d("maptrix", "the screen is locked");
							if (isAppForeground(MM_PNAME)) {
								background = false;
								android.util.Log.d("maptrix", "is mm in foreground");
								sendNotifacationReply(event);
								handler.postDelayed(new Runnable() {
									@Override                                        public void run() {
										sendNotifacationReply(event);
										if (fill()) {
											send();
										}
									}
								}, 1000);
							} else {
								background = true;
								android.util.Log.d("maptrix", "is mm in background");
								sendNotifacationReply(event);
							}
						} else {
							locked = false;
							android.util.Log.d("maptrix", "the screen is unlocked");
							// ������΢�ź����notification����֪ͨ
							if (isAppForeground(MM_PNAME)) {
								background = false;
								android.util.Log.d("maptrix", "is mm in foreground");
								sendNotifacationReply(event);
								handler.postDelayed(new Runnable() {
									@Override                                        public void run() {
										if (fill()) {
											send();
										}
									}
								}, 1000);
							} else {
								background = true;
								android.util.Log.d("maptrix", "is mm in background");
								sendNotifacationReply(event);
							}
						}
					}
				}
			}
			break;
		case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
			android.util.Log.d("maptrix", "get type window down event");
			if (!hasAction) break;
			itemNodeinfo = null;
			String className = event.getClassName().toString();
			if (className.equals("com.tencent.mm.ui.LauncherUI")) {
				if (fill()) {
					send();
				} else {
					if(itemNodeinfo != null) {
						itemNodeinfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
						handler.postDelayed(new Runnable() {
							@Override                                public void run() {
								if (fill()) {
									send();
								}
								back2Home();
								release();
								hasAction = false;
							}
						}, 1000);
						break;
					}
				}
			}

			//bring2Front();
			back2Home();
			release();
			hasAction = false;
			break;
		}
	}
	/**     * Ѱ�Ҵ����еġ����͡���ť�����ҵ����     */
	@SuppressLint("NewApi")
	private void send() {
		AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
		if (nodeInfo != null) {
			List<AccessibilityNodeInfo> list = nodeInfo                    .findAccessibilityNodeInfosByText("����");
			if (list != null && list.size() > 0) {
for (AccessibilityNodeInfo n : list) {
					if(n.getClassName().equals("android.widget.Button") && n.isEnabled())                    {
						n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
					}
				}
			} else {
				List<AccessibilityNodeInfo> liste = nodeInfo                        .findAccessibilityNodeInfosByText("Send");
				if (liste != null && liste.size() > 0) {
for (AccessibilityNodeInfo n : liste) {
						if(n.getClassName().equals("android.widget.Button") && n.isEnabled())                        {
							n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
						}
					}
				}
			}
		}
		pressBackButton();
	}
}
/**     * ģ��back����     */
private void pressBackButton() {
	Runtime runtime = Runtime.getRuntime();
	try {
		runtime.exec("input keyevent " + KeyEvent.KEYCODE_BACK);
	} catch (IOException e) {
		e.printStackTrace();
	}
}
/**     *     * @param event     */
private void sendNotifacationReply(AccessibilityEvent event) {
	hasAction = true;
	if (event.getParcelableData() != null                && event.getParcelableData() instanceof Notification) {
		Notification notification = (Notification) event                    .getParcelableData();
		String content = notification.tickerText.toString();
		String[] cc = content.split(":");
		name = cc[0].trim();
		scontent = cc[1].trim();
		android.util.Log.i("maptrix", "sender name =" + name);
		android.util.Log.i("maptrix", "sender content =" + scontent);
		PendingIntent pendingIntent = notification.contentIntent;
		try {
			pendingIntent.send();
		} catch (PendingIntent.CanceledException e) {
			e.printStackTrace();
		}
	}
}
@SuppressLint("NewApi")
private boolean fill() {
	AccessibilityNodeInfo rootNode = getRootInActiveWindow();
	if (rootNode != null) {
		return findEditText(rootNode, "����æ,�Ժ�ظ���");
	}
	return false;
}
private boolean findEditText(AccessibilityNodeInfo rootNode, String content) {
	int count = rootNode.getChildCount();
	android.util.Log.d("maptrix", "root class=" + rootNode.getClassName() + ","+ rootNode.getText()+","+count);
	for (int i = 0; i < count; i++) {
		AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
		if (nodeInfo == null) {
			android.util.Log.d("maptrix", "nodeinfo = null");
			continue;
		}
		android.util.Log.d("maptrix", "class=" + nodeInfo.getClassName());
		android.util.Log.e("maptrix", "ds=" + nodeInfo.getContentDescription());
		if(nodeInfo.getContentDescription() != null) {
			int nindex = nodeInfo.getContentDescription().toString().indexOf(name);
			int cindex = nodeInfo.getContentDescription().toString().indexOf(scontent);
			android.util.Log.e("maptrix", "nindex=" + nindex + " cindex=" +cindex);
			if(nindex != -1) {
				itemNodeinfo = nodeInfo;
				android.util.Log.i("maptrix", "find node info");
			}
		}
		if ("android.widget.EditText".equals(nodeInfo.getClassName())) {
			android.util.Log.i("maptrix", "==================");
			Bundle arguments = new Bundle();
			arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,  AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
			arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, true);
			nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,arguments);
			nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
			ClipData clip = ClipData.newPlainText("label", content);
			ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			clipboardManager.setPrimaryClip(clip);
			nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
			return true;
		}
		if (findEditText(nodeInfo, content)) {
			return true;
		}
	}
	return false;
}    @Override
public void onInterrupt() {    }
/**     * �ж�ָ����Ӧ���Ƿ���ǰ̨����     *     * @param packageName     * @return     */
private boolean isAppForeground(String packageName) {
	ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
	String currentPackageName = cn.getPackageName();
	if (!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(packageName)) {
		return true;
	}
	return false;
}
/**     * ����ǰӦ�����е�ǰ̨     */    private void bring2Front() {
	ActivityManager activtyManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	List<ActivityManager.RunningTaskInfo> runningTaskInfos = activtyManager.getRunningTasks(3);
for (ActivityManager.RunningTaskInfo runningTaskInfo : runningTaskInfos) {
		if (this.getPackageName().equals(runningTaskInfo.topActivity.getPackageName())) {
			activtyManager.moveTaskToFront(runningTaskInfo.id, ActivityManager.MOVE_TASK_WITH_HOME);
			return;
		}
	}
}    /**     * �ص�ϵͳ����     */    private void back2Home() {
	Intent home = new Intent(Intent.ACTION_MAIN);
	home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	home.addCategory(Intent.CATEGORY_HOME);
	startActivity(home);
}
/**     * ϵͳ�Ƿ�������״̬     *     * @return     */
private boolean isScreenLocked() {
	KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
	return keyguardManager.inKeyguardRestrictedInputMode();
}
private void wakeAndUnlock() {
	//��ȡ��Դ����������
	PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	//��ȡPowerManager.WakeLock���󣬺���Ĳ���|��ʾͬʱ��������ֵ�������ǵ����õ�Tag
	PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
	//������Ļ
	wl.acquire(1000);
	//�õ�����������������
	KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
	kl = km.newKeyguardLock("unLock");
	//����
	kl.disableKeyguard();
}
private void release() {
	if (locked && kl != null) {
		android.util.Log.d("maptrix", "release the lock");
		//�õ�����������������
		kl.reenableKeyguard();
		locked = false;
	}
}
}