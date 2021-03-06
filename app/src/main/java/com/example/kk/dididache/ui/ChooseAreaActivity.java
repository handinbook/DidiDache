package com.example.kk.dididache.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Explode;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.example.kk.dididache.MyOverShootInterpolator;
import com.example.kk.dididache.R;
import com.example.kk.dididache.control.adapter.MySearchItemAdapter;
import com.example.kk.dididache.control.adapter.SearchItemAdapter;
import com.example.kk.dididache.model.Location;

import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ChooseAreaActivity extends AppCompatActivity implements OnGetSuggestionResultListener {

    private List<Location> locations = new ArrayList<>();
    private List<String> mSuggest;
    private List<SuggestionResult.SuggestionInfo> infoList;
    private AutoCompleteTextView nodeText;
    private ArrayAdapter<String> nodeAdapter;
    private SuggestionSearch mSuggestionSearch = null;
    private LinearLayout itemLinear;
    public static final String LATLNG_BACK = "latlng_back";
    public static final String NAME_BACK = "name_back";
    private ImageView onePersonImage;
    private ImageView twoPersonImage;
    private ImageView backImage;
    private CardView myLocaLinear;
    private ListView mListView;
    private int nodeTextLeft;
    private View scrim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setExitTransition(new Explode());
        }
        setContentView(R.layout.activity_choose_area);

        scrim = (View) findViewById(R.id.scrim);

        nodeText = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        nodeText.setDropDownVerticalOffset(5);
        nodeText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                scrim.setVisibility(View.GONE);
            }
        });

        ViewTreeObserver vto1 = nodeText.getViewTreeObserver();
        vto1.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                nodeTextLeft = nodeText.getLeft();
            }
        });

        mSuggestionSearch = SuggestionSearch.newInstance();
        mSuggestionSearch.setOnGetSuggestionResultListener(this);
        nodeText.setThreshold(1);

        /**
         * 当输入关键字变化时，动态更新建议列表
         */
        nodeText.addTextChangedListener(new TextWatcher() {


            @Override
            public void afterTextChanged(Editable arg0) {

            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2,
                                      int arg3) {
                if (cs.length() <= 0) {
                    return;
                }

                /**
                 * 使用建议搜索服务获取建议列表，结果在onSuggestionResult()中更新
                 */
                mSuggestionSearch
                        .requestSuggestion((new SuggestionSearchOption())
                                .keyword(cs.toString()).city("广州").citylimit(true));
            }
        });

        nodeText.setOnDismissListener(new AutoCompleteTextView.OnDismissListener() {
            @Override
            public void onDismiss() {
                scrim.setVisibility(View.GONE);
            }
        });


        nodeText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LatLng latLng = infoList.get(position).pt;
                Intent intent = new Intent();
                intent.putExtra(LATLNG_BACK, latLng);
                intent.putExtra(NAME_BACK, mSuggest.get(position));
                Location location = new Location(mSuggest.get(position), latLng.latitude, latLng.longitude);
                for(Location location2 : locations){
                    if(location2.getLongtitude() == latLng.longitude && location2.getLatitude() == latLng.latitude){
                        setResult(RESULT_OK, intent);
                        finish();
                        return;
                    }
                }
                location.save();
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        twoPersonImage = (ImageView) findViewById(R.id.two_person_image);
        onePersonImage = (ImageView) findViewById(R.id.one_person_image);
        onePersonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                personAnimation();
            }
        });

        backImage = (ImageView) findViewById(R.id.back_image);
        backImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAfterTransition();
                }else {
                    finish();
                }
            }
        });

        Connector.getDatabase();
        List<String> nameList = new ArrayList<>();
        locations = DataSupport.findAll(Location.class);
        if (locations.size() >= 2){
            onePersonImage.setVisibility(View.GONE);
            twoPersonImage.setVisibility(View.GONE);
        }
        locations.add(new Location("我的位置", 0 ,0));
        Collections.reverse(locations);
        if(locations.size() > 20){
            for(int a = 20; a < locations.size(); a++){
                locations.remove(a);
            }
        }
        for(Location location : locations){
            nameList.add(location.getName());
        }
        mListView = (ListView) findViewById(R.id.list_view);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.item_enter);
        animation.setStartOffset(500);
        LayoutAnimationController lac = new LayoutAnimationController(animation);
        lac.setOrder(LayoutAnimationController.ORDER_NORMAL);
        mListView.setLayoutAnimation(lac);
        mListView.setAdapter(new MySearchItemAdapter(this, nameList, locations));
        for(String name : nameList){
            Log.d("999", name);
        }
        for (Location location : locations){
            Log.d("999", location.getLatitude() + ":" + location.getLongtitude());
        }
    }


    /**
     * 获取在线建议搜索结果，得到requestSuggestion返回的搜索结果
     * @param res
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onGetSuggestionResult(SuggestionResult res) {
        if(res == null || res.getAllSuggestions() == null){
            return;
        }
        scrim.setVisibility(View.VISIBLE);
            infoList = res.getAllSuggestions();
            mSuggest = new ArrayList<>();
            for (SuggestionResult.SuggestionInfo info : res.getAllSuggestions()) {
                if (info.key != null) {
                    mSuggest.add(info.key);
                }
            }
            nodeAdapter = new SearchItemAdapter(this, mSuggest);
            nodeText.setAdapter(nodeAdapter);
            nodeAdapter.notifyDataSetChanged();
        }

    @Override
    protected void onDestroy() {
        mSuggestionSearch.destroy();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        personAnimation();
        super.onResume();
    }

    private void personAnimation(){
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.bigloolscale);
        onePersonImage.startAnimation(animation);
//        Animation animation2 = AnimationUtils.loadAnimation(this, R.anim.bigloolscale);
//        twoPersonImage.startAnimation(animation2);
    }
}
