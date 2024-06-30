use jni::{objects::JClass, sys::jint, JNIEnv};
use parking_lot::Mutex;
use tracing::{info, level_filters::LevelFilter};
use tracing_subscriber::{
    filter,
    fmt::{self},
    layer::SubscriberExt,
    reload::{self, Handle},
    util::SubscriberInitExt,
    Registry,
};

static RELOAD_HANDLE: Mutex<Option<Handle<LevelFilter, Registry>>> = Mutex::new(None);

#[no_mangle]
pub extern "system" fn Java_dev_naturecodevoid_voicechatdiscord_Core_initLogger<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
) {
    let (filter, reload_handle) = reload::Layer::new(filter::LevelFilter::WARN);
    if tracing_subscriber::registry()
        .with(filter)
        .with(fmt::Layer::default())
        .try_init()
        .is_err()
    {
        info!("Default subscriber is already set. This is probably fine");
        return;
    }
    *(RELOAD_HANDLE.lock()) = Some(reload_handle);
}

#[no_mangle]
pub extern "system" fn Java_dev_naturecodevoid_voicechatdiscord_Core_setDebugLevel<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    debug_level: jint,
) {
    let lock = RELOAD_HANDLE.lock();
    let reload_handle = lock
        .as_ref()
        .expect("RELOAD_HANDLE should be Some by the time this function is called");
    match debug_level {
        i32::MIN..=0 => reload_handle
            .modify(|filter| *filter = filter::LevelFilter::WARN)
            .expect("failed to change logging level"),
        1 => reload_handle
            .modify(|filter| *filter = filter::LevelFilter::INFO)
            .expect("failed to change logging level"),
        2 => reload_handle
            .modify(|filter| *filter = filter::LevelFilter::DEBUG)
            .expect("failed to change logging level"),
        3..=i32::MAX => reload_handle
            .modify(|filter| *filter = filter::LevelFilter::TRACE)
            .expect("failed to change logging level"),
    }
}
