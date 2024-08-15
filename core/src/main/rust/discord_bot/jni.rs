use jni::{
    objects::{JByteArray, JClass, JString},
    sys::{jboolean, jdouble, jint, jlong, jobject, JNI_TRUE},
    JNIEnv,
};
use serenity::all::ChannelId;
use tracing::info;

use crate::ResultExt;

use super::DiscordBot;

#[no_mangle]
pub extern "system" fn Java_dev_amsam0_voicechatdiscord_DiscordBot__1new<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    token: JString<'local>,
    vc_id: jlong,
) -> jlong {
    let token = env
        .get_string(&token)
        .expect("Couldn't get java string! Please file a GitHub issue")
        .into();

    let discord_bot = DiscordBot::new(token, ChannelId::new(vc_id as u64));
    Box::into_raw(Box::new(discord_bot)) as jlong
}

#[no_mangle]
pub extern "system" fn Java_dev_amsam0_voicechatdiscord_DiscordBot__1isStarted(
    mut _env: JNIEnv<'_>,
    _obj: jobject,
    ptr: jlong,
) -> jboolean {
    let discord_bot = unsafe { &mut *(ptr as *mut DiscordBot) };
    discord_bot.is_started() as jboolean
}

#[no_mangle]
pub extern "system" fn Java_dev_amsam0_voicechatdiscord_DiscordBot__1logIn(
    mut env: JNIEnv<'_>,
    _obj: jobject,
    ptr: jlong,
) {
    let discord_bot = unsafe { &mut *(ptr as *mut DiscordBot) };
    discord_bot.log_in().discard_or_throw(&mut env);
}

#[no_mangle]
pub extern "system" fn Java_dev_amsam0_voicechatdiscord_DiscordBot__1start(
    mut env: JNIEnv<'_>,
    _obj: jobject,
    ptr: jlong,
) -> JString<'_> {
    let discord_bot = unsafe { &mut *(ptr as *mut DiscordBot) };

    // We must create the error string before the error is thrown or java gets really mad
    let value_on_throw = env
        .new_string("<error>")
        .expect("Couldn't create java string! Please file a GitHub issue");

    discord_bot
        .start()
        .map(|s| {
            env.new_string(s)
                .expect("Couldn't create java string! Please file a GitHub issue")
        })
        .unwrap_or_throw(&mut env, value_on_throw)
}

#[no_mangle]
pub extern "system" fn Java_dev_amsam0_voicechatdiscord_DiscordBot__1stop(
    mut env: JNIEnv<'_>,
    _obj: jobject,
    ptr: jlong,
) {
    let discord_bot = unsafe { &mut *(ptr as *mut DiscordBot) };
    discord_bot.stop().discard_or_throw(&mut env);
}

#[no_mangle]
pub extern "system" fn Java_dev_amsam0_voicechatdiscord_DiscordBot__1free(
    mut _env: JNIEnv<'_>,
    _obj: jobject,
    ptr: jlong,
) {
    let _ = unsafe { Box::from_raw(ptr as *mut DiscordBot) };
}

#[no_mangle]
pub extern "system" fn Java_dev_amsam0_voicechatdiscord_DiscordBot__1addAudioToHearingBuffer<
    'local,
>(
    env: JNIEnv<'local>,
    _obj: jobject,
    ptr: jlong,
    sender_id: jint,
    raw_opus_data: JByteArray<'local>,
    adjust_based_on_distance: jboolean,
    distance: jdouble,
    max_distance: jdouble,
) {
    let discord_bot = unsafe { &mut *(ptr as *mut DiscordBot) };

    let raw_opus_data = env
        .convert_byte_array(raw_opus_data)
        .expect("Unable to convert byte array. Please file a GitHub issue");

    if let Err(e) = discord_bot.add_audio_to_hearing_buffer(
        sender_id,
        raw_opus_data,
        adjust_based_on_distance == JNI_TRUE,
        distance,
        max_distance,
    ) {
        info!(
            "Error when adding audio for bot with vc_id {}: {e:#}",
            discord_bot.vc_id
        );
    }
}

#[no_mangle]
pub extern "system" fn Java_dev_amsam0_voicechatdiscord_DiscordBot__1blockForSpeakingBufferOpusData(
    env: JNIEnv<'_>,
    _obj: jobject,
    ptr: jlong,
) -> JByteArray<'_> {
    let discord_bot = unsafe { &mut *(ptr as *mut DiscordBot) };

    let opus_data = match discord_bot.block_for_speaking_opus_data() {
        Ok(d) => d,
        Err(e) => {
            info!(
                "Failed to get speaking opus data for bot with vc_id {}: {e:#}",
                discord_bot.vc_id
            );

            return env
                .byte_array_from_slice(&[])
                .expect("Couldn't create byte array from slice. Please file a GitHub issue");
        }
    };

    env.byte_array_from_slice(&opus_data)
        .expect("Couldn't create byte array from slice. Please file a GitHub issue")
}

#[no_mangle]
pub extern "system" fn Java_dev_amsam0_voicechatdiscord_DiscordBot__1resetSenders(
    _env: JNIEnv<'_>,
    _obj: jobject,
    ptr: jlong,
) {
    let discord_bot = unsafe { &mut *(ptr as *mut DiscordBot) };

    if let Err(e) = discord_bot.reset_senders() {
        info!(
            "Error for bot with vc_id {} when resetting senders: {e:#}",
            discord_bot.vc_id
        );
    }
}
