use std::{
    sync::Arc,
    thread,
    time::{Duration, Instant},
};

use eyre::Report;
use serenity::{
    all::{Context, EventHandler, GatewayIntents, Http, Ready},
    Client,
};
use songbird::SerenityInit;
use tokio::sync::mpsc;
use tracing::{info, warn};

use crate::RUNTIME;

use super::State;

impl super::DiscordBot {
    #[tracing::instrument(skip(self), fields(self.vc_id = %self.vc_id))]
    pub fn log_in(&mut self) -> Result<(), Report> {
        let mut state_lock = self.state.write();

        if !matches!(*state_lock, State::NotLoggedIn) {
            info!("Already logged in or currently logging in");
            return Ok(());
        }

        *state_lock = State::LoggingIn;

        let (tx, mut rx) = mpsc::channel(1);

        let token = self.token.clone();
        let songbird = self.songbird.clone();
        let mut client_task = self.client_task.lock();
        // While this should never happen, it's better to catch it than leave it running
        if let Some(client_task) = &*client_task {
            info!("Aborting previous client task");
            client_task.abort();
            // Wait for it to finish or 20 seconds to pass
            let start = Instant::now();
            while !client_task.is_finished()
                && start.duration_since(Instant::now()) < Duration::from_secs(10)
            {
                info!("Sleeping for client task to finish");
                thread::sleep(Duration::from_millis(500));
            }
            if !client_task.is_finished() {
                warn!("Client task did not finish");
            }
        }
        *client_task = Some(
            RUNTIME
                .spawn(async move {
                    let intents = GatewayIntents::GUILD_VOICE_STATES;

                    let mut client = match Client::builder(&token, intents)
                        .event_handler(Handler {
                            log_in_tx: tx.clone(),
                        })
                        .register_songbird_with(songbird)
                        .await
                    {
                        Ok(c) => c,
                        Err(e) => {
                            tx.send(Err(Report::new(e)))
                                .await
                                .expect("log_in rx dropped - please file a GitHub issue");
                            return;
                        }
                    };

                    if let Err(e) = client.start().await {
                        tx.send(Err(Report::new(e)))
                            .await
                            .expect("log_in rx dropped - please file a GitHub issue");
                    } else {
                        info!("Bot finished");
                    }
                })
                .abort_handle(),
        );

        match rx
            .blocking_recv()
            .expect("log_in tx dropped - please file a GitHub issue")
        {
            Ok(http) => {
                *state_lock = State::LoggedIn { http };
                Ok(())
            }
            Err(e) => {
                *state_lock = State::NotLoggedIn;
                Err(e)
            }
        }
    }
}

struct Handler {
    pub log_in_tx: mpsc::Sender<Result<Arc<Http>, Report>>,
}

#[serenity::async_trait]
impl EventHandler for Handler {
    async fn ready(&self, ctx: Context, _ready: Ready) {
        self.log_in_tx
            .send(Ok(ctx.http))
            .await
            .expect("log_in rx dropped - please file a GitHub issue");
    }
}
