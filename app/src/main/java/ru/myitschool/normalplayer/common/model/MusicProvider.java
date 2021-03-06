package ru.myitschool.normalplayer.common.model;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ru.myitschool.normalplayer.R;
import ru.myitschool.normalplayer.utils.MediaIDUtil;

import static ru.myitschool.normalplayer.utils.MediaIDUtil.MEDIA_ID_MUSICS_ALL;
import static ru.myitschool.normalplayer.utils.MediaIDUtil.MEDIA_ID_MUSICS_BY_ALBUM;
import static ru.myitschool.normalplayer.utils.MediaIDUtil.MEDIA_ID_MUSICS_BY_ARTIST;
import static ru.myitschool.normalplayer.utils.MediaIDUtil.MEDIA_ID_MUSICS_BY_GENRE;
import static ru.myitschool.normalplayer.utils.MediaIDUtil.MEDIA_ID_MUSICS_BY_SEARCH;
import static ru.myitschool.normalplayer.utils.MediaIDUtil.MEDIA_ID_ROOT;
import static ru.myitschool.normalplayer.utils.MediaIDUtil.createMediaID;

public class MusicProvider {

    public static final String EXTRA_DURATION = "extra_duration";
    private static final String TAG = MusicProvider.class.getSimpleName();

    private final ConcurrentMap<String, MutableMediaMetadata> musicListById;
    private final MusicProviderSource source;
    // Categorized caches for music track data:
    private ConcurrentMap<String, List<MediaMetadataCompat>> musicListByArtist;
    private ConcurrentMap<String, List<MediaMetadataCompat>> musicListByAlbum;
    private ConcurrentMap<String, List<MediaMetadataCompat>> musicListByGenre;

    private volatile State currentState = State.NON_INITIALIZED;

    public MusicProvider(MusicProviderSource source) {
        this.source = source;
        musicListByArtist = new ConcurrentHashMap<>();
        musicListByAlbum = new ConcurrentHashMap<>();
        musicListByGenre = new ConcurrentHashMap<>();
        musicListById = new ConcurrentHashMap<>();
    }

    public Iterable<String> getAllMusic() {
        if (currentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return musicListById.keySet();
    }

    public Iterable<String> getGenres() {
        if (currentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return musicListByGenre.keySet();
    }

    public Iterable<String> getAlbums() {
        if (currentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return musicListByAlbum.keySet();
    }


    public Iterable<String> getArtists() {
        if (currentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        return musicListByArtist.keySet();
    }

    public Iterable<MediaMetadataCompat> getShuffledMusic() {
        if (currentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> shuffled = new ArrayList<>(musicListById.size());
        for (MutableMediaMetadata mutableMetadata : musicListById.values()) {
            shuffled.add(mutableMetadata.metadata);
        }
        Collections.shuffle(shuffled);
        return shuffled;
    }

    public Iterable<MediaMetadataCompat> getAllMusics() {
        if (currentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> result = new ArrayList<>(musicListById.size());
        for (MutableMediaMetadata mutableMetadata : musicListById.values()) {
            result.add(mutableMetadata.metadata);
        }
        return result;
    }

    public Iterable<MediaMetadataCompat> getMusicsByArtist(String artist) {
        if (currentState != State.INITIALIZED || !musicListByArtist.containsKey(artist)) {
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> result = musicListByArtist.get(artist);
        return result;
    }

    public Iterable<MediaMetadataCompat> getMusicsByGenre(String genre) {
        if (currentState != State.INITIALIZED || !musicListByGenre.containsKey(genre)) {
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> result = musicListByGenre.get(genre);
        return result;
    }

    public Iterable<MediaMetadataCompat> getMusicsByAlbum(String album) {
        if (currentState != State.INITIALIZED || !musicListByAlbum.containsKey(album)) {
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> result = musicListByAlbum.get(album);
        return result;
    }


    public Iterable<MediaMetadataCompat> searchMusicBySongTitle(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_TITLE, query);
    }


    public Iterable<MediaMetadataCompat> searchMusicByAlbum(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_ALBUM, query);
    }


    public Iterable<MediaMetadataCompat> searchMusicByArtist(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_ARTIST, query);
    }

    private Iterable<MediaMetadataCompat> searchMusic(String metadataField, String query) {
        if (currentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        ArrayList<MediaMetadataCompat> result = new ArrayList<>();
        query = query.toLowerCase(Locale.US);
        for (MutableMediaMetadata track : musicListById.values()) {
            if (track.metadata.getString(metadataField).toLowerCase(Locale.US)
                    .contains(query)) {
                result.add(track.metadata);
            }
        }
        return result;
    }

    public boolean isInitialized() {
        return currentState == State.INITIALIZED;
    }

    public MediaMetadataCompat getMusic(String musicId) {
        return musicListById.containsKey(musicId) ? musicListById.get(musicId).metadata : null;
    }

    @SuppressLint("StaticFieldLeak")
    public void retrieveMediaAsync(final Callback callback) {
        Log.d(TAG, "retrieveMediaAsync called");
        if (currentState == State.INITIALIZED) {
            if (callback != null) {
                // Nothing to do, execute callback immediately
                callback.onMusicCatalogReady(true);
            }
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                retrieveMedia();
                return currentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onMusicCatalogReady(current == State.INITIALIZED);
                }
            }
        }.execute();
    }

    private synchronized void retrieveMedia() {
        try {
            if (currentState == State.NON_INITIALIZED) {
                currentState = State.INITIALIZING;

                Iterator<MediaMetadataCompat> tracks = source.iterator();
                while (tracks.hasNext()) {
                    MediaMetadataCompat item = tracks.next();
                    String musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    musicListById.put(musicId, new MutableMediaMetadata(musicId, item));
                }
                buildListsByGenre();
                buildListsByAlbum();
                buildListsByArtist();
                currentState = State.INITIALIZED;
            }
        } finally {
            if (currentState != State.INITIALIZED) {
                currentState = State.NON_INITIALIZED;
            }
        }
    }

    private synchronized void buildListsByGenre() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByGenre = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : musicListById.values()) {
            String genre = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
            List<MediaMetadataCompat> list = newMusicListByGenre.get(genre);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByGenre.put(genre, list);
            }
            list.add(m.metadata);
        }
        musicListByGenre = newMusicListByGenre;
    }

    private synchronized void buildListsByAlbum() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByAlbum = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : musicListById.values()) {
            String album = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
            List<MediaMetadataCompat> list = newMusicListByAlbum.get(album);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByAlbum.put(album, list);
            }
            list.add(m.metadata);
        }
        musicListByAlbum = newMusicListByAlbum;
    }

    private synchronized void buildListsByArtist() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByArtist = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : musicListById.values()) {
            String artist = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            List<MediaMetadataCompat> list = newMusicListByArtist.get(artist);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByArtist.put(artist, list);
            }
            list.add(m.metadata);
        }
        musicListByArtist = newMusicListByArtist;
    }

    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId, Resources resources) {
        Log.d(TAG, "getChildren: ");
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (!MediaIDUtil.isBrowseable(mediaId)) {
            return mediaItems;
        }

        if (MEDIA_ID_ROOT.equals(mediaId)) {
            mediaItems.add(createBrowsableMediaItem(mediaId, resources, null));

        } else if (MEDIA_ID_MUSICS_ALL.equals(mediaId)) {
            for (String id : getAllMusic()) {
                mediaItems.add(createMediaItem(getMusic(id), MEDIA_ID_MUSICS_ALL));
            }
        } else if (MEDIA_ID_MUSICS_BY_GENRE.equals(mediaId)) {
            for (String genre : getGenres()) {
                mediaItems.add(createBrowsableMediaItem(mediaId, resources, genre));
            }

        } else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_GENRE)) {
            String genre = MediaIDUtil.getHierarchy(mediaId)[1];
            for (MediaMetadataCompat metadata : getMusicsByGenre(genre)) {
                mediaItems.add(createMediaItem(metadata, MEDIA_ID_MUSICS_BY_GENRE));
            }

        } else if (MEDIA_ID_MUSICS_BY_ALBUM.equals(mediaId)) {
            for (String album : getAlbums()) {
                mediaItems.add(createBrowsableMediaItem(mediaId, resources, album));
            }

        } else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_ALBUM)) {
            String album = MediaIDUtil.getHierarchy(mediaId)[1];
            for (MediaMetadataCompat metadata : getMusicsByAlbum(album)) {
                mediaItems.add(createMediaItem(metadata, MEDIA_ID_MUSICS_BY_ALBUM));
            }

        } else if (MEDIA_ID_MUSICS_BY_ARTIST.equals(mediaId)) {
            for (String artist : getArtists()) {
                Log.d(TAG, "getChildren: " + artist);
                mediaItems.add(createBrowsableMediaItem(mediaId, resources, artist));
            }

        } else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_ARTIST)) {
            String artist = MediaIDUtil.getHierarchy(mediaId)[1];
            for (MediaMetadataCompat metadata : getMusicsByArtist(artist)) {
                mediaItems.add(createMediaItem(metadata, MEDIA_ID_MUSICS_BY_ARTIST));
            }
        } else {
            Log.w(TAG, "Skipping unmatched mediaId: " + mediaId);
        }
        return mediaItems;
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItem(String mediaId, Resources resources, String parameter) {

        String localMediaId = "";
        String localTitle = "";
        String localSubtitle = "";

        MediaDescriptionCompat.Builder descriptionBuilder = new MediaDescriptionCompat.Builder();

        if (MEDIA_ID_ROOT.equals(mediaId)) {
            localMediaId = MEDIA_ID_MUSICS_BY_ALBUM;
            localTitle = resources.getString(R.string.browse_genres);
            localSubtitle = resources.getString(R.string.browse_genre_subtitle);
        } else if (MEDIA_ID_MUSICS_ALL.equals(mediaId)) {
            localMediaId = MEDIA_ID_MUSICS_ALL;
            localTitle = resources.getString(R.string.browse_genres);
            localSubtitle = resources.getString(R.string.browse_genre_subtitle);
        } else if (MEDIA_ID_MUSICS_BY_GENRE.equals(mediaId)) {
            localMediaId = createMediaID(null, MEDIA_ID_MUSICS_BY_GENRE, parameter);
            localTitle = parameter;
            localSubtitle = resources.getString(R.string.browse_musics_by_genre_subtitle, parameter);
        } else if (MEDIA_ID_MUSICS_BY_ALBUM.equals(mediaId)) {
            localMediaId = createMediaID(null, MEDIA_ID_MUSICS_BY_ALBUM, parameter);
            localTitle = parameter;
            localSubtitle = resources.getString(R.string.browse_musics_by_album_subtitle, parameter);

            MediaMetadataCompat metadata = searchMusicByAlbum(parameter).iterator().next();
            String iconUri = "";
            if (metadata != null) {
                iconUri = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
            }
            descriptionBuilder.setIconUri(Uri.parse(iconUri));

        } else if (MEDIA_ID_MUSICS_BY_ARTIST.equals(mediaId)) {
            localMediaId = createMediaID(null, MEDIA_ID_MUSICS_BY_ARTIST, parameter);
            localTitle = parameter;
            localSubtitle = resources.getString(R.string.browse_musics_by_artist_subtitle, parameter);
        } else {
            Log.w(TAG, "Skipping unmatched mediaId: " + mediaId);
        }

        Bundle extras =  new Bundle();
        extras.putLong(MusicProviderSource.SOURCE_TYPE_KEY, source.sourceType.getValue());

        descriptionBuilder
                .setMediaId(localMediaId)
                .setTitle(localTitle)
                .setSubtitle(localSubtitle)
                .setExtras(extras);
        return new MediaBrowserCompat.MediaItem(descriptionBuilder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);

    }

    private MediaBrowserCompat.MediaItem createMediaItem(MediaMetadataCompat metadata, String key) {
        String unique;

        switch (key) {
            case MEDIA_ID_MUSICS_BY_ALBUM:
                unique = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
                break;
            case MEDIA_ID_MUSICS_BY_ARTIST:
                unique = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
                break;
            case MEDIA_ID_MUSICS_BY_SEARCH:
                unique = MEDIA_ID_MUSICS_BY_SEARCH;
                break;
            default:
                unique = metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
                break;
        }

        String hierarchyAwareMediaID = createMediaID(metadata.getDescription().getMediaId(), key, unique);
        Bundle extras = new Bundle();
        extras.putLong(EXTRA_DURATION, metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
        extras.putLong(MusicProviderSource.SOURCE_TYPE_KEY, metadata.getLong(MusicProviderSource.SOURCE_TYPE_KEY));
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                .setSubtitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                .setIconUri(Uri.parse(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)))
                .setMediaId(hierarchyAwareMediaID)
                .setMediaUri(Uri.parse(metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)))
                .setIconBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
                .setExtras(extras)
                .build();

        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

    }

    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }
}
