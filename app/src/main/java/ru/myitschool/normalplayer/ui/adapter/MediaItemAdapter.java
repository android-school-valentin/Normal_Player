package ru.myitschool.normalplayer.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.myitschool.normalplayer.R;
import ru.myitschool.normalplayer.common.model.MusicProviderSource;
import ru.myitschool.normalplayer.databinding.ItemGridBinding;
import ru.myitschool.normalplayer.databinding.ItemLineBinding;
import ru.myitschool.normalplayer.ui.model.MediaItemData;
import ru.myitschool.normalplayer.utils.PlayerUtil;

public class MediaItemAdapter extends ListAdapter<MediaItemData, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_LINE = 1;
    private static final int VIEW_TYPE_GRID = 2;
    private final OnItemClickListener itemClickListener;
    private List<MediaItemData> unfilteredList = new ArrayList<>();

    public MediaItemAdapter(OnItemClickListener itemClickListener) {
        super(MediaItemData.DIFF_CALLBACK);
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        switch (viewType) {
            case VIEW_TYPE_LINE:
                return new LineViewHolder(ItemLineBinding.inflate(inflater, parent, false), itemClickListener);
            case VIEW_TYPE_GRID:
                return new GridViewHolder(ItemGridBinding.inflate(inflater, parent, false), itemClickListener);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        onBindViewHolder(holder, position, Collections.emptyList());
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isBrowsable() ? VIEW_TYPE_GRID : VIEW_TYPE_LINE;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position, @NonNull List<Object> payloads) {
        MediaItemData item = getItem(position);
        boolean fullRefresh = payloads.isEmpty();

        switch (viewHolder.getItemViewType()) {
            case VIEW_TYPE_LINE:
                LineViewHolder lineViewHolder = (LineViewHolder) viewHolder;
                if (!payloads.isEmpty()) {
                    for (int i = 0; i < payloads.size(); i++) {
                        if (payloads.get(i) instanceof Integer) {
                            if (item.getPlaybackRes() == R.drawable.ic_pause_24 || item.getPlaybackRes() == R.drawable.ic_play_24) {
                                lineViewHolder.binding.itemLineRoot.setBackgroundResource(R.color.colorBackgroundSelected);
                            } else {
                                lineViewHolder.binding.itemLineRoot.setBackgroundResource(R.color.colorBackground);
                            }
                        } else {
                            fullRefresh = true;
                        }
                    }
                }

                if (fullRefresh) {
                    lineViewHolder.item = item;
                    lineViewHolder.binding.itemLineTitle.setText(item.getTitle());
                    lineViewHolder.binding.itemLineSubtitle.setText(item.getSubtitle());
                    lineViewHolder.binding.itemLineDuration.setText(PlayerUtil.convertMs(item.getDuration()));
                    if (item.getPlaybackRes() == R.drawable.ic_pause_24 || item.getPlaybackRes() == R.drawable.ic_play_24) {
                        lineViewHolder.binding.itemLineRoot.setBackgroundResource(R.color.colorBackgroundSelected);
                    } else {
                        lineViewHolder.binding.itemLineRoot.setBackgroundResource(R.color.colorBackground);
                    }
                    Picasso.get().load(item.getAlbumArtUri()).placeholder(R.drawable.ic_default_art).into(lineViewHolder.binding.itemLineArt);
                }
                break;
            case VIEW_TYPE_GRID:
                GridViewHolder gridViewHolder = (GridViewHolder) viewHolder;
                gridViewHolder.item = item;
                gridViewHolder.binding.itemGridTitle.setText(item.getTitle());
                Picasso.get().load(item.getAlbumArtUri()).placeholder(R.drawable.ic_default_art).into(gridViewHolder.binding.itemGridArt);
                break;
            default:
                throw new IllegalStateException("No view type");
        }

    }

    public void filter(String query) {
        ArrayList<MediaItemData> filteredList = new ArrayList<>();
        if (query == null || query.length() == 0) {
            filteredList.addAll(unfilteredList);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (MediaItemData mediaItemData : getCurrentList()) {
                if (mediaItemData.getTitle().toLowerCase().trim().contains(filterPattern)) {
                    filteredList.add(mediaItemData);
                }
            }
        }
        submitList(filteredList);
    }

    public void modifyList(List<MediaItemData> mediaItemDataList) {
        unfilteredList = mediaItemDataList;
        submitList(mediaItemDataList);
    }

    public interface OnItemClickListener {
        void onItemClick(MediaItemData clickedItem);
    }

    public static class LineViewHolder extends RecyclerView.ViewHolder {
        private MediaItemData item = null;
        private ItemLineBinding binding;

        public LineViewHolder(ItemLineBinding binding, OnItemClickListener itemClickListener) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (item != null) {
                        Log.d("Jopa", "onClick: " + MusicProviderSource.SOURCE_TYPE.valueOf(item.getSourceType()));
                        itemClickListener.onItemClick(item);
                    }
                }
            });
        }
    }

    public static class GridViewHolder extends RecyclerView.ViewHolder {
        private MediaItemData item = null;
        private ItemGridBinding binding;

        public GridViewHolder(ItemGridBinding binding, OnItemClickListener itemClickListener) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (item != null) {
                        itemClickListener.onItemClick(item);
                    }
                }
            });
        }
    }
}
