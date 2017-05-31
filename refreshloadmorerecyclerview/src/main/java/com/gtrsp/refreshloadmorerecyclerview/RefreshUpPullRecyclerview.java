package com.gtrsp.refreshloadmorerecyclerview;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wang.avi.AVLoadingIndicatorView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 自定义下拉刷新上拉加载更多的recyclerview
 * Created by raoxuting on 2017/5/24.
 */

public class RefreshUpPullRecyclerview extends RecyclerView {

    private final int DOWNPULL_REFRESH = 1;//下拉刷新状态
    private final int RELEASE_REFRESH = 2;//松开刷新状态
    private final int REFRESHING = 3;//正在刷新状态
    private final int ISLOADING_MORE = 4;//上拉加载更多状态
    private final int DEFAULT = 5;//默认状态
    private int currentStatus = DEFAULT;
    private int refreshHeaderHeight;
    private int footerHeight;
    private HeaderRefreshHolder headerHolder;
    private FooterLoadHolder footerLoadHolder;
    private RotateAnimation upArrowAnim;
    private RotateAnimation downArrowAnim;
    private float downY;
    private OnRefreshListener onRefreshListener;
    private boolean loadMoreEnabled = true;
    private boolean IsLoadMoreHide = false;
    private RefreshUpPullWrapper wrapper;

    public void setLoadMoreEnabled(boolean loadMoreEnabled) {
        this.loadMoreEnabled = loadMoreEnabled;
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    public interface OnRefreshListener {
        void onPullDownRefresh();

        void onLoadMore();
    }

    public RefreshUpPullRecyclerview(Context context) {
        this(context, null);
    }

    public RefreshUpPullRecyclerview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshUpPullRecyclerview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        wrapper = new RefreshUpPullWrapper(adapter);
        super.setAdapter(wrapper);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (downY == -1) {
            downY = e.getY();
        }
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = e.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                if (downY == 0)
                    downY = e.getY();

                if (currentStatus == REFRESHING || currentStatus == ISLOADING_MORE) break;

                int deltaY = (int) (e.getY() - downY);

                int firstVisibleItem;
                LayoutManager layoutManager = getLayoutManager();
                if (layoutManager instanceof GridLayoutManager) {
                    firstVisibleItem = ((GridLayoutManager) layoutManager)
                            .findFirstVisibleItemPosition();
                } else if (layoutManager instanceof LinearLayoutManager) {
                    firstVisibleItem = ((LinearLayoutManager) layoutManager)
                            .findFirstVisibleItemPosition();
                } else {
                    int[] firstPostition = new
                            int[((StaggeredGridLayoutManager) layoutManager).getSpanCount()];
                    ((StaggeredGridLayoutManager) layoutManager)
                            .findFirstVisibleItemPositions(firstPostition);
                    firstVisibleItem = findMin(firstPostition);
                }

                //头布局处理
                if (deltaY > 0 && firstVisibleItem == 0) {
                    if (deltaY > refreshHeaderHeight && currentStatus != RELEASE_REFRESH) {
                        //释放刷新
                        currentStatus = RELEASE_REFRESH;
                        refreshHeader();
                    } else if (deltaY < refreshHeaderHeight && currentStatus == RELEASE_REFRESH) {
                        //下拉刷新
                        currentStatus = DOWNPULL_REFRESH;
                        refreshHeader();
                    }
                    setViewHeight(headerHolder.itemView, deltaY);
                    return false;
                }
                break;

            case MotionEvent.ACTION_UP:
                downY = -1;
                if (currentStatus == DOWNPULL_REFRESH) {
                    //"下拉刷新"状态下松手,隐藏头布局
                    setViewHeight(headerHolder.itemView, 0);
                } else if (currentStatus == RELEASE_REFRESH) {
                    //"松开刷新"状态下松手,显示刷新view
                    currentStatus = REFRESHING;
                    setViewHeight(headerHolder.itemView, refreshHeaderHeight);
                    refreshHeader();
                    if (onRefreshListener != null)
                        onRefreshListener.onPullDownRefresh();
                    return false;
                }
                break;
        }
        return super.onTouchEvent(e);
    }

    private int findMin(int[] firstPositions) {
        int min = firstPositions[0];
        for (int value : firstPositions) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }

    private void setViewHeight(View view, int height) {
        if (height < 0) height = 0;
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        lp.height = height;
        view.setLayoutParams(lp);
    }

    /**
     * 刷新或加载完成调用此方法进行相应的ui变化
     */
    public void onRefreshOrLoadMoreCompleted() {
        if (currentStatus == REFRESHING) {
            currentStatus = DEFAULT;
            setViewHeight(headerHolder.itemView, 0);
            headerHolder.headerloadingView.setVisibility(INVISIBLE);
            headerHolder.llRefreshStatus.setVisibility(VISIBLE);
            headerHolder.tvRefreshstatus.setText("下拉刷新");
        } else currentStatus = DEFAULT;
        wrapper.notifyDataSetChanged();
    }

    /**
     * 隐藏加载更多条目
     */
    public void hideLoadmore() {
        IsLoadMoreHide = true;
        setViewHeight(footerLoadHolder.itemView, 0);
    }

    /**
     * 更新头布局ui
     */
    private void refreshHeader() {
        switch (currentStatus) {
            case DOWNPULL_REFRESH:
                //转变为下拉刷新状态
                headerHolder.tvRefreshstatus.setText("下拉刷新");
                headerHolder.ivArrow.startAnimation(downArrowAnim);
                break;
            case RELEASE_REFRESH:
                //转变为松开刷新状态
                headerHolder.tvRefreshstatus.setText("松开刷新");
                headerHolder.ivArrow.startAnimation(upArrowAnim);
                break;
            case REFRESHING:
                //正在刷新
                setViewHeight(headerHolder.itemView, refreshHeaderHeight);
                headerHolder.llRefreshStatus.setVisibility(View.INVISIBLE);
                headerHolder.headerloadingView.setVisibility(View.VISIBLE);
                break;
        }
    }

    public class RefreshUpPullWrapper extends Adapter {

        private Adapter mInnerAdapter;
        private static final int TYPE_REFRESH_HEADER = 10000;
        private static final int TYPE_LOADMORE_FOOTER = 20000;
        private Context context;

        public RefreshUpPullWrapper(Adapter adapter) {
            mInnerAdapter = adapter;
        }

        @Override
        public int getItemCount() {
            return 1 + mInnerAdapter.getItemCount() + (loadMoreEnabled ? 1 : 0);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0)
                return TYPE_REFRESH_HEADER;
            else if (position == 1 + mInnerAdapter.getItemCount()) {
                return TYPE_LOADMORE_FOOTER;
            } else return mInnerAdapter.getItemViewType(position - 1);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (context == null)
                context = parent.getContext();
            View view;
            if (viewType == TYPE_REFRESH_HEADER) {
                view = LayoutInflater.from(context).inflate(R.layout.headview, parent, false);
                headerHolder = new HeaderRefreshHolder(view);
                return headerHolder;
            } else if (viewType == TYPE_LOADMORE_FOOTER) {
                view = LayoutInflater.from(context).inflate(R.layout.footerview, parent, false);
                footerLoadHolder = new FooterLoadHolder(view);
                return footerLoadHolder;
            } else return mInnerAdapter.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
//            LogUtils.e("绑定数据position", String.valueOf(position));
            if (holder instanceof FooterLoadHolder) {
                //上拉加载更多
                //使加载的条目完全展示出来
                RefreshUpPullRecyclerview.this.scrollToPosition(getItemCount() - 1);
                currentStatus = ISLOADING_MORE;
                if (IsLoadMoreHide)
                    setViewHeight(holder.itemView, footerHeight);
                if (onRefreshListener != null)
                    onRefreshListener.onLoadMore();
            } else if (!(holder instanceof HeaderRefreshHolder))
                mInnerAdapter.onBindViewHolder(holder, position - 1);
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            mInnerAdapter.onAttachedToRecyclerView(recyclerView);

            LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager) {
                final GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
                final GridLayoutManager.SpanSizeLookup spanSizeLookup = gridLayoutManager.getSpanSizeLookup();

                gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        int viewType = getItemViewType(position);
                        if (viewType == TYPE_REFRESH_HEADER) {
                            return gridLayoutManager.getSpanCount();
                        } else if (viewType == TYPE_LOADMORE_FOOTER) {
                            return gridLayoutManager.getSpanCount();
                        }
                        if (spanSizeLookup != null)
                            return spanSizeLookup.getSpanSize(position);
                        return 1;
                    }
                });
                gridLayoutManager.setSpanCount(gridLayoutManager.getSpanCount());
            }
        }

        @Override
        public void onViewAttachedToWindow(ViewHolder holder) {
            mInnerAdapter.onViewAttachedToWindow(holder);
            int position = holder.getLayoutPosition();
            if (position == 0 || position == 1 + mInnerAdapter.getItemCount()) {
                ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();

                if (lp != null
                        && lp instanceof StaggeredGridLayoutManager.LayoutParams) {

                    StaggeredGridLayoutManager.LayoutParams p =
                            (StaggeredGridLayoutManager.LayoutParams) lp;

                    p.setFullSpan(true);
                }
            }
        }
    }

    public class HeaderRefreshHolder extends ViewHolder {

        @BindView(R2.id.headerloadingView)
        AVLoadingIndicatorView headerloadingView;
        @BindView(R2.id.iv_Arrow)
        ImageView ivArrow;
        @BindView(R2.id.tv_refreshstatus)
        TextView tvRefreshstatus;
        @BindView(R2.id.ll_refresh_status)
        LinearLayout llRefreshStatus;

        public HeaderRefreshHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.measure(0, 0);
            refreshHeaderHeight = itemView.getMeasuredHeight();
//            LogUtils.e("头布局高度", String.valueOf(refreshHeaderHeight));
            //隐藏下拉刷新布局
            setViewHeight(itemView, 0);
            //初始化箭头的旋转动画
            initAnimation();
        }
    }

    /**
     * 初始化箭头的动画
     */
    public void initAnimation() {
        upArrowAnim = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        upArrowAnim.setDuration(300);
        upArrowAnim.setFillAfter(true);//设置停留在动画结束时的状态

        downArrowAnim = new RotateAnimation(-180, 0, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        downArrowAnim.setDuration(300);
        downArrowAnim.setFillAfter(true);
    }

    public class FooterLoadHolder extends ViewHolder {

        public FooterLoadHolder(View itemView) {
            super(itemView);
            itemView.measure(0, 0);
            footerHeight = itemView.getMeasuredHeight();
//            LogUtils.e("脚布局高度", String.valueOf(footerHeight));
        }
    }
}
