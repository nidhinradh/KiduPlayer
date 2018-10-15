package me.nidhinradh.kidu_player.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import me.nidhinradh.kidu_player.R;

public class SectionItemViewHolder extends RecyclerView.ViewHolder {
    public final View clItemView;
    public final ImageView ivVideoThumbnail;
    public final TextView tvVideoDuration;

    public SectionItemViewHolder(View view) {
        super(view);
        clItemView = view;
        ivVideoThumbnail = view.findViewById(R.id.iv_thumbnail);
        tvVideoDuration = view.findViewById(R.id.tv_duration);
    }
}
