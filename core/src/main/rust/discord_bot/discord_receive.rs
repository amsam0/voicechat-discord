use discortp::{
    rtp::{RtpExtensionPacket, RtpPacket},
    Packet, PacketSize,
};
use serenity::all::ChannelId;
use songbird::{Event, EventContext, EventHandler};
use tracing::{debug, trace, warn};

pub struct VoiceHandler {
    pub vc_id: ChannelId,
    pub received_audio_tx: flume::Sender<Vec<u8>>,
}

#[serenity::async_trait]
impl EventHandler for VoiceHandler {
    #[tracing::instrument(skip(self, ctx), fields(self.vc_id = %self.vc_id))]
    async fn act(&self, ctx: &EventContext<'_>) -> Option<Event> {
        if let EventContext::VoiceTick(tick) = ctx {
            if self.received_audio_tx.is_full() {
                debug!("Receive buffer is full");
                return None;
            }
            let Some(data) = tick.speaking.values().next() else {
                trace!("No one speaking");
                return None;
            };
            let Some(data) = data.packet.as_ref() else {
                debug!("Missing packet");
                return None;
            };
            // https://github.com/serenity-rs/songbird/blob/5bbe80f20c2a7e4e889149672d8ae03f6450b9e8/src/driver/tasks/udp_rx/ssrc_state.rs#L86
            let Some(rtp) = RtpPacket::new(&data.packet) else {
                warn!("Unable to parse packet");
                return None;
            };
            let extension = rtp.get_extension() != 0;

            let payload = rtp.payload();

            // https://github.com/serenity-rs/songbird/blob/5bbe80f20c2a7e4e889149672d8ae03f6450b9e8/src/driver/tasks/udp_rx/ssrc_state.rs#L153
            let payload = &payload[data.payload_offset..data.payload_end_pad];
            let start = if extension {
                match RtpExtensionPacket::new(payload).map(|pkt| pkt.packet_size()) {
                    Some(s) => s,
                    None => {
                        warn!("Unable to parse extension packet");
                        return None;
                    }
                }
            } else {
                0
            };

            let payload = payload[start..].to_vec();

            if self.received_audio_tx.send(payload).is_err() {
                warn!("received_audio rx dropped");
            }
        }
        None
    }
}
