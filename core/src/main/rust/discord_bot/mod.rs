use std::{
    ops::Deref,
    sync::Arc,
    thread,
    time::{Duration, Instant},
};

use dashmap::DashMap;
use eyre::{eyre, Context as _, Report};
use parking_lot::{Mutex, RwLock};
use serenity::all::{ChannelId, GuildId, Http};
use songbird::{
    driver::{
        opus::coder::{Decoder, GenericCtl},
        DecodeMode,
    },
    Config, Songbird,
};
use tokio::task::AbortHandle;
use tracing::{debug, info, trace, warn};

use crate::audio_util::{RawAudio, MAX_AUDIO_BUFFER};
use crate::runtime::RUNTIME;

mod discord_receive;
mod discord_speak;
mod jni;
mod log_in;
mod start;
mod svc_receive;

type SenderId = i32;

struct Sender {
    audio_buffer_tx: flume::Sender<RawAudio>,
    audio_buffer_rx: flume::Receiver<RawAudio>,
    decoder: Mutex<Decoder>,
    last_audio_received: Mutex<Option<Instant>>,
}

struct DiscordBot {
    token: String,
    pub vc_id: ChannelId,
    songbird: Arc<Songbird>,
    state: RwLock<State>,
    received_audio_tx: flume::Sender<Vec<u8>>,
    received_audio_rx: flume::Receiver<Vec<u8>>,
    client_task: Mutex<Option<AbortHandle>>,
}

enum State {
    NotLoggedIn,
    LoggingIn,
    LoggedIn {
        http: Arc<Http>,
    },
    Started {
        http: Arc<Http>,
        guild_id: GuildId,
        senders: Arc<DashMap<i32, Sender>>,
    },
}

impl DiscordBot {
    pub fn new(token: String, vc_id: ChannelId) -> DiscordBot {
        let (received_audio_tx, received_audio_rx) = flume::bounded(MAX_AUDIO_BUFFER);
        DiscordBot {
            token,
            vc_id,
            songbird: {
                let songbird_config = Config::default().decode_mode(DecodeMode::Decrypt);
                Songbird::serenity_from_config(songbird_config)
            },
            state: RwLock::new(State::NotLoggedIn),
            received_audio_tx,
            received_audio_rx,
            client_task: Mutex::new(None),
        }
    }

    pub fn is_started(&self) -> bool {
        if let Some(lock) = self.state.try_read() {
            matches!(*lock, State::Started { .. })
        } else {
            // if the write lock is being held, the bot is currently
            // logging in, starting, or stopping, so it's not started
            false
        }
    }

    #[tracing::instrument(skip(self), fields(self.vc_id = %self.vc_id))]
    fn disconnect(&self, guild_id: GuildId) {
        let mut tries = 0;
        loop {
            if let Err(error) = RUNTIME.block_on(self.songbird.remove(guild_id)) {
                warn!(?error, "Failed to disconnect from the call: {error}");
                tries += 1;
            } else {
                info!("Successfully disconnected from call");
                break;
            }

            if tries < 5 {
                info!("Trying to disconnect again in 500 milliseconds");
                thread::sleep(Duration::from_millis(500));
            } else {
                warn!("Giving up on call disconnection");
                break;
            }
        }
    }

    #[tracing::instrument(skip(self), fields(self.vc_id = %self.vc_id))]
    pub fn stop(&mut self) -> Result<(), Report> {
        let mut state_lock = self.state.write();

        let State::Started { http, guild_id, .. } = &*state_lock else {
            info!("Bot is not started");
            return Ok(());
        };

        self.disconnect(*guild_id);

        // senders will be dropped here and the decoders will free themselves (thanks Rust)
        *state_lock = State::LoggedIn { http: http.clone() };

        Ok(())
    }

    pub fn block_for_speaking_opus_data(&self) -> Result<Vec<u8>, Report> {
        // If we don't have a timeout, the sender thread will continue to block when the bot stops
        self.received_audio_rx
            .recv_timeout(Duration::from_secs(1))
            .wrap_err("failed to recv receive_audio")
    }

    #[tracing::instrument(skip(self), fields(self.vc_id = %self.vc_id))]
    pub fn reset_senders(&self) -> Result<(), Report> {
        /// Make sure to mirror this value on the Java side (`DiscordBot.MILLISECONDS_UNTIL_RESET`)
        const DURATION_UNTIL_RESET: Duration = Duration::from_millis(1000);

        trace!("Getting state read lock");

        let State::Started { senders, .. } = &*self.state.read() else {
            return Err(eyre!("Bot is not started"));
        };

        debug!("Resetting senders");

        let now = Instant::now();
        for sender in senders.deref() {
            let sender = sender.value();
            let mut lock = sender.last_audio_received.lock();
            if let Some(time) = *lock {
                if time.duration_since(now) > DURATION_UNTIL_RESET {
                    *lock = None;
                    drop(lock); // drop it before reset in case reset takes a while
                    if let Err(error) = sender.decoder.lock().reset_state() {
                        // realistically this shouldn't ever happen but if
                        // it does we don't want to skip other resets so
                        // just debug log the error
                        info!(?error, "error when resetting decoder: {error}");
                    }
                }
            }
        }

        debug!("Reset senders");

        Ok(())
    }
}

impl Drop for DiscordBot {
    #[tracing::instrument(skip(self), fields(self.vc_id = %self.vc_id))]
    fn drop(&mut self) {
        if let Some(client_task) = &*self.client_task.lock() {
            info!("Aborting client task");
            client_task.abort();
        }
    }
}
