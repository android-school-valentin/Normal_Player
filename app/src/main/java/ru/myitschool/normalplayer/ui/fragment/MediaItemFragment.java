package ru.myitschool.normalplayer.ui.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ru.myitschool.normalplayer.common.model.MusicProviderSource;
import ru.myitschool.normalplayer.common.playback.MusicService;
import ru.myitschool.normalplayer.databinding.FragmentMediaitemBinding;
import ru.myitschool.normalplayer.ui.adapter.MediaItemAdapter;
import ru.myitschool.normalplayer.ui.model.MediaItemData;
import ru.myitschool.normalplayer.ui.viewmodel.MainActivityViewModel;
import ru.myitschool.normalplayer.ui.viewmodel.MediaItemFragmentViewModel;
import ru.myitschool.normalplayer.utils.ProviderUtil;


public class MediaItemFragment extends Fragment implements MediaItemAdapter.OnItemClickListener {

    private static final String TAG = MediaItemFragment.class.getSimpleName();

    private static final String MEDIA_ID_ARG = "media_id_arg";
    private final MediaItemAdapter adapter = new MediaItemAdapter(this);
    private String mediaId;
    private MainActivityViewModel mainActivityViewModel;
    private MediaItemFragmentViewModel mediaItemFragmentViewModel;
    private FragmentMediaitemBinding binding;

    public static MediaItemFragment newInstance(String mediaId) {
        Log.d(TAG, "newInstance: ");
        Bundle args = new Bundle();
        args.putString(MEDIA_ID_ARG, mediaId);
        MediaItemFragment mediaItemFragment = new MediaItemFragment();
        mediaItemFragment.setArguments(args);
        return mediaItemFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMediaitemBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 1);
        gridLayoutManager.setOrientation(RecyclerView.VERTICAL);
        mediaId = getArguments().getString(MEDIA_ID_ARG);
        Log.d(TAG, "onActivityCreated: ");
        if (mediaId == null) {
            return;
        }
        mainActivityViewModel = new ViewModelProvider(getActivity(), ProviderUtil.provideMainActivityViewModel(requireActivity())).get(MainActivityViewModel.class);
        mediaItemFragmentViewModel = new ViewModelProvider(this, ProviderUtil.provideSongFragmentViewModel(requireActivity(), mediaId)).get(MediaItemFragmentViewModel.class);
        mediaItemFragmentViewModel.mediaItems.observe(getViewLifecycleOwner(), mediaItems -> {
            if (mediaItems != null && !mediaItems.isEmpty()) {
                binding.spinner.setVisibility(View.GONE);
                if (mediaItems.get(0).isBrowsable()) {
                    gridLayoutManager.setSpanCount(2);
                }

            } else {
                binding.spinner.setVisibility(View.VISIBLE);
            }
            Log.d(TAG, "onActivityCreated: " + gridLayoutManager.getSpanCount());
            adapter.modifyList(mediaItems);
        });
        mainActivityViewModel.getSearch().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String query) {
                Log.d(TAG, "QUERY" + query);
                adapter.filter(query);
            }
        });
        binding.recycler.setNestedScrollingEnabled(true);
        binding.recycler.setItemAnimator(new DefaultItemAnimator());
        binding.recycler.setLayoutManager(gridLayoutManager);
        binding.recycler.setAdapter(adapter);
    }

    @Override
    public void onItemClick(MediaItemData clickedItem) {
        mainActivityViewModel.mediaItemClicked(clickedItem);
    }

    @Override
    public void onItemMenuClick(String action, MediaItemData clickedItem) {
        switch (action) {
            case MusicService.ACTION_SHARE:
                share(clickedItem.getMediaUri());
                break;
            case MusicService.ACTION_DETAILS:
                showDetails(clickedItem);
                break;
        }
        mainActivityViewModel.mediaItemMenuClicked(action, clickedItem);
    }

    private void share(Uri uri) {
        ShareCompat.IntentBuilder.from(getActivity())
                .setStream(uri)
                .setType("audio/*")
                .startChooser();
    }

    private void showDetails(MediaItemData selectedItem) {
        Log.d(TAG, "showDetails: " + selectedItem.toString());
        CharSequence[] items = new CharSequence[6];
        items[0] = "Title: " + selectedItem.getTitle();
        items[1] = "Subtitle: " + selectedItem.getSubtitle();
        items[2] = "Path to file: " + selectedItem.getMediaUri();
        items[3] = "Path to image: " + selectedItem.getAlbumArtUri();
        items[4] = "Source type: " + MusicProviderSource.SourceType.valueOf(selectedItem.getSourceType());
        items[5] = "Is browsable: " + selectedItem.isBrowsable();
        new MaterialAlertDialogBuilder(getContext())
                .setItems(items, null)
                .setPositiveButton("OK", null)
                .show();
    }
}