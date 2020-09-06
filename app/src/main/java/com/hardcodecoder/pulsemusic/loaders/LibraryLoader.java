package com.hardcodecoder.pulsemusic.loaders;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import com.hardcodecoder.pulsemusic.model.MusicModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class LibraryLoader implements Callable<List<MusicModel>> {

    private ContentResolver contentResolver;
    private String mSortOrder;
    private String mSelectionString;

    // MediaStore.Audio.Media.DURATION existed well before APi 29
    // Suppress lint
    @SuppressLint("InlinedApi")
    LibraryLoader(ContentResolver contentResolver, SortOrder sortOrder) {
        this(contentResolver, sortOrder, null);
    }

    @SuppressLint("InlinedApi")
    LibraryLoader(ContentResolver contentResolver, SortOrder sortOrder, @Nullable String selectionString) {
        this.contentResolver = contentResolver;
        switch (sortOrder) {
            case TITLE_ASC:
                mSortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";
                break;
            case TITLE_DESC:
                mSortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE DESC";
                break;
            case DURATION_ASC:
                mSortOrder = MediaStore.Audio.Media.DURATION + " ASC";
                break;
            case DURATION_DESC:
                mSortOrder = MediaStore.Audio.Media.DURATION + " DESC";
                break;
            case DATE_MODIFIED_ASC:
                mSortOrder = MediaStore.Audio.Media.DATE_MODIFIED + " ASC";
                break;
            case DATE_MODIFIED_DESC:
                mSortOrder = MediaStore.Audio.Media.DATE_MODIFIED + " DESC";
                break;
            case TRACK_NUMBER_ASC:
                mSortOrder = MediaStore.Audio.Media.TRACK + " ASC";
                break;
            case TRACK_NUMBER_DESC:
                mSortOrder = MediaStore.Audio.Media.TRACK + " DESC";
                break;
            default:
                mSortOrder = null;
        }
        this.mSelectionString = selectionString;
    }

    @SuppressLint("InlinedApi")
    @Override
    public List<MusicModel> call() {
        List<MusicModel> libraryList = new ArrayList<>();
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String[] cursor_cols = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.AudioColumns.DURATION
        };

        final Cursor cursor = contentResolver.query(
                uri,
                cursor_cols,
                mSelectionString,
                null,
                mSortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            int idColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int albumColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int albumIdColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            int dateAddedColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
            int dateModifiedColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED);
            int trackNumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK);
            int durationColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

            final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");

            do {
                int _id = cursor.getInt(idColumnIndex);
                String songName = cursor.getString(titleColumnIndex);
                String artist = cursor.getString(artistColumnIndex);
                String album = cursor.getString(albumColumnIndex);
                String songPath = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, _id).toString();
                long albumId = cursor.getLong(albumIdColumnIndex);
                long dateAdded = cursor.getLong(dateAddedColumnIndex);
                long dateModified = cursor.getLong(dateModifiedColumnIndex);
                int trackNum = cursor.getInt(trackNumIndex);
                int duration = cursor.getInt(durationColumnIndex);
                String albumArt = ContentUris.withAppendedId(sArtworkUri, albumId).toString();

                libraryList.add(new MusicModel(
                        _id,
                        songName,
                        songPath,
                        album == null ? "" : album,
                        artist == null ? "" : artist,
                        albumArt,
                        albumId,
                        dateAdded,
                        dateModified,
                        trackNum,
                        duration));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return libraryList;
    }

    private String getSelection() {
        return "("
                + "(" + MediaStore.Audio.Media.IS_MUSIC + "==?)"
                + "AND (" + MediaStore.Audio.Media.IS_ALARM + "==?)"
                + "AND (" + MediaStore.Audio.Media.IS_NOTIFICATION + "==?)"
                + "AND (" + MediaStore.Audio.Media.IS_PODCAST + "==?)"
                + "AND (" + MediaStore.Audio.Media.IS_RINGTONE + "==?)"
                + ")";
    }

    private String[] getSelectionArgs() {
        return new String[]{"1", "0", "0", "0", "0"};
    }
}
