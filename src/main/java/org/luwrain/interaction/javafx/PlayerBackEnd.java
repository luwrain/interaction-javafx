package org.luwrain.interaction.javafx;

import javafx.scene.media.Media;                                             
import javafx.scene.media.MediaPlayer;

import java.util.concurrent.Callable;

import org.luwrain.core.NullCheck;
import org.luwrain.player.BackEnd;
import org.luwrain.player.BackEndStatus;

public class PlayerBackEnd implements BackEnd
{

    private MediaPlayer player = null;
    private BackEndStatus status;

    public PlayerBackEnd(BackEndStatus status)
    {
		NullCheck.notNull(status, "status");
		this.status = status;
    }

    @Override public boolean play(String uri)
    {
    	NullCheck.notNull(uri, "uri");
    	Callable<Boolean> task=new Callable<Boolean>()
		{
			@Override public Boolean call() throws Exception
			{
				final Media media = new Media(uri);
				if(player!=null)
				{
					player.stop();
					//player.dispose(); // FIXME: or manual remove listeners
				}
				player = new MediaPlayer(media);
				player.currentTimeProperty().addListener((observable, oldValue, newValue)->
				{
					status.onBackEndTime((int)Math.floor(newValue.toSeconds()));
				});
				player.setOnEndOfMedia(new Runnable()
				{
					@Override public void run()
					{
						status.onBackEndFinish();
					}
				});
				player.play();
				return true;
			}
		};
    	return Utils.fxcall(task,false);
    }                                                                                            

    @Override public void stop()
    {
    	Runnable task=new Runnable()
		{
			@Override public void run()
			{
				if (player == null)
					return;
				player.stop();
			}
		};
		Utils.fxnowait(task);
    }

}
