package com.hardcodecoder.pulsemusic.fragments.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.FileObserver;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hardcodecoder.pulsemusic.R;
import com.hardcodecoder.pulsemusic.activities.CurrentQueuePlaylist;
import com.hardcodecoder.pulsemusic.activities.CustomizablePlaylist;
import com.hardcodecoder.pulsemusic.adapters.PlaylistAdapter;
import com.hardcodecoder.pulsemusic.dialog.RoundedBottomSheetDialog;
import com.hardcodecoder.pulsemusic.helper.RecyclerViewGestureHelper;
import com.hardcodecoder.pulsemusic.helper.UIHelper;
import com.hardcodecoder.pulsemusic.interfaces.ItemGestureCallback;
import com.hardcodecoder.pulsemusic.interfaces.PlaylistCardListener;
import com.hardcodecoder.pulsemusic.providers.ProviderManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlaylistFragment extends Fragment implements PlaylistCardListener, ItemGestureCallback<String> {

    private FileObserver mObserver;
    private PlaylistAdapter mAdapter;
    private Context mContext;
    private List<String> mPlaylistNames;

    @NonNull
    public static PlaylistFragment getInstance() {
        return new PlaylistFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        if (getContext() != null)
            mContext = getContext();
        return inflater.inflate(R.layout.fragment_playlist_cards, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mPlaylistNames = new ArrayList<>();
        mPlaylistNames.add(getString(R.string.playlist_current_queue));
        ProviderManager.getPlaylistProvider().getAllPlaylistItem(result -> {
            if (null != result) mPlaylistNames.addAll(result);
            loadPlaylistCards(view);
        });
        mObserver = new FileObserver(ProviderManager.getPlaylistProvider().getPlaylistParentFolder().getAbsolutePath(),
                FileObserver.CREATE) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                if (null != path) view.post(() -> mAdapter.addPlaylist(path));
            }
        };
        mObserver.startWatching();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_playlist, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_action_add_playlist) {
            if (null != getContext())
                UIHelper.buildCreatePlaylistDialog(getContext(), playlistName -> {
                    // File observer monitors creation of new playlists
                    // No need to call PlaylistAdapter#addPlaylist();
                });
        }
        return true;
    }

    private void loadPlaylistCards(@NonNull View view) {
        view.post(() -> {
            RecyclerView recyclerView = (RecyclerView) ((ViewStub) view.findViewById(R.id.stub_playlist_cards_rv)).inflate();
            recyclerView.setLayoutManager(new LinearLayoutManager(mContext, RecyclerView.VERTICAL, false));
            mAdapter = new PlaylistAdapter(mPlaylistNames, getLayoutInflater(), this, this);
            recyclerView.setAdapter(mAdapter);
            ItemTouchHelper.Callback itemTouchHelperCallback = new RecyclerViewGestureHelper(mAdapter);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
            itemTouchHelper.attachToRecyclerView(recyclerView);
        });
    }

    private void showEditPlaylistDialog(int pos) {
        if (getContext() != null) {
            BottomSheetDialog sheetDialog = new RoundedBottomSheetDialog(mContext);
            View layout = View.inflate(mContext, R.layout.bottom_dialog_edit_text, null);
            sheetDialog.setContentView(layout);
            sheetDialog.show();

            TextView header = layout.findViewById(R.id.header);
            header.setText(getResources().getString(R.string.edit_playlist));

            TextInputLayout til = layout.findViewById(R.id.edit_text_container);
            til.setHint(getResources().getString(R.string.create_playlist_hint));

            TextInputEditText et = layout.findViewById(R.id.text_input_field);

            Button create = layout.findViewById(R.id.confirm_btn);
            create.setOnClickListener(v -> {
                if (et.getText() != null) {
                    String text = et.getText().toString().trim();
                    if (text.length() == 0 || text.charAt(0) == ' ') {
                        Toast.makeText(mContext, mContext.getString(R.string.create_playlist_hint), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String oldName = mPlaylistNames.remove(pos);
                    String newName = et.getText().toString();
                    if (ProviderManager.getPlaylistProvider().renamePlaylistItem(oldName, newName)) {
                        mPlaylistNames.add(pos, newName);
                        mAdapter.notifyItemChanged(pos);
                    }
                } else {
                    Toast.makeText(mContext, getString(R.string.create_playlist_hint), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (sheetDialog.isShowing())
                    sheetDialog.dismiss();
            });

            Button cancel = layout.findViewById(R.id.cancel_btn);
            cancel.setOnClickListener(v -> {
                if (sheetDialog.isShowing())
                    sheetDialog.dismiss();
            });
        }
    }

    @Override
    public void onItemClick(int pos) {
        if (pos == 0)
            startActivity(new Intent(mContext, CurrentQueuePlaylist.class));
        else {
            Intent i = new Intent(mContext, CustomizablePlaylist.class);
            i.putExtra(CustomizablePlaylist.PLAYLIST_TITLE_KEY, mPlaylistNames.get(pos));
            Objects.requireNonNull(getActivity()).startActivityFromFragment(this, i, 100);
        }
    }

    @Override
    public void onItemEdit(int pos) {
        showEditPlaylistDialog(pos);
    }

    @Override
    public void onItemDismissed(@NonNull String dismissedItem, int itemPosition) {
        if (itemPosition == 0) {
            Toast.makeText(mContext, getString(R.string.cannot_delete_default_playlist), Toast.LENGTH_SHORT).show();
            mAdapter.notifyItemChanged(itemPosition);
        } else {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext)
                    .setMessage(getString(R.string.playlist_delete_dialog_title))
                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                        ProviderManager.getPlaylistProvider().deletePlaylistItem(mPlaylistNames.get(itemPosition));
                        Toast.makeText(getContext(), getString(R.string.playlist_deleted_toast), Toast.LENGTH_SHORT).show();
                        mAdapter.removePlaylist(itemPosition);
                    }).setNegativeButton(getString(R.string.no), (dialog, which) -> mAdapter.notifyItemChanged(itemPosition));
            alertDialog.create().show();
        }
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
    }

    @Override
    public void onDestroyView() {
        mObserver.stopWatching();
        super.onDestroyView();
    }
}