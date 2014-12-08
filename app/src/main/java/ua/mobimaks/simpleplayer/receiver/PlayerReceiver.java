package ua.mobimaks.simpleplayer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ua.mobimaks.simpleplayer.model.PlayerConstants;
import ua.mobimaks.simpleplayer.service.PlayerService.OnPlayerListener;

public class PlayerReceiver extends BroadcastReceiver {

    private OnPlayerListener mReceiverListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mReceiverListener != null) {
            switch (intent.getAction()) {
                case PlayerConstants.PLAY_PAUSE:
                    mReceiverListener.onPlayPause();
                    break;
                case PlayerConstants.NEXT:
                    mReceiverListener.onPlayNext();
                    break;
                case PlayerConstants.PREVIOUS:
                    mReceiverListener.onPlayPrevious();
                    break;
                case Intent.ACTION_HEADSET_PLUG:
                    int state = intent.getIntExtra("state", 0);
                    if (state == 0){
                        mReceiverListener.onPause();
                    } else {
                        mReceiverListener.onPlay();
                    }
                    break;
            }
        }
    }

    public void setOnReceiveListener(OnPlayerListener listener) {
        mReceiverListener = listener;
    }
}
