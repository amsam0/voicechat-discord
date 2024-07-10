use jni::{objects::JClass, sys::jint, JNIEnv};
use parking_lot::Mutex;
use tracing::{info, level_filters::LevelFilter, subscriber::Interest, Level, Metadata};
use tracing_subscriber::{
    fmt,
    layer::{Context, Filter, SubscriberExt},
    reload::{self, Handle},
    util::SubscriberInitExt,
    Layer, Registry,
};

static RELOAD_HANDLE: Mutex<Option<Handle<CustomFilter, Registry>>> = Mutex::new(None);

struct CustomFilter {
    max_level: Level,
}

impl CustomFilter {
    fn new(max_level: Level) -> CustomFilter {
        CustomFilter { max_level }
    }

    fn set_max_level(&mut self, max_level: Level) {
        self.max_level = max_level;
    }

    #[inline]
    fn enabled(&self, meta: &Metadata<'_>) -> bool {
        meta.level() <= &self.max_level && meta.target().starts_with("voicechat_discord")
    }
}

impl<S: tracing::Subscriber> Filter<S> for CustomFilter {
    #[inline]
    fn enabled(&self, meta: &Metadata<'_>, _cx: &Context<'_, S>) -> bool {
        self.enabled(meta)
    }

    #[inline]
    fn callsite_enabled(&self, meta: &'static Metadata<'static>) -> Interest {
        if self.enabled(meta) {
            Interest::always()
        } else {
            Interest::never()
        }
    }

    #[inline]
    fn max_level_hint(&self) -> Option<LevelFilter> {
        Some(self.max_level.into())
    }
}

impl<S: tracing::Subscriber> Layer<S> for CustomFilter {
    #[inline]
    fn enabled(&self, meta: &Metadata<'_>, _cx: Context<'_, S>) -> bool {
        self.enabled(meta)
    }

    #[inline]
    fn register_callsite(&self, metadata: &'static Metadata<'static>) -> Interest {
        if self.enabled(metadata) {
            Interest::always()
        } else {
            Interest::never()
        }
    }

    #[inline]
    fn max_level_hint(&self) -> Option<LevelFilter> {
        Some(self.max_level.into())
    }
}

pub fn ensure_init() {
    let (filter, reload_handle) = reload::Layer::new(CustomFilter::new(Level::WARN));
    if tracing_subscriber::registry()
        .with(filter)
        .with(fmt::layer())
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
            .modify(|filter| filter.set_max_level(Level::WARN))
            .expect("failed to change logging level"),
        1 => reload_handle
            .modify(|filter| filter.set_max_level(Level::INFO))
            .expect("failed to change logging level"),
        2 => reload_handle
            .modify(|filter| filter.set_max_level(Level::DEBUG))
            .expect("failed to change logging level"),
        3..=i32::MAX => reload_handle
            .modify(|filter| filter.set_max_level(Level::TRACE))
            .expect("failed to change logging level"),
    }
}
