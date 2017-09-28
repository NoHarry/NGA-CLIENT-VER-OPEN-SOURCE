package gov.anzong.androidnga.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;

import gov.anzong.androidnga.R;
import sp.phone.adapter.ActionBarUserListAdapter;
import sp.phone.adapter.MessageListAdapter;
import sp.phone.adapter.SpinnerUserListAdapter;
import sp.phone.bean.MessageListInfo;
import sp.phone.bean.MessageThreadPageInfo;
import sp.phone.bean.User;
import sp.phone.common.PhoneConfiguration;
import sp.phone.common.ThemeManager;
import sp.phone.common.UserManagerImpl;
import sp.phone.fragment.MessageDetialListContainer;
import sp.phone.fragment.MessageListContainer;
import sp.phone.fragment.TopicListContainer;
import sp.phone.fragment.material.MessageListFragment;
import sp.phone.interfaces.EnterJsonMessageThread;
import sp.phone.interfaces.OnChildFragmentRemovedListener;
import sp.phone.interfaces.OnMessageListLoadFinishedListener;
import sp.phone.interfaces.PagerOwner;
import sp.phone.interfaces.PullToRefreshAttacherOwner;
import sp.phone.utils.ActivityUtils;
import sp.phone.utils.NLog;
import sp.phone.utils.ReflectionUtil;
import sp.phone.utils.StringUtils;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshAttacher;

public class FlexibleMessageListActivity extends SwipeBackAppCompatActivity
        implements OnMessageListLoadFinishedListener, OnItemClickListener,
        PagerOwner,
        OnChildFragmentRemovedListener, PullToRefreshAttacherOwner,
        MessageDetialListContainer.OnMessageDetialListContainerListener, MessageListContainer.OnMessagelistContainerListener {

    boolean dualScreen = true;
    int flags = ThemeManager.ACTION_BAR_FLAG;
    MessageListInfo result = null;
    View view;
    int nightmode;
    private String TAG = FlexibleMessageListActivity.class.getSimpleName();
    private PullToRefreshAttacher mPullToRefreshAttacher;
    private OnItemClickListener onItemClickNewActivity = null;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        view = LayoutInflater.from(this).inflate(R.layout.messagelist_activity, null);
        Intent intent = getIntent();
        boolean isfullScreen = intent.getBooleanExtra("isFullScreen", false);
        if (isfullScreen) {
            ActivityUtils.getInstance().setFullScreen(view);
        }
        this.setContentView(view);
        nightmode = ThemeManager.getInstance().getMode();
        if (PhoneConfiguration.getInstance().isMaterialMode()){
            if (arg0 == null) {
                getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new MessageListFragment()).commit();
            }
            view.setVisibility(View.GONE);
            dualScreen = false;
            return;
        }
        PullToRefreshAttacher.Options options = new PullToRefreshAttacher.Options();
        options.refreshScrollDistance = 0.3f;
        options.refreshOnUp = true;
        mPullToRefreshAttacher = PullToRefreshAttacher.get(this, options);

        setNfcCallBack();

        if (null == findViewById(R.id.item_detail_container)) {
            dualScreen = false;
        }
        FragmentManager fm = getSupportFragmentManager();
        Fragment f1 = fm.findFragmentById(R.id.item_list);
        if (f1 == null) {
            f1 = new MessageListContainer();
            Bundle args = new Bundle();// (getIntent().getExtras());
            if (null != getIntent().getExtras()) {
                args.putAll(getIntent().getExtras());
            }
            f1.setArguments(args);
            FragmentTransaction ft = fm.beginTransaction().add(R.id.item_list,
                    f1);
            // .add(R.id.item_detail_container, f);
            ft.commit();
        }
        setNavigation();
        Fragment f2 = fm.findFragmentById(R.id.item_detail_container);
        if (null == f2) {
            f1.setHasOptionsMenu(true);
        } else if (!dualScreen) {
            fm.beginTransaction().remove(f2).commit();
            f1.setHasOptionsMenu(true);
        } else {
            f1.setHasOptionsMenu(false);
            f2.setHasOptionsMenu(true);
        }
        if (ThemeManager.getInstance().getMode() == ThemeManager.MODE_NIGHT) {
            FrameLayout v = (FrameLayout) view.findViewById(R.id.item_detail_container);
            if (v != null)
                v.setBackgroundResource(R.color.night_bg_color);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Fragment f1 = getSupportFragmentManager().findFragmentById(R.id.item_list);
        Fragment f2 = getSupportFragmentManager().findFragmentById(R.id.item_detail_container);
        if (f1 != null){
            f1.onPrepareOptionsMenu(menu);
        }
        if (f2 != null && dualScreen)
            f2.onPrepareOptionsMenu(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @TargetApi(11)
    private void setNavigation() {

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setTitle("短消息");
        final SpinnerUserListAdapter categoryAdapter = new ActionBarUserListAdapter(this);

        OnNavigationListener callback = new OnNavigationListener() {

            @Override
            public boolean onNavigationItemSelected(int itemPosition,
                                                    long itemId) {
                UserManagerImpl.getInstance().setActiveUser(itemPosition);
                MessageListContainer f1 = (MessageListContainer) getSupportFragmentManager().findFragmentById(R.id.item_list);
                if (f1 != null) {
                    f1.onCategoryChanged(itemPosition);
                }
                Fragment f2 = getSupportFragmentManager().findFragmentById(R.id.item_detail_container);
                if (f2 != null) {
                    getSupportFragmentManager().beginTransaction().remove(f2).commit();
                    if (f1 != null) {
                        f1.setHasOptionsMenu(true);
                    }
                }
                return true;
            }
        };
        actionBar.setListNavigationCallbacks(categoryAdapter, callback);

    }//设置头上一堆人

    @TargetApi(14)
    void setNfcCallBack() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        CreateNdefMessageCallback callback = new CreateNdefMessageCallback() {

            @Override
            public NdefMessage createNdefMessage(NfcEvent event) {
                FragmentManager fm = getSupportFragmentManager();
                TopicListContainer f1 = (TopicListContainer) fm
                        .findFragmentById(R.id.item_list);
                final String url = f1.getNfcUrl();
                NdefMessage msg = new NdefMessage(
                        new NdefRecord[]{NdefRecord.createUri(url)});
                return msg;
            }

        };
        if (adapter != null) {
            adapter.setNdefPushMessageCallback(callback, this);

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (PhoneConfiguration.getInstance().isMaterialMode()){
            return super.onCreateOptionsMenu(menu);
        }

        ReflectionUtil.actionBar_setDisplayOption(this, flags);
        return false;// super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        if (nightmode != ThemeManager.getInstance().getMode()) {
            onModeChanged();
            invalidateOptionsMenu();
            nightmode = ThemeManager.getInstance().getMode();
        }
        int orentation = ThemeManager.getInstance().screenOrentation;
        if (orentation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                || orentation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setRequestedOrientation(orentation);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
        if (PhoneConfiguration.getInstance().fullscreen) {
            ActivityUtils.getInstance().setFullScreen(view);
        }
        super.onResume();
    }

    @Override
    public void jsonfinishLoad(MessageListInfo result) {
        Fragment messageContainer = getSupportFragmentManager().findFragmentById(
                R.id.item_list);
        this.result = result;
        OnMessageListLoadFinishedListener listener = null;
        try {
            listener = (OnMessageListLoadFinishedListener) messageContainer;
            if (listener != null)
                listener.jsonfinishLoad(result);
        } catch (ClassCastException e) {
            NLog.e(TAG, "topicContainer should implements " + OnMessageListLoadFinishedListener.class.getCanonicalName());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!dualScreen) {// 非平板
            if (null == onItemClickNewActivity) {
                onItemClickNewActivity = new EnterJsonMessageThread(this);
            }
            onItemClickNewActivity.onItemClick(parent, view, position, id);

        } else {
            String guid = (String) parent.getItemAtPosition(position);
            if (StringUtils.isEmpty(guid))
                return;

            guid = guid.trim();

            int mid = StringUtils.getUrlParameter(guid, "mid");
            Fragment f = new MessageDetialListContainer();
            Bundle args = new Bundle();// (getIntent().getExtras());
            args.putInt("mid", mid);
            f.setArguments(args);
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.item_detail_container, f);
            Fragment f1 = fm.findFragmentById(R.id.item_list);
            f1.setHasOptionsMenu(false);
            f.setHasOptionsMenu(true);
            ft.commit();

            ListView listview = (ListView) parent;
            Object a = parent.getAdapter();
            MessageListAdapter adapter = null;
            if (a instanceof MessageListAdapter) {
                adapter = (MessageListAdapter) a;
            } else if (a instanceof HeaderViewListAdapter) {
                HeaderViewListAdapter ha = (HeaderViewListAdapter) a;
                adapter = (MessageListAdapter) ha.getWrappedAdapter();
                position -= ha.getHeadersCount();
            }
            adapter.setSelected(position);
            listview.setItemChecked(position, true);

        }

    }

    @Override
    public int getCurrentPage() {
        PagerOwner child = null;
        try {

            Fragment articleContainer = getSupportFragmentManager()
                    .findFragmentById(R.id.item_detail_container);
            child = (PagerOwner) articleContainer;
            if (null == child)
                return 0;
            return child.getCurrentPage();
        } catch (ClassCastException e) {
            NLog.e(TAG,
                    "fragment in R.id.item_detail_container does not implements interface "
                            + PagerOwner.class.getName());
            return 0;
        }

    }

    @Override
    public void setCurrentItem(int index) {
        PagerOwner child = null;
        try {

            Fragment articleContainer = getSupportFragmentManager()
                    .findFragmentById(R.id.item_detail_container);
            child = (PagerOwner) articleContainer;
            child.setCurrentItem(index);
        } catch (ClassCastException e) {
            NLog.e(TAG,
                    "fragment in R.id.item_detail_container does not implements interface "
                            + PagerOwner.class.getName());
            return;
        }

    }

    @Override
    public void OnChildFragmentRemoved(int id) {
        if (id == R.id.item_detail_container) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment f1 = fm.findFragmentById(R.id.item_list);
            f1.setHasOptionsMenu(true);
            getSupportActionBar().setTitle("短消息");
        }

    }

    @Override
    public PullToRefreshAttacher getAttacher() {
        return mPullToRefreshAttacher;
    }

    public MessageThreadPageInfo getEntry(int position) {
        if (result != null)
            return result.getMessageEntryList().get(position);
        return null;
    }

    @Override
    public void onModeChanged() {
        // TODO Auto-generated method stub
        Fragment f1 = getSupportFragmentManager().findFragmentById(R.id.item_list);
        if (f1 != null) {
            ((MessageListContainer) f1).changedmode();
        }
    }

    @Override
    public void onAnotherModeChanged() {
        // TODO Auto-generated method stub
        nightmode = ThemeManager.getInstance().getMode();
        Fragment f2 = getSupportFragmentManager().findFragmentById(R.id.item_detail_container);
        if (f2 != null) {
            ((MessageDetialListContainer) f2).changemode();
        } else {
            FrameLayout v = (FrameLayout) view.findViewById(R.id.item_detail_container);
            if (v != null) {
                if (ThemeManager.getInstance().getMode() == ThemeManager.MODE_NIGHT) {
                    v.setBackgroundResource(R.color.night_bg_color);
                } else {
                    v.setBackgroundResource(R.color.shit1);
                }
            }
        }
    }


}
