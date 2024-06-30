use std::{f64::consts::PI, time::Instant};

use dashmap::{mapref::one::RefMut, DashMap};
use eyre::{eyre, Context as _, Report};
use parking_lot::Mutex;
use songbird::driver::opus::coder::Decoder;

use crate::audio_util::{
    adjust_volume, RawAudio, CHANNELS, MAX_AUDIO_BUFFER, OPUS_CHANNELS, OPUS_SAMPLE_RATE,
    RAW_AUDIO_SIZE,
};

use super::{Sender, SenderId, State};

impl super::DiscordBot {
    #[inline]
    fn get_or_insert_sender(
        senders: &DashMap<SenderId, Sender>,
        sender_id: SenderId,
    ) -> RefMut<'_, SenderId, Sender> {
        if !senders.contains_key(&sender_id) {
            let (audio_buffer_tx, audio_buffer_rx) = flume::bounded(MAX_AUDIO_BUFFER);
            senders.insert(
                sender_id,
                Sender {
                    audio_buffer_tx,
                    audio_buffer_rx,
                    decoder: Mutex::new(
                        Decoder::new(OPUS_SAMPLE_RATE, OPUS_CHANNELS)
                            .expect("Unable to make opus decoder"),
                    ),
                    last_audio_received: Mutex::new(None),
                },
            );
        }
        // we just inserted it, this shouldn't fail
        senders.get_mut(&sender_id).unwrap()
    }

    #[tracing::instrument(skip(self, raw_opus_data), fields(self.vc_id = %self.vc_id))]
    pub fn add_audio_to_hearing_buffer(
        &mut self,
        sender_id: SenderId,
        raw_opus_data: Vec<u8>,
        adjust_based_on_distance: bool,
        distance: f64,
        max_distance: f64,
    ) -> Result<(), Report> {
        let State::Started { senders, .. } = &*self.state.read() else {
            return Err(eyre!("Bot is not started"));
        };
        let sender = Self::get_or_insert_sender(senders, sender_id);
        if sender.audio_buffer_tx.is_full() {
            return Err(eyre!("Sender audio buffer is full"));
        }

        let mut audio = vec![0i16; RAW_AUDIO_SIZE * CHANNELS as usize];
        sender
            .decoder
            .lock()
            .decode(
                Some((&raw_opus_data).try_into().wrap_err("Invalid opus data")?),
                (&mut audio).try_into().wrap_err("Unable to wrap output")?,
                false,
            )
            .wrap_err("Unable to decode raw opus data")?;
        let len = audio.len();
        let mut audio: RawAudio = audio.try_into().map_err(|_| {
            eyre!("Decoded audio is of length {len} when it should be {RAW_AUDIO_SIZE}")
        })?;

        if adjust_based_on_distance {
            // Hopefully this is a similar volume curve to what Minecraft/OpenAL uses
            let volume = ((distance / max_distance) * (PI / 2.0)).cos();
            if volume <= 0.0 {
                return Err(eyre!("Skipping packet since volume is {volume}"));
            }
            if volume < 1.0 {
                // only adjust volume if it's less than 100%
                adjust_volume(&mut audio, volume);
            }
        }

        sender
            .audio_buffer_tx
            .send(audio)
            .expect("audio_buffer rx closed - please file a GitHub issue");
        // get the now before acquiring lock
        // if we did Some(Instant::now()) I'm not sure if it
        // would delay the now until lock is acquired
        let now = Instant::now();
        *(sender.last_audio_received.lock()) = Some(now);
        Ok(())
    }
}
