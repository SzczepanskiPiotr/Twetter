package sdp.project.twitter.Utils;

import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;

public abstract class CustomAnimationDrawable extends AnimationDrawable {

    Handler mAnimationHandler;

    protected CustomAnimationDrawable(AnimationDrawable animation){
        for(int i = 0; i < animation.getNumberOfFrames(); i++){
            this.addFrame(animation.getFrame(i), animation.getDuration(i));
        }
    }

    @Override
    public void start(){
        super.start();

        mAnimationHandler = new Handler();
        mAnimationHandler.post(this::onAnimationStart);
        mAnimationHandler.postDelayed(this::onAnimationFinish, getTotalDuration());
    }

    protected abstract void onAnimationStart();
    protected abstract void onAnimationFinish();

    private int getTotalDuration(){
        int duration = 0;
        for(int i = 0; i < this.getNumberOfFrames(); i++){
            duration += this.getDuration(i);
        }
        return duration;
    }
}


