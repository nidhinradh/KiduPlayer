package me.nidhinradh.kidu_player.viewholders;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import me.nidhinradh.kidu_player.R;

public class SectionHeaderViewHolder extends RecyclerView.ViewHolder {
    public final View rlSectionHeader;
    public final TextView tvSectionHeader;
    public final TextView tvSectionCount;
    public final ImageView ivArrow;

    public SectionHeaderViewHolder(View view) {
        super(view);
        rlSectionHeader = view;
        tvSectionHeader = view.findViewById(R.id.tv_header);
        tvSectionCount = view.findViewById(R.id.tv_count);
        ivArrow = view.findViewById(R.id.iv_arrow);
    }

}
