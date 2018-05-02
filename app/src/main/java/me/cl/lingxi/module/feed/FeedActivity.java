package me.cl.lingxi.module.feed;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import me.cl.library.base.BaseActivity;
import me.cl.library.view.LoadingDialog;
import me.cl.lingxi.R;
import me.cl.lingxi.adapter.EvaluateAdapter;
import me.cl.lingxi.common.config.Api;
import me.cl.lingxi.common.config.Constants;
import me.cl.lingxi.common.okhttp.OkUtil;
import me.cl.lingxi.common.okhttp.ResultCallback;
import me.cl.lingxi.common.util.SPUtil;
import me.cl.lingxi.common.util.Utils;
import me.cl.lingxi.entity.Comment;
import me.cl.lingxi.entity.Feed;
import me.cl.lingxi.entity.Like;
import me.cl.lingxi.entity.PageInfo;
import me.cl.lingxi.entity.Reply;
import me.cl.lingxi.entity.Result;
import me.cl.lingxi.entity.User;
import me.cl.lingxi.module.member.UserActivity;
import okhttp3.Call;

public class FeedActivity extends BaseActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.user_img)
    ImageView mUserImg;
    @BindView(R.id.user_name)
    TextView mUserName;
    @BindView(R.id.mood_time)
    TextView mMoodTime;
    @BindView(R.id.mood_info)
    AppCompatTextView mMoodInfo;
    @BindView(R.id.mood_body)
    LinearLayout mMoodBody;
    @BindView(R.id.mf_see_num)
    TextView mMfSeeNum;
    @BindView(R.id.mf_comment_num)
    TextView mMfCommentNum;
    @BindView(R.id.mf_comment)
    LinearLayout mMfComment;
    @BindView(R.id.mf_like_icon)
    ImageView mMfLikeIcon;
    @BindView(R.id.mf_like_num)
    TextView mMfLikeNum;
    @BindView(R.id.mf_like)
    LinearLayout mMfLike;
    @BindView(R.id.mood_action)
    LinearLayout mMoodAction;
    @BindView(R.id.like_people)
    TextView mLikePeople;
    @BindView(R.id.like_window)
    LinearLayout mLikeWindow;
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @BindView(R.id.edit_mask)
    View mEditMask;
    @BindView(R.id.edit_tu_cao)
    AppCompatEditText mEditTuCao;
    @BindView(R.id.btn_publish)
    Button mBtnPublish;

    private int MSG_MODE;
    private final int MSG_EVALUATE = 0;
    private final int MSG_REPLY = 1;

    private String saveId;
    private String mFeedId;
    private String mCommentId;
    private String toUid;
    private String toName;
    private InputMethodManager imm;
    private EvaluateAdapter mAdapter;
    private LoadingDialog loadingProgress;

    private Feed feed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        ButterKnife.bind(this);
        init();
    }

    private void init() {
        setupToolbar(mToolbar, R.string.title_activity_feed, true, 0, null);

        saveId = SPUtil.build().getString(Constants.USER_ID);

        //输入状态模式默认为评论
        MSG_MODE = MSG_EVALUATE;
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mEditTuCao.addTextChangedListener(new EditTextWatcher());
        // 开始禁用
        mBtnPublish.setClickable(false);
        mBtnPublish.setSelected(false);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new EvaluateAdapter(this, new ArrayList<Comment>());
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemListener(new EvaluateAdapter.OnItemListener() {
            @Override
            public void onItemClick(View view, Comment comment) {
                switch (view.getId()) {
                    case R.id.user_img:
                        gotoUser();
                        break;
                    case R.id.evaluate_body:
                        MSG_MODE = MSG_REPLY;
                        mCommentId = String.valueOf(comment.getId());
                        toUid = comment.getUser().getId();
                        mEditTuCao.setHint("回复：" + comment.getUser().getUsername());
                        openSofInput(mEditTuCao);
                        mEditMask.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onItemChildClick(View view, String eid, Reply reply) {
                MSG_MODE = MSG_REPLY;
                mCommentId = eid;
                toUid = reply.getUser().getId();
                mEditTuCao.setHint("回复：" + reply.getUser().getUsername());
                openSofInput(mEditTuCao);
                mEditMask.setVisibility(View.VISIBLE);
            }
        });

        initView();
    }

    private void gotoUser() {
        Intent intent = new Intent(this, UserActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("feed", feed);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void initView() {
        Bundle bundle = this.getIntent().getExtras();
        if (bundle == null) return;
        feed = (Feed) bundle.getSerializable("feed");
        if (feed == null) return;

        mFeedId = feed.getId();

        User user = feed.getUser();
        toUid = user.getId();

        //动态详情
        Glide.with(this)
                .load(Constants.IMG_URL + user.getAvatar())
                .placeholder(R.drawable.img_user)
                .error(R.drawable.img_user)
                .bitmapTransform(new CropCircleTransformation(this))
                .into(mUserImg);
        mUserName.setText(user.getUsername());
        mMoodTime.setText(feed.getCreateTime());
        mMoodInfo.setText(feed.getFeedInfo());
        //查看评论点赞数
        mMfSeeNum.setText(String.valueOf(feed.getViewNum()));
        mMfCommentNum.setText(String.valueOf(feed.getCommentNum()));
        //是否已经点赞
        mMfLikeIcon.setSelected(feed.isLike());
        mMfLike.setClickable(feed.isLike());
        //点赞列表
        List<Like> likeList = feed.getLikeList();
        Integer likeNum = likeList == null ? 0 : likeList.size();
        switch (likeNum) {
            case 0:
                mMfLikeNum.setText("赞");
                mLikeWindow.setVisibility(View.GONE);
                break;
            default:
                String likeStr = Utils.getLongLikeStr(likeList);
                mMfLikeNum.setText(String.valueOf(likeNum));
                mLikeWindow.setVisibility(View.VISIBLE);
                likeStr = likeStr + "觉得很赞";
                mLikePeople.setText(Utils.colorFormat(likeStr));
                break;
        }

        postViewFeed();
        getEvaluateList(String.valueOf(feed.getId()));
    }

    private void postViewFeed() {
        OkUtil.post()
                .url(Api.viewFeed)
                .addParam("id", mFeedId)
                .execute(new ResultCallback<Result>() {
                    @Override
                    public void onSuccess(Result response) {

                    }

                    @Override
                    public void onError(Call call, Exception e) {

                    }

                    @Override
                    public void onFinish() {

                    }
                });
    }

    @OnClick({R.id.user_img, R.id.mf_like, R.id.mf_comment, R.id.edit_mask, R.id.edit_tu_cao, R.id.btn_publish})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.user_img:
                break;
            case R.id.mf_like:
                Utils.toastShow(this, "点赞");
                break;
            case R.id.mf_comment:
                mEditTuCao.setHint("吐槽一下");
                MSG_MODE = MSG_EVALUATE;
                openSofInput(mEditTuCao);
                mEditMask.setVisibility(View.VISIBLE);
                break;
            case R.id.edit_mask:
                mEditMask.setVisibility(View.GONE);
                hideSoftInput(mEditTuCao);
                break;
            case R.id.edit_tu_cao:
                mEditMask.setVisibility(View.VISIBLE);
                break;
            case R.id.btn_publish:
                String msg = mEditTuCao.getText().toString().trim();
                switch (MSG_MODE) {
                    case MSG_EVALUATE:
                        //评论
                        loadingProgress = new LoadingDialog(FeedActivity.this, "评论中...");
                        addEvaluate(mFeedId, saveId, toUid, msg);
                        mEditTuCao.setText(null);
                        hideSoftInput(mEditTuCao);
                        break;
                    case MSG_REPLY:
                        //回复
                        loadingProgress = new LoadingDialog(FeedActivity.this, "回复中...");
                        addReply(mFeedId, mCommentId, saveId, toUid, msg);
                        mEditTuCao.setText(null);
                        hideSoftInput(mEditTuCao);
                        break;
                }
                break;
        }
    }

    /**
     * 添加评论
     */
    public void addEvaluate(String feedId, String uid, String toUid, String comment) {
        Log.d(getClass().getName(), feedId + "," + uid + "," + toUid + "," + comment);
        OkUtil.post()
                .url(Api.saveComment)
                .addParam("feedId", feedId)
                .addParam("userId", uid)
                .addParam("toUserId", toUid)
                .addParam("commentInfo", comment)
                .addParam("type", "0")
                .execute(new ResultCallback<Result>() {
                    @Override
                    public void onSuccess(Result response) {
                        Utils.toastShow(FeedActivity.this, "评论成功");
                        mMfCommentNum.setText(String.valueOf(Integer.valueOf(mMfCommentNum.getText().toString()) + 1));

                        getEvaluateList(mFeedId);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Utils.toastShow(FeedActivity.this, "评论失败");
                    }

                    @Override
                    public void onFinish() {
                        Utils.toastShow(FeedActivity.this, "评论失败");
                    }
                });
    }

    /**
     * 添加回复
     */
    public void addReply(String feedId, String commentId, String uid, String toUid, String reply) {
        OkUtil.post()
                .url(Api.saveComment)
                .addParam("feedId", feedId)
                .addParam("commentId", commentId)
                .addParam("userId", uid)
                .addParam("toUserId", toUid)
                .addParam("commentInfo", reply)
                .addParam("type", "1")
                .execute(new ResultCallback<Result>() {
                    @Override
                    public void onSuccess(Result response) {
                        Utils.toastShow(FeedActivity.this, "回复成功");
                        getEvaluateList(mFeedId);
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Utils.toastShow(FeedActivity.this, "回复失败");
                    }

                    @Override
                    public void onFinish() {
                        Utils.toastShow(FeedActivity.this, "回复失败");
                    }
                });
    }

    /**
     * 获取评论数据
     */
    public void getEvaluateList(String feedId) {
        Integer pageNum = 1;
        Integer pageSize = 20;
        OkUtil.post()
                .url(Api.pageComment)
                .addParam("feedId", feedId)
                .addParam("pageNum", String.valueOf(pageNum))
                .addParam("pageSize", String.valueOf(pageSize))
                .execute(new ResultCallback<Result<PageInfo<Comment>>>() {
                    @Override
                    public void onSuccess(Result<PageInfo<Comment>> response) {
                        String code = response.getCode();
                        if ("00000".equals(code)) {
                            setData(response.getData().getList());
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        Utils.toastShow(FeedActivity.this, R.string.toast_get_feed_error);
                    }

                    @Override
                    public void onFinish() {
                        Utils.toastShow(FeedActivity.this, R.string.toast_get_feed_error);
                    }
                });
    }

    /**
     * 调用输入法
     */
    public void openSofInput(EditText edit) {
        edit.setText(null);
        edit.requestFocus();
//        edit.setFocusable(true);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /**
     * 隐藏输入法
     */
    public void hideSoftInput(EditText edit) {
//        edit.setFocusable(false);
        imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
    }

    public void setData(List<Comment> data) {
        mAdapter.setDate(data);
    }

    /**
     * EditText 监听
     */
    private class EditTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean isEdit = mEditTuCao.getText().length() > 0;
            mBtnPublish.setClickable(isEdit);
            mBtnPublish.setSelected(isEdit);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
