use std::sync::Arc;

use dashmap::DashMap;
use eyre::{eyre, Context as _, Report};
use serenity::all::{Channel, ChannelType};
use songbird::CoreEvent;

use crate::RUNTIME;

use super::discord_receive::VoiceHandler;
use super::discord_speak::create_playable_input;
use super::State;

impl super::DiscordBot {
    /// Returns the voice channel name
    #[tracing::instrument(skip(self), fields(self.vc_id = %self.vc_id))]
    pub fn start(&mut self) -> Result<String, Report> {
        let mut state_lock = self.state.write();

        let State::LoggedIn { http } = &*state_lock else {
            if matches!(*state_lock, State::Started { .. }) {
                return Err(eyre!("Bot is already started."));
            } else {
                return Err(eyre!("Bot is not logged in. An error may have occurred when logging in - please check the console."));
            }
        };

        // In case there are any packets left over
        self.received_audio_rx.drain();

        let channel = match RUNTIME
            .block_on(http.get_channel(self.vc_id))
            .wrap_err("Couldn't get voice channel")?
        {
            Channel::Guild(c) if c.kind == ChannelType::Voice => c,
            _ => return Err(eyre!("The specified channel is not a voice channel.")),
        };

        let songbird = self.songbird.clone();
        let vc_id = self.vc_id;
        let guild_id = channel.guild_id;
        let received_audio_tx = self.received_audio_tx.clone();
        let senders = Arc::new(DashMap::new());
        {
            let senders = senders.clone();
            if let Err(e) = RUNTIME.block_on(async move {
                let call_lock = songbird
                    .join(guild_id, vc_id)
                    .await
                    .wrap_err("Unable to join call")?;
                let mut call = call_lock.lock().await;

                call.add_global_event(
                    CoreEvent::VoiceTick.into(),
                    VoiceHandler {
                        vc_id,
                        received_audio_tx,
                    },
                );

                call.play_only_input(create_playable_input(senders)?);
                // TODO: track error handling

                eyre::Ok(())
            }) {
                self.disconnect(guild_id);
                return Err(e);
            }
        }

        *state_lock = State::Started {
            http: http.clone(),
            guild_id,
            senders,
        };

        Ok(channel.name)
    }
}
