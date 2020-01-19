package me.yuqirong.cardswipelayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yuqirong
 */

public class CardItemTouchHelperCallback<T> extends ItemTouchHelper.Callback {

    private final RecyclerView.Adapter adapter;
    private List<T> dataList;
    private OnSwipeListener<T> mListener;
    private CardRecyclerView recyclerView;
    ValueAnimator swipeAnimator;
    //需要撤回的View列表
    List<ReturnView> mLastReturnList = new ArrayList<>();
    //需要添加道卡片的列表
    List<T> needAddLastList = new ArrayList<>();

    public int getReturnSize(){
        return mLastReturnList.size();
    }

    public void addData(T data) {
        needAddLastList.add(data);
    }

    public static class ReturnView<T> {

        RecyclerView.ViewHolder viewHolder;
        Integer direction;
        T curData;

        public ReturnView(RecyclerView.ViewHolder viewHolder, Integer direction, T data) {
            this.viewHolder = viewHolder;
            this.direction = direction;
            this.curData = data;
        }

    }

    public CardItemTouchHelperCallback(@NonNull CardRecyclerView recyclerView, @NonNull RecyclerView.Adapter adapter, @NonNull List<T> dataList) {
        this(recyclerView, adapter, dataList, null);
    }

    public CardItemTouchHelperCallback(@NonNull CardRecyclerView recyclerView, @NonNull RecyclerView.Adapter adapter,
                                       @NonNull List<T> dataList, OnSwipeListener<T> listener) {
        this.recyclerView = checkIsNull(recyclerView);
        this.adapter = checkIsNull(adapter);
        this.dataList = checkIsNull(dataList);
        this.mListener = listener;
    }

    private <T> T checkIsNull(T t) {
        if (t == null) {
            throw new NullPointerException();
        }
        return t;
    }

    public void setOnSwipedListener(OnSwipeListener<T> mListener) {
        this.mListener = mListener;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = 0;
        int swipeFlags = 0;
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof CardLayoutManager) {
            swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        }
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        // 移除 onTouchListener,否则触摸滑动会乱了
        viewHolder.itemView.setOnTouchListener(null);
        int layoutPosition = viewHolder.getLayoutPosition();
        T remove = dataList.remove(layoutPosition);
        adapter.notifyDataSetChanged();

        mLastReturnList.add(new ReturnView(viewHolder,direction,remove));
        if (mListener != null) {
            mListener.onSwiped(viewHolder, remove, direction == ItemTouchHelper.LEFT ? CardConfig.SWIPED_LEFT : CardConfig.SWIPED_RIGHT);
        }

        // 当没有数据时回调 mListener
        if (adapter.getItemCount() == 0) {
            if(needAddLastList.size() > 0) {
                dataList.addAll(needAddLastList);
                needAddLastList.clear();
                mLastReturnList.clear();
                adapter.notifyDataSetChanged();
            }else if (mListener != null) {
                mListener.onSwipedClear();
            }
        }
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        View itemView = viewHolder.itemView;
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            float ratio = dX / getThreshold(recyclerView, viewHolder);
            // ratio 最大为 1 或 -1
            if (ratio > 1) {
                ratio = 1;
            } else if (ratio < -1) {
                ratio = -1;
            }
            itemView.setRotation(ratio * CardConfig.DEFAULT_ROTATE_DEGREE);
            int childCount = recyclerView.getChildCount();
            // 当数据源个数大于最大显示数时
            if (childCount > CardConfig.DEFAULT_SHOW_ITEM) {
                for (int position = 1; position < childCount - 1; position++) {
                    int index = childCount - position - 1;
                    View view = recyclerView.getChildAt(position);
                    view.setScaleX(1 - index * CardConfig.DEFAULT_SCALE + Math.abs(ratio) * CardConfig.DEFAULT_SCALE);
                    view.setScaleY(1 - index * CardConfig.DEFAULT_SCALE + Math.abs(ratio) * CardConfig.DEFAULT_SCALE);
                    view.setTranslationY((index - Math.abs(ratio)) * itemView.getMeasuredHeight() / CardConfig.DEFAULT_TRANSLATE_Y);
                }
            } else {
                // 当数据源个数小于或等于最大显示数时
                for (int position = 0; position < childCount - 1; position++) {
                    int index = childCount - position - 1;
                    View view = recyclerView.getChildAt(position);
                    view.setScaleX(1 - index * CardConfig.DEFAULT_SCALE + Math.abs(ratio) * CardConfig.DEFAULT_SCALE);
                    view.setScaleY(1 - index * CardConfig.DEFAULT_SCALE + Math.abs(ratio) * CardConfig.DEFAULT_SCALE);
                    view.setTranslationY((index - Math.abs(ratio)) * itemView.getMeasuredHeight() / CardConfig.DEFAULT_TRANSLATE_Y);
                }
            }
            if (mListener != null) {
                if (ratio != 0) {
                    mListener.onSwiping(viewHolder, ratio, ratio < 0 ? CardConfig.SWIPING_LEFT : CardConfig.SWIPING_RIGHT);
                } else {
                    mListener.onSwiping(viewHolder, ratio, CardConfig.SWIPING_NONE);
                }
            }
        }
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setRotation(0f);
    }

    private float getThreshold(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return recyclerView.getWidth() * getSwipeThreshold(viewHolder);
    }

    public T handlerCardBack(long duration) {
        if(mLastReturnList.isEmpty()) {
            Toast.makeText(recyclerView.getContext(),"当前不可返回",Toast.LENGTH_SHORT).show();
            return null;
        }

        final ReturnView returnView = mLastReturnList.remove(mLastReturnList.size()-1);

        // 移除 onTouchListener,否则触摸滑动会乱了
        RecyclerView.ViewHolder topHolder = recyclerView.findViewHolderForAdapterPosition(0);
        if(topHolder != null&&topHolder.itemView != null) {
            topHolder.itemView.setOnTouchListener(null);
        }

        dataList.add(0, (T) returnView.curData);
        adapter.notifyDataSetChanged();

        if (swipeAnimator != null && swipeAnimator.isStarted()) {
            return null;
        }
        final CardRecyclerView recyclerView = checkIsNull(this.recyclerView);
        final Canvas canvas = checkIsNull(this.recyclerView.getCanvas());
        if (returnView.viewHolder == null) {
            return null;
        }
        if (returnView.direction == CardConfig.SWIPING_LEFT) {
            swipeAnimator = ValueAnimator.ofFloat(-recyclerView.getWidth() / 2, 0);
        } else if (returnView.direction == CardConfig.SWIPING_RIGHT) {
            swipeAnimator = ValueAnimator.ofFloat(recyclerView.getWidth() / 2, 0);
        } else {
            throw new IllegalStateException("flag must be one of SWIPING_LEFT or SWIPING_RIGHT");
        }
        swipeAnimator.setDuration(duration);
        swipeAnimator.setInterpolator(null);
        swipeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                float value = (float) animation.getAnimatedValue();
                onChildDraw(canvas, recyclerView, returnView.viewHolder, value, 0, ItemTouchHelper.ACTION_STATE_SWIPE, false);
            }
        });
        swipeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
//                onSwiped(returnView.viewHolder, returnView.direction == CardConfig.SWIPING_LEFT ? ItemTouchHelper.LEFT : ItemTouchHelper.RIGHT);
            }
        });
        swipeAnimator.start();

        return (T) returnView.curData;

    }

    public void handleCardSwipe(int flag, long duration) {
        handleCardSwipe(flag, duration, null);
    }

    public void handleCardSwipe(final int flag, long duration, Interpolator interpolator) {
        if (swipeAnimator != null && swipeAnimator.isStarted()) {
            return;
        }
        final CardRecyclerView recyclerView = checkIsNull(this.recyclerView);
        final Canvas canvas = checkIsNull(this.recyclerView.getCanvas());
        final RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(0);
        if (viewHolder == null) {
            return;
        }
        if (flag == CardConfig.SWIPING_LEFT) {
            swipeAnimator = ValueAnimator.ofFloat(0, -recyclerView.getWidth() / 2);
        } else if (flag == CardConfig.SWIPING_RIGHT) {
            swipeAnimator = ValueAnimator.ofFloat(0, recyclerView.getWidth() / 2);
        } else {
            throw new IllegalStateException("flag must be one of SWIPING_LEFT or SWIPING_RIGHT");
        }
        swipeAnimator.setDuration(duration);
        swipeAnimator.setInterpolator(interpolator);
        swipeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                float value = (float) animation.getAnimatedValue();
                onChildDraw(canvas, recyclerView, viewHolder, value, 0, ItemTouchHelper.ACTION_STATE_SWIPE, true);
            }
        });
        swipeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                onSwiped(viewHolder, flag == CardConfig.SWIPING_LEFT ? ItemTouchHelper.LEFT : ItemTouchHelper.RIGHT);
                clearView(recyclerView, viewHolder);
            }
        });
        swipeAnimator.start();
    }

}
