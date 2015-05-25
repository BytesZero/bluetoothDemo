package com.zsl.bluetoothdemo.utils.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * ViewHolder
 * Created by zsl on 15/5/19.
 */
public class ViewHolder {
    private SparseArray<View> mViews;
    private View mConvertView;
    private Context mContext;

    /**
     * 初始化ViewHolder
     * @param context  上下文
     * @param parent 父视图
     * @param layoutId Item layout
     *
     */
    public ViewHolder(Context context, ViewGroup parent, int layoutId) {
        this.mContext=context;
        this.mViews=new SparseArray<View>();
        mConvertView= LayoutInflater.from(context).inflate(layoutId,parent,false);
        //设置Tag
        mConvertView.setTag(this);
    }

    /**
     * 获得到ViewHolder
     * @param context 上下文
     * @param convertView convertView
     * @param parent 父视图
     * @param layoutId Item layout
     * @return 返回ViewHolder对象
     */
    public static ViewHolder get(Context context, View convertView, ViewGroup parent, int layoutId) {
        if (convertView == null) {
            return new ViewHolder(context, parent, layoutId);
        }else{
            return (ViewHolder) convertView.getTag();
        }
    }

    /**
     * 获得到控件
     * @param viewId item layout 中控件的id
     * @param <T>  范型
     * @return 范型View
     */

    public <T extends View> T getView(int viewId){
        View view=mViews.get(viewId);
        if (view==null){
            view=mConvertView.findViewById(viewId);
            mViews.put(viewId,view);
        }
        return (T) view;
    }

    /**
     * 获得到convertView
     * @return convertView
     */
    public View getmConvertView() {
        return mConvertView;
    }

    /**
     * 设置TextView的文本
     * @param viewId item layout 中TextView的id
     * @param text 文本内容
     * @return ViewHolder
     */
    public ViewHolder setText(int viewId,String text){
        TextView textView=getView(viewId);
        textView.setText(text);
        return this;
    }

    /**
     * 通过url设置ImageView 的图片
     * 这里可以修改为自己的图片加载库
     * @param viewId item layout 中ImageView的id
     * @param url 图片的url
     * @return ViewHolder
     */
    public ViewHolder setImage(int viewId,String url){
        ImageView imageView=getView(viewId);
        //这里可以修改为自己的图片加载库
//        Ion.with(mContext).load(url).intoImageView(imageView);
        return this;
    }

    /**
     * 通过ResourceId设置ImageView 的图片
     * @param viewId item layout 中ImageView的id
     * @param resourceId 图片资源文件的id
     * @return ViewHolder
     */
    public ViewHolder setImageResource(int viewId,int resourceId){
        ImageView imageView=getView(viewId);
        imageView.setImageResource(resourceId);
        return this;
    }

    /**
     * 通过bitmap 设置ImageView 的图片
     * @param viewId item layout 中ImageView的id
     * @param bitmap bitmap
     * @return ViewHolder
     */
    public ViewHolder setImageBitmap(int viewId,Bitmap bitmap){
        ImageView imageView=getView(viewId);
        imageView.setImageBitmap(bitmap);
        return this;
    }
    
    /**
     * 设置View隐藏Gone
     * @param viewId
     * @return
     */
    public ViewHolder setViewGone(int viewId){
        getView(viewId).setVisibility(View.GONE);
        return this;
    }
    
    /**
     * 设置View隐藏Invisible
     * @param viewId
     * @return
     */
    public ViewHolder setViewInvisible(int viewId){
        getView(viewId).setVisibility(View.INVISIBLE);
        return this;
    }
    /**
     * 设置View显示Visible
     * @param viewId
     * @return
     */
    public ViewHolder setViewVisible(int viewId){
        getView(viewId).setVisibility(View.VISIBLE);
        return this;
    }


    /**
     * ==============下边可以写自己的控件的实现，参考上边的ImageView================
     */



}
