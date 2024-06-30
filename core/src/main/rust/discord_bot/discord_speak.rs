use std::{
    io::{self, Write},
    sync::Arc,
};

use dashmap::DashMap;
use eyre::{Context, Report};
use songbird::input::{
    codecs::{CODEC_REGISTRY, PROBE},
    core::io::MediaSource,
    Input, RawAdapter,
};

use crate::audio_util::{combine_audio_parts, CHANNELS, SAMPLE_RATE};

use super::Sender;

#[inline]
pub fn create_playable_input(senders: Arc<DashMap<i32, Sender>>) -> Result<Input, Report> {
    let audio_source = SendersAudioSource { senders };
    let input: Input = RawAdapter::new(audio_source, SAMPLE_RATE, CHANNELS).into();
    let input = match input {
        Input::Live(i, _) => i,
        _ => unreachable!("From<RawAdapter> for Input always gives Input::Live"),
    };
    let parsed = input
        .promote(&CODEC_REGISTRY, &PROBE)
        .wrap_err("Unable to promote input")?;
    Ok(Input::Live(parsed, None))
}

struct SendersAudioSource {
    senders: Arc<DashMap<i32, Sender>>,
}

impl io::Read for SendersAudioSource {
    fn read(&mut self, mut buf: &mut [u8]) -> io::Result<usize> {
        let parts: Vec<_> = self
            .senders
            .iter()
            .filter_map(|kv| {
                // We don't want to block
                kv.value().audio_buffer_rx.try_recv().ok()
            })
            .collect();
        let combined = combine_audio_parts(parts);
        let mut written = 0;
        for sample in combined {
            // Thanks FelixMcFelix :)
            let converted = (sample as f32) / (i16::MIN as f32).abs();
            written += buf.write(&converted.to_le_bytes())?;
        }
        Ok(written)
    }
}

impl MediaSource for SendersAudioSource {
    #[inline]
    fn is_seekable(&self) -> bool {
        false
    }

    #[inline]
    fn byte_len(&self) -> Option<u64> {
        None
    }
}

impl io::Seek for SendersAudioSource {
    fn seek(&mut self, _pos: io::SeekFrom) -> io::Result<u64> {
        Err(io::ErrorKind::Unsupported.into())
    }
}
